package com.rebecca.sunplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.rebecca.sunplayer.model.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

class AudioPlayerManager(context: Context) {
    private val applicationContext = context.applicationContext
    private val tracksById = mutableMapOf<Long, AudioTrack>()
    private val originalPlaylist = mutableListOf<AudioTrack>()
    private var pendingStartIndex = 0
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    private val _queue = MutableStateFlow<List<AudioTrack>>(emptyList())
    val queue: StateFlow<List<AudioTrack>> = _queue.asStateFlow()

    private val controllerFuture = MediaController.Builder(
        applicationContext,
        SessionToken(applicationContext, ComponentName(applicationContext, PlaybackService::class.java))
    ).buildAsync()

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()

        override fun onPlaybackStateChanged(playbackState: Int) = updateState()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = updateState()

        override fun onRepeatModeChanged(repeatMode: Int) = updateState()
    }

    init {
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get()
                controller?.addListener(controllerListener)
                if (originalPlaylist.isNotEmpty()) {
                    applyPlaylist(pendingStartIndex, startPlayback = false)
                } else {
                    updateState()
                }
            } catch (error: Exception) {
                controller = null
                updateState()
            }
        }, MoreExecutors.directExecutor())
    }

    fun setPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        originalPlaylist.clear()
        originalPlaylist.addAll(tracks)
        tracksById.clear()
        tracksById.putAll(tracks.associateBy { it.id })
        publishQueue()
        pendingStartIndex = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        if (tracks.isNotEmpty()) {
            applyPlaylist(pendingStartIndex, startPlayback = true)
        }
    }

    fun toggleShuffle() {
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
            updateState()
        }
    }

    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            updateState()
        }
    }

    fun playTrack(track: AudioTrack) {
        val index = originalPlaylist.indexOfFirst { it.id == track.id }
        if (index >= 0) {
            applyPlaylist(index, startPlayback = true)
        } else {
            originalPlaylist.add(track)
            tracksById[track.id] = track
            publishQueue()
            applyPlaylist(originalPlaylist.lastIndex, startPlayback = true)
        }
    }

    fun addToNext(track: AudioTrack) {
        val mediaController = controller ?: return
        if (mediaController.currentMediaItem?.mediaId == track.id.toString()) return
        val existingIndex = originalPlaylist.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            mediaController.removeMediaItem(existingIndex)
            originalPlaylist.removeAt(existingIndex)
        }
        val nextIndex = (mediaController.currentMediaItemIndex + 1).coerceAtMost(originalPlaylist.size)
        originalPlaylist.add(nextIndex, track)
        tracksById[track.id] = track
        mediaController.addMediaItem(nextIndex, mediaItem(track))
        publishQueue()
    }

    fun addToEnd(track: AudioTrack) {
        val mediaController = controller ?: return
        if (mediaController.currentMediaItem?.mediaId == track.id.toString()) return
        val existingIndex = originalPlaylist.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            mediaController.removeMediaItem(existingIndex)
            originalPlaylist.removeAt(existingIndex)
        }
        originalPlaylist.add(track)
        tracksById[track.id] = track
        mediaController.addMediaItem(mediaItem(track))
        publishQueue()
    }

    fun removeFromQueue(index: Int) {
        val mediaController = controller ?: return
        if (index !in originalPlaylist.indices || index == mediaController.currentMediaItemIndex) return
        mediaController.removeMediaItem(index)
        originalPlaylist.removeAt(index)
        publishQueue()
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val mediaController = controller ?: return
        if (fromIndex !in originalPlaylist.indices || toIndex !in originalPlaylist.indices) return
        mediaController.moveMediaItem(fromIndex, toIndex)
        val track = originalPlaylist.removeAt(fromIndex)
        originalPlaylist.add(toIndex, track)
        publishQueue()
    }

    fun clearQueue() {
        val mediaController = controller ?: return
        val currentIndex = mediaController.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return
        mediaController.removeMediaItems(currentIndex + 1, mediaController.mediaItemCount)
        originalPlaylist.subList((currentIndex + 1).coerceAtMost(originalPlaylist.size), originalPlaylist.size).clear()
        publishQueue()
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
            updateState()
        }
    }

    fun playNext() {
        controller?.seekToNextMediaItem()
    }

    fun playPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        updateState()
    }

    fun updatePosition() {
        updateState()
    }

    private fun applyPlaylist(startIndex: Int, startPlayback: Boolean) {
        val mediaController = controller ?: return
        val items = originalPlaylist.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }
        mediaController.setMediaItems(items, startIndex, 0L)
        mediaController.prepare()
        if (startPlayback) mediaController.play()
        updateState()
    }

    private fun mediaItem(track: AudioTrack): MediaItem = MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .build()
        )
        .build()

    private fun publishQueue() {
        _queue.value = originalPlaylist.toList()
    }

    private fun updateState() {
        val mediaController = controller
        val currentTrack = mediaController?.currentMediaItem?.mediaId?.toLongOrNull()?.let(tracksById::get)
        val repeatMode = when (mediaController?.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
        _playbackState.value = PlaybackState(
            currentTrack = currentTrack,
            isPlaying = mediaController?.isPlaying == true,
            currentPositionMs = mediaController?.currentPosition ?: 0L,
            durationMs = (mediaController?.duration ?: 0L).coerceAtLeast(0L),
            isShuffle = mediaController?.shuffleModeEnabled == true,
            repeatMode = repeatMode
        )
    }

    fun release() {
        controller?.removeListener(controllerListener)
        controller?.release()
        controller = null
        if (!controllerFuture.isDone) controllerFuture.cancel(false)
    }
}
