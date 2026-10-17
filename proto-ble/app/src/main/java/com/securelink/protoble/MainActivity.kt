package com.securelink.protoble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = BleManager(applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF7F9FB)
                ) {
                    ProtoBleScreen(bleManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.stopAdvertising()
        bleManager.stopScanning()
    }
}

@Composable
fun ProtoBleScreen(bleManager: BleManager) {
    val context = LocalContext.current
    val uiState by bleManager.uiState.collectAsState()

    val requiredPermissions = remember {
        mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título del Prototipo
        Text(
            text = "SecureLink · Prototipo BLE T0",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "Spec 104 · Validación Samsung (minSdk 34)",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Alerta de permisos
        if (!permissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Faltan permisos BLE/Ubicación",
                        color = Color(0xFF9F1239),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Conceder", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Panel de Estado
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Anunciante:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = if (uiState.isAdvertising) "Slot ${uiState.currentAdvertisingSlot}/10 (500ms)" else "Inactivo",
                        fontSize = 13.sp,
                        color = if (uiState.isAdvertising) Color(0xFF16A34A) else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = uiState.advertiseStatusText,
                    fontSize = 11.sp,
                    color = if (uiState.advertiseStatusText.contains("FAILED")) Color.Red else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Escáner (Filtro 0xA7):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = if (uiState.isScanning) "ACTIVO" else "Inactivo",
                        fontSize = 13.sp,
                        color = if (uiState.isScanning) Color(0xFF16A34A) else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = uiState.scanStatusText,
                    fontSize = 11.sp,
                    color = if (uiState.scanStatusText.contains("FAILED")) Color.Red else Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Controles de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (uiState.isAdvertising) bleManager.stopAdvertising()
                    else bleManager.startAdvertising()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isAdvertising) Color(0xFFDC2626) else Color(0xFF2563EB)
                )
            ) {
                Text(if (uiState.isAdvertising) "Detener Anuncio" else "Anunciar")
            }

            Button(
                onClick = {
                    if (uiState.isScanning) bleManager.stopScanning()
                    else bleManager.startScanning()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isScanning) Color(0xFFDC2626) else Color(0xFF059669)
                )
            ) {
                Text(if (uiState.isScanning) "Detener Escaneo" else "Escanear")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Botones secundarios: Modo P4 y Limpiar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { bleManager.toggleP4ForeignMode() },
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (uiState.isForeignP4Mode) Color(0xFFD97706) else Color(0xFF475569)
                )
            ) {
                Text(
                    text = if (uiState.isForeignP4Mode) "Modo P4: 0xA8 (Activo)" else "Modo P4: Normal",
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = { bleManager.clearReceived() },
                modifier = Modifier.weight(0.8f)
            ) {
                Text("Limpiar", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Contadores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CounterBox("Emitidos", uiState.totalAnnouncedCount, Color(0xFF3B82F6), Modifier.weight(1f))
            CounterBox("Recibidos", uiState.totalReceivedCount, Color(0xFF10B981), Modifier.weight(1f))
            CounterBox("Duplicados <1s", uiState.totalDuplicatesDiscarded, Color(0xFFF59E0B), Modifier.weight(1.2f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Panel de Validación Pruebas T0.6 (Spec 104)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Validación Pruebas T0.6 (Samsung)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TestStatusItem("P1 (27B sin DATA_TOO_LARGE):", uiState.p1Status)
                TestStatusItem("P2 (Pantalla apagada / Hex):", uiState.p2Status)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "P3 (10m · 10 paq en <10s):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = uiState.p3Status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                uiState.p3Status.contains("PASÓ") -> Color(0xFF16A34A)
                                uiState.p3Status.contains("FALLÓ") -> Color(0xFFDC2626)
                                else -> Color(0xFF64748B)
                            }
                        )
                    }
                    Button(
                        onClick = { bleManager.startP3Evaluation() },
                        enabled = !uiState.isP3Running && uiState.isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (uiState.isP3Running) "${uiState.p3ElapsedSec}s (${uiState.p3DistinctCount}/10)" else "Evaluar P3",
                            fontSize = 10.sp
                        )
                    }
                }

                TestStatusItem("P4 (Filtro bloquea 0xA8):", uiState.p4Status)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cabecera de Lista de Recibidos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paquetes Recibidos (${uiState.receivedItems.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Filtro 0xA7 · Ventana 1s",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Lista de paquetes recibidos
        if (uiState.receivedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isScanning) "Esperando paquetes BLE por el aire..." else "Escaneo inactivo. Toca 'Escanear'.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.receivedItems, key = { it.id }) { item ->
                    ReceivedItemCard(item)
                }
            }
        }
    }
}

@Composable
fun CounterBox(title: String, count: Long, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = Color(0xFF64748B))
            Text(
                text = count.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ReceivedItemCard(item: ReceivedItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${item.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Contador: 0x${item.counterHex}",
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.rssi} dBm",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.rssi > -70) Color(0xFF16A34A) else Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.timeFormatted,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hex payload completo
            Text(
                text = item.hex,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF0F172A),
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tamaño: ${item.length} bytes (Company ID 0xFFFF omitido en payload)",
                fontSize = 10.sp,
                color = if (item.length == 27) Color(0xFF16A34A) else Color.Red
            )
        }
    }
}

@Composable
fun TestStatusItem(title: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF334155),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                status.contains("PASÓ") -> Color(0xFF16A34A)
                status.contains("FALLÓ") -> Color(0xFFDC2626)
                else -> Color(0xFF64748B)
            }
        )
    }
}

