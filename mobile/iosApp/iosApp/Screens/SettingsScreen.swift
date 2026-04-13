import SwiftUI
import Shared

struct SettingsScreen: View {
    let onSwitchInstance: () -> Void
    @State private var showCacheCleared = false

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

            Section {
                Button("Switch instance", action: onSwitchInstance)

                Button("Clear cache") {
                    guard aim.activeInstance.value != nil else { return }
                    aim.repository.clearCache()
                    showCacheCleared = true
                }
            }

            Section("About") {
                LabeledContent("App version", value: appVersion)
                Text("MyFAQ.app - Native client for phpMyFAQ")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
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
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown"
    }
}
