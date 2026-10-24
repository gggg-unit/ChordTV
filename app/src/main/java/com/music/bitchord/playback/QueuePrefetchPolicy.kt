package com.music.bitchord.playback

internal fun shouldPrefetchAudioBytes(
    substitutable: Boolean,
    warmed: Boolean,
    pinnedToYouTube: Boolean,
    lossless: Boolean,
    tv: Boolean,
): Boolean = !pinnedToYouTube && (!substitutable || warmed) && !(tv && lossless)
