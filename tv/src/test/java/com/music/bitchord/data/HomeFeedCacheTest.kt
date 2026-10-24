package com.music.bitchord.data

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeFeedCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun shelf(title: String) = HomeShelf(title, listOf(ShelfItem("Song", "Artist", "https://art", "id", null)))

    @Test fun accountsAndProfilesCannotReadEachOthersSnapshots() = runBlocking {
        val cache = HomeFeedCache(temporary.root)
        cache.write("account-a:profile-a:tr", listOf(shelf("A")))
        cache.write("account-b:profile-b:tr", listOf(shelf("B")))
        assertEquals("A", cache.read("account-a:profile-a:tr")!!.single().title)
        assertEquals("B", cache.read("account-b:profile-b:tr")!!.single().title)
        assertNull(cache.read("guest:tr"))
        assertNull(cache.read("account-a:profile-b:tr"))
        assertFalse(temporary.root.listFiles()!!.any { "account" in it.name })
    }

    @Test fun expiredAndFutureDatedSnapshotsAreNotShown() = runBlocking {
        var time = 100L
        val cache = HomeFeedCache(temporary.root) { time }
        cache.write("a", listOf(shelf("A")))
        time += HomeFeedCache.MAX_AGE_MS + 1
        assertNull(cache.read("a"))
        time = 0
        assertNull(cache.read("a"))
    }

    @Test fun corruptAndOversizedFilesFallBackToNetwork() = runBlocking {
        val cache = HomeFeedCache(temporary.root)
        cache.write("a", listOf(shelf("A")))
        val file = temporary.root.listFiles()!!.single()
        file.writeText("{broken")
        assertNull(cache.read("a"))
        file.writeBytes(ByteArray(HomeFeedCache.MAX_BYTES + 1))
        assertNull(cache.read("a"))
    }

    @Test fun newerContentReplacesSameTitleRatherThanAppendingOldItems() = runBlocking {
        val cache = HomeFeedCache(temporary.root)
        cache.write("a", listOf(shelf("Home")))
        val fresh = shelf("Home").copy(items = listOf(ShelfItem("New song", "New artist", null, "new", null)))
        cache.write("a", listOf(fresh))
        assertEquals(listOf(fresh), cache.read("a"))
    }

    @Test fun snapshotsHaveShelfItemAndIdentityBudgets() = runBlocking {
        val cache = HomeFeedCache(temporary.root)
        val shelves = List(20) { shelf("Shelf $it").copy(items = List(40) { shelf("x").items.single() }) }
        cache.write("large", shelves)
        assertEquals(12, cache.read("large")!!.size)
        assertTrue(cache.read("large")!!.all { it.items.size == 24 })
        repeat(12) { cache.write("account-$it", listOf(shelf("A"))) }
        assertTrue(temporary.root.listFiles()!!.size <= 8)
    }

    @Test fun concurrentReadersNeverObservePartialWrites() = runBlocking {
        val cache = HomeFeedCache(temporary.root)
        cache.write("a", listOf(shelf("Initial")))
        (0..15).map { index -> async {
            cache.write("a", listOf(shelf("Version $index")))
            assertEquals(1, cache.read("a")!!.size)
        } }.awaitAll()
        assertFalse(temporary.root.listFiles()!!.any { it.extension == "tmp" })
    }
}
