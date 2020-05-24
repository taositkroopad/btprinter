#import "BtprinterPlugin.h"
#if __has_include(<btprinter/btprinter-Swift.h>)
#import <btprinter/btprinter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "btprinter-Swift.h"
#endif

@implementation BtprinterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBtprinterPlugin registerWithRegistrar:registrar];
}
@end
