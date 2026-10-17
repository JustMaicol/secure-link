package com.securelink.protoble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para validar las hipótesis y contratos de T0.6 (Spec 104 y Spec 001):
 * - P1: Carga útil ALERT de exactamente 27 bytes para evitar ADVERTISE_FAILED_DATA_TOO_LARGE.
 * - P2: Integridad del contenido hexadecimal de los paquetes rotados.
 * - P3: Criterio de llegada de paquetes en ventana temporal (>= 9 de 10 en < 10 s).
 * - P4: Simulación del filtro de máscara de fabricante que descarta byte mágico 0xA8.
 */
class BleValidationTest {

    private val validPayloadsHex = listOf(
        "a711a1b2c3d4e5f600012a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600022a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600032a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600042a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600052a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600062a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600072a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600082a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f600092a03ec084e40d5e3aa6068f09fc008e5f6",
        "a711a1b2c3d4e5f6000a2a03ec084e40d5e3aa6068f09fc008e5f6"
    )

    private val foreignP4PayloadHex = "a811a1b2c3d4e5f600012a03ec084e40d5e3aa6068f09fc008e5f6"

    @Test
    fun testP1_PayloadSizeMatches27BytesLegacyLimit() {
        assertEquals("Deben existir 10 payloads para la rotación", 10, validPayloadsHex.size)

        for ((index, hex) in validPayloadsHex.withIndex()) {
            val bytes = BleManager.hexToBytes(hex)
            assertEquals("El payload $index debe tener exactamente 27 bytes", 27, bytes.size)
            // Límite de legacy advertise: 31 bytes totales - 4 bytes de encabezado AD de fabricante = 27 bytes útiles
            assertTrue("27 bytes cabe en el paquete legacy sin DATA_TOO_LARGE", bytes.size <= 27)
        }
    }

    @Test
    fun testP1_PayloadStructureIntegrity() {
        for ((index, hex) in validPayloadsHex.withIndex()) {
            val bytes = BleManager.hexToBytes(hex)
            // Byte 0: Mágico 0xA7
            assertEquals("Byte mágico debe ser 0xA7", 0xA7.toByte(), bytes[0])
            // Byte 1: Versión 1 (0x1) en nibble alto, tipo 1 (ALERT) en nibble bajo -> 0x11
            assertEquals("Versión y tipo deben ser 0x11", 0x11.toByte(), bytes[1])
            // Contador (bytes 8 y 9)
            val counter = ((bytes[8].toInt() and 0xFF) shl 8) or (bytes[9].toInt() and 0xFF)
            assertEquals("El contador secuencial debe coincidir", index + 1, counter)
        }
    }

    @Test
    fun testP4_ManufacturerFilterRejectsForeignMagicByte() {
        // Filtro configurado en Spec 104 sección 4:
        // Company ID: 0xFFFF
        // Data: [0xA7, 0x10]
        // Mask: [0xFF, 0xF0]
        val filterData = byteArrayOf(0xA7.toByte(), 0x10.toByte())
        val filterMask = byteArrayOf(0xFF.toByte(), 0xF0.toByte())

        fun matchesFilter(payload: ByteArray): Boolean {
            if (payload.size < 2) return false
            val byte0Match = (payload[0].toInt() and filterMask[0].toInt()) == (filterData[0].toInt() and filterMask[0].toInt())
            val byte1Match = (payload[1].toInt() and filterMask[1].toInt()) == (filterData[1].toInt() and filterMask[1].toInt())
            return byte0Match && byte1Match
        }

        // Cada payload válido debe coincidir con el filtro
        for (hex in validPayloadsHex) {
            val bytes = BleManager.hexToBytes(hex)
            assertTrue("El payload $hex debe pasar el filtro de fabricante", matchesFilter(bytes))
        }

        // El payload ajeno con byte mágico 0xA8 debe ser rechazado
        val foreignBytes = BleManager.hexToBytes(foreignP4PayloadHex)
        assertFalse("El payload ajeno con byte mágico 0xA8 DEBE ser rechazado por el filtro", matchesFilter(foreignBytes))
    }

    @Test
    fun testP2_HexEncodingRoundTrip() {
        for (hex in validPayloadsHex) {
            val bytes = BleManager.hexToBytes(hex)
            val convertedBack = BleManager.bytesToHex(bytes)
            assertEquals("La conversión hex -> bytes -> hex debe ser 100% fiel", hex.lowercase(), convertedBack.lowercase())
        }
    }

    @Test
    fun testInboundDeduplicationWindow() {
        val dedupMap = HashMap<String, Long>()
        val hex = validPayloadsHex[0]
        val t0 = 10000L

        // Primer paquete: aceptado
        dedupMap[hex] = t0
        assertTrue("Primer paquete registrado", dedupMap.containsKey(hex))

        // Paquete repetido a los 500ms (< 1000ms ventana): debe descartarse
        val tRepeated = t0 + 500L
        val shouldDiscard = (tRepeated - dedupMap[hex]!!) < 1000L
        assertTrue("El duplicado a los 500ms debe descartarse", shouldDiscard)

        // Paquete repetido a los 1100ms (> 1000ms ventana): debe procesarse
        val tAfterWindow = t0 + 1100L
        val shouldProcess = (tAfterWindow - dedupMap[hex]!!) >= 1000L
        assertTrue("El paquete después de 1s debe procesarse", shouldProcess)
    }

    @Test
    fun testP3_CriterionEvaluationLogic() {
        // Criterio P3: al menos 9 de 10 paquetes en menos de 10 segundos
        val distinctReceivedCounters = mutableSetOf("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009")
        val elapsedSeconds = 8.5

        val passedP3 = distinctReceivedCounters.size >= 9 && elapsedSeconds <= 10.0
        assertTrue("9 de 10 paquetes recibidos en 8.5s debe dar PASÓ", passedP3)

        // Caso fallido: solo 8 paquetes
        distinctReceivedCounters.remove("0009")
        val failedP3 = distinctReceivedCounters.size >= 9 && elapsedSeconds <= 10.0
        assertFalse("8 de 10 paquetes recibidos debe dar FALLÓ", failedP3)
    }
}
