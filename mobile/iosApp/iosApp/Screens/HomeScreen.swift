import SwiftUI
import Shared

/// Observable wrapper around the shared HomeViewModel.
final class HomeStore: ObservableObject {
    let vm: HomeViewModel

    @Published var sticky: UiStateWrapper<[FaqSummary]> = .loading
    @Published var popular: UiStateWrapper<[FaqSummary]> = .loading
    @Published var latest: UiStateWrapper<[FaqSummary]> = .loading
    @Published var news: UiStateWrapper<[NewsItem]> = .loading

    private var jobs: [Kotlinx_coroutines_coreJob] = []

    init() {
        vm = HomeViewModel(aim: KoinHelper.activeInstanceManager, scope: defaultScope())
        startObserving()
        vm.loadAll()
    }

    private func startObserving() {
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.sticky) { [weak self] value in
            DispatchQueue.main.async {
                self?.sticky = unwrapUiState(value) { castList($0) as [FaqSummary]? }
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.popular) { [weak self] value in
            DispatchQueue.main.async {
                self?.popular = unwrapUiState(value) { castList($0) as [FaqSummary]? }
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.latest) { [weak self] value in
            DispatchQueue.main.async {
                self?.latest = unwrapUiState(value) { castList($0) as [FaqSummary]? }
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.news) { [weak self] value in
            DispatchQueue.main.async {
                self?.news = unwrapUiState(value) { castList($0) as [NewsItem]? }
            }
        })
    }

    deinit {
        jobs.forEach { $0.cancel(cause: nil) }
    }
}

private enum HomeTab: String, CaseIterable {
    case sticky = "Sticky"
    case popular = "Popular"
    case latest = "Latest"
    case news = "News"
}

struct HomeScreen: View {
    @StateObject private var store = HomeStore()
    @State private var selectedTab: HomeTab = .sticky
    let onFaqClick: (Int32, Int32) -> Void

    var body: some View {
        VStack(spacing: 0) {
            picker
            tabContent
        }
        .navigationTitle("MyFAQ")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var picker: some View {
        Picker("", selection: $selectedTab) {
            ForEach(HomeTab.allCases, id: \.self) { tab in
                Text(tab.rawValue).tag(tab)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal)
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .sticky:
            faqList(state: store.sticky, onRetry: { store.vm.loadSticky() })
        case .popular:
            faqList(state: store.popular, onRetry: { store.vm.loadPopular() })
        case .latest:
            faqList(state: store.latest, onRetry: { store.vm.loadLatest() })
        case .news:
            newsList(state: store.news, onRetry: { store.vm.loadNews() })
        }
    }

    @ViewBuilder
    private func faqList(state: UiStateWrapper<[FaqSummary]>, onRetry: @escaping () -> Void) -> some View {
        switch state {
        case .loading:
            LoadingView()
        case .error(let message):
            ErrorRetryView(message: message, onRetry: onRetry)
        case .success(let faqs):
            if faqs.isEmpty {
                emptyView("No FAQs found")
            } else {
                List(faqs, id: \.id) { faq in
                    Button {
                        onFaqClick(faq.categoryId, faq.id)
                    } label: {
                        FaqRow(question: faq.question, updated: faq.updated)
                    }
                    .foregroundStyle(.primary)
                }
                .listStyle(.plain)
                .refreshable { onRetry() }
            }
        }
    }

    @ViewBuilder
    private func newsList(state: UiStateWrapper<[NewsItem]>, onRetry: @escaping () -> Void) -> some View {
        switch state {
        case .loading:
            LoadingView()
        case .error(let message):
            ErrorRetryView(message: message, onRetry: onRetry)
        case .success(let items):
            if items.isEmpty {
                emptyView("No news")
            } else {
                List(items, id: \.id) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.title)
                            .font(.headline)
                        if let created = item.created, !created.isEmpty {
                            Text(created)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
                .listStyle(.plain)
                .refreshable { onRetry() }
            }
        }
    }

    private func emptyView(_ text: String) -> some View {
        VStack {
            Spacer()
            Text(text)
                .font(.body)
                .foregroundStyle(.secondary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}
