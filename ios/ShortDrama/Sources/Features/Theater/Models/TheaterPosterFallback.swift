import SwiftUI

/// Local display-only fallback posters used to keep theater cards visually close to the reference
/// when backend cover URLs are placeholders or unavailable.
enum TheaterPosterFallback {
    private struct Entry {
        let assetName: String
        let titleKeywords: [String]
        let category: String?
    }

    private static let entries: [Entry] = [
        Entry(assetName: "TheaterPoster1", titleKeywords: ["团宠", "小师妹", "剑宗"], category: nil),
        Entry(assetName: "TheaterPoster2", titleKeywords: ["男友", "复合", "前夫"], category: nil),
        Entry(assetName: "TheaterPoster3", titleKeywords: ["继夜", "昼以继夜"], category: nil),
        Entry(assetName: "TheaterPoster4", titleKeywords: ["第一季", "野路子"], category: nil)
    ]

    static func image(for drama: TheaterDrama, index: Int) -> Image? {
        if let matched = matchedAssetName(for: drama) {
            return Image(matched)
        }

        let assetName = entries[safe: index % entries.count]?.assetName ?? entries[0].assetName
        return Image(assetName)
    }

    private static func matchedAssetName(for drama: TheaterDrama) -> String? {
        for entry in entries {
            if entry.titleKeywords.contains(where: { drama.title.contains($0) }) {
                return entry.assetName
            }

            if let category = entry.category, drama.category == category {
                return entry.assetName
            }
        }

        return nil
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        guard indices.contains(index) else { return nil }
        return self[index]
    }
}
