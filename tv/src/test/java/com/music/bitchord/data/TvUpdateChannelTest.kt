package com.music.bitchord.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvUpdateChannelTest {
    @Test fun benchmarkCannotOfferAReplacementFromAnotherPackageOrSigningChannel() {
        assertNull(tvReleaseApiUrl("benchmark"))
    }

    @Test fun tvDebugAndReleaseUseTheUsersFork() {
        val fork = "https://api.github.com/repos/gggg-unit/ChordTV/releases/latest"
        assertEquals(fork, tvReleaseApiUrl("debug"))
        assertEquals(fork, tvReleaseApiUrl("release"))
    }
}
