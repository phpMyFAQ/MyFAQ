import SwiftUI

private struct LibraryEntry: Identifiable {
    let name: String
    let license: String
    let url: String
    var id: String { name }
}

private let libraries: [LibraryEntry] = [
    .init(name: "Kotlin", license: "Apache 2.0", url: "https://kotlinlang.org"),
    .init(name: "Kotlin Coroutines", license: "Apache 2.0", url: "https://github.com/Kotlin/kotlinx.coroutines"),
    .init(name: "kotlinx.serialization", license: "Apache 2.0", url: "https://github.com/Kotlin/kotlinx.serialization"),
    .init(name: "Ktor", license: "Apache 2.0", url: "https://ktor.io"),
    .init(name: "SQLDelight", license: "Apache 2.0", url: "https://github.com/cashapp/sqldelight"),
    .init(name: "Koin", license: "Apache 2.0", url: "https://insert-koin.io"),
    .init(name: "SwiftUI", license: "Apple SDK", url: "https://developer.apple.com/xcode/swiftui/"),
    .init(name: "WebKit", license: "Apple SDK", url: "https://developer.apple.com/documentation/webkit"),
]

struct LicensesScreen: View {
    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text("MyFAQ.app")
                        .font(.headline)
                    Text("Native client for phpMyFAQ — © phpMyFAQ Team.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("Licensed under MPL 2.0.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Section("Libraries") {
                ForEach(libraries) { entry in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.name)
                            .font(.body)
                        Text("\(entry.license) • \(entry.url)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .navigationTitle("Licenses")
        .navigationBarTitleDisplayMode(.inline)
    }
}
