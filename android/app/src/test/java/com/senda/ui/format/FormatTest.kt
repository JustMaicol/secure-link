package com.senda.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

// spec 100, criterio 11 · Cada regla de la sección 11 con los ejemplos de su tabla y sus bordes.
class FormatTest {

    // ---------------- Distancia ----------------

    private fun distance(meters: Double?) = classifyDistance(meters)

    @Test
    fun `distance under 1000 m rounds to 10 m`() {
        assertEquals(DistanceLabel.Meters(850), distance(850.0))
        assertEquals(DistanceLabel.Meters(850), distance(852.0))
        assertEquals(DistanceLabel.Meters(860), distance(855.0)) // mitad hacia arriba
        assertEquals(DistanceLabel.Meters(0), distance(0.0))
        assertEquals(DistanceLabel.Meters(0), distance(4.9))
        assertEquals(DistanceLabel.Meters(10), distance(5.0))
        assertEquals(DistanceLabel.Meters(990), distance(994.0))
    }

    @Test
    fun `999 m and 1000 m both become 1,0 km instead of showing 1000 m`() {
        assertEquals(DistanceLabel.Kilometers(10), distance(995.0))
        assertEquals(DistanceLabel.Kilometers(10), distance(999.0))
        assertEquals(DistanceLabel.Kilometers(10), distance(1000.0))
    }

    @Test
    fun `from 1 to 100 km it keeps one decimal`() {
        assertEquals(DistanceLabel.Kilometers(12), distance(1200.0)) // 1,2 km
        assertEquals(DistanceLabel.Kilometers(12), distance(1249.0))
        assertEquals(DistanceLabel.Kilometers(13), distance(1250.0))
        assertEquals(DistanceLabel.Kilometers(999), distance(99_900.0)) // 99,9 km
        assertEquals(DistanceLabel.Kilometers(999), distance(99_949.0))
    }

    @Test
    fun `100 km and above is a whole number`() {
        assertEquals(DistanceLabel.WholeKilometers(100), distance(99_950.0)) // no 100,0 km
        assertEquals(DistanceLabel.WholeKilometers(100), distance(100_000.0))
        assertEquals(DistanceLabel.WholeKilometers(123), distance(123_000.0)) // 123 km
        assertEquals(DistanceLabel.WholeKilometers(124), distance(123_500.0))
    }

    @Test
    fun `missing or invalid distance is unknown`() {
        assertEquals(DistanceLabel.Unknown, distance(null))
        assertEquals(DistanceLabel.Unknown, distance(-1.0))
        assertEquals(DistanceLabel.Unknown, distance(Double.NaN))
        assertEquals(DistanceLabel.Unknown, distance(Double.POSITIVE_INFINITY))
    }

    // ---------------- Tiempo transcurrido ----------------

    @Test
    fun `under a second is now, including a clock that is ahead`() {
        assertEquals(ElapsedLabel.Now, classifyElapsed(0))
        assertEquals(ElapsedLabel.Now, classifyElapsed(999))
        assertEquals(ElapsedLabel.Now, classifyElapsed(-5_000))
    }

    @Test
    fun `seconds run from 1 s up to 60 s exclusive`() {
        assertEquals(ElapsedLabel.Seconds(1), classifyElapsed(1_000))
        assertEquals(ElapsedLabel.Seconds(12), classifyElapsed(12_400))
        assertEquals(ElapsedLabel.Seconds(20), classifyElapsed(20_000))
        assertEquals(ElapsedLabel.Seconds(59), classifyElapsed(59_000))
        assertEquals(ElapsedLabel.Seconds(59), classifyElapsed(59_999))
    }

    @Test
    fun `minutes run from 60 s up to 60 min exclusive`() {
        assertEquals(ElapsedLabel.Minutes(1), classifyElapsed(60_000))
        assertEquals(ElapsedLabel.Minutes(5), classifyElapsed(5 * 60_000L))
        assertEquals(ElapsedLabel.Minutes(59), classifyElapsed(60 * 60_000L - 1))
    }

    @Test
    fun `hours run from 60 min up to 24 h exclusive`() {
        assertEquals(ElapsedLabel.Hours(1), classifyElapsed(60 * 60_000L)) // 60 min
        assertEquals(ElapsedLabel.Hours(1), classifyElapsed(119 * 60_000L)) // trunca
        assertEquals(ElapsedLabel.Hours(3), classifyElapsed(3 * 3_600_000L))
        assertEquals(ElapsedLabel.Hours(23), classifyElapsed(24 * 3_600_000L - 1))
    }

    @Test
    fun `days start at 24 h`() {
        assertEquals(ElapsedLabel.Days(1), classifyElapsed(24 * 3_600_000L)) // 24 h
        assertEquals(ElapsedLabel.Days(2), classifyElapsed(2 * 86_400_000L + 3_600_000L))
    }

    // ---------------- Hora ----------------

    @Test
    fun `clock uses HH colon mm in the phone's zone`() {
        val epoch = 1_760_600_000_000L // 2025-10-16 07:33:20 UTC

        assertEquals("07:33", formatClock(epoch, ZoneOffset.UTC))
        assertEquals("04:33", formatClock(epoch, ZoneId.of("America/Santiago"))) // UTC-3 en octubre
        assertEquals("00:00", formatClock(0, ZoneOffset.UTC))
        assertEquals("23:59", formatClock(86_340_000L, ZoneOffset.UTC))
    }

    // ---------------- Personas ----------------

    @Test
    fun `people has singular, plural and the cap`() {
        assertEquals(PeopleLabel.One, classifyPeople(1))
        assertEquals(PeopleLabel.Many(3), classifyPeople(3))
        assertEquals(PeopleLabel.Many(254), classifyPeople(254))
        assertEquals(PeopleLabel.AtLeast(255), classifyPeople(255))
        assertEquals(PeopleLabel.AtLeast(255), classifyPeople(1000))
    }

    // ---------------- Precisión ----------------

    @Test
    fun `precision is whole meters`() {
        assertEquals(PrecisionLabel.Meters(15), classifyPrecision(15.0))
        assertEquals(PrecisionLabel.Meters(15), classifyPrecision(14.5))
        assertEquals(PrecisionLabel.Meters(14), classifyPrecision(14.49))
        assertEquals(PrecisionLabel.Meters(0), classifyPrecision(0.2))
        assertEquals(PrecisionLabel.Unknown, classifyPrecision(null))
        assertEquals(PrecisionLabel.Unknown, classifyPrecision(-1.0))
        assertEquals(PrecisionLabel.Unknown, classifyPrecision(Double.NaN))
    }

    // ---------------- Rumbo cardinal ----------------

    @Test
    fun `cardinal point at the center of each sector`() {
        val centers = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

        assertEquals(CardinalPoint.entries.toList(), centers.map { cardinalOf(it) })
        assertEquals(listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"), CardinalPoint.entries.map { it.name })
    }

    @Test
    fun `sector limits belong to the next sector clockwise`() {
        assertEquals(CardinalPoint.N, cardinalOf(22.4999))
        assertEquals(CardinalPoint.NE, cardinalOf(22.5))
        assertEquals(CardinalPoint.NE, cardinalOf(67.4999))
        assertEquals(CardinalPoint.E, cardinalOf(67.5))
        assertEquals(CardinalPoint.E, cardinalOf(112.4999))
        assertEquals(CardinalPoint.SE, cardinalOf(112.5))
        assertEquals(CardinalPoint.SO, cardinalOf(202.5))
        assertEquals(CardinalPoint.NO, cardinalOf(292.5))
        assertEquals(CardinalPoint.NO, cardinalOf(337.4999))
        assertEquals(CardinalPoint.N, cardinalOf(337.5))
    }

    @Test
    fun `cardinal point wraps negative and large angles`() {
        assertEquals(CardinalPoint.N, cardinalOf(360.0))
        assertEquals(CardinalPoint.N, cardinalOf(-10.0))
        assertEquals(CardinalPoint.NO, cardinalOf(-45.0))
        assertEquals(CardinalPoint.E, cardinalOf(450.0))
        assertEquals(CardinalPoint.S, cardinalOf(-180.0))
        assertNull(cardinalOf(null))
        assertNull(cardinalOf(Double.NaN))
    }

    // ---------------- Cuenta atrás ----------------

    @Test
    fun `countdown in seconds rounds up and never goes below zero`() {
        assertEquals(15, countdownSeconds(15_000))
        assertEquals(15, countdownSeconds(14_001))
        assertEquals(14, countdownSeconds(14_000))
        assertEquals(1, countdownSeconds(1))
        assertEquals(0, countdownSeconds(0))
        assertEquals(0, countdownSeconds(-500))
    }

    @Test
    fun `countdown in m colon ss`() {
        assertEquals("0:42", countdownMinutesSeconds(42_000))
        assertEquals("0:42", countdownMinutesSeconds(41_001))
        assertEquals("0:09", countdownMinutesSeconds(9_000))
        assertEquals("0:00", countdownMinutesSeconds(0))
        assertEquals("1:00", countdownMinutesSeconds(60_000))
        assertEquals("1:01", countdownMinutesSeconds(60_001))
        assertEquals("2:05", countdownMinutesSeconds(125_000))
        assertEquals("0:00", countdownMinutesSeconds(-1))
    }

    // ---------------- Coordenadas ----------------

    @Test
    fun `coordinates are decimal degrees with a comma and four decimals`() {
        assertEquals("-33,5000, -70,6500", formatCoordinates(-335_000_000, -706_500_000))
        assertEquals("0,0000, 0,0000", formatCoordinates(0, 0))
        assertEquals("12,3457, 98,7654", formatCoordinates(123_456_789, 987_654_321)) // redondea la mitad hacia arriba
        assertEquals("90,0000, 180,0000", formatCoordinates(900_000_000, 1_800_000_000))
        assertEquals("-90,0000, -180,0000", formatCoordinates(-900_000_000, -1_800_000_000))
    }

    @Test
    fun `coordinates never show minus zero and keep leading zeros of the decimals`() {
        assertEquals("0,0000, 0,0000", formatCoordinates(-499, -1)) // se redondea a cero: sin signo
        assertEquals("-0,0001, 0,0100", formatCoordinates(-1_000, 100_000))
        assertEquals("1,0005, -2,0050", formatCoordinates(10_005_000, -20_050_000))
    }

    // ---------------- deviceId y relayTag ----------------

    @Test
    fun `deviceId is 12 lowercase hex digits in groups of 4`() {
        assertEquals("a1b2 c3d4 e5f6", formatDeviceId(0xA1B2_C3D4_E5F6L))
        assertEquals("0c0c 0c0c 0c0c", formatDeviceId(0x0C0C_0C0C_0C0CL))
        assertEquals("0000 0000 0001", formatDeviceId(1))
        assertEquals("ffff ffff ffff", formatDeviceId(0xFFFF_FFFF_FFFFL))
    }

    @Test
    fun `relayTag is 4 lowercase hex digits`() {
        assertEquals("e5f6", formatRelayTag(0xE5F6))
        assertEquals("0b0b", formatRelayTag(0x0B0B))
        assertEquals("0000", formatRelayTag(0))
        assertEquals("ffff", formatRelayTag(65_535))
    }
}
