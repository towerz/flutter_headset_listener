import 'package:flutter/material.dart';
import 'package:headset_listener/headset_listener.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String get _stateDescription {
    switch (_headsetState) {
      case HeadsetState.CONNECTED:
        return 'Connected (mic: ${_hasMicrophone ?? false})';
      case HeadsetState.DISCONNECTED:
        return 'Disconnected';
    }
    return 'Unknown';
  }

  HeadsetState _headsetState;
  bool _hasMicrophone;

  final _headsetListener = HeadsetListener();

  @override
  void initState() {
    super.initState();
    _headsetListener.events.listen(_updateHeadsetState);
    _getInitialState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Headset state is: $_stateDescription\n'),
        ),
      ),
    );
  }

  void _getInitialState() async {
    final headsetState = await _headsetListener.headsetState;
    final hasMicrophone = await _headsetListener.isMicConnected;
    setState(() {
      _headsetState = headsetState;
      _hasMicrophone = hasMicrophone;
    });
  }

  void _updateHeadsetState(HeadsetEvent event) {
    setState(() {
      _headsetState = event.state;
      _hasMicrophone = event.hasMicrophone;
    });
  }
}
