import SwiftUI

struct PaywallScreen: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showToast = false

    private let features = [
        "Rate and vote on FAQs",
        "Submit questions",
        "Post comments",
        "Register an account",
        "Priority support",
    ]

    var body: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: 32)

            Text("Unlock MyFAQ Pro")
                .font(.title)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)

            Spacer().frame(height: 32)

            VStack(alignment: .leading, spacing: 12) {
                ForEach(features, id: \.self) { feature in
                    Label(feature, systemImage: "checkmark")
                        .font(.body)
                }
            }
            .padding(.horizontal, 24)

            Spacer()

            Button {
                showToast = true
            } label: {
                Text("Annual Plan")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal, 24)

            Button {
                showToast = true
            } label: {
                Text("Lifetime Access")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal, 24)
            .padding(.top, 12)

            Button("Restore purchases") {
                showToast = true
            }
            .padding(.top, 16)
            .padding(.bottom, 24)
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .overlay(alignment: .bottom) {
            if showToast {
                Text("Coming soon")
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.ultraThinMaterial)
                    .clipShape(Capsule())
                    .padding(.bottom, 80)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation { showToast = false }
                        }
                    }
            }
        }
        .animation(.easeInOut, value: showToast)
    }
}
