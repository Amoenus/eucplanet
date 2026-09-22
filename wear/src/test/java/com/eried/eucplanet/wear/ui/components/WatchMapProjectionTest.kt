package com.eried.eucplanet.wear.ui.components

import androidx.compose.ui.unit.IntSize
import com.eried.eucplanet.hud.protocol.WatchMapPoint
import com.eried.eucplanet.hud.protocol.WatchMapRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchMapProjectionTest {
    private fun assertNear(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }

    @Test
    fun screenPositionUsesNativeWorldScaleAndRotation() {
        val projection = WatchMapProjection(
            center = WatchMapPoint(0.0, 0.0),
            zoom = 3,
            tileSize = 256.0,
            rotation = 0f,
            viewport = IntSize(200, 100),
        )
        val point = projection.screenPosition(WatchMapPoint(0.0, 1.0))
        assertNear(100f, projection.screenPosition(WatchMapPoint(0.0, 0.0)).x)
        assertNear(50f, projection.screenPosition(WatchMapPoint(0.0, 0.0)).y)
        assertNear(105.6888889f, point.x)
        assertNear(50f, point.y)

        val rotated = WatchMapProjection(
            WatchMapPoint(0.0, 0.0),
            zoom = 3,
            tileSize = 256.0,
            rotation = 90f,
            viewport = IntSize(200, 100),
        ).screenPosition(WatchMapPoint(0.0, 1.0))
        assertNear(100f, rotated.x)
        assertNear(55.6888889f, rotated.y)

        val overzoomed = WatchMapProjection(
            WatchMapPoint(0.0, 0.0),
            zoom = 3,
            tileSize = 512.0,
            rotation = 0f,
            viewport = IntSize(200, 100),
        ).screenPosition(WatchMapPoint(0.0, 1.0))
        assertNear(111.3777778f, overzoomed.x)
    }

    @Test
    fun datelineUsesNearestWorldAndEmitsOneSegment() {
        val projection = WatchMapProjection(
            WatchMapPoint(0.0, 179.9),
            zoom = 3,
            tileSize = 256.0,
            rotation = 0f,
            viewport = IntSize(200, 100),
        )
        val target = projection.screenPosition(WatchMapPoint(0.0, -179.9))
        assertNear(101.1377778f, target.x)
        assertNear(50f, target.y)

        val route = projectRoute(
            WatchMapRoute(
                navigationSessionId = "session",
                revision = 1L,
                coordinates = doubleArrayOf(0.0, 179.9, 0.0, -179.9),
            ),
            zoom = 3,
        )
        val segments = mutableListOf<Pair<androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset>>()
        projection.forEachVisibleRouteSegment(route) { from, to -> segments += from to to }
        assertEquals(1, segments.size)
        assertNear(100f, segments.single().first.x)
        assertNear(50f, segments.single().first.y)
        assertNear(target.x, segments.single().second.x)
        assertNear(target.y, segments.single().second.y)

        val rotatedTarget = WatchMapProjection(
            WatchMapPoint(0.0, 179.9),
            zoom = 3,
            tileSize = 256.0,
            rotation = 90f,
            viewport = IntSize(200, 100),
        ).screenPosition(WatchMapPoint(0.0, -179.9))
        assertNear(100f, rotatedTarget.x)
        assertNear(51.1377778f, rotatedTarget.y)
    }

    @Test
    fun visibleTilesAreNearestFirstWrappedAndYBounded() {
        val tiles = WatchMapProjection(
            WatchMapPoint(0.0, 0.0),
            zoom = 3,
            tileSize = 256.0,
            rotation = 0f,
            viewport = IntSize(256, 256),
        ).visibleTiles("street")
        assertEquals(
            listOf(3, 4, 3, 4).zip(listOf(3, 3, 4, 4)).map { (x, y) ->
                com.eried.eucplanet.hud.protocol.WatchMapTileKey("street", 3, x, y)
            },
            tiles.map { it.key },
        )
        assertEquals(-128.0, tiles.first().left, 0.0001)
        assertEquals(-128.0, tiles.first().top, 0.0001)

        val wrapped = WatchMapProjection(
            WatchMapPoint(0.0, -179.9),
            zoom = 3,
            tileSize = 256.0,
            rotation = 0f,
            viewport = IntSize(256, 256),
        ).visibleTiles("street").map { it.key.x }
        assertTrue(0 in wrapped)
        assertTrue(7 in wrapped)

        val polar = WatchMapProjection(
            WatchMapPoint(-85.0, 0.0),
            zoom = 3,
            tileSize = 256.0,
            rotation = 0f,
            viewport = IntSize(256, 256),
        ).visibleTiles("street")
        assertTrue(polar.all { it.key.y in 0..7 })
        assertFalse(polar.any { it.key.y !in 0..7 })
    }

    @Test
    fun routeOutsideExpandedViewportAndSinglePointEmitNothing() {
        val projection = WatchMapProjection(
            WatchMapPoint(0.0, 0.0),
            zoom = 3,
            tileSize = 256.0,
            rotation = 0f,
            viewport = IntSize(200, 100),
        )
        val segments = mutableListOf<Any>()
        projection.forEachVisibleRouteSegment(listOf(WorldPoint(0.0, 0.0))) { _, _ -> segments += Unit }
        projection.forEachVisibleRouteSegment(listOf(WorldPoint(0.0, 0.0), WorldPoint(0.0, 1.0))) { _, _ -> segments += Unit }
        assertTrue(segments.isEmpty())
        assertTrue(projectRoute(null, 3).isEmpty())
    }
}
