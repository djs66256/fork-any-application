# PRD-07 Menu Panel Android Code Review

## Scope

Completed the remaining Android coding scope for PRD-07 menu-panel in `android/`, covering Step 4 static menu content and Step 5 NavGraph shell integration, while keeping recently-viewed capped at 3 items and preserving the agreed Android overlay/navigation constraints.

## Review results

### 1. Navigation shell and state machine

Result: Passed

- `MainNavigationViewModel` already owned the shell state machine; this round completed the UI integration so the shell now actually consumes:
  - `OPENING -> OPEN` on drawer animation finish,
  - `OPEN/OPENING -> CLOSING -> CLOSED` on scrim/back/menu action close,
  - deferred route consumption only after `onMenuClosedAnimationFinished()`.
- Closing-state re-entry protection is preserved: the first pending menu target wins and later taps are ignored until close completes.
- Menu placeholder navigation and recently-viewed playback navigation both go through the same close-then-navigate path.

### 2. Static sections and product alignment

Result: Passed

- Added `MenuPanelStaticEntries` to centralize menu section order and action types.
- Locked the section order to:
  1. login header
  2. message preview
  3. recently viewed
  4. game center
  5. common functions
- Login / messages / booking / downloads use explicit placeholder navigation targets.
- Game center only emits local feedback (`即将上线`) and does not navigate.
- Recently-viewed display is capped to at most 3 items in `MenuPanelViewModel`, while still allowing fewer than 3.

### 3. UI composition and placeholder handoff

Result: Passed

- Added drawer container, menu route container, and menu section components.
- `NavGraph` now overlays the menu drawer above app content, so scrim intercepts interactions for both page content and bottom navigation.
- Added menu placeholder destination registration via `menuPlaceholderSpecs()` and reused `PlaceholderScreen` for:
  - 登录
  - 我的消息
  - 我的预约
  - 我的下载
- Added snackbar feedback path for game-center placeholder actions.

### 4. Tests and regression coverage

Result: Passed at code-review level; blocked at runtime validation level

Added/updated JVM tests for:

- static section order and action model,
- recently-viewed max-3 enforcement,
- menu placeholder registration spec,
- drawer render/back helper behavior,
- existing close-after-animation state machine regression.

## Key fixes applied during review

- Fixed menu panel implementation gap by wiring actual drawer UI into `NavGraph` instead of leaving only the ViewModel/state machine layer.
- Fixed placeholder route handoff gap by registering all menu placeholder destinations in the home graph.
- Fixed product constraint gap by enforcing `recently-viewed <= 3` in ViewModel success mapping.
- Fixed feedback visibility by placing snackbar host outside the drawer surface so "即将上线" remains visible while the panel is open.
- Removed redundant action extension accessors after review to avoid confusing property shadowing.

## Validation run

### Commands attempted

- `cd android && ./gradlew app:testDebugUnitTest --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest" --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest" --tests "com.djs66256.short_drama.core.network.ApiServiceTest" --tests "com.djs66256.short_drama.data.datasource.MenuPanelRemoteDataSourceTest" --tests "com.djs66256.short_drama.data.repository.MenuPanelRepositoryImplTest" --tests "com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModelTest" --tests "com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntriesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest"`
- `cd android && ./gradlew test`
- `cd android && ./gradlew assembleDebug`
- `cd android && ./gradlew detekt`

### Result

Validation no longer has a Java-runtime blocker after running with Android Studio bundled JBR.

- Menu-panel related targeted JVM tests passed.
- `assembleDebug` passed.
- `detekt` passed with `0 code smells`.
- Full `./gradlew test` still fails, but the failures are in pre-existing unrelated tests outside the PRD-07 menu-panel scope:
  - `ClassificationViewModelTest > T-03 switching gender resets dimension and emits scroll to first dimension`
  - `SearchHomeViewModelTest > T-02 submit history and quick entry emit navigation events`

## Remaining issues

- No known source-level functional gap remains within the requested Android PRD-07 menu-panel scope.
- Repository-wide full test green is currently blocked by two unrelated Android test failures already present outside the menu-panel change set.

## Conclusion

From the code-review perspective, the Android coding scope requested for PRD-07 menu-panel is implemented and validated at the feature level.

The platform is acceptable for main-agent acceptance of the PRD-07 Android scope because:
- targeted menu-panel regression passed,
- `assembleDebug` passed,
- `detekt` passed,
- and the only remaining full-test blocker is unrelated pre-existing Android test failure outside this change scope.
