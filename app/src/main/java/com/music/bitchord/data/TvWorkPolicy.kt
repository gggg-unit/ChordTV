package com.music.bitchord.data

/** Opted into by the TV initializer; phone startup and playback keep their policy. */
object TvWorkPolicy {
    @Volatile var enabled: Boolean = false
}
