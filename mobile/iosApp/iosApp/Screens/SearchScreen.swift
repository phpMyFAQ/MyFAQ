import SwiftUI
import Shared

/// Observable wrapper around the shared SearchViewModel.
final class SearchStore: ObservableObject {
    let vm: SearchViewModel

    @Published var query: String = ""
    @Published var results: UiStateWrapper<[SearchResult]> = .success([])
    @Published var popularSearches: UiStateWrapper<[PopularSearch]> = .loading

    private var jobs: [Kotlinx_coroutines_coreJob] = []

    init() {
        vm = SearchViewModel(aim: KoinHelper.activeInstanceManager, scope: defaultScope())
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.query) { [weak self] value in
            DispatchQueue.main.async {
                self?.query = (value as? String) ?? ""
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.results) { [weak self] value in
            DispatchQueue.main.async {
                self?.results = unwrapUiState(value) { castList($0) as [SearchResult]? }
            }
        })
        jobs.append(FlowCollectorKt.collectFlow(flow: vm.popularSearches) { [weak self] value in
            DispatchQueue.main.async {
                self?.popularSearches = unwrapUiState(value) { castList($0) as [PopularSearch]? }
            }
        })
        vm.loadPopularSearches()
    }

    deinit {
        jobs.forEach { $0.cancel(cause: nil) }
    }
}

struct SearchScreen: View {
    @StateObject private var store = SearchStore()
    @State private var searchText: String = ""
    let onFaqClick: (Int32, Int32) -> Void

    var body: some View {
        VStack(spacing: 0) {
            searchField
            if searchText.isEmpty {
                popularSearchesView
            } else {
                searchResultsView
            }
        }
        .navigationTitle("Search")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var searchField: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("Search FAQs...", text: $searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onChange(of: searchText) { newValue in
                    store.vm.onQueryChanged(newQuery: newValue)
                }
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                    store.vm.onQueryChanged(newQuery: "")
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(10)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .padding()
    }

    @ViewBuilder
    private var popularSearchesView: some View {
        switch store.popularSearches {
        case .loading:
            LoadingView()
        case .error:
            Spacer() // Silently ignore popular search errors
        case .success(let searches):
            if searches.isEmpty {
                Spacer()
            } else {
                List {
                    Section("Popular searches") {
                        ForEach(searches, id: \.searchTerm) { popular in
                            Button {
                                searchText = popular.searchTerm
                                store.vm.onQueryChanged(newQuery: popular.searchTerm)
                            } label: {
                                Text(popular.searchTerm)
                            }
                            .foregroundStyle(.primary)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private var searchResultsView: some View {
        switch store.results {
        case .loading:
            LoadingView()
        case .error(let message):
            ErrorRetryView(message: message, onRetry: {
                store.vm.onQueryChanged(newQuery: searchText)
            })
        case .success(let results):
            if results.isEmpty {
                VStack {
                    Spacer()
                    Text("No results")
                        .foregroundStyle(.secondary)
                    Spacer()
                }
            } else {
                List(results, id: \.id) { result in
                    Button {
                        onFaqClick(result.categoryId, result.id)
                    } label: {
                        Text(result.question)
                            .lineLimit(2)
                    }
                    .foregroundStyle(.primary)
                }
                .listStyle(.plain)
            }
        }
    }
}
