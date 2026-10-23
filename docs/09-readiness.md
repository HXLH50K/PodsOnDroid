# 09 Readiness and stage boundaries

**Latest checkpoint, 2026-10-23 evening:** alpha8/alpha9 were deployed from the
original signing workstation. The user accepts the basic alpha9 floating-panel
experience, not final aesthetic polish or native MiLink reuse. The in-case
noise-control defect shared by the app and panel is tracked as
[#1](https://github.com/HXLH50K/PodsOnDroid/issues/1), recorded and deliberately
not fixed in the next milestone. Work now proceeds to M4's opt-in main-app
Liquid Glass prototype: About-page sample without headphones, main-overview
surfaces, reduced-transparency/readability/power/background gates and unchanged
control semantics. Full debug scope reload and the brief native island have
already been user-verified. Production signing, full stability/power acceptance
and native MiLink integration are not complete.

The early-stage readiness and restart notes below are historical, not the
current task or authorization to repeat device operations.

Updated: 2026-10-22. Authority: [confirmed decisions](05-decisions.md), including the later private-repository and language instructions.

## 1. Current checkpoint

**M0/M1-A may proceed without another requirements questionnaire or connected headphones.** HyperPods source and history have been imported at `a44bc10e387f527062b9ba2b8f90b154b2e61248`. The private repository [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid) exists and remains empty remotely; no push has occurred at this documentation checkpoint. The [checkpoint](04-delivery-plan.md#9-measured-baseline-checkpoint) separates the earlier 331-test baseline from the later modern-entry, isolated-debug, locale, and restricted UI-host restart implementation. Integrated build/tests and code review are pending; runtime injection and full R01 remain unverified.

Neither readiness nor baseline test success proves that the protocol, native windows, migrated module, or new UI works. The standalone native executable exercised safety logic on the selected phone and was removed; it was not a module-injection or AirPods-control test. Record later build/probe results against their actual artifacts. Hardware control, lid-open behavior, and native-panel acceptance must wait for real equipment; synthetic data is not a substitute.

The later [read-only framework status query and official manager UI](07-device-and-native-integration.md#read-only-daemon-status-follow-up) **verify daemon-reported API 102 and show it active in the manager**. Actual P.O.D.S. host injection and new API calls remain unverified. `lspctl` is an unstable debugging CLI, not an app integration protocol.

## 2. Frozen product baseline

| Item | Decision |
| --- | --- |
| Foundation | HyperPods-derived source with independent P.O.D.S. history; preserve provenance, authorship, applicable GPL, and useful capabilities/tests |
| Label / application ID | `P.O.D.S.` / `com.hxlh50k.pods` |
| Source namespace | Keep `moe.chenxy.hyperpods` for now; do not equate namespace with installation identity |
| Platform | Current `houji` test phone, HyperOS 4, Android 17/API 37 |
| Daily phone | Xiaomi 17 Pro; not a separate initial prerequisite and not implicitly validated |
| Target headphones | AirPods 5 wireless-charging variant, user-reported firmware `1.0.0`; read exact fields when connected |
| Native comparison | Xiaomi Open-ear Headphones Pro, user-reported firmware `1.1.0.3` |
| Delivery | Prefer one modern API-102-only LSP APK; no dedicated rootless work; KSU only with demonstrated need |
| Core experience | Brief reconnect battery island, dedicated Interconnectivity headphone panel, reliable controls and feedback; first-pairing popup provisionally deferred after manual comparison |
| Island | Brief real-connection alert, then neither persistent island nor ordinary status notification; no battery-refresh alerts |
| App appearance | Native Android implementation of the Liquid Glass visual direction, not global SystemUI restyling |
| System-window behavior | Follow observed Xiaomi lock-screen, landscape, full-screen, back, and lifecycle behavior |
| Conversation volume | Keep existing additional phone-side attenuation and saved user choices |
| Multiple devices | No multi-headphone UI, proprietary multipoint, or forced takeover |
| New initial feature | Safe in-app restart of this module's verified host processes, R01 |
| Language | Coding docs and commits in English; app English and Simplified Chinese (`zh-CN`) only |
| Hosting | User-owned private GitHub repository; no public visibility without explicit authorization |
| Performance | Prioritize background power; no background glass rendering, gratuitous scans, or wakeups |

## 3. Clarifying two implementation semantics

### 3.1 Display defaults are not headphone reports

1. When a valid report exists, display the actual value and capability.
2. Otherwise prefer a saved local choice, then a defined display default, labeled **Default, not read**. Opening a page must not write it to the headphones.
3. Interaction updates the desired value. A successful write can show **Sent**, pending any report. Failure must be visible and restore the previous usable display, not imply success.
4. If reported and desired values differ, the report is authoritative. Do not repeatedly overwrite the device to make the UI agree.
5. This rule applies to configurable settings, not battery, actual wear state, model, or firmware: missing observations remain unknown.

The imported [AppPreferences](../app/src/main/java/moe/chenxy/hyperpods/utils/AppPreferences.kt) stores phone-side policies; wear-based control and additional conversation attenuation both default to `true`. The [controller](../app/src/main/java/moe/chenxy/hyperpods/pods/L2CAPController.kt) performs phone media actions in response to wear reports.

Do not relabel these phone policies as settings read from the headphones. Add a headphone-setting mapping only with real protocol evidence. Preserve existing saved choices and baseline single-/dual-ear pause rules first; do not invent a last-ear-only policy or a 30-second limit. Media-session ownership and output-route safety still require regression tests.

### 3.2 Liquid Glass versus native Xiaomi behavior

- Apply glass layers, translucency, lighting/refraction, and interaction animation to the app, incrementally within Compose.
- Keep system triggers, layering, back behavior, and lifecycle native where safe. Do not globally replace host resources for visual consistency.
- The safely customizable area inside a native panel is unverified. A custom fallback requires evidence and a separate decision.
- The mention of keimi identifies a visual reference, not an approved repository, license, or dependency. Specific pages can be supplied during prototyping.
- Use a small real Android-page prototype before broad visual changes. High contrast, reduced transparency, and disabled animation must remain usable.
- Render effects only on visible interfaces, stop unnecessary animation when static, and stop rendering in the background. Never inject the glass renderer into the Bluetooth host.

## 4. R01: Restart scoped processes

**Current milestone:** root-assisted UI-host restart is implemented only for authenticated SystemUI and observed Xiaomi Bluetooth `:ui`. It revalidates each exact PID and requires a new PID/current `BUILD_INSTANCE` before the next host. Integrated tests/review and on-device success remain pending. **Bluetooth-inclusive restart is disabled** until a real control quiesce/recovery handshake exists; the full contract below is not yet complete.

### User-facing contract

Place **Restart scoped processes** under diagnostics/module management:

- Show selected hosts, expected impact, current and target module versions, then require confirmation.
- Prefer **UI hosts only** and **All verified scopes** presets. The latter may include Bluetooth. The SystemUI plugin belongs to its host, not a separately killable plugin process.
- Explain that restarting SystemUI may temporarily remove the status bar/control center, MiLink may interrupt interconnectivity, and Bluetooth may interrupt **all** Bluetooth audio/connections, not just AirPods.
- Require a deliberate user action. Never use this as automatic connection recovery or silently after installation.
- Do not restart Bluetooth during calls. If call state cannot be established reliably, refuse or clearly restrict the action; unknown does not mean safe.
- Report denied root, unknown hosts, failure to load the new build, and timeouts explicitly.

### Implementation limits

**2026-10-23 priority update:** the interface-only preview is insufficient for APK updates because the old module remains in Bluetooth and Xiaomi Bluetooth main processes. The next debug action must reload all three required main hosts, use authenticated **old** module reports as eligible evidence, and verify the new build afterward. It cannot depend on a cooperative command that the already-installed alpha4 does not implement.

For this explicitly confirmed, debug-only operation, controlled process exit is a distinct authorization from graceful quiescence: warn that all Bluetooth audio/connections may be interrupted, check idle call state, preflight every required target, validate exact PID/UID/start identity and stop on failure. Do not set a fake `controlIpcStopped=true` acknowledgment. The operating system reclaims resources when a process exits; that is not a claim that the old controller cooperatively shut down. Normal/release/UI-only operations retain their existing guards.

1. Prefer a verified, restricted framework restart facility if one exists; otherwise use an app-side controlled root helper. This alone does not justify a KSU package.
2. Allow only statically allowlisted hosts/subprocesses actually injected by this module. Resolve exact PIDs and verify name, UID, and start-time/process identity immediately before acting, including PID reuse/shared-UID risks.
3. Act on exact PIDs only: no name-based bulk killing, wildcards, user-supplied commands, generic remote shell, or package-wide force-stop.
4. Never target PID 1, `system_server`, `zygote`, root/framework daemons, or unrelated apps. Never wipe data, weaken policy, or reboot the phone automatically.
5. Prefer genuine quiescence when both sides support it. For the debug-only upgrade path described above, explicitly authorized process exit may be used for old builds without that protocol; do not falsely report graceful preparation. Restart hosts sequentially, verifying dependencies. UI-only restart must not close a healthy AACP session.
6. Wait for the normal system lifecycle to restore each host. A process existing is insufficient: verify APK versionCode/build identity, API 102, and relevant hook readiness from the new host.
7. Distinguish **New build loaded**, **Host restored but module absent**, **Old build still loaded**, **Restore timed out**, and **Framework action/phone reboot required**. Stop on failure; do not retry indefinitely or broaden targets.
8. External broadcasts/intents cannot directly invoke root operations. No exported arbitrary-command surface; retain only necessary diagnostic results.

### Honest promise

This action recreates controlled host processes; it is **not API 102 hot reload**. Hot reload remains disabled.

It may avoid a phone reboot for ordinary module APK updates once the framework recognizes the new APK. Framework upgrades, Zygisk/native changes, or stale framework caches may still require a reboot. Report that boundary instead of promising reboot-free updates.

The optional Xiaomi `:ui` process may not immediately relaunch when no window needs it. Distinguish "stopped, awaiting normal launch" from "new module verified"; it must not hide failure in one of the three required main hosts or force a private Activity to open merely to make the check pass.

## 5. Work now versus hardware gates

| Stage | Work | Pause condition |
| --- | --- | --- |
| M0 | Preserve source provenance/docs, use independent Git history, record baseline tests/build and identity | No headphones or release key required |
| M1-A | Migrate metadata/entry/hooks, static checks, controlled probes and host self-checks, R01 contract | Runtime API below 102 or need to replace the framework: stop, do not downgrade/replace automatically |
| M1-B | Device identity, AACP, existing controls and feedback | Finish independently verified work, then explicitly wait for AirPods 5 |
| M2/M3 | Current priority: reconnect presentation and dedicated Interconnectivity panel; optional discovery popup is provisionally deferred | No further lid capture is a prerequisite for the current notification fix; agree on any hardware round with the user |
| M4 | Liquid Glass, brief island, stability/power, complete R01 verification | Ask only for genuinely new design/safety choices, not settled decisions |
| Before release | Independent release key, same-signature upgrades, GPL source obligations, approved private delivery | Resolve password custody and authorize the specific upload/release; retain private visibility |

When hardware is needed, the requested workflow is to finish and verify the current independent stage, use `task_complete` where available to summarize it, and state **Waiting for the user to connect hardware; the product is not complete**. Do not continuously poll, pair automatically, or turn synthetic results into physical-device passes.

## 6. Remaining work by kind

**Deferred choices:** release-secret custody/backup; authorization of the specific private distribution operation; a fallback panel only if native integration has a demonstrated hard limit.

**Engineering evidence, not a questionnaire:** actual P.O.D.S. host injection/new API calls and native callback/plugin loader compatibility (daemon-reported API 102 is already verified); exact AirPods fields, BLE lid semantics and control reports; native Xiaomi behavior; R01 new-build loading and reboot limits; power measurements with interference from other modules accounted for.

**Information that can arrive later:** exact daily build and failing operation sequence; Liquid Glass references; Apple-device initialization history only when relevant.

## 7. Documentation checkpoint boundaries

The English documents preserve confirmed decisions, engineering targets, and unverified boundaries. The original user-answer archive and former Git-history bundle exist outside the repository; upstream ancestry is intentionally absent from the P.O.D.S. branch.

This documentation work does not inspect signing secrets, generate keys, commit/push, install probes, or restart devices. Source import and private repository creation are established facts; concurrent build/migration work must supply its own results. The upstream [UI evidence summary](../design-qa.md) is not P.O.D.S. acceptance.
