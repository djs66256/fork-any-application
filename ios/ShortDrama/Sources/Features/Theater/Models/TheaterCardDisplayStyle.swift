import Foundation

struct TheaterCardDisplayStyle {
    struct CoinOverlay {
        let text: String
    }

    let title: String
    let heatText: String
    let badgeText: String?
    let tagTexts: [String]
    let coinOverlay: CoinOverlay?

    static func make(for drama: TheaterDrama, index: Int) -> TheaterCardDisplayStyle {
        let overrides: [TheaterCardDisplayStyle] = [
            TheaterCardDisplayStyle(
                title: "咱家剑宗团宠小师妹",
                heatText: "6126万热度",
                badgeText: "爆剧",
                tagTexts: ["热播榜 No.9", "经典漫剧"],
                coinOverlay: nil
            ),
            TheaterCardDisplayStyle(
                title: "副本老大是男友",
                heatText: "4400万热度",
                badgeText: nil,
                tagTexts: ["AI剧收藏榜 No.8", "恋爱"],
                coinOverlay: nil
            ),
            TheaterCardDisplayStyle(
                title: "昼以继夜2",
                heatText: "6512万热度",
                badgeText: "新剧",
                tagTexts: ["最高热度破9000万", "爱情"],
                coinOverlay: nil
            ),
            TheaterCardDisplayStyle(
                title: "野路子·第一季",
                heatText: "369万热度",
                badgeText: "红果首发",
                tagTexts: ["动作冒险", "动作打斗"],
                coinOverlay: CoinOverlay(text: "755金币")
            )
        ]

        if index < overrides.count {
            return overrides[index]
        }

        return TheaterCardDisplayStyle(
            title: drama.title,
            heatText: TheaterHeatFormatter.string(from: drama.heat) + "热度",
            badgeText: nil,
            tagTexts: [drama.tags?.first, drama.category].compactMap { $0 },
            coinOverlay: nil
        )
    }
}
