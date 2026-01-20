import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:new_parking/data/all_parking_response.dart';

import '../../../app/route_api.dart';
import '../../../local_storage.dart';
import '../../create_parking/create_parking.dart';
import 'package:http/http.dart' as http;

import '../../details/parking_details_screen.dart';

class AllParkingScreen extends StatefulWidget {
  const AllParkingScreen({super.key});

  @override
  State<AllParkingScreen> createState() => _AllParkingScreenState();
}

class _AllParkingScreenState extends State<AllParkingScreen> {
  List<ParkingModel> _allParking = [];
  bool loading = false;

  getAllParking() async {
    setState(() {
      loading = true;
    });
    var url =
        Uri.parse('${RouteApi.mainUrl}${RouteApi.allParkingCars}/${LocalStorage.getString(LocalStorage.garageId)}');

    var response = await http.get(
      url,
      headers: <String, String>{
        'Authorization': 'Bearer ${LocalStorage.getString(LocalStorage.apiToken)}',
        'Content-Type': 'application/json; charset=UTF-8',
        'Accept': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      var jsonResponse = jsonDecode(response.body) as Map<String, dynamic>;

      debugPrint(jsonResponse.toString());
      AllParkingResponse allParkingResponse = AllParkingResponse.fromJson(jsonResponse);
      _allParking = allParkingResponse.data ?? [];
      debugPrint('Number of books about http: ${jsonResponse["message"]}.');
    } else {
      debugPrint('Request failed with status: ${response.body}.');
    }

    setState(() {
      loading = false;
    });
  }

  @override
  void initState() {
    super.initState();
    getAllParking();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Home')),
      floatingActionButton: FloatingActionButton(
        backgroundColor: Theme.of(context).primaryColor,
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => const CreateParkingScreen(),
            ),
          );
        },
        child: const Icon(
          Icons.add,
          color: Colors.white,
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(4.0),
        child: loading
            ? const Center(child: CircularProgressIndicator())
            : _allParking.isEmpty
                ? const Center(child: Text('No Parking'))
                : ListView.separated(
                    itemCount: _allParking.length,
                    separatorBuilder: (context, index) => const SizedBox(height: 5),
                    itemBuilder: (context, index) {
                      return Card(
                        elevation: 2.0,
                        child: ListTile(
                          leading: const Icon(Icons.car_repair, size: 40),
                          title:
                              Text(_allParking[index].clientName ?? '', style: Theme.of(context).textTheme.labelLarge),
                          subtitle: Text(_allParking[index].code ?? '', style: Theme.of(context).textTheme.bodySmall),
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => ParkingDetailsScreen(
                                  parkingCode: _allParking[index].code!,
                                ),
                              ),
                            );
                          },
                        ),
                      );
                    },
                  ),
      ),
    );
  }
}
