import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:btprinter/btprinter.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _result = 'failed';
  Stream<int> _exampleStream;
  List<int> _listPrinterId = [];
  String _resultPrint = 'failed';
  String _data = '';
  bool _scanning = false;

  @override
  void initState() {
    super.initState();
    connectDevice('66:22:3E:0E:24:03');
    _exampleStream = Btprinter.getBtPrinterStream_returnStream('functionA')
      ..listen((result) {
        print('result from test-returnStream=$result');
        _listPrinterId.add(result);
      });
  }

  Future<void> connectDevice(String _address) async {
    String result;

    try {
      result = await Btprinter.connectDevices(_address);
    } on PlatformException {
      result = "Failed";
    }

    setState(() {
      _result = result;
    });
  }

  Future<void> printData(String _data) async {
    String resultPrint;

    try {
      resultPrint = await Btprinter.printString(_data);
    } on PlatformException {
      resultPrint = "Failed";
    }

    setState(() {
      _resultPrint = resultPrint;
    });
  }

  Future<void> printBarcode(String _data) async {
    String resultPrint;

    try {
      resultPrint = await Btprinter.printBarcode(_data);
    } on PlatformException {
      resultPrint = "Failed";
    }

    setState(() {
      _resultPrint = resultPrint;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Bluetooth Fujun'),
        ),
        body: Column(
          children: <Widget>[
            Text('Connect $_result'),
            RaisedButton(
              onPressed: () {
                printData('ทดสอบการพิมพ์ BT app');
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
