package com.music.bitchord.playback

import org.junit.Assert.*
import org.junit.Test

class QueuePrefetchPolicyTest {
    @Test fun tvLosslessWarmsTheUrlWithoutDownloadingTheFile() {
        assertFalse(shouldPrefetchAudioBytes(true, true, false, true, true))
        assertTrue(shouldPrefetchAudioBytes(true, true, false, false, true))
    }
    @Test fun pinnedOriginalAndUnresolvedSubstitutesNeverGetAlternateBytes() {
        assertFalse(shouldPrefetchAudioBytes(true, true, true, false, true))
        assertFalse(shouldPrefetchAudioBytes(true, false, false, false, true))
    }
    @Test fun phoneAndOrdinaryYouTubeReadAheadKeepTheirExistingBehavior() {
        assertTrue(shouldPrefetchAudioBytes(true, true, false, true, false))
        assertTrue(shouldPrefetchAudioBytes(false, false, false, false, true))
    }
}
