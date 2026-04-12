import SwiftUI
import Shared

/// Phase 0 placeholder screen. Proves that the shared KMP framework,
/// Koin graph, generated client, and serialization are all wired up
/// end-to-end on iOS. Real screens land in Phase 1.
struct ContentView: View {
    @State private var metaSummary: String = "Loading..."
    @State private var hasError: Bool = false

    var body: some View {
        VStack(spacing: 16) {
            Text("MyFAQ.app")
                .font(.title)
                .fontWeight(.bold)

            Text("Phase 0 foundations")
                .font(.title3)
                .foregroundColor(.secondary)

            if hasError {
                Text(metaSummary)
                    .font(.body)
                    .foregroundColor(.red)
            } else {
                Text(metaSummary)
                    .font(.body)
            }

            Text("Pro unlocked: \(Entitlements.shared.isPro() ? "true" : "false")")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .onAppear {
            let loader = MetaLoaderHelper.create()
            loader.load(
                onSuccess: { summary in
                    DispatchQueue.main.async {
                        metaSummary = summary
                    }
                },
                onError: { reason in
                    DispatchQueue.main.async {
                        metaSummary = "Bootstrap failed: \(reason)"
                        hasError = true
                    }
                }
            )
        }
    }
}

/// Helper to resolve MetaLoader from the Koin graph without exposing
/// Koin types to Swift directly.
private enum MetaLoaderHelper {
    static func create() -> MetaLoader {
        let koin = SharedModuleKt.koinApp.koin
        return koin.get(qualifier: nil, parameters: nil) as! MetaLoader
    }
}
