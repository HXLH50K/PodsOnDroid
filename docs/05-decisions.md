# 05 Confirmed decisions and remaining choices

Updated: 2026-10-22. This is a faithful English decision summary, not a verbatim transcript. The original Chinese user answers were preserved byte-for-byte outside the repository before import. Historical Chinese upstream documents remain in the reference project and external history backup, not the independent P.O.D.S. branch.

Latest decision: remove inherited HyperPods commit ancestry, keep the source/license/attribution, and continue development. The user reports the migration preview's basic functionality works and the original issues remain as expected. This does not declare native popups, the dedicated panel, or Liquid Glass complete.

Latest test feedback supersedes the older presentation assumptions: paired Xiaomi reconnect and wearing are silent; first-pairing/discovery shows a connect card on the tested setup. The user wants a brief battery island on P.O.D.S. reconnection, **without either the inherited persistent island or ordinary notification**. They suggested that the first-pairing popup might be unnecessary. A confirmation attempt was unavailable, so implementation provisionally defers that separate feature rather than treating its permanent removal as confirmed. Notification cleanup proceeds independently.

**Latest instructions take precedence:** the repository is private; all coding documentation and commit messages are English; the app supports English and Simplified Chinese (`zh-CN`) only. The earlier public-release intention is superseded.

**2026-10-23 morning priorities:** no further computer shutdown. The highest priority is an effective debug **full-scope process reload** after APK updates, not only interface-process restart. Battery presentation must ultimately be a direct native animation without an ordinary notification/card; use left/right earbud icons, not battery gauges. Alpha4's notification-to-island transition did not satisfy that requirement. Fixing its expansion flag is only an interim visual change, not proof of a pure native-animation route.

**Alpha5 manual follow-up:** the user confirms scope restart is effective. Two remaining display requirements are explicit: no ordinary notification before the island, and untinted pictorial earbud images with threshold colors on percentage text only. Another notification-backed hiding/expansion adjustment does not satisfy the first requirement.

**Battery requirements, 2026-10-23:** the user reports the current island only flashes and specifies five seconds of dual-ear battery, independently colored above Medium/below Low/between thresholds, editable Low20/Medium50 defaults, independent low/medium reminder switches, and one-shot downward crossings without initial-low alerts. See the authoritative [battery notification specification](10-battery-notification-spec.md). Its per-ear/per-category/per-connection budget and both-switches-on first-install defaults are explicitly proposed; clarification was unavailable. They are not retroactively presented as user answers, and no code changes are authorized merely by writing the draft.

There is no unresolved product question blocking M0/M1-A. Source/history import has happened; API migration, identity/build changes, and restart implementation are in progress. Completion claims require their own evidence. See [stage boundaries](09-readiness.md).

## 1. Device and baseline decisions

| ID | Confirmed answer | Engineering consequence / remaining evidence |
| --- | --- | --- |
| Q01 | Daily phone: Xiaomi 17 Pro; use the currently connected test phone first | Do not require a second adaptation or claim the daily phone passes because the test phone does |
| Q02 | AirPods 5 wireless-charging variant; reported firmware `1.0.0`, unchanged since manufacture; connect when needed | Read actual model and firmware fields then. Complete the independently verifiable stage and wait for the user, without continuous polling |
| Q03 | API 102 only | Daemon-reported API 102 is verified. Actual P.O.D.S. host injection and new API calls remain M1 app-test requirements, not reasons to reopen the framework-route decision |
| Q04 | HyperPods Actions latest daily; missing connection popup, persistent island and repeated battery-refresh alerts, some controls/feedback unreliable | Pin `a44bc10e387f527062b9ba2b8f90b154b2e61248` as the imported baseline. Obtain the exact daily run/hash when comparing a reported defect; never lock a build to rolling `latest` |
| Q05 | User owns the project; **latest instruction: private** | [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid) is created private, with no push at this checkpoint. Retain GPL/attribution; do not make it public without explicit authorization |

## 2. Experience and functionality

| ID | Confirmed answer | Engineering consequence / remaining evidence |
| --- | --- | --- |
| Q06 | Dedicated Interconnectivity panel remains required; first-pairing popup necessity is being reconsidered after the latest Xiaomi test | Provisionally defer optional quick-pairing; paired reconnect uses a brief battery island only. This does not downgrade the dedicated control panel to a full-app/settings redirect |
| Q07 | No ranking of advanced settings supplied | Preserve applicable existing functions; prioritize control feedback, native entry points, and notification disturbance instead of asking again |
| Q08 | Show headphone settings when readable; otherwise show defaults that change after user interaction | Label defaults/local choices as not read from the device. Reading a page must not write defaults; write failures cannot appear successful. Phone-side policies remain separately identified |
| Q09 | Lock-screen, landscape, and full-screen behavior should follow Xiaomi headphones | Compare against the supplied Xiaomi model; preserve host restrictions before observation instead of inventing behavior |
| Q10 | Keep phone-side conversation volume reduction; it already exists | Preserve the feature, baseline default, and saved user choices; do not silently remove it or default it off |
| Q11 | No multiple-headphone or proprietary multi-host manager | Control one active headset; do not alter the headset's existing multi-host behavior or forcibly take ownership |
| Q12 | Apple Liquid Glass visual direction, with projects such as keimi as references | Apply to the native Android app, not all SystemUI. The precise reference URL/license is not established. **Latest language scope: English and Simplified Chinese only** |

The island must appear **briefly for a real connection**, then leave **no persistent island or ordinary status notification**. Battery/settings updates must not create another connection alert or a replacement status entry. The earlier alpha2 choice to keep a silent status notification did not satisfy this requirement.

## 3. Engineering, authorization, and delivery

| ID | Confirmed answer | Engineering consequence / remaining evidence |
| --- | --- | --- |
| Q13 | Android application ID `com.hxlh50k.pods` | Audit authorities, actions, notification identity, self-checks, and relevant JNI/resource assumptions. Retain source namespace `moe.chenxy.hyperpods` for now; application ID and namespace are distinct |
| Q14 | Create a new signing key in a new folder under `C:\Users\25806\OneDrive\keys`; use Cowpea Release Key as an organizational reference | Do not inspect/reuse existing private keys or passwords. Final password custody and independent backup are release-stage choices; use an isolated test identity meanwhile |
| Q15 | Optimize background power use as far as practical | Numerical targets remain engineering proposals, not individually accepted contractual commitments |
| Q16 | Non-destructive work on the connected rooted ADB test phone is authorized; take care | Allows controlled probe installation and verification, not data wipes, flashing, kernel/framework replacement, headset reset, or unrelated-module changes |
| Q17 | User is unsure about other-controller interference and thinks it may be unrelated | Inspect relevant conflicts read-only; explain only evidence-based interference. Do not disable a module merely because a directory looks suspicious |
| Q18 | Prefer one LSP module; add KSU only if genuinely necessary | Root-assisted scoped restart does not itself justify a second KSU package |
| Q19 | GitHub was the tentative delivery channel; no elaborate early release system | The private repository now exists. No automatic updater or unauthorized upload; publication and visibility are separate decisions |
| Q20 | Diagnostics, packet capture, and screenshots are authorized | Collect locally, minimally, and with redaction. This is not authorization to publish raw logs, system APKs, account data, or secrets |
| Q21 | User did not yet understand the alternative-panel proposal | Native integration remains first choice. No approval for a custom fallback is implied; demonstrate its differences only if native reuse proves unsafe or infeasible |
| Q22 | Xiaomi Open-ear Headphones Pro, reported firmware `1.1.0.3`; wait for connection when needed | Native-behavior comparison is hardware-gated, not a prerequisite for import or API migration |

**R01 is confirmed:** provide an in-app **Restart scoped processes** action to reduce whole-phone reboots after module updates and aid development. Require a host allowlist, impact confirmation, exact process identity checks, and verification that the new module build actually loaded. It is not hot reload and cannot guarantee reboot-free updates.

## 4. Native versus fallback panel

- **Native route:** feed AirPods 5 state and actions into Xiaomi's existing headphone panel.
- **Fallback route:** only if native integration cannot be made safe, the same Interconnectivity entry would open a small P.O.D.S.-rendered control panel.
- The fallback is still a small panel, not the full app or Bluetooth settings. Its necessity has not been demonstrated and the user has not approved it.

## 5. Decisions that remain deferred

1. Before generating the release key: password custody, delivery, and independent encrypted backup.
2. Before distribution: authorize the specific push/release operation and review what is included. The repository URL is already known; **private visibility remains mandatory**.
3. If native-panel reuse encounters a demonstrated hard limit: present the alternative and obtain the user's decision.

Exact headphone model fields, the current daily run/hash, and specific Liquid Glass reference pages can be supplied when relevant. They do not block M0/M1-A. Whether the headphones were initialized on an Apple device matters only if a related feature fails.

## 6. Frozen implementation baseline

- Label `P.O.D.S.`, application ID `com.hxlh50k.pods`, retained source namespace `moe.chenxy.hyperpods`.
- HyperPods-derived source/history and applicable GPL/author attribution; independent signing identity.
- Current `houji` HyperOS 4 test phone first; AirPods 5 wireless-charging variant, reported `1.0.0`.
- One API-102-only LSP APK; no legacy compatibility or dedicated rootless implementation.
- First-release gates: real lid-open popup, native headphone panel, truthful settings feedback, stable existing controls, brief connection island, and safe R01.
- Liquid Glass in the app; Xiaomi behavior for system windows; background power is a priority.
- English coding documentation/commits; app English and Simplified Chinese only.
- User-owned **private** repository, no visibility change without explicit instruction.
