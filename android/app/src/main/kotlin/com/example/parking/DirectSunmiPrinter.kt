package com.example.parking

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object DirectSunmiPrinter {
    private const val TAG = "DirectSunmiPrinter"
    private const val SUNMI_VID = 0x0483  // Sunmi USB Vendor ID
    private const val SUNMI_PID = 0x5720  // Sunmi USB Product ID for V2S
    
    private var usbDevice: UsbDevice? = null
    private var usbManager: UsbManager? = null
    
    fun initPrinter(context: Context): Boolean {
        return try {
            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager?.deviceList ?: return false
            
            // Find Sunmi V2S printer
            var sunmiDevice: UsbDevice? = null
            for (device in deviceList.values) {
                if (device.vendorId == SUNMI_VID && device.productId == SUNMI_PID) {
                    sunmiDevice = device
                    usbDevice = device
                    Log.d(TAG, "Found Sunmi V2S printer: ${device.deviceName}")
                    return true
                }
            }
            
            // Try alternative method - look for any Sunmi device
            for (device in deviceList.values) {
                if (device.vendorId == SUNMI_VID) {
                    sunmiDevice = device
                    usbDevice = device
                    Log.d(TAG, "Found Sunmi device: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})")
                    return true
                }
            }
            
            // Don't log as error, just as info
            Log.d(TAG, "No Sunmi V2S printer found via USB - will try alternative methods")
            false
            
        } catch (e: Exception) {
            Log.d(TAG, "Failed to initialize printer (not critical): ${e.message}")
            false
        }
    }
    
    fun printReceipt(context: Context, parkingCode: String, carNumber: String, entryTime: Date, hourlyRate: Double, callback: (Boolean, String?) -> Unit) {
        try {
            Log.d(TAG, "Starting direct Sunmi V2S printing")
            
            // Method 1: Try USB direct printing
            if (printViaUSB(context, parkingCode, carNumber, entryTime, hourlyRate, callback)) {
                return
            }
            
            // Method 2: Try Sunmi internal API
            if (printViaInternalAPI(context, parkingCode, carNumber, entryTime, hourlyRate, callback)) {
                return
            }
            
            // Method 3: Try file-based printing (for Sunmi devices)
            printViaFile(context, parkingCode, carNumber, entryTime, hourlyRate, callback)
            
        } catch (e: Exception) {
            Log.e(TAG, "All direct printing methods failed: ${e.message}")
            callback(false, "All printing methods failed: ${e.message}")
        }
    }
    
    private fun printViaUSB(context: Context, parkingCode: String, carNumber: String, entryTime: Date, hourlyRate: Double, callback: (Boolean, String?) -> Unit): Boolean {
        return try {
            Log.d(TAG, "Trying USB direct printing")
            
            if (usbDevice == null) {
                Log.d(TAG, "No USB device available - trying to initialize")
                if (!initPrinter(context)) {
                    Log.d(TAG, "Cannot find Sunmi V2S printer via USB - will try other methods")
                    return false
                }
            }
            
            val device = usbDevice ?: return false
            
            // Check if we have permission
            if (!usbManager?.hasPermission(device)!!) {
                Log.d(TAG, "No USB permission for Sunmi V2S printer - will try other methods")
                return false
            }
            
            // Create receipt content
            val receiptContent = createReceiptContent(parkingCode, carNumber, entryTime, hourlyRate)
            
            // Convert to ESC/POS commands
            val escPosCommands = createESCPosCommands(receiptContent)
            
            // Try to open USB connection and send commands
            val connection = usbManager?.openDevice(device)
            if (connection != null) {
                try {
                    val usbInterface = device.getInterface(0)
                    connection.claimInterface(usbInterface, true)
                    
                    val endpoint = usbInterface.getEndpoint(0)
                    val outputStream = connection.bulkTransfer(endpoint, escPosCommands, escPosCommands.size, 1000)
                    
                    connection.releaseInterface(usbInterface)
                    connection.close()
                    
                    if (outputStream > 0) {
                        Log.d(TAG, "USB printing successful: $outputStream bytes sent")
                        Toast.makeText(context, "Receipt printed via USB!", Toast.LENGTH_LONG).show()
                        callback(true, null)
                        return true
                    } else {
                        Log.d(TAG, "USB transfer failed: $outputStream - will try other methods")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "USB connection error: ${e.message} - will try other methods")
                } finally {
                    connection.close()
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.d(TAG, "USB printing failed: ${e.message} - will try other methods")
            false
        }
    }
    
    private fun printViaInternalAPI(context: Context, parkingCode: String, carNumber: String, entryTime: Date, hourlyRate: Double, callback: (Boolean, String?) -> Unit): Boolean {
        return try {
            Log.d(TAG, "Trying Sunmi internal API")
            
            // Try to access Sunmi's internal printer service
            val printerClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
            val getInstanceMethod = printerClass.getMethod("getInstance", Context::class.java)
            val printerManager = getInstanceMethod.invoke(null, context)
            
            // Initialize printer
            val initMethod = printerManager.javaClass.getMethod("initPrinter")
            initMethod.invoke(printerManager)
            
            // Create receipt content
            val receiptContent = createReceiptContent(parkingCode, carNumber, entryTime, hourlyRate)
            
            // Print receipt
            val printTextMethod = printerManager.javaClass.getMethod("printText", String::class.java)
            printTextMethod.invoke(printerManager, receiptContent)
            
            // Feed paper
            val feedPaperMethod = printerManager.javaClass.getMethod("feedPaper", Int::class.java)
            feedPaperMethod.invoke(printerManager, 3)
            
            Log.d(TAG, "Internal API printing successful")
            Toast.makeText(context, "Receipt printed via internal API!", Toast.LENGTH_LONG).show()
            callback(true, null)
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Internal API printing failed: ${e.message}")
            false
        }
    }
    
    private fun printViaFile(context: Context, parkingCode: String, carNumber: String, entryTime: Date, hourlyRate: Double, callback: (Boolean, String?) -> Unit) {
        try {
            Log.d(TAG, "Trying file-based printing")
            
            // Create receipt content
            val receiptContent = createReceiptContent(parkingCode, carNumber, entryTime, hourlyRate)
            
            // Try to write to Sunmi's printer device file
            val printerPaths = listOf(
                "/dev/usb/lp0",
                "/dev/usb/lp1", 
                "/dev/ttyUSB0",
                "/dev/ttyS1",
                "/sys/class/usbmisc/usbmisc0/device"
            )
            
            var success = false
            for (path in printerPaths) {
                try {
                    val fileOutputStream = FileOutputStream(path)
                    fileOutputStream.write(receiptContent.toByteArray())
                    fileOutputStream.close()
                    
                    Log.d(TAG, "File-based printing successful via: $path")
                    Toast.makeText(context, "Receipt printed via file!", Toast.LENGTH_LONG).show()
                    callback(true, null)
                    success = true
                    break
                } catch (e: IOException) {
                    Log.d(TAG, "Failed to write to $path: ${e.message}")
                    continue
                }
            }
            
            if (!success) {
                // Try Android's built-in printing as last resort
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                    val printAdapter = ReceiptPrintAdapter(receiptContent)
                    printManager.print("Parking Receipt", printAdapter, null)
                    
                    Log.d(TAG, "Android print service initiated")
                    Toast.makeText(context, "Print job sent to Android print service!", Toast.LENGTH_LONG).show()
                    callback(true, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Android print service failed: ${e.message}")
                    callback(false, "All file-based printing methods failed")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "File-based printing failed: ${e.message}")
            callback(false, "File-based printing failed: ${e.message}")
        }
    }
    
    private fun createESCPosCommands(text: String): ByteArray {
        val commands = mutableListOf<Byte>()
        
        // Initialize printer
        commands.addAll(listOf(0x1B.toByte(), 0x40.toByte())) // ESC @
        
        // Set character code page (UTF-8 support)
        commands.addAll(listOf(0x1B.toByte(), 0x74.toByte(), 0x11.toByte())) // ESC t 17 (UTF-8)
        
        val lines = text.split("\n")
        for (line in lines) {
            if (line.contains("=====") || line.contains("---")) {
                commands.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x01.toByte())) // Bold on
            } else if (line.contains("PARKING RECEIPT")) {
                commands.addAll(listOf(0x1B.toByte(), 0x21.toByte(), 0x10.toByte())) // Double height
                commands.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())) // Center
            } else if (line.trim().isEmpty()) {
                commands.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x00.toByte())) // Bold off
                commands.addAll(listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())) // Normal
                commands.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())) // Left
            } else {
                commands.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x00.toByte())) // Bold off
                commands.addAll(listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())) // Normal
                commands.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())) // Left
            }
            
            // Add line text
            commands.addAll(line.toByteArray(charset("UTF-8")).map { it.toByte() })
            commands.add(0x0A.toByte()) // Line feed
        }
        
        // Add final line feeds and cut
        commands.addAll(listOf(0x0A.toByte(), 0x0A.toByte(), 0x0A.toByte())) // 3 line feeds
        commands.addAll(listOf(0x1D.toByte(), 0x56.toByte(), 0x00.toByte())) // Cut paper
        
        return commands.toByteArray()
    }
    
    private fun createReceiptContent(parkingCode: String, carNumber: String, entryTime: Date, hourlyRate: Double): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return """
=====================================
           PARKING RECEIPT
=====================================

Parking Type: PER HOUR PARKING
Parking Code: $parkingCode
Car Number: $carNumber
Entry Time: ${dateFormat.format(entryTime)}
Hourly Rate: ${String.format("%.2f", hourlyRate)} EGP

-------------------------------------

Thank you for using our parking service!

       QR Code: $parkingCode

=====================================
        """.trimIndent()
    }
    
    fun checkStatus(context: Context): Boolean {
        return try {
            if (usbDevice == null) {
                initPrinter(context)
            }
            
            usbDevice != null && usbManager?.hasPermission(usbDevice) == true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check printer status: ${e.message}")
            // Try internal API as fallback
            try {
                val printerClass = Class.forName("com.sunmi.printer.InnerPrinterManager")
                val getInstanceMethod = printerClass.getMethod("getInstance", Context::class.java)
                val printerManager = getInstanceMethod.invoke(null, context)
                
                val statusMethod = printerManager.javaClass.getMethod("getPrinterStatus")
                val status = statusMethod.invoke(printerManager) as? Int
                
                status == 0 || status == 1
            } catch (e2: Exception) {
                Log.e(TAG, "Internal API status check failed: ${e2.message}")
                true // Assume available if we're on a Sunmi device
            }
        }
    }
    
    fun testPrint(context: Context, callback: (Boolean, String?) -> Unit) {
        val now = Date()
        printReceipt(context, "TEST001", "TEST-CAR", now, 25.0, callback)
    }
}

// Simple print adapter for Android print service
class ReceiptPrintAdapter(private val content: String) : android.print.PrintDocumentAdapter() {
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
