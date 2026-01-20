package com.example.parking

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import woyou.aidlservice.jiuiv5.IWoyouService

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.example.parking/print"
    private var woyouService: IWoyouService? = null
    private var isBound = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            woyouService = IWoyouService.Stub.asInterface(service)
            isBound = true
            Log.d("SUNMI", "Printer service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            woyouService = null
            isBound = false
            Log.d("SUNMI", "Printer service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindSunmiService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(conn)
            isBound = false
        }
    }

    private fun bindSunmiService() {
        try {
            val intent = Intent()
            intent.setPackage("woyou.aidlservice.jiuiv5")
            intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
            val ok = bindService(intent, conn, Context.BIND_AUTO_CREATE)
            Log.d("SUNMI", "bindService: $ok")
        } catch (e: Exception) {
            Log.e("SUNMI", "bindSunmiService error: ${e.message}")
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

                        printText(text) { success, err ->
                            if (success) result.success(true) else result.error("PRINT_FAIL", err ?: "Unknown", null)
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun printText(text: String, done: (Boolean, String?) -> Unit) {
        val svc = woyouService
        if (svc == null) {
            done(false, "SUNMI printer service not connected")
            return
        }

        try {
            svc.printerInit(null)
            svc.printText(text + "\n", null)
            svc.lineWrap(3, null)
            done(true, null)
        } catch (e: Exception) {
            done(false, e.message)
        }
    }
}
