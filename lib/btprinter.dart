import 'dart:async';

import 'package:flutter/services.dart';

class Btprinter {
  static const MethodChannel _channel =
      const MethodChannel('btprinter');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
