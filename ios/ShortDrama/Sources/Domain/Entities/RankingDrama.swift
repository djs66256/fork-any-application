import Foundation

/// Ranking list item entity.
struct RankingDrama: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let description: String
    let coverUrl: String
    let category: String
    let episodeCount: Int
    let tags: [String]?
    let rating: Double?
    let createdAt: String
    let updatedAt: String
    let contentType: RankingContentType
    let playCount: Int
    let bookingCount: Int
    let recommendationScore: Double
    let isBooked: Bool
    let isBookingSubmitting: Bool

    init(
        id: String,
        title: String,
        description: String,
        coverUrl: String,
        category: String,
        episodeCount: Int,
        tags: [String]?,
        rating: Double?,
        createdAt: String,
        updatedAt: String,
        contentType: RankingContentType,
        playCount: Int,
        bookingCount: Int,
        recommendationScore: Double,
        isBooked: Bool,
        isBookingSubmitting: Bool = false
    ) {
        self.id = id
        self.title = title
        self.description = description
        self.coverUrl = coverUrl
        self.category = category
        self.episodeCount = episodeCount
        self.tags = tags
        self.rating = rating
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.contentType = contentType
        self.playCount = playCount
        self.bookingCount = bookingCount
        self.recommendationScore = recommendationScore
        self.isBooked = isBooked
        self.isBookingSubmitting = isBookingSubmitting
    }

    func withBookingState(isBooked: Bool, bookingCount: Int, isSubmitting: Bool) -> RankingDrama {
        RankingDrama(
            id: id,
            title: title,
            description: description,
            coverUrl: coverUrl,
            category: category,
            episodeCount: episodeCount,
            tags: tags,
            rating: rating,
            createdAt: createdAt,
            updatedAt: updatedAt,
            contentType: contentType,
            playCount: playCount,
            bookingCount: bookingCount,
            recommendationScore: recommendationScore,
            isBooked: isBooked,
            isBookingSubmitting: isSubmitting
        )
    }

    func withSubmitting(_ isSubmitting: Bool) -> RankingDrama {
        withBookingState(isBooked: isBooked, bookingCount: bookingCount, isSubmitting: isSubmitting)
    }
}
