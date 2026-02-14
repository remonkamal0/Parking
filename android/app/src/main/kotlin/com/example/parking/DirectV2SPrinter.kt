package com.example.parking

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object DirectV2SPrinter {
    private const val TAG = "DirectV2SPrinter"
    
    fun printReceipt(context: Context, parkingCode: String, carNumber: String): Boolean {
        return try {
            Log.d(TAG, "Starting direct V2S printing")
            
            val receiptText = createReceiptText(parkingCode, carNumber)
            
            // Try direct V2S printing methods
            if (tryDirectV2S(context, receiptText)) {
                return true
            }
            
            // Try file method as backup
            if (tryFileMethod(context, receiptText)) {
                return true
            }
            
            Log.d(TAG, "V2S printing failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "V2S printer error: ${e.message}")
            false
        }
    }
    
    private fun tryDirectV2S(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying direct V2S method")
            
            // Method 1: Try Sunmi V2S specific service
            val serviceResult = try {
                val intent = android.content.Intent()
                intent.setPackage("woyou.aidlservice.jiuiv5")
                intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
                
                var success = false
                val serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                        try {
                            val clazz = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService")
                            val woyouService = clazz.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, service)
                            
                            // Initialize printer
                            val initMethod = woyouService.javaClass.getMethod("printerInit", Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                            initMethod.invoke(woyouService, null)
                            
                            // Print text
                            val printMethod = woyouService.javaClass.getMethod("printText", String::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                            printMethod.invoke(woyouService, text + "\n\n\n", null)
                            
                            success = true
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "V2S service error: ${e.message}")
                        }
                    }
                    
                    override fun onServiceDisconnected(name: android.content.ComponentName?) {}
                }
                
                val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                if (bound) {
                    Thread.sleep(1000)
                    context.unbindService(serviceConnection)
                }
                
                success
                
            } catch (e: Exception) {
                Log.d(TAG, "V2S service failed: ${e.message}")
                false
            }
            
            if (serviceResult) {
                Log.d(TAG, "V2S service printing successful")
                Toast.makeText(context, "✅ Printed via V2S service", Toast.LENGTH_SHORT).show()
                return true
            }
            
            // Method 2: Try Inner Printer Manager
            val innerResult = try {
                val printerClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
                val getInstanceMethod = printerClass.getMethod("getInstance", Context::class.java)
                val printerManager = getInstanceMethod.invoke(null, context)
                
                val initMethod = printerManager.javaClass.getMethod("initPrinter")
                initMethod.invoke(printerManager)
                
                val printMethod = printerManager.javaClass.getMethod("printText", String::class.java)
                printMethod.invoke(printerManager, text + "\n\n\n")
                
                true
                
            } catch (e: Exception) {
                Log.d(TAG, "V2S inner printer failed: ${e.message}")
                false
            }
            
            if (innerResult) {
                Log.d(TAG, "V2S inner printer successful")
                Toast.makeText(context, "✅ Printed via V2S inner printer", Toast.LENGTH_SHORT).show()
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Direct V2S method failed: ${e.message}")
            false
        }
    }
    
    private fun tryFileMethod(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying file method")
            
            val paths = listOf(
                "/storage/emulated/0/Download/v2s_receipt.txt",
                "/data/data/com.example.parking/files/v2s_receipt.txt"
            )
            
            for (path in paths) {
                try {
                    val file = java.io.File(path)
                    file.parentFile?.mkdirs()
                    val fileOutputStream = java.io.FileOutputStream(file)
                    fileOutputStream.write(text.toByteArray())
                    fileOutputStream.close()
                    
                    Log.d(TAG, "File method successful: $path")
                    Toast.makeText(context, "✅ Receipt saved to file", Toast.LENGTH_SHORT).show()
                    return true
                    
                } catch (e: Exception) {
                    Log.d(TAG, "Path $path failed: ${e.message}")
                    continue
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "File method failed: ${e.message}")
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
    
    fun testV2SPrinter(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing V2S printer")
            
            val testText = """
=====================================
           V2S PRINTER TEST
=====================================

Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
Device: Sunmi V2S
App: Parking System

V2S Test completed successfully!

=====================================
            """.trimIndent()
            
            if (tryDirectV2S(context, testText)) {
                return true
            }
            
            if (tryFileMethod(context, testText)) {
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "V2S test failed: ${e.message}")
            false
        }
    }
}
