package com.example.parking

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

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

            Log.e(TAG, "✅ Printer connected = $isConnected")
            Toast.makeText(this@MainActivity, "✅ Printer Connected", Toast.LENGTH_SHORT).show()

            // ✅ Auto test
            try {
                printReceiptWithBarcode(
                    title = "SUNMI TEST",
                    barcodeValue = "1234567890",
                    bodyText = "Connected OK\n\n"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Auto test failed: ${e.message}")
            }
        }

        override fun onDisconnected() {
            sunmiService = null
            isConnected = false
            Log.e(TAG, "❌ Printer disconnected")
            Toast.makeText(this@MainActivity, "❌ Printer Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            InnerPrinterManager.getInstance().bindService(this, innerPrinterCallback)
            Log.e(TAG, "bindService called (SDK)")
        } catch (e: Exception) {
            Log.e(TAG, "bindService error: ${e.message}")
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

    // =========================
    // ✅ Receipt + BARCODE (مع fallback)
    // =========================
    private fun printReceiptWithBarcode(
        title: String,
        barcodeValue: String,
        bodyText: String
    ): Boolean {

        val svc = sunmiService ?: return false

        val safeCode = barcodeValue.trim()

        return try {
            try { svc.printerInit(null) } catch (_: Exception) {}

            // Title center
            try { svc.setAlignment(1, null) } catch (_: Exception) {}
            svc.printText("$title\n", null)
            svc.printText("--------------------------\n", null)

            // Body left
            try { svc.setAlignment(0, null) } catch (_: Exception) {}
            if (bodyText.isNotBlank()) {
                svc.printText(bodyText.trimEnd() + "\n\n", null)
            }

            // لو الكود فاضي: اطبع من غير باركود بدل ما تفشل
            if (safeCode.isBlank()) {
                svc.printText("⚠️ Barcode value is empty\n", null)
                try { svc.lineWrap(4, null) } catch (_: Exception) {}
                return true
            }

            // Barcode center
            try { svc.setAlignment(1, null) } catch (_: Exception) {}

            // CODE128
            svc.printBarCode(
                safeCode,
                8,      // CODE128
                200,    // height
                3,      // width
                2,      // show text under barcode
                null
            )

            try { svc.lineWrap(4, null) } catch (_: Exception) {}
            true

        } catch (e: Exception) {
            Log.e(TAG, "printReceiptWithBarcode error: ${e.message}")
            false
        }
    }

    // ✅ helper: هات الكود من أي key مشهور
    private fun readBarcodeFromArgs(args: Map<*, *>?): String {
        fun getStr(key: String): String {
            return (args?.get(key) as? String)?.trim().orEmpty()
        }

        // جرّب كل الأسماء الممكنة
        val candidates = listOf(
            getStr("code"),
            getStr("parking_id"),
            getStr("parkingCode"),
            getStr("parking_code"),
            getStr("parkingId"),
        )

        return candidates.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->

                when (call.method) {

                    "startPrint" -> {
                        val args = call.arguments as? Map<*, *>
                        val text = (args?.get("print_text") as? String) ?: ""

                        // ✅ هنا بقى الحل: اقرأ الكود من أكتر من key
                        val code = readBarcodeFromArgs(args)

                        Log.e(TAG, "startPrint => code='$code' text_len=${text.length}")

                        if (!isConnected || sunmiService == null) {
                            Toast.makeText(this, "❌ Printer not connected", Toast.LENGTH_SHORT).show()
                            result.error("NOT_CONNECTED", "Printer not connected", null)
                            return@setMethodCallHandler
                        }

                        val ok = printReceiptWithBarcode(
                            title = "PARKING RECEIPT",
                            barcodeValue = code,
                            bodyText = text
                        )

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
