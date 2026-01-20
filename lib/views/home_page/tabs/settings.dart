import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:new_parking/local_storage.dart';
import 'package:new_parking/views/login/login.dart';
import 'package:http/http.dart' as http;
import 'package:quickalert/models/quickalert_animtype.dart';
import 'package:quickalert/models/quickalert_type.dart';
import 'package:quickalert/widgets/quickalert_dialog.dart';
import '../../../app/route_api.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  String currentBalance = '0';

  @override
  void initState() {
    super.initState();
    getCurrentBalance();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: SizedBox(
        width: MediaQuery.of(context).size.width,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(
                'Current Balance: $currentBalance',
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
              ),
            ),
            ElevatedButton.icon(
              onPressed: () {
                getCurrentBalance();
              },
              icon: const Icon(
                Icons.refresh,
                color: Colors.white,
              ),
              label: const Text(
                'Get Balance',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.cyan, // الخلفية Cyan
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),

            Padding(
              padding: const EdgeInsets.all(28.0),
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  padding: const EdgeInsets.all(15),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(15),
                  ),
                ),
                onPressed: () async {
                  QuickAlert.show(
                    context: context,
                    type: QuickAlertType.info,
                    animType: QuickAlertAnimType.slideInDown,
                    text: 'Do you want to logout',
                    showCancelBtn: true,
                    title: 'Logout',
                    confirmBtnText: 'Yes',
                    cancelBtnText: 'No',
                    confirmBtnColor: Colors.green,
                    onConfirmBtnTap: () async {
                      await LocalStorage.signOut();
                      // ignore: use_build_context_synchronously
                      Navigator.of(context).pop();
                      // ignore: use_build_context_synchronously
                      Navigator.of(context).pop();
                      // ignore: use_build_context_synchronously
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const LoginScreen(),
                        ),
                      );
                    },
                    onCancelBtnTap: () {
                      Navigator.of(context).pop();
                    },
                  );
                },
                child: const Text('Logout',style: TextStyle(color: Colors.white),),
              ),
            )
          ],
        ),
      ),
    );
  }

  void getCurrentBalance() async {
    var url = Uri.parse('${RouteApi.mainUrl}${RouteApi.balance}');

    debugPrint(url.toString());
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
      setState(() {
        currentBalance = jsonResponse['current_balance'] ?? '0';
      });
      debugPrint('Response: ${jsonResponse["message"]}.');
    } else {
      debugPrint('Request failed with status: ${response.body}.');
    }
  }
}
