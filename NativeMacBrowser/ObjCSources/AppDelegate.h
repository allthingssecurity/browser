#import <Cocoa/Cocoa.h>
#import <WebKit/WebKit.h>

@interface AppDelegate : NSObject <NSApplicationDelegate, NSTextFieldDelegate, WKNavigationDelegate, WKUIDelegate, NSToolbarDelegate, NSTabViewDelegate, NSUserInterfaceValidations>
@property (strong) NSWindow *window;
@property (strong) NSTabView *tabView;

// Toolbar controls
@property (strong) NSToolbar *toolbar;
@property (strong) NSTextField *addressField;

// Bookmarks
@property (strong) NSMutableArray<NSDictionary *> *bookmarks; // { title, url }
@property (strong) NSMenuItem *bookmarksMenuItem;

// Actions
- (void)goBack:(id)sender;
- (void)goForward:(id)sender;
- (void)reloadStop:(id)sender;
- (void)goHome:(id)sender;
- (void)newTab:(id)sender;
- (void)closeTab:(id)sender;
- (void)openLocation:(id)sender;

// Bookmarks
- (void)addBookmark:(id)sender;
- (void)openBookmark:(id)sender;

@end
