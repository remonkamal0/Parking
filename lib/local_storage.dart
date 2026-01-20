import 'package:get_storage/get_storage.dart';

class LocalStorage {
  static const String apiToken = 'token';
  static const String garageId = 'garage_id';

  static init() async {
    await GetStorage.init();
  }

  /// =============================================================
  static Future setString(String key, String? value) async {
    await GetStorage().write(key, value);
  }

  static String? getString(String key) {
    String? value = GetStorage().read(key);
    return value;
  }


  static Future<void> signOut() async {
    await GetStorage().erase();
  }

}
