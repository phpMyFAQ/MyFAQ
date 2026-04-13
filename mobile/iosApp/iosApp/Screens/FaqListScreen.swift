import SwiftUI
import Shared

/// Observable wrapper for FAQ list within a category.
final class FaqListStore: ObservableObject {
    let vm: CategoriesViewModel

    @Published var faqList: UiStateWrapper<[FaqSummary]> = .loading

    private var job: Kotlinx_coroutines_coreJob?

    init(categoryId: Int32) {
        vm = CategoriesViewModel(aim: KoinHelper.activeInstanceManager, scope: defaultScope())
        job = FlowCollectorKt.collectFlow(flow: vm.faqList) { [weak self] value in
            DispatchQueue.main.async {
                self?.faqList = unwrapUiState(value) { castList($0) as [FaqSummary]? }
            }
        }
        vm.loadFaqsForCategory(categoryId: categoryId)
    }

    deinit {
        job?.cancel(cause: nil)
    }
}

struct FaqListScreen: View {
    let categoryId: Int32
    let categoryName: String
    let onFaqClick: (Int32) -> Void

    @StateObject private var store: FaqListStore

    init(categoryId: Int32, categoryName: String, onFaqClick: @escaping (Int32) -> Void) {
        self.categoryId = categoryId
        self.categoryName = categoryName
        self.onFaqClick = onFaqClick
        _store = StateObject(wrappedValue: FaqListStore(categoryId: categoryId))
    }

    var body: some View {
        Group {
            switch store.faqList {
            case .loading:
                LoadingView()
            case .error(let message):
                ErrorRetryView(message: message, onRetry: {
                    store.vm.loadFaqsForCategory(categoryId: categoryId)
                })
            case .success(let faqs):
                if faqs.isEmpty {
                    VStack {
                        Spacer()
                        Text("No FAQs in this category")
                            .foregroundStyle(.secondary)
                        Spacer()
                    }
                } else {
                    List(faqs, id: \.id) { faq in
                        Button {
                            onFaqClick(faq.id)
                        } label: {
                            FaqRow(question: faq.question, updated: faq.updated)
                        }
                        .foregroundStyle(.primary)
                    }
                    .listStyle(.plain)
                }
            }
        }
        .navigationTitle(categoryName)
        .navigationBarTitleDisplayMode(.inline)
    }
}
