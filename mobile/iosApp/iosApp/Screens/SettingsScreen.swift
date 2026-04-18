import SwiftUI
import Shared

struct LanguageOption: Identifiable, Hashable {
    let code: String
    let name: String
    var id: String { code }
}

struct SettingsScreen: View {
    let onSwitchInstance: () -> Void
    @AppStorage("app_theme") private var themeMode: String = ThemeMode.system.rawValue
    @AppStorage("app_language") private var appLanguage: String = ""
    @State private var showCacheCleared = false
    @State private var showDeleteConfirmation = false
    @State private var availableLanguages: [LanguageOption] = []
    @State private var currentLanguage: String = ""
    @State private var instanceTick: Int = 0

    private var aim: ActiveInstanceManager { KoinHelper.activeInstanceManager }

    var body: some View {
        List {
            // Active instance
            if let instance = aim.activeInstance.value as? Instance {
                Section("Active instance") {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(instance.displayName)
                            .font(.headline)
                        Text(instance.baseUrl)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            // Appearance
            Section("Appearance") {
                Picker("Theme", selection: $themeMode) {
                    ForEach(ThemeMode.allCases, id: \.rawValue) { mode in
                        Text(mode.label).tag(mode.rawValue)
                    }
                }
                .pickerStyle(.segmented)
            }

            // Content language
            if let instance = aim.activeInstance.value as? Instance, !availableLanguages.isEmpty {
                Section("Content language") {
                    Picker("Language", selection: Binding<String>(
                        get: { currentLanguage },
                        set: { newCode in
                            let oldCode = instance.language as String
                            guard newCode != oldCode else { return }
                            let now = Int64(Date().timeIntervalSince1970)
                            KoinHelper.database.instancesQueries.updateLanguage(
                                language: newCode,
                                updated_at: now,
                                id: instance.id
                            )
                            aim.setLanguage(language: newCode)
                            currentLanguage = newCode
                            appLanguage = newCode
                            instanceTick += 1
                        }
                    )) {
                        ForEach(availableLanguages) { entry in
                            Text(entry.name).tag(entry.code)
                        }
                    }
                }
            }

            Section {
                Button("Switch instance", action: onSwitchInstance)

                Button("Clear cache") {
                    guard aim.activeInstance.value != nil else { return }
                    aim.repository.clearCache()
                    showCacheCleared = true
                }

                if aim.activeInstance.value != nil {
                    Button("Delete instance", role: .destructive) {
                        showDeleteConfirmation = true
                    }
                }
            }

            Section("About") {
                LabeledContent("App version", value: appVersion)
                Text("MyFAQ.app - Native client for phpMyFAQ")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                NavigationLink("Open source licenses") {
                    LicensesScreen()
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: instanceTick) {
            await loadLanguages()
        }
        .overlay(alignment: .bottom) {
            if showCacheCleared {
                Text("Cache cleared")
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.ultraThinMaterial)
                    .clipShape(Capsule())
                    .padding(.bottom, 24)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation { showCacheCleared = false }
                        }
                    }
            }
        }
        .animation(.easeInOut, value: showCacheCleared)
        .alert("Delete Instance", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                guard let instance = aim.activeInstance.value as? Instance else { return }
                let db = KoinHelper.database
                db.instancesQueries.deleteById(id: instance.id)
                aim.clear()
                onSwitchInstance()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            if let instance = aim.activeInstance.value as? Instance {
                Text("Are you sure you want to delete \"\(instance.displayName)\"? This cannot be undone.")
            }
        }
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown"
    }

    private func loadLanguages() async {
        guard let instance = aim.activeInstance.value as? Instance else {
            availableLanguages = []
            return
        }
        currentLanguage = instance.language
        do {
            let meta = try await aim.repository.meta()
            let map = meta.availableLanguages
            var entries: [LanguageOption] = []
            for code in map.keys {
                guard let name = map[code] else { continue }
                entries.append(LanguageOption(code: code, name: name))
            }
            entries.sort { $0.name.localizedCompare($1.name) == .orderedAscending }
            availableLanguages = entries
        } catch {
            availableLanguages = []
        }
    }
}
