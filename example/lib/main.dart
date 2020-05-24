import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:btprinter/btprinter.dart';
import 'package:flutter_scan_bluetooth/flutter_scan_bluetooth.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _result = 'failed';
  String _resultPrint = 'failed';
  String _data = '';
  bool _scanning = false;
  FlutterScanBluetooth _bluetooth = FlutterScanBluetooth();

  @override
  void initState() {
    super.initState();
    initPlatformState();
    connectDevice();
    _bluetooth.devices.listen((device) {
      setState(() {
        _data += device.name+' (${device.address})\n';
      });
    });
    _bluetooth.scanStopped.listen((device) {
      setState(() {
        _scanning = false;
        _data += 'scan stopped\n';
      });
    });
  }

  Future<void> connectDevice() async {
    String result;

    try {
      result = await Btprinter.connectDevices("66:22:3E:0E:24:03");
    } on PlatformException {
      result = "Failed";
    }

    setState(() {
      _result = result;
    });
  }
  Future<void> printData() async {
    String resultPrint;

    try {
      resultPrint = await Btprinter.printString("ทดสอบการพิมพ์ BT app");
    } on PlatformException {
      resultPrint = "Failed";
    }

    setState(() {
      _resultPrint = resultPrint;
    });
  }
  Future<void> printBarcode(String s) async {
    String resultPrint;

    try {
      resultPrint = await Btprinter.printBarcode("12345678");
    } on PlatformException {
      resultPrint = "Failed";
    }

    setState(() {
      _resultPrint = resultPrint;
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await Btprinter.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Plugin example app ' + _result),
        ),
        body: Column(
          children: <Widget>[
            Center(
              child: Text(
                'Running on: $_platformVersion\n',
                style: TextStyle(fontSize: 20),
              ),
            ),
            Text('Connect $_result'),
            RaisedButton(
              onPressed: () {
                printData();
              },
              child: Text('print string'),
            ),
            RaisedButton(
              onPressed: () {
                printBarcode('12345678');
              },
              child: Text('print barcode'),
            )
          ],
        ),
      ),
    );
  }
}
