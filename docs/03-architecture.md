# 03 Architecture

## Current app page framework: alpha12

The user accepted ordinary blur as an interim material and prioritized overall
UI structure. `PageScaffold` reuses the installed Miuix 0.6.1 `TopAppBar` and
`SmallTopAppBar`; the child navigation icon is outside the scrolling viewport,
not a list item. Status-bar insets are applied once. Content retains the
floating-dock clearance and bring-into-view policy.

`PageTransitionHost` is shared by settings and headphone navigation, including
About. It retains saveable state per destination, applies a consistent
depth-based enter/pop treatment, and hides/disables outgoing interactions.
Focus is cleared only for the active tab on destination changes. Head-motion
and lid-observation UI sessions are deactivated when their page stops being
current; delayed permission results cannot start a departed capture.

The selected 260 ms motion and 140 ms exit fade are project tuning values, not
claims of a system design standard. Compose motion-duration handling remains
in use. This does not implement finger-progress-driven predictive back or
claim measured visual acceptance. The previous research below motivated this
framework; special glass/refraction work stays deferred.

## App design and material survey (2026-10-23)

Scope: research first, following the user's rejection of alpha11 and clarification
that Liquid Glass is a local control material, not the overall app style. This
survey changes no UI code, dependencies, installed APK or device state.

### Overall interface language

The supplied screenshot supports a descriptive classification: HyperOS-oriented
large titles, grouped cards/dashboard summaries and a floating translucent
navigation layer. It does not establish Keimi's source implementation, motion
rules, or use of a particular library. No matching authoritative public Keimi
source was verified; do not infer its stack from the screenshot.

The community [Miuix documentation](https://compose-miuix-ui.github.io/miuix/)
explicitly describes Xiaomi HyperOS-style components. This is a useful cohesive
component reference, not a public Xiaomi-system SDK. Google's [app-bar guidance](https://developer.android.com/develop/ui/compose/components/app-bars)
and [accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
provide Android navigation/interaction rules; for example, touch targets must
remain at least 48 dp. A component library does not choose the app's information
architecture, content density or emphasis automatically.

### What Liquid Glass does and does not specify

Apple's [Materials HIG](https://developer.apple.com/design/human-interface-guidelines/materials)
places Liquid Glass in a functional layer for navigation and controls above
content, and warns against treating it as the general content material.
Regular and clear variants have different readability/use cases. The
[SwiftUI guide](https://developer.apple.com/documentation/swiftui/applying-liquid-glass-to-custom-views)
describes optical appearance plus interactive behavior, with `glassEffect`,
`GlassEffectContainer` and coordinated morphing. Those are Apple-platform APIs,
not Android APIs and not a universal whole-app design standard.

Apple's [Landmarks sample](https://developer.apple.com/documentation/swiftui/landmarks-building-an-app-with-liquid-glass)
is a verifiable implementation reference for standard navigation/toolbars plus
selective custom glass. Use such inspectable references rather than guessing
the private implementation of commercial apps from promotional screenshots.

### Android implementation options

| Option | Verified capability | Limit / role in P.O.D.S. |
| --- | --- | --- |
| Android 12+ window blur | Public `Window.setBackgroundBlurRadius`, `FLAG_BLUR_BEHIND`, `blurBehindRadius` and availability listener | Cross-window frosting, suitable for the existing dialog; not a complete component/motion system or an intra-page dock API |
| RenderEffect / Android 13+ AGSL RuntimeShader | Compose/View rendering effects and programmable filtering/shading | Foundation for optical treatments; backdrop capture, refraction, highlights and interaction still need implementation |
| Haze | Public documentation provides backdrop visual effects; Tivi is a linked real-app example | Useful blur/source infrastructure; using it does not by itself establish Liquid Glass fidelity |
| Kyant Backdrop | Documented color/blur/lens pipeline; Android 13+ lens/refraction, optional depth/chromatic effects; bottom-bar/button/slider examples | A candidate for a small local-control comparison, not an automatic app redesign; README says high-level components are examples rather than built-in product controls |
| Miuix | HyperOS-style Compose component suite; current public repository lists navigation/blur/shape modules | Candidate for coherent components and transitions; verify compatibility with the pinned project version before importing newer examples |

Primary implementation references:

- [AOSP cross-window blur](https://source.android.com/docs/core/display/window-blurs):
  effects can be disabled by the device/runtime, requiring readable fallback.
- [Android AGSL](https://developer.android.com/develop/ui/views/graphics/agsl):
  programmable RuntimeShader behavior is available on Android 13+.
- [Haze documentation](https://chrisbanes.github.io/haze/latest/):
  current docs describe a modular v2 architecture, **not** the project's pinned
  Haze 1.7.0 API. Do not silently apply v2 recipes to the current dependency.
- [Backdrop effects](https://kyant.gitbook.io/backdrop/api/backdrop-effects.md)
  and [repository/examples](https://github.com/Kyant0/AndroidLiquidGlass):
  effects use RenderEffect; shader-dependent effects require Android 13+.
  The README links LiquidBottomTabs, LiquidButton, LiquidToggle and LiquidSlider.
- [Miuix source](https://github.com/compose-miuix-ui/miuix) and its
  [license](https://github.com/compose-miuix-ui/miuix/blob/main/LICENSE).

Haze, Backdrop and Miuix publish Apache-2.0 licenses in the verified sources.
That does not remove the need to preserve notices or validate dependency,
GPU, lifecycle and accessibility behavior. Backdrop's FAQ documents a possible
RenderThread crash scenario; prototype and verify rather than adding effects
blindly. The current project pins Miuix 0.6.1, Haze 1.7.0, Kotlin 2.2.21 and
Compose plugin 1.9.3. No dependency was upgraded during this research.

There is no verified public HyperOS call that converts an arbitrary app into
the complete screenshot's design. MiLink's private service/view infrastructure
is a separate system-panel concern, not the right route for ordinary app
headers, navigation, spacing or transitions.

### Confirmed problems in the current app

- `DashboardPageHeader` implements a back icon when a callback is supplied, but
  Settings and headphone child pages place that header inside a `LazyColumn`
  item. The return affordance therefore scrolls out of view. A specific report
  of an icon never appearing initially would need separate visual confirmation;
  the nonpersistent placement is already confirmed from source.
- Settings changes the destination enum and directly swaps the content; About
  is a separate early-return branch. There is no common enter/pop transition
  host. The headphone subpages and top-level pager have their own animations,
  so animation is not consistently absent; it is inconsistent across routes.
- Styling card fills and blur parameters cannot repair these navigation defects.

### Implementation recommendation after this survey

1. Establish a shared page frame: fixed child toolbar with a leading return
   action, root large-title behavior, safe insets and persistent readable
   navigation. Use the public app-bar pattern or an equivalent Miuix component.
2. Use a real back-stack/navigation host or an explicit shared AnimatedContent
   host. Define peer-tab, child-enter, child-pop and modal-dismiss semantics
   separately; preserve scroll and drafts, block outgoing interactions, and
   handle fast reversals and cancelled back gestures.
3. Use Android's [Compose animation guidance](https://developer.android.com/develop/ui/compose/animation/quick-guide),
   [Navigation guidance](https://developer.android.com/guide/navigation) and
   [predictive-back guidance](https://developer.android.com/develop/ui/compose/system/predictive-back)
   rather than instant replacement or unrelated per-page timings. Any initial
   200-300 ms motion range would be a project tuning proposal, not an official
   universal design requirement.
4. Validate a root/child pair and its navigation before revising all surfaces.
   Evaluate optional Backdrop optics only after the overall hierarchy works.
   Keep ordinary settings cards simple; do not multiply blur layers for effect.

The survey does not fix the reported header/transition issues and does not
claim visual acceptance. It intentionally stops before another APK iteration.

## Current alpha7 transport

The user has reported the complete debug scope reload as working. Battery presentation uses `NativeBatteryIslandBridge` in SystemUI and `NativeBatteryIslandSchema` against the locally inspected plugin controller. `NativeBatteryIslandEndpoint` resolves the OEM's lazy content proxy: plugin loading validates the endpoint without creating a window; the first real presentation reuses its controller or calls the verified creator once. The Xiaomi host sends only bounded primitive payloads; native objects and views remain in SystemUI. No Android notification is created or injected as a fake carrier.

- The native device-data path uses `DynamicIslandData` with property0/priority1, then `DynamicIslandContent.addDynamicIslandView(data, false)`.
- Refresh paints only the owned existing two regions; it does not call the OEM update method that can re-add content or restart timeouts.
- Actual owned pre-draw starts the five-second lease. Startup, detach, dismiss, expiry, plugin unload and cancellation all terminate the same owned generation.
- Zero initial width is not failure: native add initiates the SystemUI width handshake. At most one owned pending data object is allowed under the original startup lease. Pending-only cancellation removes that exact object and balances its native add event; late width replies cannot replay a removed pending entry.
- Exact object identity and key checks protect unrelated charging/device animations; busy native device content is not overwritten.
- Original model earbud resources are loaded without icon tint or image color filters. Each `TimerTextEffectView` percentage uses its own threshold color, with original appearance/accessibility/image state restored on release.
- The old notification manager is used only for proved-owned upgrade cleanup. It is not a display transport or fallback.

The alpha7 user-operated round confirmed this path reaching actual owned dual-ear pre-draw visibility and normal expiry exactly 5,000 ms later. That verifies the missing-island repair, not every visual or threshold acceptance case. Unknown or changed native schemas fail closed and report an explicit failure.

**Morning follow-up:** full debug scope reload is prioritized over another hardware notification test. The old interface-only runner cannot replace module code inside the Bluetooth main hosts. The update path must accept authenticated old-build reports, explicitly authorize process-exit disruption, preflight all required hosts and verify replacement builds. This does not enable API102 hot reload or computer power operations.

The current `BatteryIslandRenderer` still publishes a backing Android notification. Model-specific earbud icons and `enableFloat=false` separate icon/collapsing-card fixes from the stricter requested notification-free native animation. Existing `strong_toast_action` examples demonstrate another entry path, but do not establish its complete two-ear, update, cancellation and visibility contract for this ROM. Keep that adaptation separate rather than guessing new private template names.

**Specification override, 2026-10-23:** the [battery notification spec](10-battery-notification-spec.md) is authoritative. Alpha4 separates audio-session identity, useful-data readiness and native first visibility, using a new per-ear color/crossing reducer. This is compiled implementation, not a verified resolution of alpha3's flash; the user explicitly requested no tests before this package.

The active path is `BatteryNotificationSession` -> `BatteryNotificationHost` -> `BatteryIslandRenderer`. `BatteryIslandVisibilityHook` observes pre-draw of both native battery regions and reports a presentation token to the Xiaomi main-process owner; that acknowledgment starts the five-second deadline. The host watchdog cancels unconfirmed cards. Token/generation matching prevents old expiry callbacks from cancelling new cards. The old `LowBatteryReminder`/case/10% publisher is not invoked by this path. Shared settings are read by the Bluetooth host and passed with fresh per-ear observations and a control generation; settings edits rebaseline without generating battery events. A host with no proven audio-session continuity suppresses reminders until a genuine new connection.

## Notification iteration notes

Alpha2 established a single notification owner in the Xiaomi Bluetooth **main process** and a five-second A2DP-derived deadline, but incorrectly retained a separate silent status entry. The user rejects both persistent surfaces. Alpha3 removes routine status publication altogether: battery/setting/AACP updates feed internal state, may refresh only the still-active original presentation without re-alerting, and post nothing after expiry. Existing owned status/legacy entries require verified ownership before cleanup; Bluetooth tags alone are insufficient. The Xiaomi `:ui` self-check remains distinct from the notification owner.

The debug lid-observation tool is separate from production discovery and control. It uses an explicitly requested, foreground-only BLE scan with a 30-second deadline, 8 anonymous session candidates, 128 distinct records and 16 manual markers. Raw addresses exist only in a cleared in-memory association map; export retains selected clear header fields before byte11, relative timings, scan status/error and explicit drop counts. No automatic pairing, AACP writes, persistent radio scan, inferred lid state or identity claim is made. BLE permissions are confined to the debug manifest.

Status: implementation contract, not verification. Extend the imported HyperPods foundation with modern API 102. Android application ID: `com.hxlh50k.pods`; source namespace remains `moe.chenxy.hyperpods`. Target: AirPods 5 wireless-charging variant, reported firmware `1.0.0`. Protocol/native-window behavior is hardware-gated. App languages are English and Simplified Chinese only; coding documentation is English.

## 1. Decisions

| ID | Decision | Reason |
| --- | --- | --- |
| A1 | Keep the HyperPods single APK/project and useful controls/tests | Avoid simultaneous protocol rewrite, framework migration, and UI redesign |
| A2 | Require modern libxposed API 102; remove legacy dependencies/entry points | Target-102 modules cannot call legacy APIs |
| A3 | Initially retain the Bluetooth-host AACP session; separate responsibilities incrementally | No rootless-driven process move; fixes remain independently testable |
| A4 | KSU only for demonstrated privilege needs | Do not systemize the entire app just to keep it alive |
| A5 | Gradually extract pure Kotlin protocol/policy; isolate Android/OEM code | Reuse tests without making a structural rewrite a feature prerequisite |
| A6 | Native changes only for verified compatibility profiles | Reject unknown/ambiguous targets; never globally allow everything |
| A7 | System surfaces adapt state/actions; no direct AACP from UI | One writer; no whole Compose app loaded into SystemUI |
| A8 | Separate AirPods 5 discovery/window, MiLink card, and control adaptations | Old-model recognition is not new-model support |
| A9 | Brief island driven by connection events; reports only update data | Avoid persistent occupation and repeated alerts |
| A10 | App-side controlled scoped restart; hot reload disabled | Separate root management from injection and ordinary recovery |

## 2. Logical organization

Keep `:app` and the existing package structure initially. Extract Gradle modules only when testability/dependency isolation justifies them, not as a four-module rewrite prerequisite.

| Area | Responsibility | Boundary |
| --- | --- | --- |
| Protocol/state | Existing codec, queue, capabilities, session; gradual reducer extraction | Prefer pure Kotlin, independent of UI |
| Bluetooth-host runtime | Connection/device lifecycle, limited media policy, IPC | No card layout/notification styling |
| API 102/OEM adapters | Bluetooth, SystemUI plugin, MiLink process/version adaptation | No second controller or duplicated whole UI |
| App UI | Existing page logic, then Liquid Glass, settings, diagnostics, module management | No sockets or bypass of command states; no glass renderer in system hosts |

Use a small shared IPC contract, not one module per screen. No downloadable scripts, online plugin system, or remote hook configuration.

Initially retain Kotlin, Coroutines, Compose/Miuix, storage, and native build infrastructure. Reproduce the baseline before changing JDK/AGP/SDK for demonstrated API 102 needs. DataStore replacement is not a first-release prerequisite. Make visual dependencies a later isolated change, selected by license, performance, and maintenance; the name keimi alone identifies no approved dependency. See [migration](08-hyperpods-api102-migration.md).

## 3. Runtime topology

```text
P.O.D.S. settings app
  Compose UI <-> restricted state/command contract
                                |
com.android.bluetooth (API 102 hook)
  one AACP session + bounded writer + observed state
                                |
                     BR/EDR L2CAP -> AirPods 5

com.xiaomi.bluetooth main / :ui (API 102 hook)
  shared scan events -> AirPods 5 identity/lid state machine
  -> native quick-connect container, or separately approved fallback

com.android.systemui + dynamic miui.systemui.plugin ClassLoader
  target Interconnectivity card -> trusted system card route
                                |
com.milink.service:ui / other proven runtime hosts only
  native headphone panel <-> AirPods 5 state/action adapter
                                |
                      the same AACP session

Optional KSU:
  same signed APK + minimum privileged allowlist, only if required
```

Verify framework `apiVersion` at runtime; name/version branding and the presence of root do not establish module loading. Hot reload is off. Base KSU/Zygisk/LSP installation remains user-controlled.

### One control owner

- At most one P.O.D.S. control socket per device.
- No rootless Direct/Bridge switching. Keep session epochs; presentation adapters cannot open sockets.
- For target AirPods 5, explicitly delegate, replace, or suppress Xiaomi's old AirPods SDK control to prevent two command writers.
- Isolate HyperPods/LibrePods controllers during controlled tests rather than misdiagnosing competing controllers as transport defects. Do not disable unrelated software without authorization.
- A user APK and any KSU-bundled APK are the same signed product, not two parallel services.

## 4. Migration and layered self-check

1. Migrate metadata, entry, interceptors/Invoker, and plugin loaders; final artifacts contain no legacy calls.
2. Without headphones, record runtime API, actual host/process, and class-loading stage.
3. On a matched ROM, validate classic socket construction, handshake, and permissions; public LE/RFCOMM APIs are not substitutes.
4. Test whether a native negotiation patch is needed. Do not require patch success to connect on an already fixed stack; validate the native callback contract separately.
5. Report AirPods 5 identity, lid source, quick-connect container, MiLink service, card model, and action/report readiness separately.
6. Unknown builds/ambiguous targets are not injected. Preserve normal audio; a panel fallback is recorded as adaptation failure.

Track `HookLoaded`, `NativePatchApplied`, `ControlSocketConnected`, `HandshakeObserved`, and `SettingsAvailable` separately. A patch can be **Not needed**.

## 5. Protocol

### Connection and initialization

- Use authenticated/encrypted classic L2CAP for the paired BR/EDR device, AACP PSM `0x1001`.
- Advance handshake, initialization, and subscriptions on responses; allow only verified firmware variants that directly report state.
- Distinguish capability initialization from user settings. Never send a universal enable-everything packet sequence.
- Read exact model/firmware after connection. Keep both AirPods 5 case-variant capability distinctions; a case-name string or an old system table cannot authorize every command.
- Start reading before initialization writes; fixed sleeps are not acknowledgments.
- Synchronize mode, wear, battery, and identity independently. Missing case battery or a never-reported optional setting cannot block the entire session.

### Parsing and transport boundaries

- In M1 verify Android socket read semantics, maximum transfer units, truncation, and fragmentation with captures/tests. One `read()` is not assumed to equal one complete business message.
- Validate known opcodes by their actual structure: length, counts, enums, string terminators, and bounds. There is no assumed universal length field.
- Bound partial/multiple-frame and unknown-opcode handling; do not infinitely scan for plausible headers when frame length is unknown.
- Malformed input cannot refresh valid observation times or trigger controls. Log redacted error categories/counts.
- Bound waits, buffers, and writes. Close the socket on disconnect/cancellation to release blocking I/O; coroutine cancellation alone is insufficient.
- One writer, proposed queue capacity **32**. Explicitly reject a full queue; coalesce pending idempotent values per setting if appropriate, but never silently replay non-idempotent commands such as rename.

### Capabilities

```text
DeviceIdentity:
  paired-device handle, actual model, firmware, trusted identity source

Capability:
  Supported | Unsupported | Unknown | Experimental
  + evidence
  + conditions: firmware, prerequisites, permissions, backend
```

Unknown models may display structurally supported read-only observations. Writes require verified capabilities, not a generic `isPro` flag. Name and RSSI alone cannot authenticate a device.

## 6. State and command semantics

### Independent axes

| Axis | Examples |
| --- | --- |
| Environment | Missing permission, Bluetooth off, framework absent, profile mismatch |
| Audio | A2DP/HFP connected/disconnected, actual current output |
| Control | Idle / Connecting / Handshaking / Syncing / Ready / Degraded / Backoff / Stopped |
| Presentation | Standard notification, HyperOS panel available, adapter disabled |

`Ready` means initialization for that device's verified commands is complete, not that all optional fields were reported. Each setting has its own availability.

### Observed fields

```text
ObservedValue<T>:
  value: T?
  origin: ControlReport | BleEstimate | LocalPreference | Retained
  observedAtElapsed / displayTimestamp
  connectionEpoch
  availability: Known | Unknown | Offline | Retained
```

- Use monotonic time for intervals; wall clock is for display.
- Left, right, and case have independent values/timestamps. A partial report cannot refresh absent parts.
- Keep `NEED_AGAIN`, invalid percentages, offline, and real `0%` distinct.
- Cache is not authority in a new session. Stable event-driven values do not cause reconnection merely because time passes.
- Disconnected cache cannot trigger new low-battery alerts or media actions.

Settings separately expose `displayValue`/`displayOrigin`, including `LocalChoice` and `DisplayDefault`, without populating `reportedValue` or changing report timestamps. Q08 permits labeled defaults for settings, not invented battery/wear/model data. Displaying a default must not write it.

Phone wear/volume policies remain distinct from headphone settings. Both baseline phone policies default to `true`; preserve user choices and add device-setting mappings only with protocol evidence.

### Operation lifecycle

```text
Requested -> Queued -> Written
                      |-> ReportedMatching
                      |-> ReportedDifferent
                      |-> Unconfirmed
                      |-> Failed
```

- Queued means locally accepted; Written means locally written.
- Most settings have no universal transaction ID. A matching report means the observed current value matches, not that the packet is a uniquely correlated ACK.
- Keep desired and reported values separate; actual reports remain authoritative.
- Serialize/coalesce idempotent requests by setting. Late reports for superseded requests must not incorrectly complete a newer request.
- Without a known read-back contract, a successful write may display the local desired value labeled **Sent**. Queue/write/connection failures remain explicit, not hidden by optimistic UI.
- Do not endlessly resend for absent reports or override changes made by headset controls/another host.

### Session consistency

Increment `connectionEpoch` on connection, device change, or any future backend change. Async callbacks, timeouts, subscriptions, media restoration, and diagnostics carry the epoch. Old work cannot close a new socket or overwrite a new session.

## 7. Timeouts, recovery, and background work

Initial proposals to calibrate in M1 and lock into tests:

- Socket connection limit **8 seconds**; handshake/necessary-state budget **12 seconds**.
- Per-write watchdog **3 seconds**; readable settings without a matching observation after **3 seconds** become unconfirmed.
- At most **3** connection attempts per recovery cycle, including the first; waits **1 second**, then **2 seconds**, with small jitter. Whole-cycle cap **65 seconds** prevents nested retry multiplication.
- After exhaustion, cooldown **at least 60 seconds**. Cooldown ending alone does not start perpetual retries; wait for a real device/Bluetooth event or user action.
- No periodic battery report after synchronization is not failure. Recover for explicit EOF/I/O failure or necessary initialization timeout.
- Close only the module's control socket; never automatically disconnect A2DP/HFP or restart system Bluetooth.

Retain host connection-event lifecycles rather than adding rootless CDM/FGS keepalive. Initialize each Xiaomi Bluetooth main/`:ui`, SystemUI plugin, and MiLink listener once; release subscriptions at the correct lifecycle boundary. Native window ownership does not grant ordinary app background-start privileges. Any needed app service must follow Android FGS rules.

Enhancement must obey the saved enabled state even when the settings app is absent. After host/framework restart, revalidate policy/session without resetting headphone settings. No second-level WorkManager keepalive, permanent WakeLock, or battery-policy circumvention for scanning.

## 8. Permissions and IPC

### Minimum permissions

| Purpose | Plan |
| --- | --- |
| Paired communication | Existing Bluetooth host; app requests `BLUETOOTH_CONNECT` only if using app-side APIs |
| Lid scanning | Share system scan stream; `BLUETOOTH_SCAN` only for actual app scanning |
| Ordinary notifications | `POST_NOTIFICATIONS` according to platform requirements |
| Long-lived control | Host lifecycle, not a new rootless keepalive service |
| Media pause/resume | Inventory/regress existing host ability for the target route/session only |
| System battery/special routing | Evaluate individual privileged needs in M5 |
| Phone, location, microphone, contacts, accessibility-service, network | No MVP permission request |

Keeping media behavior is not unlimited delegated authority. Any notification-access alternative requires a separate explanation. Adding `uses-permission` does not give an ordinary APK MiLink `SHOW_CARD` access; retain the trusted system route and never weaken global checks.

### Cross-process contract

Inventory/test existing trusted broadcasts/providers before deciding on Binder; do not rewrite all IPC and hooks simultaneously. New MiLink consumers follow the same boundaries:

- Narrow typed commands/state, not arbitrary shell, file paths, reflection, or raw Bluetooth writes. Version any AIDL/Binder contract.
- A restricted Provider handshake returning Binder can be evaluated; an exported Intent or random token alone is not caller authentication.
- Check actual `Binder.getCallingUid()` and package ownership **before** clearing identity; never trust a supplied package-name string.
- Authorize only verified host identities for their specific roles, not every system UID. Shared UID establishes a shared trust boundary; text cannot authenticate an individual process within it.
- Hosts verify P.O.D.S. package, UID, installation user, and release certificate. Enforce primary-user scope; no default cross-user privilege.
- Injected code runs with the **host** UID/signature. A P.O.D.S. signature permission does not automatically authorize Bluetooth; design a restricted handshake instead of pretending injected code has the module APK identity.
- Test other packages sharing a UID, Binder death, protocol version mismatch across upgrades, epochs, replay, and oversized messages. An already-compromised root/system is outside the guarantee.
- Disconnect/disable closes control. Presentation subscriber death releases that subscription only; closing a panel cannot close control/audio. Host callback threads enqueue rather than block on I/O.

## 9. OEM isolation

Compatibility profiles record:

- Device, ROM fingerprint, Android API/security patch.
- Actual Bluetooth package/version, APEX or native build ID/hash, ABI.
- Framework name/version/API and injection outcome.
- Xiaomi Bluetooth, SystemUI/plugin, MiLink active versions and processes.
- Required classes/method signatures, enabled features, passed cases.

Rules:

- Native matching must be unique with verified memory ranges/ABI. Reject unknown builds; no global success return.
- If a negotiation patch cannot be restricted to the target channel, disclose possible whole-phone Bluetooth impact and require other-headphone/watch/car regression before release.
- Bluetooth control, lid popup, Interconnectivity panel, and island adapters have independent switches. Do not default to `system_server` or every similarly named interconnectivity package.
- Modify only verified owned entries and restore reused views; no shrinking other notifications, global dimensions, or gesture regions.
- OTA/APEX/MiLink mismatch disables the affected adapter while preserving ordinary UI/audio. Expire prior success state and report that the required panel is unavailable.
- Java failures may degrade safely, but ordinary try/catch cannot contain native crashes. Version rejection plus external disable/rescue procedures are prerequisites, not a promise of crash immunity.

## 10. Data and diagnostics

- Keep existing private preferences initially; introduce DataStore only when a justified migration requires it. Cached headphone settings must not be written wholesale on every connection.
- Never read pairing keys, retain Apple accounts, or export raw MACs/serial numbers.
- Map local device identifiers with an installation-specific random salt; exclude salts/device handles from exports.
- Proposed event ring: **256 entries**, plus the last important failure. Do not continuously log motion streams/full packets.
- Reports contain versions/profile, stage, error code, retries, timings, and field-received flags; preview before sharing.
- Errors include permission denial, API mismatch, framework absence, native mismatch, connect/handshake timeout, parser rejection, write failure, full queue, unconfirmed value, IPC death, and presentation failure.
- Use restricted FileProvider/system sharing, not broad storage access.
- Always offer **Stop enhancement**. Normal audio after disable, permission removal, and uninstall are required tests.

See [delivery](04-delivery-plan.md) and [signing/rollback](06-signing-and-maintenance.md).

## 11. AirPods 5 native-presentation contracts

Do not copy Xiaomi closed-source implementations. Separate:

1. **AirPods5Discovery:** decode physically verified advertisements, associate the paired device, and emit lid-cycle identity/confidence. Never derive BLE IDs from AACP `Axxxx` product model numbers.
2. **NativePresentationAdapter:** project the shared snapshot into quick-connect/card containers. Use target-scoped in-memory mappings, not cloud-config edits or permanent impersonation of Xiaomi/old Pro devices.
3. **NativeControlAdapter:** map actions into the existing queue and feed actual reports back, handling three-versus-four modes, offline case state, and non-reporting settings.

Popup, panel, and island share a device/session snapshot. Do not feed BLE estimates and AACP state back into each other as fake new observations. Suppress competing old logic only for verified targets; other devices keep the complete original call chain.

## 12. Connection island and R01 management

- Give true audio-connection cycles their own event identity, separate from the control socket's `connectionEpoch`.
- Consume each event once; end with host lifecycle/bounded timing. Data refresh cannot extend the deadline or create a persistent island.
- Paired reconnection intentionally uses only a brief battery island; it is not a detected lid event. First-pairing/discovery popup work is provisionally deferred, separate from the still-required dedicated control panel.
- R01 belongs in app-side management, using fixed allowlisted root actions. External IPC cannot supply arbitrary PIDs/commands; recheck PID, start identity, UID, and scope immediately before acting.
- UI-only reconstruction leaves AACP intact. Only explicit selection of Bluetooth restart quiesces control and accepts audio interruption.
- Successful restoration requires new-build identity, API 102, and relevant hook checks. Bound failures; no automatic system-core/framework/phone restart.
- The [full R01 contract](09-readiness.md) does not alter the ordinary rule: control failure recovers only this module's socket.
