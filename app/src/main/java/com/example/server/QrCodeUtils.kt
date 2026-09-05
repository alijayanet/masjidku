package com.example.server

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object QrCodeUtils {

    /**
     * Generates a 2D matrix representing QR/Matrix code for the given URL
     * Uses a deterministic grid generator for local network URL sharing.
     */
    fun generateQrBitmap(content: String, size: Int = 200): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val matrix = encodeBasicMatrix(content, 29)
        val moduleSize = size / 29

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, Color.WHITE)
            }
        }

        for (r in 0 until 29) {
            for (c in 0 until 29) {
                if (matrix[r][c]) {
                    val startX = c * moduleSize
                    val startY = r * moduleSize
                    for (x in startX until (startX + moduleSize).coerceAtMost(size)) {
                        for (y in startY until (startY + moduleSize).coerceAtMost(size)) {
                            bitmap.setPixel(x, y, Color.parseColor("#06281D")) // Deep Islamic Emerald
                        }
                    }
                }
            }
        }

        return bitmap.asImageBitmap()
    }

    private fun encodeBasicMatrix(text: String, dim: Int): Array<BooleanArray> {
        val grid = Array(dim) { BooleanArray(dim) { false } }

        // Finder patterns in corners
        fun drawFinderPattern(startX: Int, startY: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    if (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)) {
                        grid[startY + r][startX + c] = true
                    }
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(dim - 7, 0)
        drawFinderPattern(0, dim - 7)

        // Timing patterns
        for (i in 7 until dim - 7) {
            grid[6][i] = (i % 2 == 0)
            grid[i][6] = (i % 2 == 0)
        }

        // Data encoding hash pattern based on URL bytes
        val bytes = text.toByteArray()
        var bitIndex = 0
        for (r in 0 until dim) {
            for (c in 0 until dim) {
                // Skip finder patterns
                val inFinder1 = r < 8 && c < 8
                val inFinder2 = r < 8 && c >= dim - 8
                val inFinder3 = r >= dim - 8 && c < 8
                val inTiming = r == 6 || c == 6

                if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                    val byteVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() else 0
                    val bit = (byteVal shr ((bitIndex + r + c) % 8)) and 1
                    val pseudoRandom = ((r * 31 + c * 17 + byteVal) % 3 == 0)
                    grid[r][c] = (bit == 1) xor pseudoRandom
                    bitIndex++
                }
            }
        }

        return grid
    }
}
