package com.eried.eucplanet.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.eried.eucplanet.data.model.ADVANCED_DEFAULTS
import com.eried.eucplanet.data.repository.SettingsRepository
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Process-wide raster cache shared by Studio maps and the Wear map publisher. */
internal object MapTileCache {
    private const val DECODED_TILE_COUNT = 64
    private val DEFAULT_ENCODED_BYTES = ADVANCED_DEFAULTS.mapEncodedCacheMiB.toLong() * 1024L * 1024L
    private val DEFAULT_HTTP_BYTES = ADVANCED_DEFAULTS.mapHttpCacheMiB.toLong() * 1024L * 1024L

    private data class DecodedTile(
        val bitmap: Bitmap,
        val image: ImageBitmap,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val decoded = LruCache<String, DecodedTile>(DECODED_TILE_COUNT)
    private val encoded = object : LruCache<String, ByteArray>(DEFAULT_ENCODED_BYTES.toInt()) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }
    private val decodedInFlight = ConcurrentHashMap<String, Deferred<DecodedTile?>>()
    private val pngInFlight = ConcurrentHashMap<String, Deferred<ByteArray?>>()
    private val clientLock = Any()
    private val settingsStarted = AtomicBoolean(false)

    @Volatile
    private var requestedHttpBytes = DEFAULT_HTTP_BYTES
    private var httpCache: MapTileHttpCache? = null

    fun start(settingsRepository: SettingsRepository) {
        if (!settingsStarted.compareAndSet(false, true)) return
        scope.launch {
            settingsRepository.settings
                .map { settings ->
                    settings.advanced.mapEncodedCacheMiB.toLong() * 1024L * 1024L to
                        settings.advanced.mapHttpCacheMiB.toLong() * 1024L * 1024L
                }
                .distinctUntilChanged()
                .collect { (encodedBytes, httpBytes) ->
                    synchronized(encoded) {
                        if (encoded.maxSize() != encodedBytes.toInt()) encoded.resize(encodedBytes.toInt())
                    }
                    synchronized(clientLock) {
                        requestedHttpBytes = httpBytes
                        httpCache?.let { owner ->
                            runCatching { owner.resize(httpBytes) }
                                .onFailure { error ->
                                    Log.w("MapTileCache", "Failed to resize HTTP tile cache", error)
                                }
                        }
                    }
                }
        }
    }

    fun get(url: String): ImageBitmap? = synchronized(decoded) {
        decoded.get(url)?.image
    }

    suspend fun load(context: Context, url: String): Boolean =
        decodedTile(context.applicationContext, url) != null

    suspend fun png(context: Context, url: String): ByteArray? {
        synchronized(encoded) { encoded.get(url) }?.let { return it }
        val tile = decodedTile(context.applicationContext, url) ?: return null

        val candidate = scope.async(start = CoroutineStart.LAZY) {
            withContext(Dispatchers.Default) {
                val bytes = ByteArrayOutputStream().use { output ->
                    if (!tile.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        return@withContext null
                    }
                    output.toByteArray().takeIf { it.isNotEmpty() }
                }
                bytes?.also { png ->
                    synchronized(encoded) { encoded.put(url, png) }
                }
            }
        }
        val shared = pngInFlight.putIfAbsent(url, candidate) ?: candidate.also { deferred ->
            deferred.invokeOnCompletion { pngInFlight.remove(url, deferred) }
            deferred.start()
        }
        if (shared !== candidate) candidate.cancel()
        return shared.await()
    }

    private suspend fun decodedTile(context: Context, url: String): DecodedTile? {
        synchronized(decoded) { decoded.get(url) }?.let { return it }

        val candidate = scope.async(start = CoroutineStart.LAZY) {
            fetchDecoded(context, url)?.also { tile ->
                synchronized(decoded) { decoded.put(url, tile) }
            }
        }
        val shared = decodedInFlight.putIfAbsent(url, candidate) ?: candidate.also { deferred ->
            deferred.invokeOnCompletion { decodedInFlight.remove(url, deferred) }
            deferred.start()
        }
        if (shared !== candidate) candidate.cancel()
        return shared.await()
    }

    private fun fetchDecoded(context: Context, url: String): DecodedTile? {
        val base = fetchBitmap(context, url) ?: return null
        val referenceUrl = url.replace("_Gray_Base/", "_Gray_Reference/")
        val composite = if (referenceUrl != url) {
            val reference = fetchBitmap(context, referenceUrl)
            if (reference != null) {
                val mutable = base.copy(Bitmap.Config.ARGB_8888, true)
                if (mutable != null) {
                    Canvas(mutable).drawBitmap(reference, 0f, 0f, null)
                    base.recycle()
                    reference.recycle()
                    mutable
                } else {
                    reference.recycle()
                    base
                }
            } else {
                base
            }
        } else {
            base
        }
        return DecodedTile(composite, composite.asImageBitmap())
    }

    private fun fetchBitmap(context: Context, url: String): Bitmap? =
        httpOwner(context).fetch(url) { BitmapFactory.decodeStream(it) }

    private fun httpOwner(context: Context): MapTileHttpCache = synchronized(clientLock) {
        httpCache ?: MapTileHttpCache(
            File(context.cacheDir, "map_tiles_http"),
            requestedHttpBytes,
        ).also { httpCache = it }
    }
}
