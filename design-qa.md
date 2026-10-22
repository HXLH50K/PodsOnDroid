# Historical HyperPods UI evidence

**This is an English summary of upstream evidence, NOT P.O.D.S. verification.**

The reference source is anchored at `a44bc10e387f527062b9ba2b8f90b154b2e61248`; that commit is provenance, not an ancestor of the independent P.O.D.S. branch. The original report describes a HyperPods Compose/Miuix redesign on **2026-10-19**, followed by a **build 23** regression addendum. Its counts describe those historical runs, not the current application ID, API 102 migration, Liquid Glass design, R01, or physical AirPods 5 acceptance.

Original screenshots/reference images lived in a separate upstream analysis workspace and are not delivered here. Filenames below identify historical evidence only; there are deliberately no broken sibling-workspace links. The historical report and Chinese upstream documents remain in the reference project and external backup, not recreated as current coding documentation.

## 1. Historical test setup

- Native Android Jetpack Compose with existing Miuix controls, not a web prototype.
- Selected reference: light theme with four noise modes in one row; original reference **853 x 1844 px**.
- App-content viewport **390 x 844 dp**, Android **35** emulator, density **480 dpi / 3x**.
- Compose-root screenshots **1170 x 2532 px**, excluding status/navigation bars.
- Reference and captured content normalized to **780 x 1688 px / 2x** for side-by-side comparison; device frames/system bars were not app content.
- Synthetic debug-preview fixture: Simplified Chinese, light theme, AirPods Pro 2 USB-C **A3049**, left **92%**, right **87%**, case **73%**, ANC selected, personalized volume on, conversation awareness off, wear detection on. These were **not the user's physical-device readings**.

Historical images included `reference.png`, `light.png`, `light-comparison.png`, and `light-controls-comparison.png`; these are evidence names, not claimed current artifacts.

## 2. Review rounds

### Initial round: blocked

Two reported P2 findings:

1. Row/group spacing pushed the More Controls description outside the standard viewport (`light-before-comparison.png`). Adjustments: battery/product area **104 dp**, setting rows minimum **64 dp**, vertical padding **10 dp**, mode icons **24 dp**; full summaries and touch areas **>=48 dp** retained.
2. Light-theme summary contrast on pale gray was slightly below **4.5:1**. Color changed from `#687380` to `#66717E`; automated light/dark contrast assertions were added.

### Visual re-review

The report recorded:

- Equal-width four-mode single row, separate three-part battery, cards/dividers/switches/two bottom tabs, and visible More Controls summary.
- Platform fonts/Miuix text: title **30 sp**, settings heading **16 sp**, summary **12 sp**. No text baked into images or fixed summary truncation.
- Page margin **16 dp**, card corner **24 dp**, horizontal internal dividers, shared secondary-page spacing, max large-screen content width **680 dp**.
- Pale-gray background, white cards, selected blue `#006BE6` retained for readability rather than copying a paler generated reference; separate dark surfaces.
- Existing per-model left/right/case assets preserved aspect/alpha, with a shared home/island registry. Unknown models used a generic icon rather than Pro 2 impersonation.
- Distinct off/ANC/transparency/adaptive icons, consistent long-press panel and home controls, existing navigation icons.
- Real settings, read/write feedback, unknown/cache states, and gesture/sleep features retained under More Controls; author/project/donation attribution unchanged.

### Historical final visual result: passed

- After capability-summary corrections, **74** complete UI cases and **6** additional focused layout cases passed.
- An emulator navigation-inset change after switching from tablet back to phone was detected. The capture script refused to stretch a wrong-height image; after restoring/checking insets, **2** screenshot cases recaptured true **1170 x 2532 px** content.
- The two comparison images were visually rechecked; the report recorded no remaining actionable P0/P1/P2.
- `overview.png` only assembled/proportionally reduced native captures; it did not redraw app content.

This historical “passed” applies only to that upstream UI review.

## 3. Historical build and interaction results

Relevant imported files:

- [PodDetailPage](app/src/main/java/moe/chenxy/hyperpods/ui/PodDetailPage.kt)
- [PodsDashboard](app/src/main/java/moe/chenxy/hyperpods/ui/components/PodsDashboard.kt)
- [Theme](app/src/main/java/moe/chenxy/hyperpods/ui/Theme.kt)
- [AboutPage](app/src/main/java/moe/chenxy/hyperpods/ui/AboutPage.kt)
- [PodsIslandData](app/src/main/java/moe/chenxy/hyperpods/utils/PodsIslandData.kt)
- [MiBluetoothToastHook](app/src/main/java/moe/chenxy/hyperpods/hook/MiBluetoothToastHook.kt)

The report recorded successful Debug, Release, and AndroidTest builds, **282 JVM tests**, and Android Lint **0 errors / 117 warnings**. Warnings were not claimed eliminated.

Historical Gradle tasks were `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:assembleRelease`. Instrumentation installed test APKs on a dedicated emulator using the old `moe.chenxy.hyperpods.test` runner identity. That identity is **not a current P.O.D.S. installation command**, and emulator procedures must not be redirected to a real phone as equivalent acceptance.

The **74** UI cases covered normal and **0x** animation scales, rapid tab changes/cancelled transitions, back interruption/cancellation, independent page scroll restoration, dialog cancellation/drafts, whole-row switch interaction, theme contrast, and screenshot capture. Navigation used short interruptible slides/fades. Whole-row switches exposed a single Switch semantic; localized confirm/cancel descriptions were present. Cancelling rename did not write to the device.

Captured actual Compose screens included light/dark, dual-ear/three-part battery, unknown state, AirPods 4/5, More Controls, gesture test, device info, diagnostics, and about. Historical filenames: `dark.png`, `two-ears.png`, `unknown.png`, `airpods-4.png`, `airpods-5.png`, `more.png`, `gesture.png`, `device.png`, `diagnostics.png`, `about.png`.

### Compact and large layouts

- **320 dp / 1.5x font:** **21** additional cases passed, including toggles, rename cancellation, back/scroll restoration under normal/disabled animation. Text wrapped, four modes remained one row, bottom tabs stayed visible. Captures `compact-light.png`, `compact-more.png`, `compact-unknown.png`, `compact-gesture.png`: **960 x 2558 px**, app content approximately **320 x 852.7 dp**.
- **600 dp:** **2** capture cases passed. Three battery columns, single-row modes, and long device names showed no horizontal overflow. `tablet-light.png` and `tablet-device.png`: **1800 x 2491 px**, content approximately **600 x 830.3 dp**.
- Different emulator cutout/safe-area heights meant compact/tablet captures were not pixel-for-pixel comparisons against the standard viewport.
- For devices lacking gesture/sleep detection, More Controls summary was changed to the equivalent of **Press controls · More settings**, avoiding claims of unavailable features while retaining capability-gated navigation.

The P.O.D.S. accessibility target is independent: small/large-font mode controls may reflow if necessary; the historical one-row layout is not a mandate to sacrifice usability.

## 4. Historical build 23 addendum

Four logic fixes were recorded after the redesign:

1. A pending-refresh in-case battery no longer triggered single-ear control recovery.
2. Conversation-volume restoration was bound to the current headphone output route.
3. Notification titles consistently used the alias.
4. Delayed UUID callbacks followed the current service instance.

The first two were reportedly reproduced by failing regression tests before being fixed. Volume tests also covered output changes arriving before route callbacks.

Home/More Controls moved stable groups into separate LazyColumn items; About split app/project/diagnostic groups. Diagnostics and motion tests retained whole-component lifecycles to avoid repeated collection on row recycling. Existing interruptible transitions, rapid reversal, disabled animations, per-page scroll state, and slider drafts were retained; long-press draft choice became saveable. No new motion or measured frame-rate improvement was claimed.

Recorded addendum outcomes:

- **290 JVM tests**, **75 complete UI tests**, normal/0x animation, and a new lazy-composition test proving distant items were not composed early but remained reachable.
- Lint **0 errors / 117 warnings**.
- Release build, native boundary tests, **5 APK-naming tests**, and **16 KB ZIP-alignment** checks passed.
- `latest-light.png` / `latest-more.png` visually rechecked; grouping and four-mode row retained without eager whole-list composition.
- **20** extra compact **320 dp / 1.5x** tests passed, covering back/scroll under normal/disabled motion, slider failure rollback, distant lazy items, whole-row switches, and rename cancellation.

These counts belong to different historical runs and must not be summed or presented as a current suite result.

## 5. Accepted differences and unverified boundaries

- Historical P3 differences: native top bar **48 dp** rather than extra generated device-frame whitespace; product perspective, font antialiasing, and arrow strokes differed slightly from the reference.
- Emulator had no HyperOS/LSPosed. Model-map unit tests did not prove final SystemUI island rendering.
- No frame-rate measurement, full TalkBack, physical-keyboard, or all-OEM acceptance was recorded.
- The initial UI review did not change protocol/connection/settings-write logic; the later build 23 addendum separately records its four runtime fixes.
- Automation did not install onto or change settings on connected physical phones.
- HyperOS 4 service recreation, single-ear behavior, real route switching, and island alias refresh remained physical-device gates after upgrade/restart.
- Synthetic AirPods 5 screenshots do not verify real AirPods 5 firmware, AACP, lid-open detection, or native-panel behavior.
- None of these historical results verifies P.O.D.S. API 102, the new application ID/certificate, English/Simplified Chinese-only resource scope, Liquid Glass, or R01.

Current acceptance authority is the [P.O.D.S. delivery plan](docs/04-delivery-plan.md), populated only with newly executed, artifact-specific results.
