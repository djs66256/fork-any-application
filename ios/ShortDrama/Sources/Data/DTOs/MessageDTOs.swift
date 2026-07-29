import Foundation

struct MessagePreviewDTO: Decodable, Equatable {
    let title: String
    let summary: String
    let relativeTime: String

    func toEntity() -> MessagePreview {
        MessagePreview(title: title, summary: summary, relativeTime: relativeTime)
    }
}

struct SystemMessageDTO: Decodable, Equatable {
    let id: String
    let title: String
    let summary: String
    let sentAt: String

    func toEntity() -> SystemMessage {
        SystemMessage(id: id, title: title, summary: summary, sentAt: sentAt)
    }
}

struct InteractionMessageDTO: Decodable, Equatable {
    let id: String
    let type: InteractionMessage.MessageType
    let title: String
    let summary: String
    let sentAt: String

    func toEntity() -> InteractionMessage {
        InteractionMessage(id: id, type: type, title: title, summary: summary, sentAt: sentAt)
    }
}

struct SystemMessageListResponseDTO: Decodable, Equatable {
    let data: [SystemMessageDTO]
    let pagination: PaginationDTO

    func toEntity() -> PagedResult<SystemMessage> {
        PagedResult(
            items: data.map { $0.toEntity() },
            page: pagination.page,
            pageSize: pagination.pageSize,
            total: pagination.total,
            totalPages: pagination.totalPages
        )
    }
}

struct InteractionMessageListResponseDTO: Decodable, Equatable {
    let data: [InteractionMessageDTO]
    let pagination: PaginationDTO

    func toEntity() -> PagedResult<InteractionMessage> {
        PagedResult(
            items: data.map { $0.toEntity() },
            page: pagination.page,
            pageSize: pagination.pageSize,
            total: pagination.total,
            totalPages: pagination.totalPages
        )
    }
}
