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
    // Open popups in new tabs and hand off back to opener after OAuth finishes
    self.openPopupsInNewTabs = YES;
    self.handoffPopupsToOpener = YES;
    [self newTab:self];
}

- (void)addressEntered {
    [self load:self.addressField.stringValue];
}

- (BOOL)control:(NSControl *)control textView:(NSTextView *)textView doCommandBy:(SEL)commandSelector {
    if (commandSelector == @selector(insertNewline:)) {
        NSEvent *ev = [NSApp currentEvent];
        NSEventModifierFlags flags = ev.modifierFlags;
        if (flags & NSEventModifierFlagCommand) {
            [self goWithTLD:@".com"]; // Cmd+Enter
        } else if (flags & NSEventModifierFlagShift) {
            [self goWithTLD:@".net"];  // Shift+Enter
        } else if (flags & NSEventModifierFlagOption) {
            [self goWithTLD:@".org"];  // Option+Enter
        } else {
            [self addressEntered];
        }
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
    // Update tab title when page loads
    NSInteger idx = [self.tabView indexOfTabViewItem:self.tabView.selectedTabViewItem];
    if (idx != NSNotFound) {
        NSTabViewItem *it = [self.tabView tabViewItemAtIndex:idx];
        if (it.view == webView) {
            it.label = webView.title ?: @"New Tab";
            [self rebuildTabStrip];
        }
    }
    // OAuth popup handoff: if this webView is a popup and it has navigated off Google, load URL in opener and close this tab
    NSNumber *openerIndex = [self.popupOpenerIndex objectForKey:webView];
    if (openerIndex && self.handoffPopupsToOpener) {
        NSURL *u = webView.URL;
        if (u && [u.scheme.lowercaseString hasPrefix:@"http"]) {
            NSString *host = u.host.lowercaseString ?: @"";
            BOOL onGoogle = ([host containsString:@"google."] || [host containsString:@"accounts.google.com"]);
            if (!onGoogle) {
                NSInteger oIdx = openerIndex.integerValue;
                if (oIdx >= 0 && oIdx < self.tabView.numberOfTabViewItems) {
                    WKWebView *opener = (WKWebView *)self.tabView.tabViewItems[oIdx].view;
                    if (opener) {
                        [opener loadRequest:[NSURLRequest requestWithURL:u]];
                        [self.tabView selectTabViewItemAtIndex:oIdx];
                    }
                }
                NSInteger pIdx = [self indexOfWebView:webView];
                if (pIdx != NSNotFound) { [self closeTabAtIndex:pIdx]; }
                [self.popupOpenerIndex removeObjectForKey:webView];
            }
        }
    }
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
    [viewMenu addItem:[NSMenuItem separatorItem]];
    [viewMenu addItemWithTitle:@"Zoom In" action:@selector(zoomIn:) keyEquivalent:@"="];
    [viewMenu addItemWithTitle:@"Zoom Out" action:@selector(zoomOut:) keyEquivalent:@"-"];
    [viewMenu addItemWithTitle:@"Actual Size" action:@selector(zoomReset:) keyEquivalent:@"0"];
    [viewMenuItem setSubmenu:viewMenu];

    // History menu
    NSMenuItem *historyMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:historyMenuItem];
    NSMenu *historyMenu = [[NSMenu alloc] initWithTitle:@"History"];
    [historyMenu addItemWithTitle:@"Back" action:@selector(goBack:) keyEquivalent:@"["];
    [historyMenu addItemWithTitle:@"Forward" action:@selector(goForward:) keyEquivalent:@"]"];
    [historyMenuItem setSubmenu:historyMenu];

    // Go to Location
    NSMenuItem *goMenuItem = [[NSMenuItem alloc] init];
    [menubar addItem:goMenuItem];
    NSMenu *goMenu = [[NSMenu alloc] initWithTitle:@"Go"];
    [goMenu addItemWithTitle:@"Location…" action:@selector(openLocation:) keyEquivalent:@"l"];
    [goMenuItem setSubmenu:goMenu];

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
    // Shared web context for all WKWebView instances (ensures cookies/session for OAuth popups)
    if (!self.processPool) self.processPool = [[WKProcessPool alloc] init];
    if (!self.dataStore) self.dataStore = [WKWebsiteDataStore defaultDataStore];
    // Create tab view (content area below a custom tab strip)
    CGFloat tabStripHeight = 34.0;
    self.tabView = [[NSTabView alloc] initWithFrame:NSMakeRect(0, 0, rect.size.width, rect.size.height - 44 - tabStripHeight)];
    self.tabView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    self.tabView.delegate = (id<NSTabViewDelegate>)self;
#ifdef NSNoTabsNoBorder
    self.tabView.tabViewType = NSNoTabsNoBorder;
#else
    self.tabView.tabViewType = NSNoTabsBezelBorder;
#endif

    // Address field
    self.addressField = [[NSTextField alloc] initWithFrame:NSMakeRect(0, 0, 400, 24)];
    self.addressField.placeholderString = @"Enter URL or search…";
    self.addressField.target = self;
    self.addressField.action = @selector(addressEntered);
    self.addressField.delegate = (id<NSTextFieldDelegate>)self;

    // Toolbar
    self.toolbar = [[NSToolbar alloc] initWithIdentifier:@"BrowserToolbar"];
    self.toolbar.delegate = (id<NSToolbarDelegate>)self;
    self.toolbar.displayMode = NSToolbarDisplayModeIconAndLabel;
    self.toolbar.sizeMode = NSToolbarSizeModeDefault;
    [self.window setToolbar:self.toolbar];
    if (@available(macOS 11.0, *)) {
        self.window.toolbarStyle = NSWindowToolbarStyleUnified;
        self.window.titleVisibility = NSWindowTitleHidden;
        self.window.titlebarAppearsTransparent = YES;
    }

    // Custom tab strip: Visual effect background with horizontal stack of tab buttons
    self.tabStrip = [[NSVisualEffectView alloc] initWithFrame:NSMakeRect(0, rect.size.height - 44 - tabStripHeight, rect.size.width, tabStripHeight)];
    self.tabStrip.autoresizingMask = NSViewWidthSizable | NSViewMinYMargin;
    if (@available(macOS 10.14, *)) {
        self.tabStrip.material = NSVisualEffectMaterialTitlebar;
    } else {
        self.tabStrip.material = NSVisualEffectMaterialUnderWindowBackground;
    }
    self.tabStrip.blendingMode = NSVisualEffectBlendingModeBehindWindow;
    self.tabStrip.state = NSVisualEffectStateFollowsWindowActiveState;

    self.tabStack = [[NSStackView alloc] initWithFrame:NSMakeRect(12, 4, rect.size.width - 24, tabStripHeight - 8)];
    self.tabStack.orientation = NSUserInterfaceLayoutOrientationHorizontal;
    self.tabStack.alignment = NSLayoutAttributeCenterY;
    self.tabStack.spacing = 8.0;
    self.tabStack.edgeInsets = NSEdgeInsetsMake(4, 8, 4, 8);
    self.tabStack.autoresizingMask = NSViewWidthSizable | NSViewMinYMargin;
    [self.tabStrip addSubview:self.tabStack];

    // Content view
    NSView *contentView = [[NSView alloc] initWithFrame:rect];
    contentView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    // Progress bar under toolbar
    self.progressBar = [[NSProgressIndicator alloc] initWithFrame:NSMakeRect(0, rect.size.height - 3 - 44, rect.size.width, 3)];
    self.progressBar.indeterminate = NO;
    self.progressBar.minValue = 0.0;
    self.progressBar.maxValue = 1.0;
    self.progressBar.doubleValue = 0.0;
    self.progressBar.autoresizingMask = NSViewWidthSizable | NSViewMinYMargin;
    self.progressBar.hidden = YES;

    [contentView addSubview:self.tabView];
    [contentView addSubview:self.tabStrip];
    [contentView addSubview:self.progressBar];
    [self.window setContentView:contentView];

    if (!self.popupOpenerIndex) {
        self.popupOpenerIndex = [NSMapTable weakToStrongObjectsMapTable];
    }
}

- (WKWebView *)currentWebView {
    NSTabViewItem *item = self.tabView.selectedTabViewItem;
    if (!item) return nil;
    return (WKWebView *)item.view;
}

- (void)newTab:(id)sender {
    WKWebViewConfiguration *config = [[WKWebViewConfiguration alloc] init];
    config.processPool = self.processPool;
    config.websiteDataStore = self.dataStore;
    if (!config.preferences) config.preferences = [[WKPreferences alloc] init];
    config.preferences.javaScriptCanOpenWindowsAutomatically = YES;
    WKWebView *wv = [[WKWebView alloc] initWithFrame:self.tabView.bounds configuration:config];
    wv.customUserAgent = @"Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
    wv.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    wv.navigationDelegate = (id<WKNavigationDelegate>)self;
    wv.UIDelegate = (id<WKUIDelegate>)self;
    [wv addObserver:self forKeyPath:@"estimatedProgress" options:NSKeyValueObservingOptionNew context:nil];

    NSTabViewItem *item = [[NSTabViewItem alloc] initWithIdentifier:nil];
    item.label = @"New Tab";
    item.view = wv;
    [self.tabView addTabViewItem:item];
    [self.tabView selectTabViewItem:item];

    [self load:@"https://example.org"];
    [self rebuildTabStrip];
}

- (void)closeTab:(id)sender {
    if (self.tabView.numberOfTabViewItems <= 1) return;
    NSTabViewItem *item = self.tabView.selectedTabViewItem;
    if (item) [self.tabView removeTabViewItem:item];
    [self rebuildTabStrip];
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

#pragma mark - Zoom

- (void)zoomIn:(id)sender { [self currentWebView].pageZoom += 0.1; }
- (void)zoomOut:(id)sender { [self currentWebView].pageZoom = MAX(0.1, [self currentWebView].pageZoom - 0.1); }
- (void)zoomReset:(id)sender { [self currentWebView].pageZoom = 1.0; }

#pragma mark - WKUIDelegate (new windows -> new tabs)

- (nullable WKWebView *)webView:(WKWebView *)webView createWebViewWithConfiguration:(WKWebViewConfiguration *)configuration forNavigationAction:(WKNavigationAction *)navigationAction windowFeatures:(WKWindowFeatures *)windowFeatures {
    // Prefer loading popups in the current tab for OAuth flows
    if (!self.openPopupsInNewTabs) {
        [[self currentWebView] loadRequest:navigationAction.request];
        return nil;
    }
    configuration.processPool = self.processPool;
    configuration.websiteDataStore = self.dataStore;
    if (!configuration.preferences) configuration.preferences = [[WKPreferences alloc] init];
    configuration.preferences.javaScriptCanOpenWindowsAutomatically = YES;
    WKWebView *popup = [[WKWebView alloc] initWithFrame:self.tabView.bounds configuration:configuration];
    popup.navigationDelegate = (id<WKNavigationDelegate>)self;
    popup.UIDelegate = (id<WKUIDelegate>)self;
    popup.customUserAgent = @"Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
    NSTabViewItem *item = [[NSTabViewItem alloc] initWithIdentifier:nil];
    item.label = @"New Tab";
    item.view = popup;
    [self.tabView addTabViewItem:item];
    [self.tabView selectTabViewItem:item];
    [self rebuildTabStrip];
    // Remember opener index so we can switch back after OAuth completes
    NSInteger openerIndex = [self indexOfWebView:(WKWebView *)webView];
    if (openerIndex != NSNotFound) {
        [self.popupOpenerIndex setObject:@(openerIndex) forKey:popup];
    }
    return popup;
}

#pragma mark - KVO for progress

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSKeyValueChangeKey,id> *)change context:(void *)context {
    if ([keyPath isEqualToString:@"estimatedProgress"]) {
        double p = [[change objectForKey:NSKeyValueChangeNewKey] doubleValue];
        self.progressBar.hidden = (p <= 0.0 || p >= 1.0);
        self.progressBar.doubleValue = p;
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

- (NSInteger)indexOfWebView:(WKWebView *)webView {
    for (NSInteger i = 0; i < self.tabView.numberOfTabViewItems; i++) {
        if (self.tabView.tabViewItems[i].view == webView) return i;
    }
    return NSNotFound;
}

#pragma mark - WKNavigationDelegate (policy)

- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {
    NSURL *url = navigationAction.request.URL;
    if (!url) { decisionHandler(WKNavigationActionPolicyAllow); return; }
    NSString *scheme = url.scheme.lowercaseString;
    // Allow common external schemes to open in default apps
    NSSet *external = [NSSet setWithArray:@[@"mailto", @"tel", @"sms", @"facetime", @"maps", @"itms-apps"]];
    if ([external containsObject:scheme]) {
        [[NSWorkspace sharedWorkspace] openURL:url];
        decisionHandler(WKNavigationActionPolicyCancel);
        return;
    }
    decisionHandler(WKNavigationActionPolicyAllow);
}

- (void)webView:(WKWebView *)webView didCommitNavigation:(WKNavigation *)navigation {
    // If this is a popup tab finishing OAuth, switch back to its opener when we hit a non-Google domain
    NSNumber *openerIndex = [self.popupOpenerIndex objectForKey:webView];
    if (!openerIndex) return;
    NSURL *u = webView.URL; if (!u) return;
    NSString *host = u.host.lowercaseString ?: @"";
    BOOL onGoogle = ([host containsString:@"google."] || [host containsString:@"accounts.google.com"]);
    if (!onGoogle) {
        NSInteger idx = openerIndex.integerValue;
        if (idx >= 0 && idx < self.tabView.numberOfTabViewItems) {
            [self.tabView selectTabViewItemAtIndex:idx];
            [self rebuildTabStrip];
        }
        [self.popupOpenerIndex removeObjectForKey:webView];
    }
}

#pragma mark - NSUserInterfaceValidations

- (BOOL)validateUserInterfaceItem:(id<NSValidatedUserInterfaceItem>)item {
    SEL action = [item action];
    WKWebView *wv = [self currentWebView];
    if (action == @selector(goBack:)) return [wv canGoBack];
    if (action == @selector(goForward:)) return [wv canGoForward];
    if (action == @selector(reloadStop:)) return YES;
    if (action == @selector(zoomIn:) || action == @selector(zoomOut:) || action == @selector(zoomReset:)) return YES;
    return YES;
}

#pragma mark - Custom Tab Strip

- (NSButton *)makeTabButtonWithTitle:(NSString *)title selected:(BOOL)selected index:(NSInteger)index {
    NSButton *btn = [NSButton buttonWithTitle:(title ?: @"New Tab") target:self action:@selector(selectTab:)];
    btn.tag = index;
    btn.bordered = NO;
    btn.font = [NSFont systemFontOfSize:12 weight:(selected ? NSFontWeightSemibold : NSFontWeightRegular)];
    btn.wantsLayer = YES;
    btn.layer.cornerRadius = 6.0;
    btn.contentTintColor = selected ? [NSColor labelColor] : [NSColor secondaryLabelColor];
    NSColor *bg = selected ? [NSColor controlBackgroundColor] : [NSColor clearColor];
    btn.layer.backgroundColor = bg.CGColor;
    btn.layer.borderWidth = selected ? 1.0 : 0.0;
    btn.layer.borderColor = [NSColor separatorColor].CGColor;
    [btn sizeToFit];
    NSRect f = btn.frame; f.size.width += 16; f.size.height = 24; btn.frame = f;
    return btn;
}

- (void)rebuildTabStrip {
    if (!self.tabStack) return;
    for (NSView *sub in [self.tabStack.arrangedSubviews copy]) { [self.tabStack removeView:sub]; [sub removeFromSuperview]; }
    NSInteger count = self.tabView.numberOfTabViewItems;
    NSInteger selIndex = [self.tabView indexOfTabViewItem:self.tabView.selectedTabViewItem];
    for (NSInteger i = 0; i < count; i++) {
        NSTabViewItem *it = [self.tabView tabViewItemAtIndex:i];
        NSStackView *pill = [[NSStackView alloc] initWithFrame:NSZeroRect];
        pill.orientation = NSUserInterfaceLayoutOrientationHorizontal;
        pill.spacing = 6.0;
        NSButton *labelBtn = [self makeTabButtonWithTitle:it.label selected:(i == selIndex) index:i];
        NSButton *closeBtn = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameStopProgressTemplate] target:self action:@selector(closeTabButtonPressed:)];
        closeBtn.bordered = NO; closeBtn.tag = i; closeBtn.contentTintColor = [NSColor tertiaryLabelColor];
        closeBtn.frame = NSMakeRect(0, 0, 16, 16);
        [pill addArrangedSubview:labelBtn];
        [pill addArrangedSubview:closeBtn];
        [self.tabStack addArrangedSubview:pill];
    }
    NSButton *plus = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameAddTemplate] target:self action:@selector(newTab:)];
    plus.bordered = NO; plus.wantsLayer = YES; plus.layer.cornerRadius = 6.0; plus.frame = NSMakeRect(0, 0, 24, 24);
    [self.tabStack addArrangedSubview:plus];
}

- (void)selectTab:(id)sender {
    NSInteger idx = [sender tag];
    if (idx >= 0 && idx < self.tabView.numberOfTabViewItems) {
        [self.tabView selectTabViewItemAtIndex:idx];
        [self rebuildTabStrip];
        WKWebView *wv = (WKWebView *)self.tabView.selectedTabViewItem.view;
        if (wv.URL.absoluteString) self.addressField.stringValue = wv.URL.absoluteString;
    }
}

- (void)closeTabButtonPressed:(id)sender { [self closeTabAtIndex:[sender tag]]; }

#pragma mark - Address helpers

- (void)goWithTLD:(NSString *)tld {
    NSString *raw = self.addressField.stringValue ?: @"";
    NSString *s = [raw stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (s.length == 0) return;
    if ([s containsString:@"://"] || [s containsString:@"."]) { [self addressEntered]; return; }
    NSString *host = [NSString stringWithFormat:@"www.%@%@", s, tld];
    NSString *urlStr = [NSString stringWithFormat:@"https://%@", host];
    [self load:urlStr];
}

- (void)closeTabAtIndex:(NSInteger)index {
    if (index < 0 || index >= self.tabView.numberOfTabViewItems) return;
    NSTabViewItem *it = [self.tabView tabViewItemAtIndex:index];
    [self.tabView removeTabViewItem:it];
    NSInteger newIndex = MIN(index, self.tabView.numberOfTabViewItems - 1);
    if (newIndex >= 0) [self.tabView selectTabViewItemAtIndex:newIndex];
    [self rebuildTabStrip];
}

@end
