#import "AppDelegate.h"

@implementation AppDelegate

- (void)applicationDidFinishLaunching:(NSNotification *)notification {
    NSScreen *main = [NSScreen mainScreen];
    CGSize screenSize = main ? main.frame.size : CGSizeMake(1280, 800);
    CGFloat width = MIN(1280, screenSize.width * 0.8);
    CGFloat height = MIN(800, screenSize.height * 0.8);
    NSRect rect = NSMakeRect(0, 0, width, height);

    self.window = [[NSWindow alloc] initWithContentRect:rect
                                              styleMask:(NSWindowStyleMaskTitled | NSWindowStyleMaskClosable | NSWindowStyleMaskResizable | NSWindowStyleMaskMiniaturizable)
                                                backing:NSBackingStoreBuffered
                                                  defer:NO];
    [self.window center];
    [self.window setTitle:@"NativeMacBrowser"];
    [self.window makeKeyAndOrderFront:nil];

    // Menu bar
    [self setupMenuBar];

    // Toolbar + tab view
    [self setupToolbarAndTabsWithFrame:rect];

    // Initial tab
    [self newTab:self];
}

- (void)addressEntered {
    [self load:self.addressField.stringValue];
}

- (BOOL)control:(NSControl *)control textView:(NSTextView *)textView doCommandBy:(SEL)commandSelector {
    if (commandSelector == @selector(insertNewline:)) {
        [self addressEntered];
        return YES;
    }
    return NO;
}

- (void)load:(NSString *)urlString {
    NSString *str = [urlString stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (str.length == 0) return;

    if ([str rangeOfString:@"://"].location == NSNotFound && ![str hasPrefix:@"about:"]) {
        if ([str rangeOfString:@"."].location != NSNotFound || [str rangeOfString:@":"].location != NSNotFound) {
            str = [@"https://" stringByAppendingString:str];
        } else {
            NSString *encoded = [str stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]] ?: str;
            str = [NSString stringWithFormat:@"https://duckduckgo.com/?q=%@", encoded];
        }
    }

    NSURL *url = [NSURL URLWithString:str];
    if (url) {
        WKWebView *wv = [self currentWebView];
        if (wv) [wv loadRequest:[NSURLRequest requestWithURL:url]];
    }
}

// Navigation delegate
- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    self.window.title = webView.title ?: @"NativeMacBrowser";
    if (webView.URL.absoluteString) self.addressField.stringValue = webView.URL.absoluteString;
}

#pragma mark - Menu, Toolbar, Tabs

- (void)setupMenuBar {
    NSMenu *menubar = [[NSMenu alloc] init];

    // App menu
    NSMenuItem *appMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:appMenuItem];

    NSMenu *appMenu = [[NSMenu alloc] initWithTitle:@"Application"];
    NSString *appName = @"NativeMacBrowser";
    [appMenu addItemWithTitle:[NSString stringWithFormat:@"About %@", appName] action:nil keyEquivalent:@""];
    [appMenu addItem:[NSMenuItem separatorItem]];
    [appMenu addItemWithTitle:@"Preferences…" action:nil keyEquivalent:@","];
    [appMenu addItem:[NSMenuItem separatorItem]];
    [appMenu addItemWithTitle:[NSString stringWithFormat:@"Quit %@", appName] action:@selector(terminate:) keyEquivalent:@"q"];
    [appMenuItem setSubmenu:appMenu];

    // File menu
    NSMenuItem *fileMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:fileMenuItem];
    NSMenu *fileMenu = [[NSMenu alloc] initWithTitle:@"File"];
    [fileMenu addItemWithTitle:@"New Tab" action:@selector(newTab:) keyEquivalent:@"t"];
    [fileMenu addItemWithTitle:@"Close Tab" action:@selector(closeTab:) keyEquivalent:@"w"];
    [fileMenuItem setSubmenu:fileMenu];

    // Edit menu
    NSMenuItem *editMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:editMenuItem];
    NSMenu *editMenu = [[NSMenu alloc] initWithTitle:@"Edit"];
    [editMenu addItemWithTitle:@"Copy" action:@selector(copy:) keyEquivalent:@"c"];
    [editMenu addItemWithTitle:@"Paste" action:@selector(paste:) keyEquivalent:@"v"];
    [editMenu addItemWithTitle:@"Cut" action:@selector(cut:) keyEquivalent:@"x"];
    [editMenuItem setSubmenu:editMenu];

    // View menu
    NSMenuItem *viewMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:viewMenuItem];
    NSMenu *viewMenu = [[NSMenu alloc] initWithTitle:@"View"];
    [viewMenu addItemWithTitle:@"Reload" action:@selector(reloadStop:) keyEquivalent:@"r"];
    [viewMenuItem setSubmenu:viewMenu];

    // History menu
    NSMenuItem *historyMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:historyMenuItem];
    NSMenu *historyMenu = [[NSMenu alloc] initWithTitle:@"History"];
    [historyMenu addItemWithTitle:@"Back" action:@selector(goBack:) keyEquivalent:@"["];
    [historyMenu addItemWithTitle:@"Forward" action:@selector(goForward:) keyEquivalent:@"]"];
    [historyMenuItem setSubmenu:historyMenu];

    // Bookmarks menu
    self.bookmarksMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:self.bookmarksMenuItem];
    NSMenu *bookmarksMenu = [[NSMenu alloc] initWithTitle:@"Bookmarks"];
    [bookmarksMenu addItemWithTitle:@"Add Bookmark" action:@selector(addBookmark:) keyEquivalent:@"d"];
    [bookmarksMenu addItem:[NSMenuItem separatorItem]];
    // dynamic bookmarks will be appended after separator
    [self.bookmarksMenuItem setSubmenu:bookmarksMenu];

    [NSApp setMainMenu:menubar];
}

- (void)setupToolbarAndTabsWithFrame:(NSRect)rect {
    // Create tab view
    self.tabView = [[NSTabView alloc] initWithFrame:NSMakeRect(0, 0, rect.size.width, rect.size.height - 44)];
    self.tabView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    self.tabView.delegate = (id<NSTabViewDelegate>)self;

    // Address field
    self.addressField = [[NSTextField alloc] initWithFrame:NSMakeRect(0, 0, 400, 24)];
    self.addressField.placeholderString = @"Enter URL or search…";
    self.addressField.target = self;
    self.addressField.action = @selector(addressEntered);
    self.addressField.delegate = (id<NSTextFieldDelegate>)self;

    // Toolbar
    self.toolbar = [[NSToolbar alloc] initWithIdentifier:@"BrowserToolbar"];
    self.toolbar.delegate = (id<NSToolbarDelegate>)self;
    self.toolbar.displayMode = NSToolbarDisplayModeIconOnly;
    self.toolbar.sizeMode = NSToolbarSizeModeDefault;
    [self.window setToolbar:self.toolbar];

    // Content view
    NSView *contentView = [[NSView alloc] initWithFrame:rect];
    contentView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    [contentView addSubview:self.tabView];
    [self.window setContentView:contentView];
}

- (WKWebView *)currentWebView {
    NSTabViewItem *item = self.tabView.selectedTabViewItem;
    if (!item) return nil;
    return (WKWebView *)item.view;
}

- (void)newTab:(id)sender {
    WKWebViewConfiguration *config = [[WKWebViewConfiguration alloc] init];
    WKWebView *wv = [[WKWebView alloc] initWithFrame:self.tabView.bounds configuration:config];
    wv.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    wv.navigationDelegate = (id<WKNavigationDelegate>)self;

    NSTabViewItem *item = [[NSTabViewItem alloc] initWithIdentifier:nil];
    item.label = @"New Tab";
    item.view = wv;
    [self.tabView addTabViewItem:item];
    [self.tabView selectTabViewItem:item];

    [self load:@"https://example.org"];
}

- (void)closeTab:(id)sender {
    if (self.tabView.numberOfTabViewItems <= 1) return;
    NSTabViewItem *item = self.tabView.selectedTabViewItem;
    if (item) [self.tabView removeTabViewItem:item];
}

- (void)goBack:(id)sender {
    WKWebView *wv = [self currentWebView];
    if ([wv canGoBack]) [wv goBack];
}

#pragma mark - Bookmarks

- (void)addBookmark:(id)sender {
    WKWebView *wv = [self currentWebView];
    NSString *title = wv.title ?: @"Untitled";
    NSString *url = wv.URL.absoluteString ?: @"";
    if (!self.bookmarks) self.bookmarks = [NSMutableArray array];
    NSDictionary *bm = @{ @"title": title, @"url": url };
    [self.bookmarks addObject:bm];

    NSMenu *menu = self.bookmarksMenuItem.submenu;
    NSMenuItem *item = [[NSMenuItem alloc] initWithTitle:title action:@selector(openBookmark:) keyEquivalent:@""];
    item.representedObject = url;
    [menu addItem:item];
}

- (void)openBookmark:(id)sender {
    NSString *url = [(NSMenuItem *)sender representedObject];
    if (url.length > 0) {
        [self load:url];
    }
}

- (void)goForward:(id)sender {
    WKWebView *wv = [self currentWebView];
    if ([wv canGoForward]) [wv goForward];
}

- (void)reloadStop:(id)sender {
    WKWebView *wv = [self currentWebView];
    if (wv.isLoading) { [wv stopLoading]; } else { [wv reload]; }
}

- (void)goHome:(id)sender {
    [self load:@"https://example.org"];
}

- (void)openLocation:(id)sender {
    [self.window makeFirstResponder:self.addressField];
}

// Toolbar delegate
- (NSToolbarItem *)toolbar:(NSToolbar *)toolbar itemForItemIdentifier:(NSToolbarItemIdentifier)itemIdentifier willBeInsertedIntoToolbar:(BOOL)flag {
    if ([itemIdentifier isEqualToString:@"Back"]) {
        NSButton *b = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameGoLeftTemplate] target:self action:@selector(goBack:)];
        NSToolbarItem *ti = [[NSToolbarItem alloc] initWithItemIdentifier:itemIdentifier];
        ti.view = b; return ti;
    } else if ([itemIdentifier isEqualToString:@"Forward"]) {
        NSButton *b = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameGoRightTemplate] target:self action:@selector(goForward:)];
        NSToolbarItem *ti = [[NSToolbarItem alloc] initWithItemIdentifier:itemIdentifier];
        ti.view = b; return ti;
    } else if ([itemIdentifier isEqualToString:@"Reload"]) {
        NSButton *b = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameRefreshTemplate] target:self action:@selector(reloadStop:)];
        NSToolbarItem *ti = [[NSToolbarItem alloc] initWithItemIdentifier:itemIdentifier];
        ti.view = b; return ti;
    } else if ([itemIdentifier isEqualToString:@"Home"]) {
        NSButton *b = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameHomeTemplate] target:self action:@selector(goHome:)];
        NSToolbarItem *ti = [[NSToolbarItem alloc] initWithItemIdentifier:itemIdentifier];
        ti.view = b; return ti;
    } else if ([itemIdentifier isEqualToString:@"Address"]) {
        NSToolbarItem *ti = [[NSToolbarItem alloc] initWithItemIdentifier:itemIdentifier];
        ti.view = self.addressField; return ti;
    } else if ([itemIdentifier isEqualToString:@"NewTab"]) {
        NSButton *b = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameAddTemplate] target:self action:@selector(newTab:)];
        NSToolbarItem *ti = [[NSToolbarItem alloc] initWithItemIdentifier:itemIdentifier];
        ti.view = b; return ti;
    }
    return nil;
}

- (NSArray<NSToolbarItemIdentifier> *)toolbarDefaultItemIdentifiers:(NSToolbar *)toolbar {
    return @[ @"Back", @"Forward", @"Reload", @"Home", @"Address", @"NewTab" ];
}

- (NSArray<NSToolbarItemIdentifier> *)toolbarAllowedItemIdentifiers:(NSToolbar *)toolbar {
    return [self toolbarDefaultItemIdentifiers:toolbar];
}

@end
