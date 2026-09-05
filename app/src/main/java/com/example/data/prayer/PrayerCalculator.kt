package com.example.data.prayer

import com.example.data.model.MosqueConfig
import com.example.data.model.NextPrayerInfo
import com.example.data.model.PrayerName
import com.example.data.model.PrayerSchedule
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object PrayerCalculator {

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI

    private fun fixHour(a: Double): Double {
        var res = a - 24.0 * floor(a / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    /**
     * Calculates astronomical prayer times based on date and config
     */
    fun calculatePrayerTimes(calendar: Calendar, config: MosqueConfig): PrayerSchedule {
        val lat = config.latitude
        val lng = config.longitude
        val timezone = config.timezone

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian date calculation
        val a = floor((14.0 - month) / 12.0)
        val y = year + 4800.0 - a
        val m = month + 12.0 * a - 3.0
        val julianDate = day + floor((153.0 * m + 2.0) / 5.0) + 365.0 * y + floor(y / 4.0) - floor(y / 100.0) + floor(y / 400.0) - 32045.0
        val d = julianDate - 2451545.0

        // Sun position
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2.0 * g)))

        val e = 23.439 - 0.00000036 * d
        val dd = r2d(asin(sin(d2r(e)) * sin(d2r(l))))
        val ra = fixAngle(r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l))))) / 15.0

        // Equation of time and solar noon
        val eqt = q / 15.0 - ra
        val noon = fixHour(12.0 + timezone - lng / 15.0 - eqt)

        // Fajr / Subuh angle: Kemenag RI is 20 deg, MWL is 18 deg
        val fajrAngle = when (config.calculationMethod) {
            "MWL" -> 18.0
            "EGYPT" -> 19.5
            else -> 20.0
        }
        val ishaAngle = when (config.calculationMethod) {
            "MWL" -> 17.0
            "EGYPT" -> 17.5
            else -> 18.0
        }

        // Time calculations with sun altitude
        fun sunAngleTime(angle: Double, isMorning: Boolean): Double {
            val cosH = (sin(d2r(-angle)) - sin(d2r(lat)) * sin(d2r(dd))) / (cos(d2r(lat)) * cos(d2r(dd)))
            if (cosH > 1.0 || cosH < -1.0) return noon // Polar day/night fallback
            val h = r2d(acos(cosH)) / 15.0
            return fixHour(if (isMorning) noon - h else noon + h)
        }

        // Sunrise & Sunset (accounting for atmospheric refraction 0.833 deg)
        val sunrise = sunAngleTime(0.833, true)
        val sunset = sunAngleTime(0.833, false)

        // Fajr (Subuh)
        val fajr = sunAngleTime(fajrAngle, true)

        // Dhuha (approx 4.5 degrees elevation after sunrise)
        val dhuha = sunAngleTime(-4.5, true)

        // Asr (Shafi'i/Hanbali/Maliki: shadow factor = 1, cot(a) = 1 + tan|lat - dec|)
        val asrAlt = r2d(atan(1.0 / (1.0 + tan(d2r(abs(lat - dd))))))
        val asr = sunAngleTime(-asrAlt, false)

        // Maghrib is sunset
        val maghrib = sunset

        // Isha
        val isha = sunAngleTime(ishaAngle, false)

        // Helper to convert decimal hours to HH:mm with manual offset
        fun formatWithOffset(hours: Double, offsetMinutes: Int): String {
            val totalMinutes = (hours * 60.0).roundToInt() + offsetMinutes
            val adjustedMinutes = (totalMinutes % (24 * 60) + (24 * 60)) % (24 * 60)
            val h = adjustedMinutes / 60
            val min = adjustedMinutes % 60
            return String.format(Locale.getDefault(), "%02d:%02d", h, min)
        }

        val subuhStr = formatWithOffset(fajr, config.offsetSubuh)
        // Imsak is standard 10 minutes before Subuh
        val subuhTotalMin = (fajr * 60.0).roundToInt() + config.offsetSubuh
        val imsakTotalMin = (subuhTotalMin - 10 + config.offsetImsak + 1440) % 1440
        val imsakStr = String.format(Locale.getDefault(), "%02d:%02d", imsakTotalMin / 60, imsakTotalMin % 60)

        val terbitStr = formatWithOffset(sunrise, config.offsetTerbit)
        val dhuhaStr = formatWithOffset(dhuha, config.offsetDhuha)
        val dzuhurStr = formatWithOffset(noon, config.offsetDzuhur)
        val asharStr = formatWithOffset(asr, config.offsetAshar)
        val maghribStr = formatWithOffset(maghrib, config.offsetMaghrib)
        val isyaStr = formatWithOffset(isha, config.offsetIsya)

        // Gregorian Date String in Indonesian
        val dayName = getIndonesianDayName(calendar.get(Calendar.DAY_OF_WEEK))
        val monthName = getIndonesianMonthName(calendar.get(Calendar.MONTH))
        val gregorianStr = "$dayName, $day $monthName $year"

        // Hijri Date String
        val hijriStr = HijriCalendarHelper.getHijriDateString(calendar, config.hijriAdjustmentDays)

        return PrayerSchedule(
            imsak = imsakStr,
            subuh = subuhStr,
            terbit = terbitStr,
            dhuha = dhuhaStr,
            dzuhur = dzuhurStr,
            ashar = asharStr,
            maghrib = maghribStr,
            isya = isyaStr,
            dateStr = gregorianStr,
            hijriDateStr = hijriStr
        )
    }

    fun getNextPrayerInfo(
        currentCalendar: Calendar,
        schedule: PrayerSchedule,
        config: MosqueConfig
    ): NextPrayerInfo {
        val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentCalendar.get(Calendar.MINUTE)
        val currentSecond = currentCalendar.get(Calendar.SECOND)
        val currentSecondsOfDay = currentHour * 3600 + currentMinute * 60 + currentSecond

        fun parseSeconds(timeStr: String): Int {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                return (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60
            }
            return 0
        }

        val prayerTimes = listOf(
            PrayerName.SUBUH to parseSeconds(schedule.subuh),
            PrayerName.TERBIT to parseSeconds(schedule.terbit),
            PrayerName.DHUHA to parseSeconds(schedule.dhuha),
            PrayerName.DZUHUR to parseSeconds(schedule.dzuhur),
            PrayerName.ASHAR to parseSeconds(schedule.ashar),
            PrayerName.MAGHRIB to parseSeconds(schedule.maghrib),
            PrayerName.ISYA to parseSeconds(schedule.isya)
        )

        // Find next prayer today or next day Fajr
        var nextPrayer = prayerTimes.firstOrNull { it.second > currentSecondsOfDay }
        var diffSeconds: Long

        if (nextPrayer != null) {
            diffSeconds = (nextPrayer.second - currentSecondsOfDay).toLong()
        } else {
            // Next prayer is tomorrow's Subuh
            nextPrayer = prayerTimes.first()
            diffSeconds = (86400 - currentSecondsOfDay + nextPrayer.second).toLong()
        }

        val hours = diffSeconds / 3600
        val mins = (diffSeconds % 3600) / 60
        val secs = diffSeconds % 60
        val countdown = String.format(Locale.getDefault(), "-%02d:%02d:%02d", hours, mins, secs)

        // Check if exact prayer time (e.g. within 0 - 30 seconds after prayer arrival)
        val isAdhan = diffSeconds == 0L || (diffSeconds < 0 && diffSeconds >= -30)

        val targetTimeStr = when (nextPrayer.first) {
            PrayerName.IMSAK -> schedule.imsak
            PrayerName.SUBUH -> schedule.subuh
            PrayerName.TERBIT -> schedule.terbit
            PrayerName.DHUHA -> schedule.dhuha
            PrayerName.DZUHUR -> schedule.dzuhur
            PrayerName.ASHAR -> schedule.ashar
            PrayerName.MAGHRIB -> schedule.maghrib
            PrayerName.ISYA -> schedule.isya
        }

        return NextPrayerInfo(
            prayer = nextPrayer.first,
            timeStr = targetTimeStr,
            remainingSeconds = diffSeconds,
            formattedCountdown = countdown,
            isAdhanTime = isAdhan
        )
    }

    private fun getIndonesianDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Ahad"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jum'at"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
    }

    private fun getIndonesianMonthName(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "Januari"
            Calendar.FEBRUARY -> "Februari"
            Calendar.MARCH -> "Maret"
            Calendar.APRIL -> "April"
            Calendar.MAY -> "Mei"
            Calendar.JUNE -> "Juni"
            Calendar.JULY -> "Juli"
            Calendar.AUGUST -> "Agustus"
            Calendar.SEPTEMBER -> "September"
            Calendar.OCTOBER -> "Oktober"
            Calendar.NOVEMBER -> "November"
            Calendar.DECEMBER -> "Desember"
            else -> ""
        }
    }
}

object HijriCalendarHelper {
    private val HIJRI_MONTHS = listOf(
        "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Akhir",
        "Jumadil Awwal", "Jumadil Akhir", "Rajab", "Sya'ban",
        "Ramadhan", "Syawwal", "Dzulqa'dah", "Dzulhijjah"
    )

    fun getHijriDateString(calendar: Calendar, adjustmentDays: Int = 0): String {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_MONTH, adjustmentDays)
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Kuweit / Islamic algorithmic approximation
        var jd = (1461 * (year + 4800 + (month - 14) / 12)) / 4 +
                (367 * (month - 2 - 12 * ((month - 14) / 12))) / 12 -
                (3 * ((year + 4900 + (month - 14) / 12) / 100)) / 4 +
                day - 32075

        val l = jd - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val l1 = l - 10631 * n + 354
        val j = (((10985 - l1) / 5316) * ((50 * l1) / 17719) + (l1 / 5670) * ((43 * l1) / 15238)).toInt()
        val l2 = l1 - (((30 - j) / 15) * ((17719 * j) / 50) + (j / 16) * ((15238 * j) / 43)) + 29
        val m = ((24 * l2) / 709).toInt()
        val d = (l2 - (709 * m) / 24).toInt()
        val y = (30 * n + j - 30).toInt()

        val hMonthIndex = (m - 1).coerceIn(0, 11)
        val monthName = HIJRI_MONTHS[hMonthIndex]
        return "$d $monthName $y H"
    }
}
