package com.example

import com.example.data.model.MosqueConfig
import com.example.data.prayer.PrayerCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun testPrayerTimesCalculation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 27, 12, 0, 0)
        }
        val configJakarta = MosqueConfig(
            latitude = -6.2088,
            longitude = 106.8456,
            timezone = 7.0,
            offsetSubuh = 2,
            offsetDzuhur = 2,
            offsetAshar = 2,
            offsetMaghrib = 2,
            offsetIsya = 2
        )
        val schedule = PrayerCalculator.calculatePrayerTimes(cal, configJakarta)
        println("=== JADWAL SHOLAT JAKARTA 27 AGUSTUS ===")
        println("Subuh   : ${schedule.subuh}")
        println("Terbit  : ${schedule.terbit}")
        println("Dzuhur  : ${schedule.dzuhur}")
        println("Ashar   : ${schedule.ashar}")
        println("Maghrib : ${schedule.maghrib}")
        println("Isya    : ${schedule.isya}")

        // Verify Ashar is around 15:xx (not 14:xx)
        assertTrue("Ashar must be between 15:00 and 15:30", schedule.ashar.startsWith("15:"))
    }
}
