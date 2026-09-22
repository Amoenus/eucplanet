package com.eried.eucplanet.wear.bridge

import com.eried.eucplanet.hud.protocol.WatchMapTileKey

internal class PendingTileStore<T> {
    class Entry<T>(
        val sourceNodeId: String,
        val key: WatchMapTileKey,
        val deliveryGeneration: Long,
        val value: T,
    )

    private var sourceNodeId: String? = null
    private var visibleKeys: Set<WatchMapTileKey> = emptySet()
    private val entries = mutableMapOf<WatchMapTileKey, Entry<T>>()

    fun retain(sourceNodeId: String?, visibleKeys: Collection<WatchMapTileKey>) {
        val nextSource = sourceNodeId?.takeIf { it.isNotBlank() }
        val nextVisible = visibleKeys.toSet()
        if (nextSource != this.sourceNodeId) entries.clear()
        this.sourceNodeId = nextSource
        this.visibleKeys = nextVisible
        if (nextSource == null || nextVisible.isEmpty()) {
            entries.clear()
        } else {
            entries.keys.retainAll(nextVisible)
        }
    }

    fun offer(
        sourceNodeId: String,
        key: WatchMapTileKey,
        deliveryGeneration: Long,
        value: T,
    ): Boolean {
        if (sourceNodeId != this.sourceNodeId || key !in visibleKeys || deliveryGeneration < 0L) {
            return false
        }
        val current = entries[key]
        if (current != null && deliveryGeneration < current.deliveryGeneration) return false
        entries[key] = Entry(sourceNodeId, key, deliveryGeneration, value)
        return true
    }

    operator fun get(key: WatchMapTileKey): Entry<T>? = entries[key]

    operator fun contains(key: WatchMapTileKey): Boolean = key in entries

    fun removeIfCurrent(entry: Entry<T>): Boolean {
        if (entries[entry.key] !== entry) return false
        entries.remove(entry.key)
        return true
    }
}
