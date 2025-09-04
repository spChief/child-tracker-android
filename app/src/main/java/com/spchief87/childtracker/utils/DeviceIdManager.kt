package com.spchief87.childtracker.utils

import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceIdManager {

    private const val PREFS_DEVICE_ID = "device_id"
    private const val PREFS_NAME = "device_prefs"

    /**
     * Получает уникальный идентификатор устройства.
     * Сначала пытается получить Android ID, если он недоступен - генерирует UUID.
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Проверяем, есть ли уже сохраненный ID
        val savedId = prefs.getString(PREFS_DEVICE_ID, null)
        if (!savedId.isNullOrEmpty()) {
            return savedId
        }

        // Пытаемся получить Android ID
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        val deviceId = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            // Используем Android ID, если он валидный (не дефолтный)
            androidId
        } else {
            // Генерируем UUID если Android ID недоступен или дефолтный
            UUID.randomUUID().toString()
        }

        // Сохраняем ID для будущего использования
        prefs.edit().putString(PREFS_DEVICE_ID, deviceId).apply()

        return deviceId
    }

    /**
     * Принудительно генерирует новый ID устройства
     */
    fun generateNewDeviceId(context: Context): String {
        val newId = UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_DEVICE_ID, newId).apply()
        return newId
    }
}
