#import <Cocoa/Cocoa.h>
#import <WebKit/WebKit.h>

@interface AppDelegate : NSObject <NSApplicationDelegate, NSTextFieldDelegate, WKNavigationDelegate, WKUIDelegate, NSToolbarDelegate, NSTabViewDelegate, NSUserInterfaceValidations>
@property (strong) NSWindow *window;
@property (strong) NSTabView *tabView;
@property (strong) NSVisualEffectView *tabStrip;
@property (strong) NSStackView *tabStack;

// Toolbar controls
@property (strong) NSToolbar *toolbar;
@property (strong) NSTextField *addressField;
@property (strong) NSProgressIndicator *progressBar;

// Shared web context (critical for OAuth/popups/cookies)
@property (strong) WKProcessPool *processPool;
@property (strong) WKWebsiteDataStore *dataStore;

// Popup behavior
@property (assign) BOOL openPopupsInNewTabs; // default: NO (load in current tab for OAuth compatibility)
@property (assign) BOOL handoffPopupsToOpener; // if YES, after popup redirects off Google, load URL in opener and close popup

// Track popup -> opener tab index for OAuth handoff
@property (strong) NSMapTable<WKWebView *, NSNumber *> *popupOpenerIndex;

// Helpers
- (NSInteger)indexOfWebView:(WKWebView *)webView;

// Toolbar button refs (no names starting with 'new')
@property (strong) NSButton *backButton;
@property (strong) NSButton *forwardButton;
@property (strong) NSButton *reloadButton;
@property (strong) NSButton *homeButton;
@property (strong) NSButton *addTabButton;

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
// Tabs UI
- (void)rebuildTabStrip;
- (void)selectTab:(id)sender;
- (void)closeTabAtIndex:(NSInteger)index;

// Address helpers
- (void)goWithTLD:(NSString *)tld;
// Zoom
- (void)zoomIn:(id)sender;
- (void)zoomOut:(id)sender;
- (void)zoomReset:(id)sender;

// Bookmarks
- (void)addBookmark:(id)sender;
- (void)openBookmark:(id)sender;

@end
