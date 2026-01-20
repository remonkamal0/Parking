class AllParkingResponse {
  AllParkingResponse({
    this.success,
    this.message,
    this.data,
  });

  AllParkingResponse.fromJson(dynamic json) {
    success = json['success'];
    message = json['message'];
    if (json['data'] != null) {
      data = [];
      json['data'].forEach((v) {
        data?.add(ParkingModel.fromJson(v));
      });
    }
  }

  bool? success;
  String? message;
  List<ParkingModel>? data;
}

class ParkingModel {
  ParkingModel({
    this.id,
    this.clientName,
    this.clientCarNumber,
    this.clientIdentificationNumber,
    this.licenceNumber,
    this.clientPhone,
    this.code,
    this.costType,
    this.printText,
    this.cost,
    this.notes,
    this.startsAt,
    this.endsAt,
    this.saies,
    this.garage,
  });

  ParkingModel.fromJson(dynamic json) {
    id = json['id'];
    clientName = json['clientName'];
    clientCarNumber = json['clientCarNumber'];
    clientIdentificationNumber = json['clientIdentificationNumber'];
    licenceNumber = json['licenceNumber'];
    clientPhone = json['clientPhone'];
    printText = json['print_text'];
    code = json['code'];
    costType = json['costType'];
    cost = json['cost'];
    notes = json['notes'];
    startsAt = json['starts_at'];
    endsAt = json['ends_at'];
    saies = json['saies'] != null ? SaiesModel.fromJson(json['saies']) : null;
    garage = json['garage'] != null ? GarageModel.fromJson(json['garage']) : null;
  }

  num? id;
  String? clientName;
  String? clientCarNumber;
  String? clientIdentificationNumber;
  String? licenceNumber;
  String? clientPhone;
  String? printText;
  dynamic code;
  dynamic costType;
  dynamic cost;
  dynamic notes;
  dynamic startsAt;
  dynamic endsAt;
  SaiesModel? saies;
  GarageModel? garage;
}

class SaiesModel {
  SaiesModel({
    this.id,
    this.name,
    this.phone,
    this.garage,
  });

  SaiesModel.fromJson(dynamic json) {
    id = json['id'];
    name = json['name'];
    phone = json['phone'];
    garage = json['garage'] != null ? GarageModel.fromJson(json['garage']) : null;
  }

  num? id;
  String? name;
  String? phone;
  GarageModel? garage;
}

class GarageModel {
  GarageModel({
    this.id,
    this.nameAr,
    this.nameEn,
    this.countryId,
    this.governorateId,
    this.cityId,
    this.areaId,
  });

  GarageModel.fromJson(dynamic json) {
    id = json['id'];
    nameAr = json['nameAr'];
    nameEn = json['nameEn'];
    countryId = json['country_id'];
    governorateId = json['governorate_id'];
    cityId = json['city_id'];
    areaId = json['area_id'];
  }

  num? id;
  String? nameAr;
  String? nameEn;
  num? countryId;
  num? governorateId;
  num? cityId;
  num? areaId;
}
