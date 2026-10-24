package com.music.bitchord.data.sources

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KnownTrackDurationsTest {
    @Before fun reset() = KnownTrackDurations.clear()
    @Test fun invalidMetadataCannotReplaceAValidDuration() {
        KnownTrackDurations.put("a", 204)
        KnownTrackDurations.put("a", 0)
        KnownTrackDurations.put("a", 100_000)
        assertEquals(204, KnownTrackDurations.get("a"))
    }
    @Test fun leastRecentlyUsedEntriesAreEvicted() {
        repeat(512) { KnownTrackDurations.put("$it", 120) }
        KnownTrackDurations.get("0")
        KnownTrackDurations.put("512", 180)
        assertEquals(120, KnownTrackDurations.get("0"))
        assertNull(KnownTrackDurations.get("1"))
        assertEquals(180, KnownTrackDurations.get("512"))
    }
}
