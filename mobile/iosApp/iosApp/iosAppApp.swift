import SwiftUI
import Shared

@main
struct MyFaqApp: App {
    @AppStorage("app_theme") private var themeMode: String = ThemeMode.system.rawValue

    init() {
        IosPlatformModuleKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(resolvedColorScheme)
                .environment(\.themeMode, currentThemeMode)
        }
    }

    private var currentThemeMode: ThemeMode {
        ThemeMode(rawValue: themeMode) ?? .system
    }

    private var resolvedColorScheme: ColorScheme? {
        switch currentThemeMode {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

// MARK: - Theme mode

enum ThemeMode: String, CaseIterable {
    case system
    case light
    case dark

    var label: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }
}

// MARK: - Environment key

private struct ThemeModeKey: EnvironmentKey {
    static let defaultValue: ThemeMode = .system
}

extension EnvironmentValues {
    var themeMode: ThemeMode {
        get { self[ThemeModeKey.self] }
        set { self[ThemeModeKey.self] = newValue }
    }
}
