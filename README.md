# P.O.D.S.

A private HyperPods-derived Android project for improving the AirPods 5 experience on Xiaomi HyperOS 4.

**Status, 2026-10-22: M0/M1-A development preview deployed for manual testing.** Implementation commit **`5013f04`**, version **`0.1.0-alpha.1` (code 1)**, is installed as `com.hxlh50k.pods.debug`; the installed APK hash matches the local artifact. The post-fix build passed **377 JVM tests**, Lint with **0 errors / 119 warnings / 2 hints**, and debug, AndroidTest, and unsigned-release assembly. The corrected deployment passed two identity/locale phone tests and a cold launch (**289 ms**), showing the waiting-for-AirPods screen. This is **not a completed MVP**: injected controls and live R01 remain unverified; native lid popups, the dedicated headphone panel, Liquid Glass, and Bluetooth-inclusive restart are not implemented in this preview. See the [full deployment receipt](docs/04-delivery-plan.md#9-measured-baseline-checkpoint).

## Project boundaries

- App/module title: **P.O.D.S.**; release application ID `com.hxlh50k.pods` (unsigned), isolated debug ID `com.hxlh50k.pods.debug`. The Kotlin/Android namespace remains `moe.chenxy.hyperpods`.
- Per-app locales implemented: **English and Simplified Chinese (`zh-CN`) only**. Coding documentation and commit messages are English.
- Repository: [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid), created **private**; no initial push at this documentation checkpoint. Keep it private unless the user explicitly authorizes a visibility change.
- Preserve HyperPods history, attribution, and applicable [GPL terms](LICENSE). Private hosting does not waive license obligations when distributing derived work.

## Prerequisites and validation

Development uses Gradle 8.13, a JDK 22 toolchain, Android SDK 36, CMake 3.22.1, and the Android NDK selected by the build. The baseline used a JDK 17 launcher and cached distributions. After the initial download timeout, the wrapper now bootstraps `8.13-bin` using this environment's existing proxy process-locally; its network timeout is 60 seconds. See [build configuration](app/build.gradle.kts) and the separately [recorded build/test results](docs/04-delivery-plan.md#9-measured-baseline-checkpoint).

Runtime work targets the rooted `houji` test phone, `OS4.0.0.14.XNCCNXM`, Android 17/API 37. **Daemon-reported API 102 is verified**, and a read-only official manager UI check also showed API 102 active. P.O.D.S. host injection and new API calls remain **unverified** until app testing. Legacy Xposed and a dedicated rootless backend are out of scope. See the [framework evidence and CLI boundary](docs/07-device-and-native-integration.md#read-only-daemon-status-follow-up).

Hardware-gated validation requires the user's AirPods 5 wireless-charging variant (reported firmware `1.0.0`). Xiaomi Open-ear Headphones Pro (reported `1.1.0.3`) provide a native-behavior comparison. Neither imported tests nor synthetic previews prove real AirPods 5 control, lid detection, or native-window compatibility. Do not install or restart system hosts outside the controlled test plan.

The debug app is enabled only for the three declared scopes. **19 restart tests** passed, but live restart behavior remains unverified. Six AboutPage UI tests were **blocked before app assertions by Android 17 Espresso `InputManager.getInstance` bootstrap**, not passed. No host-process restart or earbud interaction has been performed.

## Manual testing handoff

1. **Restart the phone once** to allow scoped hosts to load the module; successful injection still needs confirmation.
2. Connect the AirPods 5, open P.O.D.S., and inspect diagnostics for host loading, device identity, and control state.
3. Try supported basic controls and report the observed results. Do not expect the planned native popups/panel, Liquid Glass, or Bluetooth-inclusive restart.

The user will perform these steps; further autonomous runtime testing and dependency upgrades are not part of this handoff.

## Documentation

Start with [readiness and stage boundaries](docs/09-readiness.md) and [confirmed decisions](docs/05-decisions.md).

| Document | Purpose |
| --- | --- |
| [Research](docs/01-research.md) | Pinned evidence, platform limitations, and feasibility |
| [Product](docs/02-product.md) | Required experience, supported scope, and honest state feedback |
| [Architecture](docs/03-architecture.md) | Process ownership, protocol, IPC, and recovery |
| [Delivery and acceptance](docs/04-delivery-plan.md) | Milestones, hardware gates, and measurable targets |
| [Signing and maintenance](docs/06-signing-and-maintenance.md) | Independent identity, private delivery, rollback, and licensing |
| [Device and native integration](docs/07-device-and-native-integration.md) | Recorded device evidence and unverified OEM integration |
| [API 102 migration](docs/08-hyperpods-api102-migration.md) | Modern module contract and migration acceptance |
| [Upstream UI evidence](design-qa.md) | Historical HyperPods results, **not P.O.D.S. verification** |
