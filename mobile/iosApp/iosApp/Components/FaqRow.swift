import SwiftUI
import Shared

/// Reusable row for FAQ summary items.
struct FaqRow: View {
    let question: String
    let updated: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(question)
                .font(.body)
                .lineLimit(3)
            if let updated = updated, !updated.isEmpty {
                Text(updated)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
