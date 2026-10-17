package com.senda.core.protocol

// spec 101 · Paquetes de referencia de la spec 001, sección 10 (teléfonos A, B y C).
internal object Fixtures {
    const val DEVICE_A = 0xA1B2_C3D4_E5F6L
    const val DEVICE_B = 0x0B0B_0B0B_0B0BL
    const val DEVICE_C = 0x0C0C_0C0C_0C0CL

    const val ALERT_HEX = "a711a1b2c3d4e5f600012a03ec084e40d5e3aa6068f09fc008e5f6"
    const val ON_THE_WAY_HEX = "a712a1b2c3d4e5f600010c0c0c0c0c0c68f0a038080c0c"
    const val STATUS_HEX = "a713a1b2c3d4e5f6000102" + "68f0a218" + "08e5f6"

    /** ALERT de A: contador 1, atrapado o grave, niños y heridos, 3 personas, −33.5 / −70.65. */
    fun alert() = Alert(
        deviceId = DEVICE_A,
        counter = 1,
        emergencyType = EmergencyType.TRAPPED_OR_SEVERE,
        automatic = false,
        vulnerable = Vulnerable(children = true, injured = true),
        people = 3,
        latE7 = -335_000_000,
        lonE7 = -706_500_000,
        createdAtMs = 1_760_600_000_000L,
    )

    fun alertWire() = WirePacket(alert(), ttl = 8, relayTag = 0xE5F6)

    /** ON_THE_WAY de C hacia la alerta de A. */
    fun onTheWayWire() = WirePacket(
        OnTheWay(alert().key, DEVICE_C, createdAtMs = 1_760_600_120_000L),
        ttl = 8,
        relayTag = 0x0C0C,
    )

    /** STATUS de A: a salvo. */
    fun statusWire() = WirePacket(
        Status(alert().key, StatusCode.SAFE, createdAtMs = 1_760_600_600_000L),
        ttl = 8,
        relayTag = 0xE5F6,
    )
}
