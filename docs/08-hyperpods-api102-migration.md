# 08 HyperPods derivation and API 102 migration

Updated: 2026-10-22. Modern-only entry integration and restricted UI-host restart code are implemented; the [deployment receipt](04-delivery-plan.md#9-measured-baseline-checkpoint) records passing build/unit checks and the user subsequently reported basic controls working. Full hardware/native-window acceptance remains open. Inherited Git ancestry has now been replaced with an identical independent P.O.D.S. snapshot; the former history is backed up externally.

Release application ID is `com.hxlh50k.pods` (unsigned), with isolated debug ID `com.hxlh50k.pods.debug` and user-visible app/module title `P.O.D.S.`. Source/Android namespace `moe.chenxy.hyperpods` and native library/JNI names remain unchanged. English/Simplified Chinese per-app locales are implemented; coding docs/commits remain English. Legacy `assets/xposed_init` and `assets/native_init` are removed, leaving modern entries only. Final artifact/runtime checks remain pending. M0/M1-A need no headphones; see [readiness](09-readiness.md).

## 1. Imported baseline and scope

- HyperPods baseline: `a44bc10e387f527062b9ba2b8f90b154b2e61248`; the reference workspace was clean during investigation.
- Baseline [app build](../app/build.gradle.kts): `versionCode 28`, `minSdk 35 / targetSdk 36 / compileSdk 36`.
- Baseline [version catalog](../gradle/libs.versions.toml): legacy Xposed API 82, YukiHookAPI 1.3.1, associated KSP generation.
- Baseline [HookEntry](../app/src/main/java/moe/chenxy/hyperpods/hook/HookEntry.kt) and helpers were legacy-dependent, not an already-modern module. Linked working-tree files may now change during migration; baseline assertions refer to the fixed import commit.
- Test-device API 37 does not require changing every build tool at once. The imported toolchain declares JDK 22, Gradle wrapper 8.13, AGP 8.13.0, Kotlin 2.2.21, Compose plugin 1.9.3, and CMake 3.22.1. Record any necessary toolchain change separately from runtime compatibility.
- The measured baseline ran cached Gradle 8.13 with a JDK 17 launcher and cached JDK 22 toolchain after an initial wrapper download timeout. Standalone native safety tests built with NDK 27 passed on the selected ARM64 phone; they do not establish API 102 native-entry loading or live Bluetooth patch compatibility.

Derivative policy:

1. Preserve source and prior planning documents, but use independent P.O.D.S. Git history as requested. Do not modify the original reference workspace.
2. Preserve copyright, applicable GPL, and change provenance. The user-owned [repository](https://github.com/HXLH50K/PodsOnDroid) is created **private**, with no push at this checkpoint. Never infer permission to make it public.
3. Use the new application ID and independent release signature. A new certificate cannot seamlessly update the original HyperPods package.
4. Preserve controller, protocol, capabilities, truthful state, single-ear recovery, bounded writes, media-route safeguards, and useful tests.
5. Do not simultaneously replace UI libraries, storage, or the control-owning process. Implement Liquid Glass separately after the framework baseline is verified.
6. Historical Chinese upstream documents remain in the reference project and external backup, not this branch ancestry or working tree. All maintained coding documentation is English.

## 2. Verified API 102 contract

Dependency: `io.github.libxposed:api:102.0.0`.

Pinned official API source:

- [XposedInterface](https://github.com/libxposed/api/blob/79b75b49255a257d38f67bce2e928649dc2fc6c9/api/src/main/java/io/github/libxposed/api/XposedInterface.java)
- [XposedModuleInterface](https://github.com/libxposed/api/blob/79b75b49255a257d38f67bce2e928649dc2fc6c9/api/src/main/java/io/github/libxposed/api/XposedModuleInterface.java)

The [official modern-module guide](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API) is maintained documentation, not a fixed source snapshot.

Constraints:

1. **A target-102 module must not call `de.robv.android.xposed` legacy APIs.** Metadata/version-number changes alone are insufficient.
2. Entry extends `XposedModule` with the modern no-argument/lifecycle arrangement. Do not use framework facilities before attachment/`onModuleLoaded`.
3. Hook through interceptor chains; explicit original-method calls use the appropriate `getInvoker(...).setType(Invoker.Type.ORIGIN)` semantics.
4. `onPackageLoaded`'s default loader differs from `onPackageReady`'s final loader. A dynamic SystemUI plugin may load after both.
5. Registration lives in `META-INF/xposed/`, not old manifest metadata or `assets/xposed_init`.
6. Legacy resource hooks cannot remain; use normal host resource/view APIs with narrow scope.
7. API 102 supports hot reload, but **this project disables it** because native hooks, sockets, and system listeners are not proven safe to replace live.

The test phone's v2.2.1 label alone is not proof, but a later read-only status query has **verified daemon-reported API 102**. Actual P.O.D.S. host injection and new API calls remain **unverified** until app testing; do not downgrade to legacy if those checks fail. The [recorded `lspctl` evidence](07-device-and-native-integration.md#read-only-daemon-status-follow-up) is diagnostic only: its help explicitly identifies an unstable human/AI CLI, not an unattended protocol. Do not use it as an app status or restart backend.

## 3. Modern registration and host scope

```kotlin
compileOnly("io.github.libxposed:api:102.0.0")
```

Required module properties:

```properties
minApiVersion=102
targetApiVersion=102
staticScope=true
autoHotReload=false
```

The official example observed during research used minimum 101 and enabled hot reload; that moving example was not pinned and must not be copied as this project's policy.

Final APK requirements:

- `META-INF/xposed/java_init.list`: exactly the intended modern Java entry.
- `META-INF/xposed/module.prop`: the API/hot-reload policy above.
- `META-INF/xposed/scope.list`: verified host packages.
- `META-INF/xposed/native_init.list`: native entry registration according to the framework contract, replacing legacy `assets/native_init`; verify loading/ABI separately.
- Exact app label P.O.D.S.; localized description resources.
- Remove legacy manifest module metadata, `assets/xposed_init`, `assets/native_init`, and YukiHook KSP-generated entry artifacts to avoid double registration.
- R8 must preserve/rewrite the entry resource consistently. Inspect final debug and release APKs, not just source-file presence.

| Host/process | Responsibility | Evidence needed |
| --- | --- | --- |
| `com.android.bluetooth` | Control session and necessary native adaptation | Actual process/library/constructor |
| `com.xiaomi.bluetooth`, main and `:ui` | Scan/lid state and native quick-connect UI | Package scope is not main-process filtering; initialize distinct responsibilities |
| `com.android.systemui` | Plugin loader, card click, existing island adaptation | Per-loader/version dedup, release/reload behavior |
| `com.milink.service`, especially `:ui` | **Planned M3** native headphone-panel state/actions | Actual runtime, service permission, model, single writer |

The API migration retains the **three existing host packages** initially and supports subprocess reporting. MiLink is a planned expansion, not a currently verified scope. Do not add `system_server`, every Settings/interconnectivity package, or assume selecting the plugin package alone loads hooks: `miui.systemui.plugin` runs inside a host ClassLoader.

### Self-check integration contract

Module-status replies retain the existing request/host/version/revision/detail fields and add:

| Extra | Meaning |
| --- | --- |
| `framework_api` | Actual runtime API, not the compile-time target |
| `framework_name` | Reported framework identity |
| `process_name` | Actual reporting process, including subprocess suffix |
| `process_id` | Reporting PID |
| `build_instance` | Build identity: unique debug timestamp; release revision |

R01 uses current-process/build evidence rather than process existence or shell completion. Identity/authentication, request correlation, stale replies, epoch handling, and final-artifact values still need tests. A field being implemented is not proof of successful on-device loading.

## 4. Migration touchpoints

The baseline investigation found **eight primary Kotlin files** with legacy Xposed/YukiHook references:

| File | Required change |
| --- | --- |
| [HookEntry](../app/src/main/java/moe/chenxy/hyperpods/hook/HookEntry.kt) | Modern host/process dispatch; remove YukiHook annotation/KSP entry |
| [HeadsetStateDispatcher](../app/src/main/java/moe/chenxy/hyperpods/hook/HeadsetStateDispatcher.kt) | A2DP hooks, hidden fields/methods, native initialization/failure reporting |
| [ModuleStatusHook](../app/src/main/java/moe/chenxy/hyperpods/hook/ModuleStatusHook.kt) | Replace XC_MethodHook; runtime API/process/build evidence, including `:ui` |
| [DeviceCardHook](../app/src/main/java/moe/chenxy/hyperpods/hook/DeviceCardHook.kt) | Preserve identity filtering/fallback experience; correct original Invoker, later native-panel route |
| [SystemUIPluginHook](../app/src/main/java/moe/chenxy/hyperpods/hook/SystemUIPluginHook.kt) | Late loader, method hooks, view-state restore, lifecycle dedup |
| [MiBluetoothToastHook](../app/src/main/java/moe/chenxy/hyperpods/hook/MiBluetoothToastHook.kt) | Constructors/methods/resources; later replace global scan suppression with verified target coordination |
| [L2CAPController](../app/src/main/java/moe/chenxy/hyperpods/pods/L2CAPController.kt) | Remove hidden legacy helpers without changing protocol/recovery semantics |
| [SystemApisUtils](../app/src/main/java/moe/chenxy/hyperpods/utils/SystemApisUtils.kt) | Replace legacy reflection helpers with small platform adapters |

Also cover [app build](../app/build.gradle.kts), [version catalog](../gradle/libs.versions.toml), [manifest](../app/src/main/AndroidManifest.xml), entry resources, and R8 rules. A build plugin whose name includes lsposed is not necessarily a runtime legacy dependency; classify by purpose, not substring deletion.

## 5. Non-mechanical parts

### Async card fallback

The baseline waits for Bluetooth identity before sometimes calling the original method. A modern interceptor `chain` belongs only to that interception:

- Never retain it in a timeout/broadcast callback and call `proceed()` after returning.
- For async fallback, retain a verified Method/Invoker and bounded-lifetime context; distinguish ORIGIN from execution through other modules' hook chains.
- Matching reply, timeout, screen destruction, and repeated taps must complete an action at most once. Late replies cannot open a second window.
- The final native route should reduce this round trip; never wait for Binder/Bluetooth on the UI thread.

### Plugin ClassLoader

Package-loaded is not proof the plugin exists. Preserve the observed loading point, resolve symbols through the actual loader, and install once per loader/version. Do not retain stale Class/Method objects indefinitely or add large guessed HyperOS 3 fallback sets; support the verified HyperOS 4 build.

### Additional fields and original calls

- Replace legacy additional-instance-field helpers with lifecycle-aware storage if needed. Weak keys do not help if values strongly reference their keys.
- Save actual view state before modification; restore on reuse/destruction.
- Prefer small typed Java-reflection/modern-Invoker helpers over another large dependency; log the exact failing adaptation point.

### Native lifecycle

The imported [C++ implementation](../app/src/main/cpp/hyperpods.cpp) uses a load callback and function-pointer ABI. The migration investigation found the documented native C ABI matches the existing header/export; that static match **does not prove runtime loading**.

- Verify native registration, exported symbols, `NativeAPIEntries` version, load ordering, target library, and ABI independently.
- Retain native library/JNI names; the application-ID change does not itself require a native rename.
- Patch only verified Bluetooth images, rejecting unknown/ambiguous matches.
- If the target no longer needs the negotiation patch, omit it while retaining API 102 for system UI; this is not a switch to rootless.
- No live repeated patching or hot reload. Recreate hosts/reboot only through the validated lifecycle.
- R01 must confirm the current build after restoration; an old module in a new PID is not success.

## 6. Retained behavior versus intentional product changes

- API migration must not simultaneously rewrite AACP values, single-ear ANC recovery, or media-volume algorithms.
- Treat known UI issues as regression cases, not permission to reshuffle every screen without evidence.
- Keep two AirPods 5 case-capability variants; verify swipes/other controls after reading actual model/firmware.
- Image hashes in old tests are not proof of asset licensing.
- Separate native-popup/panel adaptations from protocol changes for traceability.
- Audit application-ID-dependent actions, Provider/FileProvider authorities, PendingIntents, notification ownership, self-checks, and scopes. Retain source namespace/JNI where appropriate rather than a global string replacement.
- Separate display defaults from actual reports. Preserve phone wear/conversation policies and saved choices; page opening must not write defaults.
- Separately change the persistent island to brief connection-only behavior, with tests proving battery/mode updates do not re-alert.

## 7. Migration acceptance

### Without headphones

- Reproducible baseline build/tests and documented differences.
- Correct modern entry/configuration in final debug/release APKs, including R8 names.
- No unexpected legacy entry/calls in dependency graph, final DEX, or native package; no `de.robv.android.xposed` or YukiHook old runtime.
- Runtime API 102 and actual selected hosts reported; main/subprocess evidence separated.
- Delayed/repeated plugin loading, service death, missing scope, and unauthorized IPC have bounded, diagnosable outcomes.
- R01 allowlist/identity refusals and build-loading contract tested; impact confirmation before real restarts, with Bluetooth interruption tested only in a designated window.

### With AirPods 5

- Compare actual identity/firmware, part batteries, single-ear operation, applicable modes, and controls against the baseline.
- No double controller/popup from old/new SDK/windows; written versus reported state remains distinct.
- Required dynamic lid-popup and Interconnectivity cases pass.
- Disabling the module restores original behavior with no residue on other devices/notifications.

## 8. Upstream maintenance

Select upstream changes by commit, prioritizing protocol/state fixes; never reintroduce API 82/YukiHook registration. Record each adaptation's scope, ROM/component version, and test evidence, rather than scattering unexplained obfuscated names.

Keep signing, application identity, and OEM changes distinct to reduce merge conflicts. The user's rolling daily hash can be supplied when reproducing a defect; development remains pinned to the imported commit and never combines test conclusions from different artifacts.
