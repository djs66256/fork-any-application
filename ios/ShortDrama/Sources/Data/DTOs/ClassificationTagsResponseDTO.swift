import Foundation

/// API response DTO for classification tags.
struct ClassificationTagsResponseDTO: Codable, Equatable {
    let data: ClassificationTagsPayloadDTO
}

struct ClassificationTagsPayloadDTO: Codable, Equatable {
    let gender: ClassificationGender
    let dimensions: [ClassificationDimensionDTO]
}

struct ClassificationDimensionDTO: Codable, Equatable {
    let key: ClassificationDimensionKey
    let name: String
    let tags: [String]
}

extension ClassificationTagsResponseDTO {
    func toEntity() -> ClassificationTagsPayload {
        data.toEntity()
    }
}

extension ClassificationTagsPayloadDTO {
    func toEntity() -> ClassificationTagsPayload {
        ClassificationTagsPayload(
            gender: gender,
            dimensions: normalizedDimensions()
        )
    }

    private func normalizedDimensions() -> [ClassificationDimension] {
        ClassificationDimensionKey.allCases.map { key in
            if let dimension = dimensions.first(where: { $0.key == key }) {
                return dimension.toEntity()
            }

            return ClassificationDimension(key: key, name: key.title, tags: [])
        }
    }
}

extension ClassificationDimensionDTO {
    func toEntity() -> ClassificationDimension {
        ClassificationDimension(key: key, name: name, tags: tags)
    }
}
