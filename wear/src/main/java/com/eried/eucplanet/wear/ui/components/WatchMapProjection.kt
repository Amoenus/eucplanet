package com.eried.eucplanet.wear.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.eried.eucplanet.hud.protocol.WebMercator
import com.eried.eucplanet.hud.protocol.WatchMapPoint
import com.eried.eucplanet.hud.protocol.WatchMapRoute
import com.eried.eucplanet.hud.protocol.WatchMapTileKey
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal data class RenderTile(
    val key: WatchMapTileKey,
    val left: Double,
    val top: Double,
    val size: Double,
)

internal data class WorldPoint(val x: Double, val y: Double)

internal fun projectRoute(route: WatchMapRoute?, zoom: Int): List<WorldPoint> {
    if (route == null) return emptyList()
    return buildList(route.coordinates.size / 2) {
        for (index in route.coordinates.indices step 2) {
            add(
                WorldPoint(
                    WebMercator.tileX(route.coordinates[index + 1], zoom),
                    WebMercator.tileY(route.coordinates[index], zoom),
                ),
            )
        }
    }
}

internal class WatchMapProjection(
    center: WatchMapPoint,
    private val zoom: Int,
    private val tileSize: Double,
    private val rotation: Float,
    private val viewport: IntSize,
) {
    private val centerX = WebMercator.tileX(center.lon, zoom)
    private val centerY = WebMercator.tileY(center.lat, zoom)
    private val pivotX = viewport.width / 2.0
    private val pivotY = viewport.height / 2.0

    fun visibleTiles(layerId: String): List<RenderTile> {
        val halfWidth = viewport.width / 2.0
        val halfHeight = viewport.height / 2.0
        val inverseCorners = listOf(
            rotatePoint(-halfWidth, -halfHeight, -rotation),
            rotatePoint(halfWidth, -halfHeight, -rotation),
            rotatePoint(halfWidth, halfHeight, -rotation),
            rotatePoint(-halfWidth, halfHeight, -rotation),
        )
        val minX = floor(centerX + inverseCorners.minOf { it.first } / tileSize).toInt()
        val maxX = floor(centerX + inverseCorners.maxOf { it.first } / tileSize).toInt()
        val minY = floor(centerY + inverseCorners.minOf { it.second } / tileSize).toInt()
        val maxY = floor(centerY + inverseCorners.maxOf { it.second } / tileSize).toInt()
        val side = 1 shl zoom
        val result = mutableListOf<Pair<Double, RenderTile>>()
        for (unwrappedY in minY..maxY) {
            if (unwrappedY !in 0 until side) continue
            for (unwrappedX in minX..maxX) {
                val left = pivotX + (unwrappedX - centerX) * tileSize
                val top = pivotY + (unwrappedY - centerY) * tileSize
                if (!rotatedTileIntersectsViewport(left, top, tileSize)) continue
                val key = WatchMapTileKey(layerId, zoom, WebMercator.wrapX(unwrappedX, zoom), unwrappedY)
                val distance = (unwrappedX + 0.5 - centerX).let { it * it } +
                    (unwrappedY + 0.5 - centerY).let { it * it }
                result += distance to RenderTile(key, left, top, tileSize)
            }
        }
        return result.sortedBy { it.first }.map { it.second }.distinctBy { it.key }
    }

    fun screenPosition(point: WatchMapPoint): Offset {
        val x = WebMercator.nearestWorldX(WebMercator.tileX(point.lon, zoom), centerX, zoom)
        return finalScreenPoint(x, WebMercator.tileY(point.lat, zoom))
    }

    fun forEachVisibleRouteSegment(
        projected: List<WorldPoint>,
        emit: (Offset, Offset) -> Unit,
    ) {
        if (projected.size < 2) return
        var previousWorldX = WebMercator.nearestWorldX(projected.first().x, centerX, zoom)
        var previous = finalScreenPoint(previousWorldX, projected.first().y)
        for (index in 1 until projected.size) {
            val point = projected[index]
            val worldX = WebMercator.nearestWorldX(point.x, previousWorldX, zoom)
            val next = finalScreenPoint(worldX, point.y)
            if (segmentMayIntersect(previous, next)) emit(previous, next)
            previousWorldX = worldX
            previous = next
        }
    }

    private fun finalScreenPoint(x: Double, y: Double): Offset {
        val delta = rotatePoint((x - centerX) * tileSize, (y - centerY) * tileSize, rotation)
        return Offset((pivotX + delta.first).toFloat(), (pivotY + delta.second).toFloat())
    }

    private fun segmentMayIntersect(a: Offset, b: Offset): Boolean {
        val margin = 10f
        return maxOf(a.x, b.x) >= -margin && minOf(a.x, b.x) <= viewport.width + margin &&
            maxOf(a.y, b.y) >= -margin && minOf(a.y, b.y) <= viewport.height + margin
    }

    private fun rotatedTileIntersectsViewport(left: Double, top: Double, size: Double): Boolean {
        val tile = listOf(
            rotateAround(left, top, size),
            rotateAround(left + size, top, size),
            rotateAround(left + size, top + size, size),
            rotateAround(left, top + size, size),
        )
        val screen = listOf(
            0.0 to 0.0,
            viewport.width.toDouble() to 0.0,
            viewport.width.toDouble() to viewport.height.toDouble(),
            0.0 to viewport.height.toDouble(),
        )
        return polygonsIntersect(tile, screen)
    }

    private fun rotateAround(x: Double, y: Double, size: Double): Pair<Double, Double> {
        val rotated = rotatePoint(x - pivotX, y - pivotY, rotation)
        return pivotX + rotated.first to pivotY + rotated.second
    }

    private fun polygonsIntersect(
        first: List<Pair<Double, Double>>,
        second: List<Pair<Double, Double>>,
    ): Boolean {
        fun separated(axisX: Double, axisY: Double): Boolean {
            fun projection(points: List<Pair<Double, Double>>): Pair<Double, Double> {
                val values = points.map { it.first * axisX + it.second * axisY }
                return values.min() to values.max()
            }
            val a = projection(first)
            val b = projection(second)
            return a.second < b.first || b.second < a.first
        }
        for (polygon in listOf(first, second)) {
            polygon.indices.forEach { index ->
                val a = polygon[index]
                val b = polygon[(index + 1) % polygon.size]
                if (separated(-(b.second - a.second), b.first - a.first)) return false
            }
        }
        return true
    }

    private fun rotatePoint(x: Double, y: Double, degrees: Float): Pair<Double, Double> {
        val radians = Math.toRadians(degrees.toDouble())
        val cosine = cos(radians)
        val sine = sin(radians)
        return x * cosine - y * sine to x * sine + y * cosine
    }
}
