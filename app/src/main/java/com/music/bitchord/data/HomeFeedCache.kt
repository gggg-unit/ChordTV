package com.music.bitchord.data

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Small, disposable snapshots. No credentials or playback URLs are persisted. */
internal class HomeFeedCache(private val directory: File, private val now: () -> Long = System::currentTimeMillis) {
    companion object {
        const val MAX_BYTES = 512 * 1024
        const val MAX_AGE_MS = 6 * 60 * 60 * 1000L
        private val lock = Mutex()
        private val json = Json { ignoreUnknownKeys = true }
    }

    private fun file(identity: String): File {
        val key = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$key.json")
    }

    suspend fun read(identity: String): List<HomeShelf>? = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                val file = file(identity)
                if (!file.exists() || file.length() !in 1..MAX_BYTES.toLong()) return@withLock null
                val snapshot = json.decodeFromString<Snapshot>(file.readText())
                if (snapshot.version != 1 || now() - snapshot.savedAt !in 0..MAX_AGE_MS) return@withLock null
                if (snapshot.shelves.size > 12 || snapshot.shelves.any { it.items.size > 24 }) return@withLock null
                snapshot.shelves.map { it.toShelf() }.takeIf { it.isNotEmpty() }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { null }
        }
    }

    suspend fun write(identity: String, shelves: List<HomeShelf>) = withContext(Dispatchers.IO) {
        lock.withLock {
            var temporary: File? = null
            try {
                if (shelves.isEmpty()) return@withLock
                directory.mkdirs()
                val snapshot = Snapshot(savedAt = now(), shelves = shelves.take(12).map(CachedShelf::from))
                val bytes = json.encodeToString(snapshot).toByteArray(Charsets.UTF_8)
                if (bytes.size > MAX_BYTES) return@withLock
                val destination = file(identity)
                temporary = File.createTempFile("home-", ".tmp", directory)
                temporary.writeBytes(bytes)
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                directory.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() }
                    ?.drop(8)?.forEach { it.delete() }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { /* A cache failure must never block the feed. */
            } finally { temporary?.delete() }
        }
    }

    @Serializable private data class Snapshot(val version: Int = 1, val savedAt: Long, val shelves: List<CachedShelf>)
    @Serializable private data class CachedShelf(
        val title: String, val subtitle: String, val moreBrowseId: String?, val moreParams: String?, val items: List<CachedItem>,
    ) {
        fun toShelf() = HomeShelf(title, items.map { it.toItem() }, subtitle, moreBrowseId, moreParams)
        companion object {
            fun from(shelf: HomeShelf) = CachedShelf(shelf.title.take(512), shelf.subtitle.take(1024),
                shelf.moreBrowseId?.take(2048), shelf.moreParams?.take(2048), shelf.items.take(24).map(CachedItem::from))
        }
    }
    @Serializable private data class CachedItem(
        val title: String, val subtitle: String, val thumbnailUrl: String?, val videoId: String?, val browseId: String?,
    ) {
        fun toItem() = ShelfItem(title, subtitle, thumbnailUrl, videoId, browseId)
        companion object {
            fun from(item: ShelfItem) = CachedItem(item.title.take(512), item.subtitle.take(1024),
                item.thumbnailUrl?.take(2048), item.videoId?.take(256), item.browseId?.take(2048))
        }
    }
}
