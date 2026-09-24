package com.rebecca.sunplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rebecca.sunplayer.data.AudioLibraryRepository
import com.rebecca.sunplayer.data.AudioScanner
import com.rebecca.sunplayer.model.AudioTrack
import com.rebecca.sunplayer.playback.AudioPlayerManager
import com.rebecca.sunplayer.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SortOption {
    TITLE, ARTIST, ALBUM, DURATION
}

class MainActivity : ComponentActivity() {

    private lateinit var playerManager: AudioPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerManager = AudioPlayerManager(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFFB74D), // Sun orange / gold
                    onPrimary = Color(0xFF3E2723),
                    primaryContainer = Color(0xFF4E342E),
                    onPrimaryContainer = Color(0xFFFFE0B2),
                    surface = Color(0xFF1E1E1E),
                    onSurface = Color(0xFFEEEEEE),
                    surfaceVariant = Color(0xFF2C2C2C),
                    onSurfaceVariant = Color(0xFFBDBDBD),
                    background = Color(0xFF121212),
                    onBackground = Color(0xFFEEEEEE)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(playerManager = playerManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(playerManager: AudioPlayerManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scanner = remember { AudioScanner(context) }
    val libraryRepository = remember { AudioLibraryRepository(context) }
    val storedTracks by libraryRepository.tracks.collectAsState(initial = emptyList())
    val favoriteIds by libraryRepository.favoriteIds.collectAsState(initial = emptySet())
    val playlists by libraryRepository.playlists.collectAsState(initial = emptyList())
    val queue by playerManager.queue.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.TITLE) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var isQueueExpanded by remember { mutableStateOf(false) }
    var isPlaylistsExpanded by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var playlistTrackToAdd by remember { mutableStateOf<AudioTrack?>(null) }
    val selectedPlaylistTracks by libraryRepository
        .observePlaylistTracks(selectedPlaylistId ?: -1L)
        .collectAsState(initial = emptyList())

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            isLoading = true
            coroutineScope.launch {
                libraryRepository.scanAndStore(scanner)
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            permissionToRequest
        ) == PackageManager.PERMISSION_GRANTED

        hasPermission = granted
        if (granted) {
            isLoading = true
            coroutineScope.launch {
                libraryRepository.scanAndStore(scanner)
                isLoading = false
            }
        } else {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    DisposableEffect(Unit) {
        onDispose { libraryRepository.close() }
    }

    val playbackState by playerManager.playbackState.collectAsState()

    // Poll position periodically while playing
    LaunchedEffect(playbackState.isPlaying) {
        while (playbackState.isPlaying) {
            playerManager.updatePosition()
            delay(500)
        }
    }

    val filteredTracks = remember(storedTracks, searchQuery, sortOption) {
        val filtered = if (searchQuery.isBlank()) {
            storedTracks
        } else {
            storedTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortOption) {
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SortOption.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☀️ SunPlayer", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { isPlaylistsExpanded = true }) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists")
                    }
                    IconButton(onClick = { isSortMenuExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Title") },
                            onClick = { sortOption = SortOption.TITLE; isSortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Artist") },
                            onClick = { sortOption = SortOption.ARTIST; isSortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Album") },
                            onClick = { sortOption = SortOption.ALBUM; isSortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Duration") },
                            onClick = { sortOption = SortOption.DURATION; isSortMenuExpanded = false }
                        )
                    }

                    IconButton(onClick = {
                        if (hasPermission) {
                            isLoading = true
                            coroutineScope.launch {
                                libraryRepository.scanAndStore(scanner)
                                isLoading = false
                            }
                        } else {
                            permissionLauncher.launch(permissionToRequest)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (playbackState.currentTrack != null) {
                NowPlayingBottomBar(
                    track = playbackState.currentTrack!!,
                    isPlaying = playbackState.isPlaying,
                    currentPosMs = playbackState.currentPositionMs,
                    durationMs = playbackState.durationMs,
                    onTogglePlayPause = { playerManager.togglePlayPause() },
                    onNext = { playerManager.playNext() },
                    onPrev = { playerManager.playPrevious() },
                    onOpenExpanded = { isNowPlayingExpanded = true }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search songs, artists, albums...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Header summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTracks.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sorted by ${sortOption.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                !hasPermission -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Audio permission is required to read music.")
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { permissionLauncher.launch(permissionToRequest) }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
                filteredTracks.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching songs found" else "No audio files found on your device",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isSelected = playbackState.currentTrack?.id == track.id
                            TrackItem(
                                track = track,
                                isSelected = isSelected,
                                isFavorite = track.id in favoriteIds,
                                onClick = {
                                    playerManager.setPlaylist(filteredTracks, filteredTracks.indexOf(track))
                                },
                                onAddNext = { playerManager.addToNext(track) },
                                onAddToEnd = { playerManager.addToEnd(track) },
                                onAddToPlaylist = { playlistTrackToAdd = track },
                                onToggleFavorite = {
                                    coroutineScope.launch {
                                        libraryRepository.setFavorite(track.id, track.id !in favoriteIds)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Now Playing Dialog Sheet
    if (isNowPlayingExpanded && playbackState.currentTrack != null) {
        NowPlayingExpandedDialog(
            track = playbackState.currentTrack!!,
            isPlaying = playbackState.isPlaying,
            currentPosMs = playbackState.currentPositionMs,
            durationMs = playbackState.durationMs,
            isShuffle = playbackState.isShuffle,
            repeatMode = playbackState.repeatMode,
            onTogglePlayPause = { playerManager.togglePlayPause() },
            onNext = { playerManager.playNext() },
            onPrev = { playerManager.playPrevious() },
            onSeek = { playerManager.seekTo(it) },
            onToggleShuffle = { playerManager.toggleShuffle() },
            onToggleRepeat = { playerManager.toggleRepeat() },
            onOpenQueue = { isQueueExpanded = true },
            onDismiss = { isNowPlayingExpanded = false }
        )
    }

    if (isQueueExpanded) {
        QueueDialog(
            queue = queue,
            currentTrackId = playbackState.currentTrack?.id,
            onMoveUp = { playerManager.moveInQueue(it, it - 1) },
            onMoveDown = { playerManager.moveInQueue(it, it + 1) },
            onRemove = { playerManager.removeFromQueue(it) },
            onClear = { playerManager.clearQueue() },
            onDismiss = { isQueueExpanded = false }
        )
    }

    if (isPlaylistsExpanded) {
        PlaylistDialog(
            playlists = playlists,
            allTracks = storedTracks,
            selectedPlaylistId = selectedPlaylistId,
            selectedPlaylistTracks = selectedPlaylistTracks,
            onSelectPlaylist = { selectedPlaylistId = it },
            onBack = { selectedPlaylistId = null },
            onCreatePlaylist = { name ->
                coroutineScope.launch { selectedPlaylistId = libraryRepository.createPlaylist(name) }
            },
            onRenamePlaylist = { id, name ->
                coroutineScope.launch { libraryRepository.renamePlaylist(id, name) }
            },
            onDeletePlaylist = { id ->
                coroutineScope.launch {
                    libraryRepository.deletePlaylist(id)
                    selectedPlaylistId = null
                }
            },
            onAddTrack = { id, trackId ->
                coroutineScope.launch { libraryRepository.addTrackToPlaylist(id, trackId) }
            },
            onRemoveTrack = { id, trackId ->
                coroutineScope.launch { libraryRepository.removeTrackFromPlaylist(id, trackId) }
            },
            onMoveTrack = { id, trackId, position ->
                coroutineScope.launch { libraryRepository.moveTrackInPlaylist(id, trackId, position) }
            },
            onPlay = { tracks, shuffle ->
                val orderedTracks = if (shuffle) tracks.shuffled() else tracks
                playerManager.setPlaylist(orderedTracks)
                isPlaylistsExpanded = false
            },
            onDismiss = {
                isPlaylistsExpanded = false
                selectedPlaylistId = null
            }
        )
    }

    playlistTrackToAdd?.let { track ->
        PlaylistPickerDialog(
            playlists = playlists,
            track = track,
            onSelect = { playlistId ->
                coroutineScope.launch { libraryRepository.addTrackToPlaylist(playlistId, track.id) }
                playlistTrackToAdd = null
            },
            onCreateAndAdd = { name ->
                coroutineScope.launch {
                    val playlistId = libraryRepository.createPlaylist(name)
                    libraryRepository.addTrackToPlaylist(playlistId, track.id)
                }
                playlistTrackToAdd = null
            },
            onDismiss = { playlistTrackToAdd = null }
        )
    }
}

@Composable
fun TrackItem(
    track: AudioTrack,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onAddNext: () -> Unit,
    onAddToEnd: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var isQueueMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Equalizer else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = track.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { isQueueMenuExpanded = true }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue actions")
                }
                DropdownMenu(
                    expanded = isQueueMenuExpanded,
                    onDismissRequest = { isQueueMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        onClick = {
                            onAddNext()
                            isQueueMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        onClick = {
                            onAddToEnd()
                            isQueueMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        onClick = {
                            onAddToPlaylist()
                            isQueueMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NowPlayingBottomBar(
    track: AudioTrack,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenExpanded: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenExpanded() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column {
            val progress = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrev) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }
                    FilledIconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingExpandedDialog(
    track: AudioTrack,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Art Placeholder
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track details
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.artist} — ${track.album}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Seek slider
            var sliderPos by remember(currentPosMs) { mutableFloatStateOf(currentPosMs.toFloat()) }
            Slider(
                value = sliderPos,
                onValueChange = { sliderPos = it },
                onValueChangeFinished = { onSeek(sliderPos.toLong()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSec = (currentPosMs / 1000).coerceAtLeast(0)
                val totalSec = (durationMs / 1000).coerceAtLeast(0)
                Text(
                    text = "%d:%02d".format(currentSec / 60, currentSec % 60),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%d:%02d".format(totalSec / 60, totalSec % 60),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = onToggleRepeat) {
                    val icon = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(imageVector = icon, contentDescription = "Repeat", tint = tint)
                }
            }

            IconButton(onClick = onOpenQueue) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Open queue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueDialog(
    queue: List<AudioTrack>,
    currentTrackId: Long?,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Queue", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClear, enabled = queue.size > 1) {
                    Text("Clear upcoming")
                }
            }

            if (queue.isEmpty()) {
                Text(
                    text = "The queue is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(queue, key = { _, track -> track.id }) { index, track ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (track.id == currentTrackId) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onMoveUp(index) }, enabled = index > 0) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(onClick = { onMoveDown(index) }, enabled = index < queue.lastIndex) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                            }
                            IconButton(
                                onClick = { onRemove(index) },
                                enabled = track.id != currentTrackId
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from queue")
                            }
                        }
                    }
                }
            }
        }
    }
}
