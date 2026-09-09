package com.example.server

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.audio.BuzzerSoundPlayer
import com.example.audio.MurottalAudioPlayer
import com.example.data.model.FridaySchedule
import com.example.data.model.MosqueConfig
import com.example.data.model.MurottalPresetCatalogue
import com.example.data.model.PrayerName
import com.example.data.model.TransactionType
import com.example.data.model.YouTubeLiveHelper
import com.example.data.prayer.IndonesiaCityData
import com.example.data.repository.MasjidRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class LocalHttpServer(
    private val port: Int = 8080,
    private val repository: MasjidRepository,
    private val scope: CoroutineScope,
    private val context: Context? = null,
    private val onTriggerAction: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val activeTokens = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d("LocalHttpServer", "MasjidKU Web Server running on port $port")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    try {
                        clientSocket.soTimeout = 120_000
                        clientSocket.tcpNoDelay = true
                    } catch (_: Exception) {}
                    scope.launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalHttpServer", "Server exception: ${e.message}")
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
    }

    private fun getStaticToken(password: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest("masjidku_secret_$password".toByteArray(StandardCharsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "masjidku_token_$password"
        }
    }

    private suspend fun isAuthorized(headers: Map<String, String>, pathWithQuery: String, body: String): Boolean {
        val config = repository.configFlow.first()
        val staticToken = getStaticToken(config.webAdminPassword)

        fun check(token: String): Boolean {
            if (token.isBlank()) return false
            return token == staticToken || activeTokens.contains(token)
        }

        val authHeader = headers["authorization"] ?: ""
        if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
            val token = authHeader.substringAfter("Bearer ").trim()
            if (check(token)) return true
        }

        val xToken = headers["x-auth-token"] ?: ""
        if (check(xToken)) return true

        val cookie = headers["cookie"] ?: ""
        if (cookie.contains("masjidku_token=")) {
            val token = cookie.substringAfter("masjidku_token=").substringBefore(";").trim()
            if (check(token)) return true
        }

        val query = pathWithQuery.substringAfter("?", "")
        if (query.isNotEmpty()) {
            val qParams = parseParams(query)
            val token = qParams["token"] ?: ""
            if (check(token)) return true
        }

        if (body.isNotEmpty()) {
            try {
                val bParams = parseParams(body)
                val token = bParams["token"] ?: ""
                if (check(token)) return true
            } catch (_: Exception) {}
        }

        return false
    }

    private fun sendUnauthorized(output: OutputStream) {
        val json = "{\"status\":\"unauthorized\",\"message\":\"Akses ditolak. Sesi login berakhir atau PIN salah. Silakan login kembali.\"}"
        sendResponse(output, "401 Unauthorized", "application/json; charset=UTF-8", json.toByteArray(StandardCharsets.UTF_8))
    }

    private suspend fun handleLogin(body: String): String {
        val params = parseParams(body)
        val inputPassword = params["password"] ?: ""
        val config = repository.configFlow.first()
        val json = JSONObject()
        if (inputPassword.isNotEmpty() && inputPassword == config.webAdminPassword) {
            val token = getStaticToken(config.webAdminPassword)
            activeTokens.add(token)
            json.put("status", "ok")
            json.put("token", token)
            json.put("message", "Login berhasil! Selamat datang pengurus masjid.")
        } else {
            json.put("status", "error")
            json.put("message", "PIN / Password salah! Periksa kembali PIN di layar TV.")
        }
        return json.toString()
    }

    private suspend fun handleChangePassword(body: String): String {
        val params = parseParams(body)
        val oldPass = params["oldPassword"] ?: ""
        val newPass = params["newPassword"] ?: ""
        val confirmPass = params["confirmPassword"] ?: ""
        val config = repository.configFlow.first()
        val json = JSONObject()

        when {
            oldPass != config.webAdminPassword -> {
                json.put("status", "error")
                json.put("message", "PIN / Password lama tidak sesuai!")
            }
            newPass.length < 4 -> {
                json.put("status", "error")
                json.put("message", "PIN / Password baru minimal 4 karakter / angka!")
            }
            newPass != confirmPass -> {
                json.put("status", "error")
                json.put("message", "Konfirmasi PIN baru tidak cocok!")
            }
            else -> {
                val updated = config.copy(webAdminPassword = newPass)
                repository.saveConfig(updated)
                json.put("status", "ok")
                json.put("message", "PIN / Password berhasil diubah! Gunakan PIN baru ini untuk login berikutnya.")
            }
        }
        return json.toString()
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val output: OutputStream = socket.getOutputStream()

            // 1. Read HTTP request line and headers safely (max 32KB for headers)
            val headerBaos = java.io.ByteArrayOutputStream()
            var prev1 = -1
            var prev2 = -1
            var prev3 = -1
            var b: Int
            while (input.read().also { b = it } != -1) {
                headerBaos.write(b)
                if (prev3 == '\r'.code && prev2 == '\n'.code && prev1 == '\r'.code && b == '\n'.code) {
                    break
                }
                prev3 = prev2
                prev2 = prev1
                prev1 = b
                if (headerBaos.size() > 32768) break
            }

            val headerBytes = headerBaos.toByteArray()
            if (headerBytes.isEmpty()) return

            val headerText = String(headerBytes, StandardCharsets.UTF_8)
            val lines = headerText.split("\r\n")
            if (lines.isEmpty()) return

            val requestLine = lines[0]
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val pathWithQuery = parts[1]
            val path = pathWithQuery.substringBefore("?")
            val queryParams = parseQueryString(pathWithQuery.substringAfter("?", ""))

            // Handle HTTP OPTIONS preflight (CORS)
            if (method.equals("OPTIONS", ignoreCase = true)) {
                val corsHeaders = "HTTP/1.1 200 OK\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n" +
                        "Access-Control-Allow-Headers: *\r\n" +
                        "Access-Control-Max-Age: 86400\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(corsHeaders.toByteArray(StandardCharsets.UTF_8))
                output.flush()
                return
            }

            var contentLength = 0
            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) continue
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val hName = line.substring(0, colonIdx).trim().lowercase()
                    val hVal = line.substring(colonIdx + 1).trim()
                    headers[hName] = hVal
                    if (hName == "content-length") {
                        contentLength = hVal.toIntOrNull() ?: 0
                    }
                }
            }

            // Extend socket timeout for file uploads (10 minutes)
            if (path.startsWith("/api/upload-")) {
                try {
                    socket.soTimeout = 600_000
                } catch (_: Exception) {}
            }

            // Handle HTTP 100 Continue (Essential for mobile browsers sending binary uploads)
            if (headers["expect"]?.contains("100-continue", ignoreCase = true) == true) {
                output.write("HTTP/1.1 100 Continue\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
                output.flush()
            }

            // Stream media file serving directly to output (Zero RAM allocation)
            if (path.startsWith("/media/murottal/")) {
                handleServeMurottalAudio(path, output)
                return
            }
            if (path.startsWith("/media/video/")) {
                handleServeVideo(path, output)
                return
            }
            if (path.startsWith("/media/")) {
                handleServeMedia(path, output)
                return
            }

            // Direct binary stream file upload to disk (Zero OOM risk)
            if (path == "/api/upload-murottal" && method == "POST") {
                if (!isAuthorized(headers, pathWithQuery, "")) {
                    sendUnauthorized(output)
                } else {
                    val res = handleStreamUploadMurottal(input, contentLength, queryParams, headers)
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                }
                return
            }

            // Direct binary stream video upload (MP4/MKV/WebM) to disk (Zero OOM risk)
            if (path == "/api/upload-video" && method == "POST") {
                if (!isAuthorized(headers, pathWithQuery, "")) {
                    sendUnauthorized(output)
                } else {
                    val res = handleStreamUploadVideo(input, contentLength, queryParams, headers)
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                }
                return
            }

            // Upload logo masjid (binary stream, zero OOM)
            if (path == "/api/upload-logo" && method == "POST") {
                if (!isAuthorized(headers, pathWithQuery, "")) {
                    sendUnauthorized(output)
                } else {
                    val res = handleStreamUploadLogo(input, contentLength, queryParams)
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                }
                return
            }

            // Hapus logo masjid (reset ke default)
            if (path == "/api/delete-logo" && method == "POST") {
                if (!isAuthorized(headers, pathWithQuery, "")) {
                    sendUnauthorized(output)
                } else {
                    val res = handleDeleteLogo()
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                }
                return
            }

            // For other endpoints, read body with safety limit (max 5MB)
            var body = ""
            if (contentLength > 0) {
                val maxBodySize = minOf(contentLength, 5 * 1024 * 1024)
                val buffer = ByteArray(maxBodySize)
                var bytesRead = 0
                while (bytesRead < maxBodySize) {
                    val read = input.read(buffer, bytesRead, maxBodySize - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }
                var excess = contentLength - bytesRead
                while (excess > 0) {
                    val skipped = input.skip(excess.toLong())
                    if (skipped <= 0) break
                    excess -= skipped.toInt()
                }
                body = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
            }

            when {
                path == "/" || path == "/index.html" -> {
                    val html = buildDashboardHtml()
                    sendResponse(output, "200 OK", "text/html; charset=UTF-8", html.toByteArray(StandardCharsets.UTF_8))
                }
                path == "/api/login" && method == "POST" -> {
                    val res = handleLogin(body)
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                }
                path == "/api/logout" && method == "POST" -> {
                    val authHeader = headers["authorization"] ?: ""
                    val token = if (authHeader.startsWith("Bearer ", true)) authHeader.substringAfter("Bearer ").trim() else headers["x-auth-token"] ?: ""
                    if (token.isNotEmpty()) activeTokens.remove(token)
                    sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Berhasil logout\"}".toByteArray())
                }
                path == "/api/check-session" -> {
                    val valid = isAuthorized(headers, pathWithQuery, body)
                    val res = "{\"status\":\"${if (valid) "ok" else "unauthorized"}\",\"authenticated\":$valid}"
                    sendResponse(output, if (valid) "200 OK" else "401 Unauthorized", "application/json", res.toByteArray())
                }
                path == "/api/change-password" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleChangePassword(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/status" -> {
                    val json = buildStatusJson()
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", json.toByteArray(StandardCharsets.UTF_8))
                }
                path == "/api/save-config" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleSaveConfig(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Pengaturan berhasil disimpan!\"}".toByteArray())
                    }
                }
                path == "/api/add-finance" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleAddFinance(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Transaksi berhasil dicatat!\"}".toByteArray())
                    }
                }
                path == "/api/update-finance" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleUpdateFinance(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Transaksi berhasil diperbarui!\"}".toByteArray())
                    }
                }
                path == "/api/delete-finance" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleDeleteFinance(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/save-friday" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleSaveFriday(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Petugas Jum'at berhasil diperbarui!\"}".toByteArray())
                    }
                }
                path == "/api/save-tarawih" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleSaveTarawih(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Jadwal Tarawih Ramadhan berhasil disimpan!\"}".toByteArray())
                    }
                }
                path == "/api/populate-tarawih" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        repository.populateDefaultTarawihSchedules()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"30 Malam Tarawih berhasil di-generate!\"}".toByteArray())
                    }
                }
                path == "/api/add-imam-schedule" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleAddImamSchedule(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Jadwal imam khusus berhasil disimpan!\"}".toByteArray())
                    }
                }
                path == "/api/delete-imam-schedule" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleDeleteImamSchedule(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/add-activity" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleAddActivity(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Kegiatan berhasil ditambahkan!\"}".toByteArray())
                    }
                }
                path == "/api/update-activity" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleUpdateActivity(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Kegiatan berhasil diperbarui!\"}".toByteArray())
                    }
                }
                path == "/api/delete-activity" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleDeleteActivity(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/add-running-text" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleAddRunningText(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/update-running-text" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleUpdateRunningText(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/delete-running-text" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleDeleteRunningText(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/upload-media" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleUploadMedia(body)
                        sendResponse(output, "200 OK", "application/json", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/update-media-slide" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleUpdateMediaSlide(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/delete-media-slide" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleDeleteMediaSlide(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/remove-background" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleRemoveBackground()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/remove-finance-bg" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleRemoveFinanceBg()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/remove-friday-bg" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleRemoveFridayBg()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/remove-countdown-bg" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleRemoveCountdownBg()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/remove-qris" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleRemoveQris()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/set-background-dim" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleSetBackgroundDim(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/videos" && method == "GET" -> {
                    val res = handleGetVideos()
                    sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                }
                path == "/api/delete-video" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleDeleteVideo(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/set-background-video" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSetBackgroundVideo(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/play-fullscreen-video" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handlePlayFullscreenVideo(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/stop-fullscreen-video" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleStopFullscreenVideo()
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/save-murottal" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleSaveMurottalConfig(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Pengaturan Murottal berhasil disimpan!\"}".toByteArray())
                    }
                }
                path == "/api/play-murottal" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handlePlayMurottal(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/stop-murottal" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        MurottalAudioPlayer.stop()
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Pemutaran murottal dihentikan.\"}".toByteArray())
                    }
                }
                path == "/api/delete-murottal" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        handleDeleteMurottal(body)
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\"}".toByteArray())
                    }
                }
                path == "/api/set-default-murottal" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val params = parseParams(body)
                        val id = (queryParams["id"] ?: params["id"])?.toLongOrNull()
                        if (id != null) {
                            repository.setDefaultMurottalAudio(id)
                            sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Audio murottal berhasil diaktifkan sebagai audio utama di TV!\"}".toByteArray())
                        } else {
                            sendResponse(output, "400 Bad Request", "application/json", "{\"status\":\"error\",\"message\":\"ID audio tidak valid\"}".toByteArray())
                        }
                    }
                }
                path == "/api/select-murottal-preset" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val params = parseParams(body)
                        val presetId = params["presetId"] ?: ""
                        if (presetId.isNotBlank()) {
                            repository.setMurottalPreset(presetId)
                            sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"message\":\"Preset murottal berhasil diaktifkan di TV!\"}".toByteArray())
                        } else {
                            sendResponse(output, "400 Bad Request", "application/json", "{\"status\":\"error\",\"message\":\"Preset ID tidak valid\"}".toByteArray())
                        }
                    }
                }
                path == "/api/save-youtube-live" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSaveYoutubeLive(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/toggle-youtube-live" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleToggleYoutubeLive(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/save-cctv-config" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSaveCctvConfig(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/play-fullscreen-cctv" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handlePlayFullscreenCctv(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/stop-fullscreen-cctv" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleStopFullscreenCctv()
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/save-cctv-cameras" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSaveCctvCameras(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/switch-cctv-camera" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSwitchCctvCamera(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/save-cctv-schedule" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSaveCctvSchedule(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/set-background-cctv" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSetBackgroundCctv(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/wa-gateway/config" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSaveWaGatewayConfig(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/wa-gateway/check-status" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleCheckWaDeviceStatus(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/wa-gateway/send-test" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSendTestWa(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/wa-gateway/send-friday-reminder" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSendFridayBroadcast(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/wa-gateway/send-activity-reminder" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSendActivityReminder(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path == "/api/wa-gateway/send-tarawih-reminder" && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val res = handleSendWaTarawih(body)
                        sendResponse(output, "200 OK", "application/json; charset=UTF-8", res.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                path.startsWith("/media/murottal/") -> {
                    handleServeMurottalAudio(path, output)
                }
                path.startsWith("/media/") -> {
                    handleServeMedia(path, output)
                }
                path.startsWith("/api/trigger-") && method == "POST" -> {
                    if (!isAuthorized(headers, pathWithQuery, body)) {
                        sendUnauthorized(output)
                    } else {
                        val action = path.removePrefix("/api/trigger-")
                        onTriggerAction(action)
                        if (action == "sound") {
                            BuzzerSoundPlayer.playAdhanArrivalChime(scope)
                        }
                        sendResponse(output, "200 OK", "application/json", "{\"status\":\"ok\",\"action\":\"$action\"}".toByteArray())
                    }
                }
                else -> {
                    val notFound = "404 Not Found"
                    sendResponse(output, "404 Not Found", "text/plain", notFound.toByteArray())
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendResponse(output: OutputStream, status: String, contentType: String, data: ByteArray) {
        val header = "HTTP/1.1 $status\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${data.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(data)
        output.flush()
    }

    private fun handleServeMedia(path: String, output: OutputStream) {
        val relPath = path.removePrefix("/media/").substringBefore("?")
        val file = if (relPath.startsWith("logo/")) {
            val logoName = relPath.removePrefix("logo/")
            context?.filesDir?.resolve("logo")?.resolve(logoName)
        } else {
            val f1 = context?.filesDir?.resolve("media")?.resolve(relPath)
            if (f1 != null && f1.exists()) f1 else context?.filesDir?.resolve(relPath)
        }
        if (file != null && file.exists() && file.isFile) {
            val mimeType = when {
                file.name.endsWith(".png", true) -> "image/png"
                file.name.endsWith(".webp", true) -> "image/webp"
                file.name.endsWith(".gif", true) -> "image/gif"
                else -> "image/jpeg"
            }
            val headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: $mimeType\r\n" +
                    "Content-Length: ${file.length()}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            output.write(headers.toByteArray(StandardCharsets.UTF_8))
            val buf = ByteArray(16384)
            file.inputStream().use { fis ->
                var len: Int
                while (fis.read(buf).also { len = it } != -1) {
                    output.write(buf, 0, len)
                }
            }
            output.flush()
        } else {
            sendResponse(output, "404 Not Found", "text/plain", "File not found".toByteArray())
        }
    }

    private fun handleServeMurottalAudio(path: String, output: OutputStream) {
        val filename = path.removePrefix("/media/murottal/").substringBefore("?")
        val file = context?.filesDir?.resolve("murottal")?.resolve(filename)
        if (file != null && file.exists() && file.isFile) {
            val mimeType = when {
                filename.endsWith(".mp3", true) -> "audio/mpeg"
                filename.endsWith(".wav", true) -> "audio/wav"
                filename.endsWith(".m4a", true) -> "audio/mp4"
                filename.endsWith(".ogg", true) -> "audio/ogg"
                else -> "audio/mpeg"
            }
            val headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: $mimeType\r\n" +
                    "Content-Length: ${file.length()}\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            output.write(headers.toByteArray(StandardCharsets.UTF_8))
            val buf = ByteArray(16384)
            file.inputStream().use { fis ->
                var len: Int
                while (fis.read(buf).also { len = it } != -1) {
                    output.write(buf, 0, len)
                }
            }
            output.flush()
        } else {
            sendResponse(output, "404 Not Found", "text/plain", "Audio file not found".toByteArray())
        }
    }

    private fun handleServeVideo(path: String, output: OutputStream) {
        val filename = path.removePrefix("/media/video/").substringBefore("?")
        val file = context?.filesDir?.resolve("videos")?.resolve(filename)
        if (file != null && file.exists() && file.isFile) {
            val mimeType = when {
                filename.endsWith(".mp4", true) -> "video/mp4"
                filename.endsWith(".webm", true) -> "video/webm"
                filename.endsWith(".mkv", true) -> "video/x-matroska"
                filename.endsWith(".avi", true) -> "video/x-msvideo"
                filename.endsWith(".mov", true) -> "video/quicktime"
                else -> "video/mp4"
            }
            val headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: $mimeType\r\n" +
                    "Content-Length: ${file.length()}\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            output.write(headers.toByteArray(StandardCharsets.UTF_8))
            val buf = ByteArray(65536)
            file.inputStream().use { fis ->
                var len: Int
                while (fis.read(buf).also { len = it } != -1) {
                    output.write(buf, 0, len)
                }
            }
            output.flush()
        } else {
            sendResponse(output, "404 Not Found", "text/plain", "Video file not found".toByteArray())
        }
    }

    private suspend fun handleStreamUploadMurottal(
        input: java.io.InputStream,
        contentLength: Int,
        queryParams: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): String {
        val titleParam = queryParams["title"]?.trim()?.ifBlank { null }
            ?: headers["x-audio-title"]?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } }?.trim()?.ifBlank { null }
        val qariParam = queryParams["qari"]?.trim()?.ifBlank { null }
            ?: headers["x-audio-qari"]?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } }?.trim()?.ifBlank { null }
        val surahParam = queryParams["surah"]?.trim()?.ifBlank { null }
            ?: headers["x-audio-surah"]?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } }?.trim()?.ifBlank { null }
        val prayerTime = queryParams["prayerTime"]?.trim()?.ifBlank { null }
            ?: headers["x-audio-prayer"]?.trim()?.ifBlank { null }
            ?: "ALL"
        val ext = queryParams["ext"]?.trim()?.lowercase()?.ifBlank { null }
            ?: headers["x-audio-ext"]?.trim()?.lowercase()?.ifBlank { null }
            ?: "mp3"
        val expectedSize = queryParams["size"]?.toLongOrNull()?.takeIf { it > 0L }
            ?: headers["x-audio-size"]?.toLongOrNull()?.takeIf { it > 0L }
            ?: contentLength.toLong()

        val allowedExts = setOf("mp3", "wav", "m4a", "ogg", "aac", "flac", "opus", "3gp", "amr", "mp4", "mkv", "webm", "wma", "mid", "midi")
        if (ext !in allowedExts) {
            val errJson = JSONObject()
            errJson.put("status", "error")
            errJson.put("message", "Format audio (.$ext) tidak didukung. Gunakan MP3, WAV, M4A, OGG, AAC, atau FLAC.")
            return errJson.toString()
        }

        val murottalDir = context?.filesDir?.resolve("murottal") ?: File(System.getProperty("java.io.tmpdir"), "murottal")
        if (!murottalDir.exists()) murottalDir.mkdirs()

        val cleanTitle = (surahParam ?: titleParam ?: "murottal_${System.currentTimeMillis()}").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "${cleanTitle}_${System.currentTimeMillis()}.$ext"
        val targetFile = File(murottalDir, fileName)

        var totalWritten = 0L
        val maxAllowedSize = 130L * 1024L * 1024L // 130 MB max
        try {
            var remaining = if (expectedSize > 0L) expectedSize else -1L
            val buffer = ByteArray(65536)
            FileOutputStream(targetFile).use { fos ->
                while (remaining != 0L) {
                    val toRead = if (remaining > 0L) minOf(remaining, buffer.size.toLong()).toInt() else buffer.size
                    val read = input.read(buffer, 0, toRead)
                    if (read == -1) break
                    if (read > 0) {
                        fos.write(buffer, 0, read)
                        totalWritten += read
                        if (remaining > 0L) {
                            remaining -= read
                        }
                        if (totalWritten > maxAllowedSize) {
                            fos.close()
                            targetFile.delete()
                            val errJson = JSONObject()
                            errJson.put("status", "error")
                            errJson.put("message", "Ukuran audio melebihi batas maksimal (120 MB)")
                            return errJson.toString()
                        }
                    }
                }
                fos.flush()
            }

            if (!targetFile.exists() || targetFile.length() == 0L || totalWritten == 0L) {
                val errJson = JSONObject()
                errJson.put("status", "error")
                errJson.put("message", "File audio kosong atau gagal diterima dari HP")
                return errJson.toString()
            }

            // Extract audio duration using MediaMetadataRetriever
            var durationSeconds = 0
            try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(targetFile.absolutePath)
                val durStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durMs = durStr?.toLongOrNull() ?: 0L
                durationSeconds = (durMs / 1000).toInt()
                mmr.release()
            } catch (_: Throwable) {}

            val title = titleParam ?: "Murottal ${targetFile.nameWithoutExtension}"
            val qari = qariParam ?: "Qari Pilihan"
            val surah = surahParam ?: title

            val id = repository.addMurottalAudio(
                title = title,
                qari = qari,
                surah = surah,
                durationSeconds = durationSeconds,
                filePath = targetFile.absolutePath,
                prayerTime = prayerTime,
                isDefault = true
            )

            // Auto-set as active default custom audio and switch source to MANUAL_UPLOAD
            repository.setDefaultMurottalAudio(id)

            val sizeMb = String.format(java.util.Locale.US, "%.2f", totalWritten / (1024.0 * 1024.0))
            val resJson = JSONObject()
            resJson.put("status", "ok")
            resJson.put("message", "File audio murottal berhasil diunggah ($sizeMb MB) dan langsung diaktifkan di TV!")
            resJson.put("id", id)
            resJson.put("path", "/media/murottal/$fileName")
            return resJson.toString()
        } catch (t: Throwable) {
            try {
                if (targetFile.exists()) targetFile.delete()
            } catch (_: Throwable) {}
            val errJson = JSONObject()
            errJson.put("status", "error")
            errJson.put("message", "Gagal menyimpan audio: ${t.localizedMessage ?: "Error I/O"}")
            return errJson.toString()
        }
    }

    private suspend fun handleStreamUploadLogo(
        input: java.io.InputStream,
        contentLength: Int,
        queryParams: Map<String, String>
    ): String {
        val ext = queryParams["ext"]?.ifBlank { "png" }?.lowercase() ?: "png"
        val allowedExts = setOf("png", "jpg", "jpeg", "webp")
        if (ext !in allowedExts) {
            return "{\"status\":\"error\",\"message\":\"Format tidak didukung. Gunakan PNG, JPG, atau WEBP.\"}"
        }

        val logoDir = context?.filesDir?.resolve("logo") ?: return "{\"status\":\"error\",\"message\":\"Direktori tidak tersedia\"}"
        if (!logoDir.exists()) logoDir.mkdirs()

        val fileName = "mosque_logo.$ext"
        val targetFile = File(logoDir, fileName)
        val expectedSize = queryParams["size"]?.toIntOrNull()?.takeIf { it > 0 } ?: contentLength

        try {
            var remaining = if (expectedSize > 0) expectedSize else Int.MAX_VALUE
            val buffer = ByteArray(16384)
            var totalWritten = 0L
            val maxSize = 5 * 1024 * 1024 // 5MB max untuk logo
            FileOutputStream(targetFile).use { fos ->
                while (remaining > 0) {
                    val toRead = if (expectedSize > 0) minOf(remaining, buffer.size) else buffer.size
                    val read = input.read(buffer, 0, toRead)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    totalWritten += read
                    if (expectedSize > 0) remaining -= read
                    if (totalWritten > maxSize) {
                        fos.close()
                        targetFile.delete()
                        return "{\"status\":\"error\",\"message\":\"Ukuran logo terlalu besar. Maksimum 5MB.\"}"
                    }
                }
                fos.flush()
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                return "{\"status\":\"error\",\"message\":\"File logo kosong atau gagal diterima\"}"
            }

            // Simpan path ke config
            val current = repository.configFlow.first()
            repository.saveConfig(current.copy(customLogoImagePath = targetFile.absolutePath))

            return "{\"status\":\"ok\",\"message\":\"Logo masjid berhasil diupload!\",\"path\":\"${targetFile.absolutePath}\"}"
        } catch (t: Throwable) {
            try { if (targetFile.exists()) targetFile.delete() } catch (_: Exception) {}
            return "{\"status\":\"error\",\"message\":\"Gagal menyimpan logo: ${t.localizedMessage ?: "Error I/O"}\"}"
        }
    }

    private suspend fun handleDeleteLogo(): String {
        return try {
            val current = repository.configFlow.first()
            // Hapus file dari disk jika ada
            if (current.customLogoImagePath.isNotBlank()) {
                try { File(current.customLogoImagePath).delete() } catch (_: Exception) {}
            }
            // Reset path di config
            repository.saveConfig(current.copy(customLogoImagePath = ""))
            "{\"status\":\"ok\",\"message\":\"Logo berhasil direset ke default.\"}"
        } catch (t: Throwable) {
            "{\"status\":\"error\",\"message\":\"Gagal menghapus logo: ${t.localizedMessage ?: "Error"}\"}"
        }
    }

    private suspend fun handleSaveMurottalConfig(body: String) {
        val params = parseParams(body)
        val current = repository.configFlow.first()
        val updated = current.copy(
            murottalEnabled = params["murottalEnabled"]?.toBooleanStrictOrNull() ?: current.murottalEnabled,
            murottalSubuh = params["murottalSubuh"]?.toBooleanStrictOrNull() ?: current.murottalSubuh,
            murottalDzuhur = params["murottalDzuhur"]?.toBooleanStrictOrNull() ?: current.murottalDzuhur,
            murottalAshar = params["murottalAshar"]?.toBooleanStrictOrNull() ?: current.murottalAshar,
            murottalMaghrib = params["murottalMaghrib"]?.toBooleanStrictOrNull() ?: current.murottalMaghrib,
            murottalIsya = params["murottalIsya"]?.toBooleanStrictOrNull() ?: current.murottalIsya,
            murottalJumat = params["murottalJumat"]?.toBooleanStrictOrNull() ?: current.murottalJumat,
            murottalDurationMinutes = params["murottalDurationMinutes"]?.toIntOrNull() ?: current.murottalDurationMinutes,
            murottalVolume = params["murottalVolume"]?.toIntOrNull() ?: current.murottalVolume,
            murottalSourceType = params["murottalSourceType"] ?: current.murottalSourceType,
            murottalSelectedPresetId = params["murottalSelectedPresetId"] ?: current.murottalSelectedPresetId,
            murottalCustomAudioPath = params["murottalCustomAudioPath"] ?: current.murottalCustomAudioPath
        )
        repository.saveConfig(updated)
        MurottalAudioPlayer.setVolume(updated.murottalVolume)
    }

    private suspend fun handleUploadMurottal(body: String): String {
        val params = parseParams(body)
        val title = params["title"] ?: "Murottal Al-Qur'an"
        val qari = params["qari"] ?: "Qari Pilihan"
        val surah = params["surah"] ?: title
        val prayerTime = params["prayerTime"] ?: "ALL"
        val isDefault = params["isDefault"]?.toBooleanStrictOrNull() ?: false
        val base64Data = params["base64"] ?: return "{\"status\":\"error\",\"message\":\"Data audio kosong\"}"

        val cleanBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
        val bytes = try {
            Base64.decode(cleanBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            return "{\"status\":\"error\",\"message\":\"Format data audio tidak valid\"}"
        }

        val murottalDir = context?.filesDir?.resolve("murottal") ?: File("/tmp/murottal")
        if (!murottalDir.exists()) murottalDir.mkdirs()

        val ext = params["ext"] ?: if (base64Data.startsWith("data:audio/wav")) "wav" else "mp3"
        val fileName = "murottal_${System.currentTimeMillis()}.$ext"
        val targetFile = File(murottalDir, fileName)
        FileOutputStream(targetFile).use { it.write(bytes) }

        val id = repository.addMurottalAudio(
            title = title,
            qari = qari,
            surah = surah,
            durationSeconds = 0,
            filePath = targetFile.absolutePath,
            prayerTime = prayerTime,
            isDefault = isDefault
        )

        return "{\"status\":\"ok\",\"message\":\"File audio murottal berhasil diunggah!\",\"id\":$id,\"path\":\"/media/murottal/$fileName\"}"
    }

    private suspend fun handlePlayMurottal(body: String): String {
        val params = parseParams(body)
        val presetId = params["presetId"]
        val audioId = params["audioId"]?.toLongOrNull()
        val volume = params["volume"]?.toIntOrNull() ?: repository.configFlow.first().murottalVolume

        if (audioId != null) {
            val allAudios = repository.allMurottalAudiosFlow.first()
            val item = allAudios.find { it.id == audioId }
            if (item != null && context != null) {
                MurottalAudioPlayer.playFile(
                    context = context,
                    filePath = item.filePath,
                    title = item.title,
                    qari = item.qari,
                    surah = item.surah,
                    volumePercent = volume
                )
                return "{\"status\":\"ok\",\"message\":\"Memutar ${item.title} di TV\"}"
            }
        } else if (!presetId.isNullOrBlank()) {
            val preset = MurottalPresetCatalogue.getPresetById(presetId)
            MurottalAudioPlayer.playPreset(preset, volume)
            return "{\"status\":\"ok\",\"message\":\"Memutar ${preset.surahName} (${preset.qariName}) di TV\"}"
        } else {
            val cfg = repository.configFlow.first()
            val preset = MurottalPresetCatalogue.getPresetById(cfg.murottalSelectedPresetId)
            MurottalAudioPlayer.playPreset(preset, cfg.murottalVolume)
            return "{\"status\":\"ok\",\"message\":\"Memutar Murottal di TV\"}"
        }
        return "{\"status\":\"error\",\"message\":\"Sumber audio tidak ditemukan\"}"
    }

    private suspend fun handleDeleteMurottal(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        val allAudios = repository.allMurottalAudiosFlow.first()
        val item = allAudios.find { it.id == id }
        if (item != null) {
            try {
                val file = File(item.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        repository.deleteMurottalAudio(id)
    }

    // --- Video Lokal Handlers ---
    private suspend fun handleStreamUploadVideo(
        input: java.io.InputStream,
        contentLength: Int,
        queryParams: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): String {
        val titleParam = queryParams["title"]?.trim()?.ifBlank { null }
            ?: headers["x-video-title"]?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it } }?.trim()?.ifBlank { null }
        val ext = queryParams["ext"]?.trim()?.lowercase()?.ifBlank { null }
            ?: headers["x-video-ext"]?.trim()?.lowercase()?.ifBlank { null }
            ?: "mp4"
        val expectedSize = queryParams["size"]?.toLongOrNull()?.takeIf { it > 0L }
            ?: headers["x-video-size"]?.toLongOrNull()?.takeIf { it > 0L }
            ?: contentLength.toLong()

        val allowedExts = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "ts")
        if (ext !in allowedExts) {
            return "{\"status\":\"error\",\"message\":\"Format video tidak didukung. Gunakan MP4, MKV, atau WebM.\"}"
        }

        val videoDir = context?.filesDir?.resolve("videos") ?: File(System.getProperty("java.io.tmpdir"), "videos")
        if (!videoDir.exists()) videoDir.mkdirs()

        val cleanTitle = (titleParam ?: "video_${System.currentTimeMillis()}").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "${cleanTitle}_${System.currentTimeMillis()}.$ext"
        val targetFile = File(videoDir, fileName)

        var totalWritten = 0L
        val maxAllowedSize = 500L * 1024L * 1024L // 500 MB max for videos
        try {
            var remaining = if (expectedSize > 0L) expectedSize else -1L
            val buffer = ByteArray(65536)
            FileOutputStream(targetFile).use { fos ->
                while (remaining != 0L) {
                    val toRead = if (remaining > 0L) minOf(remaining, buffer.size.toLong()).toInt() else buffer.size
                    val read = input.read(buffer, 0, toRead)
                    if (read == -1) break
                    if (read > 0) {
                        fos.write(buffer, 0, read)
                        totalWritten += read
                        if (remaining > 0L) {
                            remaining -= read
                        }
                        if (totalWritten > maxAllowedSize) {
                            fos.close()
                            targetFile.delete()
                            return "{\"status\":\"error\",\"message\":\"Ukuran video melebihi batas maksimal (500 MB)\"}"
                        }
                    }
                }
                fos.flush()
            }

            if (!targetFile.exists() || targetFile.length() == 0L || totalWritten == 0L) {
                return "{\"status\":\"error\",\"message\":\"File video kosong atau gagal diterima\"}"
            }

            return "{\"status\":\"ok\",\"message\":\"Video berhasil di-upload ke TV!\",\"fileName\":\"$fileName\",\"filePath\":\"${targetFile.absolutePath}\"}"
        } catch (e: Exception) {
            try { targetFile.delete() } catch (_: Exception) {}
            return "{\"status\":\"error\",\"message\":\"Gagal menyimpan video: ${e.message}\"}"
        }
    }

    private fun handleGetVideos(): String {
        val videos = repository.getLocalVideos()
        val arr = JSONArray()
        for (v in videos) {
            val obj = JSONObject().apply {
                put("id", v.id)
                put("title", v.title)
                put("fileName", v.fileName)
                put("filePath", v.filePath)
                put("fileSize", v.fileSize)
                put("formattedSize", v.formattedSize)
                put("isBackgroundActive", v.isBackgroundActive)
                put("createdAt", v.createdAt)
            }
            arr.put(obj)
        }
        val root = JSONObject().apply {
            put("status", "ok")
            put("videos", arr)
        }
        return root.toString()
    }

    private suspend fun handleDeleteVideo(body: String): String {
        val params = parseParams(body)
        val fileName = params["fileName"] ?: ""
        if (fileName.isBlank()) return "{\"status\":\"error\",\"message\":\"Nama file tidak valid\"}"
        val deleted = repository.deleteLocalVideo(fileName)
        return if (deleted) "{\"status\":\"ok\",\"message\":\"Video berhasil dihapus\"}"
        else "{\"status\":\"error\",\"message\":\"Video tidak ditemukan atau gagal dihapus\"}"
    }

    private suspend fun handleSetBackgroundVideo(body: String): String {
        val params = parseParams(body)
        val filePath = params["filePath"] ?: ""
        val isEnabled = params["enabled"]?.toBooleanStrictOrNull() ?: true
        repository.setBackgroundVideo(filePath, isEnabled)
        return "{\"status\":\"ok\",\"message\":\"Pengaturan video latar belakang berhasil diperbarui!\"}"
    }

    private suspend fun handlePlayFullscreenVideo(body: String): String {
        val params = parseParams(body)
        val filePath = params["filePath"] ?: ""
        val title = params["title"] ?: ""
        val isMuted = params["isMuted"]?.toBooleanStrictOrNull() ?: false
        if (filePath.isBlank() || !File(filePath).exists()) {
            return "{\"status\":\"error\",\"message\":\"File video tidak ditemukan di penyimpanan TV\"}"
        }
        repository.playFullscreenVideo(filePath, title, isMuted)
        return "{\"status\":\"ok\",\"message\":\"Video ditayangkan secara layar penuh di TV!\"}"
    }

    private suspend fun handleStopFullscreenVideo(): String {
        repository.stopFullscreenVideo()
        onTriggerAction("stop_fullscreen_video")
        return "{\"status\":\"ok\",\"message\":\"Tayangan video dihentikan, TV kembali ke tampilan utama.\"}"
    }

    private suspend fun handleSaveYoutubeLive(body: String): String {
        val params = parseParams(body)
        val url = params["url"] ?: ""
        val title = params["title"]?.ifBlank { "Live Streaming Masjid" } ?: "Live Streaming Masjid"
        val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: false
        val showSlide = params["showSlide"]?.toBooleanStrictOrNull() ?: false
        val isMuted = parseBool(params["muted"], false)

        repository.updateYoutubeLiveConfig(url, enabled, title, showSlide, isMuted)
        return "{\"status\":\"ok\",\"message\":\"Pengaturan Live Streaming YouTube berhasil disimpan!\",\"enabled\":$enabled}"
    }

    private suspend fun handleToggleYoutubeLive(body: String): String {
        val params = parseParams(body)
        val enabled = params["enabled"]?.toBooleanStrictOrNull()
        val current = repository.configFlow.first()
        val targetEnabled = enabled ?: !current.youtubeLiveEnabled

        repository.updateYoutubeLiveConfig(
            url = current.youtubeLiveUrl,
            enabled = targetEnabled,
            title = current.youtubeLiveTitle,
            showSlide = current.showYoutubeLiveSlide,
            isMuted = current.youtubeLiveMuted
        )
        val msg = if (targetEnabled) "Live Streaming YouTube di TV AKTIF!" else "Live Streaming YouTube di TV DINONAKTIFKAN."
        return "{\"status\":\"ok\",\"message\":\"$msg\",\"enabled\":$targetEnabled}"
    }

    private suspend fun handleSaveCctvConfig(body: String): String {
        val params = parseParams(body)
        val url = params["url"] ?: ""
        val title = params["title"]?.ifBlank { "Siaran Langsung CCTV Masjid" } ?: "Siaran Langsung CCTV Masjid"
        val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: false
        val showSlide = params["showSlide"]?.toBooleanStrictOrNull() ?: false
        val isMuted = parseBool(params["muted"], true)
        val liveType = params["liveStreamType"] ?: "cctv"

        repository.updateCctvConfig(url, enabled, title, showSlide, isMuted, liveType)
        return "{\"status\":\"ok\",\"message\":\"Pengaturan RTSP CCTV Masjid berhasil disimpan!\",\"enabled\":$enabled}"
    }

    private suspend fun handlePlayFullscreenCctv(body: String): String {
        val params = parseParams(body)
        val url = params["url"] ?: ""
        val title = params["title"]?.ifBlank { "Siaran Langsung CCTV Masjid" } ?: "Siaran Langsung CCTV Masjid"
        val isMuted = parseBool(params["muted"], true)

        repository.playFullscreenCctv(url, title, isMuted)
        onTriggerAction("play_fullscreen_cctv")
        return "{\"status\":\"ok\",\"message\":\"CCTV ditayangkan di layar TV (Layar Penuh).\"}"
    }

    private suspend fun handleStopFullscreenCctv(): String {
        repository.stopFullscreenCctv()
        onTriggerAction("stop_fullscreen_cctv")
        return "{\"status\":\"ok\",\"message\":\"Tayangan CCTV dihentikan, TV kembali ke tampilan utama.\"}"
    }

    private suspend fun handleSaveCctvCameras(body: String): String {
        val params = parseParams(body)
        val camerasJson = params["camerasJson"] ?: "[]"
        val activeCameraId = params["activeCameraId"]
        val autoRotate = params["autoRotate"]?.toBooleanStrictOrNull()
        val autoRotateInterval = params["autoRotateInterval"]?.toIntOrNull()
        val displayLayout = params["displayLayout"]

        val cameras = com.example.data.model.CctvCameraItem.parseList(camerasJson)
        repository.saveCctvCameras(cameras, activeCameraId, autoRotate, autoRotateInterval, displayLayout)
        return "{\"status\":\"ok\",\"message\":\"Daftar kamera CCTV berhasil disimpan!\"}"
    }

    private suspend fun handleSwitchCctvCamera(body: String): String {
        val params = parseParams(body)
        val cameraId = params["cameraId"] ?: ""
        if (cameraId.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"ID Kamera tidak valid.\"}"
        }
        repository.switchActiveCctvCamera(cameraId)
        onTriggerAction("play_fullscreen_cctv")
        return "{\"status\":\"ok\",\"message\":\"TV beralih ke kamera yang dipilih!\"}"
    }

    private suspend fun handleSaveCctvSchedule(body: String): String {
        val params = parseParams(body)
        val autoPlayAfterPrayer = params["autoPlayAfterPrayer"]?.toBooleanStrictOrNull() ?: false
        val afterPrayerDuration = params["afterPrayerDuration"]?.toIntOrNull() ?: 30
        val afterPrayerWaktu = params["afterPrayerWaktu"] ?: "SUBUH,MAGHRIB,ISYA,JUMAT"
        val afterPrayerCameraId = params["afterPrayerCameraId"] ?: ""

        val scheduleEnabled = params["scheduleEnabled"]?.toBooleanStrictOrNull() ?: false
        val scheduleStartTime = params["scheduleStartTime"] ?: "18:30"
        val scheduleEndTime = params["scheduleEndTime"] ?: "19:30"
        val scheduleDays = params["scheduleDays"] ?: "ALL"
        val scheduleCameraId = params["scheduleCameraId"] ?: ""

        repository.saveCctvScheduleConfig(
            autoPlayAfterPrayer,
            afterPrayerDuration,
            afterPrayerWaktu,
            afterPrayerCameraId,
            scheduleEnabled,
            scheduleStartTime,
            scheduleEndTime,
            scheduleDays,
            scheduleCameraId
        )
        return "{\"status\":\"ok\",\"message\":\"Jadwal otomatis siaran CCTV berhasil disimpan!\"}"
    }

    private suspend fun handleSetBackgroundCctv(body: String): String {
        val params = parseParams(body)
        val enabled = parseBool(params["enabled"], true)
        val cameraId = params["cameraId"] ?: ""
        repository.setBackgroundCctv(cameraId, enabled)
        if (enabled) {
            repository.stopFullscreenCctv()
            onTriggerAction("stop_fullscreen_cctv")
        }
        return if (enabled) {
            "{\"status\":\"ok\",\"message\":\"Live CCTV berhasil dipasang sebagai Background Layar Utama TV!\"}"
        } else {
            "{\"status\":\"ok\",\"message\":\"Background layar TV dikembalikan ke tema preset default.\"}"
        }
    }

    private suspend fun handleSaveConfig(body: String) {
        val params = parseParams(body)
        val current = repository.configFlow.first()

        val cityName = params["city"] ?: current.city
        var lat = params["latitude"]?.toDoubleOrNull()
        var lng = params["longitude"]?.toDoubleOrNull()
        var tz = params["timezone"]?.toDoubleOrNull()

        // Auto-resolve coordinates and timezone if not explicitly provided or if city changed
        if (lat == null || lng == null || tz == null) {
            val matched = IndonesiaCityData.findCity(cityName).firstOrNull()
            if (matched != null) {
                if (lat == null) lat = matched.latitude
                if (lng == null) lng = matched.longitude
                if (tz == null) tz = matched.timezone
            }
        }

        val updated = current.copy(
            mosqueName = params["mosqueName"] ?: current.mosqueName,
            tagline = params["tagline"] ?: current.tagline,
            address = params["address"] ?: current.address,
            city = cityName,
            phone = params["phone"] ?: current.phone,
            bankName = params["bankName"] ?: current.bankName,
            bankAccount = params["bankAccount"] ?: current.bankAccount,
            bankAccountName = params["bankAccountName"] ?: current.bankAccountName,
            latitude = lat ?: current.latitude,
            longitude = lng ?: current.longitude,
            timezone = tz ?: current.timezone,
            calculationMethod = params["calculationMethod"] ?: current.calculationMethod,
            offsetImsak = params["offsetImsak"]?.toIntOrNull() ?: current.offsetImsak,
            offsetSubuh = params["offsetSubuh"]?.toIntOrNull() ?: current.offsetSubuh,
            offsetTerbit = params["offsetTerbit"]?.toIntOrNull() ?: current.offsetTerbit,
            offsetDhuha = params["offsetDhuha"]?.toIntOrNull() ?: current.offsetDhuha,
            offsetDzuhur = params["offsetDzuhur"]?.toIntOrNull() ?: current.offsetDzuhur,
            offsetAshar = params["offsetAshar"]?.toIntOrNull() ?: current.offsetAshar,
            offsetMaghrib = params["offsetMaghrib"]?.toIntOrNull() ?: current.offsetMaghrib,
            offsetIsya = params["offsetIsya"]?.toIntOrNull() ?: current.offsetIsya,
            iqomahSubuh = params["iqomahSubuh"]?.toIntOrNull() ?: current.iqomahSubuh,
            iqomahDzuhur = params["iqomahDzuhur"]?.toIntOrNull() ?: current.iqomahDzuhur,
            iqomahJumat = params["iqomahJumat"]?.toIntOrNull() ?: current.iqomahJumat,
            iqomahAshar = params["iqomahAshar"]?.toIntOrNull() ?: current.iqomahAshar,
            iqomahMaghrib = params["iqomahMaghrib"]?.toIntOrNull() ?: current.iqomahMaghrib,
            iqomahIsya = params["iqomahIsya"]?.toIntOrNull() ?: current.iqomahIsya,
            sholatDurationMinutes = params["sholatDurationMinutes"]?.toIntOrNull() ?: current.sholatDurationMinutes,
            hijriAdjustmentDays = params["hijriAdjustmentDays"]?.toIntOrNull() ?: current.hijriAdjustmentDays,
            activeTheme = params["activeTheme"] ?: current.activeTheme,
            displayLayoutModel = params["displayLayoutModel"] ?: current.displayLayoutModel,
            carouselIntervalSeconds = params["carouselIntervalSeconds"]?.toIntOrNull() ?: current.carouselIntervalSeconds,
            soundAlertEnabled = parseBool(params["soundAlertEnabled"], current.soundAlertEnabled),
            showImsak = parseBool(params["showImsak"], current.showImsak),
            showSubuh = parseBool(params["showSubuh"], current.showSubuh),
            showTerbit = parseBool(params["showTerbit"], current.showTerbit),
            showDhuha = parseBool(params["showDhuha"], current.showDhuha),
            showDzuhur = parseBool(params["showDzuhur"], current.showDzuhur),
            showAshar = parseBool(params["showAshar"], current.showAshar),
            showMaghrib = parseBool(params["showMaghrib"], current.showMaghrib),
            showIsya = parseBool(params["showIsya"], current.showIsya),
            showFinancialReport = parseBool(params["showFinancialReport"], current.showFinancialReport),
            showFridayOfficers = parseBool(params["showFridayOfficers"], current.showFridayOfficers),
            showActivities = parseBool(params["showActivities"], current.showActivities),
            showDailyMaklumat = parseBool(params["showDailyMaklumat"], current.showDailyMaklumat),
            customBackgroundImagePath = params["customBackgroundImagePath"] ?: current.customBackgroundImagePath,
            customBackgroundDim = params["customBackgroundDim"]?.toFloatOrNull() ?: current.customBackgroundDim,
            mainScreenBgPreset = params["mainScreenBgPreset"] ?: current.mainScreenBgPreset,
            backgroundType = if (params.containsKey("mainScreenBgPreset")) "PRESET" else (params["backgroundType"] ?: current.backgroundType),
            backgroundVideoPath = if (params.containsKey("mainScreenBgPreset")) "" else (params["backgroundVideoPath"] ?: current.backgroundVideoPath),
            backgroundCctvCameraId = params["backgroundCctvCameraId"] ?: current.backgroundCctvCameraId,
            financeBgPreset = params["financeBgPreset"] ?: current.financeBgPreset,
            fridayOfficerBgPreset = params["fridayOfficerBgPreset"] ?: current.fridayOfficerBgPreset,
            countdownBgPreset = params["countdownBgPreset"] ?: current.countdownBgPreset,
            customFinanceBgPath = params["customFinanceBgPath"] ?: current.customFinanceBgPath,
            customFridayOfficerBgPath = params["customFridayOfficerBgPath"] ?: current.customFridayOfficerBgPath,
            customCountdownBgPath = params["customCountdownBgPath"] ?: current.customCountdownBgPath,
            centerCardTransparency = params["centerCardTransparency"]?.toFloatOrNull() ?: current.centerCardTransparency,
            qrisImagePath = params["qrisImagePath"] ?: current.qrisImagePath,
            qrisLabel = params["qrisLabel"] ?: current.qrisLabel,
            donationProgramTitle = params["donationProgramTitle"] ?: current.donationProgramTitle,
            donationTargetAmount = params["donationTargetAmount"]?.toLongOrNull() ?: current.donationTargetAmount,
            donationCollectedAmount = params["donationCollectedAmount"]?.toLongOrNull() ?: current.donationCollectedAmount,
            showQrisCard = parseBool(params["showQrisCard"], current.showQrisCard),
            showDailyHadith = parseBool(params["showDailyHadith"], current.showDailyHadith),
            webAdminPassword = params["webAdminPassword"]?.takeIf { it.isNotBlank() } ?: current.webAdminPassword,
            enablePreAdhanCountdown = parseBool(params["enablePreAdhanCountdown"], current.enablePreAdhanCountdown),
            preAdhanCountdownSeconds = params["preAdhanCountdownSeconds"]?.toIntOrNull() ?: current.preAdhanCountdownSeconds,
            imamSubuh = params["imamSubuh"] ?: current.imamSubuh,
            imamDzuhur = params["imamDzuhur"] ?: current.imamDzuhur,
            imamAshar = params["imamAshar"] ?: current.imamAshar,
            imamMaghrib = params["imamMaghrib"] ?: current.imamMaghrib,
            imamIsya = params["imamIsya"] ?: current.imamIsya,
            muadzinSubuh = params["muadzinSubuh"] ?: current.muadzinSubuh,
            muadzinDzuhur = params["muadzinDzuhur"] ?: current.muadzinDzuhur,
            muadzinAshar = params["muadzinAshar"] ?: current.muadzinAshar,
            muadzinMaghrib = params["muadzinMaghrib"] ?: current.muadzinMaghrib,
            muadzinIsya = params["muadzinIsya"] ?: current.muadzinIsya,
            runningTextFontSize = params["runningTextFontSize"]?.toIntOrNull() ?: current.runningTextFontSize,

            // --- Kustomisasi Teks & Warna Laporan Keuangan ---
            financeTitleText = params["financeTitleText"] ?: current.financeTitleText,
            financeTitleColor = params["financeTitleColor"] ?: current.financeTitleColor,
            financeBalanceColor = params["financeBalanceColor"] ?: current.financeBalanceColor,
            financeIncomeColor = params["financeIncomeColor"] ?: current.financeIncomeColor,
            financeExpenseColor = params["financeExpenseColor"] ?: current.financeExpenseColor,
            financeFooterColor = params["financeFooterColor"] ?: current.financeFooterColor,

            // --- Kustomisasi Teks & Warna Petugas Jum'at ---
            fridayTitleText = params["fridayTitleText"] ?: current.fridayTitleText,
            fridayTitleColor = params["fridayTitleColor"] ?: current.fridayTitleColor,
            fridayOfficerNameColor = params["fridayOfficerNameColor"] ?: current.fridayOfficerNameColor,
            fridayOfficerLabelColor = params["fridayOfficerLabelColor"] ?: current.fridayOfficerLabelColor,

            // --- Kustomisasi Teks & Warna Mutiara Hadits ---
            hadithTitleText = params["hadithTitleText"] ?: current.hadithTitleText,
            hadithTitleColor = params["hadithTitleColor"] ?: current.hadithTitleColor,
            hadithThemeText = params["hadithThemeText"] ?: current.hadithThemeText,
            hadithThemeColor = params["hadithThemeColor"] ?: current.hadithThemeColor,
            hadithArabicText = params["hadithArabicText"] ?: current.hadithArabicText,
            hadithArabicColor = params["hadithArabicColor"] ?: current.hadithArabicColor,
            hadithTranslationText = params["hadithTranslationText"] ?: current.hadithTranslationText,
            hadithTranslationColor = params["hadithTranslationColor"] ?: current.hadithTranslationColor,
            hadithNarratorText = params["hadithNarratorText"] ?: current.hadithNarratorText,
            hadithNarratorColor = params["hadithNarratorColor"] ?: current.hadithNarratorColor,

            // --- Kustomisasi Teks & Warna Maklumat & Adab Masjid ---
            maklumatTitleLeft = params["maklumatTitleLeft"] ?: current.maklumatTitleLeft,
            maklumatTitleLeftColor = params["maklumatTitleLeftColor"] ?: current.maklumatTitleLeftColor,
            maklumatTextLeft = params["maklumatTextLeft"] ?: current.maklumatTextLeft,
            maklumatTextLeftColor = params["maklumatTextLeftColor"] ?: current.maklumatTextLeftColor,
            maklumatTitleRight = params["maklumatTitleRight"] ?: current.maklumatTitleRight,
            maklumatTitleRightColor = params["maklumatTitleRightColor"] ?: current.maklumatTitleRightColor,
            maklumatPoint1 = params["maklumatPoint1"] ?: current.maklumatPoint1,
            maklumatPoint2 = params["maklumatPoint2"] ?: current.maklumatPoint2,
            maklumatPoint3 = params["maklumatPoint3"] ?: current.maklumatPoint3,
            maklumatPointsColor = params["maklumatPointsColor"] ?: current.maklumatPointsColor,

            // --- Kustomisasi Font & Warna Jam Digital TV ---
            clockFontFamily = params["clockFontFamily"] ?: current.clockFontFamily,
            clockColor = params["clockColor"] ?: current.clockColor,
            clockColonColor = params["clockColonColor"] ?: current.clockColonColor,
            clockSecondsColor = params["clockSecondsColor"] ?: current.clockSecondsColor,

            // --- Pengaturan Sholat Hari Raya ---
            idulFitriEnabled = parseBool(params["idulFitriEnabled"], current.idulFitriEnabled),
            idulFitriDate = params["idulFitriDate"] ?: current.idulFitriDate,
            idulFitriTime = params["idulFitriTime"] ?: current.idulFitriTime,
            idulFitriIqomahMinutes = params["idulFitriIqomahMinutes"]?.toIntOrNull() ?: current.idulFitriIqomahMinutes,
            idulFitriSholatMinutes = params["idulFitriSholatMinutes"]?.toIntOrNull() ?: current.idulFitriSholatMinutes,
            idulAdhaEnabled = parseBool(params["idulAdhaEnabled"], current.idulAdhaEnabled),
            idulAdhaDate = params["idulAdhaDate"] ?: current.idulAdhaDate,
            idulAdhaTime = params["idulAdhaTime"] ?: current.idulAdhaTime,
            idulAdhaIqomahMinutes = params["idulAdhaIqomahMinutes"]?.toIntOrNull() ?: current.idulAdhaIqomahMinutes,
            idulAdhaSholatMinutes = params["idulAdhaSholatMinutes"]?.toIntOrNull() ?: current.idulAdhaSholatMinutes,

            // --- Pengaturan Tarawih Ramadhan ---
            tarawihEnabled = parseBool(params["tarawihEnabled"], current.tarawihEnabled),
            tarawihAutoDetectNight = parseBool(params["tarawihAutoDetectNight"], current.tarawihAutoDetectNight),
            tarawihManualNight = params["tarawihManualNight"]?.toIntOrNull() ?: current.tarawihManualNight,
            tarawihShowSlide = parseBool(params["tarawihShowSlide"], current.tarawihShowSlide),
            tarawihKultumMinutes = params["tarawihKultumMinutes"]?.toIntOrNull() ?: current.tarawihKultumMinutes,
            tarawihSholatMinutes = params["tarawihSholatMinutes"]?.toIntOrNull() ?: current.tarawihSholatMinutes,
            tarawihTitleText = params["tarawihTitleText"] ?: current.tarawihTitleText,
            tarawihTitleColor = params["tarawihTitleColor"] ?: current.tarawihTitleColor,
            tarawihOfficerNameColor = params["tarawihOfficerNameColor"] ?: current.tarawihOfficerNameColor,
            tarawihOfficerLabelColor = params["tarawihOfficerLabelColor"] ?: current.tarawihOfficerLabelColor,
            tarawihBgPreset = params["tarawihBgPreset"] ?: current.tarawihBgPreset,
            customTarawihBgPath = params["customTarawihBgPath"] ?: current.customTarawihBgPath
        )
        repository.saveConfig(updated)
    }

    private suspend fun handleAddImamSchedule(body: String) {
        val params = parseParams(body)
        val date = params["date"] ?: ""
        val prayerStr = params["prayerName"] ?: "DZUHUR"
        val prayer = try { PrayerName.valueOf(prayerStr.uppercase()) } catch (_: Exception) { PrayerName.DZUHUR }
        val imamName = params["imamName"] ?: ""
        val muadzinName = params["muadzinName"] ?: ""
        val notes = params["notes"] ?: ""
        if (date.isNotBlank() && imamName.isNotBlank()) {
            repository.addDailyImamSchedule(date, prayer, imamName, muadzinName, notes)
        }
    }

    private suspend fun handleDeleteImamSchedule(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        repository.deleteDailyImamSchedule(id)
    }

    private suspend fun handleAddFinance(body: String) {
        val params = parseParams(body)
        val title = params["title"] ?: "Infaq Sholat"
        val amount = params["amount"]?.toDoubleOrNull()?.toLong() ?: params["amount"]?.toLongOrNull() ?: 0L
        val typeStr = (params["type"] ?: "INCOME").uppercase()
        val type = if (typeStr == "EXPENSE" || typeStr == "PENGELUARAN") TransactionType.EXPENSE else TransactionType.INCOME
        val category = params["category"] ?: "Infaq"
        val date = params["date"] ?: "Hari Ini"
        val notes = params["notes"] ?: ""
        repository.addFinance(title, amount, type, category, date, notes)
    }

    private suspend fun handleUpdateFinance(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        val title = params["title"] ?: "Infaq Sholat"
        val amount = params["amount"]?.toDoubleOrNull()?.toLong() ?: params["amount"]?.toLongOrNull() ?: 0L
        val typeStr = (params["type"] ?: "INCOME").uppercase()
        val type = if (typeStr == "EXPENSE" || typeStr == "PENGELUARAN") TransactionType.EXPENSE else TransactionType.INCOME
        val category = params["category"] ?: "Infaq"
        val date = params["date"] ?: "Hari Ini"
        val notes = params["notes"] ?: ""
        repository.updateFinance(id, title, amount, type, category, date, notes)
    }

    private suspend fun handleDeleteFinance(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        repository.deleteFinance(id)
    }

    private suspend fun handleSaveFriday(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: 1L
        val applyAll = params["applyAll"]?.toBooleanStrictOrNull() ?: false

        if (applyAll) {
            for (weekId in 1L..5L) {
                val all = repository.allFridaySchedulesFlow.first()
                val current = all.find { it.id == weekId } ?: FridaySchedule(id = weekId, date = "", hijriDate = "", khotib = "", imam = "", muadzin = "", bilal = "", khutbahTopic = "")
                val updated = current.copy(
                    id = weekId,
                    date = if (weekId == id) (params["date"] ?: current.date) else current.date,
                    hijriDate = params["hijriDate"] ?: current.hijriDate,
                    khotib = params["khotib"] ?: current.khotib,
                    khotibPhone = params["khotibPhone"] ?: current.khotibPhone,
                    imam = params["imam"] ?: current.imam,
                    imamPhone = params["imamPhone"] ?: current.imamPhone,
                    muadzin = params["muadzin"] ?: current.muadzin,
                    muadzinPhone = params["muadzinPhone"] ?: current.muadzinPhone,
                    bilal = params["bilal"] ?: current.bilal,
                    bilalPhone = params["bilalPhone"] ?: current.bilalPhone,
                    khutbahTopic = params["khutbahTopic"] ?: current.khutbahTopic,
                    notes = params["notes"] ?: current.notes
                )
                repository.saveFridaySchedule(updated)
            }
        } else {
            val all = repository.allFridaySchedulesFlow.first()
            val current = all.find { it.id == id } ?: FridaySchedule(id = id, date = "", hijriDate = "", khotib = "", imam = "", muadzin = "", bilal = "", khutbahTopic = "")
            val updated = current.copy(
                id = id,
                date = params["date"] ?: current.date,
                hijriDate = params["hijriDate"] ?: current.hijriDate,
                khotib = params["khotib"] ?: current.khotib,
                khotibPhone = params["khotibPhone"] ?: current.khotibPhone,
                imam = params["imam"] ?: current.imam,
                imamPhone = params["imamPhone"] ?: current.imamPhone,
                muadzin = params["muadzin"] ?: current.muadzin,
                muadzinPhone = params["muadzinPhone"] ?: current.muadzinPhone,
                bilal = params["bilal"] ?: current.bilal,
                bilalPhone = params["bilalPhone"] ?: current.bilalPhone,
                khutbahTopic = params["khutbahTopic"] ?: current.khutbahTopic,
                notes = params["notes"] ?: current.notes
            )
            repository.saveFridaySchedule(updated)

            // Also persist Hari Raya specific execution settings if id == 6 or 7
            if (id == 6L) {
                val currentCfg = repository.configFlow.first()
                val updatedCfg = currentCfg.copy(
                    idulFitriEnabled = parseBool(params["idulFitriEnabled"], currentCfg.idulFitriEnabled),
                    idulFitriDate = params["idulFitriDate"] ?: currentCfg.idulFitriDate,
                    idulFitriTime = params["idulFitriTime"] ?: currentCfg.idulFitriTime,
                    idulFitriIqomahMinutes = params["idulFitriIqomahMinutes"]?.toIntOrNull() ?: currentCfg.idulFitriIqomahMinutes,
                    idulFitriSholatMinutes = params["idulFitriSholatMinutes"]?.toIntOrNull() ?: currentCfg.idulFitriSholatMinutes
                )
                repository.saveConfig(updatedCfg)
            } else if (id == 7L) {
                val currentCfg = repository.configFlow.first()
                val updatedCfg = currentCfg.copy(
                    idulAdhaEnabled = parseBool(params["idulAdhaEnabled"], currentCfg.idulAdhaEnabled),
                    idulAdhaDate = params["idulAdhaDate"] ?: currentCfg.idulAdhaDate,
                    idulAdhaTime = params["idulAdhaTime"] ?: currentCfg.idulAdhaTime,
                    idulAdhaIqomahMinutes = params["idulAdhaIqomahMinutes"]?.toIntOrNull() ?: currentCfg.idulAdhaIqomahMinutes,
                    idulAdhaSholatMinutes = params["idulAdhaSholatMinutes"]?.toIntOrNull() ?: currentCfg.idulAdhaSholatMinutes
                )
                repository.saveConfig(updatedCfg)
            }
        }
    }

    private suspend fun handleSaveTarawih(body: String) {
        val params = parseParams(body)
        val night = params["night"]?.toIntOrNull() ?: 1
        val isConfigOnly = parseBool(params["isConfigOnly"], false)

        if (!isConfigOnly) {
            val current = repository.getTarawihScheduleForNight(night) ?: com.example.data.model.TarawihSchedule(
                night = night,
                date = "Malam ke-$night Ramadhan"
            )
            val updated = current.copy(
                night = night,
                date = params["date"] ?: current.date,
                penceramah = params["penceramah"] ?: current.penceramah,
                penceramahPhone = params["penceramahPhone"] ?: current.penceramahPhone,
                judulKultum = params["judulKultum"] ?: current.judulKultum,
                imamTarawih = params["imamTarawih"] ?: current.imamTarawih,
                imamTarawihPhone = params["imamTarawihPhone"] ?: current.imamTarawihPhone,
                imamWitir = params["imamWitir"] ?: current.imamWitir,
                imamWitirPhone = params["imamWitirPhone"] ?: current.imamWitirPhone,
                bilalTarawih = params["bilalTarawih"] ?: current.bilalTarawih,
                bilalTarawihPhone = params["bilalTarawihPhone"] ?: current.bilalTarawihPhone,
                notes = params["notes"] ?: current.notes
            )
            repository.saveTarawihSchedule(updated)
        }

        if (params.containsKey("tarawihEnabled") || params.containsKey("tarawihManualNight") || params.containsKey("tarawihShowSlide")) {
            val curCfg = repository.configFlow.first()
            val updatedCfg = curCfg.copy(
                tarawihEnabled = parseBool(params["tarawihEnabled"], curCfg.tarawihEnabled),
                tarawihAutoDetectNight = parseBool(params["tarawihAutoDetectNight"], curCfg.tarawihAutoDetectNight),
                tarawihManualNight = params["tarawihManualNight"]?.toIntOrNull() ?: curCfg.tarawihManualNight,
                tarawihShowSlide = parseBool(params["tarawihShowSlide"], curCfg.tarawihShowSlide),
                tarawihKultumMinutes = params["tarawihKultumMinutes"]?.toIntOrNull() ?: curCfg.tarawihKultumMinutes,
                tarawihSholatMinutes = params["tarawihSholatMinutes"]?.toIntOrNull() ?: curCfg.tarawihSholatMinutes,
                tarawihTitleText = params["tarawihTitleText"] ?: curCfg.tarawihTitleText,
                tarawihTitleColor = params["tarawihTitleColor"] ?: curCfg.tarawihTitleColor,
                tarawihOfficerNameColor = params["tarawihOfficerNameColor"] ?: curCfg.tarawihOfficerNameColor,
                tarawihOfficerLabelColor = params["tarawihOfficerLabelColor"] ?: curCfg.tarawihOfficerLabelColor,
                tarawihBgPreset = params["tarawihBgPreset"] ?: curCfg.tarawihBgPreset,
                customTarawihBgPath = params["customTarawihBgPath"] ?: curCfg.customTarawihBgPath
            )
            repository.saveConfig(updatedCfg)
        }
    }

    private suspend fun handleSendWaTarawih(body: String): String {
        val params = parseParams(body)
        val night = params["night"]?.toIntOrNull() ?: 1

        val cfg = repository.configFlow.first()
        val schedule = repository.getTarawihScheduleForNight(night)
            ?: return "{\"status\":\"error\",\"message\":\"Jadwal Tarawih malam ke-$night belum diisi!\"}"

        val token = cfg.waGatewayToken
        if (token.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Token Fonnte belum diisi di Pengaturan WhatsApp Gateway\"}"
        }

        val template = com.example.notification.WhatsAppGatewayManager.DEFAULT_TEMPLATE_TARAWIH

        val officers = listOf(
            Triple("Penceramah Kultum", schedule.penceramah, schedule.penceramahPhone),
            Triple("Imam Sholat Tarawih", schedule.imamTarawih, schedule.imamTarawihPhone),
            Triple("Imam Sholat Witir", schedule.imamWitir, schedule.imamWitirPhone),
            Triple("Bilal / Muadzin Tarawih", schedule.bilalTarawih, schedule.bilalTarawihPhone)
        )

        var sentCount = 0
        val errors = mutableListOf<String>()

        for ((role, name, phone) in officers) {
            if (name.isNotBlank() && phone.isNotBlank()) {
                val msg = com.example.notification.WhatsAppGatewayManager.formatTarawihMessage(
                    template = template,
                    config = cfg,
                    schedule = schedule,
                    officerName = name,
                    roleName = role,
                    isyaTimeStr = "19:15 WIB"
                )
                val sendRes = com.example.notification.WhatsAppGatewayManager.sendMessage(token, phone, msg)
                if (sendRes.success) {
                    sentCount++
                } else {
                    errors.add("$role ($name): ${sendRes.message}")
                }
            }
        }

        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "Tarawih malam ke-$night broadcast: $sentCount pesan terkirim (${errors.size} gagal) pada $nowStr"
        repository.updateWaGatewayLastSent(
            thursdayDate = cfg.waGatewayLastSentThursdayDate,
            fridayDate = cfg.waGatewayLastSentFridayDate,
            logJson = logEntry
        )

        val res = JSONObject().apply {
            put("status", if (sentCount > 0 || errors.isEmpty()) "ok" else "error")
            put("message", "Berhasil mengirim $sentCount pesan pengingat Tarawih Malam ke-$night." + if (errors.isNotEmpty()) " Gagal: " + errors.joinToString(", ") else "")
        }
        return res.toString()
    }

    private suspend fun handleAddActivity(body: String) {
        val params = parseParams(body)
        val title = params["title"] ?: "Kajian Rutin"
        val speaker = params["speaker"] ?: "Ustadz Pembicara"
        val speakerPhone = params["speakerPhone"] ?: ""
        val date = params["date"] ?: "Ahad Pagi"
        val time = params["time"] ?: "05.30 WIB"
        val location = params["location"] ?: "Ruang Utama Masjid"
        val description = params["description"] ?: ""
        val category = params["category"] ?: "Kajian Rutin"
        repository.addActivity(
            title = title,
            speaker = speaker,
            speakerPhone = speakerPhone,
            date = date,
            time = time,
            location = location,
            description = description,
            category = category
        )
    }

    private suspend fun handleUpdateActivity(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        val title = params["title"] ?: "Kajian Rutin"
        val speaker = params["speaker"] ?: "Ustadz Pembicara"
        val speakerPhone = params["speakerPhone"] ?: ""
        val date = params["date"] ?: "Ahad Pagi"
        val time = params["time"] ?: "05.30 WIB"
        val location = params["location"] ?: "Ruang Utama Masjid"
        val description = params["description"] ?: ""
        val category = params["category"] ?: "Kajian Rutin"
        repository.updateActivity(
            id = id,
            title = title,
            speaker = speaker,
            speakerPhone = speakerPhone,
            date = date,
            time = time,
            location = location,
            description = description,
            category = category
        )
    }

    private suspend fun handleSaveWaGatewayConfig(body: String): String {
        val params = parseParams(body)
        val enabled = params["enabled"]?.toBooleanStrictOrNull() ?: false
        val token = params["token"] ?: ""
        val hourThu = params["sendHourThursday"]?.toIntOrNull() ?: 9
        val hourFri = params["sendHourFriday"]?.toIntOrNull() ?: 9
        val templateThu = params["templateThursday"] ?: ""
        val templateFri = params["templateFriday"] ?: ""
        val templateKajian = params["templateKajian"] ?: ""

        val cfg = repository.configFlow.first()
        repository.saveConfig(
            cfg.copy(
                waGatewayEnabled = enabled,
                waGatewayToken = token,
                waGatewaySendThursdayHour = hourThu,
                waGatewaySendFridayHour = hourFri,
                waGatewayTemplateThursday = if (templateThu.isNotBlank()) templateThu else cfg.waGatewayTemplateThursday,
                waGatewayTemplateFriday = if (templateFri.isNotBlank()) templateFri else cfg.waGatewayTemplateFriday,
                waGatewayTemplateKajian = if (templateKajian.isNotBlank()) templateKajian else cfg.waGatewayTemplateKajian
            )
        )
        return "{\"status\":\"ok\",\"message\":\"Pengaturan WhatsApp Gateway berhasil disimpan!\"}"
    }

    private suspend fun handleCheckWaDeviceStatus(body: String): String {
        val params = parseParams(body)
        var token = params["token"] ?: ""
        if (token.isBlank()) {
            val cfg = repository.configFlow.first()
            token = cfg.waGatewayToken
        }
        if (token.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Token Fonnte belum diisi\"}"
        }
        val status = com.example.notification.WhatsAppGatewayManager.checkDeviceStatus(token)
        val res = JSONObject().apply {
            put("status", if (status.success) "ok" else "error")
            put("deviceStatus", status.status)
            put("deviceName", status.deviceName)
            put("devicePhone", status.devicePhone)
            put("quota", status.quota)
            put("expired", status.expired)
            put("message", status.message)
        }
        return res.toString()
    }

    private suspend fun handleSendTestWa(body: String): String {
        val params = parseParams(body)
        var token = params["token"] ?: ""
        val phone = params["phone"] ?: ""
        val message = params["message"] ?: "Halo! Ini adalah pesan uji coba dari Sistem MasjidKU TV WhatsApp Gateway."
        if (token.isBlank()) {
            val cfg = repository.configFlow.first()
            token = cfg.waGatewayToken
        }
        if (token.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Token Fonnte belum diisi\"}"
        }
        if (phone.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Nomor WhatsApp tujuan belum diisi\"}"
        }
        val sendRes = com.example.notification.WhatsAppGatewayManager.sendMessage(token, phone, message)
        val res = JSONObject().apply {
            put("status", if (sendRes.success) "ok" else "error")
            put("message", sendRes.message)
        }
        return res.toString()
    }

    private suspend fun handleSendFridayBroadcast(body: String): String {
        val params = parseParams(body)
        val type = params["type"] ?: "THURSDAY" // "THURSDAY" or "FRIDAY"
        val weekId = params["weekId"]?.toLongOrNull() ?: com.example.viewmodel.MasjidTVViewModel.getUpcomingFridayWeekIndex(java.util.Calendar.getInstance())

        val cfg = repository.configFlow.first()
        val allFriday = repository.allFridaySchedulesFlow.first()
        val friday = allFriday.find { it.id == weekId } ?: repository.fridayScheduleFlow.first()

        val token = cfg.waGatewayToken
        if (token.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Token Fonnte belum diisi di Pengaturan WhatsApp Gateway\"}"
        }

        val template = if (type == "THURSDAY") {
            if (cfg.waGatewayTemplateThursday.isNotBlank()) cfg.waGatewayTemplateThursday else com.example.notification.WhatsAppGatewayManager.DEFAULT_TEMPLATE_THURSDAY
        } else {
            if (cfg.waGatewayTemplateFriday.isNotBlank()) cfg.waGatewayTemplateFriday else com.example.notification.WhatsAppGatewayManager.DEFAULT_TEMPLATE_FRIDAY
        }

        val officers = listOf(
            Triple("Khotib", friday.khotib, friday.khotibPhone),
            Triple("Imam Sholat", friday.imam, friday.imamPhone),
            Triple("Muadzin", friday.muadzin, friday.muadzinPhone),
            Triple("Bilal / Badal", friday.bilal, friday.bilalPhone)
        )

        var sentCount = 0
        val errors = mutableListOf<String>()

        for ((role, name, phone) in officers) {
            if (name.isNotBlank() && phone.isNotBlank()) {
                val msg = com.example.notification.WhatsAppGatewayManager.formatFridayMessage(
                    template = template,
                    config = cfg,
                    schedule = friday,
                    officerName = name,
                    roleName = role,
                    prayerTimeStr = "11:55 WIB"
                )
                val sendRes = com.example.notification.WhatsAppGatewayManager.sendMessage(token, phone, msg)
                if (sendRes.success) {
                    sentCount++
                } else {
                    errors.add("$role ($name): ${sendRes.message}")
                }
            }
        }

        val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "Manual broadcast ($type): $sentCount pesan terkirim (${errors.size} gagal) pada $nowStr"
        repository.updateWaGatewayLastSent(
            thursdayDate = if (type == "THURSDAY") java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) else cfg.waGatewayLastSentThursdayDate,
            fridayDate = if (type == "FRIDAY") java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) else cfg.waGatewayLastSentFridayDate,
            logJson = logEntry
        )

        val res = JSONObject().apply {
            put("status", if (sentCount > 0 || errors.isEmpty()) "ok" else "error")
            put("message", "Berhasil mengirim $sentCount pesan pengingat Sholat Jum'at." + if (errors.isNotEmpty()) " Gagal: " + errors.joinToString(", ") else "")
        }
        return res.toString()
    }

    private suspend fun handleSendActivityReminder(body: String): String {
        val params = parseParams(body)
        val activityId = params["id"]?.toLongOrNull() ?: return "{\"status\":\"error\",\"message\":\"ID kegiatan tidak valid\"}"
        val activities = repository.activitiesFlow.first()
        val activity = activities.find { it.id == activityId } ?: return "{\"status\":\"error\",\"message\":\"Kegiatan tidak ditemukan\"}"

        val cfg = repository.configFlow.first()
        val token = cfg.waGatewayToken
        if (token.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Token Fonnte belum diisi di Pengaturan WhatsApp Gateway\"}"
        }
        if (activity.speakerPhone.isBlank()) {
            return "{\"status\":\"error\",\"message\":\"Nomor WhatsApp pemateri / ustadz belum diisi untuk agenda ini\"}"
        }

        val template = if (cfg.waGatewayTemplateKajian.isNotBlank()) cfg.waGatewayTemplateKajian else com.example.notification.WhatsAppGatewayManager.DEFAULT_TEMPLATE_KAJIAN
        val msg = com.example.notification.WhatsAppGatewayManager.formatKajianMessage(
            template = template,
            config = cfg,
            activity = activity
        )

        val sendRes = com.example.notification.WhatsAppGatewayManager.sendMessage(token, activity.speakerPhone, msg)
        val res = JSONObject().apply {
            put("status", if (sendRes.success) "ok" else "error")
            put("message", if (sendRes.success) "Pesan pengingat agenda dakwah berhasil dikirim ke ${activity.speaker} (${activity.speakerPhone})!" else "Gagal kirim pesan: ${sendRes.message}")
        }
        return res.toString()
    }

    private suspend fun handleDeleteActivity(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        repository.deleteActivity(id)
    }

    private suspend fun handleAddRunningText(body: String) {
        val params = parseParams(body)
        val text = params["text"] ?: ""
        if (text.isNotBlank()) {
            repository.addRunningText(text, true, 0)
        }
    }

    private suspend fun handleUpdateRunningText(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        val text = params["text"] ?: ""
        val existing = repository.allRunningTextsFlow.first().find { it.id == id }
        val isActive = params["isActive"]?.toBooleanStrictOrNull() ?: existing?.isActive ?: true
        val order = params["order"]?.toIntOrNull() ?: existing?.order ?: 0
        if (text.isNotBlank()) {
            repository.updateRunningText(id, text, isActive, order)
        }
    }

    private suspend fun handleDeleteRunningText(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        repository.deleteRunningText(id)
    }

    private suspend fun handleUploadMedia(body: String): String {
        val params = parseParams(body)
        val type = params["type"] ?: "SLIDESHOW" // "BACKGROUND" or "SLIDESHOW"
        val title = params["title"] ?: ""
        val duration = params["duration"]?.toIntOrNull() ?: 15
        val base64Data = params["base64"] ?: return "{\"status\":\"error\",\"message\":\"Data gambar kosong\"}"

        val cleanBase64 = if (base64Data.contains(",")) {
            base64Data.substringAfter(",")
        } else {
            base64Data
        }

        val bytes = try {
            Base64.decode(cleanBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            return "{\"status\":\"error\",\"message\":\"Format gambar tidak valid\"}"
        }

        val mediaDir = context?.filesDir?.resolve("media") ?: File("/tmp/media")
        if (!mediaDir.exists()) mediaDir.mkdirs()

        val ext = if (base64Data.startsWith("data:image/png") || params["ext"] == "png") "png" else "jpg"
        val fileName = when (type) {
            "BACKGROUND" -> "bg_${System.currentTimeMillis()}.$ext"
            "QRIS" -> "qris_mosque.$ext"
            else -> "slide_${System.currentTimeMillis()}.$ext"
        }
        val targetFile = File(mediaDir, fileName)

        FileOutputStream(targetFile).use { it.write(bytes) }

        if (type == "BACKGROUND") {
            val cfg = repository.configFlow.first()
            repository.saveConfig(
                cfg.copy(
                    customBackgroundImagePath = targetFile.absolutePath,
                    mainScreenBgPreset = "CUSTOM_UPLOAD",
                    backgroundType = "PRESET",
                    backgroundVideoPath = ""
                )
            )
            return "{\"status\":\"ok\",\"message\":\"Background Layar TV Berhasil Diperbarui!\",\"path\":\"/media/$fileName\"}"
        } else if (type == "FINANCE_BG") {
            val cfg = repository.configFlow.first()
            repository.saveConfig(cfg.copy(customFinanceBgPath = targetFile.absolutePath, financeBgPreset = "CUSTOM_UPLOAD"))
            return "{\"status\":\"ok\",\"message\":\"Background Laporan Kas Berhasil Diperbarui!\",\"path\":\"/media/$fileName\"}"
        } else if (type == "FRIDAY_BG") {
            val cfg = repository.configFlow.first()
            repository.saveConfig(cfg.copy(customFridayOfficerBgPath = targetFile.absolutePath, fridayOfficerBgPreset = "CUSTOM_UPLOAD"))
            return "{\"status\":\"ok\",\"message\":\"Background Petugas Sholat Jum'at Berhasil Diperbarui!\",\"path\":\"/media/$fileName\"}"
        } else if (type == "COUNTDOWN_BG") {
            val cfg = repository.configFlow.first()
            repository.saveConfig(cfg.copy(customCountdownBgPath = targetFile.absolutePath, countdownBgPreset = "CUSTOM_UPLOAD"))
            return "{\"status\":\"ok\",\"message\":\"Background Countdown Pengingat Sholat Berhasil Diperbarui!\",\"path\":\"/media/$fileName\"}"
        } else if (type == "QRIS") {
            val cfg = repository.configFlow.first()
            repository.saveConfig(cfg.copy(qrisImagePath = targetFile.absolutePath))
            return "{\"status\":\"ok\",\"message\":\"Gambar QRIS Infaq Berhasil Di-upload!\",\"path\":\"/media/$fileName\"}"
        } else {
            repository.addMediaSlide(
                filePath = targetFile.absolutePath,
                title = title,
                durationSeconds = duration,
                isActive = true,
                order = 0
            )
            return "{\"status\":\"ok\",\"message\":\"Poster Slideshow Berhasil Ditambahkan!\",\"path\":\"/media/$fileName\"}"
        }
    }

    private suspend fun handleRemoveBackground() {
        val cfg = repository.configFlow.first()
        repository.saveConfig(
            cfg.copy(
                customBackgroundImagePath = "",
                mainScreenBgPreset = "PRESET_EMERALD_MIHRAB",
                backgroundType = "PRESET",
                backgroundVideoPath = ""
            )
        )
    }

    private suspend fun handleRemoveFinanceBg() {
        val cfg = repository.configFlow.first()
        repository.saveConfig(cfg.copy(customFinanceBgPath = "", financeBgPreset = "PRESET_WHITE_PEARL_GOLD"))
    }

    private suspend fun handleRemoveFridayBg() {
        val cfg = repository.configFlow.first()
        repository.saveConfig(cfg.copy(customFridayOfficerBgPath = "", fridayOfficerBgPreset = "PRESET_EMERALD_MIHRAB"))
    }

    private suspend fun handleRemoveCountdownBg() {
        val cfg = repository.configFlow.first()
        repository.saveConfig(cfg.copy(customCountdownBgPath = "", countdownBgPreset = "PRESET_COUNTDOWN_PLAQUE"))
    }

    private suspend fun handleRemoveQris() {
        val cfg = repository.configFlow.first()
        if (cfg.qrisImagePath.isNotBlank()) {
            try {
                val f = File(cfg.qrisImagePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
        repository.saveConfig(cfg.copy(qrisImagePath = ""))
    }

    private suspend fun handleSetBackgroundDim(body: String) {
        val params = parseParams(body)
        val dim = params["dim"]?.toFloatOrNull() ?: 0.5f
        val cfg = repository.configFlow.first()
        repository.saveConfig(cfg.copy(customBackgroundDim = dim))
    }

    private suspend fun handleUpdateMediaSlide(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        val title = params["title"] ?: ""
        val duration = params["duration"]?.toIntOrNull() ?: 15
        val isActive = params["isActive"]?.toBooleanStrictOrNull() ?: true
        val order = params["order"]?.toIntOrNull() ?: 0
        repository.updateMediaSlide(id, title, duration, isActive, order)
    }

    private suspend fun handleDeleteMediaSlide(body: String) {
        val params = parseParams(body)
        val id = params["id"]?.toLongOrNull() ?: return
        val allSlides = repository.allMediaSlidesFlow.first()
        val slide = allSlides.find { it.id == id }
        if (slide != null) {
            try {
                val file = File(slide.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        repository.deleteMediaSlide(id)
    }

    private suspend fun buildStatusJson(): String {
        val config = repository.configFlow.first()
        val finances = repository.financesFlow.first()
        val friday = repository.fridayScheduleFlow.first()
        val activities = repository.activitiesFlow.first()
        val runningTexts = repository.allRunningTextsFlow.first()
        val mediaSlides = repository.allMediaSlidesFlow.first()

        val json = JSONObject()
        val cfgJson = JSONObject().apply {
            put("mosqueName", config.mosqueName)
            put("tagline", config.tagline)
            put("address", config.address)
            put("city", config.city)
            put("phone", config.phone)
            put("bankName", config.bankName)
            put("bankAccount", config.bankAccount)
            put("bankAccountName", config.bankAccountName)
            put("latitude", config.latitude)
            put("longitude", config.longitude)
            put("timezone", config.timezone)
            put("showImsak", config.showImsak)
            put("showSubuh", config.showSubuh)
            put("showTerbit", config.showTerbit)
            put("showDhuha", config.showDhuha)
            put("showDzuhur", config.showDzuhur)
            put("showAshar", config.showAshar)
            put("showMaghrib", config.showMaghrib)
            put("showIsya", config.showIsya)
            put("showFinancialReport", config.showFinancialReport)
            put("showFridayOfficers", config.showFridayOfficers)
            put("showActivities", config.showActivities)
            put("showDailyMaklumat", config.showDailyMaklumat)
            put("offsetImsak", config.offsetImsak)
            put("offsetSubuh", config.offsetSubuh)
            put("offsetTerbit", config.offsetTerbit)
            put("offsetDhuha", config.offsetDhuha)
            put("offsetDzuhur", config.offsetDzuhur)
            put("offsetAshar", config.offsetAshar)
            put("offsetMaghrib", config.offsetMaghrib)
            put("offsetIsya", config.offsetIsya)
            put("iqomahSubuh", config.iqomahSubuh)
            put("iqomahDzuhur", config.iqomahDzuhur)
            put("iqomahJumat", config.iqomahJumat)
            put("iqomahAshar", config.iqomahAshar)
            put("iqomahMaghrib", config.iqomahMaghrib)
            put("iqomahIsya", config.iqomahIsya)
            put("sholatDurationMinutes", config.sholatDurationMinutes)
            put("hijriAdjustmentDays", config.hijriAdjustmentDays)
            put("activeTheme", config.activeTheme)
            put("displayLayoutModel", config.displayLayoutModel)
            put("customBackgroundImagePath", config.customBackgroundImagePath)
            put("customBackgroundDim", config.customBackgroundDim)
            put("mainScreenBgPreset", config.mainScreenBgPreset)
            put("financeBgPreset", config.financeBgPreset)
            put("fridayOfficerBgPreset", config.fridayOfficerBgPreset)
            put("countdownBgPreset", config.countdownBgPreset)
            put("customFinanceBgPath", config.customFinanceBgPath)
            put("customFridayOfficerBgPath", config.customFridayOfficerBgPath)
            put("customCountdownBgPath", config.customCountdownBgPath)
            put("centerCardTransparency", config.centerCardTransparency)
            put("qrisImagePath", config.qrisImagePath)
            put("qrisLabel", config.qrisLabel)
            put("donationProgramTitle", config.donationProgramTitle)
            put("donationTargetAmount", config.donationTargetAmount)
            put("donationCollectedAmount", config.donationCollectedAmount)
            put("showQrisCard", config.showQrisCard)
            put("showDailyHadith", config.showDailyHadith)
            put("enablePreAdhanCountdown", config.enablePreAdhanCountdown)
            put("preAdhanCountdownSeconds", config.preAdhanCountdownSeconds)
            put("imamSubuh", config.imamSubuh)
            put("imamDzuhur", config.imamDzuhur)
            put("imamAshar", config.imamAshar)
            put("imamMaghrib", config.imamMaghrib)
            put("imamIsya", config.imamIsya)
            put("muadzinSubuh", config.muadzinSubuh)
            put("muadzinDzuhur", config.muadzinDzuhur)
            put("muadzinAshar", config.muadzinAshar)
            put("muadzinMaghrib", config.muadzinMaghrib)
            put("muadzinIsya", config.muadzinIsya)
            put("runningTextFontSize", config.runningTextFontSize)
            put("murottalEnabled", config.murottalEnabled)
            put("murottalSubuh", config.murottalSubuh)
            put("murottalDzuhur", config.murottalDzuhur)
            put("murottalAshar", config.murottalAshar)
            put("murottalMaghrib", config.murottalMaghrib)
            put("murottalIsya", config.murottalIsya)
            put("murottalJumat", config.murottalJumat)
            put("murottalDurationMinutes", config.murottalDurationMinutes)
            put("murottalVolume", config.murottalVolume)
            put("murottalSourceType", config.murottalSourceType)
            put("murottalSelectedPresetId", config.murottalSelectedPresetId)
            put("murottalCustomAudioPath", config.murottalCustomAudioPath)
            put("customLogoImagePath", config.customLogoImagePath)
            put("youtubeLiveUrl", config.youtubeLiveUrl)
            put("youtubeLiveEnabled", config.youtubeLiveEnabled)
            put("youtubeLiveTitle", config.youtubeLiveTitle)
            put("showYoutubeLiveSlide", config.showYoutubeLiveSlide)
            put("youtubeLiveMuted", config.youtubeLiveMuted)
            put("cctvStreamUrl", config.cctvStreamUrl)
            put("cctvStreamEnabled", config.cctvStreamEnabled)
            put("cctvStreamTitle", config.cctvStreamTitle)
            put("showCctvSlide", config.showCctvSlide)
            put("cctvStreamMuted", config.cctvStreamMuted)
            put("liveStreamType", config.liveStreamType)
            put("cctvCamerasJson", config.cctvCamerasJson)
            put("cctvActiveCameraId", config.cctvActiveCameraId)
            put("cctvAutoRotateEnabled", config.cctvAutoRotateEnabled)
            put("cctvAutoRotateIntervalSeconds", config.cctvAutoRotateIntervalSeconds)
            put("cctvDisplayLayout", config.cctvDisplayLayout)
            put("cctvAutoPlayAfterPrayer", config.cctvAutoPlayAfterPrayer)
            put("cctvAfterPrayerDurationMinutes", config.cctvAfterPrayerDurationMinutes)
            put("cctvAfterPrayerWaktu", config.cctvAfterPrayerWaktu)
            put("cctvAfterPrayerCameraId", config.cctvAfterPrayerCameraId)
            put("cctvScheduleEnabled", config.cctvScheduleEnabled)
            put("cctvScheduleStartTime", config.cctvScheduleStartTime)
            put("cctvScheduleEndTime", config.cctvScheduleEndTime)
            put("cctvScheduleDays", config.cctvScheduleDays)
            put("cctvScheduleCameraId", config.cctvScheduleCameraId)
            put("backgroundType", config.backgroundType)
            put("backgroundVideoPath", config.backgroundVideoPath)
            put("backgroundCctvCameraId", config.backgroundCctvCameraId)
            put("fullscreenVideoPath", config.fullscreenVideoPath)
            put("fullscreenVideoTitle", config.fullscreenVideoTitle)
            put("fullscreenVideoMuted", config.fullscreenVideoMuted)
            put("fullscreenVideoPlaying", config.fullscreenVideoPlaying)

            // Kustomisasi Teks & Warna Konten TV
            put("financeTitleText", config.financeTitleText)
            put("financeTitleColor", config.financeTitleColor)
            put("financeBalanceColor", config.financeBalanceColor)
            put("financeIncomeColor", config.financeIncomeColor)
            put("financeExpenseColor", config.financeExpenseColor)
            put("financeFooterColor", config.financeFooterColor)

            put("fridayTitleText", config.fridayTitleText)
            put("fridayTitleColor", config.fridayTitleColor)
            put("fridayOfficerNameColor", config.fridayOfficerNameColor)
            put("fridayOfficerLabelColor", config.fridayOfficerLabelColor)

            put("hadithTitleText", config.hadithTitleText)
            put("hadithTitleColor", config.hadithTitleColor)
            put("hadithThemeText", config.hadithThemeText)
            put("hadithThemeColor", config.hadithThemeColor)
            put("hadithArabicText", config.hadithArabicText)
            put("hadithArabicColor", config.hadithArabicColor)
            put("hadithTranslationText", config.hadithTranslationText)
            put("hadithTranslationColor", config.hadithTranslationColor)
            put("hadithNarratorText", config.hadithNarratorText)
            put("hadithNarratorColor", config.hadithNarratorColor)

            put("maklumatTitleLeft", config.maklumatTitleLeft)
            put("maklumatTitleLeftColor", config.maklumatTitleLeftColor)
            put("maklumatTextLeft", config.maklumatTextLeft)
            put("maklumatTextLeftColor", config.maklumatTextLeftColor)
            put("maklumatTitleRight", config.maklumatTitleRight)
            put("maklumatTitleRightColor", config.maklumatTitleRightColor)
            put("maklumatPoint1", config.maklumatPoint1)
            put("maklumatPoint2", config.maklumatPoint2)
            put("maklumatPoint3", config.maklumatPoint3)
            put("maklumatPointsColor", config.maklumatPointsColor)

            put("clockFontFamily", config.clockFontFamily)
            put("clockColor", config.clockColor)
            put("clockColonColor", config.clockColonColor)
            put("clockSecondsColor", config.clockSecondsColor)

            put("waGatewayEnabled", config.waGatewayEnabled)
            put("waGatewayProvider", config.waGatewayProvider)
            put("waGatewayToken", config.waGatewayToken)
            put("waGatewaySendThursdayHour", config.waGatewaySendThursdayHour)
            put("waGatewaySendFridayHour", config.waGatewaySendFridayHour)
            put("waGatewayLastSentThursdayDate", config.waGatewayLastSentThursdayDate)
            put("waGatewayLastSentFridayDate", config.waGatewayLastSentFridayDate)
            put("waGatewayTemplateThursday", config.waGatewayTemplateThursday)
            put("waGatewayTemplateFriday", config.waGatewayTemplateFriday)
            put("waGatewayTemplateKajian", config.waGatewayTemplateKajian)
            put("waGatewayLastLogJson", config.waGatewayLastLogJson)

            put("idulFitriEnabled", config.idulFitriEnabled)
            put("idulFitriDate", config.idulFitriDate)
            put("idulFitriTime", config.idulFitriTime)
            put("idulFitriIqomahMinutes", config.idulFitriIqomahMinutes)
            put("idulFitriSholatMinutes", config.idulFitriSholatMinutes)
            put("idulAdhaEnabled", config.idulAdhaEnabled)
            put("idulAdhaDate", config.idulAdhaDate)
            put("idulAdhaTime", config.idulAdhaTime)
            put("idulAdhaIqomahMinutes", config.idulAdhaIqomahMinutes)
            put("idulAdhaSholatMinutes", config.idulAdhaSholatMinutes)

            put("tarawihEnabled", config.tarawihEnabled)
            put("tarawihAutoDetectNight", config.tarawihAutoDetectNight)
            put("tarawihManualNight", config.tarawihManualNight)
            put("tarawihShowSlide", config.tarawihShowSlide)
            put("tarawihKultumMinutes", config.tarawihKultumMinutes)
            put("tarawihSholatMinutes", config.tarawihSholatMinutes)
            put("tarawihTitleText", config.tarawihTitleText)
            put("tarawihTitleColor", config.tarawihTitleColor)
            put("tarawihOfficerNameColor", config.tarawihOfficerNameColor)
            put("tarawihOfficerLabelColor", config.tarawihOfficerLabelColor)
            put("tarawihBgPreset", config.tarawihBgPreset)
            put("customTarawihBgPath", config.customTarawihBgPath)
        }
        json.put("config", cfgJson)

        val tarawihSchedules = repository.tarawihSchedulesFlow.first()
        val tarawihArray = JSONArray()
        for (t in tarawihSchedules) {
            tarawihArray.put(JSONObject().apply {
                put("night", t.night)
                put("date", t.date)
                put("penceramah", t.penceramah)
                put("penceramahPhone", t.penceramahPhone)
                put("judulKultum", t.judulKultum)
                put("imamTarawih", t.imamTarawih)
                put("imamTarawihPhone", t.imamTarawihPhone)
                put("imamWitir", t.imamWitir)
                put("imamWitirPhone", t.imamWitirPhone)
                put("bilalTarawih", t.bilalTarawih)
                put("bilalTarawihPhone", t.bilalTarawihPhone)
                put("notes", t.notes)
            })
        }
        json.put("tarawihSchedules", tarawihArray)
        val activeNight = com.example.viewmodel.MasjidTVViewModel.resolveActiveTarawihNight(config, "")
        json.put("activeTarawihNight", activeNight)

        val pbInfo = MurottalAudioPlayer.playbackInfo.value
        val pbJson = JSONObject().apply {
            put("state", pbInfo.state.name)
            put("isPlaying", pbInfo.isPlaying)
            put("title", pbInfo.title)
            put("qari", pbInfo.qari)
            put("surah", pbInfo.surah)
            put("volumePercent", pbInfo.volumePercent)
            put("currentPositionMs", pbInfo.currentPositionMs)
            put("durationMs", pbInfo.durationMs)
            put("errorMessage", pbInfo.errorMessage)
        }
        json.put("murottalPlayback", pbJson)

        val presetArray = JSONArray()
        for (p in MurottalPresetCatalogue.list) {
            presetArray.put(JSONObject().apply {
                put("id", p.id)
                put("qariName", p.qariName)
                put("surahName", p.surahName)
                put("audioUrl", p.audioUrl)
                put("durationText", p.durationText)
                put("description", p.description)
                put("category", p.category)
            })
        }
        json.put("murottalPresets", presetArray)

        val ytPresetArray = JSONArray()
        for (yp in YouTubeLiveHelper.PRESETS) {
            ytPresetArray.put(JSONObject().apply {
                put("id", yp.id)
                put("name", yp.name)
                put("url", yp.url)
                put("channelName", yp.channelName)
                put("description", yp.description)
            })
        }
        json.put("youtubePresets", ytPresetArray)

        val murottalAudios = repository.allMurottalAudiosFlow.first()
        val audioArray = JSONArray()
        for (a in murottalAudios) {
            audioArray.put(JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("qari", a.qari)
                put("surah", a.surah)
                put("durationSeconds", a.durationSeconds)
                put("filePath", a.filePath)
                put("prayerTime", a.prayerTime)
                put("isDefault", a.isDefault)
                put("createdAt", a.createdAt)
            })
        }
        json.put("murottalAudios", audioArray)

        val dailyImams = repository.dailyImamSchedulesFlow.first()
        val imamArray = JSONArray()
        for (im in dailyImams) {
            imamArray.put(JSONObject().apply {
                put("id", im.id)
                put("date", im.date)
                put("prayerName", im.prayerName.name)
                put("prayerDisplayName", im.prayerName.displayName)
                put("imamName", im.imamName)
                put("muadzinName", im.muadzinName)
                put("notes", im.notes)
            })
        }
        json.put("dailyImams", imamArray)

        val finArray = JSONArray()
        var totalIncome = 0L
        var totalExpense = 0L
        for (f in finances) {
            if (f.type == TransactionType.INCOME) totalIncome += f.amount else totalExpense += f.amount
            finArray.put(JSONObject().apply {
                put("id", f.id)
                put("title", f.title)
                put("amount", f.amount)
                put("type", f.type.name)
                put("category", f.category)
                put("date", f.date)
                put("notes", f.notes)
            })
        }
        json.put("finances", finArray)
        json.put("totalIncome", totalIncome)
        json.put("totalExpense", totalExpense)
        json.put("balance", totalIncome - totalExpense)

        val allFriday = repository.allFridaySchedulesFlow.first()
        val allFriArray = JSONArray()
        for (f in allFriday) {
            allFriArray.put(JSONObject().apply {
                put("id", f.id)
                put("date", f.date)
                put("hijriDate", f.hijriDate)
                put("khotib", f.khotib)
                put("khotibPhone", f.khotibPhone)
                put("imam", f.imam)
                put("imamPhone", f.imamPhone)
                put("muadzin", f.muadzin)
                put("muadzinPhone", f.muadzinPhone)
                put("bilal", f.bilal)
                put("bilalPhone", f.bilalPhone)
                put("khutbahTopic", f.khutbahTopic)
                put("notes", f.notes)
            })
        }
        json.put("allFriday", allFriArray)

        val upcomingWeek = com.example.viewmodel.MasjidTVViewModel.getUpcomingFridayWeekIndex(java.util.Calendar.getInstance())
        json.put("upcomingFridayWeek", upcomingWeek)

        val friJson = JSONObject().apply {
            put("id", friday.id)
            put("date", friday.date)
            put("hijriDate", friday.hijriDate)
            put("khotib", friday.khotib)
            put("khotibPhone", friday.khotibPhone)
            put("imam", friday.imam)
            put("imamPhone", friday.imamPhone)
            put("muadzin", friday.muadzin)
            put("muadzinPhone", friday.muadzinPhone)
            put("bilal", friday.bilal)
            put("bilalPhone", friday.bilalPhone)
            put("khutbahTopic", friday.khutbahTopic)
            put("notes", friday.notes)
        }
        json.put("friday", friJson)

        val actArray = JSONArray()
        for (a in activities) {
            actArray.put(JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("speaker", a.speaker)
                put("speakerPhone", a.speakerPhone)
                put("date", a.date)
                put("time", a.time)
                put("location", a.location)
                put("category", a.category)
            })
        }
        json.put("activities", actArray)

        val rtArray = JSONArray()
        for (rt in runningTexts) {
            rtArray.put(JSONObject().apply {
                put("id", rt.id)
                put("text", rt.text)
                put("isActive", rt.isActive)
            })
        }
        json.put("runningTexts", rtArray)

        val mediaArray = JSONArray()
        for (m in mediaSlides) {
            val fileName = File(m.filePath).name
            mediaArray.put(JSONObject().apply {
                put("id", m.id)
                put("filePath", m.filePath)
                put("webUrl", "/media/$fileName")
                put("title", m.title)
                put("durationSeconds", m.durationSeconds)
                put("isActive", m.isActive)
                put("order", m.order)
            })
        }
        json.put("mediaSlides", mediaArray)

        return json.toString()
    }

    private fun buildDashboardHtml(): String {
        return """
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pengaturan Layar TV MasjidKU</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
    <style>
        :root {
            --primary: #0D5C3A;
            --primary-dark: #073B24;
            --gold: #D4AF37;
            --gold-light: #F3E5AB;
            --bg: #F4F7F5;
            --card-bg: #FFFFFF;
            --text-dark: #1A2E26;
            --text-muted: #5A7569;
            --danger: #DC2626;
            --success: #16A34A;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
        body { background: var(--bg); color: var(--text-dark); padding-bottom: 60px; }
        header { background: linear-gradient(135deg, var(--primary-dark), var(--primary)); color: #fff; padding: 18px 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
        .header-content { max-width: 900px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
        .brand-title { font-size: 20px; font-weight: 800; color: var(--gold-light); display: flex; align-items: center; gap: 8px; }
        .brand-subtitle { font-size: 12px; color: rgba(255,255,255,0.8); }
        .live-badge { background: rgba(22,163,74,0.3); border: 1px solid #4ADE80; color: #4ADE80; font-size: 11px; padding: 3px 8px; border-radius: 12px; font-weight: bold; }
        
        .nav-tabs { background: #fff; max-width: 900px; margin: 12px auto 0; padding: 4px; display: flex; overflow-x: auto; border-radius: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); gap: 4px; }
        .tab-btn { background: none; border: none; padding: 10px 14px; font-size: 13px; font-weight: 600; color: var(--text-muted); cursor: pointer; border-radius: 8px; white-space: nowrap; flex: 1; text-align: center; }
        .tab-btn.active { background: var(--primary); color: #fff; }
        
        .container { max-width: 900px; margin: 16px auto; padding: 0 14px; }
        .section-tab { display: none; }
        .section-tab.active { display: block; }
        
        .card { background: var(--card-bg); border-radius: 12px; padding: 18px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); border: 1px solid rgba(0,0,0,0.06); }
        .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid #edf2ef; }
        .card-header h2 { font-size: 16px; color: var(--primary-dark); font-weight: 700; }
        
        .form-group { margin-bottom: 14px; }
        .form-group label { display: block; font-size: 12px; font-weight: 600; color: var(--text-dark); margin-bottom: 6px; }
        .form-control { width: 100%; padding: 10px 12px; font-size: 14px; border: 1px solid #cfdcd5; border-radius: 8px; background: #fafcfb; color: #111; }
        .form-control:focus { outline: none; border-color: var(--primary); background: #fff; box-shadow: 0 0 0 2px rgba(13,92,58,0.2); }
        
        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
        .grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
        @media (max-width: 600px) { .grid-2, .grid-3 { grid-template-columns: 1fr; } }
        
        .btn-primary { background: var(--primary); color: #fff; border: none; padding: 10px 18px; font-size: 13px; font-weight: 600; border-radius: 8px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; gap: 6px; width: 100%; transition: all 0.2s; }
        .btn-primary:hover { background: var(--primary-dark); }
        .btn-gold { background: linear-gradient(135deg, #B89726, var(--gold)); color: #111; border: none; padding: 10px 18px; font-size: 13px; font-weight: 700; border-radius: 8px; cursor: pointer; width: 100%; }
        .btn-danger { background: #FEE2E2; color: var(--danger); border: 1px solid #FCA5A5; padding: 6px 12px; font-size: 12px; font-weight: 600; border-radius: 6px; cursor: pointer; }
        .btn-danger:hover { background: var(--danger); color: #fff; }
        .btn-secondary { background: #E5E7EB; color: #374151; border: none; padding: 6px 12px; font-size: 12px; font-weight: 600; border-radius: 6px; cursor: pointer; }
        
        .item-list { list-style: none; }
        .item-row { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #fafcfb; border: 1px solid #e5ede8; border-radius: 8px; margin-bottom: 8px; }
        .item-meta { flex: 1; }
        .item-title { font-size: 13px; font-weight: 700; color: var(--primary-dark); }
        .item-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
        
        .stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 16px; }
        @media (max-width: 600px) { .stat-grid { grid-template-columns: 1fr; } }
        .stat-box { background: #fff; padding: 14px; border-radius: 10px; text-align: center; border: 1px solid #e0eae4; }
        .stat-box.balance { border-top: 4px solid var(--primary); }
        .stat-box.income { border-top: 4px solid var(--success); }
        .stat-box.expense { border-top: 4px solid var(--danger); }
        .stat-val { font-size: 18px; font-weight: 800; margin-top: 4px; }
        .stat-title { font-size: 11px; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }
        
        .toast { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); background: #111827; color: #fff; padding: 10px 20px; border-radius: 20px; font-size: 13px; font-weight: 600; box-shadow: 0 4px 12px rgba(0,0,0,0.3); opacity: 0; pointer-events: none; transition: opacity 0.3s; z-index: 1000; }
        .toast.show { opacity: 1; }
        
        .pill-switch { display: flex; background: #eef4f0; padding: 3px; border-radius: 8px; margin-bottom: 12px; gap: 4px; }
        .pill-btn { flex: 1; border: none; background: transparent; padding: 7px 10px; font-size: 12px; font-weight: 600; color: var(--text-muted); border-radius: 6px; cursor: pointer; text-align: center; }
        .pill-btn.active { background: #fff; color: var(--primary); box-shadow: 0 2px 4px rgba(0,0,0,0.06); font-weight: 700; }

        .img-preview-box { width: 100%; border-radius: 10px; border: 2px dashed #cfdcd5; padding: 12px; text-align: center; margin-bottom: 12px; background: #fafcfb; }
        .img-preview-box img { max-width: 100%; max-height: 200px; border-radius: 8px; object-fit: contain; }

        /* Login Screen Styling */
        #loginSection {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #022018 0%, #064E3B 50%, #0B3322 100%);
            padding: 20px;
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            z-index: 9999;
        }
        .login-card {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.4), 0 0 0 1px rgba(212,175,55,0.3);
            width: 100%;
            max-width: 420px;
            overflow: hidden;
            border: 2px solid #D4AF37;
        }
        .login-header {
            background: linear-gradient(135deg, #073B24 0%, #0D5C3A 100%);
            color: #ffffff;
            padding: 28px 24px;
            text-align: center;
            border-bottom: 3px solid #D4AF37;
        }
        .login-header .mosque-icon {
            font-size: 40px;
            margin-bottom: 8px;
            display: inline-block;
        }
        .login-header h1 {
            font-size: 20px;
            font-weight: 800;
            color: #F3E5AB;
            letter-spacing: 0.5px;
        }
        .login-header p {
            font-size: 12px;
            color: #A5D6A7;
            margin-top: 4px;
        }
        .login-body {
            padding: 24px;
        }
        .login-input-wrap {
            position: relative;
            margin-bottom: 16px;
        }
        .login-input-wrap input {
            width: 100%;
            padding: 12px 44px 12px 14px;
            font-size: 16px;
            border: 2px solid #cfdcd5;
            border-radius: 10px;
            background: #fafcfb;
            color: #111;
            box-sizing: border-box;
            letter-spacing: 2px;
        }
        .login-input-wrap input:focus {
            outline: none;
            border-color: #0D5C3A;
            box-shadow: 0 0 0 3px rgba(13,92,58,0.2);
            background: #fff;
        }
        .login-toggle-eye {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            background: none;
            border: none;
            cursor: pointer;
            font-size: 18px;
            color: #5A7569;
            padding: 4px;
        }
        .login-error {
            background: #FEE2E2;
            color: #DC2626;
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            border: 1px solid #FCA5A5;
            margin-bottom: 16px;
            display: none;
            text-align: center;
        }
        .login-hint {
            font-size: 11px;
            color: #5A7569;
            text-align: center;
            margin-top: 16px;
            line-height: 1.5;
            background: #F4F7F5;
            padding: 10px;
            border-radius: 8px;
            border: 1px solid #E0EAE4;
        }
        .btn-logout {
            background: rgba(220,38,38,0.2);
            border: 1px solid #EF4444;
            color: #FECACA;
            font-size: 11px;
            padding: 4px 10px;
            border-radius: 8px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s;
        }
        .btn-logout:hover {
            background: #DC2626;
            color: #fff;
        }

        /* Universal Modal Popup Styling */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.72);
            z-index: 10000;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 16px;
            backdrop-filter: blur(4px);
        }
        .modal-box {
            background: #fff;
            border-radius: 14px;
            max-width: 580px;
            width: 100%;
            max-height: 90vh;
            overflow-y: auto;
            box-shadow: 0 20px 40px rgba(0,0,0,0.35);
            border: 2px solid #0D5C3A;
            animation: modalFadeIn 0.22s ease-out;
        }
        @keyframes modalFadeIn {
            from { transform: scale(0.92) translateY(10px); opacity: 0; }
            to { transform: scale(1) translateY(0); opacity: 1; }
        }
        .modal-header {
            background: linear-gradient(135deg, #073B24, #0D5C3A);
            color: #fff;
            padding: 14px 18px;
            border-radius: 12px 12px 0 0;
            display: flex;
            justify-content: space-between;
            align-items: center;
            position: sticky;
            top: 0;
            z-index: 10;
        }
        .modal-header h3 {
            font-size: 16px;
            font-weight: 800;
            color: #F3E5AB;
            margin: 0;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .modal-close {
            background: rgba(255,255,255,0.15);
            border: none;
            color: #fff;
            font-size: 20px;
            cursor: pointer;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            line-height: 1;
            transition: all 0.2s;
        }
        .modal-close:hover {
            background: #DC2626;
        }
        .modal-body {
            padding: 20px;
            color: #0F172A;
        }
        .modal-body label {
            display: block;
            font-size: 13px;
            font-weight: 700;
            color: #0F172A !important;
            margin-bottom: 5px;
        }
        .form-input {
            width: 100%;
            padding: 10px 13px;
            font-size: 14px;
            font-weight: 600;
            border: 1.5px solid #94A3B8;
            border-radius: 8px;
            background: #F8FAFC;
            color: #0F172A;
            transition: all 0.2s;
            box-sizing: border-box;
            font-family: inherit;
        }
        .form-input:focus {
            outline: none;
            border-color: #059669;
            background: #FFFFFF;
            box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.2);
        }
        /* Placeholders dengan kontras tinggi dan sangat jelas */
        .form-input::placeholder,
        .form-control::placeholder,
        input::placeholder,
        textarea::placeholder {
            color: #475569 !important;
            font-weight: 600 !important;
            opacity: 1 !important;
        }
        .form-input::-webkit-input-placeholder,
        .form-control::-webkit-input-placeholder,
        input::-webkit-input-placeholder,
        textarea::-webkit-input-placeholder {
            color: #475569 !important;
            font-weight: 600 !important;
            opacity: 1 !important;
        }
        .color-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 14px;
            background: #F1F5F9;
            border-radius: 8px;
            border: 1.5px solid #CBD5E1;
            margin-bottom: 8px;
        }
        .color-row label, .color-row span {
            font-size: 13px;
            font-weight: 700;
            color: #0F172A !important;
            margin: 0;
        }
        .color-picker-wrap {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .color-picker-wrap input[type="color"] {
            -webkit-appearance: none;
            border: none;
            width: 36px;
            height: 36px;
            border-radius: 8px;
            cursor: pointer;
            padding: 0;
            background: none;
        }
        .color-picker-wrap input[type="color"]::-webkit-color-swatch-wrapper {
            padding: 0;
        }
        .color-picker-wrap input[type="color"]::-webkit-color-swatch {
            border: 2px solid #fff;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.3);
        }
        .color-hex-text {
            font-family: monospace;
            font-size: 12px;
            font-weight: bold;
            color: #374151;
            width: 76px;
            text-align: center;
            padding: 5px;
            border: 1px solid #D1D5DB;
            border-radius: 6px;
            background: #fff;
        }
    </style>
</head>
<body>
    <!-- UNIVERSAL MODAL POPUP DI WEB ADMIN -->
    <div id="universalModal" class="modal-overlay" style="display:none;" onclick="if(event.target===this)closeUniversalModal()">
        <div class="modal-box">
            <div class="modal-header">
                <h3 id="modalTitle">✏️ Edit Data</h3>
                <button type="button" class="modal-close" onclick="closeUniversalModal()">&times;</button>
            </div>
            <div class="modal-body" id="modalBody">
            </div>
        </div>
    </div>
    <!-- LAYAR LOGIN PENGURUS DKM -->
    <div id="loginSection">
        <div class="login-card">
            <div class="login-header">
                <div class="mosque-icon">🕌</div>
                <h1>MasjidKU TV Remote</h1>
                <p id="loginMosqueName">Portal Keamanan Pengurus Masjid</p>
            </div>
            <div class="login-body">
                <div id="loginError" class="login-error"></div>
                <form id="formLogin" onsubmit="handleLoginSubmit(event)">
                    <div class="form-group">
                        <label style="font-size:13px; font-weight:700; color:#1A2E26; margin-bottom:8px; display:block;">
                            🔐 Masukkan PIN / Password Pengurus
                        </label>
                        <div class="login-input-wrap">
                            <input type="password" id="inputPin" placeholder="Masukkan PIN (Default: 123456)" required autofocus autocomplete="current-password">
                            <button type="button" class="login-toggle-eye" onclick="togglePinVisibility()" title="Lihat/Sembunyikan PIN">👁️</button>
                        </div>
                    </div>
                    <button type="submit" class="btn-gold" style="font-size:15px; padding:12px; font-weight:800; letter-spacing:0.5px;" id="btnLoginSubmit">
                        🔑 Masuk ke Web Remote
                    </button>
                </form>
                <div class="login-hint">
                    💡 <strong>Default PIN Awal:</strong> <code>123456</code><br>
                    PIN dapat dilihat atau diubah di layar TV via menu <strong>Pengaturan TV ➔ Tab HP</strong>.
                </div>
            </div>
        </div>
    </div>

    <!-- KONTEN DASHBOARD UTAMA -->
    <div id="dashboardSection" style="display:none">
        <header>
            <div class="header-content">
                <div>
                    <div class="brand-title">🕌 TV MASJIDKU REMOTE</div>
                    <div class="brand-subtitle" id="headerMosqueName">Memuat data masjid...</div>
                </div>
                <div style="display:flex; flex-direction:column; align-items:flex-end; gap:5px;">
                    <div style="display:flex; align-items:center; gap:8px;">
                        <div class="live-badge">🟢 TERHUBUNG</div>
                        <button class="btn-logout" onclick="handleLogout()" title="Kunci & Keluar">🔒 Keluar</button>
                    </div>
                    <div style="font-size:11px; text-align:right;">
                        <a href="https://app.alijaya.com/donasi" target="_blank" rel="noopener noreferrer" style="color:#FDE68A; text-decoration:none; font-weight:700; background:rgba(217,119,6,0.35); border:1px solid rgba(253,230,138,0.5); padding:2px 8px; border-radius:6px; display:inline-flex; align-items:center; gap:4px; transition:all 0.2s;" onmouseover="this.style.background='rgba(217,119,6,0.6)';this.style.borderColor='#FDE68A'" onmouseout="this.style.background='rgba(217,119,6,0.35)';this.style.borderColor='rgba(253,230,138,0.5)'">
                            ❤️ Design by <u>ALIJAYA-NET</u>
                        </a>
                    </div>
                </div>
            </div>
        </header>

    <div class="nav-tabs">
        <button class="tab-btn active" onclick="switchTab('tab-profil')">🏛️ Profil</button>
        <button class="tab-btn" onclick="switchTab('tab-waktu')">⏰ Sholat</button>
        <button class="tab-btn" onclick="switchTab('tab-tarawih')">🌙 Tarawih Ramadhan</button>
        <button class="tab-btn" onclick="switchTab('tab-murottal')">🎧 Murottal & Qari</button>
        <button class="tab-btn" onclick="switchTab('tab-youtube')">🔴 Siaran Live & CCTV</button>
        <button class="tab-btn" onclick="switchTab('tab-keuangan')">💰 Keuangan</button>
        <button class="tab-btn" onclick="switchTab('tab-jumat')">📋 Petugas Sholat</button>
        <button class="tab-btn" onclick="switchTab('tab-whatsapp')">💬 WhatsApp Gateway</button>
        <button class="tab-btn" onclick="switchTab('tab-kegiatan')">📅 Kegiatan</button>
        <button class="tab-btn" onclick="switchTab('tab-running')">📜 Running Text</button>
        <button class="tab-btn" onclick="switchTab('tab-media')">🖼️ Gambar & Background</button>
        <button class="tab-btn" onclick="switchTab('tab-video')">🎬 Video Lokal</button>
        <button class="tab-btn" onclick="switchTab('tab-kontrol')">⚡ Tampilan & Kontrol</button>
    </div>

    <div class="container">
        <!-- 1. TAB PROFIL -->
        <div id="tab-profil" class="section-tab active">
            <div class="card">
                <div class="card-header">
                    <h2>Identitas & Lokasi Masjid</h2>
                </div>
                <form id="formProfil" onsubmit="saveProfil(event)">
                    <div class="form-group">
                        <label>Nama Masjid</label>
                        <input type="text" id="cfgMosqueName" class="form-control" placeholder="Contoh: Masjid Raya Al-Falah" required>
                    </div>
                    <div class="form-group">
                        <label>Tagline / Motto</label>
                        <input type="text" id="cfgTagline" class="form-control" placeholder="Motto masjid">
                    </div>
                    <div class="form-group">
                        <label>Alamat Lengkap</label>
                        <textarea id="cfgAddress" class="form-control" rows="2" placeholder="Jl. ..."></textarea>
                    </div>
                    <!-- KARTU PETA INTERAKTIF & KOORDINAT PRESISI -->
                    <div class="form-group" style="background:#F0FDF4; padding:14px; border-radius:12px; border:1.5px solid #86EFAC; margin-bottom:15px;">
                        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px; margin-bottom:8px;">
                            <label style="color:#0D5C3A; font-weight:800; font-size:14px; margin:0; display:flex; align-items:center; gap:6px;">
                                🗺️ Peta Lokasi Masjid (Pilih Titik Koordinat Langsung)
                            </label>
                            <button type="button" onclick="detectCurrentLocation()" class="btn-gold" style="padding:5px 12px; font-size:12px; border-radius:6px; font-weight:700; width:auto; display:inline-flex; align-items:center; gap:4px; cursor:pointer;">
                                📍 Gunakan GPS Saya
                            </button>
                        </div>
                        
                        <p style="font-size:12px; color:#065F46; margin:0 0 10px 0;">
                            Klik/ketuk langsung pada peta atau geser pin merah 📍 tepat di atas atap kubah masjid Anda. Koordinat latitude & longitude otomatis terisi presisi untuk perhitungan waktu sholat akurat.
                        </p>

                        <!-- Pilihan Cepat Kota & Pencarian Alamat -->
                        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px; margin-bottom:10px;">
                            <div>
                                <select id="cfgCitySelect" class="form-control" onchange="onCitySelect(this.value)" style="font-size:13px;">
                                    <option value="">-- Lompat Cepat ke Kota / Kabupaten --</option>
                                </select>
                            </div>
                            <div style="display:flex; gap:4px;">
                                <input type="text" id="mapSearchInput" class="form-control" placeholder="Cari desa/kecamatan..." style="font-size:13px;" onkeydown="if(event.key==='Enter'){event.preventDefault();searchMapAddress();}">
                                <button type="button" onclick="searchMapAddress()" class="btn-primary" style="padding:6px 12px; width:auto; font-size:12px; border-radius:6px; white-space:nowrap; cursor:pointer;">
                                    🔍 Cari
                                </button>
                            </div>
                        </div>

                        <!-- Wadah Peta Leaflet -->
                        <div id="mosqueMap" style="height:320px; width:100%; border-radius:10px; border:2px solid #059669; box-shadow:0 2px 8px rgba(0,0,0,0.1); background:#E2E8F0;"></div>

                        <!-- Status Koordinat Terpilih & Zona Waktu -->
                        <div id="mapCoordsBadge" style="margin-top:10px; background:#FFFFFF; padding:8px 12px; border-radius:8px; border:1px solid #BBF7D0; font-size:12.5px; color:#065F46; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:6px;">
                            <span id="cityInfoText">📍 <strong>Titik:</strong> Memuat peta lokasi...</span>
                            <span id="tzBadge" style="background:#DEF7EC; color:#03543F; font-weight:700; padding:2px 8px; border-radius:4px; font-size:11px;">WIB (GMT+7)</span>
                        </div>
                    </div>

                    <div class="grid-3" style="margin-bottom:12px;">
                        <div class="form-group">
                            <label style="font-size:12px;">Latitude</label>
                            <input type="number" step="any" id="cfgLat" class="form-control" oninput="onManualCoordChange()">
                        </div>
                        <div class="form-group">
                            <label style="font-size:12px;">Longitude</label>
                            <input type="number" step="any" id="cfgLng" class="form-control" oninput="onManualCoordChange()">
                        </div>
                        <div class="form-group">
                            <label style="font-size:12px;">Zona Waktu (GMT+)</label>
                            <input type="number" step="0.5" id="cfgTimezone" class="form-control">
                        </div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Nama Wilayah / Kota di Layar</label>
                            <input type="text" id="cfgCity" class="form-control">
                        </div>
                        <div class="form-group">
                            <label>No. Kontak DKM</label>
                            <input type="text" id="cfgPhone" class="form-control">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Nama Bank Infaq</label>
                        <input type="text" id="cfgBankName" class="form-control" placeholder="BSI / BCA / Mandiri">
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>No. Rekening</label>
                            <input type="text" id="cfgBankAccount" class="form-control">
                        </div>
                        <div class="form-group">
                            <label>Atas Nama (A/N)</label>
                            <input type="text" id="cfgBankAccountName" class="form-control">
                        </div>
                    </div>
                    <button type="submit" class="btn-primary">💾 Simpan Identitas & Lokasi Masjid</button>
                </form>
            </div>

            <!-- CARD KONTROL TAMPILAN KONTEN TV (TAB 1) -->
            <div class="card" style="border: 2px solid #10B981; background: #F0FDF4;">
                <div class="card-header" style="background:#065F46; padding:10px 14px; border-radius:8px 8px 0 0;">
                    <h2 style="color:#fff; margin:0; font-size:15px; display:flex; align-items:center; gap:8px;">
                        🎛️ Kontrol Tampilan Konten & Slide TV
                    </h2>
                </div>
                <div style="padding:14px;">
                    <p style="font-size:12px; color:#065F46; margin-top:0; margin-bottom:10px;">
                        Centang konten yang ingin diaktifkan di layar TV. Jika centang dilepas, konten <strong>100% tidak akan pernah muncul di layar TV</strong>:
                    </p>
                    <div class="grid-2" style="font-size:13px; gap:10px;">
                        <label style="display:flex; align-items:center; gap:8px; cursor:pointer; background:#fff; padding:8px 10px; border-radius:6px; border:1px solid #D1E7DD;"><input type="checkbox" id="showFinReportTab1" onchange="syncAndSaveContentVisibility('showFinReportTab1', 'showFinReport')"> 💳 Laporan Kas Keuangan</label>
                        <label style="display:flex; align-items:center; gap:8px; cursor:pointer; background:#fff; padding:8px 10px; border-radius:6px; border:1px solid #D1E7DD;"><input type="checkbox" id="showFriOfficersTab1" onchange="syncAndSaveContentVisibility('showFriOfficersTab1', 'showFriOfficers')"> 👥 Petugas Sholat Jum'at</label>
                        <label style="display:flex; align-items:center; gap:8px; cursor:pointer; background:#fff; padding:8px 10px; border-radius:6px; border:1px solid #D1E7DD;"><input type="checkbox" id="showQrisCardTab1" onchange="syncAndSaveContentVisibility('showQrisCardTab1', 'showQrisCard')"> 📱 Infaq QRIS & Donasi</label>
                        <label style="display:flex; align-items:center; gap:8px; cursor:pointer; background:#fff; padding:8px 10px; border-radius:6px; border:1px solid #D1E7DD;"><input type="checkbox" id="showDailyHadithTab1" onchange="syncAndSaveContentVisibility('showDailyHadithTab1', 'showDailyHadith')"> 📖 Mutiara Hadits Shahih</label>
                        <label style="display:flex; align-items:center; gap:8px; cursor:pointer; background:#fff; padding:8px 10px; border-radius:6px; border:1px solid #D1E7DD;"><input type="checkbox" id="showActivitiesTab1" onchange="syncAndSaveContentVisibility('showActivitiesTab1', 'showActivities')"> 📅 Agenda & Kajian</label>
                        <label style="display:flex; align-items:center; gap:8px; cursor:pointer; background:#fff; padding:8px 10px; border-radius:6px; border:1px solid #D1E7DD;"><input type="checkbox" id="showDailyMaklumatTab1" onchange="syncAndSaveContentVisibility('showDailyMaklumatTab1', 'showDailyMaklumat')"> 📢 Maklumat & Adab Masjid</label>
                    </div>
                    <small style="color:#047857; margin-top:8px; display:block;">💡 Perubahan centang akan otomatis disimpan dan langsung diterapkan ke layar TV secara instan.</small>
                </div>
            </div>

            <!-- CARD GANTI PIN / PASSWORD WEB REMOTE -->
            <div class="card" style="border: 2px solid #D4AF37; background: linear-gradient(180deg, #FFFFFF 0%, #FAFCF9 100%);">
                <div class="card-header" style="border-bottom: 2px solid #F3E5AB;">
                    <h2 style="color: #073B24; display: flex; align-items: center; gap: 8px;">
                        🔐 Ganti PIN / Password Web Remote (ip:8080)
                    </h2>
                </div>
                <div style="background: #F0F8F4; padding: 10px 14px; border-radius: 8px; font-size: 12px; color: #0D5C3A; margin-bottom: 14px; border: 1px solid #D1E7DD;">
                    🛡️ <strong>Keamanan Pengurus:</strong> PIN ini digunakan saat mengakses alamat web remote TV (<code>ip:8080</code>) agar hanya pengurus DKM yang dapat mengubah data masjid.
                </div>
                <div id="changePassAlert" style="display:none; padding:10px; border-radius:8px; font-size:13px; margin-bottom:12px;"></div>
                <form id="formChangePassword" onsubmit="handleChangePasswordSubmit(event)">
                    <div class="grid-3">
                        <div class="form-group">
                            <label>PIN / Password Saat Ini</label>
                            <input type="password" id="pwdOld" class="form-control" placeholder="Default: 123456" required>
                        </div>
                        <div class="form-group">
                            <label>PIN / Password Baru</label>
                            <input type="password" id="pwdNew" class="form-control" placeholder="Minimal 4 karakter/angka" minlength="4" required>
                        </div>
                        <div class="form-group">
                            <label>Konfirmasi PIN Baru</label>
                            <input type="password" id="pwdConfirm" class="form-control" placeholder="Ulangi PIN baru" minlength="4" required>
                        </div>
                    </div>
                    <button type="submit" class="btn-gold" style="padding:10px 18px; font-size:13px; font-weight:700; width:auto; display:inline-flex; align-items:center; gap:6px;">
                        💾 Simpan & Perbarui PIN
                    </button>
                </form>
            </div>
        </div>

        <!-- 2. TAB WAKTU SHOLAT -->
        <div id="tab-waktu" class="section-tab">
            <div class="card">
                <div class="card-header">
                    <h2>Pilihan Waktu Sholat & Koreksi Menit</h2>
                </div>
                <form id="formWaktu" onsubmit="saveWaktu(event)">
                    <div style="background:#F0F8F5; padding:12px; border-radius:10px; border:1px solid #D1E7DD; margin-bottom:14px;">
                        <h3 style="font-size:13px; margin-bottom:8px; color:var(--primary);">Pilih Waktu Sholat yang Ditampilkan di Layar TV</h3>
                        <div class="grid-3" style="font-size:13px;">
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showImsak"> Imsak</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showSubuh"> Subuh</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showTerbit"> Terbit</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showDhuha"> Dhuha</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showDzuhur"> Dzuhur</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showAshar"> Ashar</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showMaghrib"> Maghrib</label>
                            <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showIsya"> Isya'</label>
                        </div>
                    </div>
                    <div class="grid-3">
                        <div class="form-group"><label>Latitude</label><input type="number" step="any" id="cfgLat2" class="form-control" oninput="document.getElementById('cfgLat').value=this.value; onManualCoordChange();"></div>
                        <div class="form-group"><label>Longitude</label><input type="number" step="any" id="cfgLng2" class="form-control" oninput="document.getElementById('cfgLng').value=this.value; onManualCoordChange();"></div>
                        <div class="form-group"><label>Zona Waktu (GMT+)</label><input type="number" step="0.5" id="cfgTimezone2" class="form-control" oninput="document.getElementById('cfgTimezone').value=this.value;"></div>
                    </div>
                    <div class="form-group">
                        <label>Koreksi Tanggal Hijriyah (Hari)</label>
                        <input type="number" id="cfgHijri" class="form-control" placeholder="0 (misal: +1 atau -1)">
                    </div>
                    <h3 style="font-size:14px; margin:14px 0 8px; color:var(--primary);">Koreksi Menit Manual (+ / - Menit)</h3>
                    <div class="grid-3">
                        <div class="form-group"><label>Subuh</label><input type="number" id="offSubuh" class="form-control"></div>
                        <div class="form-group"><label>Terbit</label><input type="number" id="offTerbit" class="form-control"></div>
                        <div class="form-group"><label>Dzuhur</label><input type="number" id="offDzuhur" class="form-control"></div>
                        <div class="form-group"><label>Ashar</label><input type="number" id="offAshar" class="form-control"></div>
                        <div class="form-group"><label>Maghrib</label><input type="number" id="offMaghrib" class="form-control"></div>
                        <div class="form-group"><label>Isya'</label><input type="number" id="offIsya" class="form-control"></div>
                    </div>
                    <h3 style="font-size:14px; margin:14px 0 8px; color:var(--primary);">Durasi Countdown Jeda Iqomah (Menit)</h3>
                    <div class="grid-2">
                        <div class="form-group"><label>Iqomah Subuh</label><input type="number" id="iqSubuh" class="form-control"></div>
                        <div class="form-group"><label>Iqomah Dzuhur (Hari Biasa)</label><input type="number" id="iqDzuhur" class="form-control"></div>
                        <div class="form-group"><label>🕌 Iqomah Khusus Sholat Jum'at</label><input type="number" id="iqJumat" class="form-control" placeholder="15"></div>
                        <div class="form-group"><label>Iqomah Ashar</label><input type="number" id="iqAshar" class="form-control"></div>
                        <div class="form-group"><label>Iqomah Maghrib</label><input type="number" id="iqMaghrib" class="form-control"></div>
                        <div class="form-group"><label>Iqomah Isya'</label><input type="number" id="iqIsya" class="form-control"></div>
                        <div class="form-group"><label>Layar Hening Sholat</label><input type="number" id="sholatDuration" class="form-control"></div>
                    </div>
                    <button type="submit" class="btn-primary">💾 Simpan Visibilitas & Pengaturan Waktu</button>
                </form>
            </div>

            <!-- CARD PRE-ADHAN COUNTDOWN & IMAM 5 WAKTU -->
            <div class="card" style="border: 2px solid #0D5C3A; background: linear-gradient(180deg, #FFFFFF 0%, #F4FAF7 100%);">
                <div class="card-header" style="border-bottom: 2px solid #D1E7DD;">
                    <h2 style="color: #073B24; display: flex; align-items: center; gap: 8px;">
                        ⏳ Hitung Mundur Pra-Adzan & 👳 Imam Sholat 5 Waktu
                    </h2>
                </div>
                <div style="background: #E8F5E9; padding: 12px; border-radius: 10px; border: 1px solid #C8E6C9; margin-bottom: 16px;">
                    <label style="display: flex; align-items: center; gap: 10px; font-weight: 700; color: #1B5E20; cursor: pointer; margin-bottom: 8px;">
                        <input type="checkbox" id="enablePreAdhanCountdown" style="width: 18px; height: 18px;">
                        Aktifkan Layar Hitung Mundur Jelang Adzan (Pra-Adzan)
                    </label>
                    <div style="font-size: 12px; color: #2E7D32; margin-bottom: 10px;">
                        Layar countdown otomatis tampil sebelum adzan berkumandang dengan timer detik, ajakan wudhu, dan nama imam yang bertugas.
                    </div>
                    <div class="form-group" style="margin-bottom: 0;">
                        <label style="font-size: 12px; font-weight: 700; color: #1B5E20;">Durasi Hitung Mundur Pra-Adzan (Detik):</label>
                        <input type="number" id="preAdhanCountdownSeconds" class="form-control" placeholder="Default: 30" min="10" max="600" style="max-width: 200px;">
                    </div>
                </div>

                <h3 style="font-size: 14px; margin: 14px 0 8px; color: var(--primary);">👳 Jadwal Nama Imam & Muadzin Harian (Default 5 Waktu)</h3>
                <div style="font-size: 12px; color: #5A7569; margin-bottom: 12px;">
                    Nama imam akan otomatis tayang di layar TV saat pra-adzan, adzan, iqomah, dan sholat berjamaah.
                </div>

                <div class="grid-2">
                    <div style="background: #FAFDFB; padding: 10px; border-radius: 8px; border: 1px solid #E0EDE7;">
                        <strong style="color: #0D5C3A; font-size: 13px;">🌅 Sholat Subuh</strong>
                        <div class="form-group" style="margin-top: 6px;"><label style="font-size: 11px;">Nama Imam</label><input type="text" id="imamSubuh" class="form-control" placeholder="Contoh: Ust. H. Ahmad Dahlan, Lc."></div>
                        <div class="form-group" style="margin-bottom: 0;"><label style="font-size: 11px;">Nama Muadzin (Opsional)</label><input type="text" id="muadzinSubuh" class="form-control" placeholder="Nama muadzin"></div>
                    </div>
                    <div style="background: #FAFDFB; padding: 10px; border-radius: 8px; border: 1px solid #E0EDE7;">
                        <strong style="color: #0D5C3A; font-size: 13px;">☀️ Sholat Dzuhur</strong>
                        <div class="form-group" style="margin-top: 6px;"><label style="font-size: 11px;">Nama Imam</label><input type="text" id="imamDzuhur" class="form-control" placeholder="Contoh: Ust. Ridwan Kamil"></div>
                        <div class="form-group" style="margin-bottom: 0;"><label style="font-size: 11px;">Nama Muadzin (Opsional)</label><input type="text" id="muadzinDzuhur" class="form-control" placeholder="Nama muadzin"></div>
                    </div>
                    <div style="background: #FAFDFB; padding: 10px; border-radius: 8px; border: 1px solid #E0EDE7;">
                        <strong style="color: #0D5C3A; font-size: 13px;">🌤️ Sholat Ashar</strong>
                        <div class="form-group" style="margin-top: 6px;"><label style="font-size: 11px;">Nama Imam</label><input type="text" id="imamAshar" class="form-control" placeholder="Contoh: Ust. Dr. Muhammad Iqbal"></div>
                        <div class="form-group" style="margin-bottom: 0;"><label style="font-size: 11px;">Nama Muadzin (Opsional)</label><input type="text" id="muadzinAshar" class="form-control" placeholder="Nama muadzin"></div>
                    </div>
                    <div style="background: #FAFDFB; padding: 10px; border-radius: 8px; border: 1px solid #E0EDE7;">
                        <strong style="color: #0D5C3A; font-size: 13px;">🌆 Sholat Maghrib</strong>
                        <div class="form-group" style="margin-top: 6px;"><label style="font-size: 11px;">Nama Imam</label><input type="text" id="imamMaghrib" class="form-control" placeholder="Contoh: Ust. H. Abdul Somad, Lc."></div>
                        <div class="form-group" style="margin-bottom: 0;"><label style="font-size: 11px;">Nama Muadzin (Opsional)</label><input type="text" id="muadzinMaghrib" class="form-control" placeholder="Nama muadzin"></div>
                    </div>
                </div>
                <div style="background: #FAFDFB; padding: 10px; border-radius: 8px; border: 1px solid #E0EDE7; margin-top: 10px;">
                    <strong style="color: #0D5C3A; font-size: 13px;">🌙 Sholat Isya'</strong>
                    <div class="grid-2" style="margin-top: 6px;">
                        <div class="form-group" style="margin-bottom: 0;"><label style="font-size: 11px;">Nama Imam</label><input type="text" id="imamIsya" class="form-control" placeholder="Contoh: Ust. Farhan Al-Hafizh"></div>
                        <div class="form-group" style="margin-bottom: 0;"><label style="font-size: 11px;">Nama Muadzin (Opsional)</label><input type="text" id="muadzinIsya" class="form-control" placeholder="Nama muadzin"></div>
                    </div>
                </div>

                <button type="button" class="btn-primary" onclick="saveImamAndPreAdhan()" style="margin-top: 14px;">
                    💾 Simpan Pengaturan Pra-Adzan & Imam Harian
                </button>
            </div>

            <!-- CARD JADWAL IMAM TANGGAL KHUSUS -->
            <div class="card">
                <div class="card-header">
                    <h2>📅 Jadwal Imam Sholat Tanggal Khusus</h2>
                </div>
                <div style="background: #FFF9E6; padding: 10px 14px; border-radius: 8px; font-size: 12px; color: #854D0E; margin-bottom: 14px; border: 1px solid #FEF08A;">
                    💡 <strong>Jadwal Khusus:</strong> Jika pada tanggal dan waktu sholat tertentu terdapat Imam tamu/khusus, masukkan di sini. Sistem akan memprioritaskan jadwal tanggal khusus ini dibanding jadwal default harian.
                </div>
                <form id="formSpecialImam" onsubmit="addSpecialImamSchedule(event)">
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Pilih Tanggal</label>
                            <input type="date" id="specialImamDate" class="form-control" required>
                        </div>
                        <div class="form-group">
                            <label>Pilih Waktu Sholat</label>
                            <select id="specialImamPrayer" class="form-control" required>
                                <option value="SUBUH">🌅 Subuh</option>
                                <option value="DZUHUR" selected>☀️ Dzuhur</option>
                                <option value="ASHAR">🌤️ Ashar</option>
                                <option value="MAGHRIB">🌆 Maghrib</option>
                                <option value="ISYA">🌙 Isya'</option>
                            </select>
                        </div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Nama Imam Sholat</label>
                            <input type="text" id="specialImamName" class="form-control" placeholder="Contoh: Syeikh Dr. Abdullah" required>
                        </div>
                        <div class="form-group">
                            <label>Nama Muadzin (Opsional)</label>
                            <input type="text" id="specialMuadzinName" class="form-control" placeholder="Nama muadzin">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Catatan / Keterangan (Opsional)</label>
                        <input type="text" id="specialImamNotes" class="form-control" placeholder="Misal: Imam Tamu dari Madinah">
                    </div>
                    <button type="submit" class="btn-gold" style="width: auto; padding: 10px 18px; font-size: 13px; font-weight: 700;">
                        ➕ Tambah Jadwal Imam Khusus
                    </button>
                </form>

                <h3 style="font-size: 14px; margin: 18px 0 8px; color: var(--primary);">Daftar Jadwal Imam Khusus Aktif</h3>
                <ul class="item-list" id="specialImamList"></ul>
            </div>
        </div>

        <!-- TAB TARAWIH RAMADHAN -->
        <div id="tab-tarawih" class="section-tab">
            <!-- 1. PENGATURAN GLOBAL TARAWIH & TAMPILAN TV -->
            <div class="card">
                <div class="card-header">
                    <h2>🌙 Pengaturan Global & Layar Tarawih Ramadhan</h2>
                    <span style="background:rgba(245,158,11,0.2); color:#F59E0B; font-size:11px; font-weight:700; padding:3px 8px; border-radius:12px; border:1px solid rgba(245,158,11,0.4);">Spesial Ramadhan</span>
                </div>
                <form id="formTarawihConfig" onsubmit="saveTarawihConfigForm(event)">
                    <div class="form-group" style="margin-bottom:14px;">
                        <label style="display:flex; align-items:center; gap:10px; cursor:pointer; font-weight:bold; color:#FDE68A; font-size:14px;">
                            <input type="checkbox" id="cfgTarawihEnabled" style="width:20px; height:20px; accent-color:#10B981;">
                            <span>Aktifkan Fitur & Tampilan Khusus Tarawih Ramadhan</span>
                        </label>
                        <small style="color:#94A3B8; font-size:11.5px; display:block; margin-top:4px;">
                            Jika aktif, TV akan otomatis menampilkan slide petugas Tarawih di Carousel dan menjalankan layar khusus Tarawih (Kultum & Sholat) saat waktu Isya' tiba.
                        </small>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>⚙️ Mode Deteksi Malam Tarawih</label>
                            <select id="cfgTarawihAutoDetect" class="form-control" onchange="toggleTarawihModeDisplay()">
                                <option value="true">Otomatis (Sesuai Tanggal Kalender Hijriyah TV)</option>
                                <option value="false">Manual (Pilih Malam Tarawih Tertentu)</option>
                            </select>
                        </div>
                        <div class="form-group" id="groupTarawihManualNight" style="display:none;">
                            <label>🌙 Pilih Malam Tarawih Aktif</label>
                            <select id="cfgTarawihManualNight" class="form-control">
                                <!-- Generated 1 to 30 -->
                            </select>
                        </div>
                    </div>

                    <div class="form-group" style="margin-bottom:14px;">
                        <label style="display:flex; align-items:center; gap:10px; cursor:pointer; font-weight:bold; color:#FFFFFF; font-size:13px;">
                            <input type="checkbox" id="cfgTarawihShowSlide" style="width:18px; height:18px; accent-color:#10B981;">
                            <span>Tampilkan Slide Petugas Tarawih di Rotasi Carousel Layar TV</span>
                        </label>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>⏳ Durasi Layar Kultum / Ceramah Tarawih (Menit)</label>
                            <input type="number" id="cfgTarawihKultumMinutes" class="form-control" min="1" max="60" value="15">
                            <small style="color:#94A3B8; font-size:11px;">Durasi tayang penceramah & judul kultum ba'da Isya'</small>
                        </div>
                        <div class="form-group">
                            <label>🕌 Durasi Layar Sholat Tarawih & Witir (Menit)</label>
                            <input type="number" id="cfgTarawihSholatMinutes" class="form-control" min="5" max="120" value="20">
                            <small style="color:#94A3B8; font-size:11px;">Durasi mode hening sholat tarawih & witir berlangsung</small>
                        </div>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>🖼️ Preset Background Slide Tarawih</label>
                            <select id="cfgTarawihBgPreset" class="form-control">
                                <option value="DEFAULT_ISLAMIC">Emerald Islamic Luxury (Default)</option>
                                <option value="RAMADHAN_LANTERN">Lentera Ramadhan Emas (Lantern Glow)</option>
                                <option value="MOSQUE_NIGHT">Masjid Malam Bertabur Bintang (Midnight Blue)</option>
                                <option value="GOLD_LUXURY">Gold Royal Arabesque (Kemilau Emas)</option>
                                <option value="MINIMAL_DARK">Minimalis Elegan Modern (Deep Emerald)</option>
                            </select>
                        </div>
                        <div class="form-group" style="display:flex; flex-direction:column; justify-content:flex-end;">
                            <button type="button" class="btn-secondary" onclick="openTarawihCustomizationModal()" style="padding:10px; font-weight:bold; color:#FDE68A; border-color:#F59E0B;">
                                🎨 Kustomisasi Teks & Warna Layar Tarawih
                            </button>
                        </div>
                    </div>

                    <button type="submit" class="btn-primary" style="margin-top:8px;">💾 Simpan Pengaturan Tarawih</button>
                </form>
            </div>

            <!-- 2. FORM JADWAL PETUGAS 30 MALAM TARAWIH -->
            <div class="card">
                <div class="card-header" style="flex-wrap:wrap; gap:8px;">
                    <div>
                        <h2>📋 Jadwal Petugas Tarawih & Kultum (30 Malam)</h2>
                        <div style="font-size:12px; color:#94A3B8;">Atur penceramah, imam sholat tarawih, imam witir, dan bilal per malam</div>
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="button" class="btn-secondary" onclick="populateDefaultTarawih()" style="font-size:12px; padding:6px 12px; color:#6EE7B7; border-color:#10B981;" title="Isi otomatis 30 malam dengan format default">✨ Auto-Isi 30 Malam</button>
                    </div>
                </div>

                <!-- Night Selector Pills Bar -->
                <div style="margin-bottom:12px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                        <label style="font-size:12px; font-weight:700; color:#FDE68A;">PILIH MALAM RAMADHAN:</label>
                        <select id="selQuickNight" class="form-control" style="width:auto; padding:4px 10px; font-size:12px;" onchange="switchTarawihNight(parseInt(this.value))">
                            <!-- Populated with Malam 1 to 30 -->
                        </select>
                    </div>
                    <div id="tarawihNightPills" style="display:flex; gap:6px; overflow-x:auto; padding-bottom:8px; scrollbar-width:thin;">
                        <!-- Generated 30 pill buttons -->
                    </div>
                </div>

                <!-- Alert Banner for Selected Night -->
                <div id="tarawihNightAlert" class="alert-info" style="margin-bottom:16px; display:flex; justify-content:space-between; align-items:center; background:rgba(245,158,11,0.15); border:1.5px solid #F59E0B; border-radius:8px; padding:10px 14px; color:#FDE68A;">
                    <div id="tarawihNightAlertText">Mengedit Jadwal Malam ke-1 Ramadhan</div>
                    <span id="tarawihNightLiveBadge" class="badge-tag" style="display:none; background:#10B981; color:#06281D; font-weight:bold; padding:2px 8px; border-radius:6px; font-size:11px;">🟢 SEDANG TAYANG DI TV</span>
                </div>

                <form id="formTarawihNight" onsubmit="saveTarawihNightForm(event)">
                    <input type="hidden" id="tarawihFormNight" value="1">

                    <div class="form-group">
                        <label>📅 Keterangan Tanggal Masehi / Keterangan Malam</label>
                        <input type="text" id="tarawihDate" class="form-control" placeholder="Contoh: 1 Ramadhan 1447 H / 28 Feb 2025" required>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>🎙️ Nama Penceramah / Kultum</label>
                            <input type="text" id="tarawihPenceramah" class="form-control" placeholder="Contoh: Ustadz Dr. H. Abdullah, M.Ag">
                        </div>
                        <div class="form-group">
                            <label>📱 No. WhatsApp Penceramah</label>
                            <input type="text" id="tarawihPenceramahPhone" class="form-control" placeholder="Contoh: 08123456789">
                        </div>
                    </div>

                    <div class="form-group">
                        <label>📖 Judul Ceramah / Tema Kultum Tarawih</label>
                        <input type="text" id="tarawihJudulKultum" class="form-control" placeholder="Contoh: Meraih Keberkahan di Awal Ramadhan">
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>🕌 Nama Imam Sholat Tarawih</label>
                            <input type="text" id="tarawihImamTarawih" class="form-control" placeholder="Contoh: Ustadz Ahmad Fauzi, S.Pd.I">
                        </div>
                        <div class="form-group">
                            <label>📱 No. WhatsApp Imam Tarawih</label>
                            <input type="text" id="tarawihImamTarawihPhone" class="form-control" placeholder="Contoh: 08123456789">
                        </div>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>🌙 Nama Imam Sholat Witir</label>
                            <input type="text" id="tarawihImamWitir" class="form-control" placeholder="Contoh: Ustadz Ahmad Fauzi / Sesuai Tarawih">
                        </div>
                        <div class="form-group">
                            <label>📱 No. WhatsApp Imam Witir</label>
                            <input type="text" id="tarawihImamWitirPhone" class="form-control" placeholder="Contoh: 08123456789">
                        </div>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>📢 Nama Bilal / Muadzin Tarawih</label>
                            <input type="text" id="tarawihBilal" class="form-control" placeholder="Contoh: Ustadz Bilal Ramadhan">
                        </div>
                        <div class="form-group">
                            <label>📱 No. WhatsApp Bilal</label>
                            <input type="text" id="tarawihBilalPhone" class="form-control" placeholder="Contoh: 08123456789">
                        </div>
                    </div>

                    <div class="form-group">
                        <label>📝 Catatan Tambahan (Opsional)</label>
                        <input type="text" id="tarawihNotes" class="form-control" placeholder="Contoh: Petugas buka puasa bersama takmir">
                    </div>

                    <div style="display:flex; flex-wrap:wrap; gap:10px; margin-top:16px;">
                        <button type="submit" class="btn-primary" style="flex:2; min-width:200px; padding:12px; font-size:14px;">💾 Simpan Petugas Malam Ini</button>
                        <button type="button" onclick="sendWaTarawihBroadcast()" style="flex:1; min-width:180px; padding:12px; font-size:13px; font-weight:bold; background:#25D366; color:#06281D; border:none; border-radius:8px; cursor:pointer;" title="Kirim pesan WhatsApp pengingat ke seluruh petugas malam ini">📱 Kirim WA Broadcast</button>
                        <button type="button" class="btn-gold" onclick="simulateTarawihTv()" style="flex:1; min-width:180px; padding:12px; font-size:13px; font-weight:bold;" title="Tampilkan simulasi slide / layar tarawih di TV sekarang">📺 Simulasi TV</button>
                    </div>
                </form>
            </div>

            <!-- 3. TABEL RINGKASAN JADWAL 30 MALAM -->
            <div class="card">
                <div class="card-header" style="flex-wrap:wrap; gap:10px;">
                    <div>
                        <h2>📊 Ringkasan Jadwal 30 Malam Ramadhan</h2>
                        <div style="font-size:12px; color:#94A3B8;">Daftar lengkap penceramah dan imam dari malam ke-1 s/d ke-30</div>
                    </div>
                    <div style="width:240px;">
                        <input type="text" id="tarawihSearchInput" class="form-control" placeholder="🔍 Cari nama / judul / malam..." oninput="filterTarawihTable()" style="padding:6px 12px; font-size:12px;">
                    </div>
                </div>

                <div class="table-responsive" style="max-height:480px; overflow-y:auto;">
                    <table class="data-table" id="tableTarawih">
                        <thead>
                            <tr>
                                <th style="width:70px;">Malam</th>
                                <th style="width:140px;">Tanggal</th>
                                <th>Penceramah & Judul Kultum</th>
                                <th>Imam Tarawih & Witir</th>
                                <th>Bilal</th>
                                <th style="width:110px; text-align:center;">Aksi</th>
                            </tr>
                        </thead>
                        <tbody id="tarawihTableBody">
                            <!-- Populated dynamically via JS -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 3. TAB KEUANGAN (FULL CRUD) -->
        <div id="tab-keuangan" class="section-tab">
            <div class="stat-grid">
                <div class="stat-box balance">
                    <div class="stat-title">SALDO KAS MASJID SAAT INI</div>
                    <div class="stat-val" id="statBalance">Rp 0</div>
                </div>
                <div class="stat-box income">
                    <div class="stat-title">Total Pemasukan</div>
                    <div class="stat-val" id="statIncome">Rp 0</div>
                </div>
                <div class="stat-box expense">
                    <div class="stat-title">Total Pengeluaran</div>
                    <div class="stat-val" id="statExpense">Rp 0</div>
                </div>
            </div>

            <!-- CARD KUSTOMISASI TEKS & WARNA LAPORAN KEUANGAN -->
            <div class="card" style="border: 2px solid #D4AF37; background: #FFFCF2;">
                <div class="card-header" style="border-bottom: 1px solid #F3E5AB;">
                    <h2 style="color: #92400E; display:flex; align-items:center; gap:8px;">🎨 Desain Font & Warna Laporan Kas</h2>
                </div>
                <p style="font-size:13px; color:#78350F; margin-bottom:12px;">
                    Ubah teks judul serta warna font saldo, pemasukan, dan pengeluaran agar selalu kontras dan indah di atas kanvas background.
                </p>
                <button type="button" class="btn-gold" onclick="openFinanceCustomizationModal()">
                    ✏️ Edit Teks & Warna Laporan Keuangan
                </button>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 id="finFormTitle">Catat Transaksi Keuangan</h2>
                </div>
                <form id="formFinance" onsubmit="saveOrAddFinance(event)">
                    <div class="form-group">
                        <label>Jenis Transaksi</label>
                        <select id="finType" class="form-control">
                            <option value="INCOME">🟢 Pemasukan (Infaq / Sedekah)</option>
                            <option value="EXPENSE">🔴 Pengeluaran (Operasional / Honor)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Keterangan Transaksi</label>
                        <input type="text" id="finTitle" class="form-control" placeholder="Contoh: Kotak Amal Jum'at 15 Ags" required>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Nominal (Rp)</label>
                            <input type="number" id="finAmount" class="form-control" placeholder="Contoh: 1500000" required>
                        </div>
                        <div class="form-group">
                            <label>Kategori</label>
                            <input type="text" id="finCategory" class="form-control" placeholder="Kotak Amal, PLN, dll">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Tanggal / Waktu</label>
                        <input type="text" id="finDate" class="form-control" placeholder="Hari Ini">
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="submit" class="btn-primary" id="btnSaveFin" style="flex:1;">➕ Tambah Transaksi</button>
                        <button type="button" class="btn-danger" id="btnCancelFin" style="display:none; width:auto; padding:10px 14px;" onclick="cancelEditFinance()">✕ Batal</button>
                    </div>
                </form>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2>Riwayat Transaksi Terbaru</h2>
                </div>
                <ul class="item-list" id="financeList"></ul>
            </div>
        </div>

        <!-- 4. TAB PETUGAS JUMAT & HARI RAYA -->
        <div id="tab-jumat" class="section-tab">
            <!-- CARD KUSTOMISASI TEKS & WARNA PETUGAS -->
            <div class="card" style="border: 2px solid #D4AF37; background: #FFFCF2;">
                <div class="card-header" style="border-bottom: 1px solid #F3E5AB;">
                    <h2 style="color: #92400E; display:flex; align-items:center; gap:8px;">🎨 Desain Font & Warna Petugas Sholat</h2>
                </div>
                <p style="font-size:13px; color:#78350F; margin-bottom:12px;">
                    Atur teks judul plakat dan warna font nama petugas (Khatib, Imam, Muadzin, Bilal) serta label jabatannya.
                </p>
                <button type="button" class="btn-gold" onclick="openFridayCustomizationModal()">
                    ✏️ Edit Teks & Warna Petugas Sholat
                </button>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 id="friCardTitle">Jadwal Petugas Sholat Jum'at & Hari Raya</h2>
                </div>
                <div id="friLiveAlert" style="margin-bottom:12px; padding:10px 14px; background:rgba(16, 185, 129, 0.15); border:1px solid #10B981; border-radius:10px; font-size:13px; color:#A7F3D0;"></div>
                <div class="pill-switch" style="margin-bottom:14px; flex-wrap:wrap; gap:6px;">
                    <button type="button" class="pill-btn active" id="btnFri1" onclick="switchFridayWeek(1)">Jum'at 1</button>
                    <button type="button" class="pill-btn" id="btnFri2" onclick="switchFridayWeek(2)">Jum'at 2</button>
                    <button type="button" class="pill-btn" id="btnFri3" onclick="switchFridayWeek(3)">Jum'at 3</button>
                    <button type="button" class="pill-btn" id="btnFri4" onclick="switchFridayWeek(4)">Jum'at 4</button>
                    <button type="button" class="pill-btn" id="btnFri5" onclick="switchFridayWeek(5)">Jum'at 5</button>
                    <button type="button" class="pill-btn" id="btnFri6" onclick="switchFridayWeek(6)" style="border-color:#10B981;">🎉 Idul Fitri</button>
                    <button type="button" class="pill-btn" id="btnFri7" onclick="switchFridayWeek(7)" style="border-color:#F59E0B;">🐑 Idul Adha</button>
                </div>
                <form id="formFriday" onsubmit="saveFriday(event)">
                    <div class="grid-2">
                        <div class="form-group"><label id="lblFriDate">Keterangan Tanggal Masehi</label><input type="text" id="friDate" class="form-control" placeholder="Contoh: 28 Agustus 2026"></div>
                        <div class="form-group"><label id="lblFriHijriDate">Tanggal Hijriyah / Label</label><input type="text" id="friHijriDate" class="form-control" placeholder="Contoh: Jum'at Barakah"></div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group"><label id="lblFriKhotib">🎙️ Nama Khotib</label><input type="text" id="friKhotib" class="form-control" placeholder="Nama Khotib"></div>
                        <div class="form-group"><label>📱 No. WhatsApp Khotib</label><input type="text" id="friKhotibPhone" class="form-control" placeholder="Contoh: 08123456789"></div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group"><label id="lblFriImam">🕌 Nama Imam Sholat</label><input type="text" id="friImam" class="form-control" placeholder="Nama Imam"></div>
                        <div class="form-group"><label>📱 No. WhatsApp Imam</label><input type="text" id="friImamPhone" class="form-control" placeholder="Contoh: 08123456789"></div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group"><label id="lblFriMuadzin">📢 Nama Muadzin</label><input type="text" id="friMuadzin" class="form-control" placeholder="Nama Muadzin"></div>
                        <div class="form-group"><label>📱 No. WhatsApp Muadzin</label><input type="text" id="friMuadzinPhone" class="form-control" placeholder="Contoh: 08123456789"></div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group"><label id="lblFriBilal">📜 Nama Bilal / Muraqqi</label><input type="text" id="friBilal" class="form-control" placeholder="Nama Bilal"></div>
                        <div class="form-group"><label>📱 No. WhatsApp Bilal</label><input type="text" id="friBilalPhone" class="form-control" placeholder="Contoh: 08123456789"></div>
                    </div>
                    <div class="form-group"><label>📖 Catatan Khutbah / Jadwal</label><input type="text" id="friNotes" class="form-control" placeholder="Catatan tambahan (opsional)"></div>

                    <!-- HARI RAYA SETTINGS CONTAINER (ONLY VISIBLE ON WEEK 6 & 7) -->
                    <div id="friHariRayaConfigBox" style="display:none; margin:14px 0; background:rgba(16,185,129,0.08); border:1.5px solid #10B981; border-radius:10px; padding:14px;">
                        <h3 id="lblHariRayaBoxTitle" style="font-size:14px; font-weight:bold; color:#10B981; margin-bottom:10px; display:flex; align-items:center; gap:6px;">
                            <span>⚙️</span> Pengaturan Waktu Pelaksanaan Sholat Hari Raya
                        </h3>
                        <div class="form-group" style="margin-bottom:12px;">
                            <label style="display:flex; align-items:center; gap:10px; cursor:pointer; font-weight:bold; color:#FDE68A;">
                                <input type="checkbox" id="friHariRayaEnabled" style="width:20px; height:20px; accent-color:#10B981;">
                                <span id="lblHariRayaEnabled">Aktifkan Otomatisasi Waktu Sholat Hari Raya di Layar TV</span>
                            </label>
                            <small style="color:#94A3B8; font-size:11.5px; display:block; margin-top:4px;">
                                Ketika tanggal & jam pelaksanaan tiba, TV akan secara otomatis menampilkan hitung mundur gema takbir, layar takbiran, dan mode hening sholat/khutbah.
                            </small>
                        </div>
                        <div class="grid-2">
                            <div class="form-group">
                                <label>📅 Tanggal Pelaksanaan (YYYY-MM-DD)</label>
                                <input type="date" id="friHariRayaDate" class="form-control">
                            </div>
                            <div class="form-group">
                                <label>⏰ Jam Pelaksanaan Sholat (HH:mm)</label>
                                <input type="time" id="friHariRayaTime" class="form-control" value="06:30">
                            </div>
                        </div>
                        <div class="grid-2">
                            <div class="form-group">
                                <label>⏳ Hitung Mundur Pra-Sholat (Menit)</label>
                                <input type="number" id="friHariRayaIqomah" class="form-control" min="1" max="60" value="15">
                            </div>
                            <div class="form-group">
                                <label>🕌 Durasi Layar Sholat & Khutbah (Menit)</label>
                                <input type="number" id="friHariRayaSholat" class="form-control" min="3" max="120" value="20">
                            </div>
                        </div>
                        <div style="margin-top:10px; padding-top:10px; border-top:1px dashed rgba(16,185,129,0.3);">
                            <button type="button" class="btn-gold" id="btnSimulateHariRaya" onclick="simulateHariRayaTv()" style="width:100%; font-size:13px; font-weight:bold; padding:10px;">
                                🚀 Uji Coba / Simulasi Layar Sholat Hari Raya di TV Sekarang
                            </button>
                        </div>
                    </div>

                    <div class="form-group" id="friApplyAllContainer" style="margin:12px 0 16px 0; background:rgba(255,255,255,0.05); padding:10px; border-radius:8px; border:1px dashed rgba(255,255,255,0.2);">
                        <label style="display:flex; align-items:center; gap:10px; cursor:pointer; font-weight:bold; color:#FDE68A;">
                            <input type="checkbox" id="friApplyAll" style="width:20px; height:20px; accent-color:#10B981;">
                            <span>Salin & Terapkan nama & no. WA petugas ini ke <strong>Semua Minggu Jum'at (Jum'at 1 s/d 5)</strong></span>
                        </label>
                    </div>
                    <button type="submit" class="btn-primary" id="btnSaveFri" style="font-size:15px; padding:12px;">💾 Simpan & Perbarui Tampilan TV</button>
                </form>
            </div>
        </div>

        <!-- 4. TAB WHATSAPP GATEWAY (FONNTE) -->
        <div id="tab-whatsapp" class="section-tab">
            <!-- 1. Status & Koneksi Akun Fonnte -->
            <div class="card" style="border: 2px solid #25D366; background: #F0FDF4;">
                <div class="card-header" style="border-bottom: 1px solid #BBF7D0;">
                    <h2 style="color: #166534; display:flex; align-items:center; gap:8px;">
                        <span>💬</span> Status Koneksi WhatsApp Gateway (Fonnte API)
                    </h2>
                </div>
                <div style="background:#DCFCE7; border:1px solid #86EFAC; border-radius:8px; padding:12px; margin-bottom:14px; font-size:12.5px; color:#14532D;">
                    💡 <strong>Integrasi WhatsApp Otomatis:</strong> Aplikasi MasjidKU-TV dapat mengirimkan pesan konfirmasi / pengingat jadwal secara otomatis kepada <strong>Khotib, Imam, Muadzin, dan Bilal</strong> setiap hari <strong>Kamis (H-1) & Jum'at (Hari H) pukul 09:00 WIB</strong>, serta pengingat pemateri kajian. Menggunakan API gateway dari <strong>Fonnte.com</strong>.
                </div>

                <div id="waDeviceStatusBox" style="padding:14px; border-radius:8px; background:#fff; border:1px solid #CBD5E1; margin-bottom:14px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                        <span style="font-weight:700; color:#1E293B;">Status Akun & Perangkat WA:</span>
                        <span id="waStatusBadge" style="background:#E2E8F0; color:#475569; padding:4px 10px; border-radius:12px; font-size:11.5px; font-weight:700;">BELUM DIKONFIGURASI</span>
                    </div>
                    <div id="waStatusDetails" style="font-size:12px; color:#64748B;">
                        Masukkan API Token Fonnte di bawah ini untuk menghubungkan akun WhatsApp.
                    </div>
                </div>

                <div class="grid-2">
                    <button type="button" class="btn-primary" style="background:#16A34A; border-color:#15803D;" onclick="checkWaDeviceStatus()">
                        🔄 Cek Koneksi Akun Fonnte Sekarang
                    </button>
                    <button type="button" class="btn-gold" onclick="sendTestWaModal()">
                        📨 Kirim Pesan WhatsApp Uji Coba (Tes)
                    </button>
                </div>
            </div>

            <!-- 2. Pengaturan Token & Waktu Kirim -->
            <div class="card">
                <div class="card-header">
                    <h2>⚙️ Pengaturan Token & Jam Pengiriman Otomatis</h2>
                </div>
                <form id="formWaConfig" onsubmit="saveWaConfig(event)">
                    <div class="form-group" style="margin-bottom:16px;">
                        <label style="display:flex; align-items:center; gap:10px; cursor:pointer; font-weight:700; font-size:14px; color:#15803D;">
                            <input type="checkbox" id="cfgWaEnabled" style="width:20px; height:20px; accent-color:#16A34A;">
                            <span>Aktifkan Pengingat Otomatis via WhatsApp (Auto-Reminder)</span>
                        </label>
                    </div>

                    <div class="form-group">
                        <label>API Token Fonnte (Dapatkan dari dashboard.fonnte.com):</label>
                        <input type="text" id="cfgWaToken" class="form-control" placeholder="Contoh: a1b2c3d4e5f6..." autocomplete="off">
                        <small style="color:#64748B; font-size:11px;">Daftar gratis / login di <a href="https://fonnte.com" target="_blank" style="color:#16A34A; font-weight:700;">https://fonnte.com</a>, scan QR WhatsApp Anda, lalu salin Token API ke sini.</small>
                    </div>

                    <div class="grid-2">
                        <div class="form-group">
                            <label>Jam Pengiriman Hari Kamis (H-1 Sholat Jum'at):</label>
                            <select id="cfgWaSendThursdayHour" class="form-control">
                                <option value="7">07:00 WIB (Pagi)</option>
                                <option value="8">08:00 WIB (Pagi)</option>
                                <option value="9" selected>09:00 WIB (Standar Rekomendasi)</option>
                                <option value="10">10:00 WIB (Siang)</option>
                                <option value="16">16:00 WIB (Sore ba'da Ashar)</option>
                                <option value="19">19:30 WIB (Malam ba'da Isya)</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Jam Pengiriman Hari Jum'at (Hari H Sholat Jum'at):</label>
                            <select id="cfgWaSendFridayHour" class="form-control">
                                <option value="6">06:00 WIB (Pagi)</option>
                                <option value="7">07:00 WIB (Pagi)</option>
                                <option value="8">08:00 WIB (Pagi)</option>
                                <option value="9" selected>09:00 WIB (Standar Rekomendasi)</option>
                                <option value="10">10:00 WIB (Menjelang Jum'at)</option>
                            </select>
                        </div>
                    </div>

                    <!-- 3. Template Pesan Kustom -->
                    <div style="margin-top:20px; border-top:1px solid #E2E8F0; padding-top:16px;">
                        <h3 style="font-size:14px; font-weight:700; color:#1E293B; margin-bottom:10px;">
                            📝 Template Pesan WhatsApp (Dapat Dikustomisasi)
                        </h3>
                        <p style="font-size:12px; color:#64748B; margin-bottom:12px;">
                            Gunakan variabel: <code>{nama_masjid}</code>, <code>{nama_petugas}</code>, <code>{peran}</code>, <code>{tanggal}</code>, <code>{hijriah}</code>, <code>{waktu_sholat}</code>, <code>{nama_kajian}</code>, <code>{pemateri}</code>, <code>{waktu_kajian}</code>, <code>{tempat_kajian}</code>.
                        </p>

                        <div class="form-group">
                            <label>Template Pesan Hari Kamis (H-1 Sholat Jum'at):</label>
                            <textarea id="cfgWaTemplateThursday" class="form-control" rows="6" placeholder="Biarkan kosong untuk menggunakan template bawaan yang rapi"></textarea>
                        </div>

                        <div class="form-group">
                            <label>Template Pesan Hari Jum'at (Hari H Sholat Jum'at):</label>
                            <textarea id="cfgWaTemplateFriday" class="form-control" rows="6" placeholder="Biarkan kosong untuk menggunakan template bawaan yang rapi"></textarea>
                        </div>

                        <div class="form-group">
                            <label>Template Pesan Pengingat Pemateri Kajian:</label>
                            <textarea id="cfgWaTemplateKajian" class="form-control" rows="6" placeholder="Biarkan kosong untuk menggunakan template bawaan yang rapi"></textarea>
                        </div>
                    </div>

                    <button type="submit" class="btn-primary" style="width:100%; font-size:15px; padding:12px; background:#16A34A; border-color:#15803D;">
                        💾 Simpan Pengaturan WhatsApp Gateway
                    </button>
                </form>
            </div>

            <!-- 4. Broadcast Manual 1-Klik -->
            <div class="card">
                <div class="card-header">
                    <h2>🚀 Kirim Pengingat Manual Sekarang (Broadcast 1-Klik)</h2>
                </div>
                <p style="font-size:13px; color:#475569; margin-bottom:14px;">
                    Kirim pengingat WhatsApp secara langsung kepada seluruh petugas Sholat Jum'at / Hari Raya tanpa menunggu jam otomatis.
                </p>
                <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px; margin-bottom:10px;">
                    <button type="button" class="btn-primary" style="background:#0284C7; border-color:#0369A1;" onclick="sendManualFridayBroadcast('THURSDAY')">
                        📨 Kirim Format H-1 (Kamis) ke Petugas Jum'at
                    </button>
                    <button type="button" class="btn-primary" style="background:#16A34A; border-color:#15803D;" onclick="sendManualFridayBroadcast('FRIDAY')">
                        📢 Kirim Format Hari H (Jum'at) ke Petugas Jum'at
                    </button>
                </div>
                <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
                    <button type="button" class="btn-primary" style="background:#059669; border-color:#047857;" onclick="sendManualFridayBroadcast('FRIDAY', 6)">
                        🎉 Kirim Pengingat Petugas Idul Fitri (1 Syawal)
                    </button>
                    <button type="button" class="btn-primary" style="background:#D97706; border-color:#B45309;" onclick="sendManualFridayBroadcast('FRIDAY', 7)">
                        🐑 Kirim Pengingat Petugas Idul Adha (10 Dzulhijjah)
                    </button>
                </div>

                <div id="waBroadcastLogBox" style="display:none; margin-top:16px; padding:12px; background:#F8FAFC; border:1px solid #CBD5E1; border-radius:8px; font-size:12px; color:#334155;"></div>
            </div>
        </div>

        <!-- 5. TAB KEGIATAN (FULL CRUD) -->
        <div id="tab-kegiatan" class="section-tab">
            <div class="card">
                <div class="card-header">
                    <h2 id="actFormTitle">Tambah Agenda / Kajian Masjid</h2>
                </div>
                <form id="formActivity" onsubmit="saveOrAddActivity(event)">
                    <div class="form-group">
                        <label>Judul Acara / Kajian</label>
                        <input type="text" id="actTitle" class="form-control" placeholder="Contoh: Kajian Tafsir Jalalain" required>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Penceramah / Ustadz</label>
                            <input type="text" id="actSpeaker" class="form-control" placeholder="Nama Penceramah" required>
                        </div>
                        <div class="form-group">
                            <label>📱 No. WhatsApp Penceramah (Opsional)</label>
                            <input type="text" id="actSpeakerPhone" class="form-control" placeholder="Contoh: 08123456789">
                        </div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Hari / Tanggal</label>
                            <input type="text" id="actDate" class="form-control" placeholder="Ahad Pagi">
                        </div>
                        <div class="form-group">
                            <label>Waktu / Jam</label>
                            <input type="text" id="actTime" class="form-control" placeholder="05.30 WIB">
                        </div>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Lokasi / Tempat</label>
                            <input type="text" id="actLocation" class="form-control" placeholder="Ruang Utama">
                        </div>
                        <div class="form-group">
                            <label>Kategori</label>
                            <input type="text" id="actCategory" class="form-control" placeholder="Kajian Rutin">
                        </div>
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="submit" class="btn-primary" id="btnSaveAct" style="flex:1;">➕ Tambah Agenda</button>
                        <button type="button" class="btn-danger" id="btnCancelAct" style="display:none; width:auto; padding:10px 14px;" onclick="cancelEditActivity()">✕ Batal</button>
                    </div>
                </form>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2>Daftar Agenda Masjid</h2>
                </div>
                <ul class="item-list" id="activityList"></ul>
            </div>
        </div>

        <!-- 6. TAB RUNNING TEXT (FULL CRUD) -->
        <div id="tab-running" class="section-tab">
            <div class="card" style="background:#F0FDF4; border:1px solid #BBF7D0;">
                <div class="card-header">
                    <h2>🔤 Pengaturan Ukuran Teks Berjalan di Layar TV</h2>
                </div>
                <div class="grid-2" style="align-items:flex-end;">
                    <div class="form-group" style="margin-bottom:0;">
                        <label>Ukuran Font Running Text (sp)</label>
                        <select id="cfgRunningFontSize" class="form-control">
                            <option value="16">16 sp (Kecil)</option>
                            <option value="18">18 sp (Sedang)</option>
                            <option value="20" selected>20 sp (Besar - Standar Rekomendasi TV)</option>
                            <option value="22">22 sp (Ekstra Besar)</option>
                            <option value="24">24 sp (Sangat Besar & Menonjol)</option>
                            <option value="28">28 sp (Maksimal)</option>
                        </select>
                    </div>
                    <button type="button" class="btn-primary" style="margin-bottom:0; background:#059669; border-color:#047857;" onclick="saveRunningFontSize()">💾 Terapkan Ukuran Teks ke TV</button>
                </div>
                <small style="color:#047857; margin-top:6px; display:block;">💡 Mendukung teks panjang tanpa batas (unlimited characters). Teks akan otomatis berjalan rapi di bagian bawah layar TV.</small>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2 id="runningFormTitle">Tambah Running Text / Pengumuman</h2>
                </div>
                <form id="formRunning" onsubmit="saveOrAddRunningText(event)">
                    <div class="form-group">
                        <label id="runningInputLabel">Teks Berjalan Baru</label>
                        <textarea id="runText" class="form-control" rows="3" placeholder="Masukkan pengumuman atau hadits..." required></textarea>
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="submit" class="btn-primary" id="btnSaveRunning" style="flex:1;">➕ Tambah Teks Berjalan</button>
                        <button type="button" class="btn-danger" id="btnCancelRunning" style="display:none; width:auto; padding:10px 14px;" onclick="cancelEditRunningText()">✕ Batal</button>
                    </div>
                </form>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2>Daftar Teks Berjalan</h2>
                </div>
                <ul class="item-list" id="runningList"></ul>
            </div>
        </div>

        <!-- 7. TAB GAMBAR & BACKGROUND -->
        <div id="tab-media" class="section-tab">
            <!-- 0. Logo Masjid -->
            <div class="card">
                <div class="card-header">
                    <h2>🏛️ Logo Masjid di Header TV</h2>
                </div>
                <div style="background:rgba(16,185,129,0.1); border:1px solid #10b981; border-radius:8px; padding:10px; margin-bottom:12px; font-size:12px; color:#1A2E26;">
                    💡 Upload logo masjid Anda (PNG/JPG/WEBP) untuk mengganti ikon default di pojok kiri header layar TV. Ukuran ideal: 200×200px transparan (PNG).
                </div>

                <div id="logoStatusBox" style="padding:10px; border-radius:8px; margin-bottom:12px; background:#EEF2F0; font-size:13px;">
                    <strong>Status Logo:</strong> <span id="logoStatusText">📌 Logo Default (Vector)</span>
                </div>

                <div id="logoPreviewContainer" style="margin-bottom:12px; text-align:center;">
                    <img id="logoPreviewImg" alt="Logo Masjid" style="display:none; width:100px; height:100px; object-fit:contain; border-radius:50%; border:3px solid #F59E0B; background:#022C22; padding:4px;">
                </div>

                <div class="form-group">
                    <label>Pilih File Logo (PNG, JPG, WEBP — maks. 5MB)</label>
                    <input type="file" id="inputLogoFile" accept="image/png,image/jpeg,image/webp" class="form-control" onchange="previewLogoImage(this)">
                </div>

                <div id="logoLocalPreviewContainer" style="display:none; margin-bottom:12px; text-align:center;">
                    <img id="logoLocalPreviewImg" alt="Preview Logo" style="width:100px; height:100px; object-fit:contain; border-radius:50%; border:3px solid #F59E0B; background:#022C22; padding:4px;">
                    <p style="font-size:11px; color:#666; margin-top:4px;">Preview — akan tampil di header TV</p>
                </div>

                <div style="display:flex; flex-direction:column; gap:8px;">
                    <button type="button" class="btn-primary" onclick="uploadLogo()">📤 Upload Logo Masjid</button>
                    <button type="button" class="btn-danger" id="btnRemoveLogo" style="display:none;" onclick="deleteLogo()">🔄 Reset ke Logo Default</button>
                </div>
            </div>

            <!-- 1. Background Wallpaper & Tema Layar TV -->
            <div class="card">
                <div class="card-header">
                    <h2>🕌 Wallpaper & Tema Background Visual TV</h2>
                </div>
                <div style="background:rgba(16,185,129,0.1); border:1px solid #10b981; border-radius:8px; padding:10px; margin-bottom:14px; font-size:12px; color:#1A2E26;">
                    💡 <strong>Konsep Tampilan Premium:</strong> Ornamen megah (kubah emas, pilar, lentera) berada di sudut & tepi layar, sedangkan area tengah semi-transparan sehingga teks kas, petugas, dan waktu sholat tetap tajam dan terbaca jelas!
                </div>

                <!-- A. Background Layar Utama TV -->
                <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:14px; margin-bottom:16px;">
                    <h3 style="font-size:14px; font-weight:700; color:var(--primary-dark); margin-bottom:8px;">
                        🖥️ 1. Background Layar Utama TV (Home Screen)
                    </h3>
                    <div class="form-group">
                        <label>Pilihan Preset Resmi Layar Utama:</label>
                        <select id="cfgMainScreenBgPreset" class="form-control" onchange="changeMainBgPreset(this.value)">
                            <option value="PRESET_EMERALD_MIHRAB">🌿 Preset 1: Emerald Nabawi Mihrab (Kubah Emas & Pilar Hijau)</option>
                            <option value="PRESET_WHITE_PEARL_GOLD">🏛️ Preset 2: White Pearl & Gold Andalus (Putih Mutiara & Emas 3D)</option>
                            <option value="PRESET_MIDNIGHT_KISWAH">🖤 Preset 3: Midnight Kiswah Ka'bah (Hitam Beludru & Emas 24K)</option>
                            <option value="PRESET_OTTOMAN_SAPPHIRE">🌌 Preset 4: Ottoman Sapphire Blue (Biru Safir Turki & Kubah Megah)</option>
                            <option value="PRESET_COUNTDOWN_PLAQUE">⏱️ Preset 5: Plakat Emas & Siluet Masjid (Elegan)</option>
                            <option value="PRESET_MIDNIGHT_LANTERNS">✨ Preset 6: Lengkung Andalusia & Lentera Gantung</option>
                            <option value="PRESET_GRADIENT_SKY">🌅 Preset 7: Langit Dinamis 5 Waktu (Otomatis Subuh s/d Isya)</option>
                            <option value="CCTV_LIVE">📹 Live Stream CCTV Masjid (RTSP Siaran Langsung)</option>
                            <option value="CUSTOM_UPLOAD">📁 Foto / Gambar Kustom Sendiri</option>
                        </select>
                    </div>

                    <!-- Pilihan Kamera untuk Background CCTV -->
                    <div id="cctvBgSelectorContainer" style="display:none; margin-bottom:12px; background:#eff6ff; border:1px solid #bfdbfe; padding:10px; border-radius:8px;">
                        <label style="font-size:12px; font-weight:700; color:#1e40af; margin-bottom:4px; display:block;">Pilih Sudut Kamera CCTV untuk Background Layar TV:</label>
                        <select id="cfgBackgroundCctvCamSelect" class="form-control" style="font-weight:600;" onchange="changeBackgroundCctvCamera(this.value)">
                            <option value="">(Kamera Aktif Otomatis)</option>
                        </select>
                        <p style="font-size:11px; color:#64748b; margin:4px 0 0 0;">Jadwal sholat, jam digital, nama masjid, dan running text akan tetap tampil di atas video CCTV.</p>
                    </div>

                    <div id="bgStatusBox" style="padding:8px 12px; border-radius:6px; margin-bottom:10px; background:#EEF2F0; font-size:12.5px;">
                        <strong>Status Utama:</strong> <span id="bgStatusText">Preset Aktif</span>
                    </div>

                    <div class="form-group" style="margin-bottom:8px;">
                        <label>Atau Upload Foto Sendiri dari Galeri HP:</label>
                        <input type="file" id="inputBgFile" accept="image/*" class="form-control" onchange="previewBgImage(this)">
                    </div>
                    <div id="bgPreviewContainer" style="display:none; margin-bottom:10px;" class="img-preview-box">
                        <img id="bgPreviewImg" alt="Preview Background" style="max-height:140px; border-radius:6px;">
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="button" class="btn-primary" id="btnUploadBg" style="flex:1;" onclick="uploadBackground()">📤 Pasang Foto Layar Utama</button>
                        <button type="button" class="btn-danger" id="btnRemoveBg" style="display:none; width:auto;" onclick="removeBackground()">🔄 Reset</button>
                    </div>
                </div>

                <!-- B. Background Slide Laporan Keuangan -->
                <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:14px; margin-bottom:16px;">
                    <h3 style="font-size:14px; font-weight:700; color:var(--primary-dark); margin-bottom:8px;">
                        📊 2. Background Slide Laporan Kas Keuangan
                    </h3>
                    <div class="form-group">
                        <label>Pilihan Preset Slide Kas:</label>
                        <select id="cfgFinanceBgPreset" class="form-control" onchange="changeFinanceBgPreset(this.value)">
                            <option value="PRESET_WHITE_PEARL_GOLD">🏛️ Preset 2: White Pearl Gold (Sangat Direkomendasikan untuk Kas)</option>
                            <option value="PRESET_EMERALD_MIHRAB">🌿 Preset 1: Emerald Nabawi Mihrab</option>
                            <option value="PRESET_MIDNIGHT_KISWAH">🖤 Preset 3: Midnight Kiswah Ka'bah</option>
                            <option value="PRESET_OTTOMAN_SAPPHIRE">🌌 Preset 4: Ottoman Sapphire Blue</option>
                            <option value="SAME_AS_MAIN">🔄 Samakan dengan Layar Utama TV</option>
                            <option value="CUSTOM_UPLOAD">📁 Foto / Gambar Kustom Sendiri</option>
                        </select>
                    </div>
                    <div class="form-group" style="margin-bottom:8px;">
                        <label>Upload Background Khusus Slide Kas:</label>
                        <input type="file" id="inputFinanceBgFile" accept="image/*" class="form-control">
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="button" class="btn-primary" id="btnUploadFinanceBg" style="flex:1;" onclick="uploadSpecificBg('FINANCE_BG', 'inputFinanceBgFile', 'btnUploadFinanceBg')">📤 Pasang Background Kas</button>
                        <button type="button" class="btn-danger" id="btnRemoveFinanceBg" style="display:none; width:auto;" onclick="removeFinanceBg()">🔄 Reset</button>
                    </div>
                </div>

                <!-- C. Background Slide Petugas Jum'at -->
                <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:14px; margin-bottom:16px;">
                    <h3 style="font-size:14px; font-weight:700; color:var(--primary-dark); margin-bottom:8px;">
                        🕌 3. Background Slide Petugas Sholat Jum'at
                    </h3>
                    <div class="form-group">
                        <label>Pilihan Preset Slide Jum'at:</label>
                        <select id="cfgFridayOfficerBgPreset" class="form-control" onchange="changeFridayBgPreset(this.value)">
                            <option value="PRESET_EMERALD_MIHRAB">🌿 Preset 1: Emerald Nabawi Mihrab (Sangat Direkomendasikan untuk Jum'at)</option>
                            <option value="PRESET_WHITE_PEARL_GOLD">🏛️ Preset 2: White Pearl Gold</option>
                            <option value="PRESET_MIDNIGHT_KISWAH">🖤 Preset 3: Midnight Kiswah Ka'bah</option>
                            <option value="PRESET_OTTOMAN_SAPPHIRE">🌌 Preset 4: Ottoman Sapphire Blue</option>
                            <option value="SAME_AS_MAIN">🔄 Samakan dengan Layar Utama TV</option>
                            <option value="CUSTOM_UPLOAD">📁 Foto / Gambar Kustom Sendiri</option>
                        </select>
                    </div>
                    <div class="form-group" style="margin-bottom:8px;">
                        <label>Upload Background Khusus Slide Jum'at:</label>
                        <input type="file" id="inputFridayBgFile" accept="image/*" class="form-control">
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="button" class="btn-primary" id="btnUploadFridayBg" style="flex:1;" onclick="uploadSpecificBg('FRIDAY_BG', 'inputFridayBgFile', 'btnUploadFridayBg')">📤 Pasang Background Jum'at</button>
                        <button type="button" class="btn-danger" id="btnRemoveFridayBg" style="display:none; width:auto;" onclick="removeFridayBg()">🔄 Reset</button>
                    </div>
                </div>

                <!-- D. Background Countdown Pengingat Sholat -->
                <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:14px; margin-bottom:16px;">
                    <h3 style="font-size:14px; font-weight:700; color:var(--primary-dark); margin-bottom:8px;">
                        ⏱️ 4. Background Countdown Pengingat Sholat & Iqomah
                    </h3>
                    <div class="form-group">
                        <label>Pilihan Preset Layar Countdown:</label>
                        <select id="cfgCountdownBgPreset" class="form-control" onchange="changeCountdownBgPreset(this.value)">
                            <option value="PRESET_COUNTDOWN_PLAQUE">⏱️ Preset 5: Plakat Emas & Siluet Masjid (Sangat Direkomendasikan)</option>
                            <option value="PRESET_MIDNIGHT_LANTERNS">✨ Preset 6: Lengkung Andalusia & Lentera Gantung</option>
                            <option value="PRESET_EMERALD_MIHRAB">🌿 Preset 1: Emerald Nabawi Mihrab</option>
                            <option value="PRESET_MIDNIGHT_KISWAH">🖤 Preset 3: Midnight Kiswah Ka'bah</option>
                            <option value="SAME_AS_MAIN">🔄 Samakan dengan Layar Utama TV</option>
                            <option value="CUSTOM_UPLOAD">📁 Foto / Gambar Kustom Sendiri</option>
                        </select>
                    </div>
                    <div class="form-group" style="margin-bottom:8px;">
                        <label>Upload Background Khusus Countdown:</label>
                        <input type="file" id="inputCountdownBgFile" accept="image/*" class="form-control">
                    </div>
                    <div style="display:flex; gap:8px;">
                        <button type="button" class="btn-primary" id="btnUploadCountdownBg" style="flex:1;" onclick="uploadSpecificBg('COUNTDOWN_BG', 'inputCountdownBgFile', 'btnUploadCountdownBg')">📤 Pasang Background Countdown</button>
                        <button type="button" class="btn-danger" id="btnRemoveCountdownBg" style="display:none; width:auto;" onclick="removeCountdownBg()">🔄 Reset</button>
                    </div>
                </div>

                <!-- E. Tingkat Dimming & Transparansi Kartu -->
                <div class="grid-2">
                    <div class="form-group">
                        <label>Lapisan Gelap Dimming Background (%)</label>
                        <select id="bgDimSelect" class="form-control" onchange="changeBgDim(this.value)">
                            <option value="0.2">20% (Sangat Terang)</option>
                            <option value="0.35">35% (Terang)</option>
                            <option value="0.5" selected>50% (Standar Rekomendasi)</option>
                            <option value="0.7">70% (Lebih Kontras)</option>
                            <option value="0.85">85% (Maksimal Gelap)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Transparansi Kartu Tengah TV (%)</label>
                        <select id="cardTransparencySelect" class="form-control" onchange="changeCardTransparency(this.value)">
                            <option value="0.2">20% (Kartu Lebih Solid)</option>
                            <option value="0.35" selected>35% (Standar Kaca Berembun)</option>
                            <option value="0.5">50% (Sangat Tembus Pandang)</option>
                            <option value="0.7">70% (Hampir Transparan Penuh)</option>
                        </select>
                    </div>
                </div>
            </div>

            <!-- 2. Slideshow Poster / Flyer Dakwah -->
            <div class="card">
                <div class="card-header">
                    <h2>🖼️ Upload Poster Slideshow Layar TV</h2>
                </div>
                <div style="background:rgba(230,200,117,0.15); border:1px solid #E6C875; border-radius:8px; padding:10px; margin-bottom:12px; font-size:12px; color:#1A2E26;">
                    📢 Upload poster kajian, pengumuman Idul Adha/Fitri, penerimaan santri, atau flyer dakwah untuk tampil berputar otomatis di carousel layar TV!
                </div>

                <form id="formSlide" onsubmit="uploadSlidePoster(event)">
                    <div class="form-group">
                        <label>Pilih File Gambar Poster / Flyer</label>
                        <input type="file" id="inputSlideFile" accept="image/*" class="form-control" required onchange="previewSlideImage(this)">
                    </div>

                    <div id="slidePreviewContainer" style="display:none; margin-bottom:12px;" class="img-preview-box">
                        <img id="slidePreviewImg" alt="Preview Slide">
                    </div>

                    <div class="form-group">
                        <label>Judul / Keterangan Poster</label>
                        <input type="text" id="slideTitle" class="form-control" placeholder="Contoh: Kajian Tabligh Akbar Ustadz Somad" required>
                    </div>

                    <div class="form-group">
                        <label>Durasi Tampil di Layar (Detik)</label>
                        <input type="number" id="slideDuration" class="form-control" value="15" min="5" max="120">
                    </div>

                    <button type="submit" class="btn-primary" id="btnUploadSlide">➕ Upload ke Slideshow TV</button>
                </form>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2>Daftar Poster Slideshow di TV</h2>
                </div>
                <ul class="item-list" id="mediaSlideList"></ul>
            </div>
        </div>

        <!-- TAB VIDEO LOKAL -->
        <div id="tab-video" class="section-tab">
            <!-- 1. Status & Kontrol Tayangan Video di TV -->
            <div class="card">
                <div class="card-header">
                    <h2>🎬 Status & Kontrol Video di TV</h2>
                </div>
                <div style="background:rgba(16,185,129,0.1); border:1px solid #10b981; border-radius:8px; padding:10px; margin-bottom:14px; font-size:12px; color:#1A2E26;">
                    💡 <strong>Fitur Video Lokal:</strong> Anda dapat mengunggah video pengumuman, profil masjid, dokumentasi renovasi/santunan, atau animasi pemandangan masjid. Video dapat dipasang sebagai <strong>Background Video TV (Loop tanpa suara)</strong> atau diputar <strong>Layar Penuh (Fullscreen)</strong> dengan audio.
                </div>

                <div style="display:flex; flex-direction:column; gap:10px; margin-bottom:14px;">
                    <div style="padding:12px; border-radius:8px; background:#f1f5f9; border:1px solid #cbd5e1; font-size:13px;">
                        <div style="font-weight:700; color:#0f172a; margin-bottom:4px;">🖼️ Video Latar Belakang (Background TV):</div>
                        <div id="statusBgVideoText" style="color:#475569;">Sedang menggunakan Preset Gambar</div>
                    </div>

                    <div style="padding:12px; border-radius:8px; background:#eff6ff; border:1px solid #bfdbfe; font-size:13px;">
                        <div style="font-weight:700; color:#1e40af; margin-bottom:4px;">▶️ Tayangan Video Layar Penuh (Fullscreen):</div>
                        <div id="statusFullscreenVideoText" style="color:#3b82f6;">Standby (Tidak ada video yang sedang tayang)</div>
                    </div>
                </div>

                <button type="button" class="btn-danger" id="btnStopFullscreenVideo" style="display:none; width:100%; margin-bottom:10px;" onclick="stopFullscreenVideo()">⏹️ Hentikan Tayangan Video Fullscreen & Kembali ke Layar Utama</button>
            </div>

            <!-- 2. Form Upload Video Baru -->
            <div class="card">
                <div class="card-header">
                    <h2>📤 Upload File Video ke TV (MP4 / MKV / WebM)</h2>
                </div>
                <div class="form-group">
                    <label>Judul / Keterangan Video:</label>
                    <input type="text" id="inputVideoTitle" class="form-control" placeholder="Contoh: Video Dokumentasi Santunan Yatim">
                </div>
                <div class="form-group">
                    <label>Pilih File Video dari HP / Laptop (maks. 500 MB):</label>
                    <input type="file" id="inputVideoFile" accept="video/mp4,video/webm,video/x-matroska,video/*" class="form-control" onchange="onVideoFileSelected(this)">
                </div>
                <div id="videoSelectedInfo" style="display:none; margin-bottom:12px; font-size:12.5px; color:#059669; background:#ecfdf5; padding:8px 12px; border-radius:6px;"></div>

                <div id="videoUploadProgressContainer" style="display:none; margin-bottom:14px;">
                    <div style="display:flex; justify-content:space-between; font-size:12px; margin-bottom:4px; font-weight:600; color:#334155;">
                        <span id="videoUploadProgressLabel">Mengunggah file ke TV...</span>
                        <span id="videoUploadProgressPercent">0%</span>
                    </div>
                    <div style="width:100%; height:10px; background:#e2e8f0; border-radius:5px; overflow:hidden;">
                        <div id="videoUploadProgressBar" style="width:0%; height:100%; background:linear-gradient(90deg, #10b981, #059669); transition:width 0.2s;"></div>
                    </div>
                </div>

                <button type="button" class="btn-primary" id="btnUploadVideo" style="width:100%;" onclick="uploadVideoFile()">🚀 Upload Video ke Penyimpanan TV</button>
            </div>

            <!-- 3. Daftar Video di TV -->
            <div class="card">
                <div class="card-header">
                    <h2>📁 Daftar Video Tersimpan di TV</h2>
                </div>
                <div id="videoListContainer">
                    <p style="text-align:center; color:#64748b; font-size:13px; padding:20px;">Memuat daftar video...</p>
                </div>
            </div>
        </div>

        <!-- 8. TAB KONTROL & TAMPILAN -->
        <div id="tab-kontrol" class="section-tab">
            <div class="card">
                <div class="card-header">
                    <h2>Model Tata Letak Layar TV (Layout Template)</h2>
                </div>
                <div class="form-group">
                    <label>Pilih Model Desain TV Masjid</label>
                    <select id="cfgLayoutModel" class="form-control" onchange="saveLayoutModel(this.value)">
                        <option value="MIHRAB_GRAND_ROYAL">🕌 Model 7: Grand Mihrab Nabawi (Portal Jam Kubah + Kartu Berkubah + Card Tengah Emas)</option>
                        <option value="CORDOBA_ANDALUSIA">🏛️ Model 8: Cordoba & Alhambra Moorish (Lengkung Tapal Kuda + QRIS Infaq)</option>
                        <option value="CAROUSEL_BOTTOM">📺 Model 1: Klasik TV Bar Bawah (Default Carousel)</option>
                        <option value="SPLIT_DASHBOARD">📊 Model 2: Modern Split Dashboard (Jam Samping + Kas & Petugas)</option>
                        <option value="DUAL_INFO_COMPACT">🗂️ Model 3: Dual Card Berdampingan (Kas & Petugas Selalu Tampil)</option>
                        <option value="SIDEBAR_ANALOG_NEO">🕌 Model 4: Al-Amin Cyber (Jadwal Samping Kiri + Jam Analog + Foto Utama)</option>
                        <option value="RIGHT_WAVE_ANALOG">🌊 Model 5: Al-Ikhsan Wave (Panel Wave Kanan + Jam Analog + Foto Utama)</option>
                        <option value="WAKTIHA_CLASSIC">⏱️ Model 6: Waktiha Klaten (Jam Digital Besar Kiri + Petugas + Foto + 7 Bar)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Tema Warna Nuansa Islami TV</label>
                    <select id="cfgTheme" class="form-control" onchange="saveTheme(this.value)">
                        <option value="DYNAMIC_SKY">🌅 Langit Dinamis 5 Waktu (Otomatis)</option>
                        <option value="NABAWI_EMERALD">🌿 Mihrab Nabawi (Hijau Zamrud & Emas Madinah)</option>
                        <option value="KISWAH_GOLD">🖤 Kiswah Ka'bah (Hitam Beludru & Emas 24K)</option>
                        <option value="OTTOMAN_BLUE">🌌 Blue Ottoman (Biru Safir & Pirus Istanbul)</option>
                        <option value="EMERALD_GOLD">🍃 Emerald Green & Gold (Klasik Islami)</option>
                        <option value="ROYAL_NAVY">🌊 Royal Navy & Star (Malam Elegan)</option>
                        <option value="SUNSET_AMBER">🌅 Sunset Gold & Amber (Senja Emas)</option>
                        <option value="MIDNIGHT_CHARCOAL">🌑 Midnight Luxury Dark</option>
                        <option value="MIHRAB_CLASSIC">🕌 Mihrab Klasik Kubah Hijau Emas</option>
                    </select>
                </div>

                <div style="background:#FFFBEB; padding:12px; border-radius:10px; border:1px solid #FDE68A; margin-top:14px;">
                    <h3 style="font-size:13px; margin-bottom:8px; color:#B45309;">📱 Infaq Digital QRIS & Program Masjid</h3>
                    <div class="form-group">
                        <label>Judul Program Donasi</label>
                        <input type="text" id="cfgDonationTitle" class="form-control" placeholder="Contoh: Renovasi Fasilitas Masjid">
                    </div>
                    <div class="grid-2" style="gap:10px;">
                        <div class="form-group">
                            <label>Target Dana (Rp)</label>
                            <input type="number" id="cfgDonationTarget" class="form-control" placeholder="25000000">
                        </div>
                        <div class="form-group">
                            <label>Dana Terkumpul (Rp)</label>
                            <input type="number" id="cfgDonationCollected" class="form-control" placeholder="16850000">
                        </div>
                    </div>
                    <button class="btn btn-primary btn-sm" style="width:auto; margin-top:6px; background:#D97706; border-color:#B45309;" onclick="saveDonationProgram()">Simpan Program Donasi</button>

                    <div style="margin-top:14px; padding-top:12px; border-top:1px dashed #FDE68A;">
                        <h4 style="font-size:12px; color:#B45309; margin-bottom:6px;">📲 Upload Gambar Kode QRIS Masjid</h4>
                        <p style="font-size:11px; color:#78350F; margin-bottom:8px;">Pilih foto QRIS masjid dari galeri HP untuk ditampilkan di layar TV dan slide infaq.</p>
                        <div id="qrisPreviewBox" style="margin-bottom:8px; display:none;">
                            <img id="qrisPreviewImg" src="" style="max-height:140px; border-radius:8px; border:1.5px solid #F59E0B; background:#fff; padding:4px; display:block;">
                            <div style="margin-top:4px;">
                                <button type="button" class="btn btn-danger btn-sm" onclick="removeQrisImage()" style="background:#EF4444; color:#fff; border:none; padding:4px 8px; border-radius:4px; font-size:11px; cursor:pointer;">🗑️ Hapus Gambar QRIS</button>
                            </div>
                        </div>
                        <div class="form-group">
                            <input type="file" id="qrisFileInput" accept="image/*" class="form-control" style="padding:6px;">
                        </div>
                        <button type="button" class="btn btn-primary btn-sm" id="btnUploadQris" style="width:auto; background:#10B981; border-color:#059669;" onclick="uploadQrisImage()">📲 Upload QRIS ke TV</button>
                    </div>
                </div>
                <div style="background:#F0F8F5; padding:12px; border-radius:10px; border:1px solid #D1E7DD; margin-top:14px;">
                    <h3 style="font-size:13px; margin-bottom:8px; color:var(--primary);">Pilih Konten yang Ditampilkan pada Layar TV</h3>
                    <div class="grid-2" style="font-size:13px; gap:8px;">
                        <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showFinReport" onchange="syncAndSaveContentVisibility('showFinReport', 'showFinReportTab1')"> 💳 Laporan Kas Keuangan</label>
                        <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showFriOfficers" onchange="syncAndSaveContentVisibility('showFriOfficers', 'showFriOfficersTab1')"> 👥 Petugas Sholat Jum'at</label>
                        <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showQrisCard" onchange="syncAndSaveContentVisibility('showQrisCard', 'showQrisCardTab1')"> 📱 Infaq QRIS & Donasi</label>
                        <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showDailyHadith" onchange="syncAndSaveContentVisibility('showDailyHadith', 'showDailyHadithTab1')"> 📖 Mutiara Hadits Shahih</label>
                        <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showActivities" onchange="syncAndSaveContentVisibility('showActivities', 'showActivitiesTab1')"> 📅 Agenda & Kajian</label>
                        <label style="display:flex; align-items:center; gap:6px; cursor:pointer;"><input type="checkbox" id="showDailyMaklumat" onchange="syncAndSaveContentVisibility('showDailyMaklumat', 'showDailyMaklumatTab1')"> 📢 Maklumat Harian</label>
                    </div>
                    <p style="font-size:11px; color:#065F46; margin-top:8px; margin-bottom:0;">💡 <i>Khusus Petugas Jum'at: Jika tidak dicentang, otomatis hanya aktif tampil pada hari Jum'at saja.</i></p>
                </div>
            </div>

            <!-- CARD KUSTOMISASI FONT, WARNA & TEKS KONTEN TV -->
            <div class="card" style="border: 2px solid #0D5C3A; background: #F4FBF7;">
                <div class="card-header" style="border-bottom: 1px solid #D1E7DD;">
                    <h2 style="color: #073B24; display:flex; align-items:center; gap:8px;">🎨 Kustomisasi Font, Warna & Teks Konten TV</h2>
                </div>
                <p style="font-size:13px; color:#1A2E26; margin-bottom:14px;">
                    Sesuaikan teks judul, tema, dan palet warna font untuk masing-masing konten TV agar selalu serasi dan kontras dengan kanvas background yang Anda pilih:
                </p>
                <div class="grid-2">
                    <button type="button" class="btn-primary" style="padding:12px;" onclick="openFinanceCustomizationModal()">
                        💳 Edit Teks & Warna Laporan Kas
                    </button>
                    <button type="button" class="btn-primary" style="padding:12px;" onclick="openFridayCustomizationModal()">
                        👥 Edit Teks & Warna Petugas Jum'at
                    </button>
                    <button type="button" class="btn-gold" style="padding:12px;" onclick="openHadithCustomizationModal()">
                        📖 Edit Mutiara Hadits & Warna Font
                    </button>
                    <button type="button" class="btn-gold" style="padding:12px;" onclick="openMaklumatCustomizationModal()">
                        📢 Edit Maklumat Masjid & Warna Font
                    </button>
                    <button type="button" class="btn-primary" style="padding:14px; background: linear-gradient(135deg, #0284c7, #0369a1); font-weight:800; font-size:13px; grid-column: span 2; box-shadow: 0 4px 12px rgba(2,132,199,0.3); border: 1.5px solid #38bdf8;" onclick="openClockCustomizationModal()">
                        ⏰ Kustomisasi Font & Warna Jam Digital TV
                    </button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h2>Uji Coba & Simulasi Layar TV</h2>
                </div>
                <div class="grid-3">
                    <button class="btn-gold" onclick="triggerAction('preadhan')">⏳ Pra-Adzan Harian (30s)</button>
                    <button class="btn-gold" onclick="triggerAction('adhan')">📢 Adzan Sholat Harian</button>
                    <button class="btn-gold" onclick="triggerAction('iqomah')">⏱️ Iqomah Harian</button>
                </div>
                <div class="grid-2" style="margin-top:10px;">
                    <button class="btn-gold" onclick="triggerAction('preadhan_friday')" style="background:#047857; color:#fff; border-color:#065F46; font-weight:bold;">🕌 Simulasi Pra-Adzan Jum'at (30s)</button>
                    <button class="btn-gold" onclick="triggerAction('adhan_friday')" style="background:#047857; color:#fff; border-color:#065F46; font-weight:bold;">🕌 Simulasi Adzan Jum'at</button>
                </div>
                <div class="grid-2" style="margin-top:10px;">
                    <button class="btn-gold" onclick="triggerAction('iqomah_friday')" style="background:#065F46; color:#fff; border-color:#047857; font-weight:bold;">⏱️ Simulasi Iqomah Jum'at</button>
                    <button class="btn-gold" onclick="triggerAction('sholat_friday')" style="background:#065F46; color:#fff; border-color:#047857; font-weight:bold;">📴 Simulasi Hening Sholat Jum'at</button>
                </div>
                <div class="grid-2" style="margin-top:10px;">
                    <button class="btn-gold" onclick="triggerAction('sholat')">📴 Simulasi Hening Harian</button>
                    <button class="btn-primary" onclick="triggerAction('reset')">🔄 Kembalikan ke Tampilan Normal</button>
                </div>
                <div style="margin-top:10px;">
                    <button class="btn-secondary" style="width:100%; padding:10px;" onclick="triggerAction('sound')">🔔 Test Suara Bel Chime Masuk Sholat</button>
                </div>
            </div>
        </div>

        <!-- 3. TAB MUROTTAL & TARHIM -->
        <div id="tab-murottal" class="section-tab">
            <!-- Player Status Card -->
            <div class="card" style="background: linear-gradient(135deg, #022c22 0%, #064e3b 100%); color: #fff; border: 1.5px solid #059669;">
                <div class="card-header" style="border-bottom: 1px solid rgba(255,255,255,0.15);">
                    <h2 style="color: #6ee7b7; display: flex; align-items: center; gap: 8px;">
                        <span>🎧</span> Live Player Murottal TV
                    </h2>
                    <span id="murottalLiveBadge" style="background: rgba(16,185,129,0.2); color: #6ee7b7; border: 1px solid #10b981; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700;">
                        STANDBY
                    </span>
                </div>
                <div style="margin-bottom: 16px;">
                    <div style="font-size: 12px; color: #a7f3d0; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">Sedang Diputar di Layar TV:</div>
                    <div id="murottalNowPlayingTitle" style="font-size: 16px; font-weight: 800; color: #ffffff; margin-top: 4px;">Tidak ada audio yang diputar</div>
                    <div id="murottalNowPlayingQari" style="font-size: 13px; color: #d1fae5; margin-top: 2px;">--</div>
                </div>

                <div class="form-group" style="margin-bottom: 14px;">
                    <label style="color: #d1fae5; font-size: 12px; font-weight: 600; display: flex; justify-content: space-between;">
                        <span>🔊 Volume Pemutar TV</span>
                        <span id="liveMurottalVolLabel" style="font-weight: 700; color: #6ee7b7;">80%</span>
                    </label>
                    <input type="range" id="liveMurottalVolSlider" min="10" max="100" value="80" class="form-control" style="accent-color: #10b981; padding: 0; height: 8px;" oninput="onLiveMurottalVolChange(this.value)">
                </div>

                <div class="grid-2" style="gap: 10px;">
                    <button type="button" class="btn-gold" style="background: linear-gradient(135deg, #10B981, #059669); color: #fff; font-weight: 700;" onclick="playMurottalNow()">
                        ▶️ Putar Sekarang ke TV
                    </button>
                    <button type="button" class="btn-danger" style="background: #ef4444; color: #fff; border: none; font-weight: 700;" onclick="stopMurottalNow()">
                        ⏹️ Hentikan Pemutaran
                    </button>
                </div>
            </div>

            <!-- Murottal Template Catalogue Card (50+ Qari & Surah) -->
            <div class="card" style="border: 1.5px solid #a7f3d0;">
                <div class="card-header" style="background: #ecfdf5; margin: -20px -20px 16px -20px; padding: 16px 20px; border-bottom: 1.5px solid #a7f3d0; border-radius: 12px 12px 0 0;">
                    <h2 style="color: #065f46; display: flex; align-items: center; gap: 8px;">
                        <span>🎙️</span> Koleksi Template Murottal & Qari Populer (Siap Pakai Tanpa Upload)
                    </h2>
                </div>
                <p style="font-size: 12.5px; color: #4b5563; margin-bottom: 14px;">
                    Pilih lantunan qari ternama dunia dan surah-surah Al-Qur'an di bawah ini. Anda dapat mendengarkan preview di HP dan langsung mengaktifkannya sebagai audio utama TV dengan 1 kali klik.
                </p>

                <!-- Filter & Search Bar -->
                <div style="background: #fafcfb; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; margin-bottom: 14px;">
                    <div class="grid-2" style="gap: 10px; margin-bottom: 10px;">
                        <input type="text" id="murottalSearchInput" class="form-control" placeholder="🔍 Cari Surah atau Qari..." oninput="filterMurottalPresets()">
                        <select id="murottalQariFilter" class="form-control" onchange="filterMurottalPresets()">
                            <option value="">Semua Qari & Pelantun</option>
                        </select>
                    </div>
                    <!-- Category Chips -->
                    <div style="display: flex; flex-wrap: wrap; gap: 6px;" id="murottalCategoryChips">
                        <button type="button" class="btn-secondary chip-btn active" style="padding: 4px 10px; font-size: 11px; border-radius: 20px; background: #065f46; color: #fff;" onclick="selectMurottalCatFilter('Semua', this)">Semua</button>
                        <button type="button" class="btn-secondary chip-btn" style="padding: 4px 10px; font-size: 11px; border-radius: 20px;" onclick="selectMurottalCatFilter('Surah Pilihan', this)">Surah Pilihan</button>
                        <button type="button" class="btn-secondary chip-btn" style="padding: 4px 10px; font-size: 11px; border-radius: 20px;" onclick="selectMurottalCatFilter('Khusus Subuh & Jum\'at', this)">Subuh & Jum'at</button>
                        <button type="button" class="btn-secondary chip-btn" style="padding: 4px 10px; font-size: 11px; border-radius: 20px;" onclick="selectMurottalCatFilter('Juz \'Amma', this)">Juz 'Amma</button>
                        <button type="button" class="btn-secondary chip-btn" style="padding: 4px 10px; font-size: 11px; border-radius: 20px;" onclick="selectMurottalCatFilter('Shalawat & Doa', this)">Shalawat & Tarhim</button>
                    </div>
                </div>

                <!-- Hidden audio preview player for HP -->
                <div id="hpAudioPreviewBox" style="display: none; background: #eff6ff; border: 1.5px solid #bfdbfe; border-radius: 8px; padding: 10px; margin-bottom: 12px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <span style="font-size: 12px; font-weight: 700; color: #1e40af;" id="hpPreviewTitle">🎵 Preview Audio di HP</span>
                        <button type="button" style="background: none; border: none; color: #6b7280; cursor: pointer; font-size: 14px;" onclick="closeHpPreview()">✖ Tutup</button>
                    </div>
                    <audio id="hpPreviewAudioPlayer" controls style="width: 100%; height: 36px;"></audio>
                </div>

                <!-- Presets Grid Container -->
                <div id="murottalPresetsGrid" style="max-height: 480px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; padding-right: 4px;">
                    <!-- Dynamically rendered via JS -->
                </div>
            </div>

            <!-- Murottal Configuration Card (Jadwal & Sholat) -->
            <div class="card">
                <div class="card-header">
                    <h2>⚙️ Pengaturan Jadwal Otomatis Murottal Sebelum Adzan</h2>
                </div>
                <form id="formMurottalConfig" onsubmit="saveMurottalConfig(event)">
                    <div style="background: #ecfdf5; border: 1.5px solid #a7f3d0; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                        <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; font-weight: 700; color: #065f46;">
                            <input type="checkbox" id="murottalEnabled" style="width: 20px; height: 20px; accent-color: #059669;">
                            <span>Aktifkan Pemutaran Murottal Otomatis Sebelum Waktu Sholat</span>
                        </label>
                        <p style="font-size: 11.5px; color: #047857; margin: 6px 0 0 30px;">
                            Audio murottal / tarhim akan mulai diputar secara otomatis menjelang masuk waktu sholat dan berhenti otomatis ketika adzan berkumandang.
                        </p>
                    </div>

                    <div class="form-group">
                        <label style="font-weight: 700; color: var(--primary-dark);">Pilih Waktu Sholat yang Memutar Murottal:</label>
                        <div class="grid-3" style="gap: 10px; margin-top: 8px;">
                            <label style="display: flex; align-items: center; gap: 8px; background: #fafcfb; padding: 10px; border-radius: 8px; border: 1px solid #cfdcd5; cursor: pointer;">
                                <input type="checkbox" id="murottalSubuh" style="accent-color: #059669;"> 🌅 <strong>Subuh</strong>
                            </label>
                            <label style="display: flex; align-items: center; gap: 8px; background: #fafcfb; padding: 10px; border-radius: 8px; border: 1px solid #cfdcd5; cursor: pointer;">
                                <input type="checkbox" id="murottalDzuhur" style="accent-color: #059669;"> ☀️ <strong>Dzuhur</strong>
                            </label>
                            <label style="display: flex; align-items: center; gap: 8px; background: #fafcfb; padding: 10px; border-radius: 8px; border: 1px solid #cfdcd5; cursor: pointer;">
                                <input type="checkbox" id="murottalAshar" style="accent-color: #059669;"> ⛅ <strong>Ashar</strong>
                            </label>
                            <label style="display: flex; align-items: center; gap: 8px; background: #fafcfb; padding: 10px; border-radius: 8px; border: 1px solid #cfdcd5; cursor: pointer;">
                                <input type="checkbox" id="murottalMaghrib" style="accent-color: #059669;"> 🌇 <strong>Maghrib</strong>
                            </label>
                            <label style="display: flex; align-items: center; gap: 8px; background: #fafcfb; padding: 10px; border-radius: 8px; border: 1px solid #cfdcd5; cursor: pointer;">
                                <input type="checkbox" id="murottalIsya" style="accent-color: #059669;"> 🌌 <strong>Isya'</strong>
                            </label>
                            <label style="display: flex; align-items: center; gap: 8px; background: #ecfdf5; padding: 10px; border-radius: 8px; border: 1px solid #a7f3d0; cursor: pointer;">
                                <input type="checkbox" id="murottalJumat" style="accent-color: #059669;"> 🕌 <strong>Sholat Jum'at</strong>
                            </label>
                        </div>
                    </div>

                    <div class="grid-2" style="margin-top: 14px;">
                        <div class="form-group">
                            <label>⏱️ Durasi Pemutaran Sebelum Adzan</label>
                            <select id="murottalDurationMinutes" class="form-control">
                                <option value="5">5 Menit Sebelum Adzan</option>
                                <option value="10" selected>10 Menit Sebelum Adzan (Standar)</option>
                                <option value="15">15 Menit Sebelum Adzan</option>
                                <option value="20">20 Menit Sebelum Adzan</option>
                                <option value="25">25 Menit Sebelum Adzan</option>
                                <option value="30">30 Menit Sebelum Adzan</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>🔊 Volume Default Suara (%)</label>
                            <select id="murottalVolume" class="form-control">
                                <option value="50">50% (Sedang)</option>
                                <option value="60">60%</option>
                                <option value="70">70%</option>
                                <option value="80" selected>80% (Direkomendasikan)</option>
                                <option value="90">90% (Keras)</option>
                                <option value="100">100% (Maksimal)</option>
                            </select>
                        </div>
                    </div>

                    <div class="form-group" style="margin-top: 10px;">
                        <label style="font-weight: 700; color: var(--primary-dark);">Pilihan Sumber Audio:</label>
                        <div class="pill-switch">
                            <button type="button" class="pill-btn active" id="btnSourcePreset" onclick="setMurottalSource('PRESET')">🎙️ Preset Qari Populer</button>
                            <button type="button" class="pill-btn" id="btnSourceUpload" onclick="setMurottalSource('MANUAL_UPLOAD')">📂 File Upload Manual</button>
                        </div>
                    </div>

                    <!-- Preset Selector Section -->
                    <div id="sectionPresetPicker" style="background: #fafcfb; padding: 14px; border-radius: 10px; border: 1px solid #cfdcd5; margin-bottom: 14px;">
                        <div class="form-group">
                            <label style="font-weight: 700;">Preset Audio Aktif Saat Ini di TV:</label>
                            <select id="murottalSelectedPresetId" class="form-control" onchange="onPresetChange(this.value)">
                                <!-- Dynamically populated -->
                            </select>
                        </div>
                        <div id="presetDescBox" style="font-size: 12px; color: #4b5563; background: #fff; padding: 10px; border-radius: 6px; border: 1px solid #e5e7eb; margin-bottom: 10px;">
                            Memuat deskripsi qari...
                        </div>
                    </div>

                    <button type="submit" class="btn-primary" style="margin-top: 10px;">
                        💾 Simpan Pengaturan Murottal
                    </button>
                </form>
            </div>

            <!-- Upload Audio Card (Opsional) -->
            <div class="card">
                <div class="card-header">
                    <h2>📤 Upload File Audio Manual Sendiri (Opsional)</h2>
                </div>
                <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 14px;">
                    Jika Anda memiliki rekaman qari masjid sendiri atau shalawat kustom berformat MP3 / WAV / M4A, Anda dapat mengunggahnya ke memori TV.
                </p>
                <div class="grid-2">
                    <div class="form-group">
                        <label>Nama Surah / Judul Audio:</label>
                        <input type="text" id="uploadMurottalSurah" class="form-control" placeholder="Contoh: Surah Ar-Rahman">
                    </div>
                    <div class="form-group">
                        <label>Nama Qari / Pelantun / Masjid:</label>
                        <input type="text" id="uploadMurottalQari" class="form-control" placeholder="Contoh: Syaikh Mishary / Imam Masjid">
                    </div>
                </div>
                <div class="grid-2">
                    <div class="form-group">
                        <label>Alokasi Waktu Sholat:</label>
                        <select id="uploadMurottalPrayer" class="form-control">
                            <option value="ALL">Semua Waktu Sholat</option>
                            <option value="SUBUH">Khusus Waktu Subuh (Tarhim / Murottal)</option>
                            <option value="DZUHUR">Khusus Waktu Dzuhur</option>
                            <option value="ASHAR">Khusus Waktu Ashar</option>
                            <option value="MAGHRIB">Khusus Waktu Maghrib</option>
                            <option value="ISYA">Khusus Waktu Isya'</option>
                            <option value="JUMAT">Khusus Sholat Jum'at</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Pilih File Audio dari HP / Laptop (maks. 120 MB):</label>
                        <input type="file" id="uploadMurottalFile" accept="audio/*,.mp3,.wav,.m4a,.ogg,.flac,.aac,.opus" class="form-control" onchange="onAudioFileSelected(this)">
                    </div>
                </div>
                <div id="uploadFileInfo" style="display: none; font-size: 12.5px; color: #059669; background: #ecfdf5; padding: 8px 12px; border-radius: 6px; margin-bottom: 12px;"></div>

                <div id="murottalUploadProgressContainer" style="display:none; margin-bottom:14px;">
                    <div style="display:flex; justify-content:space-between; font-size:12px; margin-bottom:4px; font-weight:600; color:#334155;">
                        <span id="murottalUploadProgressLabel">Mengunggah file ke TV...</span>
                        <span id="murottalUploadProgressPercent">0%</span>
                    </div>
                    <div style="width:100%; height:10px; background:#e2e8f0; border-radius:5px; overflow:hidden;">
                        <div id="murottalUploadProgressBar" style="width:0%; height:100%; background:linear-gradient(90deg, #10b981, #059669); transition:width 0.2s;"></div>
                    </div>
                </div>

                <button type="button" class="btn-primary" id="btnSubmitUploadMurottal" style="width:100%;" onclick="uploadMurottalFile()">🚀 Upload File Audio ke TV</button>

                <div style="margin-top: 20px;">
                    <h3 style="font-size: 13px; font-weight: 700; color: var(--primary-dark); margin-bottom: 10px;">
                        📚 File Audio Kustom yang Tersimpan di TV
                    </h3>
                    <ul class="item-list" id="murottalAudioList">
                        <li style="text-align: center; color: #9ca3af; padding: 14px; font-size: 12px;">Belum ada file audio manual yang di-upload.</li>
                    </ul>
                </div>
            </div>
        </div>

        <!-- 4. TAB SIARAN LIVE & CCTV -->
        <div id="tab-youtube" class="section-tab">
            <!-- ==================== BAGIAN 1: CCTV & IP CAMERA (RTSP LOKAL) ==================== -->
            <div style="margin-bottom: 28px;">
                <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; padding-bottom: 8px; border-bottom: 2px solid #0284c7;">
                    <h2 style="font-size: 17px; font-weight: 800; color: #0369a1; margin: 0; display: flex; align-items: center; gap: 8px;">
                        <span>📹</span> Siaran Multi-Kamera CCTV Masjid (RTSP Jaringan Lokal - Bebas Kuota)
                    </h2>
                    <span style="font-size: 11px; font-weight: 700; background: #e0f2fe; color: #0369a1; padding: 4px 10px; border-radius: 6px; border: 1px solid #bae6fd;">
                        ⚡ Ultra Low-Latency (&lt;1 dtk)
                    </span>
                </div>

                <!-- Card 1: Status & Quick Switch 1-Klik -->
                <div class="card" style="background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: #fff; border: 1.5px solid #0284c7; margin-bottom: 14px;">
                    <div class="card-header" style="border-bottom: 1px solid rgba(255,255,255,0.15);">
                        <h3 style="color: #38bdf8; display: flex; align-items: center; gap: 8px; font-size: 15px; margin: 0;">
                            <span>📹</span> Status Siaran CCTV di Layar TV
                        </h3>
                        <span id="cctvStatusBadge" style="background: rgba(14,165,233,0.2); color: #7dd3fc; border: 1px solid #0284c7; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700;">
                            STANDBY NONAKTIF
                        </span>
                    </div>
                    <div style="margin-bottom: 14px;">
                        <div style="font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">Kamera Aktif Saat Ini:</div>
                        <div id="cctvNowTitle" style="font-size: 16px; font-weight: 800; color: #ffffff; margin-top: 4px;">Belum ada CCTV yang ditayangkan</div>
                        <div id="cctvNowUrl" style="font-size: 12px; color: #64748b; margin-top: 2px; word-break: break-all; font-family: monospace;">--</div>
                    </div>

                    <!-- Panel Quick Switch Kamera 1-Klik -->
                    <div style="margin-bottom: 16px; background: rgba(255,255,255,0.06); padding: 12px; border-radius: 10px; border: 1px dashed rgba(56,189,248,0.3);">
                        <div style="font-size: 12px; font-weight: 700; color: #38bdf8; margin-bottom: 8px; display: flex; align-items: center; gap: 6px;">
                            <span>🎛️</span> Quick Switch Sudut Kamera (1-Klik ke Layar TV):
                        </div>
                        <div id="cctvQuickSwitchContainer" style="display: flex; flex-wrap: wrap; gap: 8px;">
                            <!-- Populated dynamically by JS -->
                            <span style="font-size: 11.5px; color: #94a3b8;">Belum ada kamera yang ditambahkan di daftar bawah.</span>
                        </div>
                    </div>

                    <div class="grid-2" style="gap: 10px;">
                        <button type="button" class="btn-primary" style="background: linear-gradient(135deg, #0284c7, #0369a1); font-weight: 700;" onclick="playFullscreenCctvNow()">
                            ▶️ Tayangkan Kamera Aktif di TV (Layar Penuh)
                        </button>
                        <button type="button" class="btn-secondary" style="background: #334155; color: #fff; border: none; font-weight: 700;" onclick="stopFullscreenCctvNow()">
                            ⏹️ Hentikan Tayangan CCTV
                        </button>
                    </div>

                    <div style="margin-top: 10px; display: flex; flex-direction: column; gap: 8px;">
                        <button type="button" class="btn-primary" style="background: linear-gradient(135deg, #059669, #047857); font-weight: 700; padding: 10px 14px; font-size: 13px; box-shadow: 0 4px 12px rgba(5,150,105,0.25);" onclick="setBackgroundCctvNow(true)">
                            🖼️ Pasang CCTV Sebagai Background Layar TV (Jadwal Sholat Tetap Tampil)
                        </button>
                        <button type="button" class="btn-secondary" id="btnResetBgCctv" style="display: none; background: #64748b; color: #fff; border: none; font-weight: 700; padding: 8px 14px;" onclick="setBackgroundCctvNow(false)">
                            🔄 Kembalikan Background Layar TV ke Wallpaper Gambar
                        </button>
                    </div>
                </div>

                <!-- Card 2: Panduan & Preset Format URL Kamera IP -->
                <div class="card" style="margin-bottom: 14px;">
                    <div class="card-header">
                        <h2>📐 Panduan &amp; Preset Format URL Kamera IP / CCTV</h2>
                    </div>

                    <!-- Alert Box Khusus Tapo -->
                    <div style="background: #fffbeb; border: 1.5px solid #fde68a; border-radius: 10px; padding: 14px; margin-bottom: 14px;">
                        <div style="display: flex; align-items: flex-start; gap: 10px;">
                            <span style="font-size: 20px;">💡</span>
                            <div style="font-size: 12.5px; color: #92400e; line-height: 1.55;">
                                <strong style="color: #78350f; font-size: 13.5px;">Wajib Diketahui untuk Kamera TP-Link Tapo (C200, C210, C310, TC70, dll):</strong><br>
                                1. Buka aplikasi Tapo di HP &rarr; Masuk ke <strong>Pengaturan Kamera (Ikon Gerigi) &rarr; Pengaturan Lanjutan &rarr; Akun Kamera</strong> &rarr; Buat Username &amp; Password akun kamera lokal.<br>
                                2. Format URL RTSP yang WAJIB dimasukkan (harus menyertakan username &amp; password):<br>
                                &bull; <strong style="color: #047857;">Sub-Stream (Sangat Direkomendasikan):</strong> <code style="background: #fef3c7; padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #b45309;">rtsp://username:password@192.168.8.96:554/stream2</code> (Format H.264, instan terbuka, sangat ringan &amp; bebas buffering di TV).<br>
                                &bull; <strong style="color: #1e40af;">Main Stream (1080p/2K):</strong> <code style="background: #fef3c7; padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #b45309;">rtsp://username:password@192.168.8.96:554/stream1</code>.<br>
                                <em>Catatan: Jika tanpa username &amp; password, kamera Tapo otomatis menolak koneksi (Error 401).</em>
                            </div>
                        </div>
                    </div>

                    <p style="font-size: 12.5px; color: var(--text-muted); margin-bottom: 12px;">
                        Klik salah satu merek / tipe di bawah ini untuk mengisi format URL RTSP ke formulir tambah kamera:
                    </p>
                    <div class="grid-3" style="gap: 8px;">
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #ecfdf5; border: 1.5px solid #a7f3d0; border-radius: 8px;" onclick="applyCctvPreset('tapo_sub')">
                            <div style="font-weight: 800; color: #065f46; font-size: 12.5px;">⭐ TP-Link Tapo (stream2)</div>
                            <div style="font-size: 11px; color: #047857; margin-top: 2px;">Sub-stream H.264 (Paling Cepat &amp; Stabil)</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #fafcfb; border: 1.5px solid #cbd5e1; border-radius: 8px;" onclick="applyCctvPreset('tapo_main')">
                            <div style="font-weight: 800; color: #0f172a; font-size: 12.5px;">TP-Link Tapo (stream1)</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 2px;">Main Stream 1080p/2K (stream1)</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #fafcfb; border: 1.5px solid #cbd5e1; border-radius: 8px;" onclick="applyCctvPreset('hikvision')">
                            <div style="font-weight: 800; color: #0f172a; font-size: 12.5px;">Hikvision / Hilook</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 2px;">Streaming/Channels/101</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #fafcfb; border: 1.5px solid #cbd5e1; border-radius: 8px;" onclick="applyCctvPreset('dahua')">
                            <div style="font-weight: 800; color: #0f172a; font-size: 12.5px;">Dahua / IMOU</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 2px;">cam/realmonitor?channel=1</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #fafcfb; border: 1.5px solid #cbd5e1; border-radius: 8px;" onclick="applyCctvPreset('ezviz')">
                            <div style="font-weight: 800; color: #0f172a; font-size: 12.5px;">Ezviz</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 2px;">h264/ch1/main/av_stream</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #fafcfb; border: 1.5px solid #cbd5e1; border-radius: 8px;" onclick="applyCctvPreset('xiongmai')">
                            <div style="font-weight: 800; color: #0f172a; font-size: 12.5px;">Xiongmai / XM / DVR</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 2px;">V_ENC_001 (Port 554)</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 10px; text-align: left; background: #fafcfb; border: 1.5px solid #cbd5e1; border-radius: 8px;" onclick="applyCctvPreset('generic')">
                            <div style="font-weight: 800; color: #0f172a; font-size: 12.5px;">RTSP Standar / NVR / ONVIF</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 2px;">rtsp://admin:pass@ip:554/live</div>
                        </button>
                    </div>
                </div>

                <!-- Card 3: Daftar Kamera CCTV (Multi-Camera Manager) -->
                <div class="card" style="margin-bottom: 14px;">
                    <div class="card-header">
                        <h2>📹 Daftar Kamera CCTV Masjid</h2>
                    </div>

                    <!-- Container Daftar Kamera -->
                    <div id="cctvCamerasListContainer" style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px;">
                        <!-- Rendered by JS -->
                    </div>

                    <!-- Form Input Tambah Kamera -->
                    <div style="background: #f8fafc; border: 1.5px solid #cbd5e1; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                        <h4 style="margin: 0 0 10px 0; font-size: 13.5px; color: #0f172a; font-weight: 700;">➕ Tambah Kamera CCTV Baru</h4>
                        <div class="form-group" style="margin-bottom: 10px;">
                            <label style="font-size: 12px; font-weight: 700; color: #334155;">Nama Lokasi / Sudut Kamera:</label>
                            <input type="text" id="newCamName" class="form-control" placeholder="Contoh: Mimbar Khotib / Mihrab Imam / Ruang Akhwat / Parkiran">
                        </div>
                        <div class="form-group" style="margin-bottom: 10px;">
                            <label style="font-size: 12px; font-weight: 700; color: #334155;">URL Stream RTSP Kamera:</label>
                            <input type="text" id="newCamUrl" class="form-control" placeholder="rtsp://admin:password@192.168.1.50:554/live/ch0" style="font-family: monospace;">
                        </div>
                        <div style="display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 12px;">
                            <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; cursor: pointer; color: #475569;">
                                <input type="checkbox" id="newCamMuted" checked style="width: 16px; height: 16px; accent-color: #f59e0b;">
                                <span>🔇 Bisukan Suara (Mute)</span>
                            </label>
                            <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; cursor: pointer; color: #475569;">
                                <input type="checkbox" id="newCamShowSlide" style="width: 16px; height: 16px; accent-color: #059669;">
                                <span>🎞️ Tampilkan di Slide Carousel TV</span>
                            </label>
                        </div>
                        <button type="button" class="btn-primary" style="background: #0284c7; font-size: 13px; padding: 8px 16px;" onclick="addCameraItem()">
                            ➕ Tambahkan Kamera Ini ke Daftar
                        </button>
                    </div>

                    <!-- Layout Tampilan & Rotasi Otomatis -->
                    <div style="background: #f0f9ff; border: 1.5px solid #bae6fd; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                        <h4 style="margin: 0 0 10px 0; font-size: 13.5px; color: #0369a1; font-weight: 700;">🖥️ Mode Tampilan Layar TV &amp; Rotasi Otomatis</h4>
                        
                        <div class="grid-2" style="gap: 12px; margin-bottom: 12px;">
                            <div>
                                <label style="font-size: 12px; font-weight: 700; color: #0369a1; display: block; margin-bottom: 4px;">Pilihan Layout Multi-View:</label>
                                <select id="cfgCctvDisplayLayout" class="form-control" style="font-weight: 600;">
                                    <option value="SINGLE">Single Fullscreen (1 Kamera Penuh - Sangat Ringan & Stabil)</option>
                                    <option value="SPLIT_2">Split Screen (2 Kamera Berdampingan)</option>
                                    <option value="GRID_4">Grid 4 Kamera (4 Sudut Kamera Sekaligus 2x2)</option>
                                </select>
                            </div>
                            <div>
                                <label style="font-size: 12px; font-weight: 700; color: #0369a1; display: block; margin-bottom: 4px;">Durasi Rotasi per Kamera (Detik):</label>
                                <input type="number" id="cfgCctvAutoRotateInterval" class="form-control" value="30" min="5" max="300">
                            </div>
                        </div>

                        <label style="display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 700; color: #0369a1; cursor: pointer;">
                            <input type="checkbox" id="cfgCctvAutoRotateEnabled" style="width: 18px; height: 18px; accent-color: #0284c7;">
                            <span>🔄 Aktifkan Rotasi Sudut Kamera Otomatis (Auto-Tour Bergantian)</span>
                        </label>
                    </div>

                    <button type="button" class="btn-primary" style="background: linear-gradient(135deg, #0284c7, #0369a1); font-weight: 700;" onclick="saveAllCctvCameras()">
                        💾 Simpan Pengaturan Multi-Kamera &amp; Layout
                    </button>
                </div>

                <!-- Card 4: Penjadwalan Tayang Otomatis (Auto-Schedule) -->
                <div class="card">
                    <div class="card-header">
                        <h2>⏰ Penjadwalan Tayang Otomatis CCTV (Selepas Sholat / Jam Tertentu)</h2>
                    </div>
                    <form id="formCctvSchedule" onsubmit="saveCctvScheduleForm(event)">
                        <!-- Opsi A: Ba'da Sholat -->
                        <div style="background: #f0fdf4; border: 1.5px solid #bbf7d0; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 13.5px; font-weight: 700; color: #15803d; margin-bottom: 10px;">
                                <input type="checkbox" id="cfgCctvAutoPlayAfterPrayer" style="width: 20px; height: 20px; accent-color: #16a34a;">
                                <span>🟢 Tayangkan CCTV Otomatis Setiap Selesai Sholat Berjamaah (Kajian / Dzikir)</span>
                            </label>
                            
                            <div style="margin-left: 30px;">
                                <p style="font-size: 12px; color: #166534; margin: 0 0 10px 0;">
                                    Pilih waktu sholat yang memicu tayangan otomatis CCTV (misal kajian ba'da Subuh/Maghrib):
                                </p>
                                <div style="display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 12px;">
                                    <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #166534; cursor: pointer;">
                                        <input type="checkbox" id="chkPraySubuh" value="SUBUH" checked> Subuh
                                    </label>
                                    <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #166534; cursor: pointer;">
                                        <input type="checkbox" id="chkPrayDzuhur" value="DZUHUR"> Dzuhur
                                    </label>
                                    <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #166534; cursor: pointer;">
                                        <input type="checkbox" id="chkPrayAshar" value="ASHAR"> Ashar
                                    </label>
                                    <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #166534; cursor: pointer;">
                                        <input type="checkbox" id="chkPrayMaghrib" value="MAGHRIB" checked> Maghrib
                                    </label>
                                    <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #166534; cursor: pointer;">
                                        <input type="checkbox" id="chkPrayIsya" value="ISYA" checked> Isya
                                    </label>
                                    <label style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #166534; cursor: pointer;">
                                        <input type="checkbox" id="chkPrayJumat" value="JUMAT" checked> Jum'at (Khotib)
                                    </label>
                                </div>

                                <div class="grid-2" style="gap: 12px;">
                                    <div>
                                        <label style="font-size: 12px; font-weight: 700; color: #166534; display: block; margin-bottom: 4px;">Durasi Tayang Ba'da Sholat (Menit):</label>
                                        <input type="number" id="cfgCctvAfterPrayerDuration" class="form-control" value="30" min="5" max="120">
                                    </div>
                                    <div>
                                        <label style="font-size: 12px; font-weight: 700; color: #166534; display: block; margin-bottom: 4px;">Sorot Kamera:</label>
                                        <select id="cfgCctvAfterPrayerCamSelect" class="form-control" style="font-weight: 600;">
                                            <!-- Populated dynamically -->
                                        </select>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Opsi B: Jadwal Jam Manual -->
                        <div style="background: #eff6ff; border: 1.5px solid #bfdbfe; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 13.5px; font-weight: 700; color: #1d4ed8; margin-bottom: 10px;">
                                <input type="checkbox" id="cfgCctvScheduleEnabled" style="width: 20px; height: 20px; accent-color: #2563eb;">
                                <span>🔵 Tayangkan CCTV Otomatis Berdasarkan Jadwal Jam / Kajian Tertentu</span>
                            </label>

                            <div style="margin-left: 30px;">
                                <div class="grid-3" style="gap: 10px; margin-bottom: 10px;">
                                    <div>
                                        <label style="font-size: 12px; font-weight: 700; color: #1e40af; display: block; margin-bottom: 4px;">Jam Mulai (HH:mm):</label>
                                        <input type="time" id="cfgCctvScheduleStartTime" class="form-control" value="18:30">
                                    </div>
                                    <div>
                                        <label style="font-size: 12px; font-weight: 700; color: #1e40af; display: block; margin-bottom: 4px;">Jam Selesai (HH:mm):</label>
                                        <input type="time" id="cfgCctvScheduleEndTime" class="form-control" value="19:30">
                                    </div>
                                    <div>
                                        <label style="font-size: 12px; font-weight: 700; color: #1e40af; display: block; margin-bottom: 4px;">Sorot Kamera:</label>
                                        <select id="cfgCctvScheduleCamSelect" class="form-control" style="font-weight: 600;">
                                            <!-- Populated dynamically -->
                                        </select>
                                    </div>
                                </div>

                                <div>
                                    <label style="font-size: 12px; font-weight: 700; color: #1e40af; display: block; margin-bottom: 4px;">Pilihan Hari Tayang:</label>
                                    <select id="cfgCctvScheduleDays" class="form-control" style="font-weight: 600;">
                                        <option value="ALL">Setiap Hari (Senin - Ahad)</option>
                                        <option value="AHAD">Khusus Hari Ahad</option>
                                        <option value="JUMAT">Khusus Hari Jum'at</option>
                                        <option value="SABTU,AHAD">Akhir Pekan (Sabtu &amp; Ahad)</option>
                                        <option value="KAMIS,JUMAT">Kamis &amp; Jum'at</option>
                                    </select>
                                </div>
                            </div>
                        </div>

                        <button type="submit" class="btn-primary" style="background: linear-gradient(135deg, #10b981, #059669); font-weight: 700;">
                            💾 Simpan Jadwal Otomatis CCTV
                        </button>
                    </form>
                </div>
            </div>

            <!-- ==================== BAGIAN 2: YOUTUBE LIVE STREAMING ==================== -->
            <div style="margin-top: 30px;">
                <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #ef4444;">
                    <h2 style="font-size: 17px; font-weight: 800; color: #dc2626; margin: 0; display: flex; align-items: center; gap: 8px;">
                        <span>🔴</span> YouTube Live Streaming (Internet)
                    </h2>
                    <span style="font-size: 11px; font-weight: 700; background: #fee2e2; color: #b91c1c; padding: 3px 8px; border-radius: 6px; border: 1px solid #fecaca;">
                        🌐 Memerlukan Akses Internet
                    </span>
                </div>

                <!-- YouTube Live Status Card -->
                <div class="card" style="background: linear-gradient(135deg, #18181b 0%, #27272a 100%); color: #fff; border: 1.5px solid #ef4444; margin-bottom: 14px;">
                    <div class="card-header" style="border-bottom: 1px solid rgba(255,255,255,0.15);">
                        <h3 style="color: #f87171; display: flex; align-items: center; gap: 8px; font-size: 15px; margin: 0;">
                            <span>🔴</span> Status Live Streaming YouTube di TV
                        </h3>
                        <span id="youtubeLiveStatusBadge" style="background: rgba(239,68,68,0.2); color: #fca5a5; border: 1px solid #ef4444; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700;">
                            STANDBY NONAKTIF
                        </span>
                    </div>
                    <div style="margin-bottom: 16px;">
                        <div style="font-size: 12px; color: #d4d4d8; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">Siaran Aktif Saat Ini:</div>
                        <div id="youtubeLiveNowTitle" style="font-size: 16px; font-weight: 800; color: #ffffff; margin-top: 4px;">Tidak ada siaran live yang aktif</div>
                        <div id="youtubeLiveNowUrl" style="font-size: 12px; color: #a1a1aa; margin-top: 2px; word-break: break-all;">--</div>
                    </div>
                    <div class="grid-2" style="gap: 10px;">
                        <button type="button" class="btn-primary" style="background: linear-gradient(135deg, #ef4444, #dc2626); font-weight: 700;" onclick="toggleYoutubeLiveNow(true)">
                            🔴 Mulai Siaran YouTube di TV
                        </button>
                        <button type="button" class="btn-secondary" style="background: #3f3f46; color: #fff; border: none; font-weight: 700;" onclick="toggleYoutubeLiveNow(false)">
                            ⏹️ Hentikan Siaran YouTube
                        </button>
                    </div>
                </div>

                <!-- YouTube Quick Presets Card -->
                <div class="card" style="margin-bottom: 14px;">
                    <div class="card-header">
                        <h2>🕋 Pilihan Preset Siaran Langsung 24 Jam</h2>
                    </div>
                    <p style="font-size: 12.5px; color: var(--text-muted); margin-bottom: 14px;">
                        Klik salah satu preset siaran langsung di bawah ini untuk mengisi formulir secara otomatis:
                    </p>
                    <div class="grid-2" style="gap: 10px;" id="youtubePresetsContainer">
                        <button type="button" class="btn-secondary" style="padding: 12px; text-align: left; background: #fafcfb; border: 1.5px solid #d1d5db; border-radius: 10px;" onclick="applyYoutubeLivePreset('makkah_live')">
                            <div style="font-weight: 800; color: #111827; font-size: 13px;">🕋 Makkah Live (24 Jam)</div>
                            <div style="font-size: 11.5px; color: #6b7280; margin-top: 2px;">Siaran langsung Ka'bah & Masjidil Haram</div>
                        </button>
                        <button type="button" class="btn-secondary" style="padding: 12px; text-align: left; background: #fafcfb; border: 1.5px solid #d1d5db; border-radius: 10px;" onclick="applyYoutubeLivePreset('madinah_live')">
                            <div style="font-weight: 800; color: #111827; font-size: 13px;">🕌 Madinah Live (24 Jam)</div>
                            <div style="font-size: 11.5px; color: #6b7280; margin-top: 2px;">Siaran langsung Raudhah & Kubah Hijau</div>
                        </button>
                    </div>
                </div>

                <!-- YouTube Configuration Form Card -->
                <div class="card">
                    <div class="card-header">
                        <h2>⚙️ Konfigurasi URL Live Streaming YouTube</h2>
                    </div>
                    <form id="formYoutubeLive" onsubmit="saveYoutubeLive(event)">
                        <div class="form-group">
                            <label style="font-weight: 700; color: var(--primary-dark);">Tautan / URL Siaran YouTube Live:</label>
                            <input type="text" id="cfgYoutubeUrl" class="form-control" placeholder="Contoh: https://www.youtube.com/watch?v=... atau https://youtu.be/..." required>
                            <small style="color: var(--text-muted); font-size: 11px; margin-top: 4px; display: block;">
                                Mendukung semua format link YouTube (Live streaming, Video, atau ID video 11 karakter).
                            </small>
                        </div>

                        <div class="form-group">
                            <label style="font-weight: 700; color: var(--primary-dark);">Judul Siaran / Nama Acara:</label>
                            <input type="text" id="cfgYoutubeTitle" class="form-control" placeholder="Contoh: Kajian Rutin Ba'da Maghrib / Sholat Tarawih Live">
                        </div>

                        <div style="background: #fef2f2; border: 1.5px solid #fecaca; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 13.5px; font-weight: 700; color: #991b1b; margin-bottom: 8px;">
                                <input type="checkbox" id="cfgYoutubeLiveEnabled" style="width: 20px; height: 20px; accent-color: #dc2626;">
                                <span>🔴 Aktifkan Live Streaming YouTube di TV Sekarang</span>
                            </label>
                            <p style="font-size: 11.5px; color: #b91c1c; margin: 0 0 0 30px;">
                                Saat aktif, layar TV akan menampilkan video siaran langsung YouTube secara penuh dengan tetap menampilkan jam, jadwal sholat, dan running text.
                            </p>
                        </div>

                        <div style="background: #fffbeb; border: 1.5px solid #fde68a; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 13.5px; font-weight: 700; color: #b45309; margin-bottom: 6px;">
                                <input type="checkbox" id="cfgYoutubeLiveMuted" style="width: 20px; height: 20px; accent-color: #f59e0b;">
                                <span>🔇 Bisukan Suara Video di TV (Mute Audio)</span>
                            </label>
                            <p style="font-size: 11.5px; color: #92400e; margin: 0 0 0 30px;">
                                Centang jika siaran YouTube hanya untuk visual tanpa suara di speaker TV (misal siaran Makkah/Madinah 24 Jam saat ada aktivitas lain di masjid).
                            </p>
                        </div>

                        <div style="background: #fafcfb; border: 1px solid #e5e7eb; padding: 14px; border-radius: 10px; margin-bottom: 16px;">
                            <label style="display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 13px; font-weight: 600; color: #374151;">
                                <input type="checkbox" id="cfgShowYoutubeLiveSlide" style="width: 18px; height: 18px; accent-color: #059669;">
                                <span>🎞️ Tampilkan Juga Sebagai Salah Satu Slide di Rotasi Carousel</span>
                            </label>
                        </div>

                        <button type="submit" class="btn-primary" style="background: linear-gradient(135deg, #ef4444, #b91c1c); font-weight: 700;">
                            💾 Simpan Pengaturan YouTube Live di TV
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    </div> <!-- /#dashboardSection -->

    <div class="toast" id="toastMsg">Pengaturan Disimpan</div>

    <script>
        // Intercept native fetch to auto-attach authorization token
        const _nativeFetch = window.fetch;
        window.fetch = async function(url, options = {}) {
            options = options || {};
            options.headers = options.headers || {};
            const token = localStorage.getItem('masjidku_session_token') || '';
            if (token) {
                if (options.headers instanceof Headers) {
                    options.headers.set('Authorization', 'Bearer ' + token);
                    options.headers.set('X-Auth-Token', token);
                } else {
                    options.headers['Authorization'] = 'Bearer ' + token;
                    options.headers['X-Auth-Token'] = token;
                }
            }
            const res = await _nativeFetch(url, options);
            if (res.status === 401 && !url.includes('/api/login') && !url.includes('/api/check-session')) {
                localStorage.removeItem('masjidku_session_token');
                showLoginSection();
                showToast('Sesi berakhir atau PIN salah. Silakan login kembali.');
            }
            return res;
        };

        function showToast(msg) {
            const t = document.getElementById('toastMsg');
            t.innerText = msg;
            t.classList.add('show');
            setTimeout(() => t.classList.remove('show'), 2500);
        }

        function escapeHtml(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function switchTab(tabId, btn) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.section-tab').forEach(s => s.classList.remove('active'));
            if (btn && btn.classList) {
                btn.classList.add('active');
            } else if (typeof event !== 'undefined' && event && event.target) {
                const target = event.target.closest ? event.target.closest('.tab-btn') : event.target;
                if (target && target.classList) target.classList.add('active');
            }
            const targetSec = document.getElementById(tabId);
            if (targetSec) targetSec.classList.add('active');
            if (tabId === 'tab-profil' && map) {
                setTimeout(() => { map.invalidateSize(); }, 200);
            }
            if (tabId === 'tab-video') {
                loadVideoList();
            }
        }

        function formatRupiah(num) {
            return 'Rp ' + Number(num).toLocaleString('id-ID');
        }

        let map = null;
        let marker = null;

        function initMap(lat, lng) {
            const mapContainer = document.getElementById('mosqueMap');
            if (!mapContainer) return;

            if (typeof L === 'undefined') {
                mapContainer.innerHTML = '<div style="padding:30px 15px; text-align:center; color:#5A7569;"><p style="font-weight:700;">⚠️ Peta Memerlukan Akses Internet</p><p style="font-size:12px; margin-top:6px;">Jika TV berada di jaringan lokal tanpa internet, Anda tetap dapat memilih dari dropdown daftar kota atau mengetik koordinat manual.</p></div>';
                return;
            }

            const initialLat = parseFloat(lat) || -6.3078;
            const initialLng = parseFloat(lng) || 107.9945;

            if (!map) {
                try {
                    map = L.map('mosqueMap').setView([initialLat, initialLng], 14);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    marker = L.marker([initialLat, initialLng], {
                        draggable: true
                    }).addTo(map);

                    marker.bindPopup("<b>🕌 Titik Lokasi Masjid</b><br>Geser pin tepat di atas atap kubah masjid.").openPopup();

                    marker.on('dragend', function () {
                        const pos = marker.getLatLng();
                        updateCoordsFromMap(pos.lat, pos.lng, false);
                    });

                    map.on('click', function (e) {
                        marker.setLatLng(e.latlng);
                        updateCoordsFromMap(e.latlng.lat, e.latlng.lng, false);
                        map.panTo(e.latlng);
                    });

                    setTimeout(() => { map.invalidateSize(); }, 400);
                } catch (e) {
                    console.error("Leaflet init error:", e);
                }
            } else {
                map.setView([initialLat, initialLng], 14);
                if (marker) marker.setLatLng([initialLat, initialLng]);
            }
            updateCoordsFromMap(initialLat, initialLng, false);
        }

        function updateCoordsFromMap(lat, lng, doFly) {
            const fixedLat = parseFloat(lat).toFixed(6);
            const fixedLng = parseFloat(lng).toFixed(6);

            const elLat = document.getElementById('cfgLat');
            const elLng = document.getElementById('cfgLng');
            if (elLat) elLat.value = fixedLat;
            if (elLng) elLng.value = fixedLng;

            const elLat2 = document.getElementById('cfgLat2');
            const elLng2 = document.getElementById('cfgLng2');
            if (elLat2) elLat2.value = fixedLat;
            if (elLng2) elLng2.value = fixedLng;

            // Hitung otomatis Zona Waktu Indonesia (WIB, WITA, WIT)
            let tz = 7.0;
            let tzName = "WIB (GMT+7)";
            const numLng = parseFloat(fixedLng);
            if (numLng >= 115.0 && numLng < 125.0) {
                tz = 8.0;
                tzName = "WITA (GMT+8)";
            } else if (numLng >= 125.0) {
                tz = 9.0;
                tzName = "WIT (GMT+9)";
            }

            const elTz = document.getElementById('cfgTimezone');
            if (elTz) elTz.value = tz.toFixed(1);
            const elTz2 = document.getElementById('cfgTimezone2');
            if (elTz2) elTz2.value = tz.toFixed(1);

            const tzBadge = document.getElementById('tzBadge');
            if (tzBadge) tzBadge.innerText = tzName;

            const cityInfo = document.getElementById('cityInfoText');
            if (cityInfo) {
                cityInfo.innerHTML = '📍 <strong>Koordinat Terpilih:</strong> ' + fixedLat + ', ' + fixedLng;
            }

            if (marker) {
                marker.setLatLng([parseFloat(fixedLat), parseFloat(fixedLng)]);
            }
            if (doFly && map) {
                map.flyTo([parseFloat(fixedLat), parseFloat(fixedLng)], 15);
            }
        }

        function onManualCoordChange() {
            const lat = parseFloat(document.getElementById('cfgLat').value);
            const lng = parseFloat(document.getElementById('cfgLng').value);
            if (!isNaN(lat) && !isNaN(lng)) {
                updateCoordsFromMap(lat, lng, true);
            }
        }

        function onCitySelect(val) {
            if (!val) return;
            const parts = val.split('|');
            if (parts.length >= 4) {
                const cityName = parts[0];
                const lat = parseFloat(parts[1]);
                const lng = parseFloat(parts[2]);
                const tz = parseFloat(parts[3]);

                document.getElementById('cfgCity').value = cityName;
                updateCoordsFromMap(lat, lng, true);
                if (marker) {
                    marker.bindPopup("<b>🕌 " + cityName + "</b><br>Geser pin ke lokasi masjid spesifik.").openPopup();
                }
            }
        }

        function detectCurrentLocation() {
            if (!navigator.geolocation) {
                alert("Fitur GPS tidak didukung pada browser ini.");
                return;
            }
            const info = document.getElementById('cityInfoText');
            if (info) info.innerHTML = '⏳ <i>Mendeteksi sinyal GPS HP Anda...</i>';

            navigator.geolocation.getCurrentPosition(
                function (pos) {
                    const lat = pos.coords.latitude;
                    const lng = pos.coords.longitude;
                    updateCoordsFromMap(lat, lng, true);
                    showToast("Lokasi GPS berhasil dikunci!");
                    if (marker) {
                        marker.bindPopup("<b>📍 Lokasi GPS Anda</b><br>Koordinat berhasil dikunci.").openPopup();
                    }
                    reverseGeocode(lat, lng);
                },
                function (err) {
                    alert("Gagal membaca GPS: " + err.message + "\nPastikan izin lokasi (GPS) diaktifkan di browser HP Anda.");
                    if (info) info.innerHTML = '⚠️ GPS belum diizinkan. Silakan klik langsung pada peta.';
                },
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
            );
        }

        async function searchMapAddress() {
            const query = (document.getElementById('mapSearchInput').value || '').trim();
            if (!query) {
                alert("Ketik nama desa, jalan, kecamatan, atau kota yang ingin dicari.");
                return;
            }
            const info = document.getElementById('cityInfoText');
            if (info) info.innerHTML = '⏳ <i>Mencari "' + query + '" di peta...</i>';

            try {
                const res = await fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(query + ', Indonesia'));
                const results = await res.json();
                if (results && results.length > 0) {
                    const first = results[0];
                    const lat = parseFloat(first.lat);
                    const lng = parseFloat(first.lon);
                    updateCoordsFromMap(lat, lng, true);
                    showToast("Ditemukan: " + first.display_name.split(',')[0]);
                    if (marker) {
                        marker.bindPopup("<b>📍 " + (first.display_name.split(',')[0]) + "</b>").openPopup();
                    }
                } else {
                    alert("Wilayah tidak ditemukan di OpenStreetMap. Coba ketik nama kecamatan atau kota yang lebih umum.");
                    if (info) info.innerHTML = '⚠️ Wilayah tidak ditemukan. Geser pin manual pada peta.';
                }
            } catch (e) {
                console.error(e);
                alert("Pencarian peta membutuhkan internet. Anda tetap dapat menggeser pin langsung pada peta.");
            }
        }

        async function reverseGeocode(lat, lng) {
            try {
                const res = await fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng);
                const data = await res.json();
                if (data && data.address) {
                    const city = data.address.city || data.address.town || data.address.municipality || data.address.county || data.address.state_district || '';
                    if (city) {
                        const currentCity = document.getElementById('cfgCity').value;
                        if (!currentCity || currentCity === 'Indramayu' || currentCity === 'Jakarta') {
                            document.getElementById('cfgCity').value = city.replace(/Kota |Kabupaten /g, '');
                        }
                    }
                }
            } catch (e) {
                // Ignore background reverse geocoding errors
            }
        }

        async function loadStatus() {
            try {
                const res = await fetch('/api/status');
                const data = await res.json();
                renderData(data);
            } catch (e) {
                console.error(e);
            }
        }

        function renderData(data) {
            const cfg = data.config || {};
            window.currentConfig = cfg;
            document.getElementById('headerMosqueName').innerText = cfg.mosqueName || 'Masjid';
            document.getElementById('cfgMosqueName').value = cfg.mosqueName || '';
            document.getElementById('cfgTagline').value = cfg.tagline || '';
            document.getElementById('cfgAddress').value = cfg.address || '';
            document.getElementById('cfgCity').value = cfg.city || '';
            document.getElementById('cfgPhone').value = cfg.phone || '';
            document.getElementById('cfgBankName').value = cfg.bankName || '';
            document.getElementById('cfgBankAccount').value = cfg.bankAccount || '';
            document.getElementById('cfgBankAccountName').value = cfg.bankAccountName || '';

            const curLat = cfg.latitude || -6.3078;
            const curLng = cfg.longitude || 107.9945;
            const curTz = cfg.timezone || 7.0;

            document.getElementById('cfgLat').value = curLat;
            document.getElementById('cfgLng').value = curLng;
            document.getElementById('cfgTimezone').value = curTz.toFixed(1);
            if (document.getElementById('cfgLat2')) document.getElementById('cfgLat2').value = curLat;
            if (document.getElementById('cfgLng2')) document.getElementById('cfgLng2').value = curLng;
            if (document.getElementById('cfgTimezone2')) document.getElementById('cfgTimezone2').value = curTz.toFixed(1);

            initMap(curLat, curLng);
            document.getElementById('cfgHijri').value = cfg.hijriAdjustmentDays || 0;
            document.getElementById('cfgTheme').value = cfg.activeTheme || 'EMERALD_GOLD';
            document.getElementById('cfgLayoutModel').value = cfg.displayLayoutModel || 'CAROUSEL_BOTTOM';

            // Prayer checkboxes
            document.getElementById('showImsak').checked = cfg.showImsak !== false;
            document.getElementById('showSubuh').checked = cfg.showSubuh !== false;
            document.getElementById('showTerbit').checked = cfg.showTerbit !== false;
            document.getElementById('showDhuha').checked = cfg.showDhuha !== false;
            document.getElementById('showDzuhur').checked = cfg.showDzuhur !== false;
            document.getElementById('showAshar').checked = cfg.showAshar !== false;
            document.getElementById('showMaghrib').checked = cfg.showMaghrib !== false;
            document.getElementById('showIsya').checked = cfg.showIsya !== false;

            // Content checkboxes (both Tab 1 and Tab 8)
            const finCheck = cfg.showFinancialReport !== false;
            const friCheck = cfg.showFridayOfficers !== false;
            const qrisCheck = cfg.showQrisCard !== false;
            const hadithCheck = cfg.showDailyHadith !== false;
            const actCheck = cfg.showActivities !== false;
            const maklumatCheck = cfg.showDailyMaklumat !== false;

            if (document.getElementById('showFinReport')) document.getElementById('showFinReport').checked = finCheck;
            if (document.getElementById('showFriOfficers')) document.getElementById('showFriOfficers').checked = friCheck;
            if (document.getElementById('showQrisCard')) document.getElementById('showQrisCard').checked = qrisCheck;
            if (document.getElementById('showDailyHadith')) document.getElementById('showDailyHadith').checked = hadithCheck;
            if (document.getElementById('showActivities')) document.getElementById('showActivities').checked = actCheck;
            if (document.getElementById('showDailyMaklumat')) document.getElementById('showDailyMaklumat').checked = maklumatCheck;

            if (document.getElementById('showFinReportTab1')) document.getElementById('showFinReportTab1').checked = finCheck;
            if (document.getElementById('showFriOfficersTab1')) document.getElementById('showFriOfficersTab1').checked = friCheck;
            if (document.getElementById('showQrisCardTab1')) document.getElementById('showQrisCardTab1').checked = qrisCheck;
            if (document.getElementById('showDailyHadithTab1')) document.getElementById('showDailyHadithTab1').checked = hadithCheck;
            if (document.getElementById('showActivitiesTab1')) document.getElementById('showActivitiesTab1').checked = actCheck;
            if (document.getElementById('showDailyMaklumatTab1')) document.getElementById('showDailyMaklumatTab1').checked = maklumatCheck;

            // Render Tarawih Ramadhan
            if (typeof renderTarawih === 'function') {
                renderTarawih(data.tarawihSchedules || [], data.activeTarawihNight || 1, cfg);
            }

            // Donation Program Form
            if (document.getElementById('cfgDonationTitle')) {
                document.getElementById('cfgDonationTitle').value = cfg.donationProgramTitle || 'Renovasi Fasilitas Masjid';
                document.getElementById('cfgDonationTarget').value = cfg.donationTargetAmount || 25000000;
                document.getElementById('cfgDonationCollected').value = cfg.donationCollectedAmount || 0;
            }

            if (document.getElementById('qrisPreviewBox')) {
                if (cfg.qrisImagePath) {
                    document.getElementById('qrisPreviewBox').style.display = 'block';
                    var qrisName = cfg.qrisImagePath.replace(/\\/g, '/').split('/').pop();
                    document.getElementById('qrisPreviewImg').src = '/media/' + qrisName + '?t=' + Date.now();
                } else {
                    document.getElementById('qrisPreviewBox').style.display = 'none';
                }
            }

            // Status Video Lokal
            const statusBgText = document.getElementById('statusBgVideoText');
            if (statusBgText) {
                if (cfg.backgroundType === 'VIDEO' && cfg.backgroundVideoPath) {
                    const vName = cfg.backgroundVideoPath.replace(/\\/g, '/').split('/').pop();
                    statusBgText.innerHTML = '<span style="color:#059669; font-weight:700;">🟢 Aktif: ' + escapeHtml(vName) + '</span>';
                } else {
                    statusBgText.innerText = 'Sedang menggunakan Preset Gambar (' + (cfg.mainScreenBgPreset || 'Default') + ')';
                }
            }

            const statusFsText = document.getElementById('statusFullscreenVideoText');
            const btnStopFs = document.getElementById('btnStopFullscreenVideo');
            if (statusFsText) {
                if (cfg.fullscreenVideoPlaying && cfg.fullscreenVideoPath) {
                    const fsTitle = cfg.fullscreenVideoTitle || cfg.fullscreenVideoPath.replace(/\\/g, '/').split('/').pop();
                    statusFsText.innerHTML = '<span style="color:#dc2626; font-weight:700;">🔴 Sedang Tayang di Layar TV: ' + escapeHtml(fsTitle) + '</span>';
                    if (btnStopFs) btnStopFs.style.display = 'block';
                } else {
                    statusFsText.innerText = 'Standby (Tidak ada video yang sedang tayang)';
                    if (btnStopFs) btnStopFs.style.display = 'none';
                }
            }

            document.getElementById('offSubuh').value = cfg.offsetSubuh || 2;
            document.getElementById('offTerbit').value = cfg.offsetTerbit || -2;
            document.getElementById('offDzuhur').value = cfg.offsetDzuhur || 2;
            document.getElementById('offAshar').value = cfg.offsetAshar || 2;
            document.getElementById('offMaghrib').value = cfg.offsetMaghrib || 2;
            document.getElementById('offIsya').value = cfg.offsetIsya || 2;

            document.getElementById('iqSubuh').value = cfg.iqomahSubuh || 10;
            document.getElementById('iqDzuhur').value = cfg.iqomahDzuhur || 10;
            if (document.getElementById('iqJumat')) document.getElementById('iqJumat').value = cfg.iqomahJumat || 15;
            document.getElementById('iqAshar').value = cfg.iqomahAshar || 10;
            document.getElementById('iqMaghrib').value = cfg.iqomahMaghrib || 7;
            document.getElementById('iqIsya').value = cfg.iqomahIsya || 10;
            document.getElementById('sholatDuration').value = cfg.sholatDurationMinutes || 10;

            // Stats
            document.getElementById('statBalance').innerText = formatRupiah(data.balance || 0);
            document.getElementById('statIncome').innerText = formatRupiah(data.totalIncome || 0);
            document.getElementById('statExpense').innerText = formatRupiah(data.totalExpense || 0);

            // Finances (Full CRUD)
            const finList = document.getElementById('financeList');
            finList.innerHTML = '';
            (data.finances || []).forEach(function(f) {
                const li = document.createElement('li');
                li.className = 'item-row';
                const isInc = f.type === 'INCOME';
                const icon = isInc ? '🟢' : '🔴';
                li.innerHTML = '<div class="item-meta">' +
                    '<div class="item-title">' + icon + ' ' + escapeHtml(f.title) + ' (' + escapeHtml(f.category) + ')</div>' +
                    '<div class="item-sub">' + escapeHtml(f.date) + ' &bull; <strong>' + formatRupiah(f.amount) + '</strong></div>' +
                    '</div>' +
                    '<div style="display:flex; gap:6px;">' +
                    '<button type="button" class="btn-primary btn-edit-fin" style="padding:4px 8px; font-size:12px; width:auto;">✏️ Edit</button>' +
                    '<button type="button" class="btn-danger btn-del-fin">Hapus</button>' +
                    '</div>';
                li.querySelector('.btn-edit-fin').onclick = function() {
                    startEditFinance(f.id, f.title, f.amount, f.type, f.category, f.date, f.notes || '');
                };
                li.querySelector('.btn-del-fin').onclick = function() {
                    deleteFinance(f.id);
                };
                finList.appendChild(li);
            });

            // Friday Multi-week Support
            window.allFridayList = data.allFriday || [];
            window.upcomingFridayWeek = data.upcomingFridayWeek || 1;
            if (!window.userSelectedFridayWeek) {
                window.currentFriWeek = window.upcomingFridayWeek;
            }
            renderFridayWeek(window.currentFriWeek);

            // WhatsApp Gateway Config Fields
            if (document.getElementById('cfgWaEnabled')) {
                document.getElementById('cfgWaEnabled').checked = cfg.waGatewayEnabled === true;
            }
            if (document.getElementById('cfgWaToken')) {
                document.getElementById('cfgWaToken').value = cfg.waGatewayToken || '';
            }
            if (document.getElementById('cfgWaSendThursdayHour')) {
                document.getElementById('cfgWaSendThursdayHour').value = (cfg.waGatewaySendThursdayHour || 9).toString();
            }
            if (document.getElementById('cfgWaSendFridayHour')) {
                document.getElementById('cfgWaSendFridayHour').value = (cfg.waGatewaySendFridayHour || 9).toString();
            }
            if (document.getElementById('cfgWaTemplateThursday')) {
                document.getElementById('cfgWaTemplateThursday').value = cfg.waGatewayTemplateThursday || '';
            }
            if (document.getElementById('cfgWaTemplateFriday')) {
                document.getElementById('cfgWaTemplateFriday').value = cfg.waGatewayTemplateFriday || '';
            }
            if (document.getElementById('cfgWaTemplateKajian')) {
                document.getElementById('cfgWaTemplateKajian').value = cfg.waGatewayTemplateKajian || '';
            }
            if (cfg.waGatewayLastLogJson && document.getElementById('waBroadcastLogBox')) {
                document.getElementById('waBroadcastLogBox').style.display = 'block';
                document.getElementById('waBroadcastLogBox').innerHTML = '🕒 <strong>Riwayat Pengiriman Terakhir:</strong> ' + escapeHtml(cfg.waGatewayLastLogJson);
            }

            // Activities (Full CRUD)
            const actList = document.getElementById('activityList');
            actList.innerHTML = '';
            (data.activities || []).forEach(function(a) {
                const li = document.createElement('li');
                li.className = 'item-row';
                const phoneLabel = a.speakerPhone ? ' <span style="background:#DCFCE7; color:#15803D; font-size:11px; padding:2px 6px; border-radius:4px; font-weight:600;">📱 ' + escapeHtml(a.speakerPhone) + '</span>' : '';
                li.innerHTML = '<div class="item-meta">' +
                    '<div class="item-title">📖 ' + escapeHtml(a.title) + '</div>' +
                    '<div class="item-sub">🎙️ ' + escapeHtml(a.speaker) + phoneLabel + ' &bull; ' + escapeHtml(a.date) + ' (' + escapeHtml(a.time) + ') [' + escapeHtml(a.location) + ']</div>' +
                    '</div>' +
                    '<div style="display:flex; gap:6px; align-items:center;">' +
                    '<button type="button" class="btn-primary btn-wa-act" style="padding:4px 8px; font-size:12px; width:auto; background:#16A34A; border-color:#15803D;" title="Kirim Pengingat WhatsApp ke Ustadz">💬 Kirim WA</button>' +
                    '<button type="button" class="btn-primary btn-edit-act" style="padding:4px 8px; font-size:12px; width:auto;">✏️ Edit</button>' +
                    '<button type="button" class="btn-danger btn-del-act">Hapus</button>' +
                    '</div>';
                li.querySelector('.btn-wa-act').onclick = function() {
                    sendActivityWaReminder(a.id, a.speaker, a.speakerPhone);
                };
                li.querySelector('.btn-edit-act').onclick = function() {
                    startEditActivity(a.id, a.title, a.speaker, a.speakerPhone || '', a.date, a.time, a.location, a.category);
                };
                li.querySelector('.btn-del-act').onclick = function() {
                    deleteActivity(a.id);
                };
                actList.appendChild(li);
            });

            // Running Texts (Full CRUD)
            const runList = document.getElementById('runningList');
            runList.innerHTML = '';
            (data.runningTexts || []).forEach(function(r) {
                const li = document.createElement('li');
                li.className = 'item-row';
                li.innerHTML = '<div class="item-meta">' +
                    '<div class="item-title">📢 ' + escapeHtml(r.text) + '</div>' +
                    '</div>' +
                    '<div style="display:flex; gap:6px;">' +
                    '<button type="button" class="btn-primary btn-edit-run" style="padding:4px 8px; font-size:12px; width:auto;">✏️ Edit</button>' +
                    '<button type="button" class="btn-danger btn-del-run">Hapus</button>' +
                    '</div>';
                li.querySelector('.btn-edit-run').onclick = function() {
                    startEditRunningText(r.id, r.text);
                };
                li.querySelector('.btn-del-run').onclick = function() {
                    deleteRunningText(r.id);
                };
                runList.appendChild(li);
            });

            if (document.getElementById('cfgRunningFontSize')) {
                document.getElementById('cfgRunningFontSize').value = (cfg.runningTextFontSize || 20).toString();
            }

            // Background Presets & Custom paths
            if (document.getElementById('cfgMainScreenBgPreset')) {
                document.getElementById('cfgMainScreenBgPreset').value = cfg.mainScreenBgPreset || 'PRESET_EMERALD_MIHRAB';
            }
            if (document.getElementById('cfgFinanceBgPreset')) {
                document.getElementById('cfgFinanceBgPreset').value = cfg.financeBgPreset || 'PRESET_WHITE_PEARL_GOLD';
            }
            if (document.getElementById('cfgFridayOfficerBgPreset')) {
                document.getElementById('cfgFridayOfficerBgPreset').value = cfg.fridayOfficerBgPreset || 'PRESET_EMERALD_MIHRAB';
            }
            if (document.getElementById('cfgCountdownBgPreset')) {
                document.getElementById('cfgCountdownBgPreset').value = cfg.countdownBgPreset || 'PRESET_COUNTDOWN_PLAQUE';
            }
            if (document.getElementById('cardTransparencySelect')) {
                document.getElementById('cardTransparencySelect').value = (cfg.centerCardTransparency || 0.35).toString();
            }

            const isCctvBg = cfg.backgroundType === 'CCTV';
            const isVideoBg = cfg.backgroundType === 'VIDEO' && !!cfg.backgroundVideoPath;
            const hasCustomBg = !isCctvBg && !isVideoBg && !!(cfg.customBackgroundImagePath && cfg.customBackgroundImagePath.length > 0);

            const cctvBgSelector = document.getElementById('cctvBgSelectorContainer');
            if (cctvBgSelector) cctvBgSelector.style.display = isCctvBg ? 'block' : 'none';

            if (isCctvBg) {
                if (document.getElementById('cfgMainScreenBgPreset')) {
                    document.getElementById('cfgMainScreenBgPreset').value = 'CCTV_LIVE';
                }
                document.getElementById('bgStatusText').innerHTML = '<span style="color:#059669; font-weight:700;">📹 Live Stream CCTV Aktif Sebagai Background TV</span>';
                document.getElementById('bgStatusBox').style.background = '#D1FAE5';
                document.getElementById('btnRemoveBg').style.display = 'none';
            } else if (isVideoBg) {
                document.getElementById('bgStatusText').innerHTML = '<span style="color:#2563eb; font-weight:700;">🎬 Video Lokal Aktif Sebagai Background</span>';
                document.getElementById('bgStatusBox').style.background = '#EFF6FF';
                document.getElementById('btnRemoveBg').style.display = 'none';
            } else if (hasCustomBg) {
                document.getElementById('bgStatusText').innerText = '✅ Foto Wallpaper Kustom Aktif';
                document.getElementById('bgStatusBox').style.background = '#D1FAE5';
                document.getElementById('btnRemoveBg').style.display = 'block';
            } else {
                document.getElementById('bgStatusText').innerText = 'Preset Aktif (' + (cfg.mainScreenBgPreset || 'Emerald Nabawi') + ')';
                document.getElementById('bgStatusBox').style.background = '#EEF2F0';
                document.getElementById('btnRemoveBg').style.display = 'none';
            }

            const hasCustomFin = !!(cfg.customFinanceBgPath && cfg.customFinanceBgPath.length > 0);
            const btnRemFin = document.getElementById('btnRemoveFinanceBg');
            if (btnRemFin) btnRemFin.style.display = hasCustomFin ? 'inline-block' : 'none';

            const hasCustomFri = !!(cfg.customFridayOfficerBgPath && cfg.customFridayOfficerBgPath.length > 0);
            const btnRemFri = document.getElementById('btnRemoveFridayBg');
            if (btnRemFri) btnRemFri.style.display = hasCustomFri ? 'inline-block' : 'none';

            const hasCustomCd = !!(cfg.customCountdownBgPath && cfg.customCountdownBgPath.length > 0);
            const btnRemCd = document.getElementById('btnRemoveCountdownBg');
            if (btnRemCd) btnRemCd.style.display = hasCustomCd ? 'inline-block' : 'none';

            if (cfg.customBackgroundDim) {
                document.getElementById('bgDimSelect').value = cfg.customBackgroundDim.toString();
            }

            // Custom Logo Status
            const hasCustomLogo = !!(cfg.customLogoImagePath && cfg.customLogoImagePath.length > 0);
            const logoStatusEl = document.getElementById('logoStatusText');
            const logoStatusBox = document.getElementById('logoStatusBox');
            const btnRemoveLogo = document.getElementById('btnRemoveLogo');
            const logoPreviewEl = document.getElementById('logoPreviewImg');
            if (logoStatusEl) logoStatusEl.innerText = hasCustomLogo ? '✅ Logo Kustom Aktif' : '📌 Logo Default (Vector)';
            if (logoStatusBox) logoStatusBox.style.background = hasCustomLogo ? '#D1FAE5' : '#EEF2F0';
            if (btnRemoveLogo) btnRemoveLogo.style.display = hasCustomLogo ? 'inline-block' : 'none';
            if (logoPreviewEl) {
                if (hasCustomLogo) {
                    logoPreviewEl.src = '/media/logo/mosque_logo.' + cfg.customLogoImagePath.split('.').pop();
                    logoPreviewEl.style.display = 'block';
                } else {
                    logoPreviewEl.style.display = 'none';
                }
            }

            // Media Slides List
            const mediaList = document.getElementById('mediaSlideList');
            mediaList.innerHTML = '';
            (data.mediaSlides || []).forEach(function(m) {
                const li = document.createElement('li');
                li.className = 'item-row';
                li.innerHTML = '<div style="display:flex; align-items:center; gap:10px; flex:1;">' +
                    '<img src="' + m.webUrl + '" style="width:50px; height:50px; object-fit:cover; border-radius:6px; border:1px solid #ccc;">' +
                    '<div class="item-meta">' +
                    '<div class="item-title">' + m.title + '</div>' +
                    '<div class="item-sub">⏱️ ' + m.durationSeconds + ' detik &bull; ' + (m.isActive ? '🟢 Aktif' : '⚪ Nonaktif') + '</div>' +
                    '</div>' +
                    '</div>' +
                    '<button class="btn-danger" onclick="deleteMediaSlide(' + m.id + ')">Hapus</button>';
                mediaList.appendChild(li);
            });

            // Pre-Adhan Countdown & 5 Daily Imams
            if (document.getElementById('enablePreAdhanCountdown')) {
                document.getElementById('enablePreAdhanCountdown').checked = cfg.enablePreAdhanCountdown !== false;
                document.getElementById('preAdhanCountdownSeconds').value = cfg.preAdhanCountdownSeconds || 30;
                document.getElementById('imamSubuh').value = cfg.imamSubuh || '';
                document.getElementById('imamDzuhur').value = cfg.imamDzuhur || '';
                document.getElementById('imamAshar').value = cfg.imamAshar || '';
                document.getElementById('imamMaghrib').value = cfg.imamMaghrib || '';
                document.getElementById('imamIsya').value = cfg.imamIsya || '';
                document.getElementById('muadzinSubuh').value = cfg.muadzinSubuh || '';
                document.getElementById('muadzinDzuhur').value = cfg.muadzinDzuhur || '';
                document.getElementById('muadzinAshar').value = cfg.muadzinAshar || '';
                document.getElementById('muadzinMaghrib').value = cfg.muadzinMaghrib || '';
                document.getElementById('muadzinIsya').value = cfg.muadzinIsya || '';
            }

            // Set default date for special imam form if empty
            const specialDateInput = document.getElementById('specialImamDate');
            if (specialDateInput && !specialDateInput.value) {
                const now = new Date();
                const year = now.getFullYear();
                const month = String(now.getMonth() + 1).padStart(2, '0');
                const day = String(now.getDate()).padStart(2, '0');
                specialDateInput.value = year + '-' + month + '-' + day;
            }

            // Daily Imams Special Schedules List
            const specialList = document.getElementById('specialImamList');
            if (specialList) {
                specialList.innerHTML = '';
                const imams = data.dailyImams || [];
                if (imams.length === 0) {
                    specialList.innerHTML = '<li style="padding: 10px; color: #5A7569; font-size: 12px; text-align: center;">Belum ada jadwal imam khusus tanggal tertentu. Sistem menggunakan jadwal harian default di atas.</li>';
                } else {
                    imams.forEach(function(im) {
                        const li = document.createElement('li');
                        li.className = 'item-row';
                        const muadzinInfo = im.muadzinName ? (' &bull; 🎙️ ' + im.muadzinName) : '';
                        const notesInfo = im.notes ? (' <span style="font-size:11px; color:#5A7569;">(' + im.notes + ')</span>') : '';
                        li.innerHTML = '<div class="item-meta">' +
                            '<div class="item-title">👳 <strong>' + im.imamName + '</strong> ' + notesInfo + '</div>' +
                            '<div class="item-sub">📅 ' + im.date + ' &bull; ⏰ <strong>Sholat ' + (im.prayerDisplayName || im.prayerName) + '</strong>' + muadzinInfo + '</div>' +
                            '</div>' +
                            '<button class="btn-danger" onclick="deleteSpecialImam(' + im.id + ')">Hapus</button>';
                        specialList.appendChild(li);
                    });
                }
            }

            renderMurottalData(data);
            renderYoutubeData(data);
        }

        // ==================== YOUTUBE & CCTV LIVE SECTION ====================
        var allYoutubePresets = [];
        var localCctvCameras = [];
        var currentActiveCameraId = '';

        const CCTV_PRESETS = {
            tapo_sub: 'rtsp://admin:password@192.168.8.96:554/stream2',
            tapo_main: 'rtsp://admin:password@192.168.8.96:554/stream1',
            hikvision: 'rtsp://admin:password@192.168.1.100:554/Streaming/Channels/101',
            dahua: 'rtsp://admin:password@192.168.1.100:554/cam/realmonitor?channel=1&subtype=0',
            ezviz: 'rtsp://admin:VERIFICATION_CODE@192.168.1.100:554/h264/ch1/main/av_stream',
            xiongmai: 'rtsp://alijaya:060111@192.168.8.70:554/V_ENC_001',
            generic: 'rtsp://admin:password@192.168.1.100:554/live'
        };

        function applyCctvPreset(key) {
            const url = CCTV_PRESETS[key];
            if (url) {
                const targetInput = document.getElementById('newCamUrl') || document.getElementById('cfgCctvUrl');
                if (targetInput) targetInput.value = url;
                showToast('Format preset ' + key.toUpperCase() + ' diterapkan! Silakan sesuaikan IP, port, dan username/password kamera Anda.');
            }
        }

        function renderCctvCamerasList(activeId, afterPrayerCamId, scheduleCamId) {
            const container = document.getElementById('cctvCamerasListContainer');
            const quickSwitch = document.getElementById('cctvQuickSwitchContainer');
            const selectAfterPrayer = document.getElementById('cfgCctvAfterPrayerCamSelect');
            const selectSchedule = document.getElementById('cfgCctvScheduleCamSelect');

            if (activeId !== undefined && activeId !== '') currentActiveCameraId = activeId;

            // 1. Populate Dropdown Selects for Automations & Background
            const updateDropdown = function(selectEl, selectedVal) {
                if (!selectEl) return;
                selectEl.innerHTML = '<option value="">(Kamera Aktif / Otomatis)</option>';
                localCctvCameras.forEach(function(cam, idx) {
                    const opt = document.createElement('option');
                    opt.value = cam.id;
                    opt.textContent = (idx + 1) + '. ' + cam.name + (cam.isEnabled ? '' : ' [Nonaktif]');
                    if (cam.id === selectedVal) opt.selected = true;
                    selectEl.appendChild(opt);
                });
            };
            if (selectAfterPrayer) updateDropdown(selectAfterPrayer, afterPrayerCamId);
            if (selectSchedule) updateDropdown(selectSchedule, scheduleCamId);
            const selectBgCctv = document.getElementById('cfgBackgroundCctvCamSelect');
            if (selectBgCctv) updateDropdown(selectBgCctv, window.lastBgCctvCamId);

            // 2. Populate Quick Switch Buttons in Header
            if (quickSwitch) {
                quickSwitch.innerHTML = '';
                const enabledCams = localCctvCameras.filter(function(c) { return c.isEnabled; });
                if (enabledCams.length === 0) {
                    quickSwitch.innerHTML = '<span style="font-size: 11.5px; color: #94a3b8;">Tambahkan kamera di bawah untuk mengaktifkan Quick-Switch 1-klik.</span>';
                } else {
                    enabledCams.forEach(function(cam, idx) {
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        const isActive = cam.id === currentActiveCameraId;
                        btn.className = 'btn-secondary';
                        btn.style.cssText = 'padding: 6px 12px; font-size: 12px; font-weight: 700; border-radius: 8px; cursor: pointer; transition: all 0.2s; ' +
                            (isActive
                                ? 'background: #0284c7; color: #ffffff; border: 1.5px solid #38bdf8; box-shadow: 0 0 10px rgba(56,189,248,0.5);'
                                : 'background: rgba(255,255,255,0.08); color: #e2e8f0; border: 1px solid rgba(255,255,255,0.2);');
                        btn.innerHTML = (isActive ? '🔴 ' : '📹 ') + (idx + 1) + '. ' + cam.name;
                        btn.onclick = function() {
                            switchCctvCamera(cam.id);
                        };
                        quickSwitch.appendChild(btn);
                    });
                }
            }

            // 3. Render Camera Management List
            if (!container) return;
            container.innerHTML = '';
            if (localCctvCameras.length === 0) {
                container.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px;">Belum ada kamera CCTV yang didaftarkan. Tambahkan kamera pertama Anda di formulir bawah ini.</div>';
                return;
            }

            localCctvCameras.forEach(function(cam, index) {
                const isActive = cam.id === currentActiveCameraId;
                const card = document.createElement('div');
                card.style.cssText = 'background: ' + (isActive ? '#f0f9ff' : '#ffffff') + '; border: 1.5px solid ' + (isActive ? '#0284c7' : '#e2e8f0') + '; border-radius: 10px; padding: 12px; transition: all 0.2s;';

                let cardHtml = '<div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; margin-bottom: 8px;">';
                cardHtml += '<div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">';
                cardHtml += '<span style="font-weight: 800; font-size: 14px; color: ' + (isActive ? '#0369a1' : '#0f172a') + ';">#' + (index + 1) + '. ' + cam.name + '</span>';
                if (isActive) cardHtml += '<span style="font-size: 10px; font-weight: 800; background: #0284c7; color: #fff; padding: 2px 8px; border-radius: 12px;">AKTIF DI TV</span>';
                if (!cam.isEnabled) cardHtml += '<span style="font-size: 10px; font-weight: 700; background: #fee2e2; color: #b91c1c; padding: 2px 8px; border-radius: 12px;">DINONAKTIFKAN</span>';
                cardHtml += '</div>';
                cardHtml += '<div style="display: flex; gap: 6px;">';
                if (!isActive) cardHtml += '<button type="button" class="btn-secondary" style="padding: 4px 10px; font-size: 11.5px; background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd; font-weight: 700; border-radius: 6px;" onclick="switchCctvCamera(\'' + cam.id + '\')">📺 Tayangkan</button>';
                cardHtml += '<button type="button" class="btn-secondary" style="padding: 4px 8px; font-size: 11.5px; background: #fee2e2; color: #dc2626; border: 1px solid #fecaca; font-weight: 700; border-radius: 6px;" onclick="deleteCameraItem(\'' + cam.id + '\')">🗑️ Hapus</button>';
                cardHtml += '</div></div>';
                cardHtml += '<div style="font-family: monospace; font-size: 11.5px; color: #64748b; margin-bottom: 10px; word-break: break-all; background: #f8fafc; padding: 6px 10px; border-radius: 6px; border: 1px solid #e2e8f0;">' + cam.streamUrl + '</div>';
                cardHtml += '<div style="display: flex; gap: 14px; flex-wrap: wrap; font-size: 12px; color: #475569;">';
                cardHtml += '<label style="display: flex; align-items: center; gap: 5px; cursor: pointer;"><input type="checkbox" ' + (cam.isEnabled ? 'checked' : '') + ' onchange="updateCameraField(\'' + cam.id + '\', \'isEnabled\', this.checked)"><span>Aktifkan Kamera</span></label>';
                cardHtml += '<label style="display: flex; align-items: center; gap: 5px; cursor: pointer;"><input type="checkbox" ' + (cam.isMuted ? 'checked' : '') + ' onchange="updateCameraField(\'' + cam.id + '\', \'isMuted\', this.checked)"><span>🔇 Mute Suara</span></label>';
                cardHtml += '<label style="display: flex; align-items: center; gap: 5px; cursor: pointer;"><input type="checkbox" ' + (cam.showSlide ? 'checked' : '') + ' onchange="updateCameraField(\'' + cam.id + '\', \'showSlide\', this.checked)"><span>🎞️ Tampil Slide TV</span></label>';
                cardHtml += '</div>';

                card.innerHTML = cardHtml;
                container.appendChild(card);
            });
        }

        function updateCameraField(camId, field, value) {
            const cam = localCctvCameras.find(function(c) { return c.id === camId; });
            if (cam) {
                cam[field] = value;
                renderCctvCamerasList();
            }
        }

        function addCameraItem() {
            const nameInput = document.getElementById('newCamName');
            const urlInput = document.getElementById('newCamUrl');
            const mutedInput = document.getElementById('newCamMuted');
            const slideInput = document.getElementById('newCamShowSlide');

            const name = (nameInput ? nameInput.value.trim() : '') || ('Kamera ' + (localCctvCameras.length + 1));
            const url = urlInput ? urlInput.value.trim() : '';

            if (!url) {
                showToast('Harap masukkan URL RTSP Kamera!');
                if (urlInput) urlInput.focus();
                return;
            }

            const newId = 'cam_' + Date.now();
            const newCam = {
                id: newId,
                name: name,
                streamUrl: url,
                isMuted: mutedInput ? mutedInput.checked : true,
                showSlide: slideInput ? slideInput.checked : false,
                isEnabled: true
            };

            localCctvCameras.push(newCam);
            if (!currentActiveCameraId || localCctvCameras.length === 1) {
                currentActiveCameraId = newId;
            }

            if (nameInput) nameInput.value = '';
            if (urlInput) urlInput.value = '';

            renderCctvCamerasList();
            showToast('Kamera "' + name + '" ditambahkan ke daftar. Jangan lupa klik "Simpan Pengaturan Multi-Kamera"!');
        }

        function deleteCameraItem(id) {
            const cam = localCctvCameras.find(function(c) { return c.id === id; });
            const camName = cam ? cam.name : 'Kamera';
            if (!confirm('Hapus ' + camName + ' dari daftar?')) return;

            localCctvCameras = localCctvCameras.filter(function(c) { return c.id !== id; });
            if (currentActiveCameraId === id) {
                currentActiveCameraId = localCctvCameras.length > 0 ? localCctvCameras[0].id : '';
            }
            renderCctvCamerasList();
            showToast(camName + ' dihapus dari daftar.');
        }

        async function switchCctvCamera(id) {
            try {
                const res = await fetch('/api/switch-cctv-camera', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ cameraId: id }).toString()
                });
                const resData = await res.json();
                currentActiveCameraId = id;
                showToast(resData.message || 'TV beralih ke kamera yang dipilih!');
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal beralih kamera');
            }
        }

        async function saveAllCctvCameras() {
            const displayLayoutEl = document.getElementById('cfgCctvDisplayLayout');
            const autoRotateEl = document.getElementById('cfgCctvAutoRotateEnabled');
            const autoRotateIntEl = document.getElementById('cfgCctvAutoRotateInterval');

            const payload = {
                camerasJson: JSON.stringify(localCctvCameras),
                activeCameraId: currentActiveCameraId || (localCctvCameras.length > 0 ? localCctvCameras[0].id : ''),
                autoRotate: autoRotateEl ? autoRotateEl.checked : false,
                autoRotateInterval: autoRotateIntEl ? (parseInt(autoRotateIntEl.value) || 30) : 30,
                displayLayout: displayLayoutEl ? displayLayoutEl.value : 'SINGLE'
            };

            try {
                const res = await fetch('/api/save-cctv-cameras', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(payload).toString()
                });
                const resData = await res.json();
                showToast(resData.message || 'Daftar kamera & layout berhasil disimpan!');
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal menyimpan daftar kamera');
            }
        }

        async function saveCctvScheduleForm(e) {
            if (e) e.preventDefault();

            const autoPlayPrayerEl = document.getElementById('cfgCctvAutoPlayAfterPrayer');
            const durationEl = document.getElementById('cfgCctvAfterPrayerDuration');
            const prayerCamEl = document.getElementById('cfgCctvAfterPrayerCamSelect');

            const scheduleEnabledEl = document.getElementById('cfgCctvScheduleEnabled');
            const startTimeEl = document.getElementById('cfgCctvScheduleStartTime');
            const endTimeEl = document.getElementById('cfgCctvScheduleEndTime');
            const daysEl = document.getElementById('cfgCctvScheduleDays');
            const scheduleCamEl = document.getElementById('cfgCctvScheduleCamSelect');

            const selectedPrayers = [];
            if (document.getElementById('chkPraySubuh') && document.getElementById('chkPraySubuh').checked) selectedPrayers.push('SUBUH');
            if (document.getElementById('chkPrayDzuhur') && document.getElementById('chkPrayDzuhur').checked) selectedPrayers.push('DZUHUR');
            if (document.getElementById('chkPrayAshar') && document.getElementById('chkPrayAshar').checked) selectedPrayers.push('ASHAR');
            if (document.getElementById('chkPrayMaghrib') && document.getElementById('chkPrayMaghrib').checked) selectedPrayers.push('MAGHRIB');
            if (document.getElementById('chkPrayIsya') && document.getElementById('chkPrayIsya').checked) selectedPrayers.push('ISYA');
            if (document.getElementById('chkPrayJumat') && document.getElementById('chkPrayJumat').checked) selectedPrayers.push('JUMAT');

            const payload = {
                autoPlayAfterPrayer: autoPlayPrayerEl ? autoPlayPrayerEl.checked : false,
                afterPrayerDuration: durationEl ? (parseInt(durationEl.value) || 30) : 30,
                afterPrayerWaktu: selectedPrayers.join(','),
                afterPrayerCameraId: prayerCamEl ? prayerCamEl.value : '',
                scheduleEnabled: scheduleEnabledEl ? scheduleEnabledEl.checked : false,
                scheduleStartTime: startTimeEl ? startTimeEl.value : '18:30',
                scheduleEndTime: endTimeEl ? endTimeEl.value : '19:30',
                scheduleDays: daysEl ? daysEl.value : 'ALL',
                scheduleCameraId: scheduleCamEl ? scheduleCamEl.value : ''
            };

            try {
                const res = await fetch('/api/save-cctv-schedule', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(payload).toString()
                });
                const resData = await res.json();
                showToast(resData.message || 'Jadwal tayang otomatis CCTV berhasil disimpan!');
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal menyimpan jadwal otomatis CCTV');
            }
        }

        async function playFullscreenCctvNow() {
            let activeCam = localCctvCameras.find(function(c) { return c.id === currentActiveCameraId; });
            if (!activeCam && localCctvCameras.length > 0) activeCam = localCctvCameras[0];

            const url = activeCam ? activeCam.streamUrl : ((document.getElementById('newCamUrl') && document.getElementById('newCamUrl').value.trim()) || '');
            const title = activeCam ? activeCam.name : 'Siaran Langsung CCTV Masjid';
            const muted = activeCam ? activeCam.isMuted : true;

            if (!url) {
                showToast('Silakan tambahkan atau pilih kamera CCTV terlebih dahulu!');
                return;
            }
            try {
                const res = await fetch('/api/play-fullscreen-cctv', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ url: url, title: title, muted: muted }).toString()
                });
                const resData = await res.json();
                showToast(resData.message || 'CCTV ditayangkan di TV!');
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal menayangkan CCTV di TV');
            }
        }

        async function stopFullscreenCctvNow() {
            try {
                const res = await fetch('/api/stop-fullscreen-cctv', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
                });
                const resData = await res.json();
                showToast(resData.message || 'Tayangan CCTV dihentikan');
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal menghentikan tayangan CCTV');
            }
        }

        function renderYoutubeData(data) {
            const cfg = data.config || {};
            allYoutubePresets = data.youtubePresets || [];

            // 0. Parse & Initialize CCTV Multi-Camera Data
            try {
                localCctvCameras = JSON.parse(cfg.cctvCamerasJson || '[]');
            } catch(e) {
                localCctvCameras = [];
            }
            if (localCctvCameras.length === 0 && cfg.cctvStreamUrl) {
                localCctvCameras = [{
                    id: 'cam_default',
                    name: cfg.cctvStreamTitle || 'Kamera Utama Masjid',
                    streamUrl: cfg.cctvStreamUrl,
                    isMuted: cfg.cctvStreamMuted !== false,
                    showSlide: cfg.showCctvSlide === true,
                    isEnabled: true
                }];
            }
            currentActiveCameraId = cfg.cctvActiveCameraId || (localCctvCameras.length > 0 ? localCctvCameras[0].id : '');

            // 0.1 CCTV Status Badge & Info
            const cctvBadge = document.getElementById('cctvStatusBadge');
            const cctvTitleEl = document.getElementById('cctvNowTitle');
            const cctvUrlEl = document.getElementById('cctvNowUrl');
            const btnResetBgCctv = document.getElementById('btnResetBgCctv');

            window.lastBgCctvCamId = cfg.backgroundCctvCameraId || '';
            const isBgCctv = cfg.backgroundType === 'CCTV';
            if (btnResetBgCctv) btnResetBgCctv.style.display = isBgCctv ? 'block' : 'none';

            const activeCamObj = localCctvCameras.find(function(c) { return c.id === currentActiveCameraId; }) || (localCctvCameras.length > 0 ? localCctvCameras[0] : null);

            if (isBgCctv && activeCamObj) {
                if (cctvBadge) {
                    cctvBadge.innerText = '🟢 LIVE BACKGROUND TV';
                    cctvBadge.style.background = 'rgba(16, 185, 129, 0.25)';
                    cctvBadge.style.borderColor = '#10b981';
                    cctvBadge.style.color = '#6ee7b7';
                }
                if (cctvTitleEl) cctvTitleEl.innerText = activeCamObj.name + ' (Background TV)';
                if (cctvUrlEl) cctvUrlEl.innerText = activeCamObj.streamUrl;
            } else if (cfg.cctvStreamEnabled && activeCamObj) {
                if (cctvBadge) {
                    cctvBadge.innerText = '🔴 CCTV LIVE DI TV (FULLSCREEN)';
                    cctvBadge.style.background = 'rgba(2, 132, 199, 0.3)';
                    cctvBadge.style.borderColor = '#0284c7';
                    cctvBadge.style.color = '#7dd3fc';
                }
                if (cctvTitleEl) cctvTitleEl.innerText = activeCamObj.name;
                if (cctvUrlEl) cctvUrlEl.innerText = activeCamObj.streamUrl;
            } else {
                if (cctvBadge) {
                    cctvBadge.innerText = 'STANDBY NONAKTIF';
                    cctvBadge.style.background = 'rgba(100, 116, 139, 0.2)';
                    cctvBadge.style.borderColor = '#64748b';
                    cctvBadge.style.color = '#94a3b8';
                }
                if (cctvTitleEl) cctvTitleEl.innerText = activeCamObj ? ('Siaga: ' + activeCamObj.name) : 'Belum ada CCTV yang ditayangkan';
                if (cctvUrlEl) cctvUrlEl.innerText = activeCamObj ? activeCamObj.streamUrl : 'Tambahkan URL RTSP kamera CCTV di formulir bawah.';
            }

            // 0.2 Populate Layout & Auto-Rotate Controls
            if (document.getElementById('cfgCctvDisplayLayout')) {
                document.getElementById('cfgCctvDisplayLayout').value = cfg.cctvDisplayLayout || 'SINGLE';
            }
            if (document.getElementById('cfgCctvAutoRotateEnabled')) {
                document.getElementById('cfgCctvAutoRotateEnabled').checked = cfg.cctvAutoRotateEnabled === true;
            }
            if (document.getElementById('cfgCctvAutoRotateInterval')) {
                document.getElementById('cfgCctvAutoRotateInterval').value = cfg.cctvAutoRotateIntervalSeconds || 30;
            }

            // 0.3 Populate Ba'da Sholat Schedule
            if (document.getElementById('cfgCctvAutoPlayAfterPrayer')) {
                document.getElementById('cfgCctvAutoPlayAfterPrayer').checked = cfg.cctvAutoPlayAfterPrayer === true;
            }
            if (document.getElementById('cfgCctvAfterPrayerDuration')) {
                document.getElementById('cfgCctvAfterPrayerDuration').value = cfg.cctvAfterPrayerDurationMinutes || 30;
            }
            const waktuArr = (cfg.cctvAfterPrayerWaktu || 'SUBUH,MAGHRIB,ISYA,JUMAT').toUpperCase().split(',').map(function(s) { return s.trim(); });
            ['Subuh', 'Dzuhur', 'Ashar', 'Maghrib', 'Isya', 'Jumat'].forEach(function(w) {
                const chk = document.getElementById('chkPray' + w);
                if (chk) chk.checked = waktuArr.includes(w.toUpperCase());
            });

            // 0.4 Populate Manual Time Schedule
            if (document.getElementById('cfgCctvScheduleEnabled')) {
                document.getElementById('cfgCctvScheduleEnabled').checked = cfg.cctvScheduleEnabled === true;
            }
            if (document.getElementById('cfgCctvScheduleStartTime')) {
                document.getElementById('cfgCctvScheduleStartTime').value = cfg.cctvScheduleStartTime || '18:30';
            }
            if (document.getElementById('cfgCctvScheduleEndTime')) {
                document.getElementById('cfgCctvScheduleEndTime').value = cfg.cctvScheduleEndTime || '19:30';
            }
            if (document.getElementById('cfgCctvScheduleDays')) {
                document.getElementById('cfgCctvScheduleDays').value = cfg.cctvScheduleDays || 'ALL';
            }

            // 0.5 Render Camera List & Quick Switch
            renderCctvCamerasList(currentActiveCameraId, cfg.cctvAfterPrayerCameraId, cfg.cctvScheduleCameraId);

            // 1. YouTube Live Status Card
            const badge = document.getElementById('youtubeLiveStatusBadge');
            const titleEl = document.getElementById('youtubeLiveNowTitle');
            const urlEl = document.getElementById('youtubeLiveNowUrl');

            if (cfg.youtubeLiveEnabled && cfg.youtubeLiveUrl) {
                if (badge) {
                    badge.innerText = '🔴 LIVE DI LAYAR TV';
                    badge.style.background = 'rgba(239, 68, 68, 0.3)';
                    badge.style.borderColor = '#ef4444';
                    badge.style.color = '#fca5a5';
                }
                if (titleEl) titleEl.innerText = cfg.youtubeLiveTitle || 'Siaran Langsung Masjid';
                if (urlEl) urlEl.innerText = cfg.youtubeLiveUrl;
            } else {
                if (badge) {
                    badge.innerText = 'STANDBY NONAKTIF';
                    badge.style.background = 'rgba(107, 114, 128, 0.2)';
                    badge.style.borderColor = '#6b7280';
                    badge.style.color = '#d1d5db';
                }
                if (titleEl) titleEl.innerText = 'Tidak ada siaran live yang aktif';
                if (urlEl) urlEl.innerText = cfg.youtubeLiveUrl ? ('Tautan standby: ' + cfg.youtubeLiveUrl) : 'Masukkan tautan YouTube Live di bawah.';
            }

            // 2. YouTube Form values
            if (document.getElementById('cfgYoutubeUrl')) {
                document.getElementById('cfgYoutubeUrl').value = cfg.youtubeLiveUrl || '';
                document.getElementById('cfgYoutubeTitle').value = cfg.youtubeLiveTitle || '';
                document.getElementById('cfgYoutubeLiveEnabled').checked = cfg.youtubeLiveEnabled === true;
                document.getElementById('cfgYoutubeLiveMuted').checked = cfg.youtubeLiveMuted === true;
                document.getElementById('cfgShowYoutubeLiveSlide').checked = cfg.showYoutubeLiveSlide === true;
            }

            // 3. Render Preset Buttons
            const presetsContainer = document.getElementById('youtubePresetsContainer');
            if (presetsContainer && allYoutubePresets.length > 0) {
                presetsContainer.innerHTML = '';
                allYoutubePresets.forEach(function(yp) {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'btn-secondary';
                    btn.style.padding = '12px';
                    btn.style.textAlign = 'left';
                    btn.style.background = '#fafcfb';
                    btn.style.border = '1.5px solid #d1d5db';
                    btn.style.borderRadius = '10px';
                    btn.onclick = function() {
                        document.getElementById('cfgYoutubeUrl').value = yp.url;
                        document.getElementById('cfgYoutubeTitle').value = yp.name;
                        showToast('Preset "' + yp.name + '" dipilih!');
                    };
                    btn.innerHTML = '<div style="font-weight:800; color:#111827; font-size:13px;">📺 ' + yp.name + '</div>' +
                                    '<div style="font-size:11.5px; color:#6b7280; margin-top:2px;">' + (yp.description || yp.channelName) + '</div>';
                    presetsContainer.appendChild(btn);
                });
            }
        }

        async function saveYoutubeLive(e) {
            if (e) e.preventDefault();
            const payload = {
                url: document.getElementById('cfgYoutubeUrl').value.trim(),
                title: document.getElementById('cfgYoutubeTitle').value.trim() || 'Live Streaming Masjid',
                enabled: document.getElementById('cfgYoutubeLiveEnabled').checked,
                muted: document.getElementById('cfgYoutubeLiveMuted').checked,
                showSlide: document.getElementById('cfgShowYoutubeLiveSlide').checked
            };
            try {
                const res = await fetch('/api/save-youtube-live', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(payload).toString()
                });
                const resData = await res.json();
                showToast(resData.message || 'Pengaturan YouTube Live berhasil disimpan!');
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal menyimpan pengaturan YouTube Live');
            }
        }

        async function toggleYoutubeLiveNow(enabled) {
            try {
                const res = await fetch('/api/toggle-youtube-live', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ enabled: enabled }).toString()
                });
                const resData = await res.json();
                showToast(resData.message || (enabled ? 'Live Streaming TV Aktif' : 'Live Streaming Dimatikan'));
                loadStatus();
            } catch (err) {
                console.error(err);
                showToast('Gagal mengubah status live streaming');
            }
        }

        function applyYoutubeLivePreset(presetId) {
            const found = allYoutubePresets.find(p => p.id === presetId);
            if (found) {
                document.getElementById('cfgYoutubeUrl').value = found.url;
                document.getElementById('cfgYoutubeTitle').value = found.name;
                showToast('Preset "' + found.name + '" dipilih!');
            }
        }

        // ==================== MUROTTAL & QARI SECTION ====================
        var currentMurottalSource = 'PRESET';
        var allMurottalPresets = [];
        var activeMurottalCatFilter = 'Semua';

        function setMurottalSource(type) {
            currentMurottalSource = type;
            const btnPreset = document.getElementById('btnSourcePreset');
            const btnUpload = document.getElementById('btnSourceUpload');
            const sectionPreset = document.getElementById('sectionPresetPicker');

            if (type === 'PRESET') {
                btnPreset.classList.add('active');
                btnUpload.classList.remove('active');
                sectionPreset.style.display = 'block';
            } else {
                btnPreset.classList.remove('active');
                btnUpload.classList.add('active');
                sectionPreset.style.display = 'none';
            }
        }

        function selectMurottalCatFilter(cat, btn) {
            activeMurottalCatFilter = cat;
            document.querySelectorAll('#murottalCategoryChips .chip-btn').forEach(b => {
                b.style.background = '';
                b.style.color = '';
            });
            if (btn) {
                btn.style.background = '#065f46';
                btn.style.color = '#fff';
            }
            filterMurottalPresets();
        }

        function filterMurottalPresets() {
            const query = (document.getElementById('murottalSearchInput')?.value || '').toLowerCase().trim();
            const qariFilter = (document.getElementById('murottalQariFilter')?.value || '').trim();
            const grid = document.getElementById('murottalPresetsGrid');
            if (!grid) return;

            const filtered = allMurottalPresets.filter(p => {
                const matchCat = activeMurottalCatFilter === 'Semua' || p.category === activeMurottalCatFilter;
                const matchQari = !qariFilter || p.qariName === qariFilter;
                const matchSearch = !query ||
                    p.surahName.toLowerCase().includes(query) ||
                    p.qariName.toLowerCase().includes(query) ||
                    (p.description || '').toLowerCase().includes(query);
                return matchCat && matchQari && matchSearch;
            });

            if (filtered.length === 0) {
                grid.innerHTML = '<div style="text-align:center; padding:20px; color:#6b7280; font-size:12px;">Tidak ditemukan murottal yang sesuai kriteria pencarian.</div>';
                return;
            }

            const currentActiveId = document.getElementById('murottalSelectedPresetId')?.value || '';
            grid.innerHTML = '';

            filtered.forEach(p => {
                const isSelected = p.id === currentActiveId;
                const card = document.createElement('div');
                card.style.background = isSelected ? '#ecfdf5' : '#ffffff';
                card.style.border = isSelected ? '2px solid #059669' : '1px solid #e5e7eb';
                card.style.borderRadius = '10px';
                card.style.padding = '10px 12px';
                card.style.display = 'flex';
                card.style.alignItems = 'center';
                card.style.justifyContent = 'space-between';
                card.style.gap = '10px';
                card.style.transition = 'all 0.2s';

                const catBadgeColor = p.category.includes('Subuh') ? '#d97706' : (p.category.includes('Juz') ? '#2563eb' : (p.category.includes('Shalawat') ? '#7c3aed' : '#059669'));
                const activeBadge = isSelected ? '<span style="background:#059669; color:#fff; font-size:10px; padding:2px 7px; border-radius:10px; font-weight:700; margin-left:6px;">🟢 AKTIF DI TV</span>' : '';

                card.innerHTML = '<div style="flex:1; min-width:0;">' +
                    '<div style="display:flex; align-items:center; gap:6px; margin-bottom:3px; flex-wrap:wrap;">' +
                    '<span style="font-weight:800; font-size:13px; color:#111827;">' + p.surahName + '</span>' +
                    '<span style="background:' + catBadgeColor + '18; color:' + catBadgeColor + '; font-size:10px; font-weight:700; padding:1px 6px; border-radius:6px; border:1px solid ' + catBadgeColor + '40;">' + p.category + '</span>' +
                    activeBadge +
                    '</div>' +
                    '<div style="font-size:11.5px; color:#4b5563;">🎙️ <strong>' + p.qariName + '</strong> &bull; ⏱️ ' + p.durationText + '</div>' +
                    '<div style="font-size:11px; color:#6b7280; margin-top:2px; text-overflow:ellipsis; overflow:hidden; white-space:nowrap;">' + (p.description || '') + '</div>' +
                    '</div>' +
                    '<div style="display:flex; gap:6px; align-items:center; flex-shrink:0;">' +
                    '<button type="button" class="btn-secondary" style="padding:6px 10px; font-size:11px;" onclick="previewMurottalPresetHp(\'' + p.audioUrl + '\', \'' + p.surahName + ' - ' + p.qariName + '\')" title="Dengarkan di HP">🎵 HP</button>' +
                    '<button type="button" class="btn-primary" style="padding:6px 10px; font-size:11px; background:#10b981; width:auto;" onclick="playPresetOnTV(\'' + p.id + '\')" title="Putar Sekarang di TV">▶️ TV</button>' +
                    '<button type="button" class="btn-primary" style="padding:6px 10px; font-size:11px; background:' + (isSelected ? '#059669' : '#0284c7') + '; width:auto;" onclick="selectMurottalPreset(\'' + p.id + '\')" title="Jadikan Audio Utama TV">' + (isSelected ? '✅ Aktif' : '⭐ Pasang') + '</button>' +
                    '</div>';
                grid.appendChild(card);
            });
        }

        function previewMurottalPresetHp(url, title) {
            const box = document.getElementById('hpAudioPreviewBox');
            const player = document.getElementById('hpPreviewAudioPlayer');
            const titleEl = document.getElementById('hpPreviewTitle');
            if (box && player) {
                box.style.display = 'block';
                if (titleEl) titleEl.innerText = '🎵 Sedang Mendengarkan: ' + title;
                player.src = url;
                player.play().catch(e => console.log('Autoplay blocked', e));
            }
        }

        function closeHpPreview() {
            const box = document.getElementById('hpAudioPreviewBox');
            const player = document.getElementById('hpPreviewAudioPlayer');
            if (player) {
                player.pause();
                player.src = '';
            }
            if (box) box.style.display = 'none';
        }

        async function selectMurottalPreset(presetId) {
            try {
                const res = await fetch('/api/select-murottal-preset', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ presetId: presetId }).toString()
                });
                const resData = await res.json();
                if (resData.status === 'ok') {
                    showToast(resData.message || 'Preset murottal berhasil diaktifkan di TV!');
                    loadStatus();
                } else {
                    showToast(resData.message || 'Gagal memilih preset');
                }
            } catch (err) {
                console.error(err);
                showToast('Gagal terhubung ke TV');
            }
        }

        async function playPresetOnTV(presetId) {
            const vol = parseInt(document.getElementById('liveMurottalVolSlider')?.value) || 80;
            try {
                const res = await fetch('/api/play-murottal', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ presetId: presetId, volume: vol }).toString()
                });
                const data = await res.json();
                showToast(data.message || 'Memutar murottal di TV');
                setTimeout(loadStatus, 500);
            } catch (e) {
                console.error(e);
                showToast('Gagal memutar murottal di TV');
            }
        }

        function onPresetChange(presetId) {
            const found = allMurottalPresets.find(p => p.id === presetId);
            const descBox = document.getElementById('presetDescBox');
            if (found && descBox) {
                descBox.innerHTML = '🎙️ <strong>' + found.qariName + '</strong> &bull; Surah: <strong>' + found.surahName + '</strong><br>' +
                                    '<span style="color:#059669;">⏱️ Durasi: ' + found.durationText + '</span> &bull; <i>' + (found.description || '') + '</i>';
            }
            filterMurottalPresets();
        }

        function renderMurottalData(data) {
            const cfg = data.config || {};
            const pb = data.murottalPlayback || {};
            allMurottalPresets = data.murottalPresets || [];

            // 1. Live player status
            const badge = document.getElementById('murottalLiveBadge');
            const titleEl = document.getElementById('murottalNowPlayingTitle');
            const qariEl = document.getElementById('murottalNowPlayingQari');
            const volSlider = document.getElementById('liveMurottalVolSlider');
            const volLabel = document.getElementById('liveMurottalVolLabel');

            if (pb.isPlaying) {
                if (badge) {
                    badge.innerText = '🔴 SEDANG MEMUTAR';
                    badge.style.background = 'rgba(239, 68, 68, 0.25)';
                    badge.style.borderColor = '#ef4444';
                    badge.style.color = '#fca5a5';
                }
                if (titleEl) titleEl.innerText = pb.title || 'Murottal Al-Qur\'an';
                if (qariEl) qariEl.innerText = (pb.qari ? ('🎙️ ' + pb.qari + ' • ') : '') + (pb.surah || '');
            } else {
                if (badge) {
                    badge.innerText = 'STANDBY';
                    badge.style.background = 'rgba(16, 185, 129, 0.2)';
                    badge.style.borderColor = '#10b981';
                    badge.style.color = '#6ee7b7';
                }
                if (titleEl) titleEl.innerText = 'Tidak ada audio yang diputar';
                if (qariEl) qariEl.innerText = 'Pemutar murottal TV dalam kondisi standby';
            }

            if (volSlider && !volSlider.matches(':active')) {
                volSlider.value = pb.volumePercent || cfg.murottalVolume || 80;
                if (volLabel) volLabel.innerText = (pb.volumePercent || cfg.murottalVolume || 80) + '%';
            }

            // 2. Config form
            if (document.getElementById('murottalEnabled')) document.getElementById('murottalEnabled').checked = cfg.murottalEnabled !== false;
            if (document.getElementById('murottalSubuh')) document.getElementById('murottalSubuh').checked = cfg.murottalSubuh !== false;
            if (document.getElementById('murottalDzuhur')) document.getElementById('murottalDzuhur').checked = cfg.murottalDzuhur !== false;
            if (document.getElementById('murottalAshar')) document.getElementById('murottalAshar').checked = cfg.murottalAshar !== false;
            if (document.getElementById('murottalMaghrib')) document.getElementById('murottalMaghrib').checked = cfg.murottalMaghrib !== false;
            if (document.getElementById('murottalIsya')) document.getElementById('murottalIsya').checked = cfg.murottalIsya !== false;
            if (document.getElementById('murottalJumat')) document.getElementById('murottalJumat').checked = cfg.murottalJumat !== false;

            if (document.getElementById('murottalDurationMinutes')) document.getElementById('murottalDurationMinutes').value = (cfg.murottalDurationMinutes || 10).toString();
            if (document.getElementById('murottalVolume')) document.getElementById('murottalVolume').value = (cfg.murottalVolume || 80).toString();

            setMurottalSource(cfg.murottalSourceType || 'PRESET');

            // Populate Qari Filter Dropdown
            const qariFilterSelect = document.getElementById('murottalQariFilter');
            if (qariFilterSelect && allMurottalPresets.length > 0) {
                const currentQari = qariFilterSelect.value;
                const uniqueQaris = Array.from(new Set(allMurottalPresets.map(p => p.qariName))).sort();
                qariFilterSelect.innerHTML = '<option value="">Semua Qari & Pelantun (' + uniqueQaris.length + ' Qari)</option>';
                uniqueQaris.forEach(q => {
                    const opt = document.createElement('option');
                    opt.value = q;
                    opt.innerText = q;
                    if (q === currentQari) opt.selected = true;
                    qariFilterSelect.appendChild(opt);
                });
            }

            // Populate presets dropdown in config
            const presetSelect = document.getElementById('murottalSelectedPresetId');
            if (presetSelect && allMurottalPresets.length > 0) {
                const currentVal = cfg.murottalSelectedPresetId || 'mishary_ar_rahman';
                presetSelect.innerHTML = '';
                allMurottalPresets.forEach(function(p) {
                    const opt = document.createElement('option');
                    opt.value = p.id;
                    opt.innerText = p.surahName + ' - ' + p.qariName + ' (' + p.durationText + ')';
                    if (p.id === currentVal) opt.selected = true;
                    presetSelect.appendChild(opt);
                });
                onPresetChange(presetSelect.value);
            }

            // Render rich presets catalogue
            filterMurottalPresets();

            // 3. Uploaded audio list
            const audioList = document.getElementById('murottalAudioList');
            if (audioList) {
                audioList.innerHTML = '';
                const audios = data.murottalAudios || [];
                if (audios.length === 0) {
                    audioList.innerHTML = '<li style="text-align: center; color: #9ca3af; padding: 14px; font-size: 12px;">Belum ada file audio manual yang di-upload ke TV.</li>';
                } else {
                    audios.forEach(function(a) {
                        const li = document.createElement('li');
                        li.className = 'item-row';
                        const pLabel = a.prayerTime === 'ALL' ? 'Semua Sholat' : a.prayerTime;
                        const defBadge = a.isDefault ? ' <span style="background:#059669; color:#fff; font-size:10px; padding:2px 7px; border-radius:10px; font-weight:700;">🟢 AKTIF DI TV</span>' : '';
                        const activateBtn = !a.isDefault ? '<button type="button" class="btn-primary" style="padding:5px 9px; font-size:11px; width:auto; background:#d97706;" onclick="setDefaultUploadedAudio(' + a.id + ')">⭐ Aktifkan</button>' : '';
                        li.innerHTML = '<div class="item-meta">' +
                            '<div class="item-title">🎧 ' + a.title + defBadge + '</div>' +
                            '<div class="item-sub">🎙️ ' + a.qari + ' &bull; Sholat: <strong>' + pLabel + '</strong></div>' +
                            '</div>' +
                            '<div style="display:flex; gap:6px; align-items:center;">' +
                            activateBtn +
                            '<button type="button" class="btn-primary" style="padding:5px 9px; font-size:11px; width:auto; background:#10b981;" onclick="playUploadedAudio(' + a.id + ')">▶️ TV</button>' +
                            '<button type="button" class="btn-danger" style="padding:5px 9px; font-size:11px;" onclick="deleteUploadedAudio(' + a.id + ')">🗑️</button>' +
                            '</div>';
                        audioList.appendChild(li);
                    });
                }
            }
        }

        async function saveMurottalConfig(e) {
            if (e) e.preventDefault();
            const payload = {
                murottalEnabled: document.getElementById('murottalEnabled').checked,
                murottalSubuh: document.getElementById('murottalSubuh').checked,
                murottalDzuhur: document.getElementById('murottalDzuhur').checked,
                murottalAshar: document.getElementById('murottalAshar').checked,
                murottalMaghrib: document.getElementById('murottalMaghrib').checked,
                murottalIsya: document.getElementById('murottalIsya').checked,
                murottalJumat: document.getElementById('murottalJumat').checked,
                murottalDurationMinutes: parseInt(document.getElementById('murottalDurationMinutes').value) || 10,
                murottalVolume: parseInt(document.getElementById('murottalVolume').value) || 80,
                murottalSourceType: currentMurottalSource,
                murottalSelectedPresetId: document.getElementById('murottalSelectedPresetId').value
            };
            try {
                const res = await fetch('/api/save-murottal', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(payload).toString()
                });
                const resData = await res.json();
                if (resData.status === 'ok') {
                    showToast('Pengaturan Murottal berhasil disimpan!');
                    loadStatus();
                } else {
                    showToast(resData.message || 'Gagal menyimpan pengaturan');
                }
            } catch (err) {
                console.error(err);
                showToast('Gagal terhubung ke TV');
            }
        }

        async function playMurottalNow() {
            const presetId = document.getElementById('murottalSelectedPresetId')?.value || '';
            const vol = parseInt(document.getElementById('liveMurottalVolSlider')?.value) || 80;
            try {
                const res = await fetch('/api/play-murottal', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ presetId: presetId, volume: vol }).toString()
                });
                const data = await res.json();
                showToast(data.message || 'Memutar murottal di TV');
                setTimeout(loadStatus, 500);
            } catch (e) {
                console.error(e);
                showToast('Gagal memutar murottal');
            }
        }

        async function playUploadedAudio(audioId) {
            const vol = parseInt(document.getElementById('liveMurottalVolSlider')?.value) || 80;
            try {
                const res = await fetch('/api/play-murottal', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ audioId: audioId, volume: vol }).toString()
                });
                const data = await res.json();
                showToast(data.message || 'Memutar audio di TV');
                setTimeout(loadStatus, 500);
            } catch (e) {
                console.error(e);
                showToast('Gagal memutar audio');
            }
        }

        async function setDefaultUploadedAudio(audioId) {
            try {
                const res = await fetch('/api/set-default-murottal?id=' + audioId, { method: 'POST' });
                const data = await res.json();
                showToast(data.message || 'Audio berhasil diaktifkan');
                setTimeout(loadStatus, 300);
            } catch (e) {
                console.error(e);
                showToast('Gagal mengaktifkan audio');
            }
        }

        async function stopMurottalNow() {
            try {
                const res = await fetch('/api/stop-murottal', { method: 'POST' });
                const data = await res.json();
                showToast(data.message || 'Murottal dihentikan');
                setTimeout(loadStatus, 300);
            } catch (e) {
                console.error(e);
                showToast('Gagal menghentikan pemutaran');
            }
        }

        function onLiveMurottalVolChange(val) {
            const volLabel = document.getElementById('liveMurottalVolLabel');
            if (volLabel) volLabel.innerText = val + '%';
        }

        function onAudioFileSelected(input) {
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const sizeMb = (file.size / (1024 * 1024)).toFixed(2);
            const info = document.getElementById('uploadFileInfo');
            if (info) {
                info.style.display = 'block';
                info.innerHTML = '🎵 <strong>File Siap Di-upload:</strong> ' + escapeHtml(file.name) + ' <span style="background:#059669; color:#fff; font-size:10px; padding:2px 6px; border-radius:10px; margin-left:6px;">' + sizeMb + ' MB</span>';
            }
            const surahInput = document.getElementById('uploadMurottalSurah');
            if (surahInput && !surahInput.value.trim()) {
                let cleanName = file.name.replace(/\.[^/.]+$/, "").replace(/[_-]/g, " ").trim();
                surahInput.value = cleanName;
            }
        }

        function uploadMurottalFile() {
            const fileInput = document.getElementById('uploadMurottalFile');
            if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
                alert('Silakan pilih file audio (MP3 / WAV / M4A) terlebih dahulu!');
                return;
            }
            const file = fileInput.files[0];
            if (file.size > 120 * 1024 * 1024) {
                alert('Ukuran file terlalu besar! Maksimal ukuran audio adalah 120 MB.');
                return;
            }

            let surah = document.getElementById('uploadMurottalSurah').value.trim();
            if (!surah) {
                surah = file.name.replace(/\.[^/.]+$/, "").replace(/[_-]/g, " ").trim() || 'Murottal Al-Qur\'an';
            }
            const qari = document.getElementById('uploadMurottalQari').value.trim() || 'Qari Pilihan';
            const prayer = document.getElementById('uploadMurottalPrayer').value || 'ALL';
            const title = surah + ' (' + qari + ')';

            const btn = document.getElementById('btnSubmitUploadMurottal');
            const progressContainer = document.getElementById('murottalUploadProgressContainer');
            const progressBar = document.getElementById('murottalUploadProgressBar');
            const progressLabel = document.getElementById('murottalUploadProgressLabel');
            const progressPercent = document.getElementById('murottalUploadProgressPercent');

            btn.disabled = true;
            btn.innerText = '⏳ Mengunggah Audio ke TV...';
            if (progressContainer) progressContainer.style.display = 'block';
            if (progressBar) progressBar.style.width = '0%';
            if (progressPercent) progressPercent.innerText = '0%';
            if (progressLabel) progressLabel.innerText = 'Mengunggah (' + (file.size / (1024 * 1024)).toFixed(1) + ' MB)...';

            const token = localStorage.getItem('masjidku_session_token') || '';
            const ext = file.name.split('.').pop().toLowerCase().trim() || 'mp3';
            const uploadUrl = '/api/upload-murottal?title=' + encodeURIComponent(title) +
                '&surah=' + encodeURIComponent(surah) +
                '&qari=' + encodeURIComponent(qari) +
                '&prayerTime=' + encodeURIComponent(prayer) +
                '&ext=' + encodeURIComponent(ext) +
                '&size=' + file.size +
                '&token=' + encodeURIComponent(token);

            const xhr = new XMLHttpRequest();
            xhr.open('POST', uploadUrl, true);
            xhr.timeout = 600000; // 10 menit
            xhr.setRequestHeader('X-Auth-Token', token);
            xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            xhr.setRequestHeader('X-Audio-Title', encodeURIComponent(title));
            xhr.setRequestHeader('X-Audio-Surah', encodeURIComponent(surah));
            xhr.setRequestHeader('X-Audio-Qari', encodeURIComponent(qari));
            xhr.setRequestHeader('X-Audio-Prayer', encodeURIComponent(prayer));
            xhr.setRequestHeader('X-Audio-Ext', ext);
            xhr.setRequestHeader('X-Audio-Size', file.size.toString());

            xhr.upload.onprogress = function (e) {
                if (e.lengthComputable) {
                    const percent = Math.round((e.loaded / e.total) * 100);
                    if (progressBar) progressBar.style.width = percent + '%';
                    if (progressPercent) progressPercent.innerText = percent + '%';
                    if (progressLabel) progressLabel.innerText = 'Mengunggah: ' + ((e.loaded / (1024 * 1024)).toFixed(1)) + ' / ' + ((e.total / (1024 * 1024)).toFixed(1)) + ' MB (' + percent + '%)';
                }
            };

            xhr.onload = function () {
                btn.disabled = false;
                btn.innerText = '🚀 Upload File Audio ke TV';
                setTimeout(() => {
                    if (progressContainer) progressContainer.style.display = 'none';
                    if (progressBar) progressBar.style.width = '0%';
                }, 2000);

                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const data = JSON.parse(xhr.responseText);
                        if (data.status === 'ok') {
                            showToast(data.message || 'File audio murottal berhasil diunggah!');
                            document.getElementById('uploadMurottalSurah').value = '';
                            document.getElementById('uploadMurottalQari').value = '';
                            document.getElementById('uploadMurottalFile').value = '';
                            const fInfo = document.getElementById('uploadFileInfo');
                            if (fInfo) fInfo.style.display = 'none';
                            setMurottalSource('MANUAL_UPLOAD');
                            loadStatus();
                        } else {
                            alert(data.message || 'Gagal mengunggah audio.');
                        }
                    } catch (e) {
                        showToast('File audio murottal berhasil diunggah!');
                        setMurottalSource('MANUAL_UPLOAD');
                        loadStatus();
                    }
                } else if (xhr.status === 401) {
                    alert('Sesi login berakhir. Silakan login kembali.');
                    showLoginSection();
                } else {
                    alert('Gagal mengunggah audio (HTTP ' + xhr.status + '). Periksa koneksi WiFi TV.');
                }
            };

            xhr.onerror = function () {
                btn.disabled = false;
                btn.innerText = '🚀 Upload File Audio ke TV';
                if (progressContainer) progressContainer.style.display = 'none';
                alert('Terjadi kesalahan jaringan saat mengunggah audio ke TV.');
            };

            xhr.ontimeout = function () {
                btn.disabled = false;
                btn.innerText = '🚀 Upload File Audio ke TV';
                if (progressContainer) progressContainer.style.display = 'none';
                alert('Upload waktu habis (Timeout). Pastikan sinyal WiFi stabil.');
            };

            xhr.send(file);
        }

        async function deleteUploadedAudio(id) {
            if (!confirm('Apakah Anda yakin ingin menghapus file audio ini dari TV?')) return;
            try {
                const res = await fetch('/api/delete-murottal', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ id: id }).toString()
                });
                showToast('File audio dihapus');
                loadStatus();
            } catch (e) {
                console.error(e);
                showToast('Gagal menghapus file audio');
            }
        }

        async function saveImamAndPreAdhan() {
            const payload = {
                enablePreAdhanCountdown: document.getElementById('enablePreAdhanCountdown').checked,
                preAdhanCountdownSeconds: parseInt(document.getElementById('preAdhanCountdownSeconds').value) || 30,
                imamSubuh: document.getElementById('imamSubuh').value,
                imamDzuhur: document.getElementById('imamDzuhur').value,
                imamAshar: document.getElementById('imamAshar').value,
                imamMaghrib: document.getElementById('imamMaghrib').value,
                imamIsya: document.getElementById('imamIsya').value,
                muadzinSubuh: document.getElementById('muadzinSubuh').value,
                muadzinDzuhur: document.getElementById('muadzinDzuhur').value,
                muadzinAshar: document.getElementById('muadzinAshar').value,
                muadzinMaghrib: document.getElementById('muadzinMaghrib').value,
                muadzinIsya: document.getElementById('muadzinIsya').value
            };
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify(payload) });
            showToast('Pengaturan Pra-Adzan & Imam Harian Berhasil Disimpan!');
            loadStatus();
        }

        async function addSpecialImamSchedule(e) {
            e.preventDefault();
            const date = document.getElementById('specialImamDate').value;
            const prayerName = document.getElementById('specialImamPrayer').value;
            const imamName = document.getElementById('specialImamName').value;
            const muadzinName = document.getElementById('specialMuadzinName').value;
            const notes = document.getElementById('specialImamNotes').value;

            if (!date || !imamName) {
                alert('Tanggal dan Nama Imam wajib diisi!');
                return;
            }

            const payload = { date, prayerName, imamName, muadzinName, notes };
            await fetch('/api/add-imam-schedule', { method: 'POST', body: JSON.stringify(payload) });
            showToast('Jadwal Imam Khusus Berhasil Ditambahkan!');
            document.getElementById('specialImamName').value = '';
            document.getElementById('specialMuadzinName').value = '';
            document.getElementById('specialImamNotes').value = '';
            loadStatus();
        }

        async function deleteSpecialImam(id) {
            if (confirm('Hapus jadwal imam khusus ini?')) {
                await fetch('/api/delete-imam-schedule', { method: 'POST', body: JSON.stringify({ id }) });
                showToast('Jadwal imam khusus dihapus');
                loadStatus();
            }
        }

        // --- RESIZE IMAGE UTILITY (CANVAS 1080p) ---
        function resizeImageToCanvas(file, maxWidth, maxHeight, callback) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const img = new Image();
                img.onload = function() {
                    let w = img.width;
                    let h = img.height;
                    if (w > maxWidth || h > maxHeight) {
                        const ratio = Math.min(maxWidth / w, maxHeight / h);
                        w = Math.round(w * ratio);
                        h = Math.round(h * ratio);
                    }
                    const canvas = document.createElement('canvas');
                    canvas.width = w;
                    canvas.height = h;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, w, h);
                    const dataUrl = canvas.toDataURL('image/jpeg', 0.88);
                    callback(dataUrl);
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        }

        // --- LOGO MASJID HANDLERS ---
        function previewLogoImage(input) {
            if (input.files && input.files[0]) {
                const file = input.files[0];
                const reader = new FileReader();
                reader.onload = function(e) {
                    document.getElementById('logoLocalPreviewImg').src = e.target.result;
                    document.getElementById('logoLocalPreviewContainer').style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        }

        async function uploadLogo() {
            const input = document.getElementById('inputLogoFile');
            if (!input.files || input.files.length === 0) {
                alert('Silakan pilih file logo terlebih dahulu!');
                return;
            }
            const file = input.files[0];
            const token = localStorage.getItem('masjidku_session_token') || '';
            const ext = file.name.split('.').pop().toLowerCase() || 'png';
            showToast('⏳ Mengunggah logo...');
            try {
                const res = await fetch('/api/upload-logo?ext=' + encodeURIComponent(ext) + '&size=' + file.size + '&token=' + encodeURIComponent(token), {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/octet-stream',
                        'Authorization': 'Bearer ' + token,
                        'X-Auth-Token': token
                    },
                    body: file
                });
                const json = await res.json();
                if (json.status === 'ok') {
                    showToast('✅ ' + json.message);
                    input.value = '';
                    document.getElementById('logoLocalPreviewContainer').style.display = 'none';
                    loadStatus();
                } else {
                    alert('Gagal: ' + (json.message || 'Error'));
                }
            } catch (err) {
                console.error(err);
                alert('Gagal mengunggah logo: ' + err);
            }
        }

        async function deleteLogo() {
            if (confirm('Reset logo masjid ke logo bawaan?')) {
                try {
                    const res = await fetch('/api/delete-logo', { method: 'POST' });
                    const json = await res.json();
                    showToast(json.message || 'Logo berhasil direset');
                    loadStatus();
                } catch (err) {
                    console.error(err);
                    alert('Gagal mereset logo');
                }
            }
        }

        // --- BACKGROUND IMAGE HANDLERS ---
        window.tempBgBase64 = null;
        function previewBgImage(input) {
            if (input.files && input.files[0]) {
                resizeImageToCanvas(input.files[0], 1920, 1080, function(dataUrl) {
                    window.tempBgBase64 = dataUrl;
                    document.getElementById('bgPreviewImg').src = dataUrl;
                    document.getElementById('bgPreviewContainer').style.display = 'block';
                });
            }
        }

        async function uploadBackground() {
            if (!window.tempBgBase64) {
                alert('Silakan pilih foto background dari galeri HP terlebih dahulu!');
                return;
            }
            const btn = document.getElementById('btnUploadBg');
            btn.innerText = '⏳ Mengunggah Background...';
            btn.disabled = true;
            try {
                const payload = {
                    type: 'BACKGROUND',
                    base64: window.tempBgBase64
                };
                const res = await fetch('/api/upload-media', { method: 'POST', body: JSON.stringify(payload) });
                const json = await res.json();
                showToast(json.message || 'Background TV Berhasil Dipasang!');
                document.getElementById('inputBgFile').value = '';
                document.getElementById('bgPreviewContainer').style.display = 'none';
                window.tempBgBase64 = null;
                loadStatus();
            } catch (e) {
                alert('Gagal mengunggah: ' + e);
            } finally {
                btn.innerText = '📤 Pasang Foto Sebagai Background TV';
                btn.disabled = false;
            }
        }

        async function removeBackground() {
            if (confirm('Kembalikan background ke tema standar TV?')) {
                await fetch('/api/remove-background', { method: 'POST' });
                showToast('Background TV kembali ke tema standar');
                loadStatus();
            }
        }

        async function setBackgroundCctvNow(enable, camId) {
            const selectedCamId = camId || (document.getElementById('cfgBackgroundCctvCamSelect') ? document.getElementById('cfgBackgroundCctvCamSelect').value : '') || currentActiveCameraId || (localCctvCameras.length > 0 ? localCctvCameras[0].id : '');
            try {
                const res = await fetch('/api/set-background-cctv', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ enabled: enable ? 'true' : 'false', cameraId: selectedCamId }).toString()
                });
                const resData = await res.json();
                showToast(resData.message || (enable ? 'CCTV terpasang sebagai Background TV!' : 'Background TV dikembalikan.'));
                loadStatus();
            } catch (e) {
                showToast('Gagal mengubah background CCTV');
            }
        }

        async function changeBackgroundCctvCamera(camId) {
            await setBackgroundCctvNow(true, camId);
        }

        async function changeMainBgPreset(val) {
            if (val === 'CCTV_LIVE') {
                await setBackgroundCctvNow(true);
                return;
            }
            await fetch('/api/set-background-cctv', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ enabled: 'false' }).toString()
            });
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ mainScreenBgPreset: val }) });
            showToast('Background Layar Utama diubah!');
            loadStatus();
        }

        async function changeFinanceBgPreset(val) {
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ financeBgPreset: val }) });
            showToast('Background Laporan Kas diubah!');
            loadStatus();
        }

        async function changeFridayBgPreset(val) {
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ fridayOfficerBgPreset: val }) });
            showToast('Background Petugas Jum\'at diubah!');
            loadStatus();
        }

        async function changeCountdownBgPreset(val) {
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ countdownBgPreset: val }) });
            showToast('Background Countdown Pengingat Sholat diubah!');
            loadStatus();
        }

        async function changeCardTransparency(val) {
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ centerCardTransparency: parseFloat(val) }) });
            showToast('Transparansi kartu tengah diatur ke ' + Math.round(parseFloat(val)*100) + '%');
            loadStatus();
        }

        async function uploadSpecificBg(type, inputId, btnId) {
            const input = document.getElementById(inputId);
            if (!input.files || !input.files[0]) {
                alert('Silakan pilih file gambar dari HP terlebih dahulu!');
                return;
            }
            const btn = document.getElementById(btnId);
            const origText = btn.innerText;
            btn.disabled = true;
            btn.innerText = '⏳ Mengunggah...';
            resizeImageToCanvas(input.files[0], 1920, 1080, async function(dataUrl) {
                try {
                    const res = await fetch('/api/upload-media', {
                        method: 'POST',
                        body: JSON.stringify({ type: type, base64: dataUrl })
                    });
                    const data = await res.json();
                    btn.disabled = false;
                    btn.innerText = origText;
                    if (data.status === 'ok') {
                        showToast('✅ ' + (data.message || 'Background berhasil dipasang!'));
                        input.value = '';
                        loadStatus();
                    } else {
                        alert('Gagal: ' + (data.message || 'Gagal upload'));
                    }
                } catch (e) {
                    btn.disabled = false;
                    btn.innerText = origText;
                    alert('Gagal mengunggah: ' + e);
                }
            });
        }

        async function removeFinanceBg() {
            if (confirm('Kembalikan background Laporan Kas ke tema preset standar?')) {
                await fetch('/api/remove-finance-bg', { method: 'POST' });
                showToast('Background Laporan Kas dikembalikan');
                loadStatus();
            }
        }

        async function removeFridayBg() {
            if (confirm('Kembalikan background Petugas Jum\'at ke tema preset standar?')) {
                await fetch('/api/remove-friday-bg', { method: 'POST' });
                showToast('Background Petugas Jum\'at dikembalikan');
                loadStatus();
            }
        }

        async function removeCountdownBg() {
            if (confirm('Kembalikan background Countdown ke tema preset standar?')) {
                await fetch('/api/remove-countdown-bg', { method: 'POST' });
                showToast('Background Countdown dikembalikan');
                loadStatus();
            }
        }

        async function changeBgDim(val) {
            await fetch('/api/set-background-dim', { method: 'POST', body: JSON.stringify({ dim: parseFloat(val) }) });
            showToast('Tingkat kegelapan background diatur ke ' + Math.round(parseFloat(val)*100) + '%');
        }

        // --- SLIDESHOW POSTER HANDLERS ---
        window.tempSlideBase64 = null;
        function previewSlideImage(input) {
            if (input.files && input.files[0]) {
                resizeImageToCanvas(input.files[0], 1920, 1080, function(dataUrl) {
                    window.tempSlideBase64 = dataUrl;
                    document.getElementById('slidePreviewImg').src = dataUrl;
                    document.getElementById('slidePreviewContainer').style.display = 'block';
                });
            }
        }

        async function uploadSlidePoster(e) {
            e.preventDefault();
            if (!window.tempSlideBase64) {
                alert('Pilih gambar poster terlebih dahulu!');
                return;
            }
            const btn = document.getElementById('btnUploadSlide');
            btn.innerText = '⏳ Mengunggah Poster...';
            btn.disabled = true;
            try {
                const payload = {
                    type: 'SLIDESHOW',
                    title: document.getElementById('slideTitle').value,
                    duration: parseInt(document.getElementById('slideDuration').value) || 15,
                    base64: window.tempSlideBase64
                };
                const res = await fetch('/api/upload-media', { method: 'POST', body: JSON.stringify(payload) });
                const json = await res.json();
                showToast(json.message || 'Poster Slideshow Ditambahkan!');
                document.getElementById('formSlide').reset();
                document.getElementById('slidePreviewContainer').style.display = 'none';
                window.tempSlideBase64 = null;
                loadStatus();
            } catch (e) {
                alert('Gagal mengunggah: ' + e);
            } finally {
                btn.innerText = '➕ Upload ke Slideshow TV';
                btn.disabled = false;
            }
        }

        async function deleteMediaSlide(id) {
            if (confirm('Hapus poster ini dari slideshow TV?')) {
                await fetch('/api/delete-media-slide', { method: 'POST', body: JSON.stringify({ id }) });
                showToast('Poster dihapus');
                loadStatus();
            }
        }

        // --- PROFIL & WAKTU ---
        async function saveProfil(e) {
            e.preventDefault();
            const payload = {
                mosqueName: document.getElementById('cfgMosqueName').value,
                tagline: document.getElementById('cfgTagline').value,
                address: document.getElementById('cfgAddress').value,
                city: document.getElementById('cfgCity').value,
                latitude: document.getElementById('cfgLat').value,
                longitude: document.getElementById('cfgLng').value,
                timezone: document.getElementById('cfgTimezone').value,
                phone: document.getElementById('cfgPhone').value,
                bankName: document.getElementById('cfgBankName').value,
                bankAccount: document.getElementById('cfgBankAccount').value,
                bankAccountName: document.getElementById('cfgBankAccountName').value
            };
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify(payload) });
            showToast('Identitas & Lokasi Masjid Berhasil Disimpan!');
            loadStatus();
        }

        async function saveWaktu(e) {
            e.preventDefault();
            const payload = {
                latitude: document.getElementById('cfgLat').value,
                longitude: document.getElementById('cfgLng').value,
                timezone: document.getElementById('cfgTimezone').value,
                showImsak: document.getElementById('showImsak').checked,
                showSubuh: document.getElementById('showSubuh').checked,
                showTerbit: document.getElementById('showTerbit').checked,
                showDhuha: document.getElementById('showDhuha').checked,
                showDzuhur: document.getElementById('showDzuhur').checked,
                showAshar: document.getElementById('showAshar').checked,
                showMaghrib: document.getElementById('showMaghrib').checked,
                showIsya: document.getElementById('showIsya').checked,
                hijriAdjustmentDays: document.getElementById('cfgHijri').value,
                offsetSubuh: document.getElementById('offSubuh').value,
                offsetTerbit: document.getElementById('offTerbit').value,
                offsetDzuhur: document.getElementById('offDzuhur').value,
                offsetAshar: document.getElementById('offAshar').value,
                offsetMaghrib: document.getElementById('offMaghrib').value,
                offsetIsya: document.getElementById('offIsya').value,
                iqomahSubuh: document.getElementById('iqSubuh').value,
                iqomahDzuhur: document.getElementById('iqDzuhur').value,
                iqomahJumat: document.getElementById('iqJumat')?.value || 15,
                iqomahAshar: document.getElementById('iqAshar').value,
                iqomahMaghrib: document.getElementById('iqMaghrib').value,
                iqomahIsya: document.getElementById('iqIsya').value,
                sholatDurationMinutes: document.getElementById('sholatDuration').value
            };
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify(payload) });
            showToast('Pengaturan Visibilitas & Waktu Sholat Disimpan!');
            loadStatus();
        }

        async function saveTheme(val) {
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ activeTheme: val }) });
            showToast('Tema TV Berhasil Diganti!');
        }

        async function saveLayoutModel(val) {
            await fetch('/api/save-config', { method: 'POST', body: JSON.stringify({ displayLayoutModel: val }) });
            showToast('Model Tampilan Jam TV Berhasil Diganti!');
        }

        function syncAndSaveContentVisibility(srcId, targetId) {
            const src = document.getElementById(srcId);
            const target = document.getElementById(targetId);
            if (src && target) target.checked = src.checked;
            saveContentVisibility();
        }

        async function saveContentVisibility() {
            const getVal = (id1, id2) => {
                const el1 = document.getElementById(id1);
                const el2 = document.getElementById(id2);
                if (el1) return !!el1.checked;
                if (el2) return !!el2.checked;
                return false;
            };

            const finVal = getVal('showFinReport', 'showFinReportTab1');
            const friVal = getVal('showFriOfficers', 'showFriOfficersTab1');
            const qrisVal = getVal('showQrisCard', 'showQrisCardTab1');
            const hadithVal = getVal('showDailyHadith', 'showDailyHadithTab1');
            const actVal = getVal('showActivities', 'showActivitiesTab1');
            const maklumatVal = getVal('showDailyMaklumat', 'showDailyMaklumatTab1');

            // Keep both DOM elements in full sync
            const pairs = [
                ['showFinReport', 'showFinReportTab1', finVal],
                ['showFriOfficers', 'showFriOfficersTab1', friVal],
                ['showQrisCard', 'showQrisCardTab1', qrisVal],
                ['showDailyHadith', 'showDailyHadithTab1', hadithVal],
                ['showActivities', 'showActivitiesTab1', actVal],
                ['showDailyMaklumat', 'showDailyMaklumatTab1', maklumatVal]
            ];
            pairs.forEach(function(item) {
                const e1 = document.getElementById(item[0]);
                const e2 = document.getElementById(item[1]);
                if (e1) e1.checked = item[2];
                if (e2) e2.checked = item[2];
            });

            const payload = {
                showFinancialReport: finVal,
                showFridayOfficers: friVal,
                showQrisCard: qrisVal,
                showDailyHadith: hadithVal,
                showActivities: actVal,
                showDailyMaklumat: maklumatVal
            };
            try {
                await fetch('/api/save-config', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('Pengaturan Tampilan Konten Disimpan!');
            } catch(e) {
                showToast('Gagal menyimpan pengaturan konten');
            }
        }

        async function saveDonationProgram() {
            const payload = {
                donationProgramTitle: document.getElementById('cfgDonationTitle').value,
                donationTargetAmount: parseInt(document.getElementById('cfgDonationTarget').value) || 0,
                donationCollectedAmount: parseInt(document.getElementById('cfgDonationCollected').value) || 0
            };
            try {
                await fetch('/api/save-config', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('Program Donasi Berhasil Disimpan!');
            } catch(e) {
                showToast('Gagal menyimpan program donasi');
            }
        }

        async function uploadQrisImage() {
            const input = document.getElementById('qrisFileInput');
            if (!input.files || input.files.length === 0) {
                alert('Silakan pilih file gambar QRIS terlebih dahulu.');
                return;
            }
            const file = input.files[0];
            const btn = document.getElementById('btnUploadQris');
            btn.disabled = true;
            btn.innerText = 'Mengunggah...';

            const reader = new FileReader();
            reader.onload = async function(e) {
                const base64 = e.target.result;
                try {
                    const res = await fetch('/api/upload-media', {
                        method: 'POST',
                        body: JSON.stringify({ type: 'QRIS', base64: base64 })
                    });
                    const json = await res.json();
                    alert(json.message || 'QRIS berhasil di-upload!');
                    input.value = '';
                    loadConfig();
                } catch(err) {
                    alert('Gagal mengunggah QRIS: ' + err);
                } finally {
                    btn.disabled = false;
                    btn.innerText = '📲 Upload QRIS ke TV';
                }
            };
            reader.readAsDataURL(file);
        }

        async function removeQrisImage() {
            if (!confirm('Hapus gambar QRIS dari layar TV?')) return;
            try {
                await fetch('/api/remove-qris', { method: 'POST' });
                alert('Gambar QRIS berhasil dihapus.');
                loadConfig();
            } catch(e) {
                alert('Gagal menghapus QRIS: ' + e);
            }
        }

        function loadConfig() {
            loadStatus();
        }

        // --- UNIVERSAL MODAL SYSTEM ---
        function openUniversalModal(title, bodyHtml) {
            const m = document.getElementById('universalModal');
            if (!m) return;
            document.getElementById('modalTitle').innerHTML = title;
            document.getElementById('modalBody').innerHTML = bodyHtml;
            m.style.display = 'flex';
        }

        function closeUniversalModal() {
            if (typeof clockPreviewInterval !== 'undefined' && clockPreviewInterval) {
                clearInterval(clockPreviewInterval);
                clockPreviewInterval = null;
            }
            window.onColorPickerChanged = null;
            const m = document.getElementById('universalModal');
            if (m) {
                m.style.display = 'none';
                document.getElementById('modalBody').innerHTML = '';
            }
        }

        function syncColor(pickerId, textId) {
            const p = document.getElementById(pickerId);
            const t = document.getElementById(textId);
            if (p && t) t.value = p.value.toUpperCase();
            if (window.onColorPickerChanged) window.onColorPickerChanged();
        }

        function syncHexToPicker(textId, pickerId) {
            const t = document.getElementById(textId);
            const p = document.getElementById(pickerId);
            if (t && p) {
                const v = t.value.trim();
                if (v.startsWith('#') && v.length === 7) {
                    p.value = v;
                }
            }
            if (window.onColorPickerChanged) window.onColorPickerChanged();
        }

        function makeColorRow(label, pickerId, textId, defaultHex, curHex) {
            const val = (curHex || defaultHex || '#FFFFFF').toUpperCase();
            return '<div class="color-row">' +
                '<span style="font-size:13px; font-weight:700; color:#0F172A;">' + label + '</span>' +
                '<div class="color-picker-wrap">' +
                    '<input type="color" id="' + pickerId + '" value="' + val + '" oninput="syncColor(\'' + pickerId + '\', \'' + textId + '\')" style="width:36px; height:36px; padding:0; border:none; border-radius:6px; cursor:pointer; background:none;">' +
                    '<input type="text" id="' + textId + '" class="form-input color-hex-text" value="' + val + '" maxlength="7" oninput="syncHexToPicker(\'' + textId + '\', \'' + pickerId + '\')">' +
                '</div>' +
            '</div>';
        }

        async function saveContentCustomization(payload, msg) {
            try {
                const res = await fetch('/api/save-config', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                if (res.ok) {
                    showToast(msg || 'Kustomisasi berhasil disimpan dan diperbarui di TV!');
                    closeUniversalModal();
                    loadStatus();
                } else {
                    alert('Gagal menyimpan kustomisasi ke server TV.');
                }
            } catch(err) {
                alert('Terjadi kesalahan: ' + err);
            }
        }

        // --- 1. MODAL KUSTOMISASI LAPORAN KEUANGAN ---
        function openFinanceCustomizationModal() {
            const cfg = window.currentConfig || {};
            const html = '<form onsubmit="handleSaveFinanceCustomization(event)">' +
                '<div style="background:#FFFBEB; border:1.5px solid #F59E0B; border-radius:10px; padding:14px; margin-bottom:14px;">' +
                    '<div style="font-weight:800; font-size:13.5px; color:#92400E; margin-bottom:10px;">📌 JUDUL LAPORAN KEUANGAN</div>' +
                    '<div style="margin-bottom:10px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Teks Judul Laporan</label>' +
                        '<input type="text" id="custFinTitle" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.financeTitleText || 'LAPORAN KAS KEUANGAN MASJID') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Judul', 'custFinTitleColorPick', 'custFinTitleColor', '#F59E0B', cfg.financeTitleColor) +
                '</div>' +
                '<div style="background:#F0FDF4; border:1.5px solid #10B981; border-radius:10px; padding:14px; margin-bottom:16px;">' +
                    '<div style="font-weight:800; font-size:13.5px; color:#065F46; margin-bottom:10px;">🎨 WARNA FONT ISI & SALDO KAS</div>' +
                    makeColorRow('Warna Saldo Kas (Saldo Akhir)', 'custFinBalColorPick', 'custFinBalColor', '#38BDF8', cfg.financeBalanceColor) +
                    makeColorRow('Warna Pemasukan (+ / Infaq Masuk)', 'custFinIncColorPick', 'custFinIncColor', '#10B981', cfg.financeIncomeColor) +
                    makeColorRow('Warna Pengeluaran (- / Kas Keluar)', 'custFinExpColorPick', 'custFinExpColor', '#EF4444', cfg.financeExpenseColor) +
                    makeColorRow('Warna Rekening Bank / Footer', 'custFinFootColorPick', 'custFinFootColor', '#94A3B8', cfg.financeFooterColor) +
                '</div>' +
                '<div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">' +
                    '<button type="button" class="btn-secondary" onclick="resetFinanceCustomization()" style="width:auto; font-size:12px; padding:8px 12px; color:#FCA5A5; border-color:#EF4444;">🔄 Reset Default</button>' +
                    '<div style="display:flex; gap:8px;">' +
                        '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 14px;">Batal</button>' +
                        '<button type="submit" class="btn-primary" style="width:auto; padding:8px 16px;">💾 Simpan ke TV</button>' +
                    '</div>' +
                '</div>' +
            '</form>';
            openUniversalModal('💰 Kustomisasi Laporan Kas Keuangan', html);
        }

        async function handleSaveFinanceCustomization(e) {
            e.preventDefault();
            const payload = {
                financeTitleText: document.getElementById('custFinTitle').value.trim() || 'LAPORAN KAS KEUANGAN MASJID',
                financeTitleColor: document.getElementById('custFinTitleColor').value.trim() || '#F59E0B',
                financeBalanceColor: document.getElementById('custFinBalColor').value.trim() || '#38BDF8',
                financeIncomeColor: document.getElementById('custFinIncColor').value.trim() || '#10B981',
                financeExpenseColor: document.getElementById('custFinExpColor').value.trim() || '#EF4444',
                financeFooterColor: document.getElementById('custFinFootColor').value.trim() || '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Teks & Warna Laporan Keuangan TV Berhasil Disimpan!');
        }

        async function resetFinanceCustomization() {
            if (!confirm('Kembalikan teks dan warna laporan keuangan ke setelan bawaan?')) return;
            const payload = {
                financeTitleText: 'LAPORAN KAS KEUANGAN MASJID',
                financeTitleColor: '#F59E0B',
                financeBalanceColor: '#38BDF8',
                financeIncomeColor: '#10B981',
                financeExpenseColor: '#EF4444',
                financeFooterColor: '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Laporan Keuangan dikembalikan ke default!');
        }

        // --- 2. MODAL KUSTOMISASI PETUGAS JUMAT ---
        function openFridayCustomizationModal() {
            const cfg = window.currentConfig || {};
            const html = '<form onsubmit="handleSaveFridayCustomization(event)">' +
                '<div style="background:#FFFBEB; border:1.5px solid #F59E0B; border-radius:10px; padding:14px; margin-bottom:14px;">' +
                    '<div style="font-weight:700; font-size:13px; color:#F59E0B; margin-bottom:10px;">📌 JUDUL JADWAL JUM\'AT</div>' +
                    '<div style="margin-bottom:10px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Teks Judul Petugas Jum\'at</label>' +
                        '<input type="text" id="custFriTitle" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.fridayTitleText || 'JADWAL PETUGAS JUM\'AT') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Judul', 'custFriTitleColorPick', 'custFriTitleColor', '#F59E0B', cfg.fridayTitleColor) +
                '</div>' +
                '<div style="background:#F0FDF4; border:1.5px solid #10B981; border-radius:10px; padding:14px; margin-bottom:16px;">' +
                    '<div style="font-weight:700; font-size:13px; color:#10B981; margin-bottom:10px;">🎨 WARNA FONT PETUGAS JUM\'AT</div>' +
                    makeColorRow('Warna Nama Petugas (Khotib, Imam, dll)', 'custFriOfficerPick', 'custFriOfficerColor', '#FFFFFF', cfg.fridayOfficerNameColor) +
                    makeColorRow('Warna Label Jabatan & Tanggal', 'custFriLabelPick', 'custFriLabelColor', '#94A3B8', cfg.fridayOfficerLabelColor) +
                '</div>' +
                '<div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">' +
                    '<button type="button" class="btn-secondary" onclick="resetFridayCustomization()" style="width:auto; font-size:12px; padding:8px 12px; color:#FCA5A5; border-color:#EF4444;">🔄 Reset Default</button>' +
                    '<div style="display:flex; gap:8px;">' +
                        '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 14px;">Batal</button>' +
                        '<button type="submit" class="btn-primary" style="width:auto; padding:8px 16px;">💾 Simpan ke TV</button>' +
                    '</div>' +
                '</div>' +
            '</form>';
            openUniversalModal('🕌 Kustomisasi Petugas Jum\'at', html);
        }

        async function handleSaveFridayCustomization(e) {
            e.preventDefault();
            const payload = {
                fridayTitleText: document.getElementById('custFriTitle').value.trim() || 'JADWAL PETUGAS JUM\'AT',
                fridayTitleColor: document.getElementById('custFriTitleColor').value.trim() || '#F59E0B',
                fridayOfficerNameColor: document.getElementById('custFriOfficerColor').value.trim() || '#FFFFFF',
                fridayOfficerLabelColor: document.getElementById('custFriLabelColor').value.trim() || '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Teks & Warna Petugas Jum\'at TV Berhasil Disimpan!');
        }

        async function resetFridayCustomization() {
            if (!confirm('Kembalikan teks dan warna petugas jum\'at ke setelan bawaan?')) return;
            const payload = {
                fridayTitleText: 'JADWAL PETUGAS JUM\'AT',
                fridayTitleColor: '#F59E0B',
                fridayOfficerNameColor: '#FFFFFF',
                fridayOfficerLabelColor: '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Petugas Jum\'at dikembalikan ke default!');
        }

        // --- 3. MODAL KUSTOMISASI MUTIARA HADITS ---
        function openHadithCustomizationModal() {
            const cfg = window.currentConfig || {};
            const html = '<form onsubmit="handleSaveHadithCustomization(event)">' +
                '<div style="background:#FFFBEB; border:1.5px solid #F59E0B; border-radius:10px; padding:14px; margin-bottom:14px;">' +
                    '<div style="font-weight:800; font-size:13.5px; color:#92400E; margin-bottom:10px;">📌 JUDUL & TEMA HADITS</div>' +
                    '<div style="margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Teks Judul Hadits</label>' +
                        '<input type="text" id="custHadithTitle" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.hadithTitleText || 'MUTIARA HADITS SHAHIH') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Judul', 'custHadithTitleColorPick', 'custHadithTitleColor', '#F59E0B', cfg.hadithTitleColor) +
                    '<div style="margin-top:10px; margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Tema / Topik Hadits</label>' +
                        '<input type="text" id="custHadithTheme" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.hadithThemeText || 'Keutamaan Menuntut Ilmu') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Tema', 'custHadithThemeColorPick', 'custHadithThemeColor', '#38BDF8', cfg.hadithThemeColor) +
                '</div>' +
                '<div style="background:#FAF5FF; border:1.5px solid #8B5CF6; border-radius:10px; padding:14px; margin-bottom:16px;">' +
                    '<div style="font-weight:800; font-size:13.5px; color:#5B21B6; margin-bottom:10px;">📖 ISI HADITS, TERJEMAHAN & PERAWI</div>' +
                    '<div style="margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Teks Hadits Arab</label>' +
                        '<textarea id="custHadithArab" class="form-input" rows="2" style="width:100%; font-size:16px; direction:rtl; text-align:right;">' + escapeHtml(cfg.hadithArabicText || 'مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ') + '</textarea>' +
                    '</div>' +
                    makeColorRow('Warna Font Tulisan Arab', 'custHadithArabColorPick', 'custHadithArabColor', '#FDE68A', cfg.hadithArabicColor) +
                    '<div style="margin-top:10px; margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Terjemahan Hadits</label>' +
                        '<textarea id="custHadithTrans" class="form-input" rows="3" style="width:100%; font-size:13px;">' + escapeHtml(cfg.hadithTranslationText || 'Barangsiapa menempuh jalan untuk menuntut ilmu, maka Allah akan memudahkan baginya jalan menuju surga.') + '</textarea>' +
                    '</div>' +
                    makeColorRow('Warna Font Terjemahan', 'custHadithTransColorPick', 'custHadithTransColor', '#FFFFFF', cfg.hadithTranslationColor) +
                    '<div style="margin-top:10px; margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Perawi / Takhrij (Sumber)</label>' +
                        '<input type="text" id="custHadithNarr" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.hadithNarratorText || 'HR. Muslim no. 2699') + '">' +
                    '</div>' +
                    makeColorRow('Warna Font Perawi', 'custHadithNarrColorPick', 'custHadithNarrColor', '#94A3B8', cfg.hadithNarratorColor) +
                '</div>' +
                '<div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">' +
                    '<button type="button" class="btn-secondary" onclick="resetHadithCustomization()" style="width:auto; font-size:12px; padding:8px 12px; color:#FCA5A5; border-color:#EF4444;">🔄 Reset Default</button>' +
                    '<div style="display:flex; gap:8px;">' +
                        '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 14px;">Batal</button>' +
                        '<button type="submit" class="btn-primary" style="width:auto; padding:8px 16px;">💾 Simpan ke TV</button>' +
                    '</div>' +
                '</div>' +
            '</form>';
            openUniversalModal('📖 Kustomisasi Mutiara Hadits Shahih', html);
        }

        async function handleSaveHadithCustomization(e) {
            e.preventDefault();
            const payload = {
                hadithTitleText: document.getElementById('custHadithTitle').value.trim() || 'MUTIARA HADITS SHAHIH',
                hadithTitleColor: document.getElementById('custHadithTitleColor').value.trim() || '#F59E0B',
                hadithThemeText: document.getElementById('custHadithTheme').value.trim() || 'Keutamaan Menuntut Ilmu',
                hadithThemeColor: document.getElementById('custHadithThemeColor').value.trim() || '#38BDF8',
                hadithArabicText: document.getElementById('custHadithArab').value.trim() || 'مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ',
                hadithArabicColor: document.getElementById('custHadithArabColor').value.trim() || '#FDE68A',
                hadithTranslationText: document.getElementById('custHadithTrans').value.trim() || 'Barangsiapa menempuh jalan untuk menuntut ilmu, maka Allah akan memudahkan baginya jalan menuju surga.',
                hadithTranslationColor: document.getElementById('custHadithTransColor').value.trim() || '#FFFFFF',
                hadithNarratorText: document.getElementById('custHadithNarr').value.trim() || 'HR. Muslim no. 2699',
                hadithNarratorColor: document.getElementById('custHadithNarrColor').value.trim() || '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Mutiara Hadits Shahih TV Berhasil Disimpan!');
        }

        async function resetHadithCustomization() {
            if (!confirm('Kembalikan teks dan warna mutiara hadits ke setelan bawaan?')) return;
            const payload = {
                hadithTitleText: 'MUTIARA HADITS SHAHIH',
                hadithTitleColor: '#F59E0B',
                hadithThemeText: 'Keutamaan Menuntut Ilmu',
                hadithThemeColor: '#38BDF8',
                hadithArabicText: 'مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ',
                hadithArabicColor: '#FDE68A',
                hadithTranslationText: 'Barangsiapa menempuh jalan untuk menuntut ilmu, maka Allah akan memudahkan baginya jalan menuju surga.',
                hadithTranslationColor: '#FFFFFF',
                hadithNarratorText: 'HR. Muslim no. 2699',
                hadithNarratorColor: '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Mutiara Hadits dikembalikan ke default!');
        }

        // --- 4. MODAL KUSTOMISASI MAKLUMAT & ADAB ---
        function openMaklumatCustomizationModal() {
            const cfg = window.currentConfig || {};
            const html = '<form onsubmit="handleSaveMaklumatCustomization(event)">' +
                '<div style="background:#FFFBEB; border:1.5px solid #F59E0B; border-radius:10px; padding:14px; margin-bottom:14px;">' +
                    '<div style="font-weight:800; font-size:13.5px; color:#92400E; margin-bottom:10px;">📜 KOLOM KIRI (HIKMAH & PENGUMUMAN)</div>' +
                    '<div style="margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Judul Kolom Kiri</label>' +
                        '<input type="text" id="custMakLeftTitle" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.maklumatTitleLeft || 'PENGUMUMAN & ADAB') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Judul Kiri', 'custMakLeftTitleColorPick', 'custMakLeftTitleColor', '#F59E0B', cfg.maklumatTitleLeftColor) +
                    '<div style="margin-top:10px; margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Isi Hikmah / Pengumuman Kiri</label>' +
                        '<textarea id="custMakLeftText" class="form-input" rows="3" style="width:100%; font-size:13px;">' + escapeHtml(cfg.maklumatTextLeft || 'Lurus dan rapatkan shaf sholat berjamaah karena lurusnya shaf merupakan bagian dari kesempurnaan sholat.') + '</textarea>' +
                    '</div>' +
                    makeColorRow('Warna Font Teks Hikmah Kiri', 'custMakLeftTextColorPick', 'custMakLeftTextColor', '#E2E8F0', cfg.maklumatTextLeftColor) +
                '</div>' +
                '<div style="background:#F0FDF4; border:1.5px solid #10B981; border-radius:10px; padding:14px; margin-bottom:16px;">' +
                    '<div style="font-weight:800; font-size:13.5px; color:#0369A1; margin-bottom:10px;">📋 KOLOM KANAN (TATA TERTIB & HIMBAUAN)</div>' +
                    '<div style="margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Judul Kolom Kanan</label>' +
                        '<input type="text" id="custMakRightTitle" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.maklumatTitleRight || 'TATA TERTIB MASJID') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Judul Kanan', 'custMakRightTitleColorPick', 'custMakRightTitleColor', '#38BDF8', cfg.maklumatTitleRightColor) +
                    '<div style="margin-top:10px; margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Poin Tata Tertib 1</label>' +
                        '<input type="text" id="custMakP1" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.maklumatPoint1 || '1. Nonaktifkan atau senyapkan nada dering HP') + '">' +
                    '</div>' +
                    '<div style="margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Poin Tata Tertib 2</label>' +
                        '<input type="text" id="custMakP2" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.maklumatPoint2 || '2. Jagalah kebersihan, buang sampah pada tempatnya') + '">' +
                    '</div>' +
                    '<div style="margin-bottom:8px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Poin Tata Tertib 3</label>' +
                        '<input type="text" id="custMakP3" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.maklumatPoint3 || '3. Dilarang tidur terlentang di area sholat utama') + '">' +
                    '</div>' +
                    makeColorRow('Warna Font Poin-Poin', 'custMakPtsColorPick', 'custMakPtsColor', '#CBD5E1', cfg.maklumatPointsColor) +
                '</div>' +
                '<div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">' +
                    '<button type="button" class="btn-secondary" onclick="resetMaklumatCustomization()" style="width:auto; font-size:12px; padding:8px 12px; color:#FCA5A5; border-color:#EF4444;">🔄 Reset Default</button>' +
                    '<div style="display:flex; gap:8px;">' +
                        '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 14px;">Batal</button>' +
                        '<button type="submit" class="btn-primary" style="width:auto; padding:8px 16px;">💾 Simpan ke TV</button>' +
                    '</div>' +
                '</div>' +
            '</form>';
            openUniversalModal('📢 Kustomisasi Maklumat & Adab Masjid', html);
        }

        async function handleSaveMaklumatCustomization(e) {
            e.preventDefault();
            const payload = {
                maklumatTitleLeft: document.getElementById('custMakLeftTitle').value.trim() || 'PENGUMUMAN & ADAB',
                maklumatTitleLeftColor: document.getElementById('custMakLeftTitleColor').value.trim() || '#F59E0B',
                maklumatTextLeft: document.getElementById('custMakLeftText').value.trim() || 'Lurus dan rapatkan shaf sholat berjamaah...',
                maklumatTextLeftColor: document.getElementById('custMakLeftTextColor').value.trim() || '#E2E8F0',
                maklumatTitleRight: document.getElementById('custMakRightTitle').value.trim() || 'TATA TERTIB MASJID',
                maklumatTitleRightColor: document.getElementById('custMakRightTitleColor').value.trim() || '#38BDF8',
                maklumatPoint1: document.getElementById('custMakP1').value.trim() || '1. Nonaktifkan atau senyapkan nada dering HP',
                maklumatPoint2: document.getElementById('custMakP2').value.trim() || '2. Jagalah kebersihan, buang sampah pada tempatnya',
                maklumatPoint3: document.getElementById('custMakP3').value.trim() || '3. Dilarang tidur terlentang di area sholat utama',
                maklumatPointsColor: document.getElementById('custMakPtsColor').value.trim() || '#CBD5E1'
            };
            await saveContentCustomization(payload, 'Kustomisasi Maklumat & Adab Masjid TV Berhasil Disimpan!');
        }

        async function resetMaklumatCustomization() {
            if (!confirm('Kembalikan teks dan warna maklumat masjid ke setelan bawaan?')) return;
            const payload = {
                maklumatTitleLeft: 'PENGUMUMAN & ADAB',
                maklumatTitleLeftColor: '#F59E0B',
                maklumatTextLeft: 'Lurus dan rapatkan shaf sholat berjamaah karena lurusnya shaf merupakan bagian dari kesempurnaan sholat.',
                maklumatTextLeftColor: '#E2E8F0',
                maklumatTitleRight: 'TATA TERTIB MASJID',
                maklumatTitleRightColor: '#38BDF8',
                maklumatPoint1: '1. Nonaktifkan atau senyapkan nada dering HP',
                maklumatPoint2: '2. Jagalah kebersihan, buang sampah pada tempatnya',
                maklumatPoint3: '3. Dilarang tidur terlentang di area sholat utama',
                maklumatPointsColor: '#CBD5E1'
            };
            await saveContentCustomization(payload, 'Kustomisasi Maklumat Masjid dikembalikan ke default!');
        }

        // --- 5. MODAL KUSTOMISASI JAM DIGITAL TV ---
        var clockPreviewInterval = null;

        function getClockFontDisplayName(key) {
            const map = {
                'RADIOLAND': '📟 7-Segment LED (Radioland)',
                'ORBITRON': '🚀 Orbitron Tech (Futuristik)',
                'RAJDHANI': '⚡ Rajdhani Bold (Tegas)',
                'SANS_SERIF': '💎 Modern Clean (Sans-Serif)',
                'SERIF': '📜 Classic Traditional (Serif)',
                'MONOSPACE': '💻 Retro Monospace (Terminal)'
            };
            return map[key] || key;
        }

        function makeClockFontButton(key, title, subtitle, cssFontFamily, selectedKey) {
            const isSelected = key === selectedKey;
            return '<button type="button" class="btn-secondary clock-font-btn" id="fontBtn_' + key + '" onclick="selectClockFontPreset(\'' + key + '\')" style="padding: 10px 6px; text-align: center; border-radius: 8px; cursor: pointer; transition: all 0.2s; ' +
                (isSelected ? 'border: 2px solid #0284C7; background: #E0F2FE; box-shadow: 0 0 10px rgba(2,132,199,0.3);' : 'border: 1.5px solid #CBD5E1; background: #FFFFFF;') + '">' +
                '<div style="font-size: 16px; font-weight: bold; font-family: ' + cssFontFamily + '; color: ' + (isSelected ? '#0369A1' : '#0F172A') + ';">12:00:45</div>' +
                '<div style="font-size: 11px; font-weight: 700; color: #0F172A; margin-top: 2px;">' + title + '</div>' +
                '<div style="font-size: 9.5px; color: #64748B;">' + subtitle + '</div>' +
            '</button>';
        }

        function openClockCustomizationModal() {
            const cfg = window.currentConfig || {};
            const initialFont = cfg.clockFontFamily || 'RADIOLAND';
            const initialClockColor = cfg.clockColor || '#FFFFFF';
            const initialColonColor = cfg.clockColonColor || '#F59E0B';
            const initialSecColor = cfg.clockSecondsColor || '#F59E0B';

            window.onColorPickerChanged = updateClockLivePreview;

            const html = '<form onsubmit="handleSaveClockCustomization(event)">' +
                '<!-- 1. Live Interactive Clock Preview -->' +
                '<div style="background: linear-gradient(135deg, #022014 0%, #01120B 100%); border: 2px solid #F59E0B; border-radius: 14px; padding: 18px 20px; text-align: center; margin-bottom: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.3);">' +
                    '<div style="font-size: 11px; font-weight: 700; color: #34D399; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px;">' +
                        '📺 Pratinjau Tampilan Jam Digital di Layar TV' +
                    '</div>' +
                    '<div style="display: inline-flex; align-items: baseline; justify-content: center; gap: 4px; padding: 8px 18px; background: rgba(0,0,0,0.45); border-radius: 12px; border: 1.5px solid rgba(245,158,11,0.4);">' +
                        '<span id="previewClockHourMin" style="font-size: 38px; font-weight: bold; letter-spacing: 2px; color: ' + initialClockColor + '; font-family: monospace;">12:00</span>' +
                        '<span id="previewClockSec" style="font-size: 20px; font-weight: bold; color: ' + initialSecColor + '; margin-left: 4px; font-family: monospace;">:45</span>' +
                        '<span style="font-size: 11px; font-weight: bold; color: #6EE7B7; margin-left: 6px;">WIB</span>' +
                    '</div>' +
                    '<div id="previewClockFontLabel" style="font-size: 11.5px; color: #94A3B8; margin-top: 6px;">Font: <b>' + getClockFontDisplayName(initialFont) + '</b></div>' +
                '</div>' +

                '<!-- 2. Preset Font Selector -->' +
                '<div style="margin-bottom: 14px;">' +
                    '<label style="display:block; font-size:12.5px; font-weight:700; color:#0F172A; margin-bottom:6px;">1. Pilih Preset Font Jam Digital:</label>' +
                    '<div class="grid-3" style="gap: 8px;">' +
                        makeClockFontButton('RADIOLAND', '📟 7-Segment LED', 'Radioland (Klasik)', 'monospace', initialFont) +
                        makeClockFontButton('ORBITRON', '🚀 Orbitron Tech', 'Futuristik Sci-Fi', 'sans-serif', initialFont) +
                        makeClockFontButton('RAJDHANI', '⚡ Rajdhani Bold', 'Tegas & Ramping', 'sans-serif', initialFont) +
                        makeClockFontButton('SANS_SERIF', '💎 Sans-Serif', 'Modern Minimalis', 'sans-serif', initialFont) +
                        makeClockFontButton('SERIF', '📜 Classic Serif', 'Islami Kaligrafi', 'serif', initialFont) +
                        makeClockFontButton('MONOSPACE', '💻 Monospace', 'Digital Terminal', 'monospace', initialFont) +
                    '</div>' +
                    '<input type="hidden" id="custClockFontFamily" value="' + initialFont + '">' +
                '</div>' +

                '<!-- 3. Preset Warna 1-Klik -->' +
                '<div style="margin-bottom: 14px;">' +
                    '<label style="display:block; font-size:12.5px; font-weight:700; color:#0F172A; margin-bottom:6px;">2. Preset Warna Tema Jam (1-Klik):</label>' +
                    '<div class="grid-4" style="gap: 6px;">' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#FFFBEB; border:1.5px solid #F59E0B; border-radius:8px;" onclick="selectClockColorPreset(\'#FFD700\', \'#F59E0B\', \'#F59E0B\')">' +
                            '<span style="font-size:12px;">🟡</span> <b style="font-size:11px; color:#92400E;">Emas Madinah</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#F8FAFC; border:1.5px solid #94A3B8; border-radius:8px;" onclick="selectClockColorPreset(\'#FFFFFF\', \'#38BDF8\', \'#38BDF8\')">' +
                            '<span style="font-size:12px;">⚪</span> <b style="font-size:11px; color:#0F172A;">Putih Kristal</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#F0FDF4; border:1.5px solid #10B981; border-radius:8px;" onclick="selectClockColorPreset(\'#34D399\', \'#10B981\', \'#10B981\')">' +
                            '<span style="font-size:12px;">🟢</span> <b style="font-size:11px; color:#065F46;">Hijau Zamrud</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#F0F9FF; border:1.5px solid #0284C7; border-radius:8px;" onclick="selectClockColorPreset(\'#38BDF8\', \'#0284C7\', \'#0284C7\')">' +
                            '<span style="font-size:12px;">🔵</span> <b style="font-size:11px; color:#0369A1;">Cyber Cyan</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#FEF2F2; border:1.5px solid #EF4444; border-radius:8px;" onclick="selectClockColorPreset(\'#EF4444\', \'#F87171\', \'#F87171\')">' +
                            '<span style="font-size:12px;">🔴</span> <b style="font-size:11px; color:#991B1B;">Merah Digital</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#FFF7ED; border:1.5px solid #F97316; border-radius:8px;" onclick="selectClockColorPreset(\'#FBBF24\', \'#F59E0B\', \'#F59E0B\')">' +
                            '<span style="font-size:12px;">🟠</span> <b style="font-size:11px; color:#9A3412;">Sunset Amber</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#FAF5FF; border:1.5px solid #A855F7; border-radius:8px;" onclick="selectClockColorPreset(\'#C084FC\', \'#A855F7\', \'#A855F7\')">' +
                            '<span style="font-size:12px;">🟣</span> <b style="font-size:11px; color:#6B21A8;">Royal Violet</b>' +
                        '</button>' +
                        '<button type="button" class="btn-secondary" style="padding:8px 4px; text-align:center; background:#ECFDF5; border:1.5px solid #34D399; border-radius:8px;" onclick="selectClockColorPreset(\'#6EE7B7\', \'#34D399\', \'#34D399\')">' +
                            '<span style="font-size:12px;">🩵</span> <b style="font-size:11px; color:#065F46;">Ice Mint</b>' +
                        '</button>' +
                    '</div>' +
                '</div>' +

                '<!-- 4. Custom Color Pickers -->' +
                '<div style="background:#F8FAFC; border:1.5px solid #CBD5E1; border-radius:10px; padding:12px; margin-bottom:16px;">' +
                    '<div style="font-weight:700; font-size:12.5px; color:#334155; margin-bottom:8px;">3. Kustomisasi Warna Bebas (Hex / Color Picker):</div>' +
                    makeColorRow('Warna Angka Jam & Menit', 'custClockColorPick', 'custClockColor', '#FFFFFF', initialClockColor) +
                    makeColorRow('Warna Titik Dua Berkedip (:)', 'custClockColonPick', 'custClockColonColor', '#F59E0B', initialColonColor) +
                    makeColorRow('Warna Angka Detik (:SS)', 'custClockSecPick', 'custClockSecColor', '#F59E0B', initialSecColor) +
                '</div>' +

                '<!-- 5. Action Buttons -->' +
                '<div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">' +
                    '<button type="button" class="btn-secondary" onclick="resetClockCustomization()" style="width:auto; font-size:12px; padding:8px 12px; color:#FCA5A5; border-color:#EF4444;">🔄 Reset Default</button>' +
                    '<div style="display:flex; gap:8px;">' +
                        '<button type="button" class="btn-secondary" onclick="closeClockModal()" style="width:auto; padding:8px 14px;">Batal</button>' +
                        '<button type="submit" class="btn-primary" style="width:auto; padding:8px 16px; background: linear-gradient(135deg, #0284c7, #0369a1); font-weight:700;">💾 Simpan ke TV</button>' +
                    '</div>' +
                '</div>' +
            '</form>';

            openUniversalModal('⏰ Kustomisasi Font & Warna Jam Digital TV', html);
            startClockPreviewTicker();
            updateClockLivePreview();
        }

        function closeClockModal() {
            if (clockPreviewInterval) {
                clearInterval(clockPreviewInterval);
                clockPreviewInterval = null;
            }
            window.onColorPickerChanged = null;
            closeUniversalModal();
        }

        function startClockPreviewTicker() {
            if (clockPreviewInterval) clearInterval(clockPreviewInterval);
            let blink = true;
            clockPreviewInterval = setInterval(function() {
                const now = new Date();
                const hh = String(now.getHours()).padStart(2, '0');
                const mm = String(now.getMinutes()).padStart(2, '0');
                const ss = String(now.getSeconds()).padStart(2, '0');
                blink = !blink;

                const colonColor = document.getElementById('custClockColonColor') ? document.getElementById('custClockColonColor').value.trim() : '#F59E0B';
                const elHourMin = document.getElementById('previewClockHourMin');
                const elSec = document.getElementById('previewClockSec');

                if (elHourMin) {
                    elHourMin.innerHTML = hh + '<span style="color:' + (blink ? colonColor : 'transparent') + ';">:</span>' + mm;
                }
                if (elSec) {
                    elSec.textContent = ':' + ss;
                }
            }, 1000);
        }

        function selectClockFontPreset(fontKey) {
            document.getElementById('custClockFontFamily').value = fontKey;
            document.querySelectorAll('.clock-font-btn').forEach(function(btn) {
                btn.style.border = '1.5px solid #CBD5E1';
                btn.style.background = '#FFFFFF';
                btn.style.boxShadow = 'none';
            });
            const activeBtn = document.getElementById('fontBtn_' + fontKey);
            if (activeBtn) {
                activeBtn.style.border = '2px solid #0284C7';
                activeBtn.style.background = '#E0F2FE';
                activeBtn.style.boxShadow = '0 0 10px rgba(2,132,199,0.3)';
            }
            const lbl = document.getElementById('previewClockFontLabel');
            if (lbl) lbl.innerHTML = 'Font: <b>' + getClockFontDisplayName(fontKey) + '</b>';
            updateClockLivePreview();
        }

        function selectClockColorPreset(clockColor, colonColor, secColor) {
            document.getElementById('custClockColor').value = clockColor;
            document.getElementById('custClockColorPick').value = clockColor;
            document.getElementById('custClockColonColor').value = colonColor;
            document.getElementById('custClockColonPick').value = colonColor;
            document.getElementById('custClockSecColor').value = secColor;
            document.getElementById('custClockSecPick').value = secColor;
            updateClockLivePreview();
            showToast('Preset warna diterapkan ke pratinjau!');
        }

        function updateClockLivePreview() {
            const fontKey = (document.getElementById('custClockFontFamily') ? document.getElementById('custClockFontFamily').value : 'RADIOLAND').toUpperCase();
            const clockColor = document.getElementById('custClockColor') ? document.getElementById('custClockColor').value.trim() : '#FFFFFF';
            const secColor = document.getElementById('custClockSecColor') ? document.getElementById('custClockSecColor').value.trim() : '#F59E0B';

            let fontCss = 'monospace';
            if (fontKey === 'SERIF') fontCss = 'serif';
            else if (fontKey === 'SANS_SERIF' || fontKey === 'RAJDHANI' || fontKey === 'ORBITRON') fontCss = 'sans-serif';

            const elHourMin = document.getElementById('previewClockHourMin');
            const elSec = document.getElementById('previewClockSec');

            if (elHourMin) {
                elHourMin.style.color = clockColor;
                elHourMin.style.fontFamily = fontCss;
            }
            if (elSec) {
                elSec.style.color = secColor;
                elSec.style.fontFamily = fontCss;
            }
        }

        async function handleSaveClockCustomization(e) {
            e.preventDefault();
            const payload = {
                clockFontFamily: document.getElementById('custClockFontFamily').value.trim() || 'RADIOLAND',
                clockColor: document.getElementById('custClockColor').value.trim() || '#FFFFFF',
                clockColonColor: document.getElementById('custClockColonColor').value.trim() || '#F59E0B',
                clockSecondsColor: document.getElementById('custClockSecColor').value.trim() || '#F59E0B'
            };
            if (clockPreviewInterval) {
                clearInterval(clockPreviewInterval);
                clockPreviewInterval = null;
            }
            await saveContentCustomization(payload, 'Kustomisasi Font & Warna Jam Digital TV Berhasil Disimpan!');
        }

        async function resetClockCustomization() {
            if (!confirm('Kembalikan font dan warna jam digital TV ke setelan bawaan?')) return;
            const payload = {
                clockFontFamily: 'RADIOLAND',
                clockColor: '#FFFFFF',
                clockColonColor: '#F59E0B',
                clockSecondsColor: '#F59E0B'
            };
            if (clockPreviewInterval) {
                clearInterval(clockPreviewInterval);
                clockPreviewInterval = null;
            }
            await saveContentCustomization(payload, 'Kustomisasi Jam Digital TV dikembalikan ke default!');
        }

        // --- KEUANGAN CRUD ---
        window.editingFinId = null;

        function startEditFinance(id, title, amount, type, category, date, notes) {
            const html = '<form onsubmit="handleModalSaveFinance(event, ' + id + ')">' +
                '<div style="margin-bottom:12px;">' +
                    '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Jenis Transaksi</label>' +
                    '<select id="modalFinType" class="form-input" style="width:100%;">' +
                        '<option value="INCOME"' + (type === 'INCOME' || type === 'PEMASUKAN' ? ' selected' : '') + '>🟢 PEMASUKAN (Kas Masuk)</option>' +
                        '<option value="EXPENSE"' + (type === 'EXPENSE' || type === 'PENGELUARAN' ? ' selected' : '') + '>🔴 PENGELUARAN (Kas Keluar)</option>' +
                    '</select>' +
                '</div>' +
                '<div style="margin-bottom:12px;">' +
                    '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Keterangan / Uraian Transaksi</label>' +
                    '<input type="text" id="modalFinTitle" class="form-input" value="' + escapeHtml(title) + '" required style="width:100%;">' +
                '</div>' +
                '<div style="margin-bottom:12px;">' +
                    '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Nominal (Rp)</label>' +
                    '<input type="number" id="modalFinAmount" class="form-input" value="' + (amount || 0) + '" required style="width:100%;">' +
                '</div>' +
                '<div class="grid-2" style="margin-bottom:16px;">' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Kategori</label>' +
                        '<input type="text" id="modalFinCategory" class="form-input" value="' + escapeHtml(category || '') + '" placeholder="Contoh: Kotak Infaq">' +
                    '</div>' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Tanggal</label>' +
                        '<input type="text" id="modalFinDate" class="form-input" value="' + escapeHtml(date || '') + '" placeholder="YYYY-MM-DD">' +
                    '</div>' +
                '</div>' +
                '<div style="display:flex; gap:10px; justify-content:flex-end;">' +
                    '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 16px;">Batal</button>' +
                    '<button type="submit" class="btn-primary" style="width:auto; padding:8px 18px;">💾 Simpan Perubahan</button>' +
                '</div>' +
            '</form>';
            openUniversalModal('✏️ Edit Transaksi Keuangan (ID: ' + id + ')', html);
        }

        async function handleModalSaveFinance(e, id) {
            e.preventDefault();
            const payload = {
                id: id,
                type: document.getElementById('modalFinType').value,
                title: document.getElementById('modalFinTitle').value,
                amount: Math.round(parseFloat(document.getElementById('modalFinAmount').value) || 0),
                category: document.getElementById('modalFinCategory').value,
                date: document.getElementById('modalFinDate').value
            };
            try {
                await fetch('/api/update-finance', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Transaksi Berhasil Diperbarui!');
                closeUniversalModal();
                loadStatus();
            } catch(err) {
                alert('Gagal memperbarui transaksi: ' + err);
            }
        }

        function cancelEditFinance() {
            window.editingFinId = null;
            document.getElementById('finTitle').value = '';
            document.getElementById('finAmount').value = '';
            document.getElementById('finCategory').value = '';
            document.getElementById('finDate').value = '';
            document.getElementById('finFormTitle').innerText = 'Catat Transaksi Keuangan';
            document.getElementById('btnSaveFin').innerText = '➕ Tambah Transaksi';
            document.getElementById('btnCancelFin').style.display = 'none';
        }

        async function saveOrAddFinance(e) {
            e.preventDefault();
            const payload = {
                type: document.getElementById('finType').value,
                title: document.getElementById('finTitle').value,
                amount: document.getElementById('finAmount').value,
                category: document.getElementById('finCategory').value,
                date: document.getElementById('finDate').value
            };
            if (window.editingFinId) {
                payload.id = window.editingFinId;
                await fetch('/api/update-finance', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Transaksi Berhasil Diperbarui!');
                cancelEditFinance();
            } else {
                await fetch('/api/add-finance', { method: 'POST', body: JSON.stringify(payload) });
                document.getElementById('finTitle').value = '';
                document.getElementById('finAmount').value = '';
                showToast('Transaksi Berhasil Dicatat!');
            }
            loadStatus();
        }

        async function deleteFinance(id) {
            if (confirm('Hapus transaksi ini?')) {
                await fetch('/api/delete-finance', { method: 'POST', body: JSON.stringify({ id }) });
                showToast('Transaksi dihapus');
                loadStatus();
            }
        }

        // --- PETUGAS JUMAT & HARI RAYA ---
        function renderFridayWeek(weekNum) {
            window.currentFriWeek = weekNum;
            const upcoming = window.upcomingFridayWeek || 1;
            for (let i = 1; i <= 7; i++) {
                const btn = document.getElementById('btnFri' + i);
                if (btn) {
                    const isLive = (i === upcoming && i <= 5);
                    const label = (i <= 5) ? ('Jum\'at ' + i) : (i === 6 ? '🎉 Idul Fitri' : '🐑 Idul Adha');
                    btn.innerHTML = label + (isLive ? ' <span style="background:#10B981; color:#022C22; font-size:9px; padding:2px 5px; border-radius:4px; font-weight:bold; margin-left:3px;">LIVE TV</span>' : '');
                    if (i === weekNum) btn.classList.add('active');
                    else btn.classList.remove('active');
                }
            }
            const btnSave = document.getElementById('btnSaveFri');
            if (btnSave) {
                if (weekNum === 6) btnSave.innerText = '💾 Simpan Petugas Sholat Idul Fitri & Perbarui TV';
                else if (weekNum === 7) btnSave.innerText = '💾 Simpan Petugas Sholat Idul Adha & Perbarui TV';
                else btnSave.innerText = '💾 Simpan Petugas Jum\'at Ke-' + weekNum + ' & Perbarui TV';
            }

            const alertDiv = document.getElementById('friLiveAlert');
            if (alertDiv) {
                if (weekNum === 6) {
                    alertDiv.innerHTML = '🎉 <strong>Petugas Sholat Hari Raya Idul Fitri (1 Syawal)</strong>. Atur nama dan nomor WhatsApp Khotib, Imam, Muadzin/Pemandu Takbir, dan Bilal Hari Raya.';
                    alertDiv.style.borderColor = '#10B981';
                    alertDiv.style.background = 'rgba(16, 185, 129, 0.15)';
                    alertDiv.style.color = '#A7F3D0';
                } else if (weekNum === 7) {
                    alertDiv.innerHTML = '🐑 <strong>Petugas Sholat Hari Raya Idul Adha (10 Dzulhijjah)</strong>. Atur nama dan nomor WhatsApp Khotib, Imam, Muadzin/Pemandu Takbir, dan Bilal Hari Raya.';
                    alertDiv.style.borderColor = '#F59E0B';
                    alertDiv.style.background = 'rgba(245, 158, 11, 0.15)';
                    alertDiv.style.color = '#FDE68A';
                } else if (weekNum === upcoming) {
                    alertDiv.innerHTML = '🟢 <strong>Jadwal Minggu ' + weekNum + '</strong> adalah jadwal yang <strong>SEDANG TAYANG DI TV</strong> saat ini. Perubahan yang Anda simpan akan langsung terlihat di layar TV!';
                    alertDiv.style.borderColor = '#10B981';
                    alertDiv.style.background = 'rgba(16, 185, 129, 0.15)';
                    alertDiv.style.color = '#A7F3D0';
                } else {
                    alertDiv.innerHTML = 'ℹ️ Anda sedang mengedit <strong>Jadwal Minggu ' + weekNum + '</strong>. Jadwal yang sedang tayang di TV saat ini adalah <strong>Minggu ' + upcoming + '</strong>. Centang kotak di bawah untuk menerapkan ke semua minggu.';
                    alertDiv.style.borderColor = '#F59E0B';
                    alertDiv.style.background = 'rgba(245, 158, 11, 0.15)';
                    alertDiv.style.color = '#FDE68A';
                }
            }

            const applyAllContainer = document.getElementById('friApplyAllContainer');
            if (applyAllContainer) {
                applyAllContainer.style.display = (weekNum >= 6) ? 'none' : 'block';
            }

            const lblKhotib = document.getElementById('lblFriKhotib');
            if (lblKhotib) lblKhotib.innerText = (weekNum === 6) ? '🎙️ Khotib Sholat Idul Fitri' : (weekNum === 7 ? '🎙️ Khotib Sholat Idul Adha' : '🎙️ Nama Khotib Jum\'at');
            const lblImam = document.getElementById('lblFriImam');
            if (lblImam) lblImam.innerText = (weekNum === 6) ? '🕌 Imam Sholat Idul Fitri' : (weekNum === 7 ? '🕌 Imam Sholat Idul Adha' : '🕌 Nama Imam Sholat');
            const lblMuadzin = document.getElementById('lblFriMuadzin');
            if (lblMuadzin) lblMuadzin.innerText = (weekNum >= 6) ? '📢 Muadzin / Pemandu Takbir' : '📢 Nama Muadzin';
            const lblBilal = document.getElementById('lblFriBilal');
            if (lblBilal) lblBilal.innerText = (weekNum >= 6) ? '📜 Bilal / Protokol Acara' : '📜 Nama Bilal / Muraqqi';

            const list = window.allFridayList || [];
            const defaultDate = (weekNum === 6) ? '1 Syawal (Idul Fitri)' : ((weekNum === 7) ? '10 Dzulhijjah (Idul Adha)' : '');
            const defaultHijri = (weekNum === 6) ? '1 Syawal' : ((weekNum === 7) ? '10 Dzulhijjah' : '');
            const fri = list.find(f => f.id === weekNum) || {
                id: weekNum,
                date: defaultDate,
                hijriDate: defaultHijri,
                khotib: '', imam: '', muadzin: '', bilal: '', khutbahTopic: '', notes: ''
            };

            document.getElementById('friDate').value = (fri.date && !fri.date.startsWith("Jum'at Ke-")) ? fri.date : defaultDate;
            document.getElementById('friHijriDate').value = (fri.hijriDate && !fri.hijriDate.startsWith("Jum'at Ke-")) ? fri.hijriDate : defaultHijri;
            document.getElementById('friKhotib').value = fri.khotib || '';
            if (document.getElementById('friKhotibPhone')) document.getElementById('friKhotibPhone').value = fri.khotibPhone || '';
            document.getElementById('friImam').value = fri.imam || '';
            if (document.getElementById('friImamPhone')) document.getElementById('friImamPhone').value = fri.imamPhone || '';
            document.getElementById('friMuadzin').value = fri.muadzin || '';
            document.getElementById('friBilal').value = fri.bilal || '';
            if (document.getElementById('friBilalPhone')) document.getElementById('friBilalPhone').value = fri.bilalPhone || '';
            document.getElementById('friNotes').value = fri.notes || '';

            const hariRayaBox = document.getElementById('friHariRayaConfigBox');
            if (hariRayaBox) {
                if (weekNum >= 6) {
                    hariRayaBox.style.display = 'block';
                    const cfg = window.currentConfig || {};
                    const lblBoxTitle = document.getElementById('lblHariRayaBoxTitle');
                    const chkEnabled = document.getElementById('friHariRayaEnabled');
                    const inpDate = document.getElementById('friHariRayaDate');
                    const inpTime = document.getElementById('friHariRayaTime');
                    const inpIqomah = document.getElementById('friHariRayaIqomah');
                    const inpSholat = document.getElementById('friHariRayaSholat');

                    if (weekNum === 6) {
                        if (lblBoxTitle) lblBoxTitle.innerHTML = '<span>🎉</span> Pengaturan Waktu Pelaksanaan Sholat Idul Fitri';
                        if (chkEnabled) chkEnabled.checked = (cfg.idulFitriEnabled !== undefined) ? cfg.idulFitriEnabled : true;
                        if (inpDate) inpDate.value = cfg.idulFitriDate || '';
                        if (inpTime) inpTime.value = cfg.idulFitriTime || '06:30';
                        if (inpIqomah) inpIqomah.value = cfg.idulFitriIqomahMinutes || 15;
                        if (inpSholat) inpSholat.value = cfg.idulFitriSholatMinutes || 20;
                    } else {
                        if (lblBoxTitle) lblBoxTitle.innerHTML = '<span>🐑</span> Pengaturan Waktu Pelaksanaan Sholat Idul Adha';
                        if (chkEnabled) chkEnabled.checked = (cfg.idulAdhaEnabled !== undefined) ? cfg.idulAdhaEnabled : true;
                        if (inpDate) inpDate.value = cfg.idulAdhaDate || '';
                        if (inpTime) inpTime.value = cfg.idulAdhaTime || '06:30';
                        if (inpIqomah) inpIqomah.value = cfg.idulAdhaIqomahMinutes || 15;
                        if (inpSholat) inpSholat.value = cfg.idulAdhaSholatMinutes || 20;
                    }
                } else {
                    hariRayaBox.style.display = 'none';
                }
            }
        }

        function switchFridayWeek(weekNum) {
            window.userSelectedFridayWeek = true;
            renderFridayWeek(weekNum);
        }

        async function simulateHariRayaTv() {
            const weekNum = window.currentFriWeek || 6;
            const action = (weekNum === 7) ? 'idul_adha_sim' : 'idul_fitri_sim';
            await triggerAction(action);
            showToast('Simulasi Layar Sholat Hari Raya Berhasil Ditampilkan di TV!');
        }

        async function saveFriday(e) {
            e.preventDefault();
            const weekNum = window.currentFriWeek || 1;
            const applyAll = (weekNum <= 5) ? (document.getElementById('friApplyAll')?.checked || false) : false;
            const payload = {
                id: weekNum,
                applyAll: applyAll,
                date: document.getElementById('friDate').value,
                hijriDate: document.getElementById('friHijriDate').value,
                khotib: document.getElementById('friKhotib').value,
                khotibPhone: document.getElementById('friKhotibPhone')?.value || '',
                imam: document.getElementById('friImam').value,
                imamPhone: document.getElementById('friImamPhone')?.value || '',
                muadzin: document.getElementById('friMuadzin').value,
                muadzinPhone: document.getElementById('friMuadzinPhone')?.value || '',
                bilal: document.getElementById('friBilal').value,
                bilalPhone: document.getElementById('friBilalPhone')?.value || '',
                notes: document.getElementById('friNotes').value
            };
            if (weekNum === 6) {
                payload.idulFitriEnabled = document.getElementById('friHariRayaEnabled')?.checked || false;
                payload.idulFitriDate = document.getElementById('friHariRayaDate')?.value || '';
                payload.idulFitriTime = document.getElementById('friHariRayaTime')?.value || '06:30';
                payload.idulFitriIqomahMinutes = parseInt(document.getElementById('friHariRayaIqomah')?.value) || 15;
                payload.idulFitriSholatMinutes = parseInt(document.getElementById('friHariRayaSholat')?.value) || 20;
            } else if (weekNum === 7) {
                payload.idulAdhaEnabled = document.getElementById('friHariRayaEnabled')?.checked || false;
                payload.idulAdhaDate = document.getElementById('friHariRayaDate')?.value || '';
                payload.idulAdhaTime = document.getElementById('friHariRayaTime')?.value || '06:30';
                payload.idulAdhaIqomahMinutes = parseInt(document.getElementById('friHariRayaIqomah')?.value) || 15;
                payload.idulAdhaSholatMinutes = parseInt(document.getElementById('friHariRayaSholat')?.value) || 20;
            }
            await fetch('/api/save-friday', { method: 'POST', body: JSON.stringify(payload) });
            const toastMsg = (weekNum === 6) ? 'Petugas Sholat Idul Fitri Berhasil Disimpan!' : ((weekNum === 7) ? 'Petugas Sholat Idul Adha Berhasil Disimpan!' : 'Petugas Jum\'at Berhasil Disimpan & Diperbarui di TV!');
            showToast(toastMsg);
            loadStatus();
        }

        // --- KEGIATAN CRUD ---
        window.editingActId = null;

        function startEditActivity(id, title, speaker, speakerPhone, date, time, location, category) {
            const html = '<form onsubmit="handleModalSaveActivity(event, ' + id + ')">' +
                '<div style="margin-bottom:12px;">' +
                    '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Nama Agenda / Kajian</label>' +
                    '<input type="text" id="modalActTitle" class="form-input" value="' + escapeHtml(title) + '" required style="width:100%;">' +
                '</div>' +
                '<div class="grid-2" style="margin-bottom:12px;">' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Penceramah / Pengisi / Ustadz</label>' +
                        '<input type="text" id="modalActSpeaker" class="form-input" value="' + escapeHtml(speaker || '') + '" style="width:100%;">' +
                    '</div>' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">No. WhatsApp Pemateri (Reminder)</label>' +
                        '<input type="tel" id="modalActSpeakerPhone" class="form-input" value="' + escapeHtml(speakerPhone || '') + '" placeholder="08123456789" style="width:100%;">' +
                    '</div>' +
                '</div>' +
                '<div class="grid-2" style="margin-bottom:12px;">' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Tanggal</label>' +
                        '<input type="text" id="modalActDate" class="form-input" value="' + escapeHtml(date || '') + '" placeholder="Setiap Ahad Subuh">' +
                    '</div>' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Waktu / Jam</label>' +
                        '<input type="text" id="modalActTime" class="form-input" value="' + escapeHtml(time || '') + '" placeholder="Ba\'da Subuh - Selesai">' +
                    '</div>' +
                '</div>' +
                '<div class="grid-2" style="margin-bottom:16px;">' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Lokasi</label>' +
                        '<input type="text" id="modalActLocation" class="form-input" value="' + escapeHtml(location || '') + '" placeholder="Ruang Utama Masjid">' +
                    '</div>' +
                    '<div>' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Kategori</label>' +
                        '<input type="text" id="modalActCategory" class="form-input" value="' + escapeHtml(category || '') + '" placeholder="Kajian Rutin">' +
                    '</div>' +
                '</div>' +
                '<div style="display:flex; gap:10px; justify-content:flex-end;">' +
                    '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 16px;">Batal</button>' +
                    '<button type="submit" class="btn-primary" style="width:auto; padding:8px 18px;">💾 Simpan Perubahan</button>' +
                '</div>' +
            '</form>';
            openUniversalModal('✏️ Edit Agenda Kegiatan (ID: ' + id + ')', html);
        }

        async function handleModalSaveActivity(e, id) {
            e.preventDefault();
            const payload = {
                id: id,
                title: document.getElementById('modalActTitle').value,
                speaker: document.getElementById('modalActSpeaker').value,
                speakerPhone: document.getElementById('modalActSpeakerPhone')?.value || '',
                date: document.getElementById('modalActDate').value,
                time: document.getElementById('modalActTime').value,
                location: document.getElementById('modalActLocation').value,
                category: document.getElementById('modalActCategory').value
            };
            try {
                await fetch('/api/update-activity', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Agenda Berhasil Diperbarui!');
                closeUniversalModal();
                loadStatus();
            } catch(err) {
                alert('Gagal memperbarui agenda: ' + err);
            }
        }

        function cancelEditActivity() {
            window.editingActId = null;
            document.getElementById('actTitle').value = '';
            document.getElementById('actSpeaker').value = '';
            if (document.getElementById('actSpeakerPhone')) document.getElementById('actSpeakerPhone').value = '';
            document.getElementById('actDate').value = '';
            document.getElementById('actTime').value = '';
            document.getElementById('actLocation').value = '';
            document.getElementById('actCategory').value = '';
            document.getElementById('actFormTitle').innerText = 'Tambah Agenda / Kajian Masjid';
            document.getElementById('btnSaveAct').innerText = '➕ Tambah Agenda';
            document.getElementById('btnCancelAct').style.display = 'none';
        }

        async function saveOrAddActivity(e) {
            e.preventDefault();
            const payload = {
                title: document.getElementById('actTitle').value,
                speaker: document.getElementById('actSpeaker').value,
                speakerPhone: document.getElementById('actSpeakerPhone')?.value || '',
                date: document.getElementById('actDate').value,
                time: document.getElementById('actTime').value,
                location: document.getElementById('actLocation').value,
                category: document.getElementById('actCategory').value
            };
            if (window.editingActId) {
                payload.id = window.editingActId;
                await fetch('/api/update-activity', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Agenda Berhasil Diperbarui!');
                cancelEditActivity();
            } else {
                await fetch('/api/add-activity', { method: 'POST', body: JSON.stringify(payload) });
                document.getElementById('actTitle').value = '';
                document.getElementById('actSpeaker').value = '';
                if (document.getElementById('actSpeakerPhone')) document.getElementById('actSpeakerPhone').value = '';
                showToast('Agenda Berhasil Ditambahkan!');
            }
            loadStatus();
        }

        async function deleteActivity(id) {
            if (confirm('Hapus agenda kegiatan ini?')) {
                await fetch('/api/delete-activity', { method: 'POST', body: JSON.stringify({ id }) });
                showToast('Agenda dihapus');
                loadStatus();
            }
        }

        // --- WHATSAPP GATEWAY HANDLERS ---
        async function saveWaConfig(e) {
            e.preventDefault();
            const payload = {
                enabled: document.getElementById('cfgWaEnabled')?.checked || false,
                token: document.getElementById('cfgWaToken')?.value || '',
                sendHourThursday: parseInt(document.getElementById('cfgWaSendThursdayHour')?.value || '9'),
                sendHourFriday: parseInt(document.getElementById('cfgWaSendFridayHour')?.value || '9'),
                templateThursday: document.getElementById('cfgWaTemplateThursday')?.value || '',
                templateFriday: document.getElementById('cfgWaTemplateFriday')?.value || '',
                templateKajian: document.getElementById('cfgWaTemplateKajian')?.value || ''
            };
            try {
                const res = await fetch('/api/wa-gateway/config', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast(data.message || 'Pengaturan WhatsApp Gateway Berhasil Disimpan!');
                    loadStatus();
                } else {
                    alert('Gagal: ' + (data.message || 'Error saat menyimpan'));
                }
            } catch(err) {
                alert('Gagal menghubungi server: ' + err);
            }
        }

        async function checkWaDeviceStatus() {
            const token = document.getElementById('cfgWaToken')?.value || '';
            const badge = document.getElementById('waStatusBadge');
            const details = document.getElementById('waStatusDetails');
            if (badge) {
                badge.innerText = 'MEMERIKSA KONEKSI...';
                badge.style.background = '#FEF08A';
                badge.style.color = '#854D0E';
            }
            try {
                const res = await fetch('/api/wa-gateway/check-status', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token: token })
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    const devStatus = (data.deviceStatus || '').toLowerCase();
                    if (devStatus === 'connect') {
                        if (badge) {
                            badge.innerText = 'TERHUBUNG (ONLINE)';
                            badge.style.background = '#DCFCE7';
                            badge.style.color = '#15803D';
                        }
                        if (details) {
                            details.innerHTML = '🟢 <strong>Perangkat:</strong> ' + escapeHtml(data.deviceName || '-') + ' (' + escapeHtml(data.devicePhone || '-') + ') | <strong>Sisa Kuota:</strong> ' + escapeHtml(data.quota || '-') + ' pesan | <strong>Kedaluwarsa:</strong> ' + escapeHtml(data.expired || '-');
                        }
                    } else {
                        if (badge) {
                            badge.innerText = 'TERPUTUS (' + (data.deviceStatus || 'DISCONNECT').toUpperCase() + ')';
                            badge.style.background = '#FEE2E2';
                            badge.style.color = '#B91C1C';
                        }
                        if (details) {
                            details.innerHTML = '⚠️ Perangkat belum terhubung ke WhatsApp. Silakan scan QR code di dashboard Fonnte.';
                        }
                    }
                } else {
                    if (badge) {
                        badge.innerText = 'ERROR / TOKEN TIDAK VALID';
                        badge.style.background = '#FEE2E2';
                        badge.style.color = '#B91C1C';
                    }
                    if (details) {
                        details.innerHTML = '❌ ' + escapeHtml(data.message || 'Gagal menghubungi server Fonnte');
                    }
                }
            } catch(err) {
                if (badge) {
                    badge.innerText = 'GAGAL TERHUBUNG';
                    badge.style.background = '#FEE2E2';
                    badge.style.color = '#B91C1C';
                }
                if (details) details.innerHTML = '❌ Kesalahan jaringan: ' + err;
            }
        }

        function sendTestWaModal() {
            const html = '<form onsubmit="handleSendTestWaSubmit(event)">' +
                '<div style="margin-bottom:12px;">' +
                    '<label style="display:block; font-size:12px; font-weight:700; margin-bottom:4px; color:#0F172A;">Nomor WhatsApp Tujuan Uji Coba</label>' +
                    '<input type="tel" id="testWaPhone" class="form-input" placeholder="Contoh: 08123456789 atau 628123456789" required style="width:100%;">' +
                '</div>' +
                '<div style="margin-bottom:16px;">' +
                    '<label style="display:block; font-size:12px; font-weight:700; margin-bottom:4px; color:#0F172A;">Isi Pesan Uji Coba</label>' +
                    '<textarea id="testWaMsg" class="form-input" rows="3" style="width:100%; resize:vertical;">Assalamu\'alaikum. Ini adalah pesan uji coba dari sistem otomatis MasjidKU TV WhatsApp Gateway.</textarea>' +
                '</div>' +
                '<div style="display:flex; gap:10px; justify-content:flex-end;">' +
                    '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 16px;">Batal</button>' +
                    '<button type="submit" class="btn-primary" style="width:auto; padding:8px 18px; background:#16A34A; border-color:#15803D;">📨 Kirim Pesan Uji Coba</button>' +
                '</div>' +
            '</form>';
            openUniversalModal('📨 Tes Kirim Pesan WhatsApp', html);
        }

        async function handleSendTestWaSubmit(e) {
            e.preventDefault();
            const phone = document.getElementById('testWaPhone')?.value || '';
            const msg = document.getElementById('testWaMsg')?.value || '';
            const token = document.getElementById('cfgWaToken')?.value || '';
            try {
                const res = await fetch('/api/wa-gateway/send-test', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ phone: phone, message: msg, token: token })
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast('✅ ' + (data.message || 'Pesan tes berhasil dikirim!'));
                    closeUniversalModal();
                } else {
                    alert('Gagal kirim pesan tes: ' + (data.message || 'Error'));
                }
            } catch(err) {
                alert('Gagal menghubungi server: ' + err);
            }
        }

        async function sendManualFridayBroadcast(type, targetWeekId) {
            const weekId = targetWeekId || window.currentFriWeek || 1;
            let targetLabel = 'Jum\'at Pekan ' + weekId;
            if (weekId === 6) targetLabel = 'Sholat Idul Fitri (1 Syawal)';
            else if (weekId === 7) targetLabel = 'Sholat Idul Adha (10 Dzulhijjah)';

            const typeLabel = (type === 'THURSDAY') ? 'H-1 (Hari Kamis)' : 'Hari H';
            if (!confirm('Kirim pesan pengingat untuk ' + targetLabel + ' (Format ' + typeLabel + ') ke seluruh petugas sekarang?')) {
                return;
            }
            const logBox = document.getElementById('waBroadcastLogBox');
            if (logBox) {
                logBox.style.display = 'block';
                logBox.innerHTML = '⏳ Sedang memproses dan mengirimkan pesan WhatsApp ke petugas ' + targetLabel + '...';
            }
            try {
                const res = await fetch('/api/wa-gateway/send-friday-reminder', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ type: type, weekId: weekId })
                });
                const data = await res.json();
                if (logBox) {
                    logBox.innerHTML = '📋 <strong>Hasil Broadcast:</strong> ' + escapeHtml(data.message || '');
                }
                if (data.status === 'ok') {
                    showToast('✅ ' + (data.message || 'Pesan broadcast berhasil dikirim!'));
                    loadStatus();
                } else {
                    alert('Hasil pengiriman: ' + (data.message || 'Gagal'));
                }
            } catch(err) {
                if (logBox) logBox.innerHTML = '❌ Kesalahan: ' + err;
                alert('Gagal menghubungi server: ' + err);
            }
        }

        async function sendActivityWaReminder(activityId, speaker, phone) {
            if (!phone) {
                alert('Nomor WhatsApp untuk pemateri ' + (speaker || '') + ' belum diisi. Silakan edit agenda dan isi nomor WA terlebih dahulu.');
                return;
            }
            if (!confirm('Kirim pesan pengingat agenda dakwah ke Ustadz ' + (speaker || '') + ' (' + phone + ') sekarang?')) {
                return;
            }
            try {
                const res = await fetch('/api/wa-gateway/send-activity-reminder', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id: activityId })
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast('✅ ' + (data.message || 'Pesan pengingat kajian berhasil dikirim!'));
                } else {
                    alert('Gagal kirim pesan: ' + (data.message || 'Error'));
                }
            } catch(err) {
                alert('Gagal menghubungi server: ' + err);
            }
        }

        // --- RUNNING TEXT CRUD ---
        window.editingRunningId = null;

        function startEditRunningText(id, text) {
            const html = '<form onsubmit="handleModalSaveRunningText(event, ' + id + ')">' +
                '<div style="margin-bottom:16px;">' +
                    '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:6px; color:#0F172A; font-weight:700;">Isi Teks Berjalan (Running Text)</label>' +
                    '<textarea id="modalRunText" class="form-input" rows="4" style="width:100%; resize:vertical;" placeholder="Tuliskan isi teks berjalan / pengumuman masjid di sini..." required>' + escapeHtml(text) + '</textarea>' +
                '</div>' +
                '<div style="display:flex; gap:10px; justify-content:flex-end;">' +
                    '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 16px;">Batal</button>' +
                    '<button type="submit" class="btn-primary" style="width:auto; padding:8px 18px;">💾 Simpan Perubahan</button>' +
                '</div>' +
            '</form>';
            openUniversalModal('✏️ Edit Running Text (ID: ' + id + ')', html);
        }

        async function handleModalSaveRunningText(e, id) {
            e.preventDefault();
            const payload = {
                id: id,
                text: document.getElementById('modalRunText').value
            };
            try {
                await fetch('/api/update-running-text', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Teks Berjalan Berhasil Diperbarui!');
                closeUniversalModal();
                loadStatus();
            } catch(err) {
                alert('Gagal memperbarui teks berjalan: ' + err);
            }
        }

        function cancelEditRunningText() {
            window.editingRunningId = null;
            document.getElementById('runText').value = '';
            document.getElementById('runningFormTitle').innerText = 'Tambah Running Text / Pengumuman';
            document.getElementById('runningInputLabel').innerText = 'Teks Berjalan Baru';
            document.getElementById('btnSaveRunning').innerText = '➕ Tambah Teks Berjalan';
            document.getElementById('btnCancelRunning').style.display = 'none';
        }

        async function saveOrAddRunningText(e) {
            e.preventDefault();
            const text = document.getElementById('runText').value;
            if (window.editingRunningId) {
                const payload = {
                    id: window.editingRunningId,
                    text: text
                };
                await fetch('/api/update-running-text', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Teks Berjalan Berhasil Diperbarui!');
                cancelEditRunningText();
            } else {
                const payload = { text: text };
                await fetch('/api/add-running-text', { method: 'POST', body: JSON.stringify(payload) });
                document.getElementById('runText').value = '';
                showToast('Teks Berjalan Berhasil Ditambahkan!');
            }
            loadStatus();
        }

        async function deleteRunningText(id) {
            if (confirm('Hapus running text ini?')) {
                await fetch('/api/delete-running-text', { method: 'POST', body: JSON.stringify({ id }) });
                showToast('Teks berjalan dihapus');
                loadStatus();
            }
        }

        async function saveRunningFontSize() {
            const size = parseInt(document.getElementById('cfgRunningFontSize').value) || 20;
            await fetch('/api/save-config', {
                method: 'POST',
                body: JSON.stringify({ runningTextFontSize: size })
            });
            showToast('Ukuran Running Text TV Diperbarui (' + size + 'sp)!');
            loadStatus();
        }

        async function triggerAction(action) {
            await fetch('/api/trigger-' + action, { method: 'POST' });
            showToast('Aksi ' + action + ' dikirim ke Layar TV!');
        }

        // Initialize city dropdown options
        const cityData = ${IndonesiaCityData.CITIES.joinToString(prefix = "[", postfix = "]") { c ->
            "\"${c.displayName}|${c.latitude}|${c.longitude}|${c.timezone}|${c.name} (${c.province}) - ${c.getTimezoneName()}\""
        }};
        const selectElem = document.getElementById('cfgCitySelect');
        cityData.forEach(function(item) {
            const p = item.split('|');
            const opt = document.createElement('option');
            opt.value = p[0] + '|' + p[1] + '|' + p[2] + '|' + p[3];
            opt.innerText = p[4];
            selectElem.appendChild(opt);
        });

        // --- AUTH & SECURITY FUNCTIONS ---
        function togglePinVisibility() {
            const pinInp = document.getElementById('inputPin');
            pinInp.type = pinInp.type === 'password' ? 'text' : 'password';
        }

        async function handleLoginSubmit(e) {
            e.preventDefault();
            const btn = document.getElementById('btnLoginSubmit');
            const errBox = document.getElementById('loginError');
            errBox.style.display = 'none';
            btn.disabled = true;
            btn.innerText = 'Memverifikasi PIN...';

            const pin = document.getElementById('inputPin').value;
            try {
                const res = await _nativeFetch('/api/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ password: pin })
                });
                const data = await res.json();
                if (res.ok && data.status === 'ok' && data.token) {
                    localStorage.setItem('masjidku_session_token', data.token);
                    document.getElementById('inputPin').value = '';
                    showDashboard();
                    showToast(data.message || 'Login berhasil! Selamat datang.');
                    loadStatus();
                } else {
                    errBox.innerText = data.message || 'PIN salah! Silakan periksa kembali.';
                    errBox.style.display = 'block';
                }
            } catch (err) {
                errBox.innerText = 'Gagal menghubungi server TV. Pastikan TV menyala dan di Wi-Fi yang sama.';
                errBox.style.display = 'block';
            } finally {
                btn.disabled = false;
                btn.innerText = '🔑 Masuk ke Web Remote';
            }
        }

        async function handleLogout() {
            if (confirm('Kunci dan keluar dari Web Remote MasjidKU?')) {
                try {
                    await fetch('/api/logout', { method: 'POST' });
                } catch (_) {}
                localStorage.removeItem('masjidku_session_token');
                showLoginSection();
                showToast('Anda telah keluar dari sesi Web Remote.');
            }
        }

        async function handleChangePasswordSubmit(e) {
            e.preventDefault();
            const oldPass = document.getElementById('pwdOld').value;
            const newPass = document.getElementById('pwdNew').value;
            const confirmPass = document.getElementById('pwdConfirm').value;
            const alertBox = document.getElementById('changePassAlert');

            if (newPass !== confirmPass) {
                alertBox.style.display = 'block';
                alertBox.style.background = '#FEE2E2';
                alertBox.style.color = '#DC2626';
                alertBox.style.border = '1px solid #FCA5A5';
                alertBox.innerText = 'Konfirmasi PIN baru tidak cocok!';
                return;
            }

            try {
                const res = await fetch('/api/change-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        oldPassword: oldPass,
                        newPassword: newPass,
                        confirmPassword: confirmPass
                    })
                });
                const data = await res.json();
                alertBox.style.display = 'block';
                if (data.status === 'ok') {
                    alertBox.style.background = '#DCFCE7';
                    alertBox.style.color = '#166534';
                    alertBox.style.border = '1px solid #86EFAC';
                    alertBox.innerText = '✅ ' + data.message;
                    document.getElementById('formChangePassword').reset();
                    showToast('PIN berhasil diperbarui!');
                } else {
                    alertBox.style.background = '#FEE2E2';
                    alertBox.style.color = '#DC2626';
                    alertBox.style.border = '1px solid #FCA5A5';
                    alertBox.innerText = '❌ ' + (data.message || 'Gagal mengubah PIN');
                }
            } catch (err) {
                alertBox.style.display = 'block';
                alertBox.style.background = '#FEE2E2';
                alertBox.style.color = '#DC2626';
                alertBox.innerText = 'Terjadi kesalahan jaringan saat menyimpan PIN.';
            }
        }

        // ==========================================
        // FITUR VIDEO LOKAL
        // ==========================================
        function onVideoFileSelected(input) {
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const sizeMb = (file.size / (1024 * 1024)).toFixed(2);
            const info = document.getElementById('videoSelectedInfo');
            if (info) {
                info.style.display = 'block';
                info.innerHTML = '🎬 <strong>File Siap Di-upload:</strong> ' + escapeHtml(file.name) + ' <span style="background:#059669; color:#fff; font-size:10px; padding:2px 6px; border-radius:10px; margin-left:6px;">' + sizeMb + ' MB</span>';
            }
            const titleInput = document.getElementById('inputVideoTitle');
            if (titleInput && !titleInput.value.trim()) {
                let clean = file.name.replace(/\.[^/.]+$/, "").replace(/[_-]/g, " ").trim();
                titleInput.value = clean;
            }
        }

        async function loadVideoList() {
            const container = document.getElementById('videoListContainer');
            if (!container) return;
            try {
                const res = await fetch('/api/videos');
                const data = await res.json();
                const videos = data.videos || [];
                if (videos.length === 0) {
                    container.innerHTML = '<div style="text-align:center; padding:24px; color:#64748b; font-size:13px; background:#f8fafc; border-radius:8px; border:1px dashed #cbd5e1;"><p style="font-weight:600; font-size:14px; margin-bottom:4px;">Belum Ada Video Tersimpan</p><p style="font-size:12px;">Gunakan form di atas untuk mengunggah file video MP4/MKV ke TV.</p></div>';
                    return;
                }

                let html = '<div style="display:flex; flex-direction:column; gap:12px;">';
                videos.forEach(v => {
                    const isBg = v.isBackgroundActive;
                    const bgBadge = isBg ? '<span style="background:#059669; color:#fff; font-size:11px; padding:3px 8px; border-radius:12px; font-weight:700;">🟢 Background Aktif</span>' : '';

                    html += '<div style="background:#fff; border:1px solid ' + (isBg ? '#10b981' : '#e2e8f0') + '; border-radius:10px; padding:14px; box-shadow:0 1px 3px rgba(0,0,0,0.05);">';
                    html += '  <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">';
                    html += '    <div>';
                    html += '      <h4 style="font-size:14px; font-weight:700; color:#0f172a; margin-bottom:2px;">🎬 ' + escapeHtml(v.title) + ' ' + bgBadge + '</h4>';
                    html += '      <span style="font-size:11.5px; color:#64748b;">' + escapeHtml(v.fileName) + ' • ' + v.formattedSize + '</span>';
                    html += '    </div>';
                    html += '    <button type="button" class="btn-danger" style="padding:4px 8px; font-size:11px;" onclick="deleteVideoFile(\'' + escapeHtml(v.fileName) + '\')">🗑️ Hapus</button>';
                    html += '  </div>';

                    html += '  <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:10px;">';
                    html += '    <button type="button" class="btn-primary" style="flex:1; min-width:140px; padding:8px 12px; font-size:12px;" onclick="playFullscreenVideo(\'' + escapeHtml(v.filePath) + '\', \'' + escapeHtml(v.title) + '\', false)">▶️ Putar Fullscreen (Bersuara)</button>';
                    html += '    <button type="button" class="btn-primary" style="flex:1; min-width:140px; padding:8px 12px; font-size:12px; background:#475569;" onclick="playFullscreenVideo(\'' + escapeHtml(v.filePath) + '\', \'' + escapeHtml(v.title) + '\', true)">🔇 Putar Fullscreen (Senyap)</button>';

                    if (isBg) {
                        html += '    <button type="button" class="btn-danger" style="flex:1; min-width:140px; padding:8px 12px; font-size:12px;" onclick="setBackgroundVideo(\'\', false)">❌ Lepas Background</button>';
                    } else {
                        html += '    <button type="button" class="btn-primary" style="flex:1; min-width:140px; padding:8px 12px; font-size:12px; background:#0284c7;" onclick="setBackgroundVideo(\'' + escapeHtml(v.filePath) + '\', true)">🖼️ Jadikan Background TV</button>';
                    }
                    html += '  </div>';
                    html += '</div>';
                });
                html += '</div>';
                container.innerHTML = html;
            } catch (e) {
                console.error(e);
                container.innerHTML = '<p style="color:#ef4444; font-size:12px; text-align:center;">Gagal memuat daftar video</p>';
            }
        }

        async function uploadVideoFile() {
            const fileInput = document.getElementById('inputVideoFile');
            if (!fileInput.files || fileInput.files.length === 0) {
                alert('Silakan pilih file video MP4/MKV/WebM terlebih dahulu!');
                return;
            }
            const file = fileInput.files[0];
            if (file.size > 500 * 1024 * 1024) {
                alert('Ukuran video melebihi batas maksimal (500 MB)!');
                return;
            }

            let title = document.getElementById('inputVideoTitle').value.trim();
            if (!title) {
                title = file.name.replace(/\.[^/.]+$/, "").replace(/[_-]/g, " ").trim() || 'Video Masjid';
            }

            const btn = document.getElementById('btnUploadVideo');
            const progressContainer = document.getElementById('videoUploadProgressContainer');
            const progressBar = document.getElementById('videoUploadProgressBar');
            const progressLabel = document.getElementById('videoUploadProgressLabel');
            const progressPercent = document.getElementById('videoUploadProgressPercent');

            btn.disabled = true;
            btn.innerText = '⏳ Mengunggah Video ke TV...';
            if (progressContainer) progressContainer.style.display = 'block';
            if (progressBar) progressBar.style.width = '0%';
            if (progressPercent) progressPercent.innerText = '0%';
            if (progressLabel) progressLabel.innerText = 'Mengunggah (' + (file.size / (1024 * 1024)).toFixed(1) + ' MB)...';

            const token = localStorage.getItem('masjidku_session_token') || '';
            const ext = file.name.split('.').pop().toLowerCase() || 'mp4';
            const uploadUrl = '/api/upload-video?title=' + encodeURIComponent(title) +
                '&ext=' + encodeURIComponent(ext) +
                '&size=' + file.size +
                '&token=' + encodeURIComponent(token);

            const xhr = new XMLHttpRequest();
            xhr.open('POST', uploadUrl, true);
            xhr.setRequestHeader('X-Auth-Token', token);
            xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            xhr.setRequestHeader('X-Video-Title', encodeURIComponent(title));
            xhr.setRequestHeader('X-Video-Ext', ext);
            xhr.setRequestHeader('X-Video-Size', file.size.toString());

            xhr.upload.onprogress = function (e) {
                if (e.lengthComputable) {
                    const percent = Math.round((e.loaded / e.total) * 100);
                    if (progressBar) progressBar.style.width = percent + '%';
                    if (progressPercent) progressPercent.innerText = percent + '%';
                    if (progressLabel) progressLabel.innerText = 'Mengunggah: ' + ((e.loaded / (1024 * 1024)).toFixed(1)) + ' / ' + ((e.total / (1024 * 1024)).toFixed(1)) + ' MB (' + percent + '%)';
                }
            };

            xhr.onload = function () {
                btn.disabled = false;
                btn.innerText = '🚀 Upload Video ke Penyimpanan TV';
                setTimeout(() => {
                    if (progressContainer) progressContainer.style.display = 'none';
                    if (progressBar) progressBar.style.width = '0%';
                }, 2000);

                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const data = JSON.parse(xhr.responseText);
                        if (data.status === 'ok') {
                            showToast('✅ ' + (data.message || 'Video berhasil di-upload ke TV!'));
                            fileInput.value = '';
                            document.getElementById('inputVideoTitle').value = '';
                            document.getElementById('videoSelectedInfo').style.display = 'none';
                            loadVideoList();
                            loadStatus();
                        } else {
                            alert('Gagal: ' + (data.message || 'Terjadi kesalahan saat upload'));
                        }
                    } catch (_) {
                        showToast('✅ Video berhasil dikirim ke TV!');
                        loadVideoList();
                        loadStatus();
                    }
                } else {
                    alert('Gagal mengunggah video (HTTP ' + xhr.status + '). Periksa koneksi WiFi TV.');
                }
            };

            xhr.onerror = function () {
                btn.disabled = false;
                btn.innerText = '🚀 Upload Video ke Penyimpanan TV';
                if (progressContainer) progressContainer.style.display = 'none';
                alert('Gagal mengirim file video. Pastikan HP dan TV terhubung pada jaringan WiFi yang sama.');
            };

            xhr.send(file);
        }

        async function playFullscreenVideo(filePath, title, isMuted) {
            try {
                const res = await fetch('/api/play-fullscreen-video', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'filePath=' + encodeURIComponent(filePath) + '&title=' + encodeURIComponent(title) + '&isMuted=' + (isMuted ? 'true' : 'false')
                });
                const data = await res.json();
                showToast(data.message || 'Menayangkan video di layar TV');
                setTimeout(loadStatus, 300);
            } catch (e) {
                console.error(e);
                showToast('Gagal memutar video di TV');
            }
        }

        async function stopFullscreenVideo() {
            try {
                const res = await fetch('/api/stop-fullscreen-video', { method: 'POST' });
                const data = await res.json();
                showToast(data.message || 'Tayangan video dihentikan');
                setTimeout(loadStatus, 300);
            } catch (e) {
                console.error(e);
                showToast('Gagal menghentikan video');
            }
        }

        async function setBackgroundVideo(filePath, enable) {
            try {
                const res = await fetch('/api/set-background-video', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'filePath=' + encodeURIComponent(filePath) + '&enabled=' + (enable ? 'true' : 'false')
                });
                const data = await res.json();
                showToast(data.message || 'Background TV diperbarui');
                loadVideoList();
                setTimeout(loadStatus, 300);
            } catch (e) {
                console.error(e);
                showToast('Gagal mengatur background video');
            }
        }

        async function deleteVideoFile(fileName) {
            if (!confirm('Yakin ingin menghapus file video "' + fileName + '" dari TV?')) return;
            try {
                const res = await fetch('/api/delete-video', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: 'fileName=' + encodeURIComponent(fileName)
                });
                const data = await res.json();
                showToast(data.message || 'Video berhasil dihapus');
                loadVideoList();
                setTimeout(loadStatus, 300);
            } catch (e) {
                console.error(e);
                showToast('Gagal menghapus video');
            }
        }

        function showLoginSection() {
            document.getElementById('loginSection').style.display = 'flex';
            document.getElementById('dashboardSection').style.display = 'none';
            setTimeout(() => {
                const inp = document.getElementById('inputPin');
                if (inp) inp.focus();
            }, 100);
        }

        function showDashboard() {
            document.getElementById('loginSection').style.display = 'none';
            document.getElementById('dashboardSection').style.display = 'block';
        }

        async function checkAuthAndInit() {
            const token = localStorage.getItem('masjidku_session_token');
            if (!token) {
                showLoginSection();
                try {
                    const res = await _nativeFetch('/api/status');
                    const data = await res.json();
                    if (data.config && data.config.mosqueName) {
                        document.getElementById('loginMosqueName').innerText = data.config.mosqueName;
                    }
                } catch (_) {}
            } else {
                try {
                    const res = await _nativeFetch('/api/check-session', {
                        headers: { 'Authorization': 'Bearer ' + token, 'X-Auth-Token': token }
                    });
                    const data = await res.json();
                    if (data.status === 'ok' || data.authenticated) {
                        showDashboard();
                        loadStatus();
                    } else {
                        localStorage.removeItem('masjidku_session_token');
                        showLoginSection();
                    }
                } catch (_) {
                    showDashboard();
                    loadStatus();
                }
            }
        }

        // --- TARAWIH RAMADHAN JAVASCRIPT FUNCTIONS ---
        window.currentTarawihList = [];
        window.currentTarawihActiveNight = 1;
        window.selectedTarawihNight = 1;

        function toggleTarawihModeDisplay() {
            const isAuto = document.getElementById('cfgTarawihAutoDetect').value === 'true';
            const groupManual = document.getElementById('groupTarawihManualNight');
            if (groupManual) groupManual.style.display = isAuto ? 'none' : 'block';
        }

        function renderTarawih(list, activeNight, cfg) {
            window.currentTarawihList = list || [];
            window.currentTarawihActiveNight = activeNight || 1;
            if (!window.selectedTarawihNight) window.selectedTarawihNight = activeNight || 1;

            // Global Config Elements
            const chkEnabled = document.getElementById('cfgTarawihEnabled');
            if (chkEnabled) chkEnabled.checked = cfg.tarawihEnabled !== false;

            const selAuto = document.getElementById('cfgTarawihAutoDetect');
            if (selAuto) selAuto.value = (cfg.tarawihAutoDetectNight !== false) ? 'true' : 'false';

            const selManual = document.getElementById('cfgTarawihManualNight');
            if (selManual) {
                let opts = '';
                for (let n = 1; n <= 30; n++) {
                    opts += '<option value="' + n + '"' + (cfg.tarawihManualNight === n ? ' selected' : '') + '>Malam ke-' + n + ' Ramadhan</option>';
                }
                selManual.innerHTML = opts;
            }
            toggleTarawihModeDisplay();

            const chkSlide = document.getElementById('cfgTarawihShowSlide');
            if (chkSlide) chkSlide.checked = cfg.tarawihShowSlide !== false;

            const numKultum = document.getElementById('cfgTarawihKultumMinutes');
            if (numKultum) numKultum.value = cfg.tarawihKultumMinutes || 15;

            const numSholat = document.getElementById('cfgTarawihSholatMinutes');
            if (numSholat) numSholat.value = cfg.tarawihSholatMinutes || 20;

            const selPreset = document.getElementById('cfgTarawihBgPreset');
            if (selPreset) selPreset.value = cfg.tarawihBgPreset || 'DEFAULT_ISLAMIC';

            // Populate Quick Select Dropdown
            const quickSel = document.getElementById('selQuickNight');
            if (quickSel) {
                let qOpts = '';
                for (let n = 1; n <= 30; n++) {
                    const isCur = (n === window.selectedTarawihNight);
                    const isAct = (n === window.currentTarawihActiveNight);
                    qOpts += '<option value="' + n + '"' + (isCur ? ' selected' : '') + '>Malam ke-' + n + (isAct ? ' (🟢 Aktif di TV)' : '') + '</option>';
                }
                quickSel.innerHTML = qOpts;
            }

            // Populate Night Pills Bar
            const pillsContainer = document.getElementById('tarawihNightPills');
            if (pillsContainer) {
                let pillsHtml = '';
                for (let n = 1; n <= 30; n++) {
                    const isCur = (n === window.selectedTarawihNight);
                    const isAct = (n === window.currentTarawihActiveNight);
                    const bg = isCur ? '#F59E0B' : (isAct ? 'rgba(16,185,129,0.3)' : 'rgba(255,255,255,0.08)');
                    const color = isCur ? '#06281D' : '#FFFFFF';
                    const border = isCur ? '#F59E0B' : (isAct ? '#10B981' : 'rgba(255,255,255,0.15)');
                    const badge = isAct ? ' <span style="font-size:9px; background:#10B981; color:#06281D; padding:1px 4px; border-radius:4px; font-weight:800;">LIVE</span>' : '';

                    pillsHtml += '<button type="button" onclick="switchTarawihNight(' + n + ')" style="padding:6px 12px; font-size:12px; font-weight:700; border-radius:8px; white-space:nowrap; cursor:pointer; background:' + bg + '; color:' + color + '; border:1.5px solid ' + border + '; display:inline-flex; align-items:center; gap:4px;">' +
                        'Malam ' + n + badge +
                    '</button>';
                }
                pillsContainer.innerHTML = pillsHtml;
            }

            // Populate Form for Selected Night
            renderTarawihFormForNight(window.selectedTarawihNight);

            // Populate Summary Table
            renderTarawihTable(window.currentTarawihList);
        }

        function switchTarawihNight(nightNum) {
            window.selectedTarawihNight = nightNum;
            const quickSel = document.getElementById('selQuickNight');
            if (quickSel) quickSel.value = nightNum;

            // Re-render pills highlight
            const pillsContainer = document.getElementById('tarawihNightPills');
            if (pillsContainer) {
                const buttons = pillsContainer.querySelectorAll('button');
                buttons.forEach((btn, idx) => {
                    const n = idx + 1;
                    const isCur = (n === window.selectedTarawihNight);
                    const isAct = (n === window.currentTarawihActiveNight);
                    btn.style.background = isCur ? '#F59E0B' : (isAct ? 'rgba(16,185,129,0.3)' : 'rgba(255,255,255,0.08)');
                    btn.style.color = isCur ? '#06281D' : '#FFFFFF';
                    btn.style.borderColor = isCur ? '#F59E0B' : (isAct ? '#10B981' : 'rgba(255,255,255,0.15)');
                });
            }

            renderTarawihFormForNight(nightNum);
        }

        function renderTarawihFormForNight(nightNum) {
            const list = window.currentTarawihList || [];
            const item = list.find(t => t.night === nightNum) || {
                night: nightNum,
                date: 'Malam ke-' + nightNum + ' Ramadhan',
                penceramah: '', penceramahPhone: '', judulKultum: '',
                imamTarawih: '', imamTarawihPhone: '',
                imamWitir: '', imamWitirPhone: '',
                bilalTarawih: '', bilalTarawihPhone: '',
                notes: ''
            };

            document.getElementById('tarawihFormNight').value = nightNum;
            document.getElementById('tarawihDate').value = item.date || ('Malam ke-' + nightNum + ' Ramadhan');
            document.getElementById('tarawihPenceramah').value = item.penceramah || '';
            document.getElementById('tarawihPenceramahPhone').value = item.penceramahPhone || '';
            document.getElementById('tarawihJudulKultum').value = item.judulKultum || '';
            document.getElementById('tarawihImamTarawih').value = item.imamTarawih || '';
            document.getElementById('tarawihImamTarawihPhone').value = item.imamTarawihPhone || '';
            document.getElementById('tarawihImamWitir').value = item.imamWitir || '';
            document.getElementById('tarawihImamWitirPhone').value = item.imamWitirPhone || '';
            document.getElementById('tarawihBilal').value = item.bilalTarawih || '';
            document.getElementById('tarawihBilalPhone').value = item.bilalTarawihPhone || '';
            document.getElementById('tarawihNotes').value = item.notes || '';

            const isAct = (nightNum === window.currentTarawihActiveNight);
            const alertText = document.getElementById('tarawihNightAlertText');
            const alertBadge = document.getElementById('tarawihNightLiveBadge');
            const alertBox = document.getElementById('tarawihNightAlert');

            if (alertText) {
                alertText.innerHTML = '🌙 <strong>Jadwal Petugas Malam ke-' + nightNum + ' Ramadhan</strong>' + (isAct ? ' (Sedang Tayang di Layar TV)' : '');
            }
            if (alertBadge) {
                alertBadge.style.display = isAct ? 'inline-block' : 'none';
            }
            if (alertBox) {
                if (isAct) {
                    alertBox.style.background = 'rgba(16,185,129,0.18)';
                    alertBox.style.borderColor = '#10B981';
                    alertBox.style.color = '#A7F3D0';
                } else {
                    alertBox.style.background = 'rgba(245,158,11,0.15)';
                    alertBox.style.borderColor = '#F59E0B';
                    alertBox.style.color = '#FDE68A';
                }
            }
        }

        function renderTarawihTable(list) {
            const tbody = document.getElementById('tarawihTableBody');
            if (!tbody) return;

            let html = '';
            for (let n = 1; n <= 30; n++) {
                const item = list.find(t => t.night === n) || {
                    night: n,
                    date: 'Malam ke-' + n + ' Ramadhan',
                    penceramah: '-', judulKultum: '-',
                    imamTarawih: '-', imamWitir: '-',
                    bilalTarawih: '-'
                };

                const isAct = (n === window.currentTarawihActiveNight);
                const isCur = (n === window.selectedTarawihNight);
                const rowBg = isCur ? 'rgba(245,158,11,0.1)' : (isAct ? 'rgba(16,185,129,0.1)' : '');

                const penceramahText = (item.penceramah && item.penceramah !== '-') ? ('<strong>' + escapeHtml(item.penceramah) + '</strong>' + (item.judulKultum ? '<div style="font-size:11px; color:#FDE68A;">📖 ' + escapeHtml(item.judulKultum) + '</div>' : '')) : '<span style="color:#64748B;">-</span>';
                const imamText = (item.imamTarawih && item.imamTarawih !== '-') ? ('🕌 ' + escapeHtml(item.imamTarawih) + (item.imamWitir ? '<div style="font-size:11px; color:#94A3B8;">🌙 Witir: ' + escapeHtml(item.imamWitir) + '</div>' : '')) : '<span style="color:#64748B;">-</span>';
                const bilalText = (item.bilalTarawih && item.bilalTarawih !== '-') ? ('📢 ' + escapeHtml(item.bilalTarawih)) : '<span style="color:#64748B;">-</span>';

                html += '<tr style="' + (rowBg ? 'background:' + rowBg + ';' : '') + '">' +
                    '<td style="font-weight:bold; color:' + (isAct ? '#10B981' : '#FDE68A') + ';">Malam ' + n + (isAct ? ' 🟢' : '') + '</td>' +
                    '<td style="font-size:11.5px;">' + escapeHtml(item.date || ('Malam ke-' + n)) + '</td>' +
                    '<td>' + penceramahText + '</td>' +
                    '<td>' + imamText + '</td>' +
                    '<td>' + bilalText + '</td>' +
                    '<td style="text-align:center;">' +
                        '<button type="button" class="btn-secondary" onclick="switchTarawihNight(' + n + '); document.getElementById(\'tab-tarawih\').scrollIntoView({behavior:\'smooth\'});" style="font-size:11px; padding:4px 8px; margin-right:4px;">✏️ Edit</button>' +
                        '<button type="button" class="btn-whatsapp" onclick="sendWaTarawihBroadcast(' + n + ')" style="font-size:11px; padding:4px 6px; background:#25D366; color:#06281D; border:none; border-radius:4px; font-weight:bold; cursor:pointer;" title="Kirim WA Petugas Malam ' + n + '">📱</button>' +
                    '</td>' +
                '</tr>';
            }
            tbody.innerHTML = html;
        }

        function filterTarawihTable() {
            const q = (document.getElementById('tarawihSearchInput').value || '').toLowerCase().trim();
            const rows = document.querySelectorAll('#tableTarawih tbody tr');
            rows.forEach(r => {
                const text = r.innerText.toLowerCase();
                r.style.display = (!q || text.includes(q)) ? '' : 'none';
            });
        }

        async function saveTarawihNightForm(e) {
            e.preventDefault();
            const night = parseInt(document.getElementById('tarawihFormNight').value) || 1;
            const payload = {
                night: night,
                date: document.getElementById('tarawihDate').value.trim() || ('Malam ke-' + night + ' Ramadhan'),
                penceramah: document.getElementById('tarawihPenceramah').value.trim(),
                penceramahPhone: document.getElementById('tarawihPenceramahPhone').value.trim(),
                judulKultum: document.getElementById('tarawihJudulKultum').value.trim(),
                imamTarawih: document.getElementById('tarawihImamTarawih').value.trim(),
                imamTarawihPhone: document.getElementById('tarawihImamTarawihPhone').value.trim(),
                imamWitir: document.getElementById('tarawihImamWitir').value.trim(),
                imamWitirPhone: document.getElementById('tarawihImamWitirPhone').value.trim(),
                bilalTarawih: document.getElementById('tarawihBilal').value.trim(),
                bilalTarawihPhone: document.getElementById('tarawihBilalPhone').value.trim(),
                notes: document.getElementById('tarawihNotes').value.trim()
            };

            try {
                const res = await fetch('/api/save-tarawih', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(payload).toString()
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast('✅ Petugas Tarawih Malam ke-' + night + ' berhasil disimpan!');
                    await loadStatus();
                } else {
                    alert('Gagal menyimpan: ' + (data.message || 'Terjadi kesalahan'));
                }
            } catch (err) {
                alert('Gagal koneksi ke server: ' + err.message);
            }
        }

        async function saveTarawihConfigForm(e) {
            e.preventDefault();
            const payload = {
                isConfigOnly: 'true',
                tarawihEnabled: document.getElementById('cfgTarawihEnabled').checked ? 'true' : 'false',
                tarawihAutoDetectNight: document.getElementById('cfgTarawihAutoDetect').value === 'true' ? 'true' : 'false',
                tarawihManualNight: document.getElementById('cfgTarawihManualNight').value || '1',
                tarawihShowSlide: document.getElementById('cfgTarawihShowSlide').checked ? 'true' : 'false',
                tarawihKultumMinutes: document.getElementById('cfgTarawihKultumMinutes').value || '15',
                tarawihSholatMinutes: document.getElementById('cfgTarawihSholatMinutes').value || '20',
                tarawihBgPreset: document.getElementById('cfgTarawihBgPreset').value || 'DEFAULT_ISLAMIC'
            };

            try {
                const res = await fetch('/api/save-tarawih', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(payload).toString()
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast('✅ Pengaturan Tarawih Ramadhan berhasil disimpan!');
                    await loadStatus();
                } else {
                    alert('Gagal menyimpan: ' + (data.message || 'Terjadi kesalahan'));
                }
            } catch (err) {
                alert('Gagal koneksi: ' + err.message);
            }
        }

        async function populateDefaultTarawih() {
            if (!confirm('Auto-isi 30 malam Tarawih dengan format dan tanggal default Ramadhan? Jadwal yang sudah ada akan disesuaikan.')) return;
            try {
                const res = await fetch('/api/populate-tarawih', { method: 'POST' });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast('✨ 30 Malam Tarawih berhasil di-generate!');
                    await loadStatus();
                } else {
                    alert('Gagal generate: ' + (data.message || 'Error'));
                }
            } catch (err) {
                alert('Gagal koneksi: ' + err.message);
            }
        }

        async function simulateTarawihTv() {
            try {
                const res = await fetch('/api/trigger-tarawih_sim', { method: 'POST' });
                const data = await res.json();
                if (data.status === 'ok') {
                    showToast('📺 Layar Simulasi Tarawih sedang tampil di TV!');
                } else {
                    alert('Gagal simulasi: ' + (data.message || 'Error'));
                }
            } catch (err) {
                alert('Gagal koneksi: ' + err.message);
            }
        }

        async function sendWaTarawihBroadcast(nightNum) {
            const n = nightNum || parseInt(document.getElementById('tarawihFormNight').value) || 1;
            if (!confirm('Kirim pesan WhatsApp pengingat kepada seluruh petugas Sholat Tarawih Malam ke-' + n + '?')) return;

            try {
                const res = await fetch('/api/wa-gateway/send-tarawih-reminder', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ night: n }).toString()
                });
                const data = await res.json();
                if (data.status === 'ok') {
                    alert('✅ ' + data.message);
                } else {
                    alert('⚠️ ' + data.message);
                }
            } catch (err) {
                alert('Gagal menghubungi server WA Gateway: ' + err.message);
            }
        }

        // --- MODAL KUSTOMISASI TARAWIH ---
        function openTarawihCustomizationModal() {
            const cfg = window.currentConfig || {};
            const html = '<form onsubmit="handleSaveTarawihCustomization(event)">' +
                '<div style="background:#FFFBEB; border:1.5px solid #F59E0B; border-radius:10px; padding:14px; margin-bottom:14px;">' +
                    '<div style="font-weight:700; font-size:13px; color:#F59E0B; margin-bottom:10px;">🌙 JUDUL SLIDE TARAWIH</div>' +
                    '<div style="margin-bottom:10px;">' +
                        '<label style="display:block; font-size:12px; font-weight:600; margin-bottom:4px; color:#0F172A; font-weight:700;">Teks Judul Slide Tarawih</label>' +
                        '<input type="text" id="custTarawihTitle" class="form-input" style="width:100%;" value="' + escapeHtml(cfg.tarawihTitleText || 'PETUGAS SHOLAT TARAWIH & WITIR') + '" required>' +
                    '</div>' +
                    makeColorRow('Warna Font Judul', 'custTarawihTitleColorPick', 'custTarawihTitleColor', '#F59E0B', cfg.tarawihTitleColor) +
                '</div>' +
                '<div style="background:#F0FDF4; border:1.5px solid #10B981; border-radius:10px; padding:14px; margin-bottom:16px;">' +
                    '<div style="font-weight:700; font-size:13px; color:#10B981; margin-bottom:10px;">🎨 WARNA FONT PETUGAS TARAWIH</div>' +
                    makeColorRow('Warna Nama Petugas (Penceramah, Imam, Bilal)', 'custTarawihOfficerPick', 'custTarawihOfficerColor', '#FFFFFF', cfg.tarawihOfficerNameColor) +
                    makeColorRow('Warna Label Jabatan & Judul Ceramah', 'custTarawihLabelPick', 'custTarawihLabelColor', '#94A3B8', cfg.tarawihOfficerLabelColor) +
                '</div>' +
                '<div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">' +
                    '<button type="button" class="btn-secondary" onclick="resetTarawihCustomization()" style="width:auto; font-size:12px; padding:8px 12px; color:#FCA5A5; border-color:#EF4444;">🔄 Reset Default</button>' +
                    '<div style="display:flex; gap:8px;">' +
                        '<button type="button" class="btn-secondary" onclick="closeUniversalModal()" style="width:auto; padding:8px 14px;">Batal</button>' +
                        '<button type="submit" class="btn-primary" style="width:auto; padding:8px 16px;">💾 Simpan ke TV</button>' +
                    '</div>' +
                '</div>' +
            '</form>';
            openUniversalModal('🌙 Kustomisasi Layar Tarawih Ramadhan', html);
        }

        async function handleSaveTarawihCustomization(e) {
            e.preventDefault();
            const payload = {
                tarawihTitleText: document.getElementById('custTarawihTitle').value.trim() || 'PETUGAS SHOLAT TARAWIH & WITIR',
                tarawihTitleColor: document.getElementById('custTarawihTitleColor').value.trim() || '#F59E0B',
                tarawihOfficerNameColor: document.getElementById('custTarawihOfficerColor').value.trim() || '#FFFFFF',
                tarawihOfficerLabelColor: document.getElementById('custTarawihLabelColor').value.trim() || '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Teks & Warna Layar Tarawih Berhasil Disimpan!');
        }

        async function resetTarawihCustomization() {
            if (!confirm('Kembalikan teks dan warna layar Tarawih ke setelan bawaan?')) return;
            const payload = {
                tarawihTitleText: 'PETUGAS SHOLAT TARAWIH & WITIR',
                tarawihTitleColor: '#F59E0B',
                tarawihOfficerNameColor: '#FFFFFF',
                tarawihOfficerLabelColor: '#94A3B8'
            };
            await saveContentCustomization(payload, 'Kustomisasi Layar Tarawih dikembalikan ke default!');
        }

        checkAuthAndInit();
    </script>
</body>
</html>
        """.trimIndent()
    }

    private fun parseBool(v: String?, defaultVal: Boolean): Boolean {
        if (v == null) return defaultVal
        return when (v.trim().lowercase()) {
            "true", "1", "on", "yes" -> true
            "false", "0", "off", "no" -> false
            else -> defaultVal
        }
    }

    private fun parseParams(body: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (body.startsWith("{")) {
            try {
                val json = JSONObject(body)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.optString(k, "")
                }
                return map
            } catch (_: Exception) {}
        }
        val pairs = body.split("&")
        for (p in pairs) {
            val idx = p.indexOf('=')
            if (idx > 0) {
                try {
                    val k = URLDecoder.decode(p.substring(0, idx), "UTF-8")
                    val v = URLDecoder.decode(p.substring(idx + 1), "UTF-8")
                    map[k] = v
                } catch (_: Exception) {}
            }
        }
        return map
    }

    private fun parseQueryString(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (query.isBlank()) return map
        val pairs = query.split("&")
        for (p in pairs) {
            val idx = p.indexOf('=')
            if (idx > 0) {
                try {
                    val k = URLDecoder.decode(p.substring(0, idx), "UTF-8")
                    val v = URLDecoder.decode(p.substring(idx + 1), "UTF-8")
                    map[k] = v
                } catch (_: Exception) {}
            }
        }
        return map
    }
}
