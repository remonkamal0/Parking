import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/services.dart';

class SunmiPrinterService {
  static const MethodChannel _channel = MethodChannel('com.example.new_parking/print');

  static Future<bool> initPrinter() async {
    try {
      await _channel.invokeMethod('bindPrinterService');
      print('Printer service bound successfully');
      return true;
    } catch (e) {
      print('Printer initialization failed: $e');
      return false;
    }
  }

  static Future<void> printPerHourParkingReceipt({
    required String parkingCode,
    required String carNumber,
    required DateTime entryTime,
    required double hourlyRate,
  }) async {
    try {
      // Initialize printer
      await initPrinter();

      // Create receipt text
      String receiptText = _createReceiptText(
        parkingCode: parkingCode,
        carNumber: carNumber,
        entryTime: entryTime,
        hourlyRate: hourlyRate,
      );

      // Print the receipt using fallback method
      await _channel.invokeMethod('startPrint', {
        'print_text': receiptText,
        'parking_id': parkingCode,
      });

      print('Receipt printed successfully');
    } catch (e) {
      print('Failed to print receipt: $e');
      throw Exception('Printer error: $e');
    }
  }

  static Future<void> printTestReceipt() async {
    try {
      await initPrinter();
      
      String testText = _createTestReceiptText();
      
      await _channel.invokeMethod('startPrint', {
        'print_text': testText,
        'parking_id': 'TEST',
      });
      
      print('Test receipt printed successfully');
    } catch (e) {
      print('Failed to print test receipt: $e');
      throw Exception('Printer test error: $e');
    }
  }

  static Future<void> testDirectPrint() async {
    try {
      await _channel.invokeMethod('testDirectPrint');
      print('Direct print test completed');
    } catch (e) {
      print('Direct print test failed: $e');
      throw Exception('Direct print test error: $e');
    }
  }

  static Future<bool> checkPrinterStatus() async {
    try {
      // Try to bind service first if not already bound
      await bindPrinterService();
      
      // Wait a moment for service to connect
      await Future.delayed(const Duration(milliseconds: 500));
      
      final status = await _channel.invokeMethod('checkPrinterStatus');
      print('Printer status: $status');
      return status == true;
    } catch (e) {
      print('Failed to check printer status: $e');
      // Return true to allow printing attempt even if status check fails
      return true;
    }
  }

  static Future<void> printCustomText(String text) async {
    try {
      await _channel.invokeMethod('startPrint', {
        'print_text': text,
        'parking_id': 'CUSTOM',
      });
    } catch (e) {
      print('Failed to print custom text: $e');
    }
  }

  static String _createReceiptText({
    required String parkingCode,
    required String carNumber,
    required DateTime entryTime,
    required double hourlyRate,
  }) {
    return '''
=====================================
           PARKING RECEIPT
=====================================

Parking Type: PER HOUR PARKING
Parking Code: $parkingCode
Car Number: $carNumber
Entry Time: ${_formatDateTime(entryTime)}
Hourly Rate: ${hourlyRate.toStringAsFixed(2)} EGP

-------------------------------------

Thank you for using our parking service!

       QR Code: $parkingCode

=====================================
''';
  }

  static String _createTestReceiptText() {
    return '''
=====================================
           TEST RECEIPT
=====================================

Date: ${_formatDateTime(DateTime.now())}
Printer: Sunmi V2S
Status: Working

Test completed successfully!

=====================================
''';
  }

  static String _formatDateTime(DateTime dateTime) {
    return '${dateTime.day.toString().padLeft(2, '0')}/'
           '${dateTime.month.toString().padLeft(2, '0')}/'
           '${dateTime.year} '
           '${dateTime.hour.toString().padLeft(2, '0')}:'
           '${dateTime.minute.toString().padLeft(2, '0')}';
  }

  static Future<void> bindPrinterService() async {
    try {
      await _channel.invokeMethod('bindPrinterService');
    } catch (e) {
      print('Failed to bind printer service: $e');
    }
  }

  static Future<void> unbindPrinterService() async {
    try {
      await _channel.invokeMethod('unbindPrinterService');
    } catch (e) {
      print('Failed to unbind printer service: $e');
    }
  }
}
