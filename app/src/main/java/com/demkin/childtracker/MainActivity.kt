package com.demkin.childtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demkin.childtracker.data.model.LocationData
import com.demkin.childtracker.data.repository.LocationRepository
import com.demkin.childtracker.ui.theme.ChildTrackerTheme
import com.demkin.childtracker.ui.viewmodel.LocationViewModel
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
                        modifier = Modifier.padding(innerPadding)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTrackingScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val unsentCount by viewModel.unsentCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLocations by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf<List<LocationData>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.refreshUnsentCount()
    }

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

        Button(
            onClick = {
                showLocations = !showLocations
                if (showLocations) {
                    // Загружаем последние 10 записей
                    // В реальном приложении это должно быть через ViewModel
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showLocations) "Скрыть историю" else "Показать историю")
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

                    // Здесь будет список записей
                    Text(
                        text = "История загрузки будет реализована в следующей версии",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}