package com.example.notification

import com.example.data.model.FridaySchedule
import com.example.data.model.MosqueActivity
import com.example.data.model.MosqueConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WaSendResult(
    val success: Boolean,
    val message: String,
    val target: String = "",
    val detail: String = ""
)

data class WaDeviceStatus(
    val success: Boolean,
    val status: String, // "connect", "disconnect", "error"
    val deviceName: String = "",
    val devicePhone: String = "",
    val quota: String = "",
    val expired: String = "",
    val message: String = ""
)

object WhatsAppGatewayManager {

    private const val FONNTE_SEND_URL = "https://api.fonnte.com/send"
    private const val FONNTE_DEVICE_URL = "https://api.fonnte.com/device"

    val DEFAULT_TEMPLATE_THURSDAY = """
Assalamu'alaikum Warahmatullahi Wabarakatuh,

Yth. *{nama_petugas}*,
Mengingatkan jadwal antum besok sebagai *{peran}* pada Sholat Jum'at di *{nama_masjid}*:

📅 *Hari/Tgl:* {tanggal} ({hijriah})
⏰ *Waktu Sholat:* {waktu_sholat}

Mohon konfirmasi kehadirannya. Jazakumullahu khairan katsiran.

— *Takmir {nama_masjid}*
    """.trimIndent()

    val DEFAULT_TEMPLATE_FRIDAY = """
Assalamu'alaikum Warahmatullahi Wabarakatuh,

Yth. *{nama_petugas}*,
Mengingatkan kembali jadwal tugas Sholat Jum'at hari ini di *{nama_masjid}* sebagai *{peran}*.

📅 *Hari/Tgl:* {tanggal}
⏰ *Waktu Sholat:* {waktu_sholat}

Diharapkan hadir 15-20 menit sebelum waktu adzan. Terima kasih.

— *Takmir {nama_masjid}*
    """.trimIndent()

    val DEFAULT_TEMPLATE_KAJIAN = """
Assalamu'alaikum Warahmatullahi Wabarakatuh,

Yth. *{pemateri}*,
Mengingatkan jadwal agenda dakwah / kajian di *{nama_masjid}*:

📖 *Tema:* {nama_kajian}
📅 *Waktu:* {waktu_kajian}
📍 *Tempat:* {tempat_kajian}

Mohon konfirmasi kehadirannya. Jazakumullahu khairan katsiran.

— *Takmir {nama_masjid}*
    """.trimIndent()

    val DEFAULT_TEMPLATE_TARAWIH = """
Assalamu'alaikum Warahmatullahi Wabarakatuh,

Yth. *{nama_petugas}*,
Mengingatkan jadwal antum pada Sholat Tarawih & Witir *Malam ke-{malam} Ramadhan* di *{nama_masjid}*:

📋 *Tugas:* {peran}
📅 *Tanggal:* {tanggal} ({malam_ramadhan})
⏰ *Waktu:* Ba'da Sholat Isya' ({waktu_isya})
📖 *Judul Kultum:* {judul_kultum}

Mohon konfirmasi kehadirannya dan hadir sebelum Sholat Isya'. Jazakumullahu khairan katsiran.

— *Panitia Ramadhan {nama_masjid}*
    """.trimIndent()

    /**
     * Normalizes Indonesian phone numbers into standard international format without '+' (e.g. 628123456789)
     */
    fun normalizePhoneNumber(phone: String): String {
        var clean = phone.replace(Regex("[^0-9]"), "").trim()
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1)
        } else if (clean.startsWith("8")) {
            clean = "62" + clean
        }
        return clean
    }

    /**
     * Renders a Friday Officer message template with contextual values.
     */
    fun formatFridayMessage(
        template: String,
        config: MosqueConfig,
        schedule: FridaySchedule,
        officerName: String,
        roleName: String,
        prayerTimeStr: String = "11:50 WIB"
    ): String {
        val tpl = if (template.isNotBlank()) template else DEFAULT_TEMPLATE_THURSDAY
        return tpl
            .replace("{nama_masjid}", config.mosqueName.ifBlank { "Masjid" })
            .replace("{nama_petugas}", officerName.ifBlank { "Petugas Sholat" })
            .replace("{peran}", roleName)
            .replace("{tanggal}", schedule.date.ifBlank { "Jum'at" })
            .replace("{hijriah}", schedule.hijriDate.ifBlank { "Jum'at Barakah" })
            .replace("{judul_khutbah}", schedule.khutbahTopic.ifBlank { "" })
            .replace("{waktu_sholat}", prayerTimeStr)
    }

    /**
     * Renders a Kajian / Activity message template.
     */
    fun formatKajianMessage(
        template: String,
        config: MosqueConfig,
        activity: MosqueActivity
    ): String {
        val tpl = if (template.isNotBlank()) template else DEFAULT_TEMPLATE_KAJIAN
        val waktu = "${activity.date} pukul ${activity.time}".trim()
        return tpl
            .replace("{nama_masjid}", config.mosqueName.ifBlank { "Masjid" })
            .replace("{pemateri}", activity.speaker.ifBlank { "Ustadz Pemateri" })
            .replace("{nama_kajian}", activity.title.ifBlank { "Kajian Masjid" })
            .replace("{waktu_kajian}", waktu)
            .replace("{tempat_kajian}", activity.location.ifBlank { "Ruang Utama Masjid" })
    }

    /**
     * Renders a Tarawih Officer message template.
     */
    fun formatTarawihMessage(
        template: String = DEFAULT_TEMPLATE_TARAWIH,
        config: MosqueConfig,
        schedule: com.example.data.model.TarawihSchedule,
        officerName: String,
        roleName: String,
        isyaTimeStr: String = "19:15 WIB"
    ): String {
        val tpl = if (template.isNotBlank()) template else DEFAULT_TEMPLATE_TARAWIH
        val judul = if (schedule.judulKultum.isNotBlank()) schedule.judulKultum else "-"
        return tpl
            .replace("{nama_masjid}", config.mosqueName.ifBlank { "Masjid" })
            .replace("{nama_petugas}", officerName.ifBlank { "Petugas Sholat" })
            .replace("{peran}", roleName)
            .replace("{malam}", schedule.night.toString())
            .replace("{malam_ramadhan}", "Malam ke-${schedule.night} Ramadhan")
            .replace("{tanggal}", schedule.date.ifBlank { "Malam ke-${schedule.night}" })
            .replace("{judul_kultum}", judul)
            .replace("{waktu_isya}", isyaTimeStr)
    }

    /**
     * Send a single WhatsApp message via Fonnte API Gateway.
     */
    suspend fun sendMessage(
        token: String,
        targetPhone: String,
        messageText: String
    ): WaSendResult = withContext(Dispatchers.IO) {
        val cleanPhone = normalizePhoneNumber(targetPhone)
        if (cleanPhone.length < 9) {
            return@withContext WaSendResult(
                success = false,
                message = "Nomor WhatsApp ($targetPhone) tidak valid",
                target = targetPhone
            )
        }
        if (token.isBlank()) {
            return@withContext WaSendResult(
                success = false,
                message = "Token Fonnte belum diisi di Pengaturan",
                target = cleanPhone
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(FONNTE_SEND_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                doInput = true
                setRequestProperty("Authorization", token.trim())
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }

            val postData = "target=" + URLEncoder.encode(cleanPhone, "UTF-8") +
                    "&message=" + URLEncoder.encode(messageText, "UTF-8") +
                    "&countryCode=62"

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(postData)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = BufferedReader(InputStreamReader(inputStream ?: connection.inputStream, "UTF-8")).use {
                it.readText()
            }

            val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
            val status = json.optBoolean("status", false)
            val reason = json.optString("reason", json.optString("detail", json.optString("message", "")))

            if (responseCode in 200..299 && status) {
                WaSendResult(
                    success = true,
                    message = "Pesan berhasil dikirim ke $cleanPhone",
                    target = cleanPhone,
                    detail = responseBody
                )
            } else {
                val errMsg = if (reason.isNotBlank()) reason else "Gagal kirim (HTTP $responseCode)"
                WaSendResult(
                    success = false,
                    message = "Gagal kirim ke $cleanPhone: $errMsg",
                    target = cleanPhone,
                    detail = responseBody
                )
            }
        } catch (e: Exception) {
            WaSendResult(
                success = false,
                message = "Koneksi ke Fonnte terputus: ${e.message}",
                target = cleanPhone,
                detail = e.toString()
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Check Fonnte Account & Device Status.
     */
    suspend fun checkDeviceStatus(token: String): WaDeviceStatus = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext WaDeviceStatus(
                success = false,
                status = "error",
                message = "API Token Fonnte belum diisi"
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(FONNTE_DEVICE_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doInput = true
                setRequestProperty("Authorization", token.trim())
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = BufferedReader(InputStreamReader(inputStream ?: connection.inputStream, "UTF-8")).use {
                it.readText()
            }

            val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
            val status = json.optBoolean("status", false)
            val devStatus = json.optString("device_status", if (status) "connect" else "disconnect")
            val name = json.optString("name", json.optString("device", ""))
            val devicePhone = json.optString("device", "")
            val quota = json.optString("quota", "-")
            val expired = json.optString("expired", "-")
            val msg = json.optString("message", json.optString("reason", ""))

            if (status) {
                WaDeviceStatus(
                    success = true,
                    status = devStatus,
                    deviceName = name,
                    devicePhone = devicePhone,
                    quota = quota,
                    expired = expired,
                    message = if (devStatus.equals("connect", true)) "WhatsApp Terhubung & Siap Digunakan" else "WhatsApp $devStatus"
                )
            } else {
                WaDeviceStatus(
                    success = false,
                    status = "error",
                    message = if (msg.isNotBlank()) msg else "Token tidak valid (HTTP $responseCode)"
                )
            }
        } catch (e: Exception) {
            WaDeviceStatus(
                success = false,
                status = "error",
                message = "Gagal menghubungi server Fonnte: ${e.message}"
            )
        } finally {
            connection?.disconnect()
        }
    }
}
