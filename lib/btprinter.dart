import 'dart:async';

import 'package:flutter/services.dart';

class Btprinter {
  static const MethodChannel _channel =
      const MethodChannel('btprinter');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<List<String>> discoveryPariedDevices() async {
    final List<dynamic> BTList = await _channel.invokeMethod('discoveryPariedDevices');
    final List<String> BTStringList = List<String>.from(BTList);
    return BTStringList;
  }

  static Future<String> discoveryNewDevices() async {
    final String result = await _channel.invokeMethod('discoveryNewDevices');
    return result;
  }

  static Future<String> connectDevices(String address) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent("address", () => address);
    final String connectResult = await _channel.invokeMethod('connectDevices', args);
    return connectResult;
  }

  static Future<String> printString(String msg) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent("msg", () => msg);
    final String printResult = await _channel.invokeMethod('printString', args);
    return printResult;
  }

  static Future<String> printBarcode(String barcodeString) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent("barcodeString", () => barcodeString);
    final String printResult = await _channel.invokeMethod('printBarcode', args);
    return printResult;
  }

}
