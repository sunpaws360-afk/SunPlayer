package com.rebecca.sunplayer.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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

@OptIn(UnstableApi::class)
class AudioPlayerManager(private val context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val originalPlaylist = mutableListOf<AudioTrack>()
    private val activePlaylist = mutableListOf<AudioTrack>()
    private var currentIndex: Int = -1

    private var isShuffleEnabled: Boolean = false
    private var currentRepeatMode: RepeatMode = RepeatMode.OFF

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onPlaybackStateChanged(state: Int) {
                updateState()
                if (state == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
            }
        })
    }

    private fun handleTrackEnded() {
        when (currentRepeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                player.play()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                if (currentIndex < activePlaylist.size - 1) {
                    playNext()
                }
            }
        }
    }

    fun setPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        originalPlaylist.clear()
        originalPlaylist.addAll(tracks)

        activePlaylist.clear()
        if (isShuffleEnabled) {
            val selectedTrack = if (startIndex in tracks.indices) tracks[startIndex] else null
            val shuffled = tracks.shuffled().toMutableList()
            if (selectedTrack != null) {
                shuffled.remove(selectedTrack)
                shuffled.add(0, selectedTrack)
            }
            activePlaylist.addAll(shuffled)
            playTrackAtIndex(0)
        } else {
            activePlaylist.addAll(tracks)
            if (tracks.isNotEmpty() && startIndex in tracks.indices) {
                playTrackAtIndex(startIndex)
            }
        }
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        val currentTrack = if (currentIndex in activePlaylist.indices) activePlaylist[currentIndex] else null

        activePlaylist.clear()
        if (isShuffleEnabled) {
            val shuffled = originalPlaylist.shuffled().toMutableList()
            if (currentTrack != null) {
                shuffled.remove(currentTrack)
                shuffled.add(0, currentTrack)
                currentIndex = 0
            }
            activePlaylist.addAll(shuffled)
        } else {
            activePlaylist.addAll(originalPlaylist)
            currentIndex = if (currentTrack != null) activePlaylist.indexOfFirst { it.id == currentTrack.id } else -1
        }
        updateState()
    }

    fun toggleRepeat() {
        currentRepeatMode = when (currentRepeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        updateState()
    }

    fun playTrack(track: AudioTrack) {
        val index = activePlaylist.indexOfFirst { it.id == track.id }
        if (index != -1) {
            playTrackAtIndex(index)
        } else {
            activePlaylist.add(track)
            originalPlaylist.add(track)
            playTrackAtIndex(activePlaylist.size - 1)
        }
    }

    private fun playTrackAtIndex(index: Int) {
        if (index !in activePlaylist.indices) return
        currentIndex = index
        val track = activePlaylist[index]

        val mediaItem = MediaItem.fromUri(track.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        updateState()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && currentIndex in activePlaylist.indices) {
                playTrackAtIndex(currentIndex)
            } else {
                player.play()
            }
        }
        updateState()
    }

    fun playNext() {
        if (activePlaylist.isEmpty()) return
        val nextIndex = (currentIndex + 1) % activePlaylist.size
        playTrackAtIndex(nextIndex)
    }

    fun playPrevious() {
        if (activePlaylist.isEmpty()) return
        val prevIndex = if (currentIndex - 1 < 0) activePlaylist.size - 1 else currentIndex - 1
        playTrackAtIndex(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        updateState()
    }

    fun updatePosition() {
        if (player.isPlaying) {
            _playbackState.value = _playbackState.value.copy(
                currentPositionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }
    }

    private fun updateState() {
        val track = if (currentIndex in activePlaylist.indices) activePlaylist[currentIndex] else null
        _playbackState.value = PlaybackState(
            currentTrack = track,
            isPlaying = player.isPlaying,
            currentPositionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            isShuffle = isShuffleEnabled,
            repeatMode = currentRepeatMode
        )
    }

    fun release() {
        player.release()
    }
}
