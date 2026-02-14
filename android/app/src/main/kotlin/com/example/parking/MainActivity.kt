package com.example.parking

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

// ✅ Sunmi Official Printer Library (printerlibrary)
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.example.new_parking/print"
    private val TAG = "SUNMI_SDK"

    private var sunmiService: SunmiPrinterService? = null
    private var isConnected = false

    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            sunmiService = service
            isConnected = (service != null)
            Log.e(TAG, "✅ Sunmi printer connected = $isConnected")
            Toast.makeText(this@MainActivity, "✅ Printer Connected", Toast.LENGTH_SHORT).show()

            // Auto test print
            try {
                printText("=== SUNMI SDK TEST ===\nConnected OK\n\n")
            } catch (e: Exception) {
                Log.e(TAG, "Auto test print failed: ${e.message}")
            }
        }

        override fun onDisconnected() {
            sunmiService = null
            isConnected = false
            Log.e(TAG, "❌ Sunmi printer disconnected")
            Toast.makeText(this@MainActivity, "❌ Printer Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ bind via Official SDK
        try {
            InnerPrinterManager.getInstance().bindService(this, innerPrinterCallback)
            Log.e(TAG, "bindService called (SDK)")
        } catch (e: Exception) {
            Log.e(TAG, "bindService (SDK) error: ${e.message}")
            Toast.makeText(this, "❌ SDK bind error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            InnerPrinterManager.getInstance().unBindService(this, innerPrinterCallback)
        } catch (_: Exception) {}
        sunmiService = null
        isConnected = false
    }

    private fun printText(text: String): Boolean {
        val svc = sunmiService ?: return false
        return try {
            // init
            try { svc.printerInit(null) } catch (_: Exception) {}

            // optional: align left (0), center (1), right (2)
            try { svc.setAlignment(0, null) } catch (_: Exception) {}

            svc.printText(text, null)
            try { svc.lineWrap(3, null) } catch (_: Exception) {}

            true
        } catch (e: Exception) {
            Log.e(TAG, "printText error: ${e.message}")
            false
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {

                    "startPrint" -> {
                        val args = call.arguments as? Map<*, *>
                        val text = (args?.get("print_text") as? String) ?: ""

                        if (!isConnected || sunmiService == null) {
                            Toast.makeText(this, "❌ Printer not connected", Toast.LENGTH_SHORT).show()
                            result.error("NOT_CONNECTED", "Printer not connected", null)
                            return@setMethodCallHandler
                        }

                        val ok = printText(text + "\n")
                        if (ok) {
                            Toast.makeText(this, "✅ Printed", Toast.LENGTH_SHORT).show()
                            result.success(true)
                        } else {
                            Toast.makeText(this, "❌ Print failed", Toast.LENGTH_SHORT).show()
                            result.error("PRINT_FAIL", "Print failed", null)
                        }
                    }

                    "checkPrinterStatus" -> {
                        result.success(isConnected && sunmiService != null)
                    }

                    else -> result.notImplemented()
                }
            }
    }
}
