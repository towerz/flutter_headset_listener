#import "HeadsetListenerPlugin.h"
#if __has_include(<headset_listener/headset_listener-Swift.h>)
#import <headset_listener/headset_listener-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "headset_listener-Swift.h"
#endif

@implementation HeadsetListenerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftHeadsetListenerPlugin registerWithRegistrar:registrar];
}
@end
