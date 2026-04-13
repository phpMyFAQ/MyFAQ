import SwiftUI
import Shared

struct AddInstanceSheet: View {
    @ObservedObject var store: WorkspacesStore
    let onAdded: () -> Void

    @State private var url: String = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("https://faq.example.com", text: $url)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                Section {
                    stateContent
                }
            }
            .navigationTitle("Add Instance")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        store.vm.resetAddState()
                        dismiss()
                    }
                }
            }
        }
        .onDisappear {
            store.vm.resetAddState()
        }
    }

    @ViewBuilder
    private var stateContent: some View {
        let state = store.addState

        if state is AddInstanceStateIdle {
            Button("Connect") {
                store.vm.probeInstance(url: url)
            }
            .disabled(url.trimmingCharacters(in: .whitespaces).isEmpty)
        } else if state is AddInstanceStateProbing {
            HStack {
                Spacer()
                ProgressView("Connecting...")
                Spacer()
            }
        } else if let confirmed = state as? AddInstanceStateConfirmed {
            VStack(alignment: .leading, spacing: 8) {
                Label(confirmed.meta.title, systemImage: "checkmark.circle.fill")
                    .font(.headline)
                    .foregroundStyle(.green)
                Text("phpMyFAQ \(confirmed.meta.version)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button("Add Instance") {
                    store.vm.confirmAdd(url: confirmed.url, meta: confirmed.meta, apiToken: nil)
                    onAdded()
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
            }
        } else if let failed = state as? AddInstanceStateFailed {
            VStack(alignment: .leading, spacing: 8) {
                Text(failed.reason)
                    .foregroundStyle(.red)
                Button("Retry") {
                    store.vm.probeInstance(url: url)
                }
            }
        }
    }
}
