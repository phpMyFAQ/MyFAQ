import SwiftUI
import Shared

struct HomeFaqItem {
    let question: String
    let date: String?
    let url: String?

    init?(rawValue: Any) {
        guard
            let object = rawValue as? NSObject,
            let question = object.value(forKey: "question") as? String
        else {
            return nil
        }

        self.question = question
        self.date = object.value(forKey: "date") as? String
        self.url = object.value(forKey: "url") as? String
    }
}

struct HomeNewsItem {
    let id: String
    let header: String
    let date: String?

    init?(rawValue: Any) {
        guard
            let object = rawValue as? NSObject,
            let header = object.value(forKey: "header") as? String
        else {
            return nil
        }

        self.header = header
        self.date = object.value(forKey: "date") as? String
        if let rawId = object.value(forKey: "id") {
            self.id = String(describing: rawId)
        } else {
            self.id = header
        }
    }
}

func mapFaqItems(_ value: Any?) -> [HomeFaqItem]? {
    castList(value)?.compactMap(HomeFaqItem.init(rawValue:))
}

func mapNewsItems(_ value: Any?) -> [HomeNewsItem]? {
    castList(value)?.compactMap(HomeNewsItem.init(rawValue:))
}

/// Observable wrapper around the shared HomeViewModel.
final class HomeStore: ObservableObject {
    let vm: HomeViewModel

    @Published var sticky: UiStateWrapper<[HomeFaqItem]> = .loading
    @Published var popular: UiStateWrapper<[HomeFaqItem]> = .loading
    @Published var latest: UiStateWrapper<[HomeFaqItem]> = .loading
    @Published var news: UiStateWrapper<[HomeNewsItem]> = .loading

    private var jobs: [Kotlinx_coroutines_coreJob] = []

    init() {
        vm = HomeViewModel(aim: KoinHelper.activeInstanceManager, scope: defaultScope())
        startObserving()
        vm.loadAll()
    }

    private func startObserving() {
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.sticky) { [weak self] value in
            DispatchQueue.main.async {
                self?.sticky = unwrapUiState(value, cast: mapFaqItems)
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.popular) { [weak self] value in
            DispatchQueue.main.async {
                self?.popular = unwrapUiState(value, cast: mapFaqItems)
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.latest) { [weak self] value in
            DispatchQueue.main.async {
                self?.latest = unwrapUiState(value, cast: mapFaqItems)
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.news) { [weak self] value in
            DispatchQueue.main.async {
                self?.news = unwrapUiState(value, cast: mapNewsItems)
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
    private func faqList(state: UiStateWrapper<[HomeFaqItem]>, onRetry: @escaping () -> Void) -> some View {
        switch state {
        case .loading:
            LoadingView()
        case .error(let message):
            ErrorRetryView(message: message, onRetry: onRetry)
        case .success(let faqs):
            if faqs.isEmpty {
                emptyView("No FAQs found")
            } else {
                List(faqs, id: \.question) { faq in
                    Button {
                        if let urlString = faq.url, let url = URL(string: urlString) {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        FaqRow(question: faq.question, updated: faq.date)
                    }
                    .foregroundStyle(.primary)
                }
                .listStyle(.plain)
                .refreshable { onRetry() }
            }
        }
    }

    @ViewBuilder
    private func newsList(state: UiStateWrapper<[HomeNewsItem]>, onRetry: @escaping () -> Void) -> some View {
        switch state {
        case .loading:
            LoadingView()
        case .error(let message):
            ErrorRetryView(message: message, onRetry: onRetry)
        case .success(let items):
            if items.isEmpty {
                emptyView("No news")
            } else {
                List {
                    ForEach(items, id: \.id) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.header)
                                .font(.headline)
                            if let date = item.date, !date.isEmpty {
                                Text(date)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
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
