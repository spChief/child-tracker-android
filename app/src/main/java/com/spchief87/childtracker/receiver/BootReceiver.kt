package com.spchief87.childtracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.spchief87.childtracker.service.LocationTrackingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Проверяем, было ли включено отслеживание до перезагрузки
                val isTrackingEnabled = sharedPreferences.getBoolean("tracking_enabled", false)

                if (isTrackingEnabled) {
                    // Запускаем сервис отслеживания местоположения
                    LocationTrackingService.startService(context)
                }
            }
        }
    }
}