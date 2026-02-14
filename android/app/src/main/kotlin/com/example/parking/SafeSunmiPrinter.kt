package com.example.parking

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object SafeSunmiPrinter {
    private const val TAG = "SafeSunmiPrinter"
    
    fun printReceipt(context: Context, parkingCode: String, carNumber: String): Boolean {
        return try {
            Log.d(TAG, "Starting safe printer test")
            
            val receiptText = createReceiptText(parkingCode, carNumber)
            
            // Try the safest method first
            if (trySafePrint(context, receiptText)) {
                return true
            }
            
            // Try alternative methods
            if (tryAlternativePrint(context, receiptText)) {
                return true
            }
            
            Log.d(TAG, "All printing methods failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Safe printer error: ${e.message}")
            false
        }
    }
    
    private fun trySafePrint(context: Context, text: String): Boolean {
        return try {
            // Method 1: Try Android print service (safest)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
            if (printManager != null) {
                val printAdapter = SafePrintAdapter(text)
                printManager.print("Parking Receipt", printAdapter, null)
                Log.d(TAG, "Android print service initiated")
                Toast.makeText(context, "✅ Print job sent", Toast.LENGTH_SHORT).show()
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.d(TAG, "Safe print failed: ${e.message}")
            false
        }
    }
    
    private fun tryAlternativePrint(context: Context, text: String): Boolean {
        return try {
            // Method 2: Try simple file writing
            val paths = listOf(
                "/storage/emulated/0/Download/print_test.txt",
                "/data/data/com.example.parking/files/print_test.txt"
            )
            
            for (path in paths) {
                try {
                    val file = java.io.File(path)
                    file.parentFile?.mkdirs()
                    val fileOutputStream = java.io.FileOutputStream(file)
                    fileOutputStream.write(text.toByteArray())
                    fileOutputStream.close()
                    
                    Log.d(TAG, "File written successfully: $path")
                    Toast.makeText(context, "✅ Receipt saved to file", Toast.LENGTH_SHORT).show()
                    return true
                    
                } catch (e: Exception) {
                    Log.d(TAG, "Path $path failed: ${e.message}")
                    continue
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Alternative print failed: ${e.message}")
            false
        }
    }
    
    private fun createReceiptText(parkingCode: String, carNumber: String): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return """
=====================================
           PARKING RECEIPT
=====================================

Parking Type: PER HOUR PARKING
Parking Code: $parkingCode
Car Number: $carNumber
Entry Time: ${dateFormat.format(Date())}
Hourly Rate: 25.00 EGP

-------------------------------------

Thank you for using our parking service!

       QR Code: $parkingCode

=====================================
        """.trimIndent()
    }
    
    fun testPrinter(context: Context): Boolean {
        return try {
            val testText = """
=====================================
           PRINTER TEST
=====================================

Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
Device: Sunmi V2S
App: Parking System

Test completed successfully!

=====================================
            """.trimIndent()
            
            // Try safe methods only
            if (trySafePrint(context, testText)) {
                return true
            }
            
            if (tryAlternativePrint(context, testText)) {
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Printer test failed: ${e.message}")
            false
        }
    }
}

// Safe print adapter that won't crash
class SafePrintAdapter(private val content: String) : android.print.PrintDocumentAdapter() {
    override fun onLayout(oldAttrs: android.print.PrintAttributes?, newAttrs: android.print.PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
        try {
            callback?.onLayoutFinished(android.print.PrintDocumentInfo.Builder("Parking Receipt").build(), true)
        } catch (e: Exception) {
            Log.e("SafePrintAdapter", "Layout error: ${e.message}")
        }
    }
    
    override fun onWrite(pages: Array<out android.print.PageRange>?, destination: android.os.ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
        try {
            val fileOutputStream = android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination)
            fileOutputStream.write(content.toByteArray())
            fileOutputStream.close()
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (e: Exception) {
            Log.e("SafePrintAdapter", "Write error: ${e.message}")
            callback?.onWriteFailed("Write failed: ${e.message}")
        }
    }
}
