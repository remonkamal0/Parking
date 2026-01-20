import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:new_parking/app/route_api.dart';
import 'package:new_parking/data/all_parking_response.dart';
import 'package:new_parking/local_storage.dart';

class ParkingDetailsScreen extends StatefulWidget {
  final dynamic parkingCode;
  final bool fromHome;

  const ParkingDetailsScreen({
    super.key,
    required this.parkingCode,
    this.fromHome = false,
  });

  @override
  State<ParkingDetailsScreen> createState() => _ParkingDetailsScreenState();
}

class _ParkingDetailsScreenState extends State<ParkingDetailsScreen> {
  ParkingModel? parkingModel;
  String printText = 'Loading...';

  @override
  void initState() {
    super.initState();
    debugPrint('Parking Details id =====>${widget.parkingCode}');
    getSinglePark(widget.parkingCode);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Parking Details')),
      body: Padding(
        padding: const EdgeInsets.all(18.0),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 10),
              const Padding(
                padding: EdgeInsets.all(8.0),
                child: Text('Invoice', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: Text(
                  printText,
                  style: const TextStyle(fontWeight: FontWeight.w400, fontSize: 14),
                ),
              ),
              const SizedBox(height: 12),

              widget.fromHome
                  ? SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.cyan,
                    padding: const EdgeInsets.all(15),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                  ),
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Back',style: TextStyle(color: Colors.white),),
                ),
              )
                  : SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    padding: const EdgeInsets.all(15),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                  ),
                  onPressed: () async {
                    await endParkedCar();
                    await _startPrint();
                  },
                  child: const Text('End Parking'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _startPrint() async {
    try {
      final parameters = {
        'print_text': printText,
        'parking_id': parkingModel?.code.toString(),
      };

      await const MethodChannel('com.example.new_parking/print').invokeMethod(
        'startPrint',
        Map.from(parameters),
      );
    } on PlatformException catch (e) {
      debugPrint("Print error: '${e.message}'.");
    }

    if (!mounted) return;
    Navigator.of(context).pop();
  }

  Future<Map<String, String>> _headers() async {
    final token = await LocalStorage.getString(LocalStorage.apiToken);
    return <String, String>{
      'Authorization': 'Bearer ${token ?? ''}',
      'Content-Type': 'application/json; charset=UTF-8',
      'Accept': 'application/json',
      'Accept-Encoding': 'identity',
    };
  }

  Future<void> endParkedCar() async {
    final url = Uri.parse('${RouteApi.mainUrl}${RouteApi.parkingCar}/${widget.parkingCode}');
    debugPrint("END URL => $url");

    final response = await http.patch(
      url,
      headers: await _headers(),
      body: jsonEncode({'id': widget.parkingCode.toString()}),
    );

    debugPrint("END STATUS => ${response.statusCode}");
    debugPrint("END BODY => '${response.body}'");

    if (response.statusCode == 200 && response.body.trim().isNotEmpty) {
      final jsonResponse = jsonDecode(response.body) as Map<String, dynamic>;
      parkingModel = ParkingModel.fromJson(jsonResponse['data']);
      printText = parkingModel?.printText ?? 'No Data To Be Printed!';
      setState(() {});
    } else {
      debugPrint('End Parking failed: ${response.body}');
    }
  }

  Future<void> getSinglePark(dynamic parkingId) async {
    final url = Uri.parse('${RouteApi.mainUrl}${RouteApi.parkingCar}/$parkingId');
    debugPrint("GET URL => $url");

    final response = await http.get(
      url,
      headers: await _headers(),
    );

    debugPrint("GET STATUS => ${response.statusCode}");
    debugPrint("GET BODY => '${response.body}'");

    if (response.statusCode == 200 && response.body.trim().isNotEmpty) {
      final jsonResponse = jsonDecode(response.body) as Map<String, dynamic>;
      parkingModel = ParkingModel.fromJson(jsonResponse['data']);
      printText = parkingModel?.printText ?? 'No Data To Be Printed!';
      setState(() {});
    } else {
      setState(() => printText = 'No Data To Be Printed!');
      debugPrint('Get details failed: ${response.body}');
    }
  }
}
