# Battery notification specification

Version: 0.1, 2026-10-23. Status: **alpha4 implementation prepared; not tested or hardware-accepted**.

Implementation note: alpha4 introduces the threshold model/UI, per-ear session reducer, initial-data readiness, one presentation arbiter, separate native left/right regions and a native pre-draw visibility acknowledgment. The prior deadline-from-A2DP policy and implicit case/10% reminder publisher are no longer the active notification path. The user requested packaging without tests; only compilation and APK integrity/identity checks are performed. All acceptance cases below remain unexecuted for alpha4.

This is the authoritative battery-notification contract for the next iteration. It supersedes conflicting battery-presentation and reminder assumptions in earlier planning documents. It does not change pairing, audio routing, the dedicated Interconnectivity panel, or firmware behavior.

## 1. Problem and evidence

The user reports that the current connection notification **flashes briefly rather than remaining as a useful five-second island**. This is a failed presentation result, not an accepted implementation.

The current source has several relevant properties:

- [AudioConnectionNotifications](../app/src/main/java/moe/chenxy/hyperpods/utils/AudioConnectionNotifications.kt) currently derives its deadline from the original A2DP connection timestamp.
- [MiBluetoothToastHook](../app/src/main/java/moe/chenxy/hyperpods/hook/MiBluetoothToastHook.kt) combines native focus/island fields, notification timeouts and explicit cancellation.
- [LowBatteryReminder](../app/src/main/java/moe/chenxy/hyperpods/utils/LowBatteryReminder.kt) currently has an ear threshold, a separate case threshold, and an implicit 10% critical level. Its predicate can alert on an initial low reading without a previously observed downward crossing.
- The current presentation concatenates left/right/case text; that does not establish independently colored left/right rendering.

These observations identify implementation work, **not the proven cause of the flash**. Timing, update flags, expiry/cleanup interaction and OEM rendering must be distinguished during a focused joint test. A passing timer unit test or successful notification post does not prove five seconds of visible battery UI.

## 2. Confirmed user requirements

| ID | Requirement |
| --- | --- |
| BN-01 | The inherited HyperPods persistent island and ordinary status notification must not appear. |
| BN-02 | A real headphone connection produces the new battery island for **five seconds**, displaying left and right batteries. |
| BN-03 | Two editable thresholds: **Low**, default **20%**, and **Medium**, default **50%**. Each ear independently uses green above Medium, red below Low, and yellow between them. |
| BN-04 | Independent **Low battery reminder** and **Medium battery reminder** switches. An ear's battery falling to a threshold produces one battery report, not a new report for each subsequent 1% decrease. |
| BN-05 | A connection's initial reading at/below a threshold must not generate a threshold reminder. It establishes a baseline only. |

BN-02 is the connection exception to BN-05: connecting at 15% still produces the single connection island with red 15%, but **not an additional low/medium reminder**.

## 3. Proposed defaults for unspecified details

These are explicit engineering defaults, not additional user-confirmed requirements. A clarification attempt was unavailable; they can be revised before implementation without reopening BN-01 through BN-05.

| Detail | Proposed decision |
| --- | --- |
| Reminder budget | At most once per **ear, threshold category, real audio-connection session**. |
| Charging/rebounds | Do not rearm a consumed or initially suppressed category within that session. |
| First-install switches | Low reminder **on**, Medium reminder **on**; preserve compatible saved user choices during migration. |
| Valid threshold domain | Integers with `1 <= Low < Medium <= 99`, step 1%. This avoids unreachable downward crossings at 100% and meaningless below-zero color regions. |
| Reminder presentation | The same dual-ear island renderer, also five seconds; no separate persistent notification. |
| Split report batching | A fixed **350 ms** collection window starting with the first eligible event, never extended by later packets. |
| Initial data preparation | Wait up to **3 s** for useful current-session readings; this wait is **not** part of visible time. See section 6 for missing-data handling. |
| Other alerts | No implicit third 10% alert and no case-battery alert in this notification feature. A future case feature needs a separate specification. |
| Sound/vibration | No additional sound or vibration; the visual report is sufficient. |
| Scope of preferences | Phone-side app preferences for the supported active headset, not settings written to the AirPods. |

This draft intentionally favors predictable, low-frequency reminders. Repeated reminders after a charge/discharge cycle in the same connection are a possible later alternative, not an accidental consequence of battery noise.

## 4. Settings and display

### 4.1 Settings surface

Place a **Battery notifications** group in the app settings, reachable without an active earbud session:

| Setting | Value / behavior |
| --- | --- |
| Low battery threshold | Integer percentage, initially 20 |
| Medium battery threshold | Integer percentage, initially 50 |
| Low battery reminder | Independent Boolean |
| Medium battery reminder | Independent Boolean |

- Thresholds always control the display colors, even when both reminder switches are off.
- Switches gate threshold reminders only; they do not disable the five-second connection island.
- Show a preview with separate left/right values and colors. Preview values must be labeled examples and must not publish a system notification or change a real device.
- Validate the pair atomically. Reject `Low >= Medium`, non-integers and out-of-range input with a localized error; retain the last valid saved pair. Do not silently swap or clamp values.
- All labels, summaries, validation errors and accessibility descriptions support English and Simplified Chinese.

### 4.2 Exact color boundaries

For a valid ear battery `b`, Low `L`, and Medium `M`:

```text
b < L        -> Red
L <= b <= M  -> Yellow
b > M        -> Green
unknown      -> Neutral, "--"
```

This follows the user's strict wording **below Low / above Medium**. With defaults:

| Battery | Color |
| --- | --- |
| 0%, 19% | Red |
| 20%, 21%, 49%, 50% | Yellow |
| 51%, 100% | Green |
| Missing, invalid, offline | Neutral `--`, never 0% |

A low reminder can therefore fire at **20% while that ear is still yellow**. The crossing predicate and the strict color boundary are different requirements; do not silently change red to `b <= L`.

Left and right are evaluated independently: `L 18% / R 72%` means a red left indicator and green right indicator. Do not color both according to their minimum, average, primary ear or most recent report.

Use semantic color tokens with light/dark contrast checks; retain readable percentage text and explicit left/right labels so color is not the only information channel. Do not bake text into product artwork or pretend a single-color string satisfies independent coloring.

### 4.3 Contents

- Required: left identifier/icon, left percentage and individual color; right identifier/icon, right percentage and individual color.
- Optional secondary content: trusted device alias and a concise reason such as "Connected", "Left ear reached 50%" or "Left and right reached a threshold".
- Case battery is not part of this notification contract. Do not delay both ear readings waiting for a case, and do not reopen a case toast.
- A single available ear still occupies its correct side; the unavailable peer displays neutral `--`.
- Unknown, cached or invalid values cannot be presented as new current measurements. If a displayed value becomes unavailable during the window, make its unknown/retained status explicit rather than substituting zero.
- If a native compact template cannot represent two independently colored ear indicators, use a verified native dual-region/expanded layout. A generic icon-only flash or one whole-string tint fails the design. Exact OEM wire fields must be validated rather than assumed.

## 5. Identity, sessions and fresh observations

### 5.1 Real connection session

One session begins when the system reports a genuine transition into connected A2DP audio for the selected paired AirPods, and ends on a confirmed audio disconnect or explicit disable.

These are **not new sessions**:

- AACP control-socket recovery or repeated handshake;
- opening/closing the app or its settings;
- repeated/late UUID callbacks for the same connection;
- duplicate battery reports, native-view rebinds or opening another UI surface;
- a transient `DISCONNECTING` observation that returns to the same connection without a confirmed disconnect.

Each session has a generation identifier. Each ear has its own first-read baseline and reminder state. The first valid left report must not initialize a missing right ear to zero.

### 5.2 Eligible battery observations

An observation can participate in crossing detection only if it:

1. Belongs to the active paired device and current session.
2. Is a **new report for that specific ear**, not a snapshot rebroadcast whose other component changed.
3. Has a valid integer level in `0..100`, a meaningful charging/availability state and valid ordering.
4. Is not cached, `NEED_AGAIN`, offline, invalid, out of order, or an unassociated BLE estimate.

Use AACP reports for control-session notification decisions. The experimental BLE recorder is not an identity authority and must not drive threshold notifications.

Fresh charging samples update displayed state/baselines but never trigger downward reminders. Charging is not permission to rearm a consumed budget under this draft.

### 5.3 Process or control discontinuity

- App activity recreation alone must not reset anything in the owning host.
- Host state tracks session identity, per-ear last eligible level and per-category eligibility/consumption. Recovered state is useful only with a valid session/boot identity; do not persist every battery packet.
- After a control-data gap, preserve consumed/suppressed budgets but quietly rebaseline the first fresh report rather than manufacturing a missed historical crossing.
- If a host restarts while audio is already connected and session continuity cannot be established, do **not** synthesize a new connection island or threshold alert. Enter a fail-closed resumed session: initialize displayed batteries, suppress reminder categories for the remainder of that audio connection, and wait for a genuine next connection to restore normal eligibility.

Prefer missing one ambiguous reminder to repeatedly notifying from reconstructed history. Diagnostics must state such suppression instead of claiming a successful reminder.

## 6. Five-second presentation contract

### 6.1 Three clocks, not one

Record these independently using a monotonic clock:

```text
audioConnectedAt  ->  dataReadyAt  ->  firstVisibleAt  ->  expiresAt
                                                    expiresAt = firstVisibleAt + 5000 ms
```

- Five seconds means **visible useful presentation time**, not five seconds from A2DP before data, IPC and SystemUI rendering have completed.
- Starting a five-second countdown at `audioConnectedAt` and spending most of it waiting for data is not compliant.
- The adapter must distinguish request accepted, first native content visible, refresh and hidden/cancelled. Use a verified native lifecycle signal where available; notification-post success alone is not visible confirmation.
- Diagnostic clocks may initially use a submission-time approximation, but physical acceptance still requires observed visible timing and must not be labeled satisfied solely by that approximation.
- Use a bounded startup/failure watchdog. Proposed maximum: **10 s after notification submission** to clean an unconfirmed/hung presentation. This is a safety bound, not the target display duration or permission for a persistent island.

### 6.2 Initial data

Proposed preparation policy:

1. Both valid current-session ears available: submit immediately.
2. Only one valid ear available by 3 s after audio connection: submit with that ear and `--` for its peer; do not block single-ear use.
3. No valid ear available by 3 s: keep the app's synchronization status, without a deceptive empty battery flash. Continue the existing bounded control initialization, but wait no longer than 10 s after audio connection for one valid ear.
4. If still no useful data at 10 s, record `initial_battery_unavailable` and skip this connection presentation. Do not disconnect audio, continually retry or substitute an old persistent notice.
5. A confirmed disconnect, explicit disable or superseding session cancels preparation immediately.

When data arrives late but within preparation, it starts its own full visible five-second window. Preparation does not emit threshold alerts from first-read values.

### 6.3 Updates and dismissal

- During an active presentation, render fresh battery values and independent colors into the **same presentation**.
- Preserve its original visible-start time/deadline; do not recreate the window, replay entry animation, set initial-float flags again, hide it prematurely or restart its timer.
- At expiry, remove the island and its owned backing notification. There must be no old HyperPods/P.O.D.S. status notice waiting underneath.
- User dismissal, actual disconnect or enhancement disable ends the window early and prevents immediate reappearance for the same event.
- A new real threshold event may later open a new five-second presentation under section 8. Routine refresh cannot.
- Do not force overlays above calls, lock screens or another system-critical UI. If the system prevents actual display, record the suppressed outcome rather than silently claiming a full five-second success.

### 6.4 The current flash is an open defect

For the next agreed joint test, correlate only:

- audio and data-ready times;
- first post / native first-visible / update / cancellation times;
- presentation ID, update count and cancellation reason;
- native timeout units and the effect of each initial/update/dismiss flag.

No device names, raw addresses or packet payloads are needed for this timing record. Check whether expiry was consumed before rendering, an update changed native mode, or cleanup targeted the active item. These are investigation candidates, not conclusions. Do not blame another installed module without evidence.

## 7. Downward crossing and deduplication

### 7.1 Eligibility states

Maintain state independently for `(connectionSession, ear, category)`, where category is `LOW` or `MEDIUM`:

```text
UNINITIALIZED -> ARMED
              -> SUPPRESSED_INITIAL

ARMED -> PENDING -> REPORTED
                 -> FAILED_OR_SUPPRESSED
      -> SUPPRESSED_DISABLED
```

`REPORTED`, `SUPPRESSED_INITIAL`, `SUPPRESSED_DISABLED`, and `FAILED_OR_SUPPRESSED` are terminal until the next genuine connection under the proposed per-connection policy.

First eligible reading `b0`:

- `b0 > threshold`: arm that category for this ear.
- `b0 <= threshold`: mark it `SUPPRESSED_INITIAL`.
- In both cases, emit **no threshold reminder**.

### 7.2 Trigger predicate

For two consecutive eligible observations from the same ear, `previous` and `current`, threshold `T`:

```text
crossedDown = previous > T AND current <= T AND current < previous

eligibleEvent =
    state == ARMED
    AND crossedDown
    AND current observation is not charging
    AND category switch is enabled
```

- A skipped exact percentage still counts: `52 -> 48` crosses 50.
- `50 -> 49`, `20 -> 19`, repeated `20 -> 20`, or decreases wholly below a threshold do not create a new crossing.
- A reading increase is never a downward event.
- On a crossing while its switch is off, update the baseline and suppress that category for this session; turning the switch on later does not replay it.
- Persist/retain the consumed event identity before it can be replayed by another callback. Record presentation outcome separately: "attempted/suppressed" is not "shown".
- Presentation failure produces a diagnostic outcome and no packet-driven retries for the same crossing. A later 1% decrement cannot become a retry trigger.

This is an **edge detector**, not `current <= threshold` evaluated on every packet.

### 7.3 Independent ears and categories

- If left crosses 50 and right is 70, emit one report showing both current batteries, with left identified as the trigger.
- If right later crosses 50, it has its own unused budget and may emit one report. The first ear must not consume both ears' budgets.
- If one sample crosses both categories (`60 -> 15`), produce **one report**, with the low category as the primary reason, and consume both crossed-category budgets for that ear. Do not show a 50% card followed by a 20% card for the same sample.
- If the two ears cross within the fixed batching interval, merge the reasons into one report, retaining separate per-ear category state.
- A color change without a threshold event does not notify. In particular, low reminder at 20 followed by red at 19 updates an already-visible card but does not create another.

### 7.4 Initial-low examples

With Low 20 / Medium 50:

| First reading | Initial category state | Later behavior |
| --- | --- | --- |
| 80 | Low armed, Medium armed | `51 -> 50` can report Medium; later `21 -> 20` can report Low |
| 45 | Low armed, Medium initially suppressed | `44 -> 43` does nothing; later `21 -> 20` can report Low |
| 20 | Both initially suppressed | `20 -> 19 -> 18` creates no reminder |
| 10 | Both initially suppressed | No threshold reminder in that session |
| Left 45, right absent | Initialize left only | Right's first report of 15 suppresses its categories silently |

Each of these still gets at most the single **connection** presentation when usable data becomes available.

## 8. Presentation coordination

There is one notification arbiter and one renderer, not separate old/new notification systems.

1. Aggregate eligible crossing reasons for up to 350 ms without extending that window.
2. If no presentation is visible, start one five-second threshold-report island containing both available ear levels.
3. If the connection or another threshold island is already visible, merge reasons and update values/colors **in place**, without extending the original expiry or queuing a back-to-back card. Mark the merged events as reported only if the update is actually accepted/visible; otherwise keep an explicit suppressed/failed outcome.
4. An event at or after expiry may start a new threshold presentation only if it is a genuine previously unconsumed crossing. The expiry callback for the old generation must not cancel the new one.
5. Ordinary battery updates after expiry publish nothing.

First-read suppression prevents "Connected at 15%" followed immediately by a second low reminder. Batching prevents left/right packets from creating two nearly identical cards milliseconds apart.

## 9. Settings edits and migration

### 9.1 Runtime edits

- Save the threshold pair atomically; use a settings revision so queued work cannot mix old/new thresholds.
- Display colors reflect the new valid pair immediately in the settings preview and any already-visible presentation; editing does not open a new system card.
- Changing thresholds or toggles is not a battery decline and never generates a threshold reminder.
- Cancel pending reasons evaluated under an obsolete configuration. Rebaseline unconsumed categories against current valid observations or their next fresh report.
- Preserve terminal per-ear/category budgets within the current connection. A settings edit, charge increase or switch off/on cannot rearm a consumed/initially-suppressed category. Explain that a fresh connection starts a new reminder cycle.
- Disabling one switch cancels pending reasons for that category only. A card containing other valid reasons may finish normally; no perpetual status card is created.

### 9.2 Existing HyperPods-derived settings

- Replace the notification feature's old ear/case reminder model with explicit Low/Medium categories.
- Remove the implicit critical-10% trigger from this feature. It must not remain as an undocumented third alert.
- Disable the old case-notification path instead of merely hiding its settings. Case battery may still appear in appropriate app/device-info screens.
- Preserve a valid existing ear threshold as Low and existing ear-enabled choice as Low-enabled. New Medium defaults to 50 and its first-install switch defaults as specified in section 3.
- If migration creates an invalid pair, do not silently change it: preserve the original configuration for recovery, use the documented 20/50 pair with an explicit migration notice, and allow the user to edit. Existing notification bits are not evidence of a new per-session downward crossing and must not be replayed.
- One authoritative preference model must feed app UI, control/notification IPC and the native renderer. Validate data at the IPC boundary; unknown or invalid configuration cannot silently fall back to an unbounded old publisher.

## 10. Implementation responsibilities

| Area | Responsibility |
| --- | --- |
| Audio lifecycle | Produce genuine connection generations; preserve identity through UUID delay and control recovery |
| Battery reducer | Apply fresh per-ear reports, maintain provenance/ordering, distinguish unavailable/charging |
| Color policy | Pure threshold-to-color mapping for each ear; no notification side effects |
| Reminder policy | Pure first-baseline/downward-crossing/budget logic and settings revisions |
| Presentation arbiter | Connection/readiness scheduling, fixed batching, merging and terminal outcomes |
| Xiaomi adapter | One main-process presentation owner, verified dual-ear layout and independent colors, explicit timeout units/flags |
| Cleanup | Cancel only current installation-owned entries, preserve unrelated Xiaomi/HyperPods notifications |
| App settings | EN/zh-CN threshold controls, independent switches, validation, preview and persistence |
| Diagnostics | Bounded timing/state/error reasons; no claims based solely on `notify()` returning |

Reuse the tested model/IPC/ownership components where they fit, but do not reuse the old level-triggered reminder predicate or aggregate single-color text in place of this contract. Avoid creating a second controller or doing Bluetooth I/O on a UI/notification callback.

## 11. Acceptance cases

Unless a row overrides it, use Low 20 / Medium 50, both switches on, valid fresh non-charging observations and an active session.

| ID | Input / action | Required result |
| --- | --- | --- |
| BAT-01 | Real connection; L80/R65 available | One dual-ear connection island, both green, five visible seconds; no persistent remainder |
| BAT-02 | Connection at L15/R45 | One connection island, left red/right yellow; zero threshold reminders |
| BAT-03 | Initial L20/R50 | Both yellow; neither initial threshold produces a reminder |
| BAT-04 | Render 19/20/50/51/0/100/unknown | Red/yellow/yellow/green/red/green/neutral respectively |
| BAT-05 | Swap L18/R72 to L72/R18 | Colors follow each ear, not min/average/primary position |
| BAT-06 | L51 -> 50 -> 49 -> 48; R70 | One Medium event for left, no later 1% events; card shows both ears |
| BAT-07 | L21 -> 20 -> 19 -> 18 | One Low event at20; yellow at20, red at19; no new event for that color change |
| BAT-08 | L52 -> 48 | One Medium event despite skipping50 |
| BAT-09 | L60 -> 15 | One combined report, Low primary reason; both categories consumed |
| BAT-10 | Left crosses50, then right crosses50 outside batching/window | Two independent ear events, at most one per ear/category |
| BAT-11 | Left/right cross within350ms | One merged report; both triggering ears/categories recorded |
| BAT-12 | Initial45 -> 44 -> 21 -> 20 | No Medium reminder; one Low reminder |
| BAT-13 | Initial15 -> 14 -> 10 | No threshold reminder and no hidden critical-10% alert |
| BAT-14 | Left first80; right first15 arrives later | Right starts suppressed, not a synthetic100->15 crossing |
| BAT-15 | Medium off:51->50; turn on at49 | No replay; a switch change is not a crossing |
| BAT-16 | Both switches off, then reconnect | Connection island still appears and colors still apply; no threshold alerts |
| BAT-17 | Valid update changes only right; snapshot also contains old left | Left baseline/budget untouched |
| BAT-18 | Cached, offline, invalid, NEED_AGAIN or reordered report | No crossing, no false zero/red, no new presentation |
| BAT-19 | Charging level decreases; then charging rises above threshold | No reminder from charging and no rearm of spent/suppressed categories |
| BAT-20 | Already reported20; noisy19->22->20 | No second Low report within that connection |
| BAT-21 | Actual disconnect/reconnect at15 | One new connection island; first reading suppresses threshold reminders again |
| BAT-22 | AACP recovery or app reopen without audio disconnect | No new connection island or reset of reminder budget |
| BAT-23 | Host restarts with uncertain session continuity | No synthetic connection/threshold alert; fail-closed resumed session |
| BAT-24 | Threshold edit or toggles changed while already below threshold | No synthetic alert; preserved terminal budgets and consistent settings revision |
| BAT-25 | Invalid `Low >= Medium`, non-integer/out-of-range input | Localized validation; last valid pair remains; no device write |
| BAT-26 | Data ready2s after connection; normal presentation startup | Full five visible seconds, not only the remaining3s from A2DP |
| BAT-27 | One ear valid by3s, peer unavailable | One useful connection island with neutral peer; later valid peer refresh does not restart timer |
| BAT-28 | No valid ear by10s | No fake empty flash/persistent fallback; explicit diagnostic result; audio unaffected |
| BAT-29 | At least100 routine updates during/after visible window | In-window content-only updates; unchanged deadline; zero post-expiry notifications |
| BAT-30 | Real crossing while connection island is visible | Merge content/reason without replay, extension or second queued card |
| BAT-31 | Old expiry callback runs after a new legitimate event | It cannot cancel the new generation |
| BAT-32 | User dismisses / disconnects / disables | Window ends; same event cannot immediately recreate it |
| BAT-33 | Renderer returns success but no useful content appears | Not hardware acceptance; report unconfirmed/failed visibility |
| BAT-34 | Posting/update fails, next packets keep decreasing1% | Explicit failed outcome; no per-packet retries or persistent fallback |
| BAT-35 | Upgrade with old owned entries and unrelated Xiaomi/HyperPods entries | Retire only proven-owned entries; unrelated notices preserved |
| BAT-36 | EN/zh-CN, light/dark, large fonts, single-ear/unknown data | Two readable independent indicators, accessible labels, no clipped percentages |

## 12. Joint-test protocol and evidence

Do not execute all cases on the phone automatically. Split validation:

1. **Local deterministic tests:** color boundaries, first-read suppression, crossings, budgets, batching, ordering, configuration migration and time arithmetic.
2. **First joint round, timing only:** the user reconnects once; observe a useful dual-ear island for five seconds and no remainder. Collect a short video or bounded presentation trace only if it flashes again.
3. **Second joint round, presentation only:** agree on labeled debug preview values such as L18/R72 to verify independent colors. Synthetic data must be isolated from production state and identified as a preview.
4. **Later threshold round:** use deterministic replay locally for51->50 and21->20, then agree with the user on a limited physical verification. Do not wait autonomously for batteries to drain, change unrelated power modules, reset headphones or rewrite battery data.

Proposed measurable visual gate: with the phone unlocked, no competing critical UI and useful battery data, continuous useful display should last **4.8-5.5 seconds** from the first visible battery content, then leave no owned notification after dismissal settles. Five jointly observed connection cycles are enough for the first acceptance round; broaden only if a specific failure justifies it. This tolerance and sample count are engineering proposals, not previously measured results.

Implementation is not complete merely because unit tests or APK assembly pass. Record separately: source/tests, submitted presentation, native visibility/timing, user-observed behavior and unresolved defects.

## 13. Open implementation evidence, not excuses to change the design

- Which native island content regions preserve two independent color treatments without flattening into one string?
- Which actual native lifecycle event reliably marks useful first visibility on this ROM?
- Which current flag/update/cleanup interaction causes the user-reported flash?
- How should per-session reminder state survive a host restart without treating stale history as fresh battery evidence?

Resolve these with focused code investigation and jointly agreed tests. The five user requirements remain fixed; section3 defaults are explicitly provisional until reviewed.
