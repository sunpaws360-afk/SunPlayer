# ProGuard rules for SunPlayer

# Keep Compose internal runtime
-keep class androidx.compose.** { *; }

# Media3 ExoPlayer keep rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep models
-keep class com.rebecca.sunplayer.model.** { *; }
