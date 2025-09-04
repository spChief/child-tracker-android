package com.spchief87.childtracker.utils

import kotlin.math.*

object LocationUtils {

    /**
     * Вычисляет расстояние между двумя точками на Земле в метрах
     * используя формулу Haversine
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Проверяет, изменилось ли местоположение значительно
     * @param newLat новая широта
     * @param newLon новая долгота
     * @param oldLat старая широта
     * @param oldLon старая долгота
     * @param minDistance минимальное расстояние в метрах для считания изменения значительным
     * @return true если местоположение изменилось значительно
     */
    fun hasLocationChanged(
        newLat: Double, newLon: Double,
        oldLat: Double, oldLon: Double,
        minDistance: Double = 10.0 // 10 метров по умолчанию
    ): Boolean {
        val distance = calculateDistance(newLat, newLon, oldLat, oldLon)
        return distance >= minDistance
    }
}
