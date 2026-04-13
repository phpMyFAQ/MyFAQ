import SwiftUI
import Shared

/// Root view. Shows Workspaces if no instance is selected,
/// otherwise shows the main TabView with NavigationStacks.
struct ContentView: View {
    @State private var hasActiveInstance = false

    private var aim: ActiveInstanceManager { KoinHelper.activeInstanceManager }

    var body: some View {
        Group {
            if hasActiveInstance {
                MainTabView(
                    onSwitchInstance: {
                        aim.clear()
                        hasActiveInstance = false
                    }
                )
            } else {
                NavigationStack {
                    WorkspacesScreen(onInstanceSelected: {
                        hasActiveInstance = true
                    })
                }
            }
        }
        .onAppear {
            hasActiveInstance = aim.activeInstance.value != nil
        }
    }
}

// MARK: - Main tab view

private struct MainTabView: View {
    let onSwitchInstance: () -> Void

    var body: some View {
        TabView {
            homeTab
            categoriesTab
            searchTab
            settingsTab
        }
    }

    private var homeTab: some View {
        HomeNavigationView()
            .tabItem {
                Label("Home", systemImage: "house")
            }
    }

    private var categoriesTab: some View {
        NavigationStack {
            CategoriesNavigationView()
        }
        .tabItem {
            Label("Categories", systemImage: "list.bullet")
        }
    }

    private var searchTab: some View {
        NavigationStack {
            SearchNavigationView()
        }
        .tabItem {
            Label("Search", systemImage: "magnifyingglass")
        }
    }

    private var settingsTab: some View {
        NavigationStack {
            SettingsScreen(onSwitchInstance: onSwitchInstance)
        }
        .tabItem {
            Label("Settings", systemImage: "gearshape")
        }
    }
}

// MARK: - Navigation containers with drill-down

/// Home tab with FAQ detail drill-down.
private struct HomeNavigationView: View {
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            HomeScreen(onFaqClick: { categoryId, faqId in
                path.append(FaqRoute(categoryId: categoryId, faqId: faqId))
            })
            .navigationDestination(for: FaqRoute.self) { route in
                FaqDetailScreen(
                    categoryId: route.categoryId,
                    faqId: route.faqId,
                    onPaywall: { path.append(PaywallRoute()) }
                )
            }
            .navigationDestination(for: PaywallRoute.self) { _ in
                PaywallScreen()
            }
        }
    }
}

/// Categories tab: categories → FAQ list → FAQ detail.
private struct CategoriesNavigationView: View {
    @State private var path = NavigationPath()

    var body: some View {
        CategoriesScreen(onCategoryClick: { categoryId, categoryName in
            path.append(CategoryFaqListRoute(categoryId: categoryId, categoryName: categoryName))
        })
        .navigationDestination(for: CategoryFaqListRoute.self) { route in
            FaqListScreen(
                categoryId: route.categoryId,
                categoryName: route.categoryName,
                onFaqClick: { faqId in
                    path.append(FaqRoute(categoryId: route.categoryId, faqId: faqId))
                }
            )
        }
        .navigationDestination(for: FaqRoute.self) { route in
            FaqDetailScreen(
                categoryId: route.categoryId,
                faqId: route.faqId,
                onPaywall: { path.append(PaywallRoute()) }
            )
        }
        .navigationDestination(for: PaywallRoute.self) { _ in
            PaywallScreen()
        }
    }
}

/// Search tab: search → FAQ detail.
private struct SearchNavigationView: View {
    @State private var path = NavigationPath()

    var body: some View {
        SearchScreen(onFaqClick: { categoryId, faqId in
            path.append(FaqRoute(categoryId: categoryId, faqId: faqId))
        })
        .navigationDestination(for: FaqRoute.self) { route in
            FaqDetailScreen(
                categoryId: route.categoryId,
                faqId: route.faqId,
                onPaywall: { path.append(PaywallRoute()) }
            )
        }
        .navigationDestination(for: PaywallRoute.self) { _ in
            PaywallScreen()
        }
    }
}

// MARK: - Navigation routes

private struct FaqRoute: Hashable {
    let categoryId: Int32
    let faqId: Int32
}

private struct CategoryFaqListRoute: Hashable {
    let categoryId: Int32
    let categoryName: String
}

private struct PaywallRoute: Hashable {}
