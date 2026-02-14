package com.example.parking

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object SimpleSafePrinter {
    private const val TAG = "SimpleSafePrinter"
    
    fun printReceipt(context: Context, parkingCode: String, carNumber: String): Boolean {
        return try {
            Log.d(TAG, "Starting simple safe printing")
            
            val receiptText = createReceiptText(parkingCode, carNumber)
            
            // Only use the safest method - no threading, no complex operations
            if (trySimpleFilePrint(context, receiptText)) {
                return true
            }
            
            Log.d(TAG, "Simple printing completed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Simple safe printer error: ${e.message}")
            false
        }
    }
    
    private fun trySimpleFilePrint(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying simple file print")
            
            // Only try one safe method - save to file
            val file = java.io.File("/storage/emulated/0/Download/parking_receipt.txt")
            
            try {
                file.parentFile?.mkdirs()
                val fileOutputStream = java.io.FileOutputStream(file)
                fileOutputStream.write(text.toByteArray())
                fileOutputStream.write("\n\n".toByteArray())
                fileOutputStream.close()
                
                Log.d(TAG, "File saved successfully: ${file.absolutePath}")
                Toast.makeText(context, "✅ Receipt saved to Download folder", Toast.LENGTH_LONG).show()
                return true
                
            } catch (e: Exception) {
                Log.d(TAG, "External storage failed, trying internal")
                
                // Try internal storage as backup
                val internalFile = java.io.File(context.filesDir, "parking_receipt.txt")
                val fileOutputStream = java.io.FileOutputStream(internalFile)
                fileOutputStream.write(text.toByteArray())
                fileOutputStream.write("\n\n".toByteArray())
                fileOutputStream.close()
                
                Log.d(TAG, "Internal file saved: ${internalFile.absolutePath}")
                Toast.makeText(context, "✅ Receipt saved to app storage", Toast.LENGTH_LONG).show()
                return true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Simple file print failed: ${e.message}")
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
    
    fun testSimplePrinter(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing simple safe printer")
            
            val testText = """
=====================================
           SIMPLE PRINTER TEST
=====================================

Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
Device: Sunmi V2S
App: Parking System

Simple test completed!

=====================================
            """.trimIndent()
            
            if (trySimpleFilePrint(context, testText)) {
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Simple printer test failed: ${e.message}")
            false
        }
    }
}
