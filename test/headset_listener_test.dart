import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:headset_listener/headset_listener.dart';

void main() {
  const MethodChannel channel = MethodChannel('headset_listener');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {});
}
