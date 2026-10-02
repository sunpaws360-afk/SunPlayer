package com.rebecca.sunplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rebecca.sunplayer.MainActivity

class PlaybackService : MediaSessionService() {
    companion object {
        private const val CHANNEL_ID = "sunplayer_media"
        private const val CHANNEL_NAME = "SunPlayer playback"
        private const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "com.rebecca.sunplayer.action.PLAY"
        const val ACTION_PAUSE = "com.rebecca.sunplayer.action.PAUSE"
        const val ACTION_NEXT = "com.rebecca.sunplayer.action.NEXT"
        const val ACTION_PREV = "com.rebecca.sunplayer.action.PREV"
        const val ACTION_STOP = "com.rebecca.sunplayer.action.STOP"
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                tryUpdateNotification(isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // Update metadata when track changes
                tryUpdateNotification(player.isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                tryUpdateNotification(player.isPlaying)
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> player.play()
            ACTION_PAUSE -> player.pause()
            ACTION_NEXT -> player.seekToNextMediaItem()
            ACTION_PREV -> player.seekToPreviousMediaItem()
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        notificationManager.cancel(NOTIFICATION_ID)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun tryUpdateNotification(isPlaying: Boolean) {
        val notification = buildNotification(isPlaying)
        if (isPlaying) {
            // When playing, start foreground so Android gives playback higher priority
            startForeground(NOTIFICATION_ID, notification)
        } else {
            // When paused, keep a notification but don't keep service in foreground state
            stopForeground(false)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val controller = mediaSession.controller
        val metadata = controller.currentMediaItem?.mediaMetadata
        val title = metadata?.title?.toString() ?: "SunPlayer"
        val artist = metadata?.artist?.toString() ?: ""

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PAUSE }
            val pi = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pi)
        } else {
            val playIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY }
            val pi = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", pi)
        }

        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPi = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val nextAction = NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextPi)

        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPi = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val prevAction = NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", prevPi)

        val stopIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 5, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)

        val largeIcon = try {
            BitmapFactory.decodeResource(resources, android.R.drawable.ic_media_play)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1) // show play/pause in compact
            )

        largeIcon?.let { builder.setLargeIcon(it) }

        return builder.build()
    }
}
