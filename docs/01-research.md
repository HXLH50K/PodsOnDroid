# 01 Research and feasibility

Research date: 2026-10-22. Findings describe the inspected snapshots, not every release or phone. Historical defects are not automatically current defects.

The selected route is a **HyperPods derivative, rooted deployment, modern libxposed API 102, and AirPods 5**, with a lid-open popup and dedicated Interconnectivity Center panel required in the first release. Rootless research below is background, not another initial implementation track. Later decisions require private hosting, English coding documentation, and English/Simplified Chinese app localization only.

Source/history import at `a44bc10e387f527062b9ba2b8f90b154b2e61248` is complete. In-repository source links locate the imported implementation, which may change during migration; snapshot-specific claims refer to that commit. See [device evidence](07-device-and-native-integration.md), [migration](08-hyperpods-api102-migration.md), and [readiness](09-readiness.md).

## 1. Evidence levels

| Mark | Source | What it establishes |
| --- | --- | --- |
| L | Local source, build declarations, Git metadata | How the inspected snapshot works, not physical-device behavior |
| U | Upstream README, release notes, repository metadata | Upstream claims/activity, not independent reproduction |
| O | Android/KernelSU official documentation or AOSP source | Platform mechanisms; OEM behavior still needs verification |
| P | Project proposal | Intended design, target, or assumption |
| V | Target-device observation | Recorded system/root/component metadata; **no dynamic AirPods 5 feature acceptance** |
| D | Static analysis of APKs extracted from the target | Classes, entry points, dependencies, and branches, not successful windows or protocol operation |

Repository code references use pinned public snapshots when external. Official platform documentation is maintained guidance rather than an immutable source snapshot; record the tested implementation/version before relying on it.

## 2. Reference snapshots

| Project | Inspected baseline | Relevance |
| --- | --- | --- |
| HyperPods | `a44bc10e387f527062b9ba2b8f90b154b2e61248`, commit dated 2026-10-22 +08:00; source workspace was clean when inspected | Deep Xiaomi integration; code retained as provenance, inherited Git ancestry removed at the user's request |
| LibrePods | `89356c08cea3db0b6b54792d9329d09a87cc85fa`, dated 2026-10-16 +05:30; source workspace was clean | Rich cross-platform AirPods protocol/capability references |
| OpenPods | `9cd800875f594fa6eb7325dd33d9216c476a5782`, dated 2025-09-26 UTC | BLE status/notifications rather than a complete AACP controller |

Recorded provenance:

- HyperPods' inspected origin was `Mo-SeTian/HyperPods`, with an additional `HXLH50K/HyperPods` remote; upstream identity was not guessed from the name.
- LibrePods' remote was `librepods-org/librepods`; it was not archived when checked and `pushed_at` was 2026-10-22. No source branch was changed or updated during that investigation.
- OpenPods was not archived when checked. An older last commit alone does not prove abandonment.
- Repository push activity is not a stable-release date or quality guarantee.

These projects differ in scope, protocol coverage, and integration cost. A claim that all alternatives are unmaintained is not a technical justification.

## 3. HyperPods evidence and reuse

### Confirmed in the imported baseline

1. [HookEntry](../app/src/main/java/moe/chenxy/hyperpods/hook/HookEntry.kt) dispatches to `com.android.bluetooth`, `com.xiaomi.bluetooth`, and `com.android.systemui`.
2. [HeadsetStateDispatcher](../app/src/main/java/moe/chenxy/hyperpods/hook/HeadsetStateDispatcher.kt) tracks A2DP, recognizes service UUIDs, and handles late UUID discovery.
3. [L2CAPController](../app/src/main/java/moe/chenxy/hyperpods/pods/L2CAPController.kt) reflectively constructs a classic Bluetooth socket in the Bluetooth host: type `3`, PSM `0x1001`, not BLE GATT.
4. [Native adaptation](../app/src/main/cpp/hyperpods.cpp) targets `l2c_fcr_chk_chan_modes` in `libbluetooth_jni.so`; the inspected instruction-matching branch is ARM64-only and skips/logs non-unique matches.
5. [SystemUIPluginHook](../app/src/main/java/moe/chenxy/hyperpods/hook/SystemUIPluginHook.kt) relies on `miui.systemui.*` classes, a plugin ClassLoader, notification ownership, and layout methods.
6. [AACPWriter](../app/src/main/java/moe/chenxy/hyperpods/utils/AACPWriter.kt) isolates blocking writes with a bounded queue and timeouts. Its return value acknowledges enqueueing, **not the headphones**.
7. [StatusFreshness](../app/src/main/java/moe/chenxy/hyperpods/utils/StatusFreshness.kt) does not treat an unchanged event-driven value as proof that control has failed.

### Lessons to preserve

- Single-ear operation, the other ear in its case, late UUIDs, missing setting reports, and user changes during recovery are first-class cases.
- AACP failure must not automatically disconnect A2DP/HFP or restart all Bluetooth.
- Volume restoration must follow the actual output route, never reuse a previous headset's volume on a speaker or another headset.
- UI, ordinary notifications, and system cards should share model identity, alias, and state provenance.
- Restrict host-layout changes to verified owned entries and restore recycled views.

Historical feature-comparison/issue-fix documents remain in the reference project and the external history backup, not the independent P.O.D.S. branch. Relevant current code/tests and the [English historical UI summary](../design-qa.md) provide navigation. Historical problem sections are not a current defect list.

### What not to copy blindly

- A large controller combining protocol, media policies, and notifications increases the system-process failure surface; separate responsibilities incrementally.
- Private OEM classes, hidden constructors, and native instructions can change with ROM/APEX updates.
- Baseline [build declarations](../app/build.gradle.kts) were `minSdk 35 / targetSdk 36 / compileSdk 36`, `versionCode 28`; these do not imply HyperOS 4 means Android 16. Older README build records were historical.
- The baseline ties connection readiness to native-hook success. First test whether this ROM needs the patch; no patch may mean **not needed**, not **unsupported**.
- Review image, animation, font, and signing provenance separately. Code licensing does not automatically authorize bundled assets.

## 4. LibrePods protocol and capability references

Pinned to `89356c08cea3db0b6b54792d9329d09a87cc85fa`:

- [AAP Definitions](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/docs/AAP%20Definitions.md): AAP/AACP over L2CAP PSM `0x1001`; handshake, subscriptions, battery, wear, and mode messages. Initially documented against a particular AirPods Pro 2 USB-C firmware, not a universal specification.
- [BluetoothConnectionManager](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/java/me/kavishdevar/librepods/bluetooth/BluetoothConnectionManager.kt): several hidden constructor signatures, including an Android 16 QPR3 variant.
- [JNI socket helper](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/cpp/bluetooth_socket.cpp): hidden-API access remains an ordinary-APK compatibility burden.
- [KotlinModule](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/java/me/kavishdevar/librepods/utils/KotlinModule.kt): modern libxposed adaptation for Bluetooth and Settings hosts.
- [Native compatibility](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/cpp/l2c_fcr_hook.cpp): L2CAP negotiation and optional SDP VendorID modification are distinct operations.
- [Capability table](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/java/me/kavishdevar/librepods/data/AirPods.kt): separate capabilities for modes, swipes, gestures, sleep detection, and more. Enumeration is not our hardware verification.
- [Privileged permission list](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/root-module-manual/system/etc/permissions/privapp-permissions-librepods.xml): includes `BLUETOOTH_PRIVILEGED`; do not copy the whole list for convenience.

### Rootless claims and limits

The pinned [Android instructions](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/README.md) claim most features can work without root on some ColorOS/OxygenOS 16, realme UI 7.0, and sufficiently updated Pixel Android 16 QPR3 builds. Other systems may still need an Xposed negotiation workaround; upstream expects broader fixes with Android 17. VendorID-dependent extras still need hooks; system-app packaging is a separate optional enhancement.

These are **U-level claims**, not guarantees for the Xiaomi target. [Google issue 371713238](https://issuetracker.google.com/issues/371713238) returned a login page during the investigation; its fix commit and OEM adoption were not independently verified.

Excluded initial features include VendorID impersonation, forced multi-device takeover, hearing parameters, heart rate, proprietary high-quality bidirectional audio, Find My, and system head-tracked spatial audio. Each brings additional protocol, firmware, permission, audio-stack, or safety requirements; upstream marks several experimental/unknown. Reading motion data does not mean Android Spatializer integration, and AACP is not the Apple Find My network.

## 5. OpenPods: lightweight reference, not the control foundation

Pinned evidence:

- [Scan/candidate selection](https://github.com/adolfintel/OpenPods/blob/9cd800875f594fa6eb7325dd33d9216c476a5782/app/src/main/java/com/dosse/airpods/pods/PodsStatusScanCallback.java)
- [Broadcast decoder](https://github.com/adolfintel/OpenPods/blob/9cd800875f594fa6eb7325dd33d9216c476a5782/app/src/main/java/com/dosse/airpods/pods/PodsStatus.java)
- [README](https://github.com/adolfintel/OpenPods/blob/9cd800875f594fa6eb7325dd33d9216c476a5782/README.md)

It reads Apple manufacturer data, ID `76 / 0x004C`. The inspected battery nibbles `0..10` represent approximate `0..100%` steps, not 1%-precision measurements. Candidate selection favors strong signals in a 10-second window with an approximately `-60 dBm` threshold: proximity heuristics are not authenticated identity, particularly with multiple nearby AirPods.

P.O.D.S. uses AACP for bound-device state/control and includes BLE lid discovery in the first release, preferably sharing system scan events. Add verified AirPods 5 parsing and identity correlation rather than unlimited scanning. Unauthenticated advertisements may describe a **nearby device**, but must not control media or overwrite trusted bound-device observations.

## 6. Platform limits

### Classic L2CAP versus LE L2CAP

- [Non-SDK interface restrictions](https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces) apply to reflection and JNI too.
- The [BluetoothSocketSettings.Builder documentation](https://developer.android.com/reference/android/bluetooth/BluetoothSocketSettings.Builder) describes RFCOMM and LE as supported types, even though parameter enumerations show additional values. Enumeration alone does not establish support.
- In the AOSP `BluetoothSocketSettings.java` implementation inspected during research, `setSocketType` rejected types other than RFCOMM/LE and `setL2capPsm` accepted only `128..255`. That observation was from a moving `main` snapshot with no recorded commit; treat it as a research observation requiring a pinned recheck before implementation, not a reproducible fixed-source guarantee.
- That public implementation cannot directly carry classic AACP PSM `0x1001 = 4097`. Similar API names are not replacement evidence.

**G1:** Can the process construct the correct classic socket? **G2:** Can the stack negotiate with the headphones? Test separately. A G2 fix does not establish G1 access, and constructing a socket does not prove handshake success.

### Background operation and permissions

- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions): paired communication uses `BLUETOOTH_CONNECT`, scanning uses `BLUETOOTH_SCAN`; location is not universally required.
- `neverForLocation` must honor its promise and may affect scan visibility. Test it rather than silently broadening permissions.
- [Background Bluetooth guidance](https://developer.android.com/develop/connectivity/bluetooth/ble/background) discusses Companion Device Manager/CompanionDeviceService and appropriately started [connectedDevice foreground services](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device). These are background-route research, not a new rootless requirement.
- GATT examples do not implement classic AACP. Companion association does not grant platform signatures or fix the Bluetooth stack.
- HyperOS auto-start, power-saving, lock-screen, and process restrictions need physical-device testing; root packaging is not proof of permanent survival.

### HyperOS evidence

Early web results included media summaries/predictions. The attempted Xiaomi OS site redirected to an error page, so no official all-model matrix was obtained. The test phone establishes Android 17/API 37 and its exact ROM only:

- Accept the user's HyperOS 4 target, but do not infer all devices, underlying versions, or plugin symbols from media articles.
- ROM fingerprint and component versions are recorded in the [device report](07-device-and-native-integration.md).
- Native library identity, injection timing, and actual headphone firmware still require implementation-stage checks.

## 7. Deployment options

| Route | Can provide | Does not automatically solve | Decision |
| --- | --- | --- | --- |
| Ordinary APK with companion/foreground lifecycle | UI, state, notifications, control where platform permits | Hidden APIs, old negotiation, privileged/OEM UI | Dedicated rootless backend deferred |
| APK with LSP adapter | Host-level control and native-window integration | Framework/ROM compatibility and process-crash risk | Initial route, API 102 only |
| KSU privileged APK only | Some allowlisted privileged permissions on matching partitions | ART/native hooks or negotiation | Not a complete control solution |
| KSU + Zygisk + LSP | Injection environment | Version/scope/protocol compatibility | Use verified existing environment; never install silently |
| Custom root daemon/Zygisk injector | Possible privileged transport/injection | Binder, SELinux, host ownership, lifecycle, maintenance | Not initial scope; Linux BlueZ is not a drop-in Android stack |
| Shizuku/ADB shell | Some operations and diagnostics | Platform identity, Bluetooth-host access, native negotiation | Not a complete substitute |
| BLE only | Approximate battery and nearby-device status | Full AACP control and reliable ownership | Possible degradation, not fulfillment of core goals |

Recorded framework findings:

- Original LSPosed was not archived when queried, but its latest release resolved to [v1.9.2, 2023-10-11](https://github.com/LSPosed/LSPosed/releases/tag/v1.9.2). Recent pushes do not prove that release supports new Android.
- `JingMatrix/LSPosed` redirected to Vector. The inspected README claimed support through Android 17 Beta; [Vector v2.2](https://github.com/JingMatrix/Vector/releases/tag/v2.2) declared modern API 102. These remain upstream claims.
- The official API dependency is `io.github.libxposed:api:102.0.0`; target-102 modules cannot call legacy Xposed. No dual entry and no initial hot reload.
- The test phone reports `LSPosed v2.2.1 (7912)`; do not identify its lineage as Vector by name alone. A later read-only status query **verified daemon-reported API 102**; actual P.O.D.S. host injection and new API calls remain unverified. See the [daemon evidence](07-device-and-native-integration.md#read-only-daemon-status-follow-up).
- The [KernelSU module guide](https://kernelsu.org/guide/module.html) says KSU does not bundle Zygisk and the inspected guide requires a mounting-capable metamodule for `system` mounts. Branch/version differences require environment-specific checks.
- [Partition-matched privileged allowlisting](https://source.android.com/docs/core/permissions/perms-allowlist) applies. Putting a self-signed APK in `priv-app` does not platform-sign it.

## 8. Recommendation and confidence

Use the imported HyperPods APK/control foundation, migrate API 102 first, then add AirPods-5-specific native presentation adapters. Add KSU packaging only if a concrete privilege requires it.

1. Keep useful protocol/state/tests and avoid simultaneous process/project rewrites.
2. Verify whether the target actually needs the native negotiation patch, independently of rootless claims.
3. Verify advertisements, lid events, and device association before native quick-connect reuse.
4. Adapt the real SystemUI-plugin-to-MiLink service/model path, not just `third_headset` to `headset`.
5. Enforce one command writer; Xiaomi's older AirPods SDK must not compete. Unknown versions degrade safely, but a normal settings-page fallback does not satisfy the required panel.

Confidence is high that the foundation is worth extending. Native entry points have been located; actual AirPods 5 lid messages and end-to-end rendering/control remain unverified.

## 9. Licenses and assets

- HyperPods' imported [LICENSE](../LICENSE) contains GPL v3. Determine any later-version option from the actual reused file grants, not a generic license template.
- The pinned [LibrePods README](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/README.md) states GPL-3.0-or-later; OpenPods also grants v3-or-later.
- Preserve pinned source provenance, copyright, modification notices, and applicable corresponding-source obligations. Independent Git history and private hosting do not relicense derived work or remove distribution duties.
- Review assets independently. LibrePods distinguishes trademarks and mentions Apple fonts; use system fonts and original/clearly licensed art rather than unverified Apple renders or animations.
- [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid) was private during the initial research. The user explicitly authorized public hosting and beta publication on 2026-10-24. Preserve the source/license/asset notices for distribution. This is engineering guidance, not legal advice.

## 10. Setting readback audit (2026-10-24)

The user requested existing upstream solutions, not speculative protocol work.
This audit covers the missing long-press cycle, missing microphone selection,
and tone-volume conflicts. It did not operate the headphones, collect radio
traffic, request ownership or restart a host.

### Sources and evidence limits

The GitHub API resolved these current public snapshots:

| Source | Revision | Scope |
| --- | --- | --- |
| LibrePods `main` / `v1.0.4` | `89356c08cea3db0b6b54792d9329d09a87cc85fa` | Android control state, setting screens and generic AACP subscription |
| LibrePods `android/rewrite` / `rewrite-v2.0.0-alpha.4` | `80aa7461df414470f379ebdfe540b2e8889fcbe2` | Rewritten Android parser/state and the released tone minimum correction |
| OpenPods `master` | `9cd800875f594fa6eb7325dd33d9216c476a5782` | BLE battery/wear/model decoding, not AACP settings |

The rewrite explicitly lists AirPods 5 and 5 WCC in
[its model table](https://github.com/librepods-org/librepods/blob/80aa7461df414470f379ebdfe540b2e8889fcbe2/android/core/src/main/kotlin/me/kavishdevar/librepods/core/devices/AirPods.kt).
That establishes model coverage, not acceptance of every setting on the user's
earbuds. The [protocol notes](https://github.com/librepods-org/librepods/blob/80aa7461df414470f379ebdfe540b2e8889fcbe2/docs/AAP%20Definitions.md)
are explicitly based on AirPods Pro 2 USB-C firmware 7A305, not an AirPods 5
specification.

### Long-press noise cycle: no ready readback fix found

- Both Android branches use `LISTENING_MODE_CONFIGS` (`0x1A`) and the same
  Off/ANC/Transparency/Adaptive mask bits `1/2/4/8`.
- The [main screen](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/java/me/kavishdevar/librepods/presentation/screens/PressAndHoldSettingsScreen.kt)
  and [rewrite screen](https://github.com/librepods-org/librepods/blob/80aa7461df414470f379ebdfe540b2e8889fcbe2/android/app/src/main/kotlin/me/kavishdevar/librepods/presentation/screens/apple/PressAndHoldSettingsScreen.kt)
  read the first byte from the shared control-state map and fall back to zero
  when it is absent.
- That map is not exclusively a headphone report. Both
  [main `sendControlCommand`](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/java/me/kavishdevar/librepods/bluetooth/AACPManager.kt)
  and [rewrite `sendControlCommand`](https://github.com/librepods-org/librepods/blob/80aa7461df414470f379ebdfe540b2e8889fcbe2/android/core/src/main/kotlin/me/kavishdevar/librepods/core/bluetooth/aacp/AACPManager.kt)
  update local state when submitting a write. A populated/selected upstream UI
  therefore does not prove successful initial readback.
- The inspected Android code provides the generic notification subscription,
  but no distinct AirPods-5-specific query that reliably retrieves this mask.
  P.O.D.S. already subscribes to those notifications.
- [librepods-org/librepods#691](https://github.com/librepods-org/librepods/pull/691)
  is a closed, unmerged reconnect/stem-configuration proposal tested on AirPods
  Pro 3. Replaying stored settings is not a readback fix and would change the
  user's headphone state. It is not adopted.

**Decision:** leave this readback issue unresolved. Keep unknown and sent-only
state distinguishable; allow the existing explicit manual selection without
inventing a reported cycle.

### Microphone selection: no ready readback fix found

The [main microphone screen](https://github.com/librepods-org/librepods/blob/89356c08cea3db0b6b54792d9329d09a87cc85fa/android/app/src/main/java/me/kavishdevar/librepods/presentation/screens/MicrophoneSettingsScreen.kt)
and [rewrite microphone screen](https://github.com/librepods-org/librepods/blob/80aa7461df414470f379ebdfe540b2e8889fcbe2/android/app/src/main/kotlin/me/kavishdevar/librepods/presentation/screens/apple/MicrophoneSettingsScreen.kt)
use `MIC_MODE` (`0x01`), with wire values `0=automatic`, `1=right`, `2=left`.
P.O.D.S. already has this mapping. Both upstream screens default an absent value
to automatic, while using the same optimistic map described above. Copying that
fallback would hide the unknown state, not retrieve a setting.

[librepods-org/librepods#730](https://github.com/librepods-org/librepods/issues/730)
reports reconnects and selection returning to automatic on AirPods Pro 2 USB-C.
It was open with no published fix in the inspected discussion. It is neither
AirPods 5 evidence nor a recipe for fixing missing initial microphone reports.
The rewrite's high-resolution microphone recording work is a separate stream
feature and does not solve `MIC_MODE` readback.

**Decision:** leave initial readback unresolved. Do not display an assumed
automatic selection or add an unverified query/takeover.

### Tone volume: a released partial fix is available

[librepods-org/librepods#865](https://github.com/librepods-org/librepods/issues/865)
describes a setting reverting after leaving Accessibility on AirPods Pro 3.
In the [maintainer response](https://github.com/librepods-org/librepods/issues/865#issuecomment-6350903110),
values below 15 percent are identified as returning to the previous level.
The promised rewrite fix is present in the released
[`rewrite-v2.0.0-alpha.4`](https://github.com/librepods-org/librepods/releases/tag/rewrite-v2.0.0-alpha.4):
the [pinned Accessibility screen](https://github.com/librepods-org/librepods/blob/80aa7461df414470f379ebdfe540b2e8889fcbe2/android/app/src/main/kotlin/me/kavishdevar/librepods/presentation/screens/apple/AccessibilitySettingsScreen.kt)
uses `15f..100f`, whereas `main` still uses `0f..100f`.

**Initial adoption:** restrict user-submitted tone values to 15-100 in both the
slider and controller validation, while accepting actual 0-100 reports.

**User-requested UI follow-up:** 15 percent is now a two-stage stop rather than
a hard write minimum. A gesture starting above 15 stops there for that entire
gesture. Release and start a new gesture from 15 or below to submit a lower
value. The low range is orange-red in both themes. Help text states the
recommended range, without explaining the second gesture. The page-local release
warning was removed in the copy follow-up because the common setting-result
handler already reports rejected changes. The controller allows explicit
requests throughout 0-100; lower requests
can still be rejected by firmware. Cancelled gestures do not submit a setting
or carry an unlocked drag into the next gesture.

Actual readings are never relabeled as 15. Merely opening the control does not
send a setting. The existing animation/notice behavior continues to handle
different later reports. This deliberately softens the upstream minimum at the
user's request; it does not establish that lower values work on AirPods 5.
The soft-stop follow-up passed 128 focused JVM tests, app/UI-test compilation
and Lint. A bounded synthetic phone check confirmed a drag from 60 stopped at
15, a new gesture reached 4, the track turned orange-red, and the release toast
appeared. That release toast was removed in the later copy follow-up; the
two-stage gesture, color and common error handling are unchanged. The preview
used local callbacks only, not headphone commands.

Both upstream screens send `CHIME_VOLUME` (`0x1F`) with a hardcoded second byte
of `0x50` and initialize a local slider value with a 75-percent fallback.
Their `snapshotFlow` can submit that initial value after its debounce.
Neither behavior is copied as a readback solution: P.O.D.S. already writes only
after explicit user interaction and preserves the actual second configuration
byte, including zero. Its parser retains all four control-value bytes instead
of treating embedded zero bytes as padding.

This is a conservative adoption of a released upstream range correction, not
proof of every AirPods 5 firmware limit or a fix for all volume conflicts.
The upstream issue's physical report is for Pro 3. Reversion of values within
15-100 remains unresolved without an agreed AirPods 5 test round.

### OpenPods does not supply these control fixes

The pinned [README](https://github.com/adolfintel/OpenPods/blob/9cd800875f594fa6eb7325dd33d9216c476a5782/README.md)
and [status decoder](https://github.com/adolfintel/OpenPods/blob/9cd800875f594fa6eb7325dd33d9216c476a5782/app/src/main/java/com/dosse/airpods/pods/PodsStatus.java)
cover nearby BLE status: battery, charging, wear and model identity. The inspected
implementation does not retrieve these three AACP settings. BLE estimates
must not be used to manufacture microphone, cycle or tone-volume values.

### Remaining acceptance

No ready AirPods-5-specific fix for the two missing readbacks was found in the
inspected sources/discussions. This does not prove such a query is impossible,
or establish that the failures are unique to AirPods 5. No additional protocol
write, ATT polling loop, firmware reset or ownership takeover is justified by
the evidence above.

The initial range correction passed **122 focused JVM tests** for controller behavior,
setting bounds, tone interaction state and AACP parsing; app/UI-test compilation;
and Lint with **0 errors, 129 warnings and 2 hints**. UI instrumentation was
compiled, not executed, because the earlier device idle blocker remains.

Physical acceptance remains collaborative: only after agreeing on a round,
connect the user's AirPods 5 and check one explicit
15-percent selection and one ordinary value such as 60 percent. Compare actual
reports without assuming that a sent command was accepted.
