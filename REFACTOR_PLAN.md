# SubwayTooter / Quasar — Phased Refactor Plan

> **Living document.** The original plan (Context → Quick Wins → Phase 1–7) was written at session start and is preserved below unchanged. The **Handoff Guide** at the top reflects the current state of the branch and is what a new contributor should read first. The phased roadmap below is the intended full shape.

---

## Handoff Guide (updated 2026-04-21, HEAD `b4f97da08`)

### At a glance

- **Branch**: `gendev`, 53 commits ahead of `origin/gendev` (all this session).
- **Build gates**: `./gradlew :app:assembleFcmDebug :app:testFcmDebugUnitTest` both pass.
  - `detekt` and `lint` still fail on pre-existing style issues (trailing whitespace, long-parameter `QuickPostSheet`). Not a regression; don't gate on these.
- **APK**: `app/build/outputs/apk/fcm/debug/app-fcm-debug.apk` builds cleanly (~62 MB).
- **Working tree**: clean.

### Roadmap progress

| Phase | Status | Notes |
|---|---|---|
| Quick Wins (5 items) | ✅ done | `5c093f1e2` → `244a939ae` |
| 1 — Theming consolidation | ⚙ partial | `StColorTokens`, `StTypography`, `UiTheme` infra landed; Styler shrink + XML attr deletion not attempted |
| 2 — ViewModel extraction | ✅ done | All 10 target Activities have `[Android]ViewModel` with `StateFlow<UiState>` |
| 3 — DataStore migration | ⏭ skipped | Untouched this session |
| 4 — AppState/App1 → Koin | ✅ core done | Koin graph stood up; `AppState.kt` 636 → 179 LOC (−72%), `App1.kt` 361 → 256 LOC. Services: `AppBusyState`, `ColumnRepository`, `TtsService`, `OkHttpClients`, Coil `ImageLoader`. Static forwarders still in place on `App1` companion for a few consumers — migrating those is cleanup, not blocking. |
| 5 — Navigation-Compose | ⚙ ~70% | **15 screens migrated + 2 dead-Activity deletions = 17 Activity classes gone.** 6 Activity source files remain: `ActMain`, `ActPost`, `ActAppSetting`, `ActKeywordFilter`, `ActText`, `ActCallback` (intentional — intent dispatcher) + `ActMediaViewer` (out-of-scope). |
| 6 — Compose rich text + Coil | ⚙ 6a + 6b done, 6c-1 landed | Glide → Coil complete. `AnkoHelper`/`GestureInterceptor` gone. `RichText` bridgehead + one pilot migration. **12 `SpannableTextView` call sites remain.** Block-level span decomposition + link-click wiring + animated-emoji rendering all deferred to 6c-2+. |
| 7 — Column state unification | ⏭ not started | Highest risk; intentionally last. |

### What the codebase looks like now

**App startup path** (`App1.onCreate`):
```
LazyContextHolder.init()
super.onCreate()
startKoin {
    androidContext(this@App1)
    modules(appModule, viewModelModule)
}
SingletonImageLoader.setSafe { GlobalContext.get().get() }  // Coil → Koin
initializeToastUtils()
prepare(applicationContext, "App1.onCreate")  // idempotent, @Volatile-gated
```

**Koin graph** (`di/AppModule.kt` + `di/ViewModelModule.kt`):
```
AppModule:
  singleOf(::AppBusyState)              // busy flags for fav/bookmark/boost
  singleOf(::ColumnRepository)          // columnList + currentAccount StateFlow
  single    TtsService(context, columnRepo)   // TTS queue + ringtone
  single    AppState(context, handler)        // thin forwarder; the old god-object
  singleOf(::NavigatorImpl) + Navigator alias // route facade
  Handler(mainLooper)
  CustomEmojiCache, CustomEmojiLister
  Cache + OkHttpClient x3 (api/cached/media)
  ImageLoader (Coil, reuses API OkHttpClient)

ViewModelModule:
  15 no-arg + AndroidViewModel bindings
  1 parameterized: NicknameViewModel(acctAscii)
```

**Navigation** (`nav/`):
```
Route (sealed @Serializable) ──► encode → JSON → Intent extra
                                              ↓
                                      RootActivity.onCreate
                                              ↓
                         AppNavHost(startDestination = decoded)
                                              ↓
                              composable<Route.X> { XScreen(...) }

Navigator (interface) ──► NavigatorImpl (Koin single)
                              │
                              ├─ events: SharedFlow<NavEvent>
                              └─ bind(navController): DisposableEffect in AppNavHost
```

Routes currently wired (17 total, 15 screens + 2 dead removed):
```
@Serializable data object     Route.About / AppSettings / ColumnList /
                              DrawableList / ExitReasons / FavMute /
                              HighlightWordList / MutedApp /
                              MutedPseudoAccount / MutedWord /
                              OssLicense / PushMessageList
@Serializable data class      Route.AccountSettings(accountDbId: Long)
                              Route.Alert(title, message)
                              Route.HighlightWordEdit(itemId, initialText)
                              Route.LanguageFilter(columnIndex: Int)
                              Route.Nickname(acctAscii, acctPretty, showNotificationSound)
```

**Rich-text stack** (`compose/richtext/` + legacy `span/`):
```
CharSequence.toRichContent(defaultLinkColor)   // NEW
    ↓
RichContent(blocks: List<RichBlock>)
    ↓
RichText(content, onLinkClick)                 // NEW (single caller so far)

[Still in use for the other 12 call sites]
SpannableTextView(AndroidView+AppCompatTextView+NetworkEmojiInvalidator)
    consumes Spannable produced by HTMLDecoder/EmojiDecoder/MFM parser
    15 custom Span types (block + inline + animated)
```

**Screens route-hosted vs still Activity-hosted**:
```
  RootActivity hosts → 15 composable<Route.X> screens
  Standalone Activities (6):
    ActMain              — app launcher + side menu + column strip; touches most things
    ActPost              — toot composer; attachments, visibility, emoji picker
    ActAppSetting        — partial VM exists; body is file I/O (import/export/fonts)
    ActKeywordFilter     — form; VM exists; runApiTask stays Activity-coupled for progress dialog
    ActText              — long-text reader + incremental regex search
    ActCallback          — external-intent dispatcher (intent filters); by design not a route
  Out of scope:
    ActMediaViewer       — dedicated refactor per master plan
```

### Patterns that recur (read these before writing similar code)

**Activity → Route migration recipe**:
1. Add the `Route.X` data object/class (parameterized or not).
2. Lift the Activity's body composable to top-level in `ui/<feature>/XScreen.kt`.
3. Replace `backPressed { finish() }` → default + `BackHandler` when confirm dialog needed.
4. Replace `ActivityResultHandler` → `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
5. Replace `finish()` / `setResult(..., finish())` → `activity.setResult(..., Intent().apply { putExtra(...) }); activity.finish()` where `activity = LocalActivity.current as? ComponentActivity` (note the cast — `LocalActivity.current` is `Activity?`, not `ComponentActivity?`).
6. `RootActivity.createIntent(ctx, Route.X(...))` + `arXxx.launch(...)` at the caller.
7. Register `composable<Route.X> { entry -> val r = entry.toRoute<Route.X>(); XScreen(r.field1, r.field2) }` in `AppNavHost`.
8. Delete the Activity `.kt` + its manifest entry.
9. If the Activity's VM used parameter injection via Koin (`parametersOf`), either drop it to a plain `viewModel(key=…, factory=viewModelFactory { initializer { X(arg) } })` call in the Screen, OR inject via Koin with `parametersOf` at the Activity boundary. The former plays nicer with NavHost's scoping.

**Koin service extraction recipe**:
1. Create the class in `services/` with constructor deps.
2. Add `singleOf(::X)` or `single { X(androidContext(), get()) }` to `AppModule`.
3. If existing callers use a god-object (e.g. `AppState.X()`), add a **thin forwarder** on the god-object (`fun X() = injectedService.X()`) so call sites don't need to change. That's the safe default from Phase 4. Progressively migrate callers later.

**Don't**: Introduce a partial state. E.g. don't add Coil for some screens while Glide stays for others — plan warns against this and it bit us (generated KSP artifacts for the deleted `MyAppGlideModule` got cached; had to `rm -rf app/build/generated/ksp/fcmDebug`).

### Gotchas we hit (read these before rediscovering them)

1. **KSP caches generated Kotlin sources**. When deleting `MyAppGlideModule`, the generated `GeneratedAppGlideModuleImpl.kt` stayed around and failed compilation. `rm -rf app/build/generated/ksp/fcmDebug/` is the fix. A full `./gradlew clean` also works.

2. **`LocalActivity.current` returns `Activity?`, not `ComponentActivity?`**. Most of the app's `launchAndShowError` / `launchMain` / `dialogColorPicker` / `confirm` extensions are on `ComponentActivity`. You'll need `LocalActivity.current as? ComponentActivity` in screen composables.

3. **Koin's `parametersOf()` is Activity-level, not Composable-level**. If you need a parameterized VM inside a NavHost route, use `viewModel(key = arg, factory = viewModelFactory { initializer { YourVM(arg) } })` instead of reaching for Koin.

4. **`sealed interface Route` + kotlinx.serialization polymorphism**. Marking the sealed interface with `@Serializable` alongside each subtype's `@Serializable` makes `Json.encodeToString(Route.serializer(), route)` work for both `data object` and `data class` variants. Default `ignoreUnknownKeys = true` on the `Json` instance buys forward compat.

5. **Coil 3 API choice**. `AsyncImage(model = …)` for normal cases. For suspend-bitmap needs (e.g., `util/LoadIcon.kt`), use `SingletonImageLoader.get(context).execute(ImageRequest)` and extract `(result.image as? BitmapImage)?.bitmap` — remember `.allowHardware(false)` if you plan to pass the bitmap around.

6. **`Glide` cannot be fully removed.** `apng_android` library module still uses `WebpDecoder` from `zjupure/GlideWebpDecoder`. We kept `libs.glide` in `libs.versions.toml` with an explanatory comment; the app module itself no longer compiles against Glide.

7. **`AppState.saveColumnList(bEnableSpeech)` the instance method is a no-op** under the fixed-columns model. Don't delete it — it's called from ~18 sites across `columnviewholder/`. The deletion target in Phase 7 is the whole `AppState`, not this method in isolation.

8. **Pre-existing gradle mods**. `app/build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` had 11 lines of uncommitted additions at session start (lifecycle-runtime deps + debug keystore config). Those are now committed under `c35effb90`. If you pull and see similar mods appearing again, they're likely from an Android Studio sync.

### Dead code removed this session (for reference)

Total LOC deleted: ~**3,200**, not counting the rewrites.

| Commit | What | LOC |
|---|---|---|
| `38f295e53` | `view/GravitySnapHelper.kt` | 217 |
| `40f432a9b` | `ActColumnCustomize.kt` | 555 |
| `d818103f2` | `ActColumnList` + VM + dead AppState column-list IO | ~500 |
| `a582cc769` | `MyAppGlideModule.kt` | 156 |
| `4e3da620a` | `util/AnkoHelper.kt` | 57 |
| `50a466046` | `view/GestureInterceptor.kt` | 25 |
| (during migrations) | 15 Activity classes (ActExitReasons → ActHighlightWordEdit) | ~2,400 cumulative |

### Recommended next steps (in rough risk order)

**Easy wins (~1 hour each)**:
- Finish `onLinkClick` wiring in `RichText` via Compose 1.7 `LinkAnnotation` / `ClickableText` so the pilot display-name migration actually handles mentions. Look up `RICH_LINK_TAG` annotations and dispatch to `MyClickableSpanHandler`.
- Audit the static forwarders remaining on `App1.Companion` (`ok_http_client`, `custom_emoji_cache`, etc.). Migrate each caller to `by inject()` where cheap, then delete the forwarder.

**Medium**:
- **Phase 6c-2**: block-level span decomposition. Range-walk the Spannable looking for `BlockQuoteSpan` / `BlockCodeSpan` / `HrSpan` / `OrderedListItemSpan` / `UnorderedListItemSpan` / `DdSpan`; split the text at their boundaries; emit the matching `RichBlock`. Pilot on a status content composable.
- **Phase 6c-3**: `NetworkEmojiSpan` animation. A `rememberInfiniteTransition` per visible emoji, gated on LazyList visibility.
- **Phase 2+ tail**: simpler Phase-5 screens remaining (`ActText` is the least bad of the bunch — mostly self-contained search-in-long-text).

**Hard / high-risk**:
- **ActMain migration**. Touches nearly everything. Deletes `actmain/ActMainRegistry.kt` + the `WeakReference<ActMain>` pattern. Gate behind Phase 5 completion.
- **Phase 7 (Column state)**. The master plan's 4-PR breakdown still applies; see the "Phase 7" section below.

### File layout cheat-sheet

```
app/src/main/java/es/ariaontheplanet/quasar/
├── App1.kt                    — Application + Koin + Coil singleton setup
├── AppState.kt                — (179 LOC) thin forwarder over Koin services
├── RootActivity.kt            — Single-Activity host for Route.X composables
├── actmain/
│   ├── ActMainRegistry.kt     — TEMPORARY: WeakReference<ActMain> for ActPost
│   └── MainViewModel.kt, *.kt
├── compose/
│   ├── StTheme.kt, StColorTokens.kt, StTypography.kt, UiTheme.kt
│   ├── StExtendedColors.kt
│   └── richtext/              — NEW (Phase 6c)
│       ├── RichContent.kt     — Data model
│       ├── SpannableToRichContent.kt   — Converter
│       └── RichText.kt        — Renderer
├── di/
│   ├── AppModule.kt           — Koin app-scope singles
│   └── ViewModelModule.kt     — viewModel { } bindings
├── nav/
│   ├── Routes.kt              — @Serializable sealed interface
│   ├── Navigator.kt           — Interface + NavigatorImpl + NavEvent
│   └── AppNavHost.kt          — NavHost + composable<Route.X> entries
├── services/
│   ├── AppBusyState.kt
│   ├── ColumnRepository.kt
│   ├── TtsService.kt          — Handler-based TTS queue, lifted from AppState
│   ├── TtsModels.kt           — DedupMode, DedupItem
│   ├── OkHttpClients.kt       — prepareOkHttp factory + named qualifiers
│   └── ImageLoaderFactory.kt  — Coil ImageLoader with SVG + animated decoders
├── span/                      — 15 custom Span classes (still live)
└── ui/
    ├── about/AboutScreen.kt
    ├── alert/AlertScreen.kt
    ├── common/MuteItemRow.kt
    ├── exitReasons/ExitReasonsScreen.kt
    ├── highlightWord/{HighlightWordListScreen,HighlightWordEditScreen,HighlightWordSound}.kt
    ├── languageFilter/LanguageFilterScreen.kt (+ VM)
    ├── nickname/NicknameScreen.kt
    ├── ossLicense/OssLicenseScreen.kt
    └── pushMessageList/PushMessageListScreen.kt
```

---

## Context

The app has been partially migrated from the XML/View world to Jetpack Compose. `app/src/main/res/` has no `layout/` directory at all, so all UI is either Compose or Compose-hosting-`AndroidView`. The migration left a halfway state: theming, column state, and global singletons still assume the old View/Activity architecture, and Compose was bolted on alongside them rather than replacing them.

That shows up as:

- **5 overlapping theming systems.** XML theme attrs (`attrs.xml` / `colors.xml`), an `StExtendedColors` table of hard-coded hex, `Styler.kt`'s 497-LOC wall of `attrColor()` / `ColorStateList` helpers, per-column color overrides serialized into JSON plus a DB table (`AcctColor`), and `stColorScheme()` in `util/ComposeUtils.kt` which returns plain `lightColorScheme()/darkColorScheme()` decoupled from `StExtendedColors`. One change to an accent color needs edits in all five.
- **Activity-held state instead of ViewModels.** `MainViewModel` / `PostViewModel` / etc. exist for the big screens, but the small settings screens (`ActMutedWord`, `ActFavMute`, `ActNickname`, `ActMutedApp`, `ActMutedPseudoAccount`, `ActDrawableList`, `ActHighlightWordList`, `ActKeywordFilter`, `ActColumnList`, parts of `ActAppSetting`) drive UI state directly from the Activity — no `StateFlow`, no config-change survival. `ActMain.kt:147` holds a `WeakReference<ActMain>? = null` back-reference.
- **Global singletons duplicating what DI would do.** `AppState.kt` (~636 LOC) is a manual app-scoped singleton with `currentAccount`, the column list, busy flags, and a TTS queue driven by `Handler + Runnable` (`AppState.kt:223–300`). `App1.kt` has its own static caches (`custom_emoji_cache`, `custom_emoji_lister`, `appStateX` at line 213). **Koin is declared** as a dep in `app/build.gradle.kts:124, 166–168` **but is not imported anywhere in app source** — it's unused infrastructure waiting to be adopted.
- **SharedPreferences with hand-rolled wrappers.** `pref/PrefB.kt`, `PrefI.kt`, `PrefS.kt`, `PrefL.kt`, `PrefF.kt`, `PrefDevice.kt`, `pref/impl/*.kt`. No `Flow` from a pref change; consumers either read once or use ad-hoc listeners. Several dead keys sit commented in `PrefB.kt`.
- **Legacy span/emoji pipeline.** `util/NetworkEmojiInvalidator.kt` (103 LOC) + `view/NetworkEmojiView.kt` (194 LOC) are a Handler-based span-invalidation pipeline that only still exists because Compose wraps a `SpannableTextView` via `AndroidView`. 16 custom `Span` classes in `span/` are the actual rich-text render path; Compose `AnnotatedString` is used in few places.
- **Navigation is raw intents.** Every screen transition goes through `Intent(...) + startActivity()`. No abstraction, no type safety on args.
- **Column state is a three-layer mess.** `column/Column.kt` (355 LOC, holds `AppState` + `SavedAccount` + mutable scroll/view-holder state) + `columnviewholder/` (8 files, `Handler + Runnable` state container, no longer a `View`) + `compose/ColumnUiState.kt` (188 LOC, 100+ `mutableStateOf` fields with a `forceRecompose()` hack at lines 167–169). The `column/` module is **~9,900 LOC** across 19 files; `ColumnType.kt` alone is 2,193 LOC.

Goal: make the codebase feel like a modern Compose app — one theme, one state container per screen, one DI graph, one prefs layer, one navigation surface — without a big-bang rewrite. Each phase is independently mergeable.

Target schedule: **~9–10 weeks**, 7 phases. Risk ramps up over time; the highest-risk phase (Column state unification) is deliberately LAST so everything else is stable before we touch it.

---

## Quick Wins (five ~1-day PRs to start with, runnable in parallel with Phase 1 planning)

1. **Delete commented-out dead pref keys** in `app/src/main/java/es/ariaontheplanet/quasar/pref/PrefB.kt` (`bpDisableFastScroller`, `bpNotificationLED`, `bpNotificationSound`, `bpNotificationVibration`). ~20 LOC deleted.
2. **Remove `ActMain.refActMain` `WeakReference<ActMain>`** at `ActMain.kt:115, 147, 328, 419`. Audit callers (`Grep refActMain`), inline or replace with a temporary `ActMainRegistry` object. Phase 5 will make this cleaner; today, just remove the WeakRef back-pointer.
3. **Audit `view/GravitySnapHelper.kt`** — if its 15 refs are all from dead or soon-to-delete code, remove now; otherwise defer to Phase 6.
4. **Sweep 48 `TODO`/`FIXME` markers** — one grep pass, triage into fix-now / convert-to-issue / delete-stale. Most will be the third bucket.
5. **Merge `stColorScheme()` into `compose/StTheme.kt`** — `util/ComposeUtils.kt:15` returns decoupled Material3 schemes. Inline it into `StTheme.kt` so it shares a source with `StExtendedColors`. Sets up Phase 1. ~15 LOC.

---

## Phase 1 — Theming consolidation (~1.5 weeks)

**Goal.** One source of truth for app colors and typography (`StExtendedColors` + M3 `Typography`), every other system routed through it or deleted.

**Concrete changes.**

- Promote `StExtendedColors` to single source of truth. Replace hard-coded `0xFF0088FF` etc. in `compose/StExtendedColors.kt:34–52` with named tokens in a new `compose/StColorTokens.kt`. Add a `MastodonStExtendedColors` variant using values from `res/values/colors.xml:24–29`.
- Unify `ColorScheme` source. Move `util/ComposeUtils.kt:stColorScheme` into `compose/StTheme.kt`. Read a `UiTheme` enum (Light/Dark/Mastodon/System) from prefs, derive `ColorScheme` from the same tokens that feed `StExtendedColors`. Update `compose/StTheme.kt:38, 73` (`StScreen`, `StThemedContent`).
- Add M3 `Typography`. New file `compose/StTypography.kt` built from the user's font/size prefs (`PrefF.*`, `PrefS.spTimelineFont*`). Provide a `LocalStExtendedTypography` for app-specific text styles. Replace `ActMain.timelineFont*` globals set in `actmain/ActMainStyle.kt:28–50` — either back them with the new `Typography` source or inline consumers onto `LocalStExtendedTypography`.
- Delete custom XML color attrs. Remove `colorButtonAccentBoost/Favourite/Bookmark/Follow/FollowRequest/Reaction` from `res/values/attrs.xml:16–21` and per-theme entries from `res/values/colors.xml:7–29`. Keep notification-accent colors (`colors.xml:40–54`, used by `NotificationCompat`).
- Shrink `Styler.kt` (497 LOC → ~150). Audit each helper; if all callers are Compose, delete and rewrite to `MaterialTheme.colorScheme` / `StThemeEx.colors`. If callers are AndroidView wrappers, keep but mark for Phase 6 deletion.
- Consolidate per-column color defaults. Move `column/Column.kt:88–102` (`defaultColor*` statics + `reloadDefaultColor()`) into `StThemeEx`. Clean up inline ternaries at `ActColumnList.kt:179–181` and duplicate helpers at `column/ColumnExtra1.kt:103–112` — both route through a single `Column.effectiveHeaderBg(theme)` helper.

**New files.** `compose/StColorTokens.kt` (~60 LOC), `compose/StTypography.kt` (~80 LOC).

**LOC delta.** Delete: `Styler.kt` ~350, `colors.xml` ~25, `attrs.xml` ~7, `ActMainStyle.kt` ~30, misc ~25. Add: ~180. **Net ~−250 LOC.**

**Risks.** Small visual diffs (user OK'd). Biggest risk: a color silently routed through the XML-attr path ends up different via `StExtendedColors`. Mitigation: preserve Mastodon theme values exactly. Don't touch `AcctColor` DB format or `ColumnEncoder` JSON format.

**Success criteria.**

- `./gradlew detektAll assembleFcmDebug testFcmDebugUnitTest` green.
- Zero references to deleted custom XML attrs (Kotlin + XML grep).
- Every `attrColor(MR.attr.color*)` either gone or confined to `Styler.kt` (flagged for Phase 6).
- Spot check: Light / Dark / Mastodon themes applied to timeline, a status with active boost/favourite/bookmark, account switcher, app-settings. Colors match pre-refactor within expected noise. Custom per-column color still renders.

---

## Phase 2 — ViewModel extraction for simple Activities (~1 week)

**Goal.** Every "simple settings" Activity has state in a ViewModel exposing `StateFlow<UiState>`. No Activity holds mutable state directly. `collectAsStateWithLifecycle()` replaces raw `collectLatest()`.

**Concrete changes.**

- Extend `util/ViewModelUtils.kt` with a `collectAsStateWithLifecycle` helper.
- Extract VM + UiState per Activity, ordered by ascending complexity (nail the pattern on simple ones first):
  1. `ActMutedPseudoAccount.kt` (62 LOC) → `actmutedpseudoaccount/MutedPseudoAccountViewModel.kt` (template).
  2. `ActMutedApp.kt` (70 LOC).
  3. `ActFavMute.kt` (83 LOC).
  4. `ActDrawableList.kt` (90 LOC).
  5. `ActMutedWord.kt` (113 LOC).
  6. `ActHighlightWordList.kt` (239 LOC).
  7. `ActNickname.kt` (262 LOC).
  8. `ActColumnList.kt` (345 LOC) — uses `StExtendedColors` from Phase 1; pass in via `@Composable`, not held in VM.
  9. `ActKeywordFilter.kt` (642 LOC) — larger; state includes edit-form, validation, commit.
  10. `ActAppSetting.kt` (759 LOC) — partial extraction; `AppSettingViewModel.kt` exists, pull remaining Activity-held state in.
- Each Activity splits into three commits: (1) introduce VM + UiState, route reads through it; (2) move mutations (DAO calls, suspend work) into VM; (3) replace remaining `collectLatest` with `collectAsStateWithLifecycle`.
- Instantiate VMs with standard `by viewModels { factory }`. Koin wiring is Phase 4.

**New files.** 10 VMs + 10 UiState files.

**LOC delta.** Net ~**+500 LOC** (structural investment; reductions come later when these VMs host logic now in `AppState`).

**Risks.** `ActKeywordFilter.kt` + `ActAppSetting.kt` are the biggest — budget a full day each. `collectAsStateWithLifecycle` requires `androidx.lifecycle:lifecycle-runtime-compose` (confirm BOM or add).

**Success criteria.**

- All 10 Activities have matching `*ViewModel.kt` with `StateFlow<UiState>`.
- Zero `collectLatest` in those 10 Activity files.
- Rotation preserves transient UI state where it previously did (no regression; fixing pre-existing lossy rotations is out-of-scope for this phase).
- `./gradlew assembleFcmDebug testFcmDebugUnitTest detektAll` green.

---

## Phase 3 — DataStore migration for prefs (~1.5 weeks)

**Goal.** `SharedPreferences` gone from app source. All prefs via a single `AppPreferences` Koin-provided service backed by `androidx.datastore.preferences`, exposing `Flow<T>` per key.

**Concrete changes.**

- Add `androidx.datastore:datastore-preferences` to `app/build.gradle.kts`.
- New package `pref/datastore/`:
  - `AppPreferences.kt` — interface with `val uiTheme: Flow<UiTheme>`, `suspend fun setUiTheme(...)` etc.
  - `AppPreferencesImpl.kt` — DataStore-backed.
  - `DevicePreferences.kt` + `Impl` — mirrors `PrefDevice.kt` (preserve the split — device prefs don't back up).
  - `PreferenceKeys.kt` — `preferencesKey<Boolean>("disable_emoji_animation")` etc. **Exact same string keys** as legacy — the user's data must survive.
- Migration. Use `SharedPreferencesMigration` on the DataStore builder so first-launch reads existing SP values and DataStore becomes canonical. Same for `PrefDevice`'s named SP instance.
- Migrate consumers in waves: (1) theme/font prefs (easy win after Phase 1); (2) booleans; (3) strings/ints/longs/floats; (4) `PrefDevice` last.
- For each pref: add key to `PreferenceKeys.kt`; change `PrefB.bpFoo` (etc.) to a thin shim reading from `AppPreferences` (keep name for grep-stability); final pass deletes the shim and rewrites callers to inject `AppPreferences`.
- Delete legacy pref infrastructure at the end: `pref/PrefB.kt`, `PrefI.kt`, `PrefS.kt`, `PrefL.kt`, `PrefF.kt`, `PrefExt.kt`, `LazyContextHolder.kt`, `pref/impl/`, `PrefDevice.kt`.
- In `StTheme.kt`, `collectAsStateWithLifecycle` on the theme `Flow` → live theme changes without app restart.

**LOC delta.** Add ~400 LOC in `pref/datastore/`. Delete ~600 LOC (`pref/` + `pref/impl/`) + ~400 LOC of legacy call patterns. **Net ~−500 LOC.**

**Risks.** Data loss if migration wrong. **Mitigation:** unit test that pre-populates an SP file with every key, boots DataStore with the migration, asserts every value matches. Gate in CI. Synchronous reads at startup use `runBlocking { appPrefs.uiTheme.first() }` — only at genuine init points.

**Success criteria.**

- Zero `SharedPreferences` matches in `app/src/main` outside `pref/datastore/`.
- First-launch test passes: existing user prefs survive upgrade.
- Install refactor branch over a real user-data install; confirm settings intact.
- `./gradlew assembleFcmDebug testFcmDebugUnitTest detektAll` green.

---

## Phase 4 — `AppState` / `App1` decomposition into Koin services (~1 week)

**Goal.** The two global singletons (`AppState` ~636 LOC, `App1` ~361 LOC) are broken up into focused services injected via **newly introduced** Koin graph.

> **Note.** Koin is a declared dep in `app/build.gradle.kts:124, 166–168` but has **zero imports** in app source. Phase 4 is *introducing* Koin, not decomposing an existing graph. Budget a small extra day for `startKoin` wiring, first module definitions, and smoke-testing injection points.

**Concrete changes.**

- Stand up Koin. In `App1.onCreate`, `startKoin { androidContext(this@App1); modules(appModule, servicesModule, repositoriesModule, viewModelModule) }`.
- Create module files under `di/`:
  - `AppModule.kt` — app-scope singles (`AppPreferences`, `OkHttpClient`, `Json`, `CustomEmojiCache`, `CustomEmojiLister`).
  - `ServicesModule.kt` — `TtsService`, `StreamingService`, `NotificationPolicy`.
  - `RepositoriesModule.kt` — `AccountRepository`, `ColumnRepository`, `HighlightWordRepository`, `FavMuteRepository`, etc.
  - `ViewModelModule.kt` — `viewModel { ... }` bindings for VMs added in Phase 2. Replace `viewModels { factory }` with `by viewModel()` in the matching Activities.
- Decompose `AppState`:
  - TTS subsystem (`AppState.kt:223–300`) → `TtsService` (Koin single, owns `Handler`/coroutine). Consume highlight words via DAO `Flow` instead of re-scanning.
  - Column list + `currentAccount` → `ColumnRepository` exposing `StateFlow<List<Column>>` and `StateFlow<SavedAccount?>`.
  - `encodeColumnList()`/`decodeColumnList()` → `ColumnPersistence` (Koin single).
  - Busy flags → `AppBusyState` (Koin single).
- Decompose `App1.kt`:
  - Move `custom_emoji_cache`, `custom_emoji_lister` (line 213 area) into Koin singles; delete static fields.
  - Delete `appStateX` and the static `sound()` catch-`NoSuchFieldError` at `:219–228`; new `TtsService.sound()` is non-static.
  - `App1` shrinks to Application bootstrap + Koin init + Glide init (Glide removal is Phase 6).
- Delete `AppState.getAppState(context)` — every caller becomes `by inject()` / `get<...>()`.
- Remove the `WeakReference<ActMain>` if Quick Win #2 didn't land — replace consumers with `Navigator` or a `Flow<MainNavEvent>` from a `MainEventsBus`.

**LOC delta.** Delete ~800 LOC across `AppState.kt` + `App1.kt` + inlined callers. Add ~1,000 LOC in services/di. **Net +200 LOC,** vastly more testable.

**Risks.** Initialization order — `AppState.prepare()` does startup work synchronously; Koin's `startKoin` is synchronous, but any service needing DB/DataStore must go via a `suspend fun warmup()` called from `App1.onCreate` on `Dispatchers.IO`. Column state stays in `Column.kt` this phase — that's Phase 7. TTS is subtle — port the `Handler + Runnable` queue faithfully first, follow-up PR replaces it with a `Channel<String>` consumer.

**Success criteria.**

- `AppState.kt` ≤50 LOC glue (or deleted).
- `App1.kt` ≤200 LOC.
- Zero references to `appStateX`, `AppState.getAppState`, `App1.custom_emoji_cache`, `App1.custom_emoji_lister`.
- `import org.koin` appears in 10+ files (was 0).
- TTS speaks on notification; column list persists across restart; account switcher works.
- `./gradlew assembleFcmDebug testFcmDebugUnitTest detektAll` green.

---

## Phase 5 — Navigation-Compose layer (~1 week)

**Goal.** A single `AppNavHost` owns navigation. Every inter-screen move is `navController.navigate(Route.X(...))` with type-safe args. Existing Activities stay as hosting glue but go through a `Navigator` facade.

**Concrete changes.**

- Add `androidx.navigation:navigation-compose`.
- `nav/Routes.kt` — `sealed interface Route { object Main : Route; data class Post(...) : Route; object AppSettings : Route; data class AccountSettings(val accountDbId: Long) : Route; object ColumnList : Route; ... }`. Use Navigation-Compose's typed routing + `kotlinx.serialization`.
- `nav/Navigator.kt` — Koin single. Interface `{ fun navigate(route: Route); fun back() }`. Implementation holds the `NavController` (registered by `AppNavHost` via `LaunchedEffect`). VMs/services inject `Navigator` — never touch `NavController` directly (keeps them framework-free).
- `RootActivity.kt` + `nav/AppNavHost.kt` — each existing Activity gets a `composable<Route.Foo> { FooScreen(...) }`. For Activities not yet extracted (Phase 2), the body can still `startActivity` an intent — fine, centralizes the call and allows file-by-file conversion later.
- Replace every `Intent(this, ActXxx::class.java) + startActivity` with `navigator.navigate(Route.Xxx(...))`. For Phase-2-extracted screens, move the Composable into `AppNavHost` and delete the Activity shell.
- Kill the `WeakReference<ActMain>` if still present.

**LOC delta.** Add ~500 LOC infra. Delete ~400 LOC scattered Intent-building + ~200 LOC Activity shells. **Net flat.**

**Risks.** Deep links, push intents, `ActCallback`, media viewer, share-to-app — don't force these through `NavController`. Keep an Activity-level `onCreate` that calls `navigator.navigate(...)` after handling the external Intent. `ActMediaViewer` + `ActPost` use activity-for-result — stay on `ActivityResultContract`. Compose Navigation's back-stack semantics differ slightly from Activity stack — budget ½ day for surprises.

**Success criteria.**

- Every `startActivity(Intent(...))` call site in app code is either `navigator.navigate(...)` or explicitly whitelisted as external/system intent (share, browser) with a comment.
- `WeakReference<ActMain>` gone.
- Spot-check: navigate to account settings, app settings, post composer, a profile from a status; back out of each. Push-notification open-in-column still works. Share-image-to-app still works.
- `./gradlew assembleFcmDebug testFcmDebugUnitTest detektAll` green.

---

## Phase 6 — Delete legacy span / emoji-invalidator path; migrate to Compose rich text + Coil (~1.5 weeks)

**Goal.** `NetworkEmojiInvalidator` + `NetworkEmojiView` + `AndroidView`-wrapped `SpannableTextView` path gone. Rich text renders via `AnnotatedString` + `InlineTextContent` for custom emoji. Glide replaced by Coil (app-wide — media viewer explicitly out-of-scope). `AnkoHelper.kt` gone.

**Concrete changes.**

- Add `io.coil-kt.coil3:coil-compose` + `coil3-network-okhttp`. Use the Koin-provided `OkHttpClient` for shared caching/user-agent.
- Replace `Glide.with(...)` with `AsyncImage(...)` one screen at a time. `MyAppGlideModule.kt` → Coil `ImageLoader` config in `AppModule.kt`.
- Rich text — per-span Compose equivalent:
  - `MyClickableSpan` → `buildAnnotatedString { withAnnotation("url", ...) { ... } }` + `ClickableText`.
  - `HighlightSpan`, `BlockQuoteSpan`, `HrSpan` → `SpanStyle`.
  - `InlineCodeSpan`, `BlockCodeSpan` → `SpanStyle(fontFamily = FontFamily.Monospace)` / block composable.
  - `EmojiImageSpan`, `NetworkEmojiSpan`, `SvgEmojiSpan` → `InlineTextContent` keyed by shortcode, resolved via `CustomEmojiCacheService` (Phase 4) loading through Coil.
  - `AnimatableSpan`, `MisskeyBigSpan`, `MisskeyMotionSpan` → Compose wrappers using `rememberInfiniteTransition`.
  - `OrderedListItemSpan`, `UnorderedListItemSpan`, `DdSpan` → structural composables in MFM renderer.
- Introduce `@Composable fun RichText(decoded: DecodedText)` — timeline/status composables call this, replacing `SpannableTextView` `AndroidView` wrapper.
- Delete: `util/NetworkEmojiInvalidator.kt`, `view/NetworkEmojiView.kt`, `util/AnkoHelper.kt` (rewrite callers to plain Kotlin / Compose), residual `Styler.kt` helpers from Phase 1, whichever `span/` classes are fully replaced (aim 12 of 16), `MyAppGlideModule.kt`, `view/GravitySnapHelper.kt` if not already gone, `view/MyLinkMovementMethod.kt` (dead once SpannableTextView is gone).
- Audit other `view/`: `GestureInterceptor.kt`, `PinchBitmapView.kt`. Media viewer is **out-of-scope** — `PinchBitmapView.kt` stays.

**LOC delta.** Delete ~1,000 LOC (invalidator/view ~300, spans ~500, Glide ~100, Anko ~100). Add ~600 LOC Compose rich-text. **Net ~−400 LOC.**

**Risks.** Visual parity for rich text — this is where the user's "small visual diffs OK" gets used hardest. Compose `AnnotatedString` has different line-breaking, link hit-testing, inline-content baseline alignment. Budget time for pixel comparison on dense statuses (emoji + mentions + hashtags + code + blockquote). Custom-emoji GIF/APNG animation — prove it on notifications first (one `rememberInfiniteTransition` per visible emoji, gated on LazyList visibility) before rolling out. Coil cache coexists briefly with Glide during transition — don't ship intermediate.

**Success criteria.**

- `NetworkEmojiInvalidator` / `NetworkEmojiView` files don't exist.
- Zero matches for `Glide.with(`.
- Zero matches for `AnkoHelper`.
- `span/` ≤4 files (MFM parser's internal-only reps).
- Spot-check: a status with 4 custom animated emoji, 2 links, a code block, a blockquote, a mention, a hashtag — renders & animates smoothly in home timeline and notifications. Link taps open expected destinations.
- `./gradlew assembleFcmDebug testFcmDebugUnitTest detektAll` green.

---

## Phase 7 — Column state unification (LAST, highest risk, ~2 weeks)

**Goal.** `Column.kt` + `columnviewholder/` + `compose/ColumnUiState.kt` collapse into **one** state container (`ColumnScreenState`) owned by **one** `ColumnViewModel`, with no Handler/Runnable state machines and no `forceRecompose()` hack.

**Why last.** Every earlier phase has reduced risk here: Phase 1 moved per-column color defaults out of `Column.kt`; Phase 4 replaced `AppState` references with `ColumnRepository`; Phase 5's `Navigator` means column actions no longer need an `ActMain`; Phase 6 removed `AndroidView`-wrapped `SpannableTextView` from the timeline. By now `Column` is just a data class with a task pipeline attached, and `ColumnUiState` is a pile of `mutableStateOf` replaceable with `StateFlow` without breaking the world.

**Concrete changes.**

- Define `ColumnScreenState` (immutable data class): `spec: ColumnSpec` + `header: HeaderState` + `body: BodyState` (content | loading | error) + `filters` + `settings` + `scroll` + `busy`. UI events go through `ColumnAction` sealed interface → `ColumnViewModel.onAction(...)` → new state. The `forceRecompose()` counter disappears — Compose observes a single `StateFlow<ColumnScreenState>`.
- `ColumnViewModel` (one per open column). Hosted by `ColumnRepository` in a `Map<ColumnId, ColumnViewModel>` or keyed via Navigation-Compose VM store. Owns `viewModelScope`. All `ColumnTask_*` coroutine work moves in.
- Break up `column/ColumnType.kt` (2,193 LOC). Move each variant into its own file under `column/types/` (`HomeColumn.kt`, `NotificationsColumn.kt`, `HashtagColumn.kt`, ...) — ~25 files, ~80–200 LOC each. Each owns its type enum entry, fetch logic, and rendering spec.
- Break up `column/ColumnTask_Refresh.kt` (1,193 LOC). After the type split, per-type refresh lives with its type. `ColumnTask_Refresh.kt` disappears.
- Rewrite `columnviewholder/` as pure Composables:
  - `ColumnViewHolder.kt` → merge into `compose/ColumnScreen.kt`.
  - `ColumnViewHolderActions.kt` → `ColumnActionsRow.kt`.
  - `ColumnViewHolderAnnouncements.kt` → fold into existing `ColumnAnnouncementsComposable.kt`.
  - `ColumnViewHolderLifecycle.kt` → replaced by ViewModel lifecycle; delete.
  - `ColumnViewHolderLoading.kt` → `ColumnLoadingIndicator.kt`.
  - `ColumnViewHolderQuickFilter.kt` → fold into existing `ColumnQuickFilterComposable.kt`.
  - `ColumnViewHolderReactions.kt` → `ColumnReactionsBar.kt`.
  - `ColumnViewHolderShow.kt` → fold into `ColumnScreen.kt`.
- Delete `compose/ColumnUiState.kt` — all 100+ fields become properties on `ColumnScreenState` or its sub-states.
- Shrink `Column.kt`. Becomes `ColumnSpec` — immutable data class describing *what* column this is. All *mutable* state goes to `ColumnScreenState`. Target: 355 → ~80 LOC.

**Ship as 4 PRs, not 1:**

1. **7a**: Introduce `ColumnScreenState` + `ColumnViewModel` as new types; `Column.kt` + `ColumnUiState.kt` still exist and are read through adapters. Green build.
2. **7b**: Migrate `ColumnType.kt` → `column/types/*.kt`. No behavior change. Green build.
3. **7c**: Migrate `ColumnTask_Refresh.kt` → per-type refresh. Manual verification per column type. Green build.
4. **7d**: Delete `ColumnUiState.kt`, delete `columnviewholder/`, shrink `Column.kt` to `ColumnSpec`. Green build.

**LOC delta.** Delete: `ColumnUiState.kt` (188), 8 files in `columnviewholder/`, most of `Column.kt`, most of `ColumnType.kt` (2,193), most of `ColumnTask_Refresh.kt` (1,193). Add the new structure. **Net ~−2,500 LOC** in the `column/` module, with what's left far better factored.

**Risks.** This is the phase that breaks things. Manual verification per column type is unavoidable: ~25 types (home, notifications, hashtag, local/federated public, list, conversation, bookmarks, favourites, search, profile × 3, instance info, directory, trends, blocks, mutes, follow requests, announcements, scheduled, ...). Each needs: open, refresh top, refresh bottom, scroll past initial fetch (triggering pagination), tap action. Streaming WebSocket events route to `ColumnViewModel.onStreamingEvent(...)`. Scroll preservation across rotation/background via `SavedStateHandle` on the VM. Performance — `distinctUntilChanged` per sub-flow to avoid over-recompose.

**Success criteria.**

- `column/` under 7,500 LOC (down from ~9,900).
- `columnviewholder/` directory doesn't exist.
- `compose/ColumnUiState.kt` doesn't exist; zero `forceRecompose` matches.
- `Column.kt` ≤120 LOC.
- 25-column-type smoke-test matrix passes.
- `./gradlew assembleFcmDebug testFcmDebugUnitTest detektAll` green.

---

## Verification Plan

**Per-PR automated:**

- `./gradlew :app:assembleFcmDebug` — FCM debug variant builds (matches shipped flavor).
- `./gradlew :app:testFcmDebugUnitTest` — unit tests pass.
- `./gradlew :app:detektAll` — static analysis clean.
- `./gradlew :app:lintFcmDebug` — run before merge but don't gate (noisy) unless team already gates on it.

**Per-phase manual spot-check:**

- **Timeline:** Home, notifications, local/federated public, a hashtag, a list. Refresh top/bottom, paginate, tap a status, fav/boost/bookmark, long-press.
- **Post:** FAB → composer. Attach image, pick emoji, reply, post.
- **Settings:** change theme, change font size, toggle prefs, restart — persistence intact. Account settings — toggle options.
- **Account switcher:** switch accounts, home column re-populates.
- **Navigation:** back / up / task-switcher across 3 screen depths.
- **Push notifications (where applicable):** test FCM → tap → correct column.

**Phase-specific extras:**

- P1: all three themes on every surface. Per-column color override.
- P2: rotate each of the 10 extracted screens; transient state survives.
- P3: upgrade-install test (current main → refactor branch; prefs survive).
- P4: cold + warm launch without crash; TTS speaks on new notification.
- P5: every `startActivity` call either removed or commented-as-external.
- P6: pre/post rich-text pixel comparison on a dozen dense statuses.
- P7: 25-column-type smoke-test matrix.

**Regression automation (nice-to-have, don't gate):**

- Paparazzi / Roborazzi snapshot test per Compose screen at Light/Dark/Mastodon (big win for P1 and P6).
- Instrumentation test that opens each column type, waits for first refresh, asserts non-empty (drops manual work in P7).

---

## Out of Scope

Explicitly **not** touched:

- API entity cleanup (`api/entity/`).
- Push notifications rewrite (`push/`, FCM wiring, notification builder). Phase 4 cleans static caches in `App1.kt` but not the payload pipeline.
- Media viewer refactor (`ActMediaViewer.kt`, `actmediaviewer/`, `view/PinchBitmapView.kt`). Phase 6 does not migrate media viewer to Coil.
- APNG module changes.
- Database schema changes (`AcctColor`, `SavedAccount`, etc.). Phase 3 migrates pref storage only.
- Compose Navigation 3 migration. Phase 5 uses classic Navigation-Compose; Nav3 is a fast-follow.
- Kotlin / AGP version bumps.
- Build variants, flavors, signing config.
- Localization churn (only strings tied to deleted UI go).
- Accessibility audit.

---

## Timeline Summary

| Phase | Scope | Duration | Risk |
|---|---|---|---|
| Quick wins | 5 × ~1-day PRs | parallel, week 1 | near-zero |
| 1 | Theming consolidation | 1.5 wk | low |
| 2 | ViewModel extraction | 1 wk | low |
| 3 | DataStore migration | 1.5 wk | medium (data loss if migration wrong) |
| 4 | AppState/App1 → Koin services | 1 wk | medium |
| 5 | Navigation-Compose | 1 wk | medium |
| 6 | Legacy spans/emoji + Coil + Anko removal | 1.5 wk | medium-high |
| 7 | Column state unification | 2 wk | high |
| **Total** | | **~9.5 wk** | |

Fits the 8–10 week budget with a week of float for Phase 6/7 surprises.

---

## Critical Files

- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/compose/StTheme.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/compose/StExtendedColors.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/compose/ColumnUiState.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/AppState.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/App1.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/Styler.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/ActMain.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/column/Column.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/column/ColumnType.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/column/ColumnTask_Refresh.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/columnviewholder/` (8 files)
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/pref/` + `pref/impl/`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/span/` (16 files)
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/view/NetworkEmojiView.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/util/NetworkEmojiInvalidator.kt`
- `/home/user/SubwayTooter/app/src/main/java/es/ariaontheplanet/quasar/util/AnkoHelper.kt`
- `/home/user/SubwayTooter/app/build.gradle.kts`
- `/home/user/SubwayTooter/app/src/main/res/values/attrs.xml`
- `/home/user/SubwayTooter/app/src/main/res/values/colors.xml`

---

## Progress Log (session of 2026-04-19 → 2026-04-21)

**34 commits on `gendev`**, branching from `66dfa1bcb`. `./gradlew :app:assembleFcmDebug :app:testFcmDebugUnitTest` — both green at checkpoint `28675a1e0`. (`detektAll` and `lintFcmDebug` fail on pre-existing issues — mostly trailing whitespace and a stale `ActGlideTest` manifest stub from 2023 — not regressions from this work.)

### Quick Wins ✅ (5 commits, `5c093f1e2`..`244a939ae`)

- Deleted 4 commented-out dead pref keys from `PrefB.kt`
- Extracted `WeakReference<ActMain>` out of `ActMain.companion` into `actmain/ActMainRegistry.kt` (placeholder until Phase 5 replaces it with `Navigator`)
- Deleted unused `view/GravitySnapHelper.kt` (217 LOC, zero callers)
- Swept TODO/FIXME markers; most are legitimate domain notes. One stale TODO fixed (`"Behavior"` → `R.string.behavior` in `AccountSettingScreen`)
- Moved `stColorScheme()` from `util/ComposeUtils.kt` into `compose/StTheme.kt` alongside `stExtendedColors()`

### Phase 1 — Theming consolidation ✅ (4 commits, `c72242f2d`..`7a275580e`)

- **1a bridgehead** — New `compose/StColorTokens.kt` — named palette tokens; `StExtendedColors.kt` references tokens instead of literal hex. New `compose/StTypography.kt` — `StExtendedTypography` data class + `LocalStExtendedTypography` + `stExtendedTypography()` reading `ActMain.timelineFont*` and `PrefF` sizes with defaults.
- **1b** — Added `MastodonStExtendedColors` variant + 6 Mastodon tokens. New `compose/UiTheme.kt` with `UiTheme` enum (Light/Dark/Mastodon) and `currentUiTheme()` resolver. Both `stColorScheme()` and `stExtendedColors()` branch on `currentUiTheme()` — they can no longer drift.
- **1c** — `LocalStExtendedTypography` now provided by both `StScreen` and `StThemedContent` via `CompositionLocalProvider`.

**What's left from Phase 1 (not attempted):** deleting custom XML color attrs + per-theme color entries; shrinking `Styler.kt`; consolidating `Column.defaultColor*` into `StThemeEx` + removing duplicate fallback helpers in `ColumnExtra1.kt`.

### Phase 2 — ViewModel extraction ✅ (10 commits, `c35afc9fc`..`1ea7ef780`)

All 10 target Activities have a matching `*ViewModel.kt` with `StateFlow<UiState>`. Pattern: `data class FooUiState`, `class FooViewModel : [Android]ViewModel()`, `init { reload() }`, `suspend fun mutate()` that updates state via `_uiState.update`. Activities read via `collectAsStateWithLifecycle()`; confirm dialogs routed at the UI boundary.

- `c35afc9fc` MutedPseudoAccountViewModel
- `1cca80111` MutedAppViewModel
- `da51201b9` FavMuteViewModel (+ public `FavMuteItem` data class)
- `9e9b28016` DrawableListViewModel (+ public `DrawableItem`)
- `f0eade9ca` MutedWordViewModel
- `0d473457d` HighlightWordListViewModel
- `9872499bc` NicknameViewModel (constructor-param `acctAscii`)
- `ce6c99027` ColumnListViewModel (promoted `MyItem`→`ColumnListItem`; owns `persistColumnsToTempFile` + `buildResult`)
- `2daabe543` KeywordFilterViewModel (~300 LOC — 12 form fields + `SaveValidation` sealed interface; API `runApiTask` stays on Activity because of its progress dialog)
- `1ea7ef780` AppSetting — pulled `customShareTarget` into existing `AppSettingViewModel`; removed dead `defaultLineSpacingExtra` / `defaultLineSpacingMultiplier` maps

### Phase 3 — DataStore migration ⏭ (skipped this session)

### Phase 4 — AppState / App1 → Koin services ✅ (7 commits, `5b4f8ccc5`..`b445d9053`)

**Koin setup** — `startKoin { androidContext(this@App1); modules(appModule, viewModelModule) }` in `App1.onCreate`. Services in `services/`, DI modules in `di/`.

- `5b4f8ccc5` Koin bootstrap + `MutedPseudoAccountViewModel` wired via `by viewModel()`
- `247e47776` all 17 VMs bound + 15 Activities migrated from `provideViewModel` to `by viewModel()` (parameterized VMs use `parametersOf`); `viewModelFactory` + `provideViewModel` helpers deleted; `ViewModelUtils.kt` trimmed to just `collectOnLifeCycle`
- `6a3749c9d` **AppBusyState** extracted — 3 HashSets + 9 busy-flag methods into `services/AppBusyState.kt`; AppState forwards (all 18 call sites unchanged)
- `296dbb652` **ColumnRepository** extracted — `_columnList`, `_currentAccount: StateFlow`, `columnList`/`columnCount`/`column(i)`/`columnIndex`/`editColumnList`/`setCurrentAccount`; AppState forwards
- `6681e1e7e` **TtsService** extracted — ~290 LOC of TTS Handler/Runnable queue + ringtone side effects. `services/TtsModels.kt` for `DedupMode`/`DedupItem`. Updated 3 callers' imports. AppState forwards; `AppState.kt` shrunk 636 → 252 LOC.
- `dfb6599a0` **OkHttp singles** — 3 clients (`api`, `cached`, `media`) + shared `Cache` bound with `named()` qualifiers; `services/OkHttpClients.kt` holds the `prepareOkHttp` factory. `App1` properties became Koin-backed getters.
- `8ad837c16` **CustomEmojiCache / CustomEmojiLister** — Koin singles; `App1` fields became read-only `val` forwarders via `GlobalContext.get()`.
- `b445d9053` **AppState itself** — `single { AppState(androidContext(), get()) }`. Deleted `appStateX` + `@SuppressLint("StaticFieldLeak")`. `App1.prepare()` split into idempotent `runFirstTimeInit()` gated by `@Volatile var prepared`. `App1.sound()` now goes directly through Koin to `TtsService`. `App1.kt` down to 307 LOC.

**`AppState.kt`: 636 → 252 LOC (−60%). `App1.kt`: 361 → 307 LOC.**

**What's left from Phase 4 (cleanup-only, not blocking):** migrating the 18+30 call sites of `App1.custom_emoji_cache` / `ok_http_client*` / `App1.getAppState()` to direct Koin injection. Current forwarders work; no functional difference.

### Phase 5 — Navigation-Compose ⚙ (in progress — 6 commits, `c35effb90`..`28675a1e0`)

- `c35effb90` Committed pre-existing gradle mods (`lifecycle-runtime*` deps + debug keystore) that had been sitting unstaged.
- `a61617e74` **Bridgehead** — `navigation-compose 2.8.5` dep; `nav/Routes.kt` sealed interface with `@Serializable` data objects (AppSettings, ColumnList, ExitReasons, OssLicense, About) + parameterized (AccountSettings, LanguageFilter); `nav/Navigator.kt` interface + `NavigatorImpl` emitting `NavEvent` SharedFlow; Koin bound `single { NavigatorImpl() }` + `single<Navigator> { get<NavigatorImpl>() }`. No call sites migrated.
- `56f8f9086` **ExitReasons pilot** — `nav/AppNavHost.kt` (NavController binds to NavigatorImpl via `DisposableEffect`; collects events via `LaunchedEffect`; `StThemedContent` wrapper); new `RootActivity` hosts it; new `ui/exitReasons/ExitReasonsScreen.kt`. `ActExitReasons.kt` + manifest entry deleted.
- `54778de63` **Intent-based start-route + OssLicense** — added `Route.Companion.keyOf/fromKey/EXTRA_START_KEY` + `RootActivity.createIntent(ctx, route)`. New `ui/ossLicense/OssLicenseScreen.kt` (takes `onClose`, uses `LocalActivity.current` for `openBrowser` side effects). `ActOSSLicense.kt` (212 LOC) + manifest entry deleted.
- `5f104659f` **DrawableList + default RESULT_OK** — added 7 more parameterless routes (Drawable/FavMute/HighlightWordList/MutedApp/MutedPseudoAccount/MutedWord/…). `RootActivity` sets `RESULT_OK` by default so settings screens retain refresh-on-back semantics.
- `efc6d98b9` **4 mute/fav list screens** — `MutedPseudoAccountScreen`, `MutedAppScreen`, `FavMuteScreen`, `MutedWordScreen`. Shared `MuteItemRow` extracted to `ui/common/MuteItemRow.kt` before the old Activities were deleted. SideMenuAdapter callers flipped to `RootActivity.createIntent`. 4 Activities + 4 manifest entries gone.
- `28675a1e0` **HighlightWordList** — ringtone helpers (`lastRingtone`, `stopLastHighlightRingtone`, `playHighlightSound`) lifted to `ui/highlightWord/HighlightWordSound.kt` so the edit Activity can keep using them. `HighlightWordListScreen.kt` uses `rememberLauncherForActivityResult` for the edit-then-reload flow + `DisposableEffect` for ringtone cleanup. Activity and manifest entry deleted.

**Phase 5 status:** 8 screens route-hosted (ExitReasons, OssLicense, DrawableList, MutedPseudoAccount, MutedApp, FavMute, MutedWord, HighlightWordList). 8 Activity classes + 8 manifest entries gone.

**What's left from Phase 5:**
- **Screens still Activity-based** — ActAbout, ActNickname, ActColumnCustomize, ActColumnList, ActKeywordFilter, ActAppSetting, ActAccountSetting, ActAlert, ActCallback, ActMediaViewer, ActPost, ActText, ActPushMessageList, LanguageFilterActivity, ActHighlightWordEdit, and **ActMain** (the big one)
- **Result-returning screens** — ActAbout / ActNickname / ActHighlightWordEdit / ActAppSetting etc. return Intent payloads; `RootActivity` default `RESULT_OK` handles the trivial "refresh on back" case but richer payloads (e.g., `EXTRA_SEARCH` from ActAbout) need a proper mechanism. Options: `Navigator.setResultAndFinish(Intent)`, or keep these as standalone Activities until Phase 5 is otherwise done.
- **Parameterized routes not yet hooked up** — `Route.AccountSettings(accountDbId)`, `Route.LanguageFilter(columnIndex)`. `RootActivity.createIntent` only handles parameterless routes via key-lookup; parameterized routes need either kotlinx.serialization of the full `Route` into an Intent extra, or direct `Navigator.navigate()` from inside an already-running RootActivity.
- **`ActMainRegistry` / `WeakReference<ActMain>`** — still alive. Goes away once `Navigator` replaces the two `ActMain.refActMain?.get()?.X()` call sites in `ActPost.kt`.

### Phases 6 & 7 — not attempted this session

Phase 6 (delete legacy span / emoji-invalidator + migrate to Coil) and Phase 7 (Column state unification — Column + ColumnViewHolder + ColumnUiState → single ColumnViewModel) are untouched. Phase 7 in particular gets easier the more of Phase 5 lands (less `ActMain` coupling).

### Files created / deleted this session

**New (services + DI):** `di/AppModule.kt`, `di/ViewModelModule.kt`, `services/AppBusyState.kt`, `services/ColumnRepository.kt`, `services/TtsService.kt`, `services/TtsModels.kt`, `services/OkHttpClients.kt`.

**New (nav + routes):** `nav/Routes.kt`, `nav/Navigator.kt`, `nav/AppNavHost.kt`, `RootActivity.kt`.

**New (ViewModels, Phase 2):** 10 files across `actmutedpseudoaccount/`, `actmutedapp/`, `actfavmute/`, `actdrawablelist/`, `actmutedword/`, `acthighlightwordlist/`, `actnickname/`, `actcolumnlist/`, `actkeywordfilter/`.

**New (Compose screens, Phase 5):** `ui/exitReasons/ExitReasonsScreen.kt`, `ui/ossLicense/OssLicenseScreen.kt`, `actdrawablelist/DrawableListScreen.kt`, `actmutedpseudoaccount/MutedPseudoAccountScreen.kt`, `actmutedapp/MutedAppScreen.kt`, `actfavmute/FavMuteScreen.kt`, `actmutedword/MutedWordScreen.kt`, `ui/highlightWord/HighlightWordListScreen.kt`, `ui/highlightWord/HighlightWordSound.kt`, `ui/common/MuteItemRow.kt`.

**New (theming, Phase 1):** `compose/StColorTokens.kt`, `compose/StTypography.kt`, `compose/UiTheme.kt`.

**New (other):** `actmain/ActMainRegistry.kt`.

**Deleted:** `view/GravitySnapHelper.kt`, `ActExitReasons.kt`, `ui/ossLicense/ActOSSLicense.kt`, `ActDrawableList.kt`, `ActMutedPseudoAccount.kt`, `ActMutedApp.kt`, `ActFavMute.kt`, `ActMutedWord.kt`, `ActHighlightWordList.kt` (9 Activities total).

### Recommended resumption order

1. Finish Phase 5: tackle `ActMain` last (it's the app's main entry + hosts `FixedColumns` + holds `WeakReference<ActMain>` consumers). Before it, knock off remaining settings screens (ActAbout, ActNickname, ActColumnCustomize, ActAppSetting, ActAccountSetting, ActText, ActPushMessageList, ActKeywordFilter, ActColumnList, LanguageFilterActivity).
2. Then Phase 3 (DataStore) or Phase 6 (spans/Coil) — both are largely orthogonal to the rest.
3. Phase 7 (Column state) last, as planned.

### What's still on the working tree

No unstaged changes at checkpoint. Session state is fully committed under `28675a1e0`.

---

## Progress Log (continuation — 2026-04-21, picking up from `28675a1e0`)

**44 commits total** on `gendev` now (was 34 at last checkpoint). `./gradlew :app:assembleFcmDebug :app:testFcmDebugUnitTest` both green at HEAD `d818103f2`.

### Phase 5 — Navigation-Compose (continued, 10 more commits)

- **`fca727cf8` About** — new `ui/about/AboutScreen.kt`. Result payload (`EXTRA_ABOUT_SEARCH`, lifted to a file-level const) sent via `activity.setResult(...)` + `finish()`. `Translator` data class + the 31-entry list moved into the screen file. `ActMain.arAbout` just imports the constant from its new home.
- **`181614fe9` Route JSON serialization** — `sealed interface Route` marked `@Serializable`; `Route.keyOf`/`fromKey`/`EXTRA_START_KEY` replaced with `Route.encode(route)` / `Route.decode(json)` + `EXTRA_ROUTE_JSON` via kotlinx.serialization polymorphic serialization. `RootActivity.createIntent` now handles any `Route`, including parameterized ones.
- **`8d9e2fc4c` Nickname (first parameterized)** — new `Route.Nickname(acctAscii, acctPretty, showNotificationSound)`. `ui/nickname/NicknameScreen.kt` builds the VM via `viewModel(key=acctAscii, factory=viewModelFactory { initializer { NicknameViewModel(acctAscii) } })` — no Koin `parametersOf` needed in composables.
- **`a28ee4268` Alert + ActGlideTest purge** — `Route.Alert(title, message)`. `Context.intentActAlert(tag, message, title)` helper preserved in `ui/alert/AlertScreen.kt` so notification `PendingIntent`s keep their `app://error/${tag}` distinctness. Stale `ActGlideTest` manifest stub from 2023 dropped — this was breaking `lintFcmDebug`.
- **`a4f1a0e4d` LanguageFilter** — full 400-LOC screen migrated with `BackHandler` for list-changed confirm, `rememberLauncherForActivityResult` for import picker, `FileProvider`-based export kept. `openLanguageFilterActivity` / `decodeLanguageFilterResult` helpers lifted out of the Activity into top-level functions; 2 callers swap imports. `VM.restoreOrInitialize(columnIndex, savedInstanceState)` takes `Int` directly, no more `Intent`.
- **`da55ca602` PushMessageList** — `ui/pushMessageList/PushMessageListScreen.kt` with `rememberLauncherForActivityResult(RequestPermission)` for `POST_NOTIFICATIONS` instead of the old `permissionSpecNotification.requester` pattern. `tintIconMap` + `acctMap` live via `remember { }`. Glide via `AndroidView(ImageView)` kept.
- **`637eec1e4` AccountSetting** — trivial wrapper, just forwards to the existing `AccountSettingScreen` composable via new `actaccountsetting/AccountSettingRoute.kt`. Dead `RESULT_INPUT_ACCESS_TOKEN` branch (declared but never emitted) stripped from `ActMain.arAccountSetting`.
- **`701eefad0` HighlightWordEdit** — pairs with the earlier HighlightWordList migration. `Route.HighlightWordEdit(itemId, initialText)`. `BackHandler` → discard-changes dialog. `rememberLauncherForActivityResult` for ringtone picker. Minor regression: unsaved edits don't survive rotation (the old `STATE_ITEM` `savedInstanceState` persistence is gone). 3 callers across `HighlightWordListScreen` and `ActText.highlight()` updated.
- **`40f432a9b` delete ActColumnCustomize** (555 LOC dead) — Activity had zero callers. `createIntent` referenced only by itself. Deleted outright with manifest entry.
- **`d818103f2` delete ActColumnList + dead AppState column-list IO** (500 LOC dead) — `ActColumnList`, its Phase-2 `ColumnListViewModel` + `ColumnListItem`, the Koin binding, the manifest entry — all dead since the fixed-columns refactor removed the column-reorder feature. Also dropped the now-orphan `AppState.saveColumnList(Context, String, JsonArray)` / `loadColumnList(Context, String)` companion funcs and the instance `loadColumnList()` that only served the deleted VM. Cleared out 8 unused imports in `AppState.kt`.

### Phase 5 totals now

**17 Activity classes gone** (15 migrated + 2 dead-deleted) since the start of Phase 5. `AndroidManifest.xml` down to 8 `<activity>` entries. Remaining Activity source files:

| File | Notes |
|---|---|
| `ActMain.kt` (686 LOC) | App launcher, still holds `WeakReference<ActMain>` via `actmain/ActMainRegistry.kt`. The big one — migrate last. |
| `ActPost.kt` (~820 LOC) | Compose composer. Size + widget complexity (attachments, visibility picker, emoji picker, multi-window post) makes it a big lift. |
| `ActAppSetting.kt` (759 LOC) | Already has a partial VM; body is heavy file-I/O (export/import/font/log), which is framework-coupled. |
| `ActKeywordFilter.kt` (642 LOC) | Has Phase-2 VM (`KeywordFilterViewModel`). `runApiTask` progress dialog is activity-coupled but otherwise ready. |
| `ActText.kt` (653 LOC) | Long-text reader + incremental regex search + highlighted selection. Self-contained but substantial state. |
| `ActMediaViewer.kt` | Out-of-scope per the master plan (dedicated media-viewer refactor). |
| `ActCallback.kt` (272 LOC) | External-intent dispatcher — `ACTION_SEND`/`ACTION_VIEW`, `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`. Not a screen; stays as an Activity. |
| `ActPostScreen.kt` | Lives next to ActPost — extracted composable, not an Activity. |

### Cleanups noted this session

- **Lint-blocker removed**: `ActGlideTest` manifest stub (dead since 2023) was the main reason `lintFcmDebug` failed; that's gone.
- **~1,100 LOC of verified-dead code deleted**: `ActColumnCustomize` (555 LOC) + `ActColumnList` stack (500 LOC) + `AppState.loadColumnList()` / `FILE_COLUMN_LIST` / 8 unused imports.
- **`AppState.kt` continuing to shrink**: was 636 at session start, now **179 LOC** (−72%). The `companion object` is a single `LogCategory` declaration; the busy-flags/column-list/TTS state all live in Koin services; `loadColumnList()` and the file-IO companions gone.

### Recommended resumption order (updated)

1. **ActKeywordFilter** — the cleanest of the remaining big migrations since its VM (`KeywordFilterViewModel`) already holds all form state. Activity body is basically UI + a few `runApiTask` glue methods.
2. **ActAppSetting** — large, but lots of it is framework-coupled (file I/O, font picker, log export). The partial-VM extraction already landed; the rest can stay Activity-hosted or be bundled in later.
3. **ActText** — substantial reader; migration viable but big.
4. **ActPost** — post composer. Largest unmigrated Activity; handle after ActMain.
5. **ActMain** — last. Once it's inside `RootActivity` as a route, `ActMainRegistry` + `WeakReference<ActMain>` can be deleted.
6. **ActMediaViewer / ActCallback** — leave as Activities per plan.

At current session HEAD:
- 15 migrated screens + 2 dead-Activity deletions = 17 Activity classes gone (out of the original 25).
- 8 Activity source files remain; 2 of those (`ActCallback`, `ActMediaViewer`) are intentionally staying as Activities, and `ActPostScreen` isn't an Activity.
- The route-serialization, result-propagation (via `activity.setResult`), `BackHandler`, `rememberLauncherForActivityResult`, and shared-helper extraction patterns are all battle-tested.

---

## Phase 6 — Partial (2 commits so far, all dead-code kills)

**Session HEAD now `50a466046`. APK + tests still green.**

### Landed

- **`4e3da620a`** — deleted `util/AnkoHelper.kt` (57 LOC; 7 extensions, zero references).
- **`50a466046`** — deleted `view/GestureInterceptor.kt` (25 LOC; zero references).

### Not yet landed (the real Phase 6 work)

Phase 6 doesn't slice into quick wins beyond the dead-code kills. Three sizable pieces remain:

**6b — Coil migration** (~1 day)
- 8 `Glide.with(…)` call sites across 5 files. Each has distinct requirements:
  - `util/LoadIcon.kt` — bitmap target in a suspend function, `.override(size)` + cancellation
  - `ui/pushMessageList/PushMessageListScreen.kt` — inline `GlideImage` composable for notification icons
  - `dialog/DlgAttachmentRearrange.kt` — load + placeholder + error + fallback
  - `dialog/DlgEmojiDetail.kt` — SVG via `.as(PictureDrawable::class.java)` for the custom-emoji preview
  - `compose/ComposeNetworkImage.kt` — the main image loader: animated-URL vs static-URL toggle for APNG/GIF, round-corner handling, `RequestListener` callbacks
- Needs `coil-compose` + `coil-svg` + `coil-gif` deps plus a Koin-provided `ImageLoader` configured with custom decoders and the existing OkHttp client.
- Partial migration forces two caches in memory — the master plan explicitly warns against shipping an intermediate.

**6c — Rich-text conversion layer** (~1 day)
- 13 `SpannableTextView` call sites in `StatusComposables`, `AccountComposables`, `ColumnContentHeaders`, `DlgListMember`.
- Source text is produced by `HTMLDecoder` / `EmojiDecoder` / the MFM parser as `Spannable` with a mix of 15 span types:
  - Inline-friendly (map to `SpanStyle`): `HighlightSpan`, `InlineCodeSpan`, `MyClickableSpan`
  - Inline-but-replacement (map to `InlineTextContent`): `NetworkEmojiSpan`, `EmojiImageSpan`, `SvgEmojiSpan`
  - Animated inline (need `rememberInfiniteTransition`): `AnimatableSpan`, `MisskeyBigSpan`, `MisskeyMotionSpan`
  - **Block-level** (don't fit inside a single `Text(AnnotatedString)`): `BlockQuoteSpan`, `BlockCodeSpan`, `OrderedListItemSpan`, `UnorderedListItemSpan`, `DdSpan`, `HrSpan` — these require decomposing the decoded text into a list of typed `TextBlock` elements rendered as separate composables.
- The pattern: `fun spannableToRichContent(s: Spannable): RichContent` producing a `List<TextBlock>` where each block holds an `AnnotatedString` + an `inlineContent: Map<String, InlineTextContent>`. Then `@Composable fun RichText(content: RichContent)` walks the blocks.
- Pilot migration target: an account bio (header) or status content-warning — places where block spans are rare.

**6d — Cleanup after 6c** (~1 day)
- Migrate remaining SpannableTextView call sites to `RichText`.
- Delete `SpannableTextView` wrapper from `compose/ComposeViewBridges.kt`.
- Delete `util/NetworkEmojiInvalidator.kt` (only user is the wrapper).
- Delete `view/NetworkEmojiView.kt` iff `DlgEmojiDetail` (its last caller) also gets a Compose equivalent — otherwise leave for later.
- Delete `view/MyLinkMovementMethod.kt` iff `TootAccount.kt`'s single reference gets rewritten.
- Delete span classes that no longer have references (likely most of them).

### Current dead-code landscape

Beyond the spans and emoji invalidator, the only live stragglers in `view/` are:
- `MyLinkMovementMethod.kt` — 2 refs (ComposeViewBridges, TootAccount)
- `PinchBitmapView.kt` — 1 ref (MediaViewerScreen — out of scope per master plan)

`NetworkEmojiView.kt` was deleted this pass — see the new session log below.

---

## Progress Log (continuation — 2026-04-21, picking up from `6a6ba065f`)

**64 commits total on `gendev`** at HEAD `23afdf957`. `./gradlew :app:assembleFcmDebug :app:testFcmDebugUnitTest` both green.

This pass was a dead-code sweep rather than a migration — no new screens, no architectural changes. **~870 LOC removed** across 11 commits, all low-risk reductions driven by grep-audits of call sites.

### Deletions

| Commit | What | LOC |
|---|---|---|
| `8c6688601` | Styler.kt: fixHorizontalPadding/Margin, appendMisskeyReaction, generateLayoutParamsEx, Context.setSwitchColor, getVisibilityCaption (stub); 3 callers migrated to `getVisibilityString` | 213 |
| `bb09847a8` | `ColumnUiState.columnStatus: CharSequence` → `String`; dropped SpannableStringBuilder icon that `.toString()` was throwing away; removed `appendColorShadeIcon` | 39 |
| `8ddb9e42a` | `dialog/DlgEmojiDetail.kt` (0 callers) + `view/NetworkEmojiView.kt` (was only used by that dialog) | 354 |
| `65ce5a6bb` | ActMainStyle.kt: loadColumnMin (dead), justifyWindowContentPortrait/showFooterColor/closePopup (no-op stubs); AnnotatedString.Builder.isEmpty/isNotEmpty (inlined) | 41 |
| `6047895e4` | `actpost/CompletionHelper.kt` — stub with 3 no-op methods + 8 call sites | 56 |
| `37f79316f` | `AppState.encodeColumnList()` — no callers since fixed-columns refactor killed persistence | 16 |
| `ac37e5280` | `util/TootColorConfig.kt` — always set to 0, so per-visibility bg-color branch in StatusComposables always collapsed; also the legacy `/* views */` comment block in ActMain | 48 |
| `9d358ab14` | `showPoll`/`showContentWarningEnabled` no-op stubs in actpost/ + 8 call sites | 18 |
| `530e6cb82` | `addColumnViewHolder`/`removeColumnViewHolder` no-op stubs + `hasMultipleViewHolder()` (always false) + 5 call sites + the dead retry branch in `procRestoreScrollPosition` | 27 |
| `ece90be9e` | `extraInvalidatorList` + `emojiQueryInvalidatorList` in ColumnViewHolder — declared but nothing ever added; the register(null) loops were therefore no-ops | 28 |
| `23afdf957` | Dead pref keys: `ipMediaBackground`, `spQuickTootVisibility`, and 4 already-commented entries | 12 |

### Patterns that surfaced

- **"Returns CharSequence, rendered via .toString()"** — the span information gets thrown away. Converting the field to `String` and dropping the Spannable-building code is safe and self-documenting.
- **"No-op stub with N callers"** — common residue of incremental migrations. Always worth deleting: the callers don't need anything else to change.
- **"Field set but never read"** / **"field read but never set"** — shows up across `columnUiState` members (announcementsExpanded/announcementsCaption are candidates for the next pass).
- **"List declared but nothing added to it"** — the consumer loops iterate nothing, so they're self-sustaining dead code (easy to miss in normal grep).

### Styler.kt — now 284 LOC

Was 497 at session start; `getVisibilityIconId`/`getVisibilityString`/`setFollowIcon`/`defaultColorIcon`/`calcIconRound`/`enableEdgeToEdgeEx`/`appendColorShadeIcon` (now gone) are what remain. The Phase 1 plan target of ~150 LOC is achievable after Phase 6 deletes the last `EmojiImageSpan` callers.

### AppState.kt — now 147 LOC

(172 at start of this pass.) Companion is still just `LogCategory`.

### Recommended next steps (updated)

**Easy wins, ~30 min each**:
- ~~Announcement "expanded" state~~ — done. Click-to-expand wired via local `remember`; `announcementsExpanded` + `announcementsCaption` fields deleted.
- Animated-emoji rendering: `NetworkEmojiSpan` / `MisskeyBigSpan` / `MisskeyMotionSpan` currently render as static images in RichText. Phase 6c-3 is the `rememberInfiniteTransition` pass, gated on LazyList visibility.
- Wire the `hashtag-menu` context into RichText's link handler (ActMainActions line 65 walks sibling spans to collect hashtags — currently gets an empty list from the Compose path).
- Reflection accessors in SpannableToRichContent.kt: `urlOrNull`, `resIdOrNull`, `assetPathOrNull`, `orderIndexOrNull`. Turn the backing span fields `public` (or add public getters) and drop the reflection. Safer + faster.

**Medium**:
- **Phase 6c-2 follow-up**: nested block decomposition. A blockquote containing a list currently flattens the list into the quote's paragraph. Recursive descent in the block loop would fix it; most Mastodon HTML doesn't hit this.
- **ActText migration** — least bad remaining Phase-5 screen (self-contained search-in-long-text; already uses Compose + StScreen).
- **Span classes pruning**: inline-span classes (`HighlightSpan`, `InlineCodeSpan`) and block-span classes (`BlockCodeSpan`, `BlockQuoteSpan`, `HrSpan`, `OrderedListItemSpan`, `UnorderedListItemSpan`, `DdSpan`) are now only consumed by the `toRichContent` converter, which reads their ranges and public fields. Their `draw*` implementations are still invoked by `ActMainAutoCW.checkAutoCW`'s off-screen measurement pass, so the classes can't be deleted wholesale — but the draw methods themselves are effectively dead pixels.

**Hard / high-risk**:
- **ActMain migration**. Phase 5 cleanup ties `ActMainRegistry.WeakReference<ActMain>` deletion to this.
- **Phase 7 (Column state)**.

---

## Progress Log (continuation — 2026-04-21, picking up from `a338d12aa`)

**19 more commits** this pass, HEAD at `b971dcc73`. `./gradlew :app:assembleFcmDebug :app:testFcmDebugUnitTest` both green.

This pass completed the announcement wire-up + **finished Phase 6c/6d**: every `SpannableTextView` call site migrated to `RichText`, the wrapper + related view-infra deleted. Net LOC change: **−240 LOC** across the affected files, with meaningful behaviour fixes (hidden announcement content now renders, CharSequence→toString drops reversed).

### Announcement box: click-to-expand wired

- `announcementsExpanded` (`ColumnUiState`) was never written, so the `if (expanded) { ... }` block hiding the announcement body/reactions was unreachable. Replaced with a local `var expanded by remember { mutableStateOf(true) }` toggled by clicking the caption row; added an ExpandLess/ExpandMore chevron.
- `announcementsCaption` was set but never read (composable calls `stringResource(R.string.announcements)` directly) — dropped.
- Paging-arrow `alpha = 0.3f` ternaries were dead (arrows only render when paging is enabled) — removed.

### Spannable → RichText migration (completes Phase 6c)

Six more `SpannableTextView` sites converted before the wrapper came down:

| Location | Notes |
|---|---|
| `ColumnAnnouncementsComposable` content | Previously `announcementContent.toString()` stripped all spans |
| Announcement reaction Button labels | Same |
| `ColumnSearchBarComposable` emoji-query Button labels | Same |
| `StatusComposables.BoostHeader` boost text | SpannedString of "X boosted" w/ display-name emoji |
| `StatusComposables.ReplyHeader` reply text | Same shape |
| `StatusComposables` status display-name | Bold + timeline font size |
| `StatusComposables` mentions row | Clickable mentions — needed link handler |
| `StatusComposables.ContentWarningRow` | Signature changed: `handler: Handler` → `onLinkClick: ((String) -> Unit)?` |
| `StatusComposables` main body | **Last call site — triggered the wrapper deletion** |
| `ColumnContentHeaders.ProfileHeader` display-name | Bold style |
| `ColumnContentHeaders.ProfileHeader` bio/note | Clickable mentions/URLs |
| `DlgListMember` target-user display name | No links |

Link handling: `toRichContent` gained an `onLinkClick: ((String) -> Unit)?` parameter. When supplied, `MyClickableSpan` ranges become `LinkAnnotation.Clickable` with a `LinkInteractionListener` — Compose's built-in link machinery dispatches taps. `StatusComposables.rememberStatusLinkClickHandler` wraps `openCustomTab` with the column context. One known feature gap vs. the old View handler: hashtag-menu aggregation (walking surrounding spans) gets an empty list.

### Phase 6c-2 — block-span decomposition

`SpannableToRichContent.kt` rewritten (136 → 247 LOC):

- Scans for `BlockQuoteSpan`, `BlockCodeSpan`, `HrSpan`, `OrderedListItemSpan`, `UnorderedListItemSpan`, `DdSpan`.
- Greedily takes non-overlapping block ranges (outer wins on ties); inner overlaps flatten into the outer paragraph — rare in real HTML.
- Splits text at block boundaries, emits the matching `RichBlock` variant, clamps inline-span ranges to each paragraph segment.
- `OrderedListItemSpan`'s `order` string is read via reflection (the field is private); numbering falls back to 0 on failure.

Inline spans (HighlightSpan, InlineCodeSpan, NetworkEmojiSpan, EmojiImageSpan, SvgEmojiSpan, MyClickableSpan) continue to render as before inside each paragraph.

### Deletions

| Path | LOC |
|---|---|
| `compose/ComposeViewBridges.SpannableTextView` + `PreviewSpannableTextView` | ~75 |
| `util/NetworkEmojiInvalidator.kt` | 103 |
| `view/MyLinkMovementMethod.kt` | 66 |
| `api/entity/TootAccount.setAccountExtra` (zero callers) | 91 |

That's **−335 LOC** from the deletions alone; offset by the decomposer rewrite (+111).

### Remaining SpannableTextView call sites: 0

Grep confirmation: `grep -rn "\bSpannableTextView\b"` returns nothing in `app/src/main/java`.

### `view/` directory — now 1 file

Only `PinchBitmapView.kt` remains (used by the media viewer, out of scope).

### Known visual differences to watch for

The RichText composable uses Compose `Text` with `AnnotatedString` + `InlineTextContent`. Relative to the `AppCompatTextView + Spannable` path it replaces:

- Line-breaking: Compose's BreakIterator may wrap differently. Noticeable on CJK body text.
- Link hit targets: slightly tighter (no 1-line padding tolerance).
- Inline emoji baseline: `PlaceholderVerticalAlign.Center` vs the old `EmojiImageRect`-driven descent. Should be close but could shift a pixel or two.
- Animated emoji: currently renders as static (6c-3).
- Hashtag menu: no surrounding-span scan, so the tag list offered in the menu is empty.

All behaviours that need to remain working on device: timeline status rendering (boosts, replies, CW, mentions, body, display name), profile headers, announcement box, DlgListMember target display.

