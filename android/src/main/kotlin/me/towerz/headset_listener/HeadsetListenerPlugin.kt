package me.towerz.headset_listener

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

/** HeadsetListenerPlugin */
public class HeadsetListenerPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var eventChannel : EventChannel

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, methodChannelName)
    channel.setMethodCallHandler(this)
    eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, eventChannelName)
    eventChannel.setStreamHandler(eventStreamHandler)

    listenToHeadsetChanges(flutterPluginBinding.applicationContext)
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    private val LOG_TAG = HeadsetListenerPlugin::class.java.name

    private var eventChannelName = "me.towerz.headsetlistener/event"
    private var methodChannelName = "me.towerz.headsetlistener/method"

    private var eventSink: EventChannel.EventSink? = null

    private lateinit var audioManager: AudioManager

    val isHeadphoneConnected: Boolean
      get() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        Log.d(LOG_TAG, "isHeadphoneConnected - checking devices:")
        devices.forEach { Log.d(LOG_TAG, "productName=${it.productName}, type=${it.type}") }
        Log.d(LOG_TAG, "isHeadphoneConnected - finished")
        return devices.any {
          when (it.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> true
            else -> false
          }
        }
      }

    val isMicConnected: Boolean
      get() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        Log.d(LOG_TAG, "isMicConnected - checking devices:")
        devices.forEach { Log.d(LOG_TAG, "productName=${it.productName}, type=${it.type}") }
        Log.d(LOG_TAG, "isMicConnected - finished")
        return devices.any {
          when(it.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> true
            else -> false
          }
        }
      }

    private var eventStreamHandler: EventChannel.StreamHandler = object : EventChannel.StreamHandler {
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
      }

      override fun onCancel(arguments: Any?) {
        eventSink = null
      }
    }

    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "headset_listener")
      channel.setMethodCallHandler(HeadsetListenerPlugin())
      EventChannel(registrar.messenger(), eventChannelName)
              .setStreamHandler(eventStreamHandler)
      listenToHeadsetChanges(registrar.context().applicationContext)
    }

    @JvmStatic
    fun listenToHeadsetChanges(context: Context) {
      audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
      Log.d(LOG_TAG, "listenToHeadsetChanges")
      audioManager.registerAudioDeviceCallback(object: AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
          Log.d(LOG_TAG, "onAudioDevicesAdded:")
          addedDevices?.forEach { Log.d(LOG_TAG, "productName=${it.productName}, type=${it.type}") }
          onAudioDevicesUpdated()
          Log.d(LOG_TAG, "onAudioDevicesAdded - finished")
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
          Log.d(LOG_TAG, "onAudioDevicesRemoved:")
          removedDevices?.forEach { Log.d(LOG_TAG, "productName=${it.productName}, type=${it.type}") }
          onAudioDevicesUpdated()
          Log.d(LOG_TAG, "onAudioDevicesRemoved - finished")
        }

        private fun onAudioDevicesUpdated() {
          val eventSink = this@Companion.eventSink ?: return
          eventSink.success(mapOf(
                  "type" to "DeviceChanged",
                  "connected" to isHeadphoneConnected,
                  "mic" to isMicConnected
          ))
        }
      }, HandlerCompat.createAsync(context.mainLooper))
      Log.d(LOG_TAG, "listenToHeadsetChanges - added listener")
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "isHeadphoneConnected" -> result.success(isHeadphoneConnected)
      "isMicConnected" -> result.success(isMicConnected)
      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    eventChannel.setStreamHandler(null)
  }
}
