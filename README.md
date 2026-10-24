# P.O.D.S.

A private HyperPods-derived Android project for improving the AirPods 5 experience on Xiaomi HyperOS 4.

## Current checkpoint: alpha17 accepted AirPods 5 head tracking

UI follow-up corrections: the remaining root-title gap was a duplicated
status-bar inset, not the title size. The page frame now consumes its manually
applied top padding before Miuix adds its own insets. The UI regression measures
the title-to-status-bar gap on both main tabs, not just toolbar height.
App/UI-test compilation passed. Bounded direct screenshots of the synthetic
headphone and settings tabs on the HyperOS 4 phone confirmed the reduced gap.
UIAutomator still cannot obtain an idle hierarchy, so the automated spacing
assertion is compiled but not claimed as executed.

UI follow-up: both main tabs now use compact headers. App copy uses direct
instructions rather than implementation language; detailed protocol identifiers
remain in diagnostics. About lists the original and forked HyperPods repositories,
LibrePods and OpenPods with visible, clickable addresses. Attribution, the pinned
source revision and license notices are retained; the repository remains private.
The original UI batch passed 156 affected JVM regressions, app/UI-test
compilation and Lint. Both APKs installed on the HyperOS 4 phone without clearing app data.
The first phone instrumentation attempt exited in its first About test;
the final 42-case round stalled in that first case for 600 seconds with no
completed result. The runner and test app were stopped. Runtime UI acceptance
is not claimed, and no scope restart or physical earbud operation was performed.

Battery settings no longer show an app-card imitation of the Super Island.
Reminder switches save immediately; valid threshold pairs save on field blur,
leaving the page or backgrounding the app. Invalid edits show an error and keep
the last saved thresholds; switches can still be saved independently.

The long-press cycle sheet has no confirm/cancel header buttons. A changed valid
cycle is sent once on dismissal. An unread state uses a short single-line label
and a separate explanation. The unsupported assistant row is disabled unless
the existing debug override is enabled; even then it only explains that no
assistant implementation exists and never pretends to change the headphones.

Tone-volume dragging now keeps a local target until release and sends once.
Headphone reports cannot move the thumb during a drag. A different real report
after release updates the display with a short animation and a toast; sent-only
optimistic state is not treated as confirmation. Failed writes animate back to
the last real reading. This is an interaction fix, not an AirPods 5 readback fix.

Module settings separate scope restart and module status into independent cards.
Themed Compose dialogs use the same Miuix colors, cards, checkboxes and buttons
as the rest of the app. Choices are still retained locally, but ordinary copy
only explains the operation and its effects. After confirmation the progress
dialog cannot be dismissed and offers no cancel/stop button; it closes when
the bounded operation completes or fails.

All registered scopes, including Bluetooth, are now available in both debug and
release builds. Explicit approval, authenticated reports, root PID/UID checks,
call-state checks, provider-first ordering and current-build verification still
apply. The normal non-approved control-shutdown guard is unchanged. Debug-only
fixture mode `scope_restart_preview` simulates selection and progress without
preference writes, root commands or process restarts. JVM tests exercise fake
executors in both build variants; no real scope restart is part of UI validation.
The follow-up passed all 574 Debug JVM tests and 52 focused Release JVM tests,
app/UI-test compilation and Lint. Direct synthetic screenshots on HyperOS 4
confirmed both themed dialogs and the absence of a progress cancel/stop button.
The simulated preview was stopped after checking; automated UI assertions
remain unexecuted because of the device's idle-hierarchy blocker.

The online audit found a concrete AirPods 5 path in LibrePods'
`android/rewrite` revision `80aa7461df414470f379ebdfe540b2e8889fcbe2`.
After the user chose to try it, P.O.D.S. now selects CMA_COMMAND (79) for
AirPods 5/5 WCC stream startup and DEVMOTION6 (16) for stop, matching that
upstream implementation. Other models retain their existing packet pairs.
AP5 does not fall back to the obsolete generic starts. Motion decoding is
unchanged. The user accepted the AP5 pose/head-tracking round on 2026-10-24 and
requested closing [#2](https://github.com/HXLH50K/PodsOnDroid/issues/2).
This does not claim a separate incoming-call acceptance test.

Swipe interval has no verified fix for the current device. It is labeled
**Not yet implemented** and disabled by default. Settings > Advanced &
diagnostics contains an explicit persisted **Force unfinished features (debug)**
switch. It only unlocks the control, keeps the warning label, and never writes
a headphone setting by itself. Model/value/readiness checks still apply.
Both the UI and Bluetooth controller enforce the default gate.
The follow-up main/rewrite audit found the same `0x23`, values 1/2/3 and generic
sender, not a new interval fix. [#3](https://github.com/HXLH50K/PodsOnDroid/issues/3)
remains open; its current behavior is unchanged.

Connection diagnostics now requests a fresh read-only snapshot when opened.
Head diagnostics say **non-pose replies**, not headphone rejection. The
rewrite's protobuf schema identifies the earlier short responses as
`start_ack`; that is not proof that sensor samples follow.

## Previous checkpoint: alpha16 authorized ownership experiment

The user authorized headphone ownership operations. Connection diagnostics in
the debug build now offers one manual local-ownership request, with an explicit
pending/real-report/timeout result. It does not enable automatic takeover, send
smart-routing/hijack commands, or initiate audio transfer. The button remains
disabled until the Bluetooth host reports support for the new experiment.

Explicit ownership requests, pose tests and swipe-interval changes open bounded
debug evidence windows: at most 10 seconds and 64 allowlisted records. The trace
distinguishes queued writes, failed writes and actual replies. It can retain the
observed 20-byte short head response, but never a full sensor stream or unrelated
key/device-information packets.

[The read-only capture script](tools/capture_protocol_evidence.py) retrieves only
these metadata records, is time/record bounded, and rejects output inside the
repository. This experiment is not yet proof that ownership fixes #2 or #3.

## Previous checkpoint: alpha15 protocol-evidence follow-up

User acceptance confirmed speaker switching when the earbud sensors are not
covered: issue [#4](https://github.com/HXLH50K/PodsOnDroid/issues/4) is closed.
P.O.D.S. consumes the headphone-reported wear state; it does not implement
Apple's skin/object classifier. Cycle auto-save is also accepted; issue
[#5](https://github.com/HXLH50K/PodsOnDroid/issues/5) now tracks deferred minor UI
follow-up only.

Issues [#2](https://github.com/HXLH50K/PodsOnDroid/issues/2) and
[#3](https://github.com/HXLH50K/PodsOnDroid/issues/3) remain **unresolved**:
alpha14 showed rejected head replies but no sensor samples, and swipe interval
has no confirmed readback or observed effect. Alpha15 prepares bounded evidence
instead of broadening the sensor parser or remembering a request as a device value.

- Pose diagnostics show why the last reply was rejected, its length and only
  its first 12 envelope bytes, never a full sensor payload.
- Connection diagnostics retain the first four bytes/length of actual `0x23`
  replies, including values rejected by the current setting schema.
- An explicit swipe-interval write can issue one existing status-subscription
  refresh while awaiting its echo; the original deadline is unchanged.
- Last-request information is connection-local diagnostic state only. It is
  neither persisted as a preference nor used as the selected headphone value.

All **218 affected JVM regressions pass**; app/UI-test builds and Lint pass.
No ownership takeover, background log collector, automatic headset test,
sensor override or minor UI fix is included. Results from the subsequent
user-operated round are below.

The completed alpha15 user round produced a 20-byte head reply but no valid
samples, and a real swipe reply of 3 while the last request was 1. Their ordering
is not captured by the snapshot. Ownership remains unknown. LibrePods attempts
a conditional takeover before head tracking; P.O.D.S. does not, but this is not
yet proof of the cause or permission to add automatic takeover. See delivery-plan
section 27 for the evidence and the comparison requiring explicit authorization.

## Previous checkpoint: alpha14 functional corrections

Four English issues track the latest reports: [#2](https://github.com/HXLH50K/PodsOnDroid/issues/2)
combines missing head gestures/pose data, [#3](https://github.com/HXLH50K/PodsOnDroid/issues/3)
covers the disabled swipe interval, [#4](https://github.com/HXLH50K/PodsOnDroid/issues/4)
covers outside-case speaker switching, and [#5](https://github.com/HXLH50K/PodsOnDroid/issues/5)
covers long-press cycle auto-save. Implementations and local regressions are
complete; actual AirPods acceptance is still required and the issues remain open.

- Head tracking now distinguishes valid sensor data from control replies and
  gesture-filtered motion, has a bounded alternate-to-standard stream retry,
  and exposes readiness, packet and ownership diagnostics. No connection takeover
  or simulated sensor data is introduced.
- Supported swipe intervals can be explicitly selected before a report, with
  no invented selected/default value. A real echo is still required for confirmation.
- Speaker routing re-evaluates late route discovery, matches verified system
  route identity independently of display names, and cancels stale pending work.
- Valid changed long-press cycles submit once on normal dismissal. Explicit
  Cancel discards; unchanged/invalid/unready selections never silently write.

The head implementation was inherited from HyperPods, whose protocol/math work
references LibrePods; it is not an iPhone preference synchronized to Android.
See [provenance and validation](docs/04-delivery-plan.md#26-alpha14-functional-issue-batch).
All **210 affected JVM regressions pass**. A wider run has **529/535 passing**;
the remaining six preference tests reproduce on pristine `e00dd87` and are not
caused or changed by this batch. App/UI-test APK builds and Lint pass. No
physical earbud, mode, volume, call or scope-restart action was performed.

## Previous checkpoint: alpha13 dropdown repair

**Session scope:** the user requested pushing the completed work and auditing
remaining TODOs. Miscellaneous UI polish is explicitly deferred to another
session. The remaining functional/verification/release work is summarized in
[the current backlog](docs/04-delivery-plan.md#25-post-alpha13-backlog-audit).

The user accepts the overall alpha12 UI and defers miscellaneous polish. The
blocking secondary-page dropdown problem is repaired: the custom app frame
had omitted Miuix's shared popup host that its original Scaffold provided.
Alpha13 restores that host once at the app root, outside page animations,
blur-source and IME-padding layers. No selector readiness or value callback
was changed.

Direct authorized phone UI checks confirmed press-speed and hold-duration
menus render their options, Back closes them without leaving More controls,
and a normal outside-body tap dismisses a reopened menu. Both displayed values
remained Default; no option, toggle, mode or slider value was changed.

16 focused JVM tests, app/UI-test APK assembly and Lint passed. Three dropdown
regression tests were added and compiled. A single instrumentation attempt did
not complete and was explicitly aborted; it is not counted as passing. Actual
UI validation used direct app interaction and screenshots, **not background log
collection**. See the [alpha13 receipt](docs/04-delivery-plan.md#24-alpha13-shared-dropdown-popup-host-repair).

## Previous checkpoint: alpha12 unified page framework

The user prioritized the overall UI over special glass optics; ordinary blur
is acceptable while navigation and hierarchy are corrected. Alpha12 retains
the agreed Headphones/Settings split and replaces per-page headers with one
shared frame using the pinned Miuix app bars. Child back buttons remain outside
scrollable content, with explicit 48 dp touch targets and contrast.

Settings, About and headphone pages now share depth-aware enter/return
transitions, saved page state and outgoing-interaction protection. Root settings
use grouped app/support rows instead of a large card for every destination.
The headphone overview has one original product image and compact per-part
battery/state tiles. The accepted panel and device-control logic are unchanged.
No new glass library, framework API or special Xiaomi rendering hook was added.

Local verification: **33 focused JVM tests passed**; app and UI-test APKs build,
and Lint has no errors. Pinned-back/scroll and interruptible navigation UI tests
were compiled, not phone-executed. Actual visual and motion acceptance remains
a user check. The in-case noise-control defect [#1](https://github.com/HXLH50K/PodsOnDroid/issues/1)
is still open and was not addressed by this UI milestone.

## Previous checkpoint: alpha11 app redesign and information architecture

The user rejected alpha10's visual interpretation and supplied a Keimi reference:
flat backgrounds, layered solid cards and a floating frosted navigation capsule
that blurs the real content scrolling behind it. Alpha11 replaces the inherited
app layout accordingly. There is **no legacy/new UI switch** and no glass-preview
opt-in. Reduced transparency remains an accessibility/power option only.

- **Headphones:** device battery/state, listening modes, headphone controls and
  device information. Notification settings and diagnostic/test screens no
  longer live in More controls.
- **Settings:** battery notifications, appearance, project About and Advanced &
  diagnostics. This root contains category links, not expanded diagnostic data.
- **Settings > Advanced & diagnostics:** connection reports/export, module
  checks and scope reload, head-motion tests and debug-only case observation.
  Entering the menu does not restart processes or begin a headset test.
- **Settings > About:** project purpose/version, pinned HyperPods provenance
  and source/license notices. All sponsorship/donation entries were removed;
  legal attribution remains intact.

The dock uses the existing Haze renderer against the actual page content; the
artificial gradient and glass-card demo are removed. Dark/light themes, readable
opaque fallback, lifecycle/power/high-contrast gates and keyboard/focus clearance
remain supported. Changes are app-side only: the accepted panel, native island,
controller semantics and the recorded in-case noise issue [#1](https://github.com/HXLH50K/PodsOnDroid/issues/1)
are unchanged.

Local validation: **29 focused JVM tests passed**; app and updated UI-test APKs
build successfully. UI tests are compiled, not executed on the phone. Android
Lint has no errors. Final visual acceptance still belongs to the user-operated
round; the reference image and Keimi implementation are not redistributed.

## Previous checkpoint: alpha10 opt-in app appearance prototype

The user accepted alpha9's basic floating-panel experience, while noting that
its aesthetics still need refinement. A shared control defect is recorded as
[#1](https://github.com/HXLH50K/PodsOnDroid/issues/1): noise-control changes fail
with the earbuds in the case and reportedly require both earbuds to be removed,
in both the app and panel. Its cause is unconfirmed and it is **not fixed** here,
as the user requested recording it before continuing the plan.

The next planned M4 milestone is a small real-app Liquid Glass prototype.
Alpha10 adds **About > Appearance > Liquid Glass preview**, off by default, with
a headphone-free sample and a separate Reduce transparency choice. The effect
is limited to the main headphone overview, not the panel, settings subpages or
SystemUI. Existing labels, reports, capabilities and command callbacks remain
unchanged.

The existing Haze dependency renders a static layered backdrop and glass
surfaces. No continuous animation or background timer is added. Effects are
disabled offscreen/in the background, under power saving/high-contrast text, or
when transparency is reduced. Appearance preferences remain app-local and are
not transmitted to the Bluetooth hosts.

Local evidence: **23 focused JVM tests passed**, including all 64 combinations
of the effect policy gates; assembly and Lint passed. Visual approval and broader
M4 stability/power acceptance are still pending. See
[the M4 prototype record](docs/04-delivery-plan.md#21-alpha9-acceptance-known-control-defect-and-m4-app-prototype).

## Previous checkpoint: alpha9 reference-style panel

The user accepted alpha8's floating-panel entry but rejected its dashboard-like
appearance, providing a Xiaomi headphone-card screenshot and preferring MiLink
reuse. Alpha9 replaces the miniature dashboard with a compact battery header,
large original earbud image, rounded translucent controls and More settings.
Android's native cross-window and behind-window blur provide the glass surface;
when blur is unavailable, the window explicitly uses an opaque readable surface.
This is still a P.O.D.S.-owned window, **not native MiLink panel reuse**.

The installed MiLink detail panel was inspected: its fragment/view depends on
internal service/device models and a headset controller. No isolated adapter
that preserves the single P.O.D.S. command writer has been verified. Alpha9 does
not spoof a Xiaomi model, add a MiLink scope, import OEM code, weaken privileged
permissions or claim that full native integration is impossible.

The panel retains reported-only mode selection and truthful battery state.
Its volume slider controls the phone's actual **media stream**, writes only on
user input, and reads back Android's resulting level. It does not send a second
headphone protocol or expose unverified ring-to-find controls.

The accepted alpha8 round also exposed a null plugin `applicationContext` in the
device-card artwork hook. Alpha9 obtains the actual SystemUI package context
for the artwork and click receivers. **28 focused JVM tests passed** and Android
Lint completed without errors; actual glass rendering remains for the next
user-operated check. See [the alpha9 record](docs/04-delivery-plan.md#20-alpha9-reference-style-panel-and-native-reuse-boundary).

## Previous checkpoint: alpha8 Interconnectivity preview

Development resumed on 2026-10-23 after the user reported no obvious alpha7 issues.
Alpha8 adds identity-scoped AirPods artwork in Interconnectivity and a dedicated
P.O.D.S. floating panel with earbud/case images, battery readings, capability-gated
listening modes, and **More settings** to open the full app. Unknown batteries
remain unknown; mode selection is not treated as a headphone report. The panel
uses the existing authenticated Bluetooth channel, not Xiaomi's old AirPods SDK.
It is an app-owned dialog window, **not** a claimed native MiLink headphone panel.
No additional module scope or root shell is required for these changes.

**Deployment update, 2026-10-23:** the user returned to the original workstation
and authorized pulling, packaging and deploying alpha8. Source `8ad93c7b` was
fast-forwarded without modification. **45 focused JVM tests passed** (22 panel/
model/settings cases plus 23 native-island/artwork regressions), and debug
assembly succeeded. The development signer matches the installed alpha7 signer.
Alpha8 was installed in place on the connected `houji` test phone; installed APK
bytes/version and panel action registration were verified. App data was not
cleared, and no scope restart, UI launch or earbud operation was performed.
See [the deployment receipt](docs/04-delivery-plan.md#19-alpha8-original-workstation-build-and-deployment).

The previous cross-machine signing blocker was specific to the other
workstation; its [transfer checklist](docs/04-delivery-plan.md#cross-machine-transfer-checklist)
remains historical context. Broader controller CI still fails at the documented
unstubbed mock-address tests; this deployment does not claim those tests pass.
Actual Interconnectivity artwork, floating-panel rendering and headphone
controls still await the user-operated round after full DEBUG scope reload.

## Historical checkpoint: alpha7 daily-use freeze

Earlier on 2026-10-23 the user reported preliminary validation passed and chose a one-day trial on the primary phone. That freeze was superseded by the alpha8 request above. The following receipt describes the previous checkpoint.

- User-confirmed: basic preview functionality, complete DEBUG scope reload, and the corrected native connection island.
- Runtime-confirmed: actual dual-ear visibility followed by normal expiry exactly **5,000 ms** later.
- Implemented: no ordinary notification carrier or persistent battery surface; original untinted earbud artwork; independently colored battery text; editable Low/Medium thresholds (20/50 defaults), separate reminder switches, downward crossings and initial-reading suppression.
- Latest focused validation: **23 JVM tests passed**, debug assembly and APK identity/signature/API102 checks passed. Full-day stability, power use, recovery and the complete reminder/edge-case matrix remain unverified.
- Frozen trial APK: `dist\PODS-0.1.0-alpha.7-debug.apk`, source `d3227eec`, SHA-256 `32afa88fde3922839829ff8d54eb402785a7e4a670e1c1c1d194c3284b9224bc`. See the [package and runtime receipt](docs/04-delivery-plan.md#alpha7-package-receipt).
- Still planned, not part of this checkpoint: dedicated Interconnectivity headphone panel, Liquid Glass UI, broader device/ROM acceptance and independently signed production delivery. Discovery/first-pairing popup work remains provisionally deferred.

**Alpha7, 2026-10-23:** the user's alpha6 test produced no island. A user-operated reload/reconnect captured the failure: the OEM returns a **lazy content proxy**, which alpha6 incorrectly treated as the concrete window controller. Alpha7 resolves the verified proxy on demand and preserves the native width-request handshake instead of rejecting an initially zero width. The subsequent user-operated round confirmed actual dual-ear visibility and normal expiry exactly 5,000 ms later. Model-specific artwork remains untinted; colors apply only to percentage text. This verifies the missing-island repair, not every visual or battery-policy acceptance case.

The battery path no longer creates a `Notification`, notification channel, or `StatusBarNotification`, and has no notification publication fallback. It sends bounded authenticated data to SystemUI, which creates owned native island content using the contract inspected in this phone's system APKs. Ordinary notification access is retained only to remove proved-owned older-version remnants.

The priority full DEBUG scope reload was introduced in alpha5 and subsequently confirmed working by the user. Older builds and their validation boundaries remain historical receipts in the [delivery plan](docs/04-delivery-plan.md); alpha7 is the accepted baseline and alpha8 is the deployed, not-yet-accepted panel preview.

The user's Xiaomi comparison found silent paired reconnect/wearing and a quick-connect card in discovery/first-pairing mode. P.O.D.S. intentionally keeps the user's preferred brief battery island for reconnect. First-pairing popup work is **provisionally deferred**, not permanently cancelled; ordinary Android Bluetooth pairing remains available. The dedicated Interconnectivity control panel and Liquid Glass remain planned. See the [latest UX evidence](docs/07-device-and-native-integration.md#manual-xiaomi-comparison-and-ios-distinction) and [decisions](docs/05-decisions.md).

At the user's request, `main` now starts at the independent P.O.D.S. snapshot **`781b4b6`**, with an identical source tree and no HyperPods commit ancestry. The former local history, including preview implementation `5013f04`, was verified and archived outside the repository before rewriting. Historical receipt hashes refer to that archive, not the new branch ancestry.

## Project boundaries

- App/module title: **P.O.D.S.**; release application ID `com.hxlh50k.pods` (unsigned), isolated debug ID `com.hxlh50k.pods.debug`. The Kotlin/Android namespace remains `moe.chenxy.hyperpods`.
- Per-app locales implemented: **English and Simplified Chinese (`zh-CN`) only**. Coding documentation and commit messages are English.
- Repository: [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid), **private**. The user authorized publication of this checkpoint; keep the repository private unless explicitly instructed otherwise.
- Preserve HyperPods attribution, pinned source provenance, and applicable [GPL terms](LICENSE), not its Git ancestry. Private hosting and independent history do not waive distribution obligations.

## Prerequisites and validation

Development uses Gradle 8.13, a JDK 22 toolchain, Android SDK 36, CMake 3.22.1, and the Android NDK selected by the build. The baseline used a JDK 17 launcher and cached distributions. After the initial download timeout, the wrapper now bootstraps `8.13-bin` using this environment's existing proxy process-locally; its network timeout is 60 seconds. See [build configuration](app/build.gradle.kts) and the separately [recorded build/test results](docs/04-delivery-plan.md#9-measured-baseline-checkpoint).

Runtime work targets the rooted `houji` test phone, `OS4.0.0.14.XNCCNXM`, Android 17/API 37. **Daemon-reported API 102 is verified**; user-led module/reload checks and the alpha7 native-island lifecycle provide actual host integration evidence. This does not certify every hook or another phone/ROM. Legacy Xposed and a dedicated rootless backend are out of scope. See the [framework evidence and CLI boundary](docs/07-device-and-native-integration.md#read-only-daemon-status-follow-up).

Hardware-gated validation requires the user's AirPods 5 wireless-charging variant (reported firmware `1.0.0`). Xiaomi Open-ear Headphones Pro (reported `1.1.0.3`) provide a native-behavior comparison. Neither imported tests nor synthetic previews prove real AirPods 5 control, lid detection, or native-window compatibility. Do not install or restart system hosts outside the controlled test plan.

The debug app uses the same three declared scopes: Android Bluetooth, Xiaomi Bluetooth and SystemUI. Full DEBUG scope reload is user-confirmed working; optional Xiaomi `:ui` handling remains conditional. No autonomous host-process restart or physical earbud manipulation is part of the daily-use freeze.

## Manual native-island regression check

The alpha7 missing-island repair is runtime-verified in the delivery record. The following procedure is for future agreed regression rounds, not an outstanding step for that repair.

1. Install alpha7, then use the now user-verified full DEBUG scope reload while no call is active and Bluetooth interruption is acceptable. Verify that the required three hosts report the new build.
2. In one agreed round, reconnect the earbuds once. Expect direct native island animation with no preceding ordinary notification/card and no notification remaining after it closes.
3. Confirm both earbud pictures retain their original shading/material. Only the respective percentage text should use the Low/Medium color rules.
4. If nothing appears or the lifecycle is wrong, report that result before retrying. Native schema mismatch fails closed instead of falling back to a notification. No repeated automated phone tests or power actions are performed.

The user has completed preliminary alpha7 validation and paused development for daily use. Future hardware interactions remain user-driven; hand off a verified preview or capture procedure rather than autonomously manipulating the headphones.

Device acceptance is collaborative: after focused local validation, agree on one or two checks with the user and wait for their observations. Do not extend the handoff into repeated UI automation, wake/unlock retries, scope restarts, or radio capture. The previous computer-shutdown request is withdrawn.

## Documentation

Start with [readiness and stage boundaries](docs/09-readiness.md) and [confirmed decisions](docs/05-decisions.md).

| Document | Purpose |
| --- | --- |
| [Research](docs/01-research.md) | Pinned evidence, platform limitations, and feasibility |
| [Product](docs/02-product.md) | Required experience, supported scope, and honest state feedback |
| [Battery notification specification](docs/10-battery-notification-spec.md) | Authoritative dual-ear colors, visible timing, downward crossings, initial-low suppression and joint acceptance cases |
| [Architecture](docs/03-architecture.md) | Process ownership, protocol, IPC, and recovery |
| [Delivery and acceptance](docs/04-delivery-plan.md) | Milestones, hardware gates, and measurable targets |
| [Signing and maintenance](docs/06-signing-and-maintenance.md) | Independent identity, private delivery, rollback, and licensing |
| [Device and native integration](docs/07-device-and-native-integration.md) | Recorded device evidence and unverified OEM integration |
| [API 102 migration](docs/08-hyperpods-api102-migration.md) | Modern module contract and migration acceptance |
| [Upstream UI evidence](design-qa.md) | Historical HyperPods results, **not P.O.D.S. verification** |
