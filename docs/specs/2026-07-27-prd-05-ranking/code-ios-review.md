# PRD-05 Ranking iOS Code Review

## Scope

Implemented the iOS first-pass coding scope for PRD-05 ranking in `ios/` and completed local validation in an isolated worktree before transplanting the changes into the feature worktree.

## What was implemented

- Replaced the old `rankingHome` placeholder with a real ranking page entry.
- Added ranking domain and data stack:
  - ranking enums, query, page item, and booking result entities,
  - ranking list and booking DTOs,
  - remote data source and repository wiring,
  - `FetchRankingsUseCase` and `BookDramaUseCase`.
- Added ranking presentation stack:
  - `RankingHomeView`,
  - tab bars, state container, list, metric, card, and booking button components,
  - `RankingViewModel` covering default load, tab switching, stale-response protection, pagination, booking updates, and login interception effect,
  - `RankingRouteBuilder` to preserve `.player(videoId:)` route reuse.
- Updated app routing/deeplink integration so `rankingHome` remains the canonical ranking entry and `djsdrama://ranking` still resolves correctly.
- Added or updated tests for API contract, repository mapping, routing/deeplink behavior, and ranking view model flows.

## Validation run

### Passed in isolated coding worktree

- `cd ios && xcodegen generate`
  - Passed.
- `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - Passed after fixing an early test compile issue and a booking DTO decode mismatch.
- `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - Passed.
- `cd ios && swiftlint lint`
  - Completed with warnings only; no serious failures.
- Final post-fix verification also passed with 137 tests and 0 failures after wiring the visible login alert.

### Passed in feature worktree after transplant

- `cd ios && xcodegen generate`
  - Passed.
- `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama test -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - Passed in the feature worktree.
  - Result: 137 tests passed, 0 failed.
- `cd ios && xcodebuild -project ShortDrama.xcodeproj -scheme ShortDrama build -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0'`
  - Passed in the feature worktree.
  - Note: the existing bundle identifier warning remained non-blocking.
- `cd ios && swiftlint lint`
  - Completed in the feature worktree with warnings only.
- During transplant validation, one required dependency file (`ios/ShortDrama/Sources/Domain/Entities/PagedResult.swift`) was found missing from the initial copy set and then added before rerunning `xcodegen`, test, and build.

### Follow-up note

- A visible SwiftUI alert is shown for the `.requireLogin` effect in `RankingHomeView`.
- No existing iOS login route or shared login interception surface was found in the current codebase, so the implementation keeps the login interception entry visible without inventing a new login flow inside this PRD.

## Review conclusion

iOS implementation is in good shape for the PRD-05 first-pass ranking scope:

- ranking entry and route reuse are implemented,
- tab switching, stale-response protection, pagination, and booking flows are covered,
- automated test coverage was added for the main state transitions and API contract,
- the missing review artifact has now been created in the feature worktree.

## coding-platforms completion assessment

iOS can be marked `coding-platforms completed`.

The transplanted files now pass the local feature-worktree verification flow (`xcodegen`, `xcodebuild test`, `xcodebuild build`, `swiftlint lint`).
