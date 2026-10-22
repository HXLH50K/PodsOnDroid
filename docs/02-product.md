# 02 Product scope and interaction

Status: confirmed design baseline; imported functionality is not automatically verified P.O.D.S. functionality. API/build/restart changes are in progress. The app follows a Liquid Glass visual direction; system behavior should resemble native Xiaomi headphones, not recreate Apple's ecosystem. See [readiness](09-readiness.md).

**2026-10-23 battery contract:** [Battery notification specification](10-battery-notification-spec.md) supersedes older notification/reminder details below. Alpha3's brief flash failed the requested visible five seconds. Alpha4 implements independent left/right colors, editable Low20/Medium50, separate switches, falling-edge reminders and first-read suppression, with no active case/implicit10% alert path. Per user instruction it is packaged without tests; real timing/rendering and behavioral acceptance remain open.

**Latest manual feedback, 2026-10-22:** paired Xiaomi headphones reconnect silently when the case opens and show no extra insertion animation; discovery/first pairing displays a connect card. This is evidence for the user's tested model/ROM, not every Xiaomi device. P.O.D.S. intentionally differs on paired reconnect by showing one brief battery island, then **no ongoing island and no ongoing notification**. First-pairing popup work is provisionally deferred following the user's suggestion; it has not been permanently cancelled. The dedicated Interconnectivity control panel remains required.

## 1. Constraints

- Exact label **P.O.D.S.**; Android application ID `com.hxlh50k.pods`; retain source namespace `moe.chenxy.hyperpods` for now. Use original or clearly licensed icons.
- Initially one user, the verified Xiaomi HyperOS 4 test phone, and one active AirPods headset. No multiple-headphone manager or proprietary multipoint; do not change existing headset multi-host behavior.
- App languages: **English and Simplified Chinese (`zh-CN`) only**, with resource-based strings. Coding documentation and commits: English.
- Support light/dark themes, system font scaling, and TalkBack from the first release.
- Offline operation, no account, ads, automatic log uploads, or default network permission.
- Android owns pairing and A2DP/HFP audio; the module adds controls rather than replacing the audio stack.
- Require root and runtime-verified modern API 102. No legacy framework support or dedicated rootless backend.
- Preserve applicable HyperPods controls and tests. A UI redesign must not silently remove controls or change saved/default policies without evidence.
- Hosting is private; the latest privacy instruction supersedes earlier public intent.

Target: AirPods 5 wireless-charging variant, user-reported firmware `1.0.0`; read model/protocol fields when connected. Initial platform: `houji`, `OS4.0.0.14.XNCCNXM`. Xiaomi 17 Pro is the daily phone, not an additional starting gate. Native comparison: Xiaomi Open-ear Headphones Pro, reported `1.1.0.3`; finish the current verified stage and wait when hardware is needed.

## 2. Phased feature scope

| Feature | Initial release | Later | Limit / acceptance focus |
| --- | --- | --- | --- |
| Installation and capability self-check | Required | More compatibility profiles | Separately check permissions, Bluetooth, control, hooks, and cards |
| Select a paired AirPods device | Required | No multi-headphone UI planned | A name containing AirPods is not authentication |
| Left/right/case battery | Required | Optional additional BLE discovery | Independent observations; never invent live case battery |
| ANC/transparency/off/adaptive | Capability-gated | Firmware differences | Unsupported modes unavailable; off mode may require a separate allowance |
| Wear state | Required | Better diagnostics | Unknown is not unworn; primary/secondary need not mean left/right |
| Remove-to-pause / reinsert-to-resume | Preserve and regress | Adjustable policy | Inventory defaults; resume only the same media session actually paused by this policy |
| Brief connection battery island | Required | Tiles/widgets | No persistent island or ordinary status notification after the initial window |
| Device/firmware info and diagnostic export | Required | Compatibility summaries | Redact by default; sharing is manual |
| Low/Medium battery reminders | Alpha4 implementation, untested | Joint verification still required | Defaults20/50, independent switches; initial-at/below readings establish a baseline without alerting |
| Conversation awareness / personalized volume | Preserve and regress | Firmware fixes | Keep additional phone attenuation distinct from the headphone setting |
| Long-press cycle, microphone, tones, swipes | Preserve and regress | More settings | Actual case variant/capabilities gate controls; sent is not confirmed |
| Single-ear ANC / sleep pause | Preserve and regress | More firmware | Do not continually overwrite user operations |
| Discovery/first-pairing connect popup | **Provisionally deferred** | Optional native quick-pairing enhancement | Initial pairing uses system Bluetooth settings; do not require continuous lid discovery for reconnect UX |
| Dedicated Interconnectivity headphone panel | **Required, M3** | More quick controls | Real state/actions; neither full app nor Bluetooth-settings redirect |
| Dual-ear connection battery island | **Alpha4 reworked after alpha3's brief flash** | Verify native lifecycle and renderer jointly | Five seconds from observed dual-ear content, independent color indicators, then no persistent remainder |
| Bluetooth details/battery integration | Regress applicable existing behavior | New privileges in M5 | No unnecessary KSU systemization package |
| Head gestures | Keep experimental | Hardware verification | Not an initial completion claim; no default phone permission |
| Rename | Preserve and regress | Naming consistency | Separate local alias and device name; explain possible re-pairing |
| Liquid Glass app UI | **M4** | Refinement | Framework/function before visuals; no background animation cost |
| R01 scoped-process restart | **M1 contract, full M4 acceptance** | More verified hosts | Confirmation, allowlist, actual new-build verification |
| Lid observation diagnostic | **Debug alpha2 only** | Validate the AirPods 5 lid contract | Opt-in 30-second capture, manual open/closed markers, redacted export; not a popup or production detector |

The active release route prioritizes connection-only presentation, M3's dedicated control panel and M4's stability/visual work. First-pairing popup acceptance is suspended under the provisional deferral, not marked passed. Retained code does not inherit hardware acceptance.

## 3. Explicit exclusions

- Apple ID/iCloud sync, Find My network, UWB precision finding, remote case sound.
- Firmware flashing/downgrading/update servers or a guarantee that initial setup never needs an Apple device.
- System head-tracked spatial audio, Dolby/HRTF redesign, forced codecs.
- High-quality call-recording injection that bypasses HFP.
- Hearing tests, hearing-aid/medical use, or generated hearing parameters.
- Default SDP VendorID impersonation, forced takeover from another host, global Bluetooth reset.
- Initial gesture call answering/hangup for third-party apps such as WeChat/QQ.
- Root manager, framework downloader, silent Zygisk/LSP installation, SELinux disabling.
- Compatibility promises for other OEM ROMs, special foldable/tablet system cards, work profiles/multiple users, or counterfeit AirPods.
- Dedicated rootless, API 101, and legacy-Xposed compatibility work.

Future additions require a recorded decision and separate acceptance, not silent scope expansion.

## 4. Screens and native entry points

### Onboarding

1. Explain verified support and the current environment. Detecting root does not automatically request it.
2. Verify runtime API 102, loaded scopes, and system component versions. Request Bluetooth permissions only when app-side APIs need them; pairing remains in system UI.
3. Select a paired device. Allow refusal of optional notification permissions and explain the consequence.
4. Check audio profile, classic control socket, protocol handshake, and capabilities independently.
5. Explain scope/restart/rescue requirements for Bluetooth, connection presentation, and Interconnectivity separately. First-pairing popup adaptation, if resumed, gets its own gate. Ticking a framework scope is not proof it loaded.
6. Keep diagnostics and system Bluetooth settings available on failure; do not loop authorization/install prompts.

Preserve page information/control logic before moving the app to Liquid Glass. Native popups/panels share state/actions but remain independent entry points; opening the home page does not replace them, and system-wide windows must not all inherit the app theme.

### Home

- Prefer a local alias, then a confirmed device name; use a generic icon for unknown models.
- Distinguish Bluetooth off, disconnected, audio connected/control connecting, control ready/some reports absent, and control unavailable/audio unaffected.
- Show left/right/case battery and charging independently. Missing valid data is `--`, not `0%`.
- Unchanged valid event-driven battery while connected is not automatically stale failure. After disconnect, label retained readings with time. Hide an offline case by default while allowing history inspection.
- Modes follow verified capabilities. A single row is acceptable at ordinary sizes; compact/large-font layouts may wrap instead of shrinking hit areas or cutting labels.
- Show sending feedback immediately. Identify a mode as current headphone state only after a valid observation.
- Unreadable settings may show labeled defaults/local choices without locking controls forever. After a successful write show **Sent, no headphone report** where appropriate. Defaults are not reported state and page opening never writes them.

### More settings

| Category | Examples | Explanation |
| --- | --- | --- |
| Headphone-persistent setting | Long press, single-ear ANC, conversation awareness | May affect behavior on other hosts |
| Phone-side policy | Wear pause, low-battery alerts, notifications | Only affects this phone; not a read-back headphone setting |

Unsupported capabilities must not have active controls. Plausible but unverified support belongs in a clearly explained experimental area, not automatically in the unsupported category.

### Diagnostics and about

Show understandable outcomes and next steps rather than a wall of packets:

- Audio/control connection, handshake stage, and which fields have been received.
- Host session, API 102/plugin loading, whether a native patch is needed/applied, lid and MiLink adaptation state.
- Framework and scope evidence, matched ROM/Bluetooth profile, current versus historical failure.
- Latest operation: requested/queued/written/reported/unconfirmed/failed.
- **Reconnect control only**, **Stop enhancement**, **Open system Bluetooth settings**, **Copy redacted report**.
- **Restart scoped processes**, with host list and impact confirmation; Bluetooth restart interrupts other connections and is not ordinary reconnect.
- Version/build source, licenses, third-party attribution.

Any future **Disconnect headphones** action must be distinct from reconnecting control because it affects audio.

### Discovery / first-pairing popup (provisionally deferred)

- Do not show a modal popup for every paired reconnection. Initial pairing remains available through Android Bluetooth settings. The following constraints apply only if optional quick-pairing work is resumed.
- Unpaired discovery needs explicit identity confirmation, never automatic binding to a stranger's nearby device. A discovered advertisement is not proof the device belongs to the user.
- Prefer verified HyperOS quick-connect/animation containers, with AirPods-5-specific state/capabilities rather than forced Pro/old-model templates.
- A2DP is not a lid sensor. Verify advertisements, opening/closing semantics, and random-address correlation; a battery report is not inherently a new opening.
- Show name, valid part batteries, and connection progress. Label broadcast-only battery as estimated, not precise AACP data.
- At most one popup per lid cycle. Repeated broadcasts, mode reports, or reconnects cannot spam it; manual dismissal suppresses it for the rest of that cycle.
- Match observed Xiaomi comparison behavior for lock-screen, landscape, and full-screen apps. Until then keep host restrictions: no arbitrary screen wake, lock bypass, or forced overlay.
- Suppress competing old system logic only for verified devices the new adapter can actually own; neither blindly remove all old suppression nor suppress all other headphones.

### Dedicated Interconnectivity panel

- Clicking the target AirPods 5 card opens a dedicated small panel, not Bluetooth settings or the full P.O.D.S. Activity.
- First view includes name, valid battery, supported modes, and a **More settings** entry, with consistent focus, close/back/reopen behavior.
- Commands use the same queue and reports as the app. A legacy three-mode host must not lose the adaptive fourth mode.
- Do not fake Xiaomi-only protocols, cross-device transfer, or firmware upgrades. Define any disconnect action separately.
- Native container plus data/control adaptation is first choice. A custom small-panel fallback needs evidence and approval; a full-screen redirect never counts as passing.
- Unknown/Xiaomi/other-brand devices keep original behavior. Safe fallback must be labeled adaptation failure, not success.

## 5. Behavioral rules

### Connection and lifecycle

After initial authorization/enabling, follow system paired-device events to establish control, without taking over another host. Disabling enhancement must stop host-side actions; do not use root to revive them secretly. Explain that system hooks outlive the settings Activity, so force-stopping the app and disabling enhancement may differ.

Share system scan events where possible; limit extra scanning/wakeups. Control failure cannot trigger unlimited high-frequency scanning or second-by-second retries. After bounded attempts/cooldown, wait for a real external event or explicit retry.

### Media and volume

- Prefer readable device values; otherwise label local/default values and do not write them automatically.
- Existing wear-pause is a phone-side policy with baseline default `true`. Preserve saved choices and single-/dual-ear rules before changing anything; no invented last-ear-only or 30-second policy.
- If resume ownership cannot be established, expose **Auto-pause only** rather than guessing via a global play/pause toggle.
- Preserve separate conversation-awareness and extra phone-volume controls. The extra attenuation baseline default is `true`; keep user choices.
- Manual volume changes, route changes, disconnect, and session replacement cancel old volume restoration.

### Notifications and degradation

- No ongoing connection/status notification, even at silent/minimum importance. State continues to feed the application and dedicated control panel without notification posts.
- A necessary presentation fallback must be transient and share the same original deadline. Missing native presentation cannot justify a persistent ordinary notification.
- A brief island consumes a real new audio-connection event once. Socket recovery, page reopening, partial battery reports, and setting reports are not new connections.
- Display valid battery values available during the initial window; unknown is `--`, not invented zero. Late battery updates may update that same window without replaying its animation or extending the deadline.
- Count five seconds of useful visibility, not the time since the underlying audio event; startup/data delay must not consume the visible window. See the battery spec for readiness limits and timing evidence.
- End the alert with bounded cancellation; post-expiry data must not post a replacement notification, a case-battery toast or another island unless a genuine new threshold event qualifies.
- Low/Medium reminders require a new downward crossing by an eligible ear. First values, repeated values, ordinary1% declines below a crossed threshold, settings edits and charging are not new reminder events.
- On upgrade, clean only notifications proven to belong to this P.O.D.S. installation. A `BTHeadset` tag or Bluetooth UID alone is not ownership proof; independently running HyperPods and genuine Xiaomi notifications must not be deleted.
- Denied notification permission is not control failure. Follow actual foreground-service rules if a service is required.
- Keep legally required service notifications separate from island presentation; do not hide required notifications or emulate every system surface using overlay permission.
- Do not repeatedly notify successful background retries.

### R01

Offer UI-only and all-verified-host presets, disclose impact, handle exact verified PIDs only, and verify the module build in restored hosts. No system-core/unrelated process actions, data wipe, automatic reboot, or automatic connection-recovery use. This is not hot reload; see the [full contract](09-readiness.md).

## 6. Candidate experience targets

These are proposed engineering thresholds, not achieved results or individually accepted contractual values. Measurement details are in the [acceptance plan](04-delivery-plan.md).

- Accessible interaction feedback within **100 ms**.
- Read-back mode confirmation **P95 <= 1.5 seconds** when control is ready.
- Home opening, back, and scrolling never blocked by Bluetooth I/O.
- Touch targets **>= 48 dp**; ordinary-text contrast **>= 4.5:1**.
- At **320 dp**, no essential action lost at **1.0/1.3/1.6x** font; **2.0x** may reflow rather than clip.
- No default network requirement, permanent extra scans, or wake locks. Missing OEM cloud resources must be reported; offline host assets cannot be assumed complete.
- Glass effects run only while visible; no useless idle refresh or background rendering/animation wakeups. Reduced transparency/motion retains every control.
