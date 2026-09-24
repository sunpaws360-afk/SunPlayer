package com.rebecca.sunplayer.model

import android.net.Uri

data class AudioTrack(
    val uri: Uri,
    val title: String,
    val artist: String
)
