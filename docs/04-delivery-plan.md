# 04 Delivery and acceptance plan

**Current status, 2026-10-23 evening:** the user accepts alpha9 as basically meeting the floating-panel requirement and asks to record the shared in-case noise-control issue before continuing the plan. Section 21 records issue #1 and the next M4 opt-in app appearance prototype. The defect is not fixed or declared a firmware limitation. No automatic scope restart or earbud operation is authorized.

**Morning priority override, 2026-10-23:** the user returned and withdrew all further shutdown actions. Alpha4's UI-only restart cannot satisfy update/reload requirements; implement and locally validate full debug scope reload first, leaving actual process execution for a joint check. The user additionally requires earbud icons and a direct native animation without a normal notification. Icon and ordinary-expansion corrections are distinct from replacing the current notification-backed transport; do not claim the stricter pure-native route is complete merely because a notification is hidden.

**Current task, 2026-10-23:** after requesting the [battery notification specification](10-battery-notification-spec.md), the user requested implementation, packaging **without tests**, and then computer shutdown. Alpha4 follows that contract. Only compilation/packaging and artifact checks are permitted in this iteration; BAT-01 through BAT-36 and native timing/rendering remain unexecuted, not passed.

Updated: 2026-10-22. The user accepted the basic alpha1 behavior and requested further development with independent Git history. Section 10 records the alpha2 notification/capture changes; section 9 preserves the original preview receipt. Native-window, live R01 and broader hardware acceptance remain incomplete. The release plan remains a future acceptance contract, not a completed-MVP claim.

**Current scope override:** after the user's Xiaomi comparison, ordinary paired reconnect targets a brief battery island followed by **no persistent island or status notification**. The user is reconsidering first-pairing/discovery popups; that work is provisionally deferred, not permanently cancelled or marked accepted. The active path can proceed to the dedicated Interconnectivity panel without demanding another lid-capture round.

## 0. Starting conditions and pause points

- Confirmed: `P.O.D.S.`, application ID `com.hxlh50k.pods`, retained namespace `moe.chenxy.hyperpods`, AirPods 5 wireless-charging variant/reported `1.0.0`, current test phone first, API-102-only LSP, and non-destructive testing authority.
- Latest instructions: private [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid), English coding docs/commits, app English and Simplified Chinese only. The repository exists with no initial push at this checkpoint; visibility changes require explicit authorization.
- M0/M1-A need no headphones: baseline build/tests, identity, API migration, controlled probes and actual-host loading checks.
- M1-B requires AirPods 5. M2/M3 use Xiaomi Open-ear Headphones Pro/reported `1.1.0.3` when native comparison is needed.
- When hardware is needed, finish and verify the independent stage, summarize it with `task_complete` where available, and explicitly wait for the user. Do not poll continuously or declare the whole product complete.
- Release-password custody and any proven-necessary alternative panel remain deferred. The repository URL is no longer an unresolved choice; specific upload/release authorization remains separate.

## 1. Dependencies

```text
M0 Imported HyperPods baseline / identity / baseline build and tests
 -> M1-A API 102, no-headphone probes, R01 self-check contract
 -> wait for AirPods 5
 -> M1-B control and settings/report regression
 -> connection-only battery presentation and cleanup
 -> M3 dedicated Interconnectivity headphone panel
 -> M4 Liquid Glass / brief island / R01 / stability and power
 -> initial release through an explicitly approved private channel

M2 first-pairing popup research is optional/provisionally deferred.
After M0, M3 research/model design may proceed independently.
Real injection/control requires the M1 gates.
After M4: M5 additional features and evidence-driven optional KSU.
```

The dedicated control panel remains mandatory and cannot be replaced by the brief island. M2's earlier mandatory-popup assumption is suspended under the scope override above. No dedicated rootless/legacy-framework workstream.

## 2. Milestones

### M0: Derivative baseline and test conditions

Work:

- Preserve source provenance from HyperPods `a44bc10e387f527062b9ba2b8f90b154b2e61248`, design docs, attribution, and applicable GPL. The user subsequently requested independent Git ancestry; the previous history is archived externally and the reference workspace stays unchanged.
- Record clear upstream/project-origin roles. The origin repository is private; no upload or public visibility is implied.
- Reproduce the smallest existing build/test baseline before API changes. Do not also replace storage, project structure, and UI libraries.
- Establish application identity without renaming the retained source namespace unnecessarily.
- Inventory current features/defaults/system entry points, known UI issues, and regression screenshots.
- Use recorded phone/component metadata from the [device report](07-device-and-native-integration.md). Obtain the user's rolling daily run/hash only when needed for a comparison.
- Xiaomi headphones are a later comparison device, not a substitute for AirPods 5 tests or a prerequisite for static work.

Exit: pinned derivative baseline, runnable recorded build/tests, correct identity/license handling, and controlled testing/rescue procedure. Import alone does not satisfy the build/test exit criteria.

Estimate: **0.5-1 effective development day**, excluding hardware/decision waits.

### M1: API 102 and control regression

**M1-A, no headphones:**

1. Replace legacy entry, YukiHook DSL/KSP, XposedHelpers/Bridge, and metadata per the [migration plan](08-hyperpods-api102-migration.md), not merely a version number.
2. Probe runtime `apiVersion >= 102`, hosts/processes, plugin loader, and cross-process self-checks.
3. Define R01 build identity/loading confirmation. Test unknown-host refusal, root denial, and read-only preflight before restarting multiple system hosts.
4. Check final-artifact modern registration, no legacy calls, and host/profile matching.

**M1-B, hardware required:**

5. Test classic socket construction, stack negotiation, handshake/subscriptions separately. Determine whether this ROM needs the native patch; no additional rootless backend.
6. Preserve/test IPC caller boundaries and session epochs; no second connection owner.
7. On supported capabilities, make reversible setting changes and restore the original state. Record each step; no reset, firmware update, or expansion into high-risk functions.
8. Test dual/single ear, lid open/close, Bluetooth toggle, and app restart while verifying recovery does not interrupt audio.
9. Preserve wear/conversation defaults and saved choices. Add default/unread and sent/reported provenance without writing display defaults.

Exit:

- **At least 19 of 20 connection cycles** produce necessary valid state within **15 seconds**; every failure has a diagnosable stage.
- **30 consecutive matching noise-mode reports** in normal supported operation. Unsupported capability is explicitly not applicable, never faked.
- Control failure preserves system audio; no Bluetooth/SystemUI crash.
- Runtime API and module metadata agree; target DEX has no legacy calls; plugin registration is not duplicated; unauthorized IPC/old sessions cannot change state.

Stop/redirect:

- Runtime lacks API 102: stop injection, report; no legacy fallback or unauthorized framework replacement.
- Native target ambiguous or requires globally unsafe changes: do not force the patch.
- No real equipment: only replay/UI work may pass, not physical compatibility.

Estimate: **3-6 effective development days**. M1-A may be delivered independently; waiting for hardware is not an M1-B pass.

### M2: Discovery / first-pairing popup (provisionally deferred)

The following work is retained as a future option, not a dependency of the current reconnect/island correction. If resumed, first-pairing discovery and paired-device battery display must be specified separately; do not force a popup for every paired lid opening.

Work:

- Capture timestamped open/close, before/after connection, and single-/dual-ear advertisements/control reports; create redacted fixtures.
- Map actual BLE model IDs to actual hardware/case variants; `Axxxx` model numbers are not BLE identifiers and old lid-bit assumptions are not evidence.
- Replace global scan suppression with version/device-bounded coordination only when the new route can own the target.
- Prefer the system quick-connect Activity/plugin container; verify resources, initialization, identity, lifecycle, and writer ownership rather than adding a blind allowlist entry.
- Implement cycle deduplication, manual-dismiss suppression, connection progress, and actual/estimated battery provenance.
- Match Xiaomi-observed lock-screen/landscape/full-screen behavior; cover unknown/multiple nearby devices, reordered/missing ads, and no A2DP connection.

Exit: repeated real AirPods 5 lid openings produce the correct window without requiring an A2DP event; no duplicate popup/scanner/control session or changes to unrelated devices. Synthetic events test UI only.

Estimate: **3-6 effective development days**, reassess if resources or advertisement semantics remain unknown.

### M3: Dedicated Interconnectivity headphone panel

Work:

- Follow the identified SystemUI-plugin-to-MiLink service route, checking device/category/capability models and trusted calling boundaries.
- Prefer native headphone container plus AirPods 5 state/actions, not the baseline full-Activity redirect.
- Scope identity/type mappings to the target; globally replacing `third_headset` with `headset` is unacceptable and does not satisfy model/online/listener requirements.
- Read a common snapshot and write only through the single queue; prove Xiaomi's old SDK is not competing.
- Cover three/four modes, unknown case battery, fast taps, close/back, rotation, lock, and MiLink reconstruction.
- Compare layering, animation, return behavior, density, and control results using the Xiaomi headphones on the same phone/theme/font when connected.

Exit: target-card click directly opens a useful dedicated panel with real data/actions; ordinary/Xiaomi headphone behavior remains intact. Type-string changes, settings redirects, and empty cards fail.

Estimate: **4-8 effective development days**. If safe native reuse fails, present evidence and an alternative small-panel design; do not silently lower the target.

### M4: UI, retained features, restart, and stability

Work:

- Prototype Liquid Glass on a real Android page, then migrate theme/controls separately from API work.
- Share model/name/state provenance and command semantics across app, popup, panel, and island without global SystemUI changes.
- Remove the persistent battery island. Consume true connection events once, end the prompt within a bound, and keep state reports from re-alerting.
- Regress applicable conversation awareness, single-ear ANC, swipes, and existing AirPods 5 controls by actual case capability; no false read-back confirmation.
- Regress process recreation/reclamation, lock/power saving, disable/enhancement-off, bounded recovery, and media-route safety.
- Run **24-hour** mixed use, A/B power, accessibility/large-font, and redacted-export tests. Inventory power-affecting test-phone modules without changing them without permission.
- Complete R01 allowlist, impact confirmation, exact PID/identity verification, sequential restoration, and new-build checks. Test UI hosts first, then Bluetooth interruptions in an explicit test window.
- Before authorized release, create the independent signing identity and verify independent installation/import, same-signature upgrades, disable, and rollback.

Exit: every applicable first-release mandatory item passes with evidence; unverified capabilities are labeled. Lid popup/panel cannot be removed from acceptance as optional failures.

Estimate: **4-7 effective development days**, plus at least a **24-hour** stability/power window.

### M5: Incremental features and optional KSU

Add genuinely needed capabilities/firmware, individually verify new privilege needs, and create systemless privileged packaging only if warranted. Test the actual KSU/metamodule/framework combination, same-signature updates, return to user APK, disable/uninstall, and permission mismatch.

Exit: additions preserve verified combinations and recover safely. Estimate by feature; KSU packaging is not a native-panel shortcut.

The earlier **3-6 week** total was a rough estimate, not a commitment. Liquid Glass/R01 and M1/M2 evidence require re-estimation; deferred release choices do not block independent M0/M1-A work.

## 3. Physical-device result template

| Field | Current recorded value / requirement |
| --- | --- |
| Model / SoC / region | `23127PN0CC / houji / SM8650`, CNXM ROM |
| Full ROM / fingerprint | `OS4.0.0.14.XNCCNXM`; full fingerprint in device report |
| Android / API / patch | `17 / 37 / 2026-08-01` |
| ABI / page size | `arm64-v8a / 4096` |
| Bluetooth / APEX / native identity | `com.android.bluetooth / versionCode 37 / com.android.bt`; library build ID pending |
| Xiaomi Bluetooth / SystemUI / plugin / MiLink | Exact versions/hashes in [device report](07-device-and-native-integration.md) |
| Root / Zygisk / framework | UID 0; Zygisk Next 1.5.0; LSPosed v2.2.1 (7912); daemon-reported API 102 verified, P.O.D.S. host injection/new API calls still unverified |
| AirPods model / case / firmware | User reports AirPods 5 wireless-charging/1.0.0; read actual fields on connection |
| Native comparison | Xiaomi Open-ear Headphones Pro/1.1.0.3, user report |
| Daily-phone scope | Xiaomi 17 Pro is not automatically validated |
| Product version / source / certificate | Record the exact tested P.O.D.S. artifact, not imported test history |
| Other controllers | Exact package queries found no HyperPods/LibrePods on the surveyed phone; confirm the later test environment |
| Cases / counts / outcomes / logs | Record redacted results; static readings are not functional passes |

Select the authorized ADB device explicitly; never batch operations across every connected device. Do not record serial numbers, Bluetooth addresses, or pairing keys in project documentation.

## 4. Test layers

| Layer | Coverage | Cannot replace |
| --- | --- | --- |
| JVM | Frame boundaries, capabilities, reducer, command states, retry, battery policy, media ownership | Actual sockets/OEM behavior |
| Parser boundary/random input | Truncation, count overflow, invalid enums, long fields, unknown opcodes, split/coalesced frames | Real firmware protocol differences |
| Native host/device | Memory ranges, unique matches, unknown-image rejection, JNI failure, 4/16 KB pages | Injection timing/ROM stability |
| Android/Compose | Permissions, pages, process recreation, fonts/themes, TalkBack, notifications | Final native popup/panel/island rendering |
| Physical integration | AirPods 5 lid/panel/control, lock, route safety, disable | Long-term OTA evolution |
| Stability/upgrades | 24 hours, A/B power, same-signature upgrades, profile mismatch, rescue | Universal all-device support |

Use existing targeted tasks, for example `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`, plus existing native tests and release metadata/DEX inspection. Run `:app:connectedDebugAndroidTest` only with explicitly selected, authorized test equipment; it installs/runs instrumentation and is not a documentation-only check.

## 5. Required cases

| ID | Scenario | Expected result |
| --- | --- | --- |
| T01 | First install; deny nearby-device/notification permission | Clear reduced capability, no crash or false online state |
| T02 | Audio connected, control fails | Audio continues; only control reconnects with reason shown |
| T03 | Late UUID/service, connected before boot completion | Bounded recovery; no duplicate socket |
| T04 | Left-only/right-only, other ear in case | Unknown other ear cannot invalidate usable single ear |
| T05 | Offline case, partial report, pending refresh, real 0% | Correct independent states, no fabricated battery |
| T06 | Rapid modes plus physical long press | Old reports do not complete new requests; no forced overwrite |
| T07 | Truncated/malformed/duplicate/unknown packets | No crash, bounds violation, or media action; diagnosable errors |
| T08 | Blocked I/O, EOF, full queue, silent handshake | Timely release, explicit failures, bounded recovery |
| T09 | Old thread/timeout arrives after device change | Old epoch cannot affect new device |
| T10 | Speaker/car output, manual volume change | No old-volume restoration or wrong-route control |
| T11 | Already-paused media, new player, pause by another actor | No unsolicited play; distinguish phone policy from headphone settings |
| T12 | Low-battery reconnect/restart, charge then discharge | Dedup survives; genuine new discharge cycle can alert |
| T13 | Lock/Doze/power saving/host recreation/reboot | Correct lifecycle, no duplicate hooks/scanners |
| T14 | Force-stop app, disable enhancement, disable module | Distinct documented behavior; host honors disable, no secret UI revival |
| T15 | Framework enabled but wrong scopes/library absent | No false adapted status; identify failed layer |
| T16 | Unknown native build ID, changed methods, multiple matches | Refuse unsafe injection |
| T17 | Simulated OTA/APEX/plugin change | Disable mismatched adapters, preserve basic UI/audio |
| T18 | Unauthorized IPC, forged package, cross-user, oversized message | Reject with bounded error records; no raw-write surface |
| T19 | Reused notification/card views | No style residue, wrong clipping, or other-entry changes |
| T20 | Same-signature upgrade, system/user APK transition | Compatible data, one service, matching certificate |
| T21 | Other nearby AirPods/controllers | No strongest-signal binding; explain verified conflicts |
| T22 | Headphones never configured through Apple | Report successful/unknown functions; no guaranteed initialization-free claim |
| T23 | API 102 metadata, release shrinking, runtime checks | min/target 102, no legacy references, correct entry/plugin loading |
| T24 | Real lid opening before A2DP | Real lid detection, not a disguised connection notification |
| T25 | Repeated ads, manual dismissal, fast open/close | At most one popup per validated cycle; no repeated reopening |
| T26 | Nearby other/Xiaomi headphones, random BLE addresses | No wrong binding/window; original behavior preserved |
| T27 | Click target Interconnectivity card | Dedicated panel, not Bluetooth settings/full Activity |
| T28 | Panel mode/long press/four-mode mapping | Truthful shared state, one AACP writer |
| T29 | Old AirPods strategy and new adapter coexist | Only target adapted; no second SDK controller/global impersonation |
| T30 | Panel close/back/lock/rotation/MiLink restart | Correct focus/listener cleanup; control survives UI closure |
| T31 | Xiaomi native comparison when hardware connected | Compare layer/gesture/animation/density; not an AirPods test substitute |
| T32 | Firmware/MiLink/quick-connect resource update | Diagnosable mismatch; no dependence on permanent cloud-config tampering |
| T33 | Battery/mode updates and control recovery in one audio connection | No new/extended/reappearing island and no ordinary persistent notification; cleanup only with proven ownership |
| T34 | No setting report, default display, manual choice, failed send | Clear provenance; display does not write; failure feedback/rollback; reports win |
| T35 | Conversation-volume default/choice and route change | Preserve policy; restore only on the correct route |
| T36 | R01 root denied, unknown host, PID reuse/identity mismatch | Explicit refusal, no wrong action or name-based bulk fallback |
| T37 | R01 UI hosts and controlled Bluetooth restart | Selected allowlist only; UI restart leaves control intact; Bluetooth impact disclosed |
| T38 | R01 restored host with old/absent module or timeout | No false success, endless retry, or system-core/reboot escalation |
| T39 | Glass visible/static/background/motion disabled | Readable/operable; no background rendering wakeups; effects optional |
| T40 | New app ID, authorities/actions/notifications/JNI self-checks | Consistent installation identity with retained namespace; no cross-talk/double control with HyperPods |

All active-scope cases remain release requirements. T24/T25 and the numeric lid-popup/dedup gates below are dormant while M2 is deferred, not passed. T27 and the dedicated-panel requirements remain mandatory. T31 now has user-reported Xiaomi connection behavior recorded, but no broader device matrix is implied.

Localization is cross-cutting: run relevant page, accessibility, permission, error, and export cases in **English and Simplified Chinese**, including fallback behavior; no additional app locales are in scope.

## 6. Candidate quantitative release gates

The user requested strong background optimization, not individual contractual acceptance of these numbers. Preserve them as engineering targets; measure/calibrate after M1 and record any justified adjustment. Never relax thresholds merely to make tests green or wait for a new questionnaire before starting.

| Metric | Proposed threshold | Measurement |
| --- | --- | --- |
| Cold control connection | **48/50** Ready within **15 s** | Start at system target-device connection; retain every failure/outlier |
| Warm control recovery | **P95 <= 8 s** | Audio remains connected; control-only reopen; **>= 30** trials |
| Readable mode change | **>= 99/100** matching reports; successful **P95 <= 1.5 s** | User request to valid report; absent/different reports fail; not a unique ACK guarantee |
| Interaction feedback | **<= 100 ms** | Fake backend and physical device separately; feedback is not confirmation |
| Island dedup | **0** new connection alerts from same-connection state updates | Mix **>= 100** battery/settings/control-recovery events; no reappearance after ending |
| R01 correctness | **0** non-target processes acted on; **0** old-build false successes | Inject identity/allowlist/version failures; recheck restored hosts |
| Background glass UI | **0** background animation/render tasks | Inspect scheduling/render traces after backgrounding, not just visibility |
| 24-hour stability | **0** product-caused Bluetooth/SystemUI crashes; **0** recovery-caused audio disconnects | **>= 50** lid/connection transitions; correlate process/audio events |
| Resource cleanup | Own sockets/listeners/timers released within **5 s** of disable | Cached process need not disappear; no lingering WakeLock |
| Lid-discovery power | No added permanent high-frequency scan/WakeLock | Compare system-scan baseline; disclose all new scanning |
| Lid popup | **48/50** correct; event-to-first-frame **P95 <= 1.5 s** | Real headphones, aligned video; also record physical-lid-to-event delay |
| Lid dedup | At most **1** window per cycle | **50** cycles plus repeated-ad/manual-dismiss cases; first validate cycle boundaries |
| Interconnectivity panel | **50/50** clicks reach dedicated panel; first-frame **P95 <= 500 ms** | Separate cold/background results; unknown placeholders allowed, fabricated values forbidden |
| Added standby drain | Over **8 h**, <= **1 percentage point** of battery capacity above enhancement-off | Same phone/environment, alternating A/B **>= 3 pairs**; all readings and median difference, charge-counter/system corroboration |
| UI accessibility | **320 dp**, fonts **1.0/1.3/1.6x**, no essential clipping | **2.0x** reflow remains operable; actual page screenshots |
| Privacy/release | **0** raw MACs/serials/keys in default report; matching release certificate | Fixed-field export tests and `apksigner` |

Integer battery readings with excessive variance cannot prove a sub-1-point result; extend observation or use more precise charge-counter data. Do not combine different system signatures/background policies into one measurement group. Product-level targets also require touch areas >=48 dp and ordinary-text contrast >=4.5:1.

## 7. Risks

| Risk | Impact | Mitigation / trigger |
| --- | --- | --- |
| Unverified AirPods 5 fields/advertisements | Wrong identity, lid, capability, art | Actual AirPods 5 tests, not an older model substitute |
| Old system model lacks new capability | Recognition works, UI/control wrong | Verify discovery, state, UI, and write contract end to end |
| API 102 forbids legacy | Metadata-only migration fails | Remove legacy/YukiHook generation; inspect final DEX |
| Native/hidden APIs change with OTA | Control failure or host crash | Build ID/signature matching, reject unknown, rescue rehearsal |
| Framework incompatibility | Missing injection/system faults | Pin tested combination; no automatic replacement |
| Unreliable/changed reports | False confirmation/wrong state | Separate command states, fixtures, firmware-specific gating |
| Competing controllers | Hard-to-reproduce failures | Controlled isolation; no automatic uninstall |
| HyperOS background policy | Control loss when locked | Host-lifecycle tests; validate CDM/FGS only if such a route is actually introduced |
| Private card layout changes | Clipping or unrelated notification damage | Separate adapters/switches, ownership filters, failure fallback |
| Unclear license/assets | Distribution cannot comply | Provenance inventory, original art, pre-release review even while private |
| Signing loss/leak | Lost upgrade identity/impersonation | Encrypted container, separate password custody, offline backup/recovery test |

## 8. Documentation acceptance

Facts, upstream claims, proposals, and unverified scope must remain distinct. Local links resolve within the imported repository; external code evidence is pinned where available. No sibling-workspace links, secret content, unauthorized public intent, or future work described as completed.

This documentation pass does not run product tests, generate APKs/keys, change phone configuration, or publish anything. Extracted system APKs from prior read-only investigation remain outside source/deliverables. Add actual milestone evidence only after the corresponding work executes.

## 9. Measured baseline checkpoint

Recorded: **2026-10-22**, from results supplied by the implementation coordinator. These are current baseline results, not the historical upstream UI runs in [design-qa.md](../design-qa.md), and not acceptance of the in-progress API migration or R01.

| Check | Measured result | Boundary |
| --- | --- | --- |
| Build environment | Cached **Gradle 8.13**, **JDK 17 launcher**, cached **JDK 22 toolchain** worked | Initial wrapper distribution download timed out; no code failure was attributed to that timeout |
| JVM suite | **331 tests passed** | Baseline only; rerun applicable tests after migration/restart integration |
| Android Lint | **0 errors, 112 warnings, 2 hints** | Warnings/hints remain; not a clean-warning claim |
| Debug packaging | Debug APK built | Does not prove installation, module injection, release signing, or hardware control |
| Native safety tests | Boundary, address-reference, and ambiguous-target tests **passed** | Compiled with **NDK 27** as a standalone **ARM64 Android executable**, run on the explicitly selected phone; temporary executable removed |
| Framework daemon | Read-only status **verified `apiVersion:102`** with `daemonRunning:true`, version `2.2.1 / 7912`, `developerMode:false`, `safeMode:false` | Actual P.O.D.S. host injection and new API calls remain unverified; the debugging CLI is not an app integration protocol. See [recorded evidence](07-device-and-native-integration.md#read-only-daemon-status-follow-up) |
| APK naming | `PODS-<version>-<sha256-prefix12>.apk`; **5 Node tests passed** | Naming behavior, not API/runtime verification |
| CI configuration | `main`/private workflow limited to debug tests and artifact upload; `contents: read`; no secrets restoration or release publishing | Configuration changed locally; no remote CI run is claimed |
| Repository | Local history preserved; remote remains **private and empty** | No push, release, or visibility change |

Standalone native test execution validates the exercised safety logic on the phone; it does **not** prove framework native-entry loading, a live Bluetooth-library patch, or AirPods interoperability. API 102 migration, runtime injection, R01 new-build loading, real AirPods 5 protocol/lid behavior, native panels, Liquid Glass, and release acceptance remain open.

These results do not identify a final migrated artifact or its certificate/hash. Record those identifiers with the subsequent artifact-specific verification rather than assigning the baseline results to later changes.

### Integration follow-up: implementation, not runtime acceptance

Later on 2026-10-22, the implementation coordinator reported:

- Debug application ID is isolated as `com.hxlh50k.pods.debug`; release remains `com.hxlh50k.pods` and unsigned. The user-visible module title is P.O.D.S.; English/Simplified Chinese per-app locales are implemented.
- Legacy `assets/xposed_init` and `assets/native_init` have been removed; only modern module entries remain.
- Root-assisted **UI-host-only** restart UI is implemented for authenticated `com.android.systemui` and observed `com.xiaomi.bluetooth:ui` only. Each exact PID is revalidated; the replacement PID and current `BUILD_INSTANCE` must be confirmed before proceeding to the next host.
- **Bluetooth-inclusive restart is explicitly disabled in this milestone**, pending a real control quiesce/recovery handshake. This restricted implementation does not complete R01 or establish successful on-device restarting.
- A read-only official manager UI check additionally showed API 102 active. This corroborates the framework status, not P.O.D.S. injection or successful new API calls.
- The wrapper now bootstraps `8.13-bin` with the environment's existing proxy applied process-locally, and the root wrapper network timeout is **60 seconds**. The earlier timeout remains a historical download failure; no global proxy change or integrated-build pass is implied.

At that implementation checkpoint, integrated build/tests and code review were still pending. The later results below supersede that pending status only for the checks explicitly recorded; earlier test counts must not be attributed to later fixes. Hardware gates, unsigned-release status, and the **private, empty remote** remain unchanged.

### Pre-commit verification and cancellation-fix follow-up

Recorded later on **2026-10-22**, from implementation-coordinator results. The integrated run below occurred **before the subsequent restart-cancellation fixes**; it is not a final post-fix acceptance result.

| Check | Recorded outcome | Boundary |
| --- | --- | --- |
| Integrated wrapper run | `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:assembleRelease` **passed** | Applies to that tested integration, not automatically to later fixes |
| JVM suite | **375 tests, 0 failures** | Distinct from the earlier 331-test baseline |
| Android Lint | **0 errors, 118 warnings, 2 hints** | Warnings/hints remain |
| APK-naming suite | **5 Node tests passed** | Does not validate module runtime behavior |
| Certificates | Debug and AndroidTest APKs share the development certificate; release is **intentionally unsigned** | Release assembly success is not release-signing completion |
| APK inspection | ZIP inspection confirms modern-only metadata/entries; manifest inspection confirms application IDs, launcher, and locales | Artifact inspection does not prove host injection |
| Phone installation | Shell `adb install` was rejected by HyperOS with `INSTALL_FAILED_USER_RESTRICTED: Invalid apk`; authorized root `pm install` succeeded | No security settings were altered |
| On-device app tests | **2 passed**: identity/launcher and English/Simplified Chinese resources | These app tests do not verify injected hosts, AirPods control, or R01 execution |
| Framework manager | Recognizes **P.O.D.S. / API 102** and enables only the three declared scopes: `com.android.bluetooth`, `com.xiaomi.bluetooth`, `com.android.systemui` | Actual host injection remains pending bootstrap; enabling scopes is not evidence of injection |

Review then identified unsafe cancellation handling around a blocking `su` command. The implementation was changed to use `runInterruptible`, with **2 deterministic regression tests**; the targeted **19 ScopeRestart tests passed**. A further integration change persists an **unknown outcome** when verification is canceled, so an interrupted check cannot be silently reported as success.

At that checkpoint, final post-fix build/runtime verification was pending. The subsequent full-build result below supersedes the build-pending status, but not the open runtime gates. Do not sum the earlier 375-test run and targeted results into a new suite count. Bluetooth-inclusive restart remains disabled pending a real control quiesce/recovery handshake.

### M0/M1-A development preview handoff

Latest direction on **2026-10-22:** hand the deployed initial APK to the user for **manual testing**. Corrected deployment is now complete as recorded below. Do not continue fully autonomous runtime testing or upgrade dependencies just to bypass the UI-test blocker during this handoff.

| Check | Latest evidence | Boundary |
| --- | --- | --- |
| Final post-fix full build | **377 JVM tests, 0 failures**; Debug, AndroidTest, and intentionally unsigned Release assembly **passed** | This supersedes the earlier pre-fix build result, not hardware/runtime acceptance |
| Final Lint | **0 errors, 119 warnings, 2 hints** | Warnings/hints remain |
| Restart regressions | **19 ScopeRestart tests passed**, including **2 deterministic cancellation regressions** | Not an on-device R01 execution result; do not add these to the 377 total |
| Identity/locales on phone | **2 tests rerun and passed on the corrected deployment**: identity/launcher and English/Simplified Chinese resources | Supersedes the earlier pre-fix-only evidence for these surfaces; not proof of hook/control behavior |
| AboutPage UI tests | **6 attempted, blocked** during inherited Espresso bootstrap by `NoSuchMethodException: android.hardware.input.InputManager.getInstance` on Android 17 | Failure occurred before app assertions; **none may be reported passed** |
| Source | Local implementation commit **`5013f04eebaf373e1539b09f37ca5dfa84081182`** | Read-only `git rev-parse HEAD` confirmed the identifier; no remote push |
| Rebuild | Debug and AndroidTest rebuilt successfully from that commit in **49 seconds** | Separate from the preceding full-suite run |
| Deployment | Corrected debug and test APKs installed through authorized root `pm install`; staged files removed | No security settings altered; manager enables only `com.android.bluetooth`, `com.xiaomi.bluetooth`, and `com.android.systemui` |
| Version / identity | **`0.1.0-alpha.1`**, versionCode **`1`**, debug ID **`com.hxlh50k.pods.debug`** | Release ID remains `com.hxlh50k.pods`; release intentionally unsigned |
| APK integrity | On-device `base.apk` SHA-256 **matches** the local debug APK: `d8812be615917db1a353bcced6743a0c994c49b632bb24d52fd570043672cbab` | Identifies the corrected installed artifact |
| Launch / visual smoke check | MainActivity cold launch **`Status: ok`**, **289 ms**; screenshot visually confirms P.O.D.S. waiting for AirPods | App launch only, not injected-host or earbud validation |

No host-process restart or earbud interaction has been performed. P.O.D.S. host injection, native controls, and live R01 signals remain **unverified**. Native lid popups, Liquid Glass, the dedicated headphone panel, and Bluetooth-inclusive restart are not implemented in this preview; full R01 and MVP acceptance remain open.

**Manual handoff:** the user should first restart the phone once so scoped hosts can load the module, then connect AirPods 5 and inspect diagnostics/device identity/control readiness before trying supported basic controls. The restart is a requested user step, not an operation already performed or a guarantee that injection succeeds. Further autonomous runtime testing is deferred to the user's manual feedback; all hardware gates remain intact.

The remote remains **private and empty**, with **no push**. This documentation update records the existing local implementation commit and deployment receipt; it creates no commit, release, or additional artifact.

**Follow-up test tooling (2026-10-22):** the subsequent development request permits a test-only fix for the Android 17/API 37 Espresso bootstrap blocker. Pin `espresso-core:3.7.0`, `runner:1.7.0`, and `ext:junit:1.3.0`; keep Compose UI tests at `1.9.4` and production dependencies unchanged. The [official stable 3.7.0 release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0) explicitly replace reflective `InputManager.getInstance` with `getSystemService`; the [published sources](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/3.7.0/espresso-core-3.7.0-sources.jar) confirm the public lookup on API 23+. The `3.8.0-alpha01` preview is unnecessary for this specific failure. No phone tests were run for this tooling update: all six AboutPage cases still require runtime verification, and no earbud or system-host acceptance is implied.

## 10. Alpha2 notification and lid-observation iteration

The user reports basic alpha1 controls work and the inherited defects remain, as expected. At their request, normal branch history was reset to an identical independent P.O.D.S. snapshot (`781b4b6`); the previous history is backed up externally, source/license/provenance remain intact, and the private remote was not rewritten or pushed.

Implemented changes:

- One five-second connection presentation, tied to a real A2DP event and shared with late UUID discovery. Normal battery/settings/AACP refresh does not rearm it.
- Separate silent status updates, owner-scoped cancellation, and an explicit Xiaomi-main-process presentation guard. Existing ambiguous alpha1 notification tags are not blindly cancelled.
- Debug-only lid observations: opt-in nearby-device permission, a 30-second foreground scan, manual case markers, bounded records and redacted clear-header export including the raw lid byte. This prepares the actual AirPods 5 contract; it is not lid detection or a native popup.
- Test-only AndroidX updates to stable Espresso 3.7.0, runner 1.7.0 and ext.junit 1.3.0. Official Espresso release notes identify the removed reflective `InputManager.getInstance` dependency; production dependencies were not changed for this fix.

Validation before deployment:

- Integrated unit/build/Lint run: **404 JVM tests passed**, **0 errors / 120 Lint warnings / 2 hints**, debug/test/unsigned-release APKs assembled.
- The final clear-header correction then passed **14 focused observation tests** and rebuilt all three APK variants. Do not add those 14 to the earlier suite total.
- Modern API102-only packaging, isolated debug identity, EN/zh-CN resources and development signing were checked. BLE permissions are present only in debug, absent from release.
- Code review found no significant issues in the reviewed notification/observation changes. Actual island rendering, radio capture and lid semantics still require user checks.

### Alpha2 deployment and manual handoff

- Implementation commit: `03f18a5`, on the independent P.O.D.S. branch.
- Rebuilt debug and instrumentation APKs from that commit in 49 seconds, then installed them with authorized root `pm install` without changing system security settings or clearing app data.
- Installed version: `0.1.0-alpha.2` (code2), package `com.hxlh50k.pods.debug`.
- The installed APK SHA-256 matches the local artifact: `696ba0a17539fcb780c72e9a9609ba92f69c581dff504d9621467a8367d7ff7e`.
- Two non-UI phone checks (application identity/launcher and EN/zh-CN resources) passed.
- The updated test stack got past the old `InputManager.getInstance` failure. Five AboutPage UI cases passed; the first case failed with no Compose hierarchy. The lid-panel test stalled while the device lost foreground visibility and later slept/locked. The run was terminated by stopping only the verified test-app PID. This is not a successful complete UI run or a spontaneous product-crash claim.
- No BLE scan was started, no physical lid operation was automated, and no system/Bluetooth host was restarted. The phone remains subject to normal lock-screen authentication.

User checks: reload all three module scopes, normally with one phone restart; confirm the connection island ends after about five seconds and ordinary state refresh does not bring it back. For the next popup step, use **About → Lid observation (experimental)**, explicitly start the 30-second capture, mark closed/open/closed with a few seconds between actions, and copy the redacted report. Empty or failed observations can also be copied. This tool does not enable a native popup or assert that a nearby candidate belongs to the user.

The repository remains private with no push. Native lid popups, the dedicated Interconnectivity panel, Liquid Glass, and full Bluetooth-inclusive restart are still later work.

### Collaborative acceptance boundary

The user explicitly requested joint physical-world testing after the extended UI automation. Alpha2 implementation, local regression checks, deployment, and independent Git history are the completed deliverables of this iteration. Physical island timing and lid semantics are **not** marked accepted. Stop further device operations here; agree on one or two manual checks, let the user operate the phone/headphones, and investigate only the reported result. Lock-screen or focus blockers must not trigger another autonomous testing loop.

## 11. Alpha3: connection-only presentation

User feedback: the initial island briefly appeared, then an inherited-looking persistent island and ordinary notification took over. The alpha2 source still unconditionally posted a silent status notification for routine `updatepodsnotification` messages; a reminder error path could repost the same status. This was a product implementation gap, not evidence that the user had misconfigured the phone or another module.

Implemented correction:

- Delete routine status-notification creation and its error fallback, and stop normal case/battery report animations. In-app control/state reporting is unchanged.
- Only genuine audio connection events authorize the original five-second presentation. Available left/right/case readings may update its existing key during that window with no first-float replay or deadline reset. After expiry, ordinary updates post nothing.
- Keep explicitly configured low-battery warnings as separate bounded, deduplicated events, consumed only when submitted successfully.
- Retire active alpha2 namespaced entries and legacy candidates only with P.O.D.S. ownership evidence. Never delete a notification merely because it has a Bluetooth/headset tag. Another enabled HyperPods module remains independent and must not be silently disabled.
- Document silent Xiaomi paired reconnection versus discovery-first-pairing UI. First-pairing popup work is provisionally deferred, while the dedicated control panel remains required.

Focused local validation:

- **55 JVM tests passed** across connection policy, ownership, ordinary case suppression, low-battery batching/storage, and the requested prior scope-cancellation/interface regressions.
- `:app:lintDebug :app:assembleDebug` passed: **0 Lint errors, 117 warnings, 2 hints**. The development APK signature verifies.
- Selected review comment `a75635c4-cb97-415a-b67f-ae0a36b9f314` was an already-fixed earlier cancellation issue. Current interruptible root execution, process cleanup, and unknown-outcome reporting were verified and the thread resolved; no redundant source rewrite was needed.

Version: `0.1.0-alpha.3`, code3, isolated package `com.hxlh50k.pods.debug`. No ADB command, installation, scope restart, radio scan, or physical headphone interaction was performed for this iteration.

The next agreed manual check is only: install/reload the new version, reconnect once, and confirm both the battery island and ordinary status notification are gone after the brief window. If a residual entry remains, collect its displayed application identity and screenshot before attributing it to another module. Do not require new lid captures, unpairing or headset reset for this fix.

## 12. Battery specification after failed visibility test

The subsequent user test reports a brief flash, not a useful five-second island. Record the alpha3 presentation as failed; source tests and notification-post success do not settle visible behavior.

The new design requires:

- no inherited persistent island or ordinary status notification;
- left/right battery content visible for five seconds, independently colored using Low20/Medium50 by default;
- separate low/medium reminder switches and edge detection of genuine percentage decreases crossing a threshold;
- no threshold reminder from the first already-low reading of either ear;
- explicit per-ear/category deduplication, initial-data readiness, settings migration and system-renderer timing evidence.

The [specification](10-battery-notification-spec.md) distinguishes these confirmed requirements from proposed defaults (per-connection reminder budget, switch defaults, batching and data wait limits). Those defaults were chosen for a reviewable draft after clarification was unavailable, not represented as user-confirmed choices.

Before implementation, use the spec as the single acceptance contract. The next joint round after a code change should address only actual five-second visibility, followed separately by color and threshold semantics. No device tests or production-code changes were made while writing this design.

## 13. Alpha4: build-only battery specification implementation

Implemented:

- `BatteryNotificationSettings`: atomic valid Low/Medium pair, defaults20/50, independent reminder switches, per-ear red/yellow/green/unknown bands.
- Settings in both the connected controls and always-accessible About page, with preview, EN/zh-CN validation, explicit migration notice and private recovery of legacy settings.
- `BatteryNotificationSession`: fresh-ear-only baselines, downward crossings, per-connection category consumption, no initial-low alerts, no charging rearm, no hidden case/critical10% events.
- `BatteryNotificationHost`: real audio-session ownership, readiness limits, fixed350ms batching, active-card merging, bounded startup, separate settings/control generations and safe owned-notification retirement.
- `BatteryIslandRenderer`: native left/right template regions, independent colored battery indicators and percentages, no `dismissIsland=true` flag on creation/refresh, no ordinary persistent status card.
- `BatteryIslandVisibilityHook`: scoped native pre-draw observation, dual-ear visibility token and first-visible timestamp; five-second expiry starts on the acknowledgment rather than A2DP connection. If that lifecycle cannot be confirmed, record `visibility_unconfirmed` and cancel by watchdog. The actual ROM behavior remains unverified.

No unit tests, instrumentation tests, Lint, ADB, installation, scope restart, radio capture or earbud operation is performed. Build warnings and artifact metadata are recorded separately. The user's shutdown request is executed only after a successful package has been saved; no applications are forcibly terminated to make shutdown succeed.

### Alpha4 package receipt

- Implementation commit: `7052323`.
- Final command: `:app:assembleDebug -x :app:lintVitalDebug -x :app:lintVitalAnalyzeDebug -x :app:lintVitalReportDebug --console=plain`.
- Result: **BUILD SUCCESSFUL**, 9 seconds; compiler deprecation warnings remain. This is compilation evidence only, not a test result.
- Package: `dist\PODS-0.1.0-alpha.4-debug.apk`, 31,743,627 bytes, version `0.1.0-alpha.4` / code4, application ID `com.hxlh50k.pods.debug`.
- SHA-256: `56f11972974d97b898180b90f73f2184f15c74c16f422d1872a948dcc64a55ff`; a sidecar checksum file is stored with the APK.
- APK v2 development signature and modern API102 metadata inspected successfully. No new release key was created or reused from another product.
- Not installed. No test, Lint, or hardware acceptance is claimed. The native pre-draw signal, actual continuous visibility, independent colors and threshold behavior remain for a jointly agreed later test.
- The source and documentation are saved locally; no remote push. After saving this receipt, the requested non-forced computer shutdown can be scheduled with a short grace period.

## 14. Alpha5: priority full debug scope reload

The user withdrew shutdown instructions and reported that alpha4 still required a phone reboot to update module code. The previous UI-only action intentionally excluded the Android Bluetooth and Xiaomi Bluetooth main hosts; it could not satisfy full APK-update loading.

Implemented:

- A debug-only **Reload all module scopes** action, with a separate explicit process-exit confirmation; the lower-impact interface-only option remains.
- Old alpha4 host reports are eligible. The action does not require a new cooperative command inside already-running old module code, and it never fabricates a graceful-quiescence acknowledgment.
- Fresh authenticated reports for all three required main hosts, real PackageManager UIDs, idle call/audio state and root PID/UID/start identities are preflighted before the first signal.
- Sequence: Android Bluetooth -> Xiaomi Bluetooth main -> SystemUI -> optional observed Xiaomi `:ui`. Each target is revalidated immediately before a one-time signal, then its replacement must prove a new PID and the installed version/revision/build instance before progression.
- Providers may indirectly recreate later hosts; a verified current replacement is accepted without signalling it again. Lazy optional `:ui` is reported as awaiting normal launch, never counted as verified.
- Bounded application-owned progress survives navigation; explicit cancellation interrupts pending root work and records uncertain already-signalled outcomes. Busy/unknown call state, missing required hosts, stale modules or identity failures stop further signals. No system-core process, framework daemon, data wipe, package-wide force-stop or phone reboot is used.

Related presentation changes:

- Model-specific left/right earbud icons replace the battery gauges; unknown models use separate generic earbud silhouettes.
- Ordinary focus expansion is disabled independently of the island's initial animation.
- **The transport still uses a backing notification.** This does not yet implement the user's stricter pure charging-style native animation requirement. Public/source examples establish `strong_toast_action` as a different path, but not the complete safe dual-ear/update/cancel/visibility contract on this ROM. That work stays separate from the priority reload handoff.

Local validation: **46 focused JVM tests passed**, covering policy, root cancellation, full coordinator sequencing and failure/old-build handling. Debug assembly passed. No ADB command, installation, real root process signal, headset operation or computer power command was performed.

Next agreed manual check: install alpha5 without first rebooting, open module diagnostics, and while no call is active explicitly confirm full debug reload. Report the individual outcomes for the three required hosts. A successful signal or an old module in a replacement PID is not success. Framework cache or Zygisk/framework updates can still require separate framework action; the app reports that limitation rather than automatically rebooting or pretending all updates are reboot-free.

### Alpha5 local package receipt

- Implementation commit: `6e60f3a`.
- Focused result: **46 JVM tests, zero failures/errors** across core restart policy, cancellation, interface eligibility and the full reload coordinator.
- The committed debug APK assembled successfully in 11 seconds. No full suite, Lint, instrumentation or device execution was performed in this iteration.
- Handoff: `dist\PODS-0.1.0-alpha.5-debug.apk`, 31,800,951 bytes; development v2 signature, application ID `com.hxlh50k.pods.debug`, versionCode5, modern API102-only metadata.
- SHA-256: `bdc737642f0d8cdea8d5bcb43edcea5b6fc7e29af67d4fdcbdcc5342386e806b`; a matching sidecar file is saved with the APK.
- Not installed or executed on the phone. Real process restoration, framework cache refresh and native rendering remain user-led acceptance checks.
- No remote push or power operation. The old shutdown request remains withdrawn.

## 15. Alpha6: direct native island and original earbud artwork

The user reports full debug scope reload as effective. That implementation is unchanged in this iteration.

The alpha5 normal-notification-before-island behavior is addressed by replacing the transport:

- Xiaomi Bluetooth's battery owner now sends a bounded primitive payload to an authenticated SystemUI receiver; it no longer creates a notification or notification channel for presentation.
- The SystemUI bridge uses the statically inspected native device-island data/controller contract. No fake `StatusBarNotification`, forced charger state, or ordinary-notification fallback is used.
- Native updates paint only the existing owned ear regions without renewing or re-adding the animation. Pre-draw acknowledgment, startup/visibility deadlines, token revisions, cancellation and plugin-unload handling remain explicit.
- Known model artwork is loaded in its original colors and material; threshold colors are applied only through the native percentage-text appearance API. Saved view appearance, accessibility description and image state are restored on release.
- Ownership checks restrict native removal to the exact current data object and application key. Existing charging/device islands are not overwritten; mismatched native schemas fail closed.

Local validation: **11 focused JVM tests passed** (4 artwork cases and 7 payload/trust/lease cases) and debug assembly succeeded. No broad suite, Lint, runtime display, scan, restart, installation or earbud operation was performed. The only ADB work was read-only copying of system APKs for local static inspection; those binaries are not product artifacts.

Next joint check: install alpha6, use the now user-verified full debug reload, then reconnect once. Verify direct island appearance with no preceding ordinary notification/card and untinted pictorial ear images with independently colored percentage text. If there is no island or timing is wrong, report that first instead of running repeated automated trials.

### Alpha6 package receipt

- Committed source: `4bae5498db014330124cd1eaddb739f79ebc5e4f`.
- File: `dist/PODS-0.1.0-alpha.6-debug.apk`; **31,850,099 bytes**.
- SHA-256: `318646455db71e7e9165ea8334ec1b9ff3373f6844c033534fcbbddc3f56e3c9`.
- Identity verified from the APK: `com.hxlh50k.pods.debug`, display name `P.O.D.S.`, version `0.1.0-alpha.6`, code `6`.
- Modern API102 Java entry/scopes and absence of the legacy Java entry verified; the committed source revision is present in packaged DEX.
- APK v2 signature verified with the existing development certificate. This is not the independent production signing key.
- Final debug assembly from committed source passed. The 11 focused local regressions above are not substitutes for native rendering acceptance.
- Temporary system APK/decompiler analysis directory removed after recording contract evidence. No APK, decompiler output, private key or device log was added to Git.
- Not installed or exercised on the phone; no remote push and no power operation.

## 16. Alpha7: repair lazy native island initialization

The user performed the agreed DEBUG scope reload and lid-open sequence. A persistent module-only collector captured:

1. Xiaomi Bluetooth's battery owner registering normally.
2. SystemUI rejecting content in `NativeBatteryIslandSchema`'s concrete-controller class check.
3. A genuine audio connection and battery IPC, followed by `native_failed`.

The supplied OEM content is a lazy proxy, not the actual controller. Alpha7 accepts only the verified OEM proxy/direct-controller shapes, resolves the current controller when needed and creates the native window only on a real presentation. It also removes the pre-add positive-width requirement: native add must first request geometry. The existing startup lease bounds a single owned pending entry, and cancellation balances that entry without affecting foreign content.

Local validation: **23 focused JVM tests passed** (7 endpoint, 9 payload/lease/trust, 3 pending-identity cleanup, 4 artwork) and debug assembly succeeded. These tests validate the production resolver against independent reflection fixtures, deadlines and ownership; they do not prove actual native animation/visibility on the phone.

For subsequent physical rounds, start and verify the log collector before handoff, state the exact one-round operation and return control. Do not use an interactive questionnaire for this physical-operation handoff. The user then reports completion in chat. Keep captured raw logs and APK analysis inputs out of Git.

### Alpha7 package receipt

- Committed source: `d3227eec1b8ad78365086d69701db823eae8a1db`.
- File: `dist/PODS-0.1.0-alpha.7-debug.apk`; **31,850,103 bytes**.
- SHA-256: `32afa88fde3922839829ff8d54eb402785a7e4a670e1c1c1d194c3284b9224bc`.
- Verified APK identity: `com.hxlh50k.pods.debug`, `P.O.D.S.`, `0.1.0-alpha.7`, version code `7`.
- Verified development v2 signature, modern API102 metadata, packaged endpoint implementation and committed revision. Independent reflection test fixtures are not packaged.
- Final debug assembly from committed source succeeded. The preceding 23 focused tests all passed.
- Both extracted OEM APK hashes still match the evidence recorded in document 07. Named temporary APK/disassembly files were removed; the real diagnostic capture remains local and uncommitted.
- Alpha7 has not been installed or accepted on the phone. The captured failure belongs to alpha6; a separate user-led installation/reload/reconnect round is needed to validate this fix.
- No remote push, autonomous process restart, earbud operation or power action.

### Alpha7 user-operated runtime verification

The collector saved the user's installation/reload/reconnect round on 2026-10-23:

- `09:13:02.227`: SystemUI accepted the actual OEM lazy content endpoint.
- `09:13:13.900`: the next audio connection reached the connected state.
- `09:13:15.213`: the real native controller was bound.
- `09:13:15.244`: native island data was submitted.
- `09:13:15.760`: the owned dual-ear content passed the actual pre-draw visibility checks; Xiaomi Bluetooth acknowledged visibility at `09:13:15.764`.
- `09:13:20.760`: SystemUI ended the island through `native_visible_timeout`; the host ended its presentation at `09:13:20.764`.

The observed visible interval was exactly **5,000 ms**. This verifies the requested missing-island repair through the real native display lifecycle, rather than merely successful IPC or compilation. An earlier reconnect during scope reload had reached the previous bridge before the new SystemUI process was ready; it is not the post-reload verification result.

The collector has exited. No additional physical round is required for this repair. This record does not claim subjective artwork/animation approval or execution of every battery threshold/edge-case acceptance test. The verified APK, source revision and checksum above are unchanged; this follow-up is documentation only.

## 17. Preliminary acceptance and one-day usage freeze

On 2026-10-23 the user confirmed preliminary validation passed, chose a one-day primary-phone trial, authorized pushing the current source and explicitly prohibited further functional changes.

Freeze baseline: alpha7/code7, source `d3227eec`, with the exact APK/hash in section 16. Do not rebuild, replace, deploy or alter that APK merely to publish the repository. All current changes are documentation only.

Confirmed so far:

- Basic preview functionality and full DEBUG scope reload are user-confirmed.
- The missing native-island defect is repaired and the user-operated runtime round reached actual dual-ear visibility with exactly five seconds before normal expiry.
- The latest 23 focused local regressions and package/signature/API102 checks passed.

Implemented but not exhaustively accepted: threshold colors, independently configurable Low/Medium reminders, downward-crossing suppression/budgets, single-ear/unknown readings, long idle recovery and phone-side interaction with inherited controls.

During ordinary use, useful observations are connection/dropout behavior, SystemUI/Bluetooth stability, audio/call recovery, persistent or repeated unwanted surfaces, reminder behavior at naturally occurring thresholds, and unusual power use. Do not force battery depletion or require repetitive test cycles. Primary-phone use extends validation beyond the specific test-phone contract; report model/ROM context only if needed for a failure.

Deferred work remains unchanged: the dedicated Interconnectivity headphone panel, Liquid Glass UI, broader acceptance and independent production signing/distribution. First-pairing discovery popup work is provisionally deferred; rootless and legacy-framework support are not active workstreams.

Publication constraints: private `HXLH50K/PodsOnDroid`, independent `main` ancestry rooted at `781b4b6`, no force push, no imported HyperPods commit ancestry, and no raw logs, extracted OEM APKs, signing keys or local APK binaries in Git. Existing CI may run after the push; it is not permission to resume feature work.

## 18. Alpha8 Interconnectivity preview

The user resumed development and clarified the desired flow: card -> small
headphone panel -> More settings -> app. They authorized choosing an appropriate
implementation rather than requiring a particular native service, and requested
plain handoffs instead of interactive question forms.

Implementation:

- Only a Bluetooth-UID-authenticated identity reply matching the bound card may
  replace its artwork. Model-specific original images are loaded from module
  resources. Binding tokens reject replies for recycled holders; requests expire
  after 500 ms and are bounded to eight. The inspected OEM asynchronous icon
  callback cannot overwrite owned artwork; reuse restores the original tint.
- Existing authenticated click routing now opens an app-owned floating Activity
  in a separate task, not MainActivity. More settings opens the normal app task.
- The panel reuses battery/artwork/capability components and the existing single
  Bluetooth command writer. Case artwork is visible even when its battery is
  unknown; unknown values are not replaced with zero.
- Mode highlighting comes from reported diagnostics, not optimistic broadcasts.
  Unsupported/unready/pending controls are disabled. Failed or unconfirmed
  changes remain explicit, with a 6.5-second local response deadline.
- Back, outside touch, Close and backgrounding end the panel. No root shell,
  MiLink permission bypass, new module scope, native SDK writer, or volume
  manipulation was added. Native MiLink reuse and volume controls are not
  claimed as completed.

Validation:

- 22 focused JVM tests passed: 7 panel mode-state, 8 AirPods 5 support, 7 settings.
- Debug APK assembly and development signature verification passed.
- Preview: `dist/PODS-0.1.0-alpha.8-debug.apk`, application ID
  `com.hxlh50k.pods.debug`, version code 8. SHA-256:
  `2d0f9302b1be0b3cab0d855dd9b7d7bdfe6569ecbc37ea07fd826592bc2e5221`.
  Built from the uncommitted working tree based on `75a404c`; the embedded base
  revision alone does not identify these changes. No commit/push occurred during
  preview packaging; the later source-only transfer checkpoint includes them.
- An additional controller suite has 10 failures among 64 tests. The identical
  failures reproduce on pristine `75a404c`: mock Bluetooth addresses are null at
  notification helpers. They are not caused by this preview and were not fixed.
- Artwork rendering, floating-window geometry/lifecycle and actual headphone
  controls still require a user-operated test. Compilation is not acceptance.

Environment and deployment blocker:

- Official ADB 37.0.1 installed in the per-user Android SDK; user PATH updated.
  JDK 22.0.2, SDK 36, Gradle 8.13, CMake 3.22.1 and NDK 27 restored.
- Current attached device: `pandora`, `OS4.0.4.0.XBLCNXM`, API 37. All device
  access used ordinary UID 2000 shell; no `su` or `adb root`.
- Installed alpha7 signer SHA-256:
  `092596c4c2e3b35ed33c7f09c2ce96d2dfdf8ed24989c2813612598976acd8c0`.
  New computer's debug signer SHA-256:
  `e279b440e547dd2df2f6da8da61d3b74cd42d539d44e4059e38226dd42157c98`.
  Android will reject an in-place update. No install/uninstall was attempted.
- Preferred next step: the user securely restores the previous development
  signing key locally (never in Git/chat), then rebuild with that identity.
  Uninstalling alpha7 would lose app data and require module setup again; that
  requires explicit user authorization and is not an automatic workaround.

After signing continuity is restored and the user performs installation and
module reload, use one agreed round: (1) inspect the card icon and open/close
the small panel, then More settings; (2) with AirPods 5 connected, compare live
batteries and change one supported mode, then restore it. Do not manipulate
the earbuds, restart hosts or automate additional rounds without agreement.

### Cross-machine transfer checklist

At 16:35 on 2026-10-23 the user requested saving and pushing the current progress
for continued work on another computer. **Testing is paused**, not failed or
accepted. This transfer does not rebuild the preview, operate the phone, change
signing identity, or publish a binary release.

1. Clone/pull private `HXLH50K/PodsOnDroid`, branch `main`. Preserve independent
   history and the existing source/license attribution. No force push is needed.
2. Configure this identity in the new clone; Git's repository-local settings do
   not travel with commits:

   ```powershell
   git config --local user.name "HXLH50K"
   git config --local user.email "hxlh50k@gmail.com"
   ```

3. Restore JDK 22, Android SDK 36, build-tools 35.0.0, CMake 3.22.1 and NDK
   27.0.12077973. The checked-in wrapper selects Gradle 8.13. Configure
   `JAVA_HOME` and `ANDROID_HOME` locally; do not copy this computer's absolute
   paths or commit `local.properties`.
4. Securely restore the development signing key used for installed alpha7.
   Compare its certificate digest with the installed-alpha7 digest above before
   any update. A newly generated debug key is not an upgrade-compatible
   replacement. Never commit or send key contents in chat.
5. The source, resources, tests and this handoff travel through Git. The APK
   under `dist`, SDK/JDK caches, local test logs, credentials and signing keys do
   **not**. Temporary extracted system APKs and decompiler files were removed.
   The recorded alpha8 APK hash belongs only to the pre-commit build on this
   computer; a later rebuild has a different build instance and may have a
   different signing certificate/hash.
6. When the user resumes validation, the focused local command is:

   ```powershell
   .\gradlew.bat :app:testDebugUnitTest --tests '*PanelModeStateTest' --tests '*AirPods5SupportTest' --tests '*PodsSettingsTest' :app:assembleDebug --console=plain
   ```

   Expect 22 tests for these selectors. Do not describe the wider controller
   suite as passing; the 10 pre-existing mock-address failures are documented
   above. New code does not yet have on-device rendering/control evidence.
7. After signing continuity and explicit user readiness, perform only the two
   collaborative checks above. No root shell is needed for normal installation
   or read-only diagnostics. Do not automatically restart scopes or operate
   headphones. Future requests for input should use a plain task-completion
   handoff and stop, not an interactive questionnaire.

## 19. Alpha8 original-workstation build and deployment

The user returned to the original development workstation and explicitly requested
pulling the latest source, catching up with the handoff, packaging and deployment.
Clean `main` was fast-forwarded from `75a404c` to
`8ad93c7b771d43d8f6b1ece69b51b204c4035489`. The private repository and independent
history were preserved. No functionality, dependency or workflow was changed.

The preview remains an app-owned Interconnectivity headphone panel, not a claim
of native MiLink integration. Read-only inspection confirmed the incoming changes
and the other workstation's signing blocker before proceeding.

### Local verification

- **45 focused JVM tests passed:** PanelModeState 7, AirPods5Support 8,
  PodsSettings 7, NativeBatteryIslandEndpoint 7, NativeBatteryIslandPayload 9,
  NativeBatteryIslandQueue 3 and BatteryEarbudArtwork 4.
- Debug assembly from committed source succeeded. No full-suite or Lint success
  is claimed for this build. The upstream CI run remains failed in the already
  documented broader controller tests; those unrelated fixture failures were
  not changed as part of packaging/deployment.
- Package: `com.hxlh50k.pods.debug`, display name `P.O.D.S.`,
  version `0.1.0-alpha.8`, code `8`.
- Artifact: `dist/PODS-0.1.0-alpha.8-debug.apk`, **31,883,723 bytes**.
- SHA-256: `4275348421f68bc15a191d68c8ec01644861dcd6ce370ef7521153c5e05eef8d`.
- API102 metadata, absence of the legacy Java entry, packaged panel Activity,
  embedded committed revision and APK v2 signature were checked.
- Installed alpha7 and rebuilt alpha8 both use development signer SHA-256
  `092596c4c2e3b35ed33c7f09c2ce96d2dfdf8ed24989c2813612598976acd8c0`.
  The other workstation's signer mismatch is not present here. No key was
  exported, generated, printed or committed.

### Deployment evidence and acceptance boundary

- Target was the sole connected `houji` test phone, not the `pandora` phone used
  on the other workstation. Existing permission for root package-manager
  installation on this ROM was used; no device security setting was changed.
- `pm install -r -t` returned `Success`; no uninstall or data reset occurred.
- Installed package reports alpha8/code8 and update time `2026-10-23 17:57:57`;
  the original first-install time remains `2026-10-22 21:25:55`.
- SHA-256 of the installed base APK exactly matches the artifact above.
- The `show_headphone_panel` action resolves to
  `moe.chenxy.hyperpods.HeadphonePanelActivity` without launching it.
- Exact temporary device-staging and local comparison APKs were deleted.
- No UI launch, scope restart, phone reboot, audio-mode command, earbud operation
  or computer power action was performed. Existing injected host code may remain
  alpha7 until the user performs the full DEBUG scope reload.

Next user round: after full DEBUG scope reload, (1) inspect the Interconnectivity
AirPods artwork, open/close the panel and try More settings; (2) compare actual
battery readings and, if a listening mode is supported and ready, change one
mode and restore it. Start and verify a bounded module-log collector before
handoff. Unknown readings should stay unknown. Successful package installation
and action resolution do not establish artwork/control acceptance.

## 20. Alpha9 reference-style panel and native reuse boundary

User feedback: alpha8's floating entry works, but its UI differs too much from the
Xiaomi headphone panel. The user-selected screenshot was read locally from the
phone; it is not bundled into the app or committed.

### Native feasibility evidence

The currently installed MiLink is `18.2.2.1.2609281717`, SHA-256
`6c05c0566ae696fd2594ec03d4e700050e0eaa2837fe3f16c61228c451143652`.
Targeted static inspection located
`com.miui.circulateplus.world.headset.HeadsetDetailFragment` and `HeadSetsDetail`.
The fragment requires `CirculateServiceInfo` plus device models, lifecycle/service
injection and `HeadsetServiceController`; the view also owns controller listeners
and state-dependent operations. This is not a standalone Activity accepting
P.O.D.S. battery values. A presentation-only adapter with authenticated target
identity and exclusive P.O.D.S. control has not yet been established.

Alpha9 therefore uses the existing approved preview route, not a counterfeit
native route. Native MiLink remains preferred future integration work, not
declared infeasible or completed. No OEM code/resources were copied into the
project; no privileged permission bypass, global device-type change, additional
Xposed scope or competing headset SDK writer was introduced.

### Implemented preview

- Window width follows the reference's approximately 84% viewport proportion,
  capped at 392 dp; height is bounded by available system-bar/cutout-safe space.
- A rounded native window background uses cross-window blur plus behind-window
  blur. Runtime blur-disable changes use an explicitly logged readable opaque
  surface instead of claiming a fake blur effect.
- Title, compact left/right/case battery row, charging indicators, large original
  product image, rounded monochrome supported-mode controls and More settings
  replace the compressed full-app dashboard. Unknown batteries stay unknown;
  retained readings are identified. Product artwork remains untinted.
- Real phone media volume is shown in a native SeekBar. Binding/polling does not
  change it; only explicit touch/accessibility/key adjustment writes STREAM_MUSIC.
  The platform's actual readback wins over requested levels, including Android
  volume limits. Fixed/unavailable volume is disabled/reported. No call/ring
  volume, remote volume protocol, test sound or find-device ringing was added.
- Mode selection still comes from genuine headphone reports, never a tap alone.
  Failure/pending states remain visible. Back, outside touch and the close
  affordance dismiss; backgrounding ends the panel as before.
- The alpha8 capture identified a real `DeviceCardArtworkHook` failure:
  plugin `applicationContext` can be null. Both device-card receivers now use
  an actual SystemUI package context; authentication is unchanged.

### Local verification and physical boundary

28 focused JVM cases passed: 4 SystemUI context, 4 panel sizing, 6 media volume,
7 mode-state, 4 untinted artwork and 3 state-freshness cases. They cover the
captured null-context shape, invalid metrics, bounded/narrow geometry, no volume
writes during reads, fixed/range refusal and actual volume readback. Android
Lint passed with no errors; unrelated existing warnings remain.

These are not screenshot/visual acceptance tests. No UI automation, scope
restart, mode change, volume change or earbud operation was performed by the
assistant. The next user round checks the glass layout and preserved artwork,
then checks the controls only if the user chooses. Do not report this preview
as an accepted native MiLink implementation.

### Alpha9 package and deployment receipt

- Source: `e94dde4bc720f5d137b92abb621f4dbd2972567b`.
- File: `dist/PODS-0.1.0-alpha.9-debug.apk`; **31,935,551 bytes**.
- SHA-256: `b984c961d0e783171527589bd92e0faa0eb1b51e0193d49ee5c12af12f2bdbb6`.
- Identity: `com.hxlh50k.pods.debug`, `P.O.D.S.`, alpha9/code9.
- Final committed-source build and all 28 focused tests passed. Final Lint:
  **0 errors, 136 warnings, 2 hints**; broader existing controller-suite failures
  were not part of this targeted run and are not claimed fixed.
- Development v2 signature matches alpha8; packaged revision/API102 metadata
  and the unchanged original three scopes were verified.
- Root package-manager in-place installation returned `Success` on `houji`;
  installed base-APK hash matches the artifact. Update time:
  `2026-10-23 18:18:20`; original first-install time remains unchanged.
- The normal `MODIFY_AUDIO_SETTINGS` permission is granted. No media-volume
  command, UI launch, scope restart or earbud operation was executed.
- Task-specific MiLink APK/disassembly files and device install staging were
  removed. Reference image and diagnostic capture remain local, outside Git.
- Start a verified 20-minute collector including `PODS-Panel`, `PODS-Battery`
  and `Art_Chen` before handoff. The user reloads scopes, opens the panel and
  judges layout/glass/artwork; control interaction is user-operated. Native
  MiLink reuse is still explicitly unimplemented, not silently claimed.

## 21. Alpha9 acceptance, known control defect and M4 app prototype

The user accepts the current floating-panel function and basic appearance,
while noting remaining aesthetic roughness. The alpha9 capture confirms native
window blur was enabled when the user opened the panel. It also contains old
SystemUI-PID artwork errors; installation alone is not evidence that the new
hook was loaded. Do not claim fresh artwork-hook acceptance from those logs.
The collector has been stopped; no repeated device round was initiated.

### Open high-priority issue: noise control with earbuds in the case

Tracked in private [issue #1](https://github.com/HXLH50K/PodsOnDroid/issues/1).

- The user reports noise-control changes fail while earbuds are in the case.
- The full app and floating panel are both affected; this is not panel-only.
- Removing both earbuds reportedly allows switching.
- Lid position, exact mode pair, single-ear result and control readiness are
  unrecorded. Root cause is unknown: do not assert a Bluetooth, UI or firmware
  explanation without evidence.
- The user explicitly requested recording this issue and continuing the next
  milestone. No control-path workaround, gating change, forced command, mode
  write or headphone operation is included in alpha10.

### Planned next step: M4 real-app Liquid Glass prototype

This is the first incremental app-appearance milestone, not completion of all
M4 work. Alpha10 adds an **off-by-default** Liquid Glass preview in About >
Appearance. A sample card works without headphones and contains no simulated
battery, connection or mode data. Enabling it affects only the headphone
overview's backdrop, header and groups. Settings subpages, the accepted panel,
SystemUI and the Bluetooth controller are not restyled or rewritten.

The implementation uses the existing Haze dependency and static background
layers. No shaders or UI loops are injected into Bluetooth, no continuous
animation is added, and no API/framework migration or new scope is needed.
Power saving, high-contrast text, explicit reduced transparency, non-visible
pages and a non-resumed app all disable blur effects. Solid surfaces retain
the existing theme's foreground colors and the same accessibility/control
semantics. Appearance choices use a separate local preference file and never
enter the headphone policy/command channel. Persistence failures are logged
and visibly reported.

Local validation: **23 JVM tests passed** (6 appearance, 7 mode-state,
7 settings, 3 freshness); the appearance policy enumerates all 64 gate
combinations. Debug assembly and Android Lint passed. No in-case defect
resolution, screenshot acceptance or measured power improvement is claimed.

Next independent check requires no headphones or scope reload: open About >
Appearance, enable the preview and compare reduced transparency on/off.
Connected-home appearance can be checked during ordinary use. Further
24-hour use, power comparison, accessibility/large-font coverage, retained
features and independent production signing remain later M4 gates.
