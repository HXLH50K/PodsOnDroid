# P.O.D.S.

A private HyperPods-derived Android project for improving the AirPods 5 experience on Xiaomi HyperOS 4.

## Current checkpoint: alpha9 reference-style panel

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
