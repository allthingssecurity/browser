import Cocoa
import WebKit

class AppDelegate: NSObject, NSApplicationDelegate, NSTextFieldDelegate, WKNavigationDelegate {
    var window: NSWindow!
    var webView: WKWebView!
    var addressField: NSTextField!

    func applicationDidFinishLaunching(_ notification: Notification) {
        let screenSize = NSScreen.main?.frame.size ?? NSSize(width: 1280, height: 800)
        let width = min(1280, screenSize.width * 0.8)
        let height = min(800, screenSize.height * 0.8)
        let rect = NSRect(x: 0, y: 0, width: width, height: height)

        window = NSWindow(
            contentRect: rect,
            styleMask: [.titled, .closable, .resizable, .miniaturizable],
            backing: .buffered,
            defer: false
        )
        window.center()
        window.title = "NativeMacBrowser"
        window.makeKeyAndOrderFront(nil)

        let contentView = NSView(frame: rect)
        contentView.autoresizingMask = [.width, .height]

        addressField = NSTextField(frame: NSRect(x: 10, y: rect.height - 36, width: rect.width - 20, height: 24))
        addressField.autoresizingMask = [.width, .minYMargin]
        addressField.delegate = self
        addressField.placeholderString = "Enter URL or search…"
        addressField.target = self
        addressField.action = #selector(addressEntered)

        let config = WKWebViewConfiguration()
        webView = WKWebView(frame: NSRect(x: 0, y: 0, width: rect.width, height: rect.height - 44), configuration: config)
        webView.autoresizingMask = [.width, .height]
        webView.navigationDelegate = self

        contentView.addSubview(addressField)
        contentView.addSubview(webView)
        window.contentView = contentView

        load(urlString: "https://example.org")
    }

    @objc func addressEntered() {
        load(urlString: addressField.stringValue)
    }

    func control(_ control: NSControl, textView: NSTextView, doCommandBy commandSelector: Selector) -> Bool {
        if commandSelector == #selector(NSResponder.insertNewline(_:)) {
            addressEntered()
            return true
        }
        return false
    }

    func load(urlString: String) {
        var str = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !str.isEmpty else { return }

        if !str.contains("://") && !str.hasPrefix("about:") {
            if str.contains(".") || str.contains(":") {
                str = "https://" + str
            } else {
                let encoded = str.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? str
                str = "https://duckduckgo.com/?q=\(encoded)"
            }
        }

        if let url = URL(string: str) {
            webView.load(URLRequest(url: url))
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        window.title = webView.title ?? "NativeMacBrowser"
        if let u = webView.url?.absoluteString { addressField.stringValue = u }
    }
}
