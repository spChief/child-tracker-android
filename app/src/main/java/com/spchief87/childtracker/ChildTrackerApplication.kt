package com.spchief87.childtracker

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.spchief87.childtracker.data.repository.LocationRepository
import com.spchief87.childtracker.service.LocationTrackingService
import com.spchief87.childtracker.worker.LocationSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChildTrackerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        initializeAutoStart()
    }

    private fun initializeAutoStart() {
        // Проверяем, было ли включено отслеживание до перезапуска приложения
        val isTrackingEnabled = sharedPreferences.getBoolean("tracking_enabled", false)

        if (isTrackingEnabled) {
            // Запускаем сервис отслеживания местоположения
            LocationTrackingService.startService(this)

            // Запускаем периодическую синхронизацию
            LocationSyncWorker.enqueuePeriodic(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}
