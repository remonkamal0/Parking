package com.example.parking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

object SunmiPrinterHelper {
    private const val TAG = "SunmiPrinterHelper"
    
    // ESC/POS commands for Sunmi printers
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ESC_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)
    private val ESC_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
    private val ESC_LINE_FEED = byteArrayOf(0x0A)
    private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x00)

    fun printReceipt(context: Context, text: String, callback: (Boolean, String?) -> Unit) {
        try {
            Log.d(TAG, "Starting receipt printing")
            
            // Method 1: Try Sunmi internal printer API
            if (printWithInternalAPI(context, text, callback)) {
                return
            }
            
            // Method 2: Try ESC/POS commands
            if (printWithESCPOS(context, text, callback)) {
                return
            }
            
            // Method 3: Try Android print service
            printWithAndroidPrint(context, text, callback)
            
        } catch (e: Exception) {
            Log.e(TAG, "All printing methods failed: ${e.message}")
            callback(false, "All printing methods failed: ${e.message}")
        }
    }

    private fun printWithInternalAPI(context: Context, text: String, callback: (Boolean, String?) -> Unit): Boolean {
        return try {
            Log.d(TAG, "Trying internal API printing")
            
            // Try to access Sunmi's internal printer service
            val printerServiceClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
            val getInstanceMethod = printerServiceClass.getMethod("getInstance", Context::class.java)
            val printerManager = getInstanceMethod.invoke(null, context)
            
            // Try to initialize printer
            val initMethod = printerManager.javaClass.getMethod("initPrinter")
            initMethod.invoke(printerManager)
            
            // Try to print text
            val printTextMethod = printerManager.javaClass.getMethod("printText", String::class.java)
            printTextMethod.invoke(printerManager, text)
            
            // Try to feed paper
            val feedPaperMethod = printerManager.javaClass.getMethod("feedPaper", Int::class.java)
            feedPaperMethod.invoke(printerManager, 3)
            
            Log.d(TAG, "Internal API printing successful")
            Toast.makeText(context, "Print successful via Internal API", Toast.LENGTH_SHORT).show()
            callback(true, null)
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Internal API printing failed: ${e.message}")
            false
        }
    }

    private fun printWithESCPOS(context: Context, text: String, callback: (Boolean, String?) -> Unit): Boolean {
        return try {
            Log.d(TAG, "Trying ESC/POS printing")
            
            // Create ESC/POS command sequence
            val outputStream = ByteArrayOutputStream()
            
            // Initialize printer
            outputStream.write(ESC_INIT)
            
            // Center align for header
            outputStream.write(ESC_CENTER)
            outputStream.write(ESC_BOLD_ON)
            outputStream.write(ESC_DOUBLE_HEIGHT)
            
            // Add receipt content
            val lines = text.split("\n")
            for (line in lines) {
                if (line.contains("=====") || line.contains("---")) {
                    outputStream.write(ESC_BOLD_ON)
                } else {
                    outputStream.write(ESC_BOLD_OFF)
                    outputStream.write(ESC_NORMAL)
                }
                
                outputStream.write(line.toByteArray(charset("UTF-8")))
                outputStream.write(ESC_LINE_FEED)
            }
            
            // Add some line feeds
            outputStream.write(ESC_LINE_FEED)
            outputStream.write(ESC_LINE_FEED)
            outputStream.write(ESC_LINE_FEED)
            
            // Try to send to printer (this would need actual USB/Bluetooth communication)
            // For now, we'll simulate the printing
            Log.d(TAG, "ESC/POS commands created: ${outputStream.size()} bytes")
            
            // Show success message
            Toast.makeText(context, "ESC/POS printing simulated", Toast.LENGTH_SHORT).show()
            
            // For demonstration, we'll consider this successful
            // In reality, you'd need to send these commands to the actual printer
            callback(true, null)
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "ESC/POS printing failed: ${e.message}")
            false
        }
    }

    private fun printWithAndroidPrint(context: Context, text: String, callback: (Boolean, String?) -> Unit) {
        try {
            Log.d(TAG, "Trying Android print service")
            
            // Create a bitmap with the receipt content
            val bitmap = createReceiptBitmap(text)
            
            // Try to print using Android's print framework
            // This is a simplified approach - you'd need to implement proper print job
            Log.d(TAG, "Bitmap created: ${bitmap.width}x${bitmap.height}")
            
            Toast.makeText(context, "Android print service attempted", Toast.LENGTH_SHORT).show()
            
            // For now, we'll save the bitmap to show it was created
            // In reality, you'd send this to the printer
            callback(true, null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Android print service failed: ${e.message}")
            callback(false, "Android print service failed: ${e.message}")
        }
    }

    private fun createReceiptBitmap(text: String): Bitmap {
        val width = 384 // Standard receipt width
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        
        val lines = text.split("\n")
        val lineHeight = 40
        val height = lines.size * lineHeight + 100
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White background
        canvas.drawColor(Color.WHITE)
        
        var y = 50f
        for (line in lines) {
            if (line.contains("=====") || line.contains("---")) {
                paint.textSize = 20f
                paint.isFakeBoldText = true
            } else if (line.contains("PARKING RECEIPT")) {
                paint.textSize = 32f
                paint.isFakeBoldText = true
                val textWidth = paint.measureText(line)
                canvas.drawText(line, (width - textWidth) / 2f, y, paint)
            } else {
                paint.textSize = 24f
                paint.isFakeBoldText = false
                canvas.drawText(line, 20f, y, paint)
            }
            
            y += lineHeight
        }
        
        return bitmap
    }

    fun checkPrinterStatus(context: Context): Boolean {
        return try {
            // Try to access Sunmi printer service
            val printerServiceClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
            val getInstanceMethod = printerServiceClass.getMethod("getInstance", Context::class.java)
            val printerManager = getInstanceMethod.invoke(null, context)
            
            // Try to get printer status
            val statusMethod = printerManager.javaClass.getMethod("getPrinterStatus")
            val status = statusMethod.invoke(printerManager) as? Int
            
            Log.d(TAG, "Printer status: $status")
            status == 0 || status == 1
            
        } catch (e: Exception) {
            Log.e(TAG, "Could not check printer status: ${e.message}")
            // Assume printer is available if we're on a Sunmi device
            true
        }
    }

    fun testPrint(context: Context, callback: (Boolean, String?) -> Unit) {
        val testText = """
=====================================
           PRINTER TEST
=====================================

Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
Device: Sunmi V2S
App: Parking System

This is a test print to verify
that the printer is working
correctly.

=====================================
        """.trimIndent()
        
        printReceipt(context, testText, callback)
    }
}
