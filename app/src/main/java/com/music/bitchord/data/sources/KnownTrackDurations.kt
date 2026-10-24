package com.music.bitchord.data.sources

/** Catalogue durations only: decoding a substitute must not overwrite the original timeline. */
internal object KnownTrackDurations {
    private val durations = object : LinkedHashMap<String, Int>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?) = size > 512
    }
    @Synchronized fun get(id: String): Int? = durations[id]
    @Synchronized fun put(id: String, seconds: Int) {
        if (id.isNotBlank() && seconds in 1..86_400) durations[id] = seconds
    }
    @Synchronized internal fun clear() = durations.clear()
}
