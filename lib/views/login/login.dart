import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:new_parking/app/route_api.dart';
import 'package:new_parking/local_storage.dart';
import 'package:new_parking/views/home_page/home_page.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  late TextEditingController _phone;
  late TextEditingController _password;
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  @override
  void initState() {
    super.initState();
    _phone = TextEditingController();
    _password = TextEditingController();
  }

  @override
  void dispose() {
    _phone.dispose();
    _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: Colors.grey.shade100,
      body: SizedBox(
        height: double.infinity,
        width: double.infinity,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: EdgeInsets.symmetric(
            horizontal: 25,
            vertical: MediaQuery.of(context).size.height * 0.2,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Image.asset(
                'assets/logo.png',
                width: 100,
                height: 100,
                fit: BoxFit.cover,
              ),
              const Padding(
                padding: EdgeInsets.all(18.0),
                child: Text(
                  'valet & parking management',
                  style: TextStyle(
                    color: Colors.cyan,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
              const SizedBox(height: 50),
              Container(
                alignment: Alignment.centerLeft,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(10),
                  boxShadow: const [
                    BoxShadow(
                      color: Colors.black26,
                      blurRadius: 6,
                      offset: Offset(0, 2),
                    ),
                  ],
                ),
                height: 60,
                child: TextField(
                  controller: _phone,
                  keyboardType: TextInputType.text,
                  textInputAction: TextInputAction.next,
                  style: const TextStyle(
                    color: Colors.black87,
                  ),
                  decoration: const InputDecoration(
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.only(top: 14),
                    prefixIcon: Icon(
                      Icons.phone,
                      color: Colors.cyan,
                    ),
                    hintText: "phone",
                    hintStyle: TextStyle(
                      color: Colors.black38,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Container(
                alignment: Alignment.centerLeft,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(10),
                  boxShadow: const [
                    BoxShadow(
                      color: Colors.black26,
                      blurRadius: 6,
                      offset: Offset(0, 2),
                    ),
                  ],
                ),
                height: 60,
                child: TextField(
                  controller: _password,
                  obscureText: true,
                  style: const TextStyle(
                    color: Colors.black87,
                  ),
                  decoration: const InputDecoration(
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.only(top: 14),
                    prefixIcon: Icon(
                      Icons.lock,
                      color: Colors.cyan,
                    ),
                    hintText: "password",
                    hintStyle: TextStyle(
                      color: Colors.black38,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.all(18.0),
                child: SizedBox(
                  width: MediaQuery.of(context).size.width,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.cyan,
                      padding: const EdgeInsets.all(15),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(15),
                      ),
                    ),
                    child: const Text(
                      'Login',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color:Colors.white
                      ),
                    ),
                    onPressed: () {
                      login(context, _phone.text, _password.text);
                    },
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  login(context, phone, password) async {
    var url = Uri.parse(RouteApi.mainUrl + RouteApi.login);

    var response = await http.post(
      url,
      headers: <String, String>{
        'Content-Type': 'application/json; charset=UTF-8',
      },
      body: jsonEncode(<String, String>{
        'phone': phone,
        'password': password,
      }),
    );

    if (response.statusCode == 200) {
      var jsonResponse = jsonDecode(response.body) as Map<String, dynamic>;
      debugPrint(jsonResponse.toString());
      var token = jsonResponse['data']['token'];
      var garage = jsonResponse['data']['garage']['id'];
      await LocalStorage.setString(LocalStorage.apiToken, token);
      await LocalStorage.setString(LocalStorage.garageId, garage.toString());
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => const MainHomePage(),
        ),
      );

      debugPrint('token: $token.');
    } else {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text(
            'Login Error',
            style: TextStyle(
              color: Colors.red,
            ),
          ),
          content: Text(
            jsonDecode(response.body)['message'].toString(),
          ),
        ),
      );

      debugPrint('Request failed : ${response.body.toString()}');
    }
  }
}
