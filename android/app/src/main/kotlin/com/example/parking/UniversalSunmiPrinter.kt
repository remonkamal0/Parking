package com.example.parking

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object UniversalSunmiPrinter {
    private const val TAG = "UniversalSunmiPrinter"
    
    fun printReceipt(context: Context, parkingCode: String, carNumber: String): Boolean {
        return try {
            Log.d(TAG, "Starting universal Sunmi printer test")
            
            val receiptText = createReceiptText(parkingCode, carNumber)
            
            // Method 1: Try all possible Sunmi printer services
            if (tryAllSunmiServices(context, receiptText)) {
                return true
            }
            
            // Method 2: Try direct file writing
            if (tryFileWriting(context, receiptText)) {
                return true
            }
            
            // Method 3: Try Android print service
            if (tryAndroidPrint(context, receiptText)) {
                return true
            }
            
            Log.e(TAG, "All printing methods failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Universal printer error: ${e.message}")
            false
        }
    }
    
    private fun tryAllSunmiServices(context: Context, text: String): Boolean {
        val services = listOf(
            "woyou.aidlservice.jiuiv5.IWoyouService",
            "com.sunmi.printer.InnerPrinterManager",
            "com.sunmi.printer.WoyouService"
        )
        
        for (serviceName in services) {
            try {
                Log.d(TAG, "Trying service: $serviceName")
                
                if (serviceName.contains("WoyouService")) {
                    if (tryWoyouService(context, text)) {
                        Log.d(TAG, "WoyouService successful")
                        Toast.makeText(context, "✅ Printed via WoyouService", Toast.LENGTH_SHORT).show()
                        return true
                    }
                } else if (serviceName.contains("InnerPrinterManager")) {
                    if (tryInnerPrinterManager(context, text)) {
                        Log.d(TAG, "InnerPrinterManager successful")
                        Toast.makeText(context, "✅ Printed via InnerPrinterManager", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
                
            } catch (e: Exception) {
                Log.d(TAG, "Service $serviceName failed: ${e.message}")
                continue
            }
        }
        
        return false
    }
    
    private fun tryWoyouService(context: Context, text: String): Boolean {
        return try {
            val intent = android.content.Intent()
            intent.setPackage("woyou.aidlservice.jiuiv5")
            intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
            
            var result = false
            val serviceConnection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    try {
                        val clazz = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService")
                        val woyouService = clazz.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, service)
                        
                        // Initialize and print
                        val initMethod = woyouService.javaClass.getMethod("printerInit", Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        initMethod.invoke(woyouService, null)
                        
                        val printMethod = woyouService.javaClass.getMethod("printText", String::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        printMethod.invoke(woyouService, text + "\n", null)
                        
                        val lineWrapMethod = woyouService.javaClass.getMethod("lineWrap", Int::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        lineWrapMethod.invoke(woyouService, 3, null)
                        
                        result = true
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "WoyouService error: ${e.message}")
                    }
                }
                
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }
            
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (bound) {
                Thread.sleep(1500)
                context.unbindService(serviceConnection)
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "WoyouService failed: ${e.message}")
            false
        }
    }
    
    private fun tryInnerPrinterManager(context: Context, text: String): Boolean {
        return try {
            val printerClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
            val getInstanceMethod = printerClass.getMethod("getInstance", Context::class.java)
            val printerManager = getInstanceMethod.invoke(null, context)
            
            // Initialize and print
            val initMethod = printerManager.javaClass.getMethod("initPrinter")
            initMethod.invoke(printerManager)
            
            val printMethod = printerManager.javaClass.getMethod("printText", String::class.java)
            printMethod.invoke(printerManager, text + "\n")
            
            val feedMethod = printerManager.javaClass.getMethod("feedPaper", Int::class.java)
            feedMethod.invoke(printerManager, 3)
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "InnerPrinterManager failed: ${e.message}")
            false
        }
    }
    
    private fun tryFileWriting(context: Context, text: String): Boolean {
        return try {
            val printerPaths = listOf(
                "/dev/usb/lp0",
                "/dev/usb/lp1",
                "/dev/ttyUSB0",
                "/dev/ttyS1",
                "/sys/class/usbmisc/usbmisc0/device",
                "/data/data/com.sunmi.printer/files/print.txt"
            )
            
            for (path in printerPaths) {
                try {
                    val fileOutputStream = java.io.FileOutputStream(path)
                    fileOutputStream.write(text.toByteArray())
                    fileOutputStream.write("\n\n\n".toByteArray())
                    fileOutputStream.close()
                    
                    Log.d(TAG, "File writing successful via: $path")
                    Toast.makeText(context, "✅ Printed via file: $path", Toast.LENGTH_SHORT).show()
                    return true
                    
                } catch (e: Exception) {
                    Log.d(TAG, "Path $path failed: ${e.message}")
                    continue
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "File writing failed: ${e.message}")
            false
        }
    }
    
    private fun tryAndroidPrint(context: Context, text: String): Boolean {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
            val printAdapter = SimplePrintAdapter(text)
            printManager.print("Parking Receipt", printAdapter, null)
            
            Log.d(TAG, "Android print service initiated")
            Toast.makeText(context, "✅ Sent to Android print service", Toast.LENGTH_SHORT).show()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Android print failed: ${e.message}")
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
            
            // Try all methods
            if (tryAllSunmiServices(context, testText)) {
                return true
            }
            
            if (tryFileWriting(context, testText)) {
                return true
            }
            
            if (tryAndroidPrint(context, testText)) {
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Printer test failed: ${e.message}")
            false
        }
    }
}

// Simple print adapter for Android print service
class SimplePrintAdapter(private val content: String) : android.print.PrintDocumentAdapter() {
    override fun onLayout(oldAttrs: android.print.PrintAttributes?, newAttrs: android.print.PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
        callback?.onLayoutFinished(android.print.PrintDocumentInfo.Builder("Parking Receipt").build(), true)
    }
    
    override fun onWrite(pages: Array<out android.print.PageRange>?, destination: android.os.ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
        try {
            val fileOutputStream = android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination)
            fileOutputStream.write(content.toByteArray())
            fileOutputStream.close()
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}
