import SwiftUI
import WebKit

struct MallWebView: UIViewRepresentable {
    let request: URLRequest
    let loadRevision: Int
    let hostMessage: MallHostMessage?
    let onPageLoaded: (URL?) -> Void
    let onPageLoadFailed: (URL?, String) -> Void
    let onBridgeMessage: (MallBridgeMessage) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            onPageLoaded: onPageLoaded,
            onPageLoadFailed: onPageLoadFailed,
            onBridgeMessage: onBridgeMessage
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.userContentController.add(context.coordinator, name: Coordinator.bridgeChannel)

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = false
        webView.load(request)
        context.coordinator.lastLoadedRequest = request
        context.coordinator.lastLoadRevision = loadRevision
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if context.coordinator.lastLoadRevision != loadRevision {
            webView.load(request)
            context.coordinator.lastLoadedRequest = request
            context.coordinator.lastLoadRevision = loadRevision
        }

        if let hostMessage {
            webView.evaluateJavaScript(hostMessage.script)
        }
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.configuration.userContentController.removeScriptMessageHandler(forName: Coordinator.bridgeChannel)
        webView.navigationDelegate = nil
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
        static let bridgeChannel = "mallBridge"

        let onPageLoaded: (URL?) -> Void
        let onPageLoadFailed: (URL?, String) -> Void
        let onBridgeMessage: (MallBridgeMessage) -> Void
        var lastLoadedRequest: URLRequest?
        var lastLoadRevision = 0

        init(
            onPageLoaded: @escaping (URL?) -> Void,
            onPageLoadFailed: @escaping (URL?, String) -> Void,
            onBridgeMessage: @escaping (MallBridgeMessage) -> Void
        ) {
            self.onPageLoaded = onPageLoaded
            self.onPageLoadFailed = onPageLoadFailed
            self.onBridgeMessage = onBridgeMessage
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            onPageLoaded(webView.url)
        }

        func webView(
            _ webView: WKWebView,
            didFail navigation: WKNavigation!,
            withError error: Error
        ) {
            onPageLoadFailed(webView.url, error.localizedDescription)
        }

        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation navigation: WKNavigation!,
            withError error: Error
        ) {
            onPageLoadFailed(webView.url ?? lastLoadedRequest?.url, error.localizedDescription)
        }

        func userContentController(
            _ userContentController: WKUserContentController,
            didReceive message: WKScriptMessage
        ) {
            guard message.name == Self.bridgeChannel,
                  let bridgeMessage = MallBridgeMessage(body: message.body) else {
                return
            }
            onBridgeMessage(bridgeMessage)
        }
    }
}
