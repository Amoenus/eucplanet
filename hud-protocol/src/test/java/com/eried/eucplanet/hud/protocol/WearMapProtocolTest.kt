package com.eried.eucplanet.hud.protocol

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearMapProtocolTest {

    @Test
    fun routeCodecRejectsTheWholeInvalidPayload() {
        val valid = WatchMapRoute(
            navigationSessionId = "nav-1",
            revision = 1L,
            coordinates = doubleArrayOf(90.0, -180.0, -90.0, 180.0),
        )
        assertNotNull(WatchMapProtocol.decodeRoute(WatchMapProtocol.encodeRoute(valid)!!))

        assertNull(WatchMapProtocol.encodeRoute(valid.copy(revision = 0L)))
        assertNull(WatchMapProtocol.encodeRoute(valid.copy(coordinates = doubleArrayOf())))
        assertNull(WatchMapProtocol.encodeRoute(valid.copy(coordinates = doubleArrayOf(1.0))))
        assertNull(
            WatchMapProtocol.encodeRoute(
                valid.copy(coordinates = doubleArrayOf(90.0001, 0.0)),
            ),
        )
        assertNull(
            WatchMapProtocol.encodeRoute(
                valid.copy(coordinates = doubleArrayOf(0.0, Double.NaN)),
            ),
        )
    }

    @Test
    fun presenceCodecEnforcesIdentityAndTileBounds() {
        val valid = WatchMapPresence(
            viewerId = "viewer-1",
            viewerEpoch = 0L,
            sequence = 0L,
            foreground = true,
            mapVisible = true,
            missingRoute = WatchMapRouteKey("nav-1", 1L),
            missingTiles = listOf(WatchMapTileKey("OSM", 3, 7, 7)),
        )
        assertNotNull(WatchMapProtocol.decodePresence(WatchMapProtocol.encodePresence(valid)!!))

        assertNull(WatchMapProtocol.encodePresence(valid.copy(viewerId = "")))
        assertNull(WatchMapProtocol.encodePresence(valid.copy(viewerEpoch = -1L)))
        assertNull(
            WatchMapProtocol.encodePresence(
                valid.copy(missingTiles = List(WatchMapProtocol.MAX_TILE_REQUESTS + 1) { valid.missingTiles.single() }),
            ),
        )
        assertNull(
            WatchMapProtocol.encodePresence(
                valid.copy(missingTiles = listOf(WatchMapTileKey("OSM", 3, 8, 0))),
            ),
        )
    }

    @Test
    fun frameCodecKeepsNullsAndIgnoresFutureFields() {
        val frame = WatchMapFrame(
            phoneSessionId = "phone-1",
            viewerId = "viewer-1",
            viewerEpoch = 1L,
            presenceSequence = 2L,
            sequence = 3L,
            enabled = true,
            headingUp = false,
            layerId = "OSM",
            unavailableTiles = emptyList(),
            locationStatus = WatchMapLocationStatus.WAITING_FIX,
            fix = null,
            anchor = null,
            fixMaxAgeMs = 10_000L,
            headingDeg = null,
            navigationSessionId = "",
            navigationActive = false,
            routeRevision = 0L,
            target = null,
            cue = null,
        )
        val encoded = WatchMapProtocol.encodeFrame(frame)!!
        val json = encoded.toString(Charsets.UTF_8)
        assertTrue(json.contains("\"fix\":null"))
        assertTrue(json.contains("\"headingDeg\":null"))

        val withFutureField = json.dropLast(1) + ",\"future\":true}"
        assertNotNull(WatchMapProtocol.decodeFrame(withFutureField.toByteArray(Charsets.UTF_8)))
        assertNull(WatchMapProtocol.encodeFrame(frame.copy(phoneSessionId = "")))
        assertNull(WatchMapProtocol.encodeFrame(frame.copy(fixMaxAgeMs = -1L)))
        assertNull(
            WatchMapProtocol.encodeFrame(
                frame.copy(unavailableTiles = listOf(WatchMapTileKey("OSM", 3, 8, 0))),
            ),
        )
        assertNull(
            WatchMapProtocol.encodeFrame(
                frame.copy(fix = WatchMapFix(WatchMapPoint(0.0, 0.0), -1L)),
            ),
        )
        assertNull(WatchMapProtocol.encodeFrame(frame.copy(headingDeg = Float.NaN)))
        assertNull(
            WatchMapProtocol.encodeFrame(
                frame.copy(cue = WatchMapCue(Float.NaN, "", "", false)),
            ),
        )
        assertNull(
            WatchMapProtocol.encodeFrame(
                frame.copy(fix = WatchMapFix(WatchMapPoint(90.0001, 0.0), 0L)),
            ),
        )
        val invalidTarget = frame.copy(target = WatchMapPoint(0.0, 180.0001))
        val invalidTargetJson = WatchMapProtocol.json
            .encodeToString(WatchMapFrame.serializer(), invalidTarget)
            .toByteArray(Charsets.UTF_8)
        assertNull(WatchMapProtocol.decodeFrame(invalidTargetJson))
        assertNull(WatchMapProtocol.decodeFrame(frame.copy(version = 2).let { WatchMapProtocol.json.encodeToString(WatchMapFrame.serializer(), it) }.toByteArray(Charsets.UTF_8)))
        assertNull(WatchMapProtocol.decodeFrame(byteArrayOf(0xC3.toByte(), 0x28)))
    }
}
