package com.spchief87.childtracker

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spchief87.childtracker.data.model.LocationData
import com.spchief87.childtracker.data.repository.LocationRepository
import com.spchief87.childtracker.ui.theme.ChildTrackerTheme
import com.spchief87.childtracker.ui.viewmodel.LocationViewModel
import com.spchief87.childtracker.utils.DeviceIdManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var locationRepository: LocationRepository

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Разрешения получены, можно продолжить
        } else {
            // Разрешения отклонены
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestLocationPermissions()

        setContent {
            ChildTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationTrackingScreen(
                        modifier = Modifier.padding(innerPadding),
                        onCopyDeviceId = { copyToClipboard(it) }
                    )
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        locationPermissionRequest.launch(permissions.toTypedArray())
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Device ID", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "ID устройства скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onCopyDeviceId: (String) -> Unit = {}
) {
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val unsentCount by viewModel.unsentCount.collectAsStateWithLifecycle()
    val lastLocations by viewModel.lastLocations.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLocations by remember { mutableStateOf(false) }
    var showDeviceInfo by remember { mutableStateOf(false) }

    val deviceId = remember { DeviceIdManager.getDeviceId(context) }

    // Счетчик неотправленных записей теперь обновляется автоматически через Flow
    // LaunchedEffect больше не нужен

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Child Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isTracking) "Отслеживание активно" else "Отслеживание остановлено",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Неотправленных записей: $unsentCount",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { viewModel.toggleTracking() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isTracking) "Остановить отслеживание" else "Начать отслеживание"
                    )
                }

                if (unsentCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.syncLocations() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Синхронизировать данные")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    showLocations = !showLocations
                    if (showLocations) {
                        viewModel.loadLastLocations(10)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (showLocations) "Скрыть историю" else "Показать историю")
            }

            OutlinedButton(
                onClick = {
                    if (!showDeviceInfo) {
                        // Копируем ID в буфер обмена при нажатии на кнопку "Показать ID"
                        onCopyDeviceId(deviceId)
                    }
                    showDeviceInfo = !showDeviceInfo
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (showDeviceInfo) "Скрыть ID" else "Показать ID")
            }
        }

        if (showLocations) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Последние записи местоположения",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (lastLocations.isEmpty()) {
                        Text(
                            text = "Нет записей местоположения",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(lastLocations) { location ->
                                LocationItem(location = location)
                            }
                        }
                    }
                }
            }
        }

        if (showDeviceInfo) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Идентификатор устройства",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = deviceId,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    )

                    Text(
                        text = "Этот ID используется для идентификации устройства на сервере",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationItem(location: LocationData) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(location.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (location.isSent) "Отправлено" else "Не отправлено",
                    fontSize = 10.sp,
                    color = if (location.isSent)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Широта: ${String.format("%.6f", location.latitude)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Долгота: ${String.format("%.6f", location.longitude)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Точность: ${String.format("%.1f", location.accuracy)} м",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            location.provider?.let { provider ->
                Text(
                    text = "Источник: $provider",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}