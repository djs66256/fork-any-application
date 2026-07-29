import SwiftUI

struct CommentComposerView: View {
    @Binding var text: String
    let isSubmitting: Bool
    let errorMessage: String?
    let onSubmit: () -> Void

    private var trimmedText: String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var characterCount: Int {
        trimmedText.count
    }

    private var isSubmitDisabled: Bool {
        trimmedText.isEmpty || characterCount > 500 || isSubmitting
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            HStack(alignment: .bottom, spacing: DesignTokens.Spacing.md) {
                TextField("写下你的评论", text: $text, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(1...4)

                Button(isSubmitting ? "发送中" : "发送", action: onSubmit)
                    .buttonStyle(.borderedProminent)
                    .disabled(isSubmitDisabled)
            }

            HStack {
                if let errorMessage, !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                }

                Spacer()

                Text("\(characterCount)/500")
                    .font(.caption)
                    .foregroundStyle(characterCount > 500 ? .red : .secondary)
            }
        }
        .padding(DesignTokens.Spacing.lg)
        .background(.ultraThinMaterial)
    }
}
