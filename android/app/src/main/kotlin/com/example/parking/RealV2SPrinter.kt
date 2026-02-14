package com.example.parking

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

object RealV2SPrinter {
    private const val TAG = "RealV2SPrinter"
    
    fun printReceipt(context: Context, parkingCode: String, carNumber: String): Boolean {
        return try {
            Log.d(TAG, "Starting REAL V2S printing - will print on paper")
            
            val receiptText = createReceiptText(parkingCode, carNumber)
            
            // Try REAL printing methods that actually print on paper
            if (tryRealPaperPrint(context, receiptText)) {
                return true
            }
            
            // Try ESC/POS commands directly
            if (tryESCPOSDirect(context, receiptText)) {
                return true
            }
            
            // Try system print
            if (trySystemPrint(context, receiptText)) {
                return true
            }
            
            Log.e(TAG, "All real printing methods failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Real V2S printer error: ${e.message}")
            false
        }
    }
    
    private fun tryRealPaperPrint(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying REAL paper print")
            
            // Method 1: Try Sunmi thermal printer directly
            val thermalResult = tryThermalPrinter(context, text)
            if (thermalResult) {
                Log.d(TAG, "Thermal printer successful")
                Toast.makeText(context, "✅ Printed on thermal paper!", Toast.LENGTH_LONG).show()
                return true
            }
            
            // Method 2: Try USB printer directly
            val usbResult = tryUSBPrinter(context, text)
            if (usbResult) {
                Log.d(TAG, "USB printer successful")
                Toast.makeText(context, "✅ Printed via USB!", Toast.LENGTH_LONG).show()
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Real paper print failed: ${e.message}")
            false
        }
    }
    
    private fun tryThermalPrinter(context: Context, text: String): Boolean {
        return try {
            // Try to access Sunmi thermal printer API
            val thermalClass = Class.forName("com.sunmi.thermalprinter.ThermalPrinter")
            val getInstance = thermalClass.getMethod("getInstance")
            val printer = getInstance.invoke(null)
            
            // Initialize thermal printer
            val initMethod = printer.javaClass.getMethod("initThermalPrinter")
            initMethod.invoke(printer)
            
            // Set printer settings
            val setGrayMethod = printer.javaClass.getMethod("setGrayLevel", Int::class.java)
            setGrayMethod.invoke(printer, 7)
            
            // Print text line by line
            val lines = text.split("\n")
            for (line in lines) {
                val addTextMethod = printer.javaClass.getMethod("addText", String::class.java, Int::class.java, Int::class.java, Int::class.java)
                addTextMethod.invoke(printer, line, 24, 0, 0) // 24pt, normal, left align
            }
            
            // Commit and print
            val commitMethod = printer.javaClass.getMethod("commitPrinter")
            commitMethod.invoke(printer)
            
            Log.d(TAG, "Thermal printer committed successfully")
            true
            
        } catch (e: Exception) {
            Log.d(TAG, "Thermal printer failed: ${e.message}")
            false
        }
    }
    
    private fun tryUSBPrinter(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying USB printer")
            
            val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
            val deviceList = usbManager.deviceList
            
            for (device in deviceList.values) {
                if (device.vendorId == 0x0483) { // Sunmi USB VID
                    Log.d(TAG, "Found Sunmi USB device: ${device.deviceName}")
                    
                    val connection = usbManager.openDevice(device)
                    if (connection != null) {
                        try {
                            val usbInterface = device.getInterface(0)
                            connection.claimInterface(usbInterface, true)
                            
                            // Send ESC/POS commands for real printing
                            val escPosCommands = createRealESCPOS(text)
                            val endpoint = usbInterface.getEndpoint(0)
                            val result = connection.bulkTransfer(endpoint, escPosCommands, escPosCommands.size, 5000)
                            
                            connection.releaseInterface(usbInterface)
                            connection.close()
                            
                            if (result > 0) {
                                Log.d(TAG, "USB ESC/POS sent successfully: $result bytes")
                                return true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "USB printer error: ${e.message}")
                        }
                    }
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "USB printer failed: ${e.message}")
            false
        }
    }
    
    private fun tryESCPOSDirect(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying ESC/POS direct")
            
            // Create real ESC/POS commands
            val commands = createRealESCPOS(text)
            
            // Try to send to printer device directly
            val printerPaths = listOf(
                "/dev/usb/lp0",
                "/dev/usb/lp1",
                "/dev/ttyUSB0",
                "/dev/ttyS0"
            )
            
            for (path in printerPaths) {
                try {
                    val outputStream = java.io.FileOutputStream(path)
                    outputStream.write(commands)
                    outputStream.flush()
                    outputStream.close()
                    
                    Log.d(TAG, "ESC/POS sent to: $path")
                    Toast.makeText(context, "✅ ESC/POS sent to printer!", Toast.LENGTH_LONG).show()
                    return true
                    
                } catch (e: Exception) {
                    Log.d(TAG, "Path $path failed: ${e.message}")
                    continue
                }
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "ESC/POS direct failed: ${e.message}")
            false
        }
    }
    
    private fun trySystemPrint(context: Context, text: String): Boolean {
        return try {
            Log.d(TAG, "Trying system print")
            
            // Try Android's built-in print service
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
            val printAdapter = RealPrintAdapter(text)
            printManager.print("Parking Receipt", printAdapter, null)
            
            Log.d(TAG, "System print initiated")
            Toast.makeText(context, "✅ Sent to system printer!", Toast.LENGTH_LONG).show()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "System print failed: ${e.message}")
            false
        }
    }
    
    private fun createRealESCPOS(text: String): ByteArray {
        val commands = mutableListOf<Byte>()
        
        // Initialize printer
        commands.addAll(listOf(0x1B.toByte(), 0x40.toByte())) // ESC @
        commands.addAll(listOf(0x1B.toByte(), 0x74.toByte(), 0x11.toByte())) // ESC t 17 (UTF-8)
        
        val lines = text.split("\n")
        for (line in lines) {
            if (line.contains("=====") || line.contains("---")) {
                commands.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x01.toByte())) // Bold on
            } else if (line.contains("PARKING RECEIPT")) {
                commands.addAll(listOf(0x1B.toByte(), 0x21.toByte(), 0x10.toByte())) // Double height
                commands.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())) // Center
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
    
    fun testRealPrinter(context: Context): Boolean {
        return try {
            Log.d(TAG, "Testing REAL V2S printer")
            
            val testText = """
=====================================
           REAL PRINTER TEST
=====================================

Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
Device: Sunmi V2S
App: Parking System

This test will PRINT ON PAPER!

If you see this on paper, it works!

=====================================
            """.trimIndent()
            
            if (tryRealPaperPrint(context, testText)) {
                return true
            }
            
            if (tryESCPOSDirect(context, testText)) {
                return true
            }
            
            if (trySystemPrint(context, testText)) {
                return true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Real printer test failed: ${e.message}")
            false
        }
    }
}

// Real print adapter that actually prints
class RealPrintAdapter(private val content: String) : android.print.PrintDocumentAdapter() {
    override fun onLayout(oldAttrs: android.print.PrintAttributes?, newAttrs: android.print.PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
        try {
            val builder = android.print.PrintDocumentInfo.Builder("Parking Receipt")
            builder.setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            callback?.onLayoutFinished(builder.build(), true)
        } catch (e: Exception) {
            Log.e("RealPrintAdapter", "Layout error: ${e.message}")
        }
    }
    
    override fun onWrite(pages: Array<out android.print.PageRange>?, destination: android.os.ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
        try {
            val fileOutputStream = android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination)
            fileOutputStream.write(content.toByteArray())
            fileOutputStream.write("\n\n\n".toByteArray()) // Extra line feeds
            fileOutputStream.close()
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            Log.d("RealPrintAdapter", "Write completed - should print on paper")
        } catch (e: Exception) {
            Log.e("RealPrintAdapter", "Write error: ${e.message}")
            callback?.onWriteFailed("Write failed: ${e.message}")
        }
    }
}
