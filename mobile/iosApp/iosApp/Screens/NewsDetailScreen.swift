import SwiftUI

struct NewsDetailScreen: View {
    let item: HomeNewsItem
    let fullContent: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(item.header)
                    .font(.title2)
                    .fontWeight(.semibold)

                if let date = item.date, !date.isEmpty {
                    Text(date)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if let html = fullContent, !html.isEmpty {
                    Divider()
                    NewsHTMLView(html: html)
                }
            }
            .padding()
        }
        .navigationTitle("News")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Lightweight HTML view for news body

private struct NewsHTMLView: View {
    let html: String
    @State private var contentHeight: CGFloat = 200

    var body: some View {
        NewsWebView(html: html, contentHeight: $contentHeight)
            .frame(height: contentHeight)
    }
}

import WebKit

private struct NewsWebView: UIViewRepresentable {
    let html: String
    @Binding var contentHeight: CGFloat

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> WKWebView {
        let wv = WKWebView(frame: .zero)
        wv.isOpaque = false
        wv.backgroundColor = .clear
        wv.scrollView.isScrollEnabled = false
        wv.navigationDelegate = context.coordinator
        return wv
    }

    func updateUIView(_ wv: WKWebView, context: Context) {
        let wrapped = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { font-family: -apple-system, sans-serif; font-size: 16px;
                 padding: 0; margin: 0; word-wrap: break-word; color-scheme: light dark; }
          @media (prefers-color-scheme: dark)  { body { color: #fff; } a { color: #64b5f6; } }
          @media (prefers-color-scheme: light) { body { color: #000; } a { color: #1a73e8; } }
          img { max-width: 100%; height: auto; }
        </style>
        </head><body>\(html)</body></html>
        """
        wv.loadHTMLString(wrapped, baseURL: nil)
    }

    class Coordinator: NSObject, WKNavigationDelegate {
        let parent: NewsWebView
        init(_ parent: NewsWebView) { self.parent = parent }
        func webView(_ wv: WKWebView, didFinish _: WKNavigation!) {
            wv.evaluateJavaScript("document.body.scrollHeight") { result, _ in
                if let h = result as? CGFloat, h > 0 {
                    DispatchQueue.main.async { self.parent.contentHeight = h + 16 }
                }
            }
        }
    }
}
