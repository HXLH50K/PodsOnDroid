# P.O.D.S.

A private HyperPods-derived Android project for improving the AirPods 5 experience on Xiaomi HyperOS 4.

**Latest feedback, 2026-10-23:** alpha4 shows an ordinary notification/card before collapsing into the island, uses battery-shaped icons instead of earbuds, and its interface-only restart does not reload the Bluetooth hosts after updates. The immediate priority is **complete debug scope reload with actual new-build verification**. This is process restart, not API102 hot reload and not a phone reboot.

The next presentation changes use separate model-appropriate left/right earbud icons and disable ordinary focus expansion. The renderer still has a backing Android notification: that interim change is **not** completion of the requested pure charging-style native animation channel. That route needs a separately verified system contract rather than a guessed template or a hidden persistent notification.

Alpha5 implements the priority debug reload path and passed **46 focused JVM tests** plus debug assembly. Alpha4's [build-only receipt](docs/04-delivery-plan.md#alpha4-package-receipt) remains historical; the [alpha5 record](docs/04-delivery-plan.md#14-alpha5-priority-full-debug-scope-reload) distinguishes local checks from live acceptance. No autonomous device restart, installation, headset operation or computer shutdown is part of this iteration.

The user's Xiaomi comparison found silent paired reconnect/wearing and a quick-connect card in discovery/first-pairing mode. P.O.D.S. intentionally keeps the user's preferred brief battery island for reconnect. First-pairing popup work is **provisionally deferred**, not permanently cancelled; ordinary Android Bluetooth pairing remains available. The dedicated Interconnectivity control panel and Liquid Glass remain planned. See the [latest UX evidence](docs/07-device-and-native-integration.md#manual-xiaomi-comparison-and-ios-distinction) and [decisions](docs/05-decisions.md).

The last assistant-managed device deployment was alpha2 (`03f18a5`); alpha3 was built locally and handed to the user. Its **55 focused JVM tests**, assembly and Lint results do not override the subsequent failed manual presentation test. Physical acceptance remains a joint user-led step. Historical receipts remain in the [delivery plan](docs/04-delivery-plan.md).

At the user's request, `main` now starts at the independent P.O.D.S. snapshot **`781b4b6`**, with an identical source tree and no HyperPods commit ancestry. The former local history, including preview implementation `5013f04`, was verified and archived outside the repository before rewriting. Historical receipt hashes refer to that archive, not the new branch ancestry.

## Project boundaries

- App/module title: **P.O.D.S.**; release application ID `com.hxlh50k.pods` (unsigned), isolated debug ID `com.hxlh50k.pods.debug`. The Kotlin/Android namespace remains `moe.chenxy.hyperpods`.
- Per-app locales implemented: **English and Simplified Chinese (`zh-CN`) only**. Coding documentation and commit messages are English.
- Repository: [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid), created **private**; no initial push at this documentation checkpoint. Keep it private unless the user explicitly authorizes a visibility change.
- Preserve HyperPods attribution, pinned source provenance, and applicable [GPL terms](LICENSE), not its Git ancestry. Private hosting and independent history do not waive distribution obligations.

## Prerequisites and validation

Development uses Gradle 8.13, a JDK 22 toolchain, Android SDK 36, CMake 3.22.1, and the Android NDK selected by the build. The baseline used a JDK 17 launcher and cached distributions. After the initial download timeout, the wrapper now bootstraps `8.13-bin` using this environment's existing proxy process-locally; its network timeout is 60 seconds. See [build configuration](app/build.gradle.kts) and the separately [recorded build/test results](docs/04-delivery-plan.md#9-measured-baseline-checkpoint).

Runtime work targets the rooted `houji` test phone, `OS4.0.0.14.XNCCNXM`, Android 17/API 37. **Daemon-reported API 102 is verified**, and a read-only official manager UI check also showed API 102 active. P.O.D.S. host injection and new API calls remain **unverified** until app testing. Legacy Xposed and a dedicated rootless backend are out of scope. See the [framework evidence and CLI boundary](docs/07-device-and-native-integration.md#read-only-daemon-status-follow-up).

Hardware-gated validation requires the user's AirPods 5 wireless-charging variant (reported firmware `1.0.0`). Xiaomi Open-ear Headphones Pro (reported `1.1.0.3`) provide a native-behavior comparison. Neither imported tests nor synthetic previews prove real AirPods 5 control, lid detection, or native-window compatibility. Do not install or restart system hosts outside the controlled test plan.

The debug app uses the same three declared scopes. Live restart behavior remains unverified. Alpha1's AboutPage automation was blocked by old Espresso reflection; test-only dependencies now use the official stable fix. No autonomous host-process restart or physical earbud manipulation is part of this iteration.

## Next joint check: debug scope reload

1. After installing the next debug APK, open the module-loading diagnostics while the **old** module is still running in the hosts. Do not preemptively reboot just to make the new action available.
2. When no call is active and Bluetooth interruption is acceptable, explicitly confirm the new full debug reload. The expected required hosts are Android Bluetooth, Xiaomi Bluetooth main and SystemUI; an observed Xiaomi `:ui` process is handled separately.
3. Read the per-host result. Success requires replacement-process evidence and the currently installed build, not merely a successful signal. If a host stays old or disappears, report that exact outcome; do not loop restarts or change the framework automatically.
4. Only after the reload path is understood, agree on a separate island-rendering round. Pure notification-free animation and the dedicated native control panel remain distinct follow-up work.

The user has completed the initial basic-function check and requested continued development. Future hardware interactions still remain user-driven; hand off a verified preview or capture procedure rather than autonomously manipulating the headphones.

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
