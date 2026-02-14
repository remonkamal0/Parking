package com.example.parking

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object SimplePrinterTest {
    private const val TAG = "SimplePrinterTest"
    
    fun testBasicPrint(context: Context): Boolean {
        return try {
            Log.d(TAG, "Starting basic printer test")
            
            // Method 1: Try Inner Printer Manager (most reliable)
            if (testWithInnerPrinter(context)) {
                return true
            }
            
            // Method 2: Try Woyou Service
            if (testWithWoyouService(context)) {
                return true
            }
            
            // Method 3: Try direct USB
            if (testWithDirectUSB(context)) {
                return true
            }
            
            Log.e(TAG, "All printing methods failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Printer test failed: ${e.message}")
            false
        }
    }
    
    private fun testWithWoyouService(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing with Woyou Service")
            
            // Try to bind to Woyou service
            val intent = android.content.Intent()
            intent.setPackage("woyou.aidlservice.jiuiv5")
            intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
            
            val serviceConnection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    try {
                        val clazz = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService")
                        val woyouService = clazz.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, service)
                        
                        // Initialize printer
                        val initMethod = woyouService.javaClass.getMethod("printerInit", Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        initMethod.invoke(woyouService, null)
                        
                        // Print test text
                        val testText = createTestText()
                        val printMethod = woyouService.javaClass.getMethod("printText", String::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        printMethod.invoke(woyouService, testText, null)
                        
                        // Add line feeds
                        val lineWrapMethod = woyouService.javaClass.getMethod("lineWrap", Int::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        lineWrapMethod.invoke(woyouService, 3, null)
                        
                        Log.d(TAG, "Woyou Service test successful")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Woyou Service test failed: ${e.message}")
                    }
                }
                
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }
            
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (bound) {
                Thread.sleep(2000) // Wait for connection
                context.unbindService(serviceConnection)
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Woyou Service test error: ${e.message}")
            false
        }
    }
    
    private fun testWithInnerPrinter(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing with Inner Printer Manager")
            
            val printerClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
            val getInstanceMethod = printerClass.getMethod("getInstance", Context::class.java)
            val printerManager = getInstanceMethod.invoke(null, context)
            
            // Initialize printer
            val initMethod = printerManager.javaClass.getMethod("initPrinter")
            initMethod.invoke(printerManager)
            
            // Print test text
            val testText = createTestText()
            val printTextMethod = printerManager.javaClass.getMethod("printText", String::class.java)
            printTextMethod.invoke(printerManager, testText)
            
            // Feed paper
            val feedPaperMethod = printerManager.javaClass.getMethod("feedPaper", Int::class.java)
            feedPaperMethod.invoke(printerManager, 3)
            
            Log.d(TAG, "Inner Printer test successful")
            Toast.makeText(context, "✅ Inner Printer test successful!", Toast.LENGTH_LONG).show()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Inner Printer test failed: ${e.message}")
            false
        }
    }
    
    private fun testWithDirectUSB(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing with Direct USB")
            
            val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
            val deviceList = usbManager.deviceList
            
            // Look for Sunmi device
            for (device in deviceList.values) {
                if (device.vendorId == 0x0483) { // Sunmi VID
                    Log.d(TAG, "Found Sunmi USB device: ${device.deviceName}")
                    
                    val connection = usbManager.openDevice(device)
                    if (connection != null) {
                        try {
                            val usbInterface = device.getInterface(0)
                            connection.claimInterface(usbInterface, true)
                            
                            // Send simple test command
                            val testCommand = "PRINTER TEST\n".toByteArray()
                            val endpoint = usbInterface.getEndpoint(0)
                            val result = connection.bulkTransfer(endpoint, testCommand, testCommand.size, 1000)
                            
                            connection.releaseInterface(usbInterface)
                            connection.close()
                            
                            if (result > 0) {
                                Log.d(TAG, "Direct USB test successful: $result bytes")
                                Toast.makeText(context, "✅ Direct USB test successful!", Toast.LENGTH_LONG).show()
                                return true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Direct USB test error: ${e.message}")
                        }
                    }
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Direct USB test failed: ${e.message}")
            false
        }
    }
    
    private fun createTestText(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return """
=====================================
           PRINTER TEST
=====================================

Date: ${dateFormat.format(Date())}
Device: Sunmi V2S
App: Parking System

This is a test print to verify
that the printer is working
correctly.

=====================================

        """.trimIndent()
    }
    
    fun printParkingReceipt(context: Context, parkingCode: String, carNumber: String): Boolean {
        return try {
            Log.d(TAG, "Printing parking receipt: $parkingCode")
            
            val receiptText = createParkingReceiptText(parkingCode, carNumber)
            
            // Try Inner Printer first (most reliable)
            if (printWithInnerPrinterText(context, receiptText)) {
                return true
            }
            
            // Try Woyou Service
            if (printWithWoyouServiceText(context, receiptText)) {
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Parking receipt print failed: ${e.message}")
            false
        }
    }
    
    private fun printWithInnerPrinterText(context: Context, text: String): Boolean {
        return try {
            val printerClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
            val getInstanceMethod = printerClass.getMethod("getInstance", Context::class.java)
            val printerManager = getInstanceMethod.invoke(null, context)
            
            val printTextMethod = printerManager.javaClass.getMethod("printText", String::class.java)
            printTextMethod.invoke(printerManager, text)
            
            val feedPaperMethod = printerManager.javaClass.getMethod("feedPaper", Int::class.java)
            feedPaperMethod.invoke(printerManager, 3)
            
            Log.d(TAG, "Inner Printer receipt successful")
            Toast.makeText(context, "✅ Receipt printed!", Toast.LENGTH_LONG).show()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Inner Printer receipt failed: ${e.message}")
            false
        }
    }
    
    private fun printWithWoyouServiceText(context: Context, text: String): Boolean {
        return try {
            // Similar to testWithWoyouService but with receipt text
            val intent = android.content.Intent()
            intent.setPackage("woyou.aidlservice.jiuiv5")
            intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
            
            val serviceConnection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                    try {
                        val clazz = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService")
                        val woyouService = clazz.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, service)
                        
                        val initMethod = woyouService.javaClass.getMethod("printerInit", Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        initMethod.invoke(woyouService, null)
                        
                        val printMethod = woyouService.javaClass.getMethod("printText", String::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        printMethod.invoke(woyouService, text, null)
                        
                        val lineWrapMethod = woyouService.javaClass.getMethod("lineWrap", Int::class.java, Class.forName("woyou.aidlservice.jiuiv5.ICallback"))
                        lineWrapMethod.invoke(woyouService, 3, null)
                        
                        Log.d(TAG, "Woyou Service receipt successful")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Woyou Service receipt failed: ${e.message}")
                    }
                }
                
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            }
            
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (bound) {
                Thread.sleep(2000)
                context.unbindService(serviceConnection)
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Woyou Service receipt error: ${e.message}")
            false
        }
    }
    
    private fun createParkingReceiptText(parkingCode: String, carNumber: String): String {
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
}
