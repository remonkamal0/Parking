import 'package:flutter/services.dart';

class PrinterDebugService {
  static const MethodChannel _channel = MethodChannel('com.example.new_parking/print');

  static Future<void> testPrinterConnection() async {
    print('=== Testing Printer Connection ===');
    
    try {
      // Test 1: Bind service
      print('1. Testing service binding...');
      await _channel.invokeMethod('bindPrinterService');
      print('   Service bound successfully');
      
      // Wait for connection
      await Future.delayed(const Duration(seconds: 1));
      
      // Test 2: Check status
      print('2. Testing printer status...');
      final status = await _channel.invokeMethod('checkPrinterStatus');
      print('   Printer status: $status');
      
      // Test 3: Print test text
      print('3. Testing print functionality...');
      await _channel.invokeMethod('startPrint', {
        'print_text': '=== PRINTER TEST ===\nDate: ${DateTime.now()}\nStatus: Working\n=== END TEST ===',
        'parking_id': 'TEST',
      });
      print('   Test print sent successfully');
      
      print('=== All tests completed ===');
      
    } catch (e) {
      print('=== Test failed ===');
      print('Error: $e');
      print('=== End test ===');
    }
  }

  static Future<void> printDetailedInfo() async {
    try {
      await _channel.invokeMethod('startPrint', {
        'print_text': '''
=====================================
         PRINTER DIAGNOSTIC
=====================================

Test Time: ${DateTime.now()}
Device: Sunmi V2S
App: Parking System

Status Check: ${DateTime.now()}

If you can read this,
the printer is working!

=====================================
''',
        'parking_id': 'DIAG',
      });
    } catch (e) {
      print('Diagnostic print failed: $e');
    }
  }
}
