# 07 Test device and Xiaomi native integration

Investigation date: 2026-10-22. This is the record of a **prior read-only survey** of the user's rooted test phone: metadata queries, local APK extraction, and static analysis. That survey did not install a probe, invoke windows, change configuration, enable/disable modules, restart processes, or connect headphones. Concurrent implementation work must record its own later results rather than retroactively upgrading this evidence.

The user subsequently supplied AirPods 5 wireless-charging/reported `1.0.0` and Xiaomi Open-ear Headphones Pro/reported `1.1.0.3`, and authorized future non-destructive tests. These are user reports, not firmware fields read from a connected headset.

## 1. Requirements and evidence boundary

- Extend HyperPods rather than rewrite the protocol foundation.
- AirPods 5 is the required target; older Xiaomi AirPods integration cannot be presumed to cover it.
- Real lid-open popup and dedicated Interconnectivity Center headphone panel are mandatory initial features.
- Root-first, modern API 102 only; no dedicated rootless or legacy work.
- Use the supplied Xiaomi headphones for native-behavior comparison when needed. Finish the verified independent stage and wait for the user, without automatic pairing or continuous polling.
- Daily phone is Xiaomi 17 Pro; initial work uses the current test phone, not a separate second-device gate.

**Device observations** cover metadata, root identity, and selected configuration; the follow-up below also verifies daemon-reported API 102. **Static evidence** covers APK symbols, methods, resources, and branches. **Unverified** includes actual P.O.D.S. host injection/new API calls, AirPods 5 advertisements/lid semantics, native-window rendering, and card controls. This survey neither proves AirPods 5 compatibility nor identifies the user's daily HyperPods build as identical to the imported source.

## 2. Device profile

| Field | Recorded result |
| --- | --- |
| Model / codename | `23127PN0CC / houji` |
| SoC | `SM8650` |
| Incremental ROM | `OS4.0.0.14.XNCCNXM` |
| HyperOS property | `OS4.0` |
| Android / API | `17 / 37` |
| Security patch | `2026-08-01` |
| ABI / page size | `arm64-v8a / 4096` |
| Root | `su -c id` returned UID 0, SELinux context in the KSU domain |
| Framework module self-report | `LSPosed v2.2.1 (7912)`, directory ID `zygisk_lsposed` |
| Injection module self-report | `Zygisk Next 1.5.0 (843-5217106-release)` |
| KSU version clue | Zygisk description included `KernelSU (40900)`; not an independently queried manager version |
| Framework processes | `lspd` and corresponding Zygisk companion present; no disable marker found for the two queried modules |
| API 102 | **Daemon-reported API 102 verified** by the read-only follow-up below; actual P.O.D.S. host injection/new API calls still unverified |
| AirPods apps | Exact package queries found neither `moe.chenxy.hyperpods` nor `me.kavishdevar.librepods` |
| Headphones | None connected during the survey; later user-reported variant/firmware is not a protocol reading |

Fingerprint:

```text
Xiaomi/houji/houji:17/CP2A.260605.016/OS4.0.0.14.XNCCNXM:user/release-keys
```

Existing root/framework does not contradict the absence of AirPods apps; neither was replaced. Do not infer that this LSPosed branch is JingMatrix/Vector by name. A non-project power-related root module was present, but its configuration/effective behavior was not investigated. Control such interference in power measurements without unauthorized disabling.

Do not record ADB serials, Bluetooth addresses, pairing databases, or account data in project documentation.

### Read-only daemon status follow-up

On 2026-10-22, the installed framework's read-only debugging command `/data/adb/modules/zygisk_lsposed/lspctl --json status` returned:

```json
{
  "daemonRunning": true,
  "versionName": "2.2.1",
  "versionCode": 7912,
  "apiVersion": 102,
  "developerMode": false,
  "safeMode": false
}
```

This **verifies daemon-reported API 102**, rather than inferring support from a version label. It does **not** verify P.O.D.S. injection into any host, successful new API calls, plugin/native loading, or R01; those remain app-test gates.

A subsequent **read-only official manager UI check also showed API 102 active**. This independently observed display corroborates the framework status, but does not establish any P.O.D.S. runtime success.

The CLI's own help describes it as an **unstable human/AI debugging CLI, not an unattended protocol**. Do not build P.O.D.S. status checks, restart behavior, or other app integration against `lspctl`; this query is diagnostic evidence only.

## 3. Component versions and binary anchors

| Package | versionName / versionCode | Observed role |
| --- | --- | --- |
| `com.android.bluetooth` | `17 / 37` | AOSP Bluetooth host in `com.android.bt` APEX |
| `com.xiaomi.bluetooth` | `17 / 37` | Bluetooth extension, AirPods scan/state, quick-connect entry |
| `com.android.systemui` | `17.03.260226.r / 202602260` | Control Center host |
| `miui.systemui.plugin` | `18.2.1.88.0 / 182018800` | Interconnectivity card-click entry |
| Active `com.milink.service` update | `18.2.2.1.2609281717 / 180020201` | Card host, headphone strategies/runtime |
| Factory `com.milink.service` copy | `18.1.1.17.2607231617 / 180010117` | Not the active APK analyzed |
| `com.xiaomi.mi_connect_service` | `6.0.163.10 / 60001631` | Candidate interconnectivity component; no proven hook need |
| `com.xiaomi.mirror` | `18.00.16-26.07.14 / 180016` | Candidate; no proven hook need |

Do not broaden scopes merely because package names include connect or mirror.

| Extracted APK identity | SHA-256 |
| --- | --- |
| System BluetoothExtension | `12426fc54c50ff473874875ba569a8d385ed1290319d0e735b712078f9138d7f` |
| System MIUISystemUIPlugin | `de51bd76933922fbd3c3baeb2b5bfeaee7013b43d5816759f2dcda35cb37a068` |
| Active MiLink base APK | `6c05c0566ae696fd2594ec03d4e700050e0eaa2837fe3f16c61228c451143652` |

Extracted system APKs/decompiled source are local interoperability evidence, not P.O.D.S. source or deliverables. Retain hashes/symbols/behavior summaries for repeatable inspection; do not copy closed-source implementations.

## 4. The inspected model table does not contain AirPods 5

Read-only inspection found `fc_support_airpods.json` in BluetoothExtension's device-protected quick-connect resources, approximately **1500 bytes**.

The `AirPodsSupport` table included:

| Configuration name | Broadcast-model identifier string |
| --- | --- |
| AirPods 1 / 2 / 3 | `0220 / 0F20 / 1320` |
| AirPods 4 / AirPods 4 ANC | `1920 / 1B20` |
| AirPods Pro / Pro 2 / Pro 2 USB-C | `0E20 / 1420 / 2420` |
| AirPods Pro 3 | `2720` |
| AirPods MAX / MAX 2 | `0A20 / 1F20` |

Normal/Pro/Max groupings also lacked AirPods 5. This supports the user's report, but does not prove every other ROM path lacks new-model code.

**Adding one JSON entry is not complete adaptation:**

1. Actual AirPods 5 BLE identity and lid bits/counters have not been captured.
2. Product numbers such as `A3531` are not the same namespace as broadcast IDs such as `0220`.
3. Window initialization/resources, battery semantics, wear/mode capabilities, and the writer backend may still only understand older devices.
4. Editing downloaded system resources is update-fragile and can contaminate original behavior. Prefer target/version-scoped in-memory adaptation.

The imported [AirPods capability table](../app/src/main/java/moe/chenxy/hyperpods/utils/AirPods.kt) and [AirPods5SupportTest](../app/src/test/java/moe/chenxy/hyperpods/utils/AirPods5SupportTest.kt) distinguish two case variants. They justify retaining the control foundation, not claiming hardware acceptance.

## 5. Lid-open / quick-connect route

### 5.1 Imported HyperPods baseline

[MiBluetoothToastHook](../app/src/main/java/moe/chenxy/hyperpods/hook/MiBluetoothToastHook.kt) in the imported baseline locates obfuscated `b1.d.U(ScanResult)` and replaces it with a no-op, intended to avoid competition from system AirPods quick-connect/popup/island control.

This is a concrete factor affecting missing popups, but:

- The user identified the interface as primarily older-model support; the inspected system table lacks AirPods 5.
- The suppression is **not automatically a bug to delete**; restoring the original method is not proof of AirPods 5 support.
- Define discovery, display, and control ownership by target device instead of choosing between global suppression and global unsuppression.

### 5.2 APK-confirmed route

```text
MiuiFastConnectService scanning in com.xiaomi.bluetooth
 -> b1.d.U(ScanResult), log name MiuiAirPodsFastConnectHelper
 -> older-model / signal / address association and state
 -> quick-connect display route
 -> com.android.bluetooth.ble.app.MiuiFastConnectActivity
    process com.xiaomi.bluetooth:ui
 -> window controller and resources
```

Static details:

- `U` reads manufacturer data `0x004C`, checks type `0x07`, and applies RSSI filtering. Key behavior was cross-checked against DEX instructions, not decompiler output alone.
- Display uses action `FAST_CONNECT_DEVICE_AIRPODS`, device/state/category extras, and protocol number **16**. These are private contracts for this build.
- Orientation, Bluetooth state, and existing-window checks exist. `MiuiFastConnectActivity` is in `:ui` and is not freely exported to ordinary apps.
- `airpodsstate`/`airpodsRepository`, older-model resources, and a control SDK exist. Starting an Activity alone does not complete state initialization.
- No window was dynamically triggered, and downloaded resources were not proven sufficient to render AirPods 5.

### 5.3 Implementation order

1. Preserve isolation as the baseline; do not blindly restore old scanning.
2. Observe the scan entry and build verified AirPods 5 decode/identity mapping.
3. Prefer native quick-connect with new-adapter input, not Pro impersonation or a global enable-all switch.
4. Suppress competing old control/duplicate alerts only for confirmed owned targets; leave Xiaomi/other devices unchanged.
5. Without reliable lid evidence, do not claim lid support from A2DP or cached-battery updates.
6. If the native container is inseparable from old protocol assumptions, evaluate a replacement data adapter or separately approved compatible small-panel content. An ordinary notification is not completion.

## 6. Dedicated Interconnectivity panel route

### 6.1 Baseline is a full-app redirect

Imported [DeviceCardHook](../app/src/main/java/moe/chenxy/hyperpods/hook/DeviceCardHook.kt) intercepts `DeviceInfoWrapper.performClicked(Context)` for `third_headset`, asynchronously confirms identity, then starts HyperPods' own Activity and hides the panel. Mismatch/timeout invokes the original method.

This provides a click entry and identity-boundary reference, not the required Xiaomi-like dedicated headphone panel.

### 6.2 Confirmed static system route

```text
com.android.systemui
 -> miui.systemui.plugin DeviceInfoWrapper.performClicked(Context)
 -> card-management clients i0.a / j0.b
 -> action com.miLink.card.frame.show
    binds the service designated by system configuration
 -> Messenger request containing deviceInfo and other card data
 -> com.milink.service:ui
    com.miui.circulate.world.MLCardViewHostService
 -> deviceType selects content strategy / service model
 -> native headphone content, state listeners, control adapter
```

Read-only action resolution found **two** candidates: the MiLink card host and a Mi Home control service. Preserve the system-metadata trusted route; **do not choose the first resolver result arbitrarily**.

Active MiLink dispatch:

- `headset` selects a headphone-content-specific strategy.
- `third_headset` selects generic third-party strategy `se.q`.
- Online/service-info prerequisites differ; this is not merely a View style.
- The manifest places the card service in `:ui`, protected by `SHOW_CARD`, protection level **`0x12` (`signature|privileged`)**. Do not weaken its global checks.

Type dispatch was DEX-verified. The user observed ordinary-brand cards opening Bluetooth settings, but the survey did not dynamically reproduce the final redirect. Static class presence is not complete UI verification.

### 6.3 Older AirPods strategy is not proof of AirPods 5 support

The active MiLink APK contains:

- `AirPodsHeadsetStrategy`
- `ThirdPartyHeadsetStrategy`
- `XiaomiHeadsetStrategy`
- `HeadsetStrategyFactory`

In the inspected factory branch, internal types **5/6** select AirPods and **9** selects generic third-party behavior. These are private internal values, not a complete universal device-type definition.

The AirPods strategy uses `MxBluetoothManager.getInstanceForAirpods`, system AirPods state, capability/wear checks, and an ANC setter. The inspected setter covers off/ANC/transparency; a fourth adaptive mode is not established.

Therefore adapt state/action sources, not just device type; ensure the old SDK and HyperPods do not both write; and limit mappings to the target presentation context without global VendorID/model impersonation.

## 7. Preferred design and stop conditions

**HyperPods owns reliable control; native containers own presentation; an AirPods 5 adapter connects them.**

| Area | Required work | Unacceptable shortcut |
| --- | --- | --- |
| Identity | Actual model/case/BLE ID/firmware and trusted device association | Name substring or strongest RSSI |
| Lid popup | Real broadcast semantics, cycle dedup, input/resources/lifecycle | Unsuppress/add JSON entry and declare success |
| Interconnectivity | Service model, content strategy, trusted target routing | Full-app redirect or global `third_headset` change |
| State | AACP authority, BLE estimates, offline/cache distinctions | Re-injecting cache as a new headphone report |
| Control | One queue, capabilities, mode mapping, genuine reports | Parallel Xiaomi SDK and HyperPods writers |
| Lifecycle | API 102, process boundaries, plugin loader, version refusal | Package-only checks that ignore main versus `:ui` |

Stop and report if:

- Runtime API is below 102.
- No real AirPods 5 messages establish reliable lid semantics.
- Native content requires Xiaomi authentication/private services that cannot safely be supplied.
- A panel would require global impersonation or disabling system safeguards.
- Single control ownership cannot be ensured.

Stopping means gathering evidence and potentially discussing an alternative, not quietly lowering the initial requirements.

## 8. Physical verification still required

### AirPods 5

- Read exact product/firmware fields for the reported wireless-charging/1.0.0 device. Ask about Apple initialization only if relevant to a failure.
- Timestamp lid-open-without-A2DP, connected, single/dual-ear, return-to-case, and closed-lid samples for the same headset.
- Run the authorized probe on the selected phone; this survey did not pair or execute that test.

### Xiaomi comparison

- Wait for Xiaomi Open-ear Headphones Pro/reported `1.1.0.3` when dynamic comparison is needed; it does not block static/API work.
- Record popup, Interconnectivity click, panel action, back/close, lock-screen, and landscape behavior.
- Establish the untouched Xiaomi route first, then prove enabling P.O.D.S. does not change it.
- Do not update firmware, change account binding, or reset headphones merely for comparison.

## 9. Method and limitations

- Existing ADB and SDK `apkanalyzer` inspected manifests, class indexes, and DEX. An old PATH wrapper had an invalid SDK location; the existing `cmdline-tools\latest` was used without changing global SDK configuration.
- JADX was absent, so official **JADX 1.5.6** was used in the investigation workspace after verifying its release ZIP SHA-256. No system APK was uploaded.
- Whole BluetoothExtension decompilation reported **16 errors**. Key scan/card branches were cross-checked in DEX; never treat all pseudocode as correct. Selected MiLink classes could be inspected individually, but runtime UI/control remains unverified.
- Retain hashes/symbols/conclusions, not closed-source code or APKs in the project. Private repository status does not authorize distributing those binaries.
