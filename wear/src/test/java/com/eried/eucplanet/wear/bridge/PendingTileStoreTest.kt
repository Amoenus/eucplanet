package com.eried.eucplanet.wear.bridge

import com.eried.eucplanet.hud.protocol.WatchMapTileKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingTileStoreTest {
    private val key = WatchMapTileKey("street", 3, 1, 2)
    private val otherKey = WatchMapTileKey("street", 3, 2, 2)

    @Test
    fun viewportRetentionRejectsLateHiddenAssets() {
        val store = PendingTileStore<String>()
        store.retain("phone-a", listOf(key))
        assertTrue(store.offer("phone-a", key, 1L, "first"))

        store.retain("phone-a", listOf(otherKey))
        assertFalse(store.contains(key))
        assertFalse(store.offer("phone-a", key, 2L, "late"))

        store.retain("phone-a", listOf(key))
        assertTrue(store.offer("phone-a", key, 2L, "fresh"))
    }

    @Test
    fun hideAndShowCreatesNewIdentityTicket() {
        val store = PendingTileStore<String>()
        store.retain("phone-a", listOf(key))
        assertTrue(store.offer("phone-a", key, 1L, "old"))
        val old = store[key]

        store.retain("phone-a", emptyList())
        assertFalse(store.contains(key))
        assertFalse(store.offer("phone-a", key, 1L, "hidden"))

        store.retain("phone-a", listOf(key))
        assertTrue(store.offer("phone-a", key, 1L, "new"))
        val current = store[key]
        assertFalse(store.removeIfCurrent(old!!))
        assertSame(current, store[key])
        assertTrue(store.removeIfCurrent(current!!))
    }

    @Test
    fun newerGenerationReplacesOlderTicket() {
        val store = PendingTileStore<String>()
        store.retain("phone-a", listOf(key))
        assertTrue(store.offer("phone-a", key, 1L, "old"))
        val old = store[key]
        assertFalse(store.offer("phone-a", key, 0L, "late"))
        assertTrue(store.offer("phone-a", key, 2L, "new"))
        val current = store[key]
        assertFalse(store.removeIfCurrent(old!!))
        assertSame(current, store[key])
        assertTrue(store.removeIfCurrent(current!!))
    }

    @Test
    fun sourceChangeRejectsOldOffersAndCompletions() {
        val store = PendingTileStore<String>()
        store.retain("phone-a", listOf(key))
        assertTrue(store.offer("phone-a", key, 1L, "old"))
        val old = store[key]

        store.retain("phone-b", listOf(key))
        assertFalse(store.offer("phone-a", key, 1L, "late"))
        assertTrue(store.offer("phone-b", key, 1L, "new"))
        assertFalse(store.removeIfCurrent(old!!))
        assertTrue(store.contains(key))
    }

    @Test
    fun longViewportSequenceRetainsOnlyCurrentKeys() {
        val store = PendingTileStore<String>()
        repeat(1_000) { index ->
            val current = WatchMapTileKey("street", 3, index, 0)
            store.retain("phone-a", listOf(current))
            assertTrue(store.offer("phone-a", current, index.toLong(), index.toString()))
            if (index > 0) {
                val previous = WatchMapTileKey("street", 3, index - 1, 0)
                assertFalse(store.contains(previous))
            }
        }
    }
}
