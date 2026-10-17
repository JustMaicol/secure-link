package com.securelink.protoble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceivedItem(
    val id: Long,
    val hex: String,
    val rssi: Int,
    val timeFormatted: String,
    val length: Int,
    val counterHex: String
)

data class BleUiState(
    val isAdvertising: Boolean = false,
    val isScanning: Boolean = false,
    val isForeignP4Mode: Boolean = false,
    val advertiseStatusText: String = "Inactivo",
    val scanStatusText: String = "Inactivo",
    val currentAdvertisingSlot: Int = 0,
    val totalAnnouncedCount: Long = 0,
    val totalReceivedCount: Long = 0,
    val totalDuplicatesDiscarded: Long = 0,
    val receivedItems: List<ReceivedItem> = emptyList(),
    // Validación de pruebas T0.6 (Spec 104 sección 7)
    val p1Status: String = "Pendiente",
    val p2Status: String = "Pendiente",
    val p3Status: String = "Pendiente",
    val p4Status: String = "Pendiente",
    val isP3Running: Boolean = false,
    val p3ElapsedSec: Int = 0,
    val p3DistinctCount: Int = 0
)

class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    // 10 Payloads escritos a mano según Spec 001 sección 10 (ALERT con contador 0001 a 000a)
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

    // Payload de prueba ajeno con byte mágico 0xA8 (prueba P4)
    private val foreignP4PayloadHex = "a811a1b2c3d4e5f600012a03ec084e40d5e3aa6068f09fc008e5f6"

    private val validPayloadsBytes = validPayloadsHex.map { hexToBytes(it) }
    private val foreignP4Bytes = hexToBytes(foreignP4PayloadHex)

    // Advertiser state
    private var advertiser: BluetoothLeAdvertiser? = null
    private var currentAdvertisingSet: AdvertisingSet? = null
    private var rotationIndex = 0
    private var rotationRunnable: Runnable? = null

    // Scanner state
    private var scanner: BluetoothLeScanner? = null
    // Mapa para deduplicación dentro de INBOUND_DEDUP_WINDOW_MS (1000 ms)
    private val lastSeenTimestamp = HashMap<String, Long>()
    private var itemCounter = 0L

    // Estado de prueba P3 (10m, rotación 10 paquetes en ventana de 10s)
    private val p3ReceivedCounters = HashSet<String>()
    private var p3StartTimeMs = 0L
    private var p3Runnable: Runnable? = null

    fun toggleP4ForeignMode() {
        _uiState.update { it.copy(isForeignP4Mode = !it.isForeignP4Mode) }
    }

    fun startP3Evaluation() {
        p3ReceivedCounters.clear()
        p3StartTimeMs = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isP3Running = true,
                p3ElapsedSec = 0,
                p3DistinctCount = 0,
                p3Status = "Evaluando ventana de 10s..."
            )
        }

        p3Runnable?.let { handler.removeCallbacks(it) }
        p3Runnable = object : Runnable {
            override fun run() {
                val elapsed = ((System.currentTimeMillis() - p3StartTimeMs) / 1000L).toInt()
                val distinct = p3ReceivedCounters.size

                if (elapsed >= 10 || distinct >= 9) {
                    val passed = distinct >= 9
                    _uiState.update {
                        it.copy(
                            isP3Running = false,
                            p3ElapsedSec = elapsed.coerceAtMost(10),
                            p3DistinctCount = distinct,
                            p3Status = if (passed) "PASÓ: $distinct/10 paquetes en ${elapsed}s (umbral >=9/10)" else "FALLÓ: solo $distinct/10 paquetes en 10s"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            p3ElapsedSec = elapsed,
                            p3DistinctCount = distinct,
                            p3Status = "Evaluando: $distinct/10 recibidos (${elapsed}s/10s)..."
                        )
                    }
                    handler.postDelayed(this, 500L)
                }
            }
        }
        handler.postDelayed(p3Runnable!!, 500L)
    }

    fun clearReceived() {
        lastSeenTimestamp.clear()
        p3ReceivedCounters.clear()
        p3Runnable?.let { handler.removeCallbacks(it) }
        p3Runnable = null
        _uiState.update {
            it.copy(
                totalReceivedCount = 0,
                totalDuplicatesDiscarded = 0,
                receivedItems = emptyList(),
                isP3Running = false,
                p3ElapsedSec = 0,
                p3DistinctCount = 0,
                p3Status = "Pendiente",
                p2Status = "Pendiente",
                p4Status = "Pendiente"
            )
        }
    }

    // -------------------------------------------------------------
    // ANUNCIANTE (T0.4)
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _uiState.update { it.copy(advertiseStatusText = "Error: Bluetooth apagado o no disponible") }
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            _uiState.update { it.copy(advertiseStatusText = "Error: Anunciante BLE no soportado (ADVERTISE_UNSUPPORTED)") }
            return
        }

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setConnectable(false)
            .setScannable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_MIN)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()

        rotationIndex = 0
        val activePayload = if (_uiState.value.isForeignP4Mode) foreignP4Bytes else validPayloadsBytes[0]
        val initialData = buildAdvertiseData(activePayload)

        _uiState.update {
            it.copy(
                isAdvertising = true,
                advertiseStatusText = "Iniciando AdvertisingSet...",
                currentAdvertisingSlot = 1
            )
        }

        advertiser?.startAdvertisingSet(parameters, initialData, null, null, null, advertisingCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        rotationRunnable?.let { handler.removeCallbacks(it) }
        rotationRunnable = null

        currentAdvertisingSet?.let {
            try {
                advertiser?.stopAdvertisingSet(advertisingCallback)
            } catch (e: Exception) {
                // ignorar si ya estaba detenido
            }
        }
        currentAdvertisingSet = null

        _uiState.update {
            it.copy(
                isAdvertising = false,
                advertiseStatusText = "Detenido"
            )
        }
    }

    private fun buildAdvertiseData(payload: ByteArray): AdvertiseData {
        return AdvertiseData.Builder()
            .addManufacturerData(0xFFFF, payload)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
    }

    private val advertisingCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                currentAdvertisingSet = advertisingSet
                _uiState.update {
                    it.copy(
                        advertiseStatusText = "Activo (txPower=$txPower)",
                        totalAnnouncedCount = it.totalAnnouncedCount + 1,
                        p1Status = if (!it.isForeignP4Mode) "PASÓ: 27B sin DATA_TOO_LARGE" else it.p1Status
                    )
                }
                startRotationLoop()
            } else {
                val isDataTooLarge = status == ADVERTISE_FAILED_DATA_TOO_LARGE
                val errorMsg = when (status) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "FAILED: DATA_TOO_LARGE (P1 FALLÓ - payload > 27b)"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "FAILED: TOO_MANY_ADVERTISERS"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FAILED: FEATURE_UNSUPPORTED"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "FAILED: INTERNAL_ERROR"
                    else -> "FAILED: código=$status"
                }
                _uiState.update {
                    it.copy(
                        isAdvertising = false,
                        advertiseStatusText = errorMsg,
                        p1Status = if (isDataTooLarge) "FALLÓ: DATA_TOO_LARGE" else "Error código=$status"
                    )
                }
            }
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
            if (status != ADVERTISE_SUCCESS) {
                _uiState.update {
                    it.copy(advertiseStatusText = "Error en setAdvertisingData: status=$status")
                }
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            currentAdvertisingSet = null
            _uiState.update {
                it.copy(
                    isAdvertising = false,
                    advertiseStatusText = "Detenido"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRotationLoop() {
        rotationRunnable?.let { handler.removeCallbacks(it) }

        rotationRunnable = object : Runnable {
            override fun run() {
                if (!_uiState.value.isAdvertising || currentAdvertisingSet == null) return

                val activeList = if (_uiState.value.isForeignP4Mode) {
                    listOf(foreignP4Bytes)
                } else {
                    validPayloadsBytes
                }

                rotationIndex = (rotationIndex + 1) % activeList.size
                val nextPayload = activeList[rotationIndex]

                try {
                    currentAdvertisingSet?.setAdvertisingData(buildAdvertiseData(nextPayload))
                    _uiState.update {
                        it.copy(
                            currentAdvertisingSlot = rotationIndex + 1,
                            totalAnnouncedCount = it.totalAnnouncedCount + 1
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(advertiseStatusText = "Excepción en rotación: ${e.message}")
                    }
                }

                // Rotación cada 500 ms (ROTATION_SLOT_MS)
                handler.postDelayed(this, 500L)
            }
        }

        handler.postDelayed(rotationRunnable!!, 500L)
    }

    // -------------------------------------------------------------
    // ESCÁNER (T0.5)
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun startScanning() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _uiState.update { it.copy(scanStatusText = "Error: Bluetooth apagado o no disponible") }
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _uiState.update { it.copy(scanStatusText = "Error: Escáner BLE no disponible") }
            return
        }

        // Filtro de fabricante de la Spec 001 sección 1 y Spec 104 sección 4:
        // Company ID: 0xFFFF
        // Data: [0xA7, 0x10]
        // Mask: [0xFF, 0xF0] (primer byte 0xA7 exacto, segundo byte versión 1 en los 4 bits altos)
        val manufacturerData = byteArrayOf(0xA7.toByte(), 0x10.toByte())
        val manufacturerDataMask = byteArrayOf(0xFF.toByte(), 0xF0.toByte())

        val filter = ScanFilter.Builder()
            .setManufacturerData(0xFFFF, manufacturerData, manufacturerDataMask)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .setLegacy(true)
            .build()

        _uiState.update {
            it.copy(
                isScanning = true,
                scanStatusText = "Escaneando (LOW_LATENCY)"
            )
        }

        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // ignorar si ya estaba detenido
        }
        _uiState.update {
            it.copy(
                isScanning = false,
                scanStatusText = "Detenido"
            )
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val record = result?.scanRecord ?: return
            val rawData = record.getManufacturerSpecificData(0xFFFF) ?: return

            val hexString = bytesToHex(rawData)
            val nowMs = System.currentTimeMillis()

            // Deduplicación en ventana de 1000 ms (INBOUND_DEDUP_WINDOW_MS)
            val lastSeen = lastSeenTimestamp[hexString]
            if (lastSeen != null && (nowMs - lastSeen) < 1000L) {
                // Descartado por repetido dentro de la ventana de 1s
                _uiState.update { it.copy(totalDuplicatesDiscarded = it.totalDuplicatesDiscarded + 1) }
                return
            }

            // Actualizar marca de tiempo
            lastSeenTimestamp[hexString] = nowMs

            // Extraer contador para fácil visualización (bytes 8-9 si mide al menos 10 bytes)
            val counterHex = if (rawData.size >= 10) {
                String.format("%02x%02x", rawData[8], rawData[9])
            } else {
                "??"
            }

            val timeFormatted = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(nowMs))
            itemCounter++

            val newItem = ReceivedItem(
                id = itemCounter,
                hex = hexString,
                rssi = result.rssi,
                timeFormatted = timeFormatted,
                length = rawData.size,
                counterHex = counterHex
            )

            // Actualización de pruebas T0.6
            val isKnownAlert = rawData.size == 27 && validPayloadsHex.contains(hexString)
            val p2Result = if (isKnownAlert) "PASÓ: 27B íntegro exacto" else _uiState.value.p2Status

            if (_uiState.value.isP3Running) {
                p3ReceivedCounters.add(counterHex)
            }

            val p4Result = if (rawData.isNotEmpty() && rawData[0] == 0xA8.toByte()) {
                "FALLÓ: Paquete ajeno 0xA8 recibido"
            } else {
                "PASÓ: Filtro bloquea 0xA8"
            }

            _uiState.update {
                it.copy(
                    totalReceivedCount = it.totalReceivedCount + 1,
                    receivedItems = listOf(newItem) + it.receivedItems.take(99), // mantener últimos 100
                    p2Status = p2Result,
                    p4Status = p4Result,
                    p3DistinctCount = if (it.isP3Running) p3ReceivedCounters.size else it.p3DistinctCount
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "FAILED: SCANNING_TOO_FREQUENTLY"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "FAILED: APPLICATION_REGISTRATION_FAILED"
                SCAN_FAILED_INTERNAL_ERROR -> "FAILED: INTERNAL_ERROR"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "FAILED: FEATURE_UNSUPPORTED"
                else -> "FAILED: código=$errorCode"
            }
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanStatusText = errorMsg
                )
            }
        }
    }

    companion object {
        fun hexToBytes(hex: String): ByteArray {
            val clean = hex.replace(" ", "")
            val bytes = ByteArray(clean.length / 2)
            for (i in bytes.indices) {
                val index = i * 2
                bytes[i] = clean.substring(index, index + 2).toInt(16).toByte()
            }
            return bytes
        }

        fun bytesToHex(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        }
    }
}
