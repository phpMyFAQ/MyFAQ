import SwiftUI
import Shared

/// Alias to avoid clash with Foundation.Category
typealias FaqCategory = Shared.Category

/// Observable wrapper around the shared CategoriesViewModel.
final class CategoriesStore: ObservableObject {
    let vm: CategoriesViewModel

    @Published var categories: UiStateWrapper<[FaqCategory]> = .loading

    private var job: Kotlinx_coroutines_coreJob?

    init() {
        vm = CategoriesViewModel(aim: KoinHelper.activeInstanceManager, scope: defaultScope())
        job = FlowCollectorKt.collectFlow(flow: vm.categories) { [weak self] value in
            DispatchQueue.main.async {
                self?.categories = unwrapUiState(value) { castList($0) as [FaqCategory]? }
            }
        }
        vm.loadCategories()
    }

    deinit {
        job?.cancel(cause: nil)
    }
}

struct CategoriesScreen: View {
    @StateObject private var store = CategoriesStore()
    let onCategoryClick: (Int32, String) -> Void

    var body: some View {
        Group {
            switch store.categories {
            case .loading:
                LoadingView()
            case .error(let message):
                ErrorRetryView(message: message, onRetry: { store.vm.loadCategories() })
            case .success(let categories):
                let tree = buildFlatTree(categories)
                List(tree, id: \.category.id) { item in
                    Button {
                        onCategoryClick(item.category.id, item.category.name)
                    } label: {
                        HStack {
                            Text(item.category.name)
                                .font(.body)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.leading, CGFloat(item.depth) * 24)
                    }
                    .foregroundStyle(.primary)
                }
                .listStyle(.plain)
                .refreshable { store.vm.loadCategories() }
            }
        }
        .navigationTitle("Categories")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Tree flattening

private struct CategoryItem {
    let category: FaqCategory
    let depth: Int
}

private func buildFlatTree(_ categories: [FaqCategory]) -> [CategoryItem] {
    // Convert KotlinInt? parentId to Swift Int? for safe grouping
    let byParent = Dictionary(grouping: categories) { cat -> Int? in
        cat.parentId?.intValue as Int?
    }
    var result: [CategoryItem] = []

    func walk(parentId: Int?, depth: Int) {
        let children = byParent[parentId]?.sorted(by: { $0.name < $1.name }) ?? []
        for cat in children {
            result.append(CategoryItem(category: cat, depth: depth))
            walk(parentId: Int(cat.id), depth: depth + 1)
        }
    }

    // Root categories: parentId == nil or 0
    walk(parentId: nil, depth: 0)
    walk(parentId: 0, depth: 0)

    // Fallback: flat list if tree-walk found nothing
    if result.isEmpty {
        result = categories.map { CategoryItem(category: $0, depth: 0) }
    }

    return result
}
