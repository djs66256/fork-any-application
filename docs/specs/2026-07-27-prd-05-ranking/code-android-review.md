# PRD-05 Ranking Android Code Review

## Scope

Implemented the Android first-pass coding scope for PRD-05 ranking in `android/` and completed local Android verification in an isolated worktree before transplanting the changes into the feature worktree.

## What was implemented

- Added canonical ranking route/query support while preserving existing `play/{videoId}` navigation semantics.
- Replaced the ranking placeholder page entry with a real `RankingScreen` integration in navigation.
- Added ranking domain models, repository contracts, use cases, DTOs, remote data source, and repository implementation.
- Added ranking UI model, `RankingViewModel`, and `RankingScreen` with support for:
  - default `all + hot` loading,
  - primary/secondary tab switching while preserving the other dimension,
  - stale-response protection,
  - pagination append and retry,
  - authenticated booking success patch update,
  - unauthenticated booking interception.
- Extended DI and API contracts for ranking list and booking endpoints.
- Added Android unit tests for route contract, API contract, repository mapping, remote data source, and ranking view model behavior.

## Validation run

### Passed

Validation was executed in the isolated Android coding worktree before transplant:

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew :app:testDebugUnitTest --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.core.network.ApiServiceTest" --tests "com.djs66256.short_drama.data.repository.RankingRepositoryImplTest" --tests "com.djs66256.short_drama.data.datasource.RankingRemoteDataSourceTest" --tests "com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModelTest"`
  - Passed.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew test`
  - Passed.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew assembleDebug`
  - Passed.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew detekt`
  - Passed.

### Environment note

- The default shell Java was not sufficient for Android Gradle execution in this environment.
- Validation required explicitly using Android Studio bundled JBR via `JAVA_HOME`.

## Review conclusion

Android implementation is complete for the PRD-05 first-pass ranking scope described in `plan-android.md`:

- ranking page route and screen are wired,
- ranking state machine behavior is covered by unit tests,
- pagination / booking / login interception behavior is implemented,
- local Android test, build, and detekt validation passed.

## coding-platforms completion assessment

Android can be marked `coding-platforms completed` after the transplanted files in this feature worktree are treated as the source of truth.

The only caveat is environmental: Android validation depends on explicitly setting `JAVA_HOME` to Android Studio bundled JBR.