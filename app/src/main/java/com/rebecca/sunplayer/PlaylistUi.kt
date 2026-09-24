package com.rebecca.sunplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rebecca.sunplayer.data.PlaylistSummary
import com.rebecca.sunplayer.model.AudioTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDialog(
    playlists: List<PlaylistSummary>,
    allTracks: List<AudioTrack>,
    selectedPlaylistId: Long?,
    selectedPlaylistTracks: List<AudioTrack>,
    onSelectPlaylist: (Long) -> Unit,
    onBack: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddTrack: (Long, Long) -> Unit,
    onRemoveTrack: (Long, Long) -> Unit,
    onMoveTrack: (Long, Long, Int) -> Unit,
    onPlay: (List<AudioTrack>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showAddTracks by remember { mutableStateOf(false) }
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (selectedPlaylist == null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Playlists", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create playlist")
                    }
                }
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onSelectPlaylist(playlist.id) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${playlist.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete playlist")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to playlists")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedPlaylist.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${selectedPlaylist.trackCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showRename = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename playlist")
                    }
                    IconButton(onClick = { onDeletePlaylist(selectedPlaylist.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete playlist")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onPlay(selectedPlaylistTracks, false) },
                        enabled = selectedPlaylistTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = { onPlay(selectedPlaylistTracks, true) },
                        enabled = selectedPlaylistTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Shuffle")
                    }
                    IconButton(onClick = { showAddTracks = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add tracks")
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    itemsIndexed(selectedPlaylistTracks, key = { _, track -> track.id }) { index, track ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onMoveTrack(selectedPlaylist.id, track.id, index - 1) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = { onMoveTrack(selectedPlaylist.id, track.id, index + 1) },
                                enabled = index < selectedPlaylistTracks.lastIndex
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                            }
                            IconButton(onClick = { onRemoveTrack(selectedPlaylist.id, track.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove track")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = "Create playlist",
            confirmLabel = "Create",
            onConfirm = { onCreatePlaylist(it); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }
    if (showRename && selectedPlaylist != null) {
        PlaylistNameDialog(
            title = "Rename playlist",
            initialName = selectedPlaylist.name,
            confirmLabel = "Rename",
            onConfirm = { onRenamePlaylist(selectedPlaylist.id, it); showRename = false },
            onDismiss = { showRename = false }
        )
    }
    if (showAddTracks && selectedPlaylist != null) {
        AddTracksDialog(
            tracks = allTracks,
            onAdd = { onAddTrack(selectedPlaylist.id, it) },
            onDismiss = { showAddTracks = false }
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String = "",
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddTracksDialog(
    tracks: List<AudioTrack>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add tracks") },
        text = {
            LazyColumn {
                items(tracks, key = { it.id }) { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onAdd(track.id) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add track")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistSummary>,
    track: AudioTrack,
    onSelect: (Long) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${track.title} to playlist") },
        text = {
            Column {
                playlists.forEach { playlist ->
                    TextButton(onClick = { onSelect(playlist.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(playlist.name)
                    }
                }
                TextButton(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create playlist")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (showCreate) {
        PlaylistNameDialog(
            title = "Create playlist",
            confirmLabel = "Create",
            onConfirm = { onCreateAndAdd(it); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }
}
