import SwiftUI
import WebKit
import Shared

/// Observable wrapper around the shared FaqDetailViewModel.
final class FaqDetailStore: ObservableObject {
    let vm: FaqDetailViewModel

    @Published var faq: UiStateWrapper<FaqDetail> = .loading
    @Published var comments: UiStateWrapper<[Comment]> = .loading

    private var jobs: [Kotlinx_coroutines_coreJob] = []

    init(categoryId: Int32, faqId: Int32) {
        vm = FaqDetailViewModel(aim: KoinHelper.activeInstanceManager, scope: defaultScope())
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.faq) { [weak self] value in
            DispatchQueue.main.async {
                self?.faq = unwrapUiState(value) { $0 as? FaqDetail }
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.comments) { [weak self] value in
            DispatchQueue.main.async {
                self?.comments = unwrapUiState(value) { castList($0) as [Comment]? }
            }
        })
        vm.load(categoryId: categoryId, faqId: faqId)
    }

    deinit {
        jobs.forEach { $0.cancel(cause: nil) }
    }
}

struct FaqDetailScreen: View {
    let categoryId: Int32
    let faqId: Int32
    let onPaywall: () -> Void

    @StateObject private var store: FaqDetailStore
    @State private var showComments = false

    init(categoryId: Int32, faqId: Int32, onPaywall: @escaping () -> Void) {
        self.categoryId = categoryId
        self.faqId = faqId
        self.onPaywall = onPaywall
        _store = StateObject(wrappedValue: FaqDetailStore(categoryId: categoryId, faqId: faqId))
    }

    var body: some View {
        Group {
            switch store.faq {
            case .loading:
                LoadingView()
            case .error(let message):
                ErrorRetryView(message: message, onRetry: {
                    store.vm.load(categoryId: categoryId, faqId: faqId)
                })
            case .success(let faq):
                detailContent(faq)
            }
        }
        .navigationTitle("FAQ")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if case .success(let faq) = store.faq {
                ToolbarItem(placement: .primaryAction) {
                    ShareLink(item: faq.question) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
        }
    }

    @State private var htmlContentHeight: CGFloat = 200

    private func detailContent(_ faq: FaqDetail) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Question
                Text(faq.question.strippingHTMLEntities())
                    .font(.title2)
                    .fontWeight(.semibold)

                // Author & date
                let meta = [faq.author, faq.updated].compactMap { $0 }.filter { !$0.isEmpty }
                if !meta.isEmpty {
                    Text(meta.joined(separator: " | "))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // HTML answer (auto-sizing)
                if !faq.answer.isEmpty {
                    HTMLView(html: faq.answer, contentHeight: $htmlContentHeight)
                        .frame(height: htmlContentHeight)
                }

                // Tags
                if !faq.tags.isEmpty {
                    tagsView(faq.tags)
                }

                // Rate button
                Button {
                    onPaywall()
                } label: {
                    Label("Rate this FAQ", systemImage: "star")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Divider()

                // Comments
                commentsSection
            }
            .padding()
        }
    }

    private func tagsView(_ tags: [String]) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(tags, id: \.self) { tag in
                Text(tag)
                    .font(.caption)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(.secondary.opacity(0.15))
                    .clipShape(Capsule())
            }
        }
    }

    @ViewBuilder
    private var commentsSection: some View {
        switch store.comments {
        case .loading:
            Text("Loading comments...")
                .font(.caption)
                .foregroundStyle(.secondary)
        case .error:
            Text("Could not load comments")
                .font(.caption)
                .foregroundStyle(.red)
        case .success(let comments):
            if comments.isEmpty {
                Text("No comments yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                Button {
                    withAnimation { showComments.toggle() }
                } label: {
                    Text(showComments ? "Hide comments (\(comments.count))" : "Show comments (\(comments.count))")
                        .font(.subheadline)
                }

                if showComments {
                    ForEach(comments, id: \.id) { comment in
                        CommentCard(comment: comment)
                    }
                }
            }
        }
    }
}

// MARK: - Comment card

private struct CommentCard: View {
    let comment: Comment

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(comment.username)
                    .font(.caption)
                    .fontWeight(.medium)
                Spacer()
                if let date = comment.date, !date.isEmpty {
                    Text(date)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            Text(comment.comment)
                .font(.caption)
        }
        .padding(12)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

// MARK: - HTML WebView (auto-sizing)

private struct HTMLView: UIViewRepresentable {
    let html: String
    @Binding var contentHeight: CGFloat

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.isScrollEnabled = false
        webView.navigationDelegate = context.coordinator
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        let wrapped = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body {
            font-family: -apple-system, sans-serif;
            font-size: 16px;
            padding: 0; margin: 0;
            word-wrap: break-word;
            color-scheme: light dark;
          }
          @media (prefers-color-scheme: dark) {
            body { color: #fff; }
            a { color: #64b5f6; }
          }
          @media (prefers-color-scheme: light) {
            body { color: #000; }
            a { color: #1a73e8; }
          }
          img { max-width: 100%; height: auto; }
          pre, code { overflow-x: auto; }
        </style>
        </head>
        <body>\(html)</body>
        </html>
        """
        webView.loadHTMLString(wrapped, baseURL: nil)
    }

    class Coordinator: NSObject, WKNavigationDelegate {
        let parent: HTMLView

        init(_ parent: HTMLView) {
            self.parent = parent
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            webView.evaluateJavaScript("document.body.scrollHeight") { result, _ in
                if let height = result as? CGFloat, height > 0 {
                    DispatchQueue.main.async {
                        self.parent.contentHeight = height + 16
                    }
                }
            }
        }
    }
}

// MARK: - FlowLayout (simple tag layout for iOS 16+)

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrangeSubviews(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                                  proposal: .unspecified)
        }
    }

    private struct LayoutResult {
        var positions: [CGPoint]
        var size: CGSize
    }

    private func arrangeSubviews(proposal: ProposedViewSize, subviews: Subviews) -> LayoutResult {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0
        var maxX: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth, currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }
            positions.append(CGPoint(x: currentX, y: currentY))
            lineHeight = max(lineHeight, size.height)
            currentX += size.width + spacing
            maxX = max(maxX, currentX)
        }

        return LayoutResult(
            positions: positions,
            size: CGSize(width: maxX, height: currentY + lineHeight)
        )
    }
}
