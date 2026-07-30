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
        VStack(spacing: 8) {
            if let errorMessage, !errorMessage.isEmpty {
                Text(errorMessage)
                    .font(.system(size: 12))
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, DesignTokens.Spacing.lg)
            }

            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color(red: 0.95, green: 0.95, blue: 0.95))
                    .frame(height: 44)
                    .overlay(alignment: .leading) {
                        Group {
                            if trimmedText.isEmpty {
                                Text("有趣评论千千万，不如你也来一条？")
                                    .foregroundStyle(Color.black.opacity(0.28))
                            } else {
                                Text(text)
                                    .foregroundStyle(Color.black.opacity(0.88))
                                    .lineLimit(1)
                            }
                        }
                        .font(.system(size: 15))
                        .padding(.leading, 18)
                        .padding(.trailing, 52)
                    }
                    .overlay(alignment: .trailing) {
                        if !isSubmitDisabled {
                            Button(isSubmitting ? "发送中" : "发送", action: onSubmit)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Color.blue)
                                .padding(.trailing, 14)
                        }
                    }
                    .overlay {
                        TextField("有趣评论千千万，不如你也来一条？", text: $text, axis: .vertical)
                            .opacity(0.015)
                            .padding(.horizontal, 18)
                    }

                composerIcon(systemName: "photo.on.rectangle")
                composerIcon(systemName: "face.smiling")
            }

            HStack {
                Spacer()
                Text("\(characterCount)/500")
                    .font(.system(size: 10))
                    .foregroundStyle(characterCount > 500 ? Color.red : Color.black.opacity(0.18))
            }
            .padding(.horizontal, 2)
        }
        .padding(.top, 8)
        .padding(.bottom, 4)
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .background(Color.white)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.black.opacity(0.06))
                .frame(height: 0.5)
        }
    }

    private func composerIcon(systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 18, weight: .medium))
            .foregroundStyle(Color.black.opacity(0.88))
            .frame(width: 28, height: 28)
    }
}
