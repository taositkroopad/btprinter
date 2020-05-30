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
  String _resultConnect = 'failed';
  Stream<int> _connectListener;

  @override
  void initState() {
    super.initState();
  }

  Future<void> setDevice(String _address) async {
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

  void connect(){
    setDevice('66:22:3E:0E:24:03');
    _connectListener = Btprinter.getBtPrinterStream_returnStream('functionA')
      ..listen((result) {
        print('result from test-returnStream=$result');
        if(result == 0){
          printData('connected');
        } else if(result == 7){
          print('try again');
        }
      });
  }

  Future<void> printData(String _data) async {
    String result;
    _data = _data + '\n';
    try {
      result = await Btprinter.printString(_data);
    } on PlatformException {

    }
  }

  Future<void> printBarcode(String _data) async {
    String result;
    try {
      result = await Btprinter.printBarcode(_data);
    } on PlatformException {
    }
  }

  Future<void> printQrCode(String _data) async {
    String result;
    try {
      result = await Btprinter.printQrCode(_data);
    } on PlatformException {
    }
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
            StreamBuilder(
                stream: _connectListener,
                builder: (context, snapshot) {
                  return Text('${_resultConnect}');
                }),
            RaisedButton(
              onPressed: () {
                connect();
              },
              child: Text('connect'),
            ),
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
            ),
            RaisedButton(
              onPressed: () {
                printQrCode('111222');
              },
              child: Text('print qrcode'),
            ),
          ],
        ),
      ),
    );
  }
}
