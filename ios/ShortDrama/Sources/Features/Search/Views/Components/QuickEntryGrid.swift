import SwiftUI

/// Quick entry grid shown on the search discovery page.
struct QuickEntryGrid: View {
    let entries: [QuickEntry]
    let onTap: (QuickEntry) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 18) {
                ForEach(entries) { entry in
                    Button {
                        onTap(entry)
                    } label: {
                        HStack(spacing: 7) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .fill(entry.symbolBackgroundColor)
                                    .frame(width: 30, height: 30)

                                Image(systemName: entry.systemImage)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(entry.accentColor)
                            }

                            Text(entry.title)
                                .font(.system(size: 17, weight: .medium))
                                .foregroundStyle(.primary)
                                .lineLimit(1)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 4)
            .padding(.trailing, 18)
        }
    }
}
