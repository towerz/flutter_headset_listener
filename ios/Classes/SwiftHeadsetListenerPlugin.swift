import AVFoundation
import Flutter
import UIKit

class HeadsetEventStreamHandler: NSObject, FlutterStreamHandler {
    var eventSink: FlutterEventSink? = nil
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        eventSink = events
        return nil
    }

    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        eventSink = nil
        return nil
    }
}

public class SwiftHeadsetListenerPlugin: NSObject, FlutterPlugin {
    static let eventStreamHandler = HeadsetEventStreamHandler()

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "me.towerz.headsetlistener/method", binaryMessenger: registrar.messenger())
        let instance = SwiftHeadsetListenerPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        FlutterEventChannel(name: "me.towerz.headsetlistener/event", binaryMessenger: registrar.messenger()).setStreamHandler(eventStreamHandler)
        setupNotifications()
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "isHeadphoneConnected":
            result(SwiftHeadsetListenerPlugin.hasHeadphones())
        case "isMicConnected":
            result(SwiftHeadsetListenerPlugin.hasMicrophone())
        default:
            result(FlutterMethodNotImplemented)
        }

    }

    static func setupNotifications() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(handleRouteChange),
                                               name: AVAudioSession.routeChangeNotification,
                                               object: nil)
    }

    @objc static func handleRouteChange(notification: Notification) {
        guard let userInfo = notification.userInfo,
            let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
            let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else {
                return
        }

        switch reason {
        case .newDeviceAvailable: // New device found.
            onDeviceChange()
        case .oldDeviceUnavailable: // Old device removed.
            onDeviceChange()
        default:
            break
        }
    }

    static func onDeviceChange() {
        guard let eventSink = eventStreamHandler.eventSink else { return }

        let headphonesConnected = hasHeadphones()
        let micConnected = hasMicrophone()

        let event: [String: Any] = [
            "type": "DeviceChanged",
            "connected": headphonesConnected,
            "mic": micConnected
        ]

        eventSink(event)
    }

    static func hasHeadphones() -> Bool {
        let session = AVAudioSession.sharedInstance()
        return session.currentRoute.outputs.filter({ $0.portType == .headphones || $0.portType == .bluetoothA2DP }).count > 0
    }

    static func hasMicrophone() -> Bool {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(AVAudioSession.Category.playAndRecord, options: [.mixWithOthers, .allowBluetooth])
        guard session.isInputAvailable, let inputs = session.availableInputs else { return false }
        return inputs.filter({ $0.portType == .headsetMic || $0.portType == .bluetoothHFP }).count > 0
    }
}
