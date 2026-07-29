import Foundation

struct CommentListResponseDTO: Codable, Equatable {
    let data: [CommentDTO]
    let pagination: PaginationDTO

    func toEntity() -> PagedResult<Comment> {
        PagedResult(
            items: data.map { $0.toEntity() },
            page: pagination.page,
            pageSize: pagination.pageSize,
            total: pagination.total,
            totalPages: pagination.totalPages
        )
    }
}
