# 04 Delivery and acceptance plan

Updated: 2026-10-22. The user accepted the basic alpha1 behavior and requested further development with independent Git history. Section 10 records the alpha2 notification/capture changes; section 9 preserves the original preview receipt. Native-window, live R01 and broader hardware acceptance remain incomplete. The release plan remains a future acceptance contract, not a completed-MVP claim.

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
 -> M2 lid discovery and native popup
 -> M3 dedicated Interconnectivity headphone panel
 -> M4 Liquid Glass / brief island / R01 / stability and power
 -> initial release through an explicitly approved private channel

After M0, read-only M2/M3 research/model design may proceed in parallel.
Real injection/control requires the M1 gates.
After M4: M5 additional features and evidence-driven optional KSU.
```

Lid popup and dedicated panel are mandatory, not deferred optional features. The brief island cannot prove either. No dedicated rootless/legacy-framework workstream.

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

### M2: AirPods 5 lid discovery and native popup

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
| T33 | Battery/mode updates and control recovery in one audio connection | No new/extended/reappearing island alert |
| T34 | No setting report, default display, manual choice, failed send | Clear provenance; display does not write; failure feedback/rollback; reports win |
| T35 | Conversation-volume default/choice and route change | Preserve policy; restore only on the correct route |
| T36 | R01 root denied, unknown host, PID reuse/identity mismatch | Explicit refusal, no wrong action or name-based bulk fallback |
| T37 | R01 UI hosts and controlled Bluetooth restart | Selected allowlist only; UI restart leaves control intact; Bluetooth impact disclosed |
| T38 | R01 restored host with old/absent module or timeout | No false success, endless retry, or system-core/reboot escalation |
| T39 | Glass visible/static/background/motion disabled | Readable/operable; no background rendering wakeups; effects optional |
| T40 | New app ID, authorities/actions/notifications/JNI self-checks | Consistent installation identity with retained namespace; no cross-talk/double control with HyperPods |

All applicable cases are initial-release requirements, particularly T19, T23-T30, and T32-T40. Execute T31 once the supplied Xiaomi headphones are available to establish the requested native behavior. Missing hardware or fallback cannot turn T24/T27 into a pass.

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
