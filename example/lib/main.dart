import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:btprinter/btprinter.dart';

void main() {
  runApp(TestApp());
}

class TestApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Welcome to Flutter',
      debugShowCheckedModeBanner: false,
      home: MyApp(),
    );
  }
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

  void _onLoading() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return Dialog(
          child: Container(
            padding: EdgeInsets.all(10.0),
            child: new Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                new CircularProgressIndicator(),
                Text('กำลังส่งเชื่อมต่อ')
              ],
            ),
          ),
        );
      },
    );
  }

  void connect() {
    setDevice('66:22:3E:0E:24:03');
    _connectListener = Btprinter.getBtPrinterStream_returnStream('functionA')
      ..listen((result) {
        print('result from test-returnStream=$result');
        if (result == 0) {
          printData('เชื่อมต่อสำเร็จ');
          printBarcode('2800099058214');
          Btprinter.fujunClosePort();
        } else if (result == 7) {
          print('try again');
        }
      });
  }

  void connectZenpert() {
    setDevice('DC:0D:30:F5:73:8A');
    _connectListener =
        Btprinter.getZenpertBtPrinterStream_returnStream('functionA')
          ..listen((result) {
            print('result from test-returnStream=$result');
            if (result == 0) {
              printZenpertText('เชื่อมต่อสำเร็จ');
              Btprinter.zenpertClosePort();
            } else if (result == 7) {
              print('try again');
            }
          });
  }

  Future<void> printData(String _data) async {
    String result;
    _data = _data + '\n';
    try {
      result = await Btprinter.printString(_data);
    } on PlatformException {}
  }

  Future<void> printBarcode(String _data) async {
    String result;
    try {
      result = await Btprinter.printBarcode(_data);
    } on PlatformException {}
  }

  Future<void> printQrCode(String _data) async {
    String result;
    try {
      result = await Btprinter.printQrCode(_data);
    } on PlatformException {}
  }

  Future<void> printZenpertText(String _data) async {
    String result;
    try {
      result = await Btprinter.zenpertPrintString(_data);
    } on PlatformException {}
  }

  Future<void> printZenpertBarcode(String _data) async {
    String result;
    try {
      result = await Btprinter.zenpertPrintBarcode(_data);
    } on PlatformException {}
  }

  Future<void> printZenpertQrCode(String _data) async {
    String result;
    try {
      result = await Btprinter.zenpertPrintQrCode(_data);
    } on PlatformException {}
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
//            StreamBuilder(
//                stream: _connectListener,
//                builder: (context, snapshot) {
//                  return Text('${_resultConnect}');
//                }),
            Center(
              child: RaisedButton(
                onPressed: () {
                  connect();
                },
                child: Text('connect fujun'),
              ),
            ),
            Center(
              child: RaisedButton(
                onPressed: () {
                  connectZenpert();
                },
                child: Text('connect zenpert'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
