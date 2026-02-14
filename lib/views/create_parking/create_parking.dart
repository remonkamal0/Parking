import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:new_parking/data/all_parking_response.dart';
import '../../app/route_api.dart';
import '../../local_storage.dart';
import '../details/parking_details_screen.dart';
import '../../services/sunmi_printer_service.dart';
import '../../services/printer_debug_service.dart';

class CreateParkingScreen extends StatefulWidget {
  const CreateParkingScreen({super.key});

  @override
  State<CreateParkingScreen> createState() => _CreateParkingScreenState();
}

class _CreateParkingScreenState extends State<CreateParkingScreen> {
  final _formKey = GlobalKey<FormState>();

  final _clientName = TextEditingController();

  final _clientCarNumber = TextEditingController();

  final _clientIdentificationNumber = TextEditingController();

  final _licenceNumber = TextEditingController();

  final _clientPhone = TextEditingController();

  int _parkingType = 0;

  ParkingModel? parkingModel;

  // Constants for parking types
  static const int VALET_PARKING = 0;
  static const int VIP_PARKING = 1;
  static const int PER_HOUR_PARKING = 2;
  static const int FINE_PARKING = 3;

  // Hourly rate for per hour parking
  static const double HOURLY_RATE = 25.0;

  @override
  void dispose() {
    _clientName.dispose();
    _clientCarNumber.dispose();
    _clientIdentificationNumber.dispose();
    _licenceNumber.dispose();
    _clientPhone.dispose();

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
         backgroundColor: Colors.cyan,
          title: const Text(
        'Create new parking',
        style: TextStyle(color: Colors.white),
      )),
      body: Padding(
        padding: const EdgeInsets.all(12.0),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(height: 30),
              // const Padding(
              //   padding: EdgeInsets.all(8.0),
              //   child: Text(
              //     'Parking Information',
              //     style: TextStyle(fontWeight: FontWeight.w600),
              //   ),
              // ),
              // Container(
              //   height: 50,
              //   alignment: Alignment.centerLeft,
              //   decoration: BoxDecoration(
              //     color: Colors.white,
              //     borderRadius: BorderRadius.circular(10),
              //     boxShadow: const [
              //       BoxShadow(
              //         color: Colors.black26,
              //         blurRadius: 6,
              //         offset: Offset(0, 2),
              //       )
              //     ],
              //   ),
              //   child: TextField(
              //     controller: _clientName,
              //     keyboardType: TextInputType.text,
              //     style: const TextStyle(
              //       color: Colors.black87,
              //     ),
              //     decoration: const InputDecoration(
              //       border: InputBorder.none,
              //       contentPadding: EdgeInsets.all(12),
              //       // prefixIcon: Icon(
              //       //   Icons.person,
              //       //   color: Color(0xff5ac18e),
              //       // ),
              //       hintText: "Client Name",
              //       hintStyle: TextStyle(
              //         color: Colors.black38,
              //       ),
              //     ),
              //   ),
              // ),
              // const SizedBox(height: 10),
              // Container(
              //   height: 50,
              //   alignment: Alignment.centerLeft,
              //   decoration: BoxDecoration(
              //     color: Colors.white,
              //     borderRadius: BorderRadius.circular(10),
              //     boxShadow: const [
              //       BoxShadow(
              //         color: Colors.black26,
              //         blurRadius: 6,
              //         offset: Offset(0, 2),
              //       )
              //     ],
              //   ),
              //   child: TextField(
              //     controller: _clientCarNumber,
              //     keyboardType: TextInputType.number,
              //     style: const TextStyle(
              //       color: Colors.black87,
              //     ),
              //     decoration: const InputDecoration(
              //       border: InputBorder.none,
              //       contentPadding: EdgeInsets.all(12),
              //       // prefixIcon: Icon(
              //       //   Icons.person,
              //       //   color: Color(0xff5ac18e),
              //       // ),
              //       hintText: "Car Number",
              //       hintStyle: TextStyle(
              //         color: Colors.black38,
              //       ),
              //     ),
              //   ),
              // ),
              // const SizedBox(height: 10),
              // Container(
              //   height: 50,
              //   alignment: Alignment.centerLeft,
              //   decoration: BoxDecoration(
              //     color: Colors.white,
              //     borderRadius: BorderRadius.circular(10),
              //     boxShadow: const [
              //       BoxShadow(
              //         color: Colors.black26,
              //         blurRadius: 6,
              //         offset: Offset(0, 2),
              //       )
              //     ],
              //   ),
              //   child: TextField(
              //     controller: _clientIdentificationNumber,
              //     keyboardType: TextInputType.text,
              //     style: const TextStyle(
              //       color: Colors.black87,
              //     ),
              //     decoration: const InputDecoration(
              //       border: InputBorder.none,
              //       contentPadding: EdgeInsets.all(12),
              //       // prefixIcon: Icon(
              //       //   Icons.person,
              //       //   color: Color(0xff5ac18e),
              //       // ),
              //       hintText: "Identification Number",
              //       hintStyle: TextStyle(
              //         color: Colors.black38,
              //       ),
              //     ),
              //   ),
              // ),
              // const SizedBox(height: 10),
              // Container(
              //   height: 50,
              //   alignment: Alignment.centerLeft,
              //   decoration: BoxDecoration(
              //     color: Colors.white,
              //     borderRadius: BorderRadius.circular(10),
              //     boxShadow: const [
              //       BoxShadow(
              //         color: Colors.black26,
              //         blurRadius: 6,
              //         offset: Offset(0, 2),
              //       )
              //     ],
              //   ),
              //   child: TextField(
              //     controller: _licenceNumber,
              //     keyboardType: TextInputType.text,
              //     style: const TextStyle(
              //       color: Colors.black87,
              //     ),
              //     decoration: const InputDecoration(
              //       border: InputBorder.none,
              //       contentPadding: EdgeInsets.all(12),
              //       // prefixIcon: Icon(
              //       //   Icons.person,
              //       //   color: Color(0xff5ac18e),
              //       // ),
              //       hintText: "Licence Number",
              //       hintStyle: TextStyle(
              //         color: Colors.black38,
              //       ),
              //     ),
              //   ),
              // ),
              // const SizedBox(height: 10),
              // Container(
              //   height: 50,
              //   alignment: Alignment.centerLeft,
              //   decoration: BoxDecoration(
              //     color: Colors.white,
              //     borderRadius: BorderRadius.circular(10),
              //     boxShadow: const [
              //       BoxShadow(
              //         color: Colors.black26,
              //         blurRadius: 6,
              //         offset: Offset(0, 2),
              //       )
              //     ],
              //   ),
              //   child: TextField(
              //     controller: _clientPhone,
              //     keyboardType: TextInputType.text,
              //     style: const TextStyle(
              //       color: Colors.black87,
              //     ),
              //     decoration: const InputDecoration(
              //       border: InputBorder.none,
              //       contentPadding: EdgeInsets.all(12),
              //       // prefixIcon: Icon(
              //       //   Icons.person,
              //       //   color: Color(0xff5ac18e),
              //       // ),
              //       hintText: "Client Phone",
              //       hintStyle: TextStyle(
              //         color: Colors.black38,
              //       ),
              //     ),
              //   ),
              // ),
              const SizedBox(height: 10),
              const Padding(
                padding: EdgeInsets.all(8.0),
                child: Text(
                  'Parking Type',
                  style: TextStyle(fontWeight: FontWeight.w600, fontSize: 20),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Radio(
                      value: 0,
                      groupValue: _parkingType,
                      onChanged: (value) {
                        _parkingType = value ?? 0;
                        setState(() {});
                        debugPrint('Current parking type $value');
                      }),
                  const Expanded(
                    child: Text(
                      'Valet Parking',
                      maxLines: 2,
                      style:
                          TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Radio(
                      value: 1,
                      groupValue: _parkingType,
                      onChanged: (value) {
                        _parkingType = value ?? 0;
                        setState(() {});
                        debugPrint('Current parking type $value');
                      }),
                  const Expanded(
                    child: Text(
                      'Vip Parking',
                      maxLines: 2,
                      style:
                          TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Radio(
                      value: 2,
                      groupValue: _parkingType,
                      onChanged: (value) {
                        _parkingType = value ?? 0;
                        setState(() {});
                        debugPrint('Current parking type $value');
                      }),
                  const Expanded(
                    child: Text(
                      'Per Hour Parking',
                      maxLines: 2,
                      style:
                          TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Radio(
                      value: 3,
                      groupValue: _parkingType,
                      onChanged: (value) {
                        _parkingType = value ?? 0;
                        setState(() {});
                        debugPrint('Current parking type $value');
                      }),
                  const Expanded(
                    child: Text(
                      'Fine Parking',
                      maxLines: 2,
                      style:
                          TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 40),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 25),
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                     backgroundColor: Colors.cyan,
                    padding: const EdgeInsets.all(15),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(15),
                    ),
                  ),
                  child: const Text(
                    'Create',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  onPressed: () {
                    createParkingCar(context);
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  createParkingCar(context) async {
    var url = Uri.parse(RouteApi.mainUrl + RouteApi.parkingCar);

    var response = await http.post(
      url,
      headers: <String, String>{
        'Authorization':
            'Bearer ${LocalStorage.getString(LocalStorage.apiToken)}',
        'Content-Type': 'application/json; charset=UTF-8',
        'Accept': 'application/json',
      },
      body: jsonEncode(
        {
          // 'clientName': clientName,
          // 'clientCarNumber': clientCarNumber,
          // 'clientIdentificationNumber': clientIdentificationNumber,
          // 'licenceNumber': licenceNumber,
          // 'clientPhone': clientPhone,
          'type': _parkingType.toString()
        },
      ),
    );

    if (response.statusCode == 200) {
      var jsonResponse = jsonDecode(response.body) as Map<String, dynamic>;
      parkingModel = ParkingModel.fromJson(jsonResponse['data']);
      setState(() {});
      debugPrint('Response : ${jsonResponse["message"]}.');
      if (parkingModel != null) {
        // Check if it's per hour parking and print receipt
        if (_parkingType == PER_HOUR_PARKING) {
          await _printPerHourParkingReceipt();
        } else {
          // Use existing print method for other parking types
          _startPrint(parkingModel?.printText ?? '', parkingModel?.code.toString());
        }
        
        debugPrint(jsonResponse.toString());
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => ParkingDetailsScreen(
              parkingCode: parkingModel?.code,
              fromHome: true,
            ),
          ),
        );
      }
    } else {
      debugPrint('Request failed with status: ${response.body}.');
    }
  }

  Future<void> _printPerHourParkingReceipt() async {
    try {
      // Show loading indicator
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const AlertDialog(
          content: Row(
            children: [
              CircularProgressIndicator(),
              SizedBox(width: 20),
              Text('Printing receipt...'),
            ],
          ),
        ),
      );

      // First try direct print test
      print('Testing direct print...');
      try {
        await SunmiPrinterService.testDirectPrint();
        print('Direct print test successful');
      } catch (e) {
        print('Direct print test failed: $e');
      }
      
      // Wait a moment for tests to complete
      await Future.delayed(const Duration(seconds: 1));

      // Check printer status (but allow printing even if check fails)
      bool printerReady = await SunmiPrinterService.checkPrinterStatus();
      print('Printer status check result: $printerReady');
      
      // Always try to print regardless of status check
      print('Attempting to print receipt...');

      // Print the receipt
      await SunmiPrinterService.printPerHourParkingReceipt(
        parkingCode: parkingModel?.code.toString() ?? '',
        carNumber: _clientCarNumber.text.isNotEmpty ? _clientCarNumber.text : 'N/A',
        entryTime: DateTime.now(),
        hourlyRate: HOURLY_RATE,
      );

      Navigator.pop(context); // Close loading dialog
      _showSuccessDialog('Receipt printed successfully!');
      
    } catch (e) {
      Navigator.pop(context); // Close loading dialog
      print('Print error details: $e');
      
      // Show more helpful error message
      String errorMessage = e.toString();
      if (errorMessage.contains('All printing methods failed')) {
        _showErrorDialog('Printer not responding. Please check:\n\n1. Device is Sunmi V2S\n2. Printer has paper\n3. Printer is powered on\n4. Try restarting the device\n\nIf problem persists, contact support.');
      } else {
        _showErrorDialog('Print attempt failed: ${errorMessage}\n\nThe app tried multiple printing methods.\nPlease ensure:\n1. This is a Sunmi V2S device\n2. Printer is powered on\n3. Paper is loaded');
      }
    }
  }

  void _showSuccessDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Success'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  void _showErrorDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Error'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  Future<void> _startPrint(String text, String? parkingId) async {
    try {
      var parameters = {
        'print_text': text,
        'parking_id': parkingId ?? '',
      };

      await const MethodChannel('com.example.new_parking/print').invokeMethod(
        'startPrint',
        Map.from(parameters),
      );
    } on PlatformException catch (e) {
      debugPrint("Failed to get printer status: '${e.message}'.");
    }
  }
}
