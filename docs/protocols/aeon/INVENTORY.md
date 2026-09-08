# EUC Planet / NOSFET Aeon implementation inventory

Generated from [capabilities.json](capabilities.json). Edit the ledger, then run `python tools/render_aeon_capabilities.py`; `--check` verifies coverage and generated-file freshness without writing.

Status date: 2026-09-08. Complete coverage of the current known control ledger, not a claim of all firmware capabilities or a completed per-field telemetry audit. Shared LeaperKim and WheelLog references remain candidates unless Aeon applicability is established.

implemented means code/UI exists, not physical validation. partial/open/deferred are unchecked. Deferred items may intentionally remain unavailable. Record exact packet/build/firmware and separate send success, readback match and physical effect.

Coverage: 50 work items, all 28 APK construction sites / 24 command groups, 13 settings readbacks, 9 gap groups, 34 WheelAdapter members, 9 capability flags, 12 WheelSettings slots and 46 WheelData fields.

An expected API entry is an optional contract, not a requirement that every wheel implement it. Null follow-up packets can be correct. Unmapped, unsupported by app policy and physically absent are different states.

## Implemented progress

These parts are already in source. Pending physical tests do not make backend/UI work unimplemented. N/A means no UI is needed, not unfinished UI. Partial items remain in the detailed checklist.

| Work item | Backend | UI | Physical validation / remaining work |
|---|---|---|---|
| [SPEED-LIMIT-READBACK-UI](#speed-limit-readback-ui) | Implemented | Implemented | partial: source mapping confirmed; offline tests cover values, zero, missing/nonfinite fields, freshness, disconnect and Legal/restored pairs. Physical UI verification pending; not included in delivered d01249f3 APK. |
| [LEGAL-RESTORE](#legal-restore) | Implemented | Implemented | partial: offline regression tests cover repeated enable, changed readbacks, partial/missing restoration, late readbacks and per-wheel isolation; actual Aeon command execution remains pending |
| [LIGHT-TOGGLE](#light-toggle) | Implemented | Implemented | verified: owner confirms app toggles OFF/LOW and remains silent even at SND10%; observations AEON-HEADLIGHT-DASHBOARD and AEON-REMOTE-SETTINGS-SOUND. Not proof of SND-aware acknowledgements. |
| [LIGHT-LEVEL](#light-level) | Implemented | Implemented | verified: owner confirms dashboard correctly reads all headlight states changed directly on the wheel, in addition to earlier physical cycles and captures |
| [SND](#snd) | Implemented | Implemented | partial: physical SND readback verified; remote setter pending |
| [DISPLAY-BRT](#display-brt) | Implemented | Implemented | verified: owner reports remote display brightness works, with an acknowledgement beep regardless of tested SND values. Exact values and readback-result UI not reported. |
| [UNITS](#units) | Implemented | Implemented | verified: owner reports remote unit selection changes wheel units and produces an acknowledgement beep. Direction, exact SND value and restoration not specified. |
| [HORN](#horn) | Implemented | Implemented | partial: Aeon horn worked; record current single-frame app retest separately |
| [TIME](#time) | Implemented | N/A | partial: exact capture fixture, DST/fractional offsets and connection lifecycle covered by unit tests; wheel clock execution/readback unverified |
| [LOCK](#lock) | Implemented | Implemented | partial: app policy tested; firmware support unresolved |

## EUC Planet expected surface mapped to Aeon

Confirmed in EUC Planet source means current code behavior only, not firmware verification. Interface coverage is checked against the current source; newly added methods or flags require an explicit entry.

| API / property | Aeon mapping, missing mapping or deliberate absence | Work items |
|---|---|---|
| `buildSettingChange` | Normal WheelPreferenceChange extension for brightness, panel-button sound and wheel units; adapter maps to existing Aeon packets. Other Aeon settings rejected. | [SND](#snd), [DISPLAY-BRT](#display-brt), [UNITS](#units) |
| `familyId` | Shared veteran wire-family identity, not proof all LeaperKim features apply. | [REFERENCE-AUDIT](#reference-audit) |
| `capabilities` | Model-specific flags; inspect the flag table below. | [REFERENCE-AUDIT](#reference-audit) |
| `bleProfile` | Shared HM10 FFE0/FFE1 transport; not a control command. | [REFERENCE-AUDIT](#reference-audit) |
| `notifyConnectingTo` | Name preselection, then model44 telemetry identification. | [REFERENCE-AUDIT](#reference-audit) |
| `pickAdapterByDiscoveredServices` | Composite family routing; not a rider capability. | [REFERENCE-AUDIT](#reference-audit) |
| `initSequence` | Immediate sequence remains empty. Aeon defers official clock sync until a valid model44 frame, then the normal poll loop queues it once per model session. | [TIME](#time) |
| `pollRealtime` | Normally empty for streamed telemetry. On Aeon, consumes one deferred official clock-sync command after valid model44 data; other Veteran models unchanged. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `pollSettings` | Empty write; settings arrive in stream. Not a generic settings-query mapping. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `pollStats` | Default null; no separate Aeon stats query mapped. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `horn` | Aeon single LkAp frame; current app retest pending. | [HORN](#horn) |
| `hornFollowup` | Null deliberately; Aeon does not send the generic Veteran companion. | [HORN](#horn) |
| `setLight` | New policy: fresh SND>0 selects original LkAp/LdAp pair; zero/unknown/stale selects owner-verified silent ASCII path. Prior silent-at10% observation predates this policy. Not volume scaling; integrated policy pending wheel validation. | [LIGHT-TOGGLE](#light-toggle), [LIGHT-LEVEL](#light-level) |
| `setLightFollowup` | Original LdAp companion only when primary light call selected audible paired path; null for silent ASCII. Snapshot preserves the decision if SND changes between calls. | [LIGHT-TOGGLE](#light-toggle) |
| `setMaxSpeed` | Returns null; separate commit methods below carry the commands. Do not count this null alone as no support. | [TILTBACK](#tiltback), [ALARM-SPEED](#alarm-speed) |
| `setMaxSpeedCommit` | Shared LdAp builder; Aeon remote effect not verified. | [TILTBACK](#tiltback) |
| `setAlarmSpeedCommit` | Shared LkAp builder; owner clarifies no audible confirmation when applying the setting. Threshold persistence was uncertain; riding-alarm failure was not established. | [ALARM-SPEED](#alarm-speed) |
| `setVolume` | Unsupported/null. Panel SND is separate and must not be substituted for global/alarm volume. | [GLOBAL-VOLUME](#global-volume), [SND](#snd) |
| `setDRL` | Unsupported/null. Separate physical DRL exists; remote mapping missing. | [DRL](#drl) |
| `setLock` | Disabled/null for Aeon by policy; firmware support unresolved. | [LOCK](#lock) |
| `setLockFollowup` | Null for Aeon; no shared Veteran lock tail sent. | [LOCK](#lock) |
| `resetTripMeter` | Existing ASCII CLEARMETER; official binary alternative not adopted or physically verified. | [TRIP-RESET](#trip-reset) |
| `requestAuthKey` | Null; no equivalent Aeon auth requirement established. | [AUTH](#auth) |
| `verifyAuth` | Null; no equivalent Aeon auth flow mapped. | [AUTH](#auth) |
| `requiresConnectAuth` | Default false; no connect-time auth flow required by current implementation. | [AUTH](#auth) |
| `onRawNotification` | Shared frame reassembly/CRC/parser plus model-specific readbacks; field audit still open. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `onDisconnect` | Clears model readbacks, parser and pending lock tail. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `getDiagnosticCommands` | Legacy family catalogue; entries are not blanket Aeon support. | [REFERENCE-AUDIT](#reference-audit) |
| `diagnosticCatalogs` | Separate NOSFET catalogue with Aeon light on/off and horn. | [REFERENCE-AUDIT](#reference-audit) |
| `familyDisplayName` | Shared family LeaperKim; NOSFET catalogue and model brand handled separately. | [REFERENCE-AUDIT](#reference-audit) |
| `nominalPackVoltage` | Model-table metadata; audit integer representation versus 151.2V class. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `seriesCells` | Expected36S; check metadata and BMS per-pack36-cell cap separately. | [TELEMETRY-AUDIT](#telemetry-audit) |
| `brand` | Model brand override supplies NOSFET, while shared familyId remains veteran. | [REFERENCE-AUDIT](#reference-audit) |
| `inspectMessageTypes` | Aeon-specific Inspector trace selection through model catalogue; no new wheel command. | [REFERENCE-AUDIT](#reference-audit) |

### Declared capability flags

These are current app declarations. In particular, speed/alarm true does not establish working Aeon execution; false does not prove the physical feature is absent.

| Flag | Declared on Aeon | Work item |
|---|---|---|
| `reportsChargeCurrent` | `true` | [TELEMETRY-CURRENT-POWER](#telemetry-current-power) |
| `hasHorn` | `true` | [HORN](#horn) |
| `hasLight` | `true` | [LIGHT-TOGGLE](#light-toggle) |
| `hasLock` | `false` | [LOCK](#lock) |
| `hasMaxSpeed` | `true` | [TILTBACK](#tiltback) |
| `hasAlarmSpeed` | `true` | [ALARM-SPEED](#alarm-speed) |
| `hasVolume` | `false` | [GLOBAL-VOLUME](#global-volume) |
| `hasDRL` | `false` | [DRL](#drl) |
| `needsAuthForLock` | `false` | [AUTH](#auth) |

### Generic WheelSettings slots

These generic fields include other-family settings. Defaults are not Aeon observations, and similarly named values must not be equated without evidence.

| Expected slot | Aeon mapping or explicit gap |
|---|---|
| `maxSpeedKmh` | TILTBACK: generic settings slot; Aeon uses reported WheelData threshold/commit path. Do not treat default50 as wheel readback. |
| `alarmSpeedKmh` | ALARM-SPEED: generic slot; exact Aeon reported setting/effect unresolved. Default40 is not evidence. |
| `pedalAdjustment` | PEDAL-ANGLE: semantic candidate only; generic slot not established as Aeon readback. |
| `offroadMode` | UNMAPPED on Aeon. Do not equate with hardness or legacy ride modes. |
| `fancierMode` | UNMAPPED on Aeon. Generic feature has no established Aeon equivalent. |
| `comfortSensitivity` | UNMAPPED on Aeon. Do not substitute continuous hardness without evidence. |
| `classicSensitivity` | UNMAPPED on Aeon. No established counterpart. |
| `standbyDelayMinutes` | UNMAPPED on Aeon. Observed automatic shutdown does not establish an editable standby-delay command. |
| `mute` | GLOBAL-VOLUME / SND: generic mute is not panel keypress volume or a proven alarm mute. |
| `drl` | DRL: physical feature observed, remote/readback mapping missing. |
| `transportMode` | TRANSPORT: separate typed Aeon raw readback exists; generic Boolean slot does not authorize a write or prove equivalent protocol semantics. |
| `lockState` | LOCK: security lock intentionally unavailable; generic default0 does not prove unlocked state. |

## Backend / UI / validation checklist

Priority 1: validate existing low-risk support. Priority 2: passive features and bounded additions. Priority 3: ambiguous or riding-affecting behavior. Priority 4: deferred maintenance/security/safety scope. Priority is not authorization to send commands.

### SPEED-LIMIT-READBACK-UI

**Read-only wheel-reported tiltback and alarm thresholds** (priority 1)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [x] Backend: implemented: passive projection of WheelData.wheelMaxSpeedKmh and wheelAlarmSpeedKmh, independent of stored normal/Legal presets; no new BLE command
- [x] Ui: implemented: read-only row in Settings > Speed > Speed limits; selected speed units, independent missing values, disconnected/stale values unavailable and new sample required after opening/reconnecting. Hidden when neither threshold is reported.
- [ ] Validation: partial: source mapping confirmed; offline tests cover values, zero, missing/nonfinite fields, freshness, disconnect and Legal/restored pairs. Physical UI verification pending; not included in delivered d01249f3 APK.
- [ ] Next: On the next requested APK, compare both read-only values with panel settings before/on/off Legal mode. A reported threshold is not an execution ACK or proof of riding enforcement.

### LEGAL-RESTORE

**EUC Planet Legal mode: temporary previous speed limits** (priority 1)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [x] Backend: implemented: in-memory per-wheel snapshot of configured normal tiltback/alarm before enabling; restore snapshot on disable; protect it and normal settings from temporary readback adoption
- [x] Ui: implemented: existing Legal toggle uses snapshot for restoration; no new buttons or confirmation UI
- [ ] Validation: partial: offline regression tests cover repeated enable, changed readbacks, partial/missing restoration, late readbacks and per-wheel isolation; actual Aeon command execution remains pending
- [ ] Next: Stationary validation of both thresholds before/on/off; no riding threshold test required. Transport outcome and optimistic UI behavior remain separate open work, not fixed by the snapshot.

### GLOBAL-VOLUME

**Generic wheel/alarm volume versus panel SND** (priority 1)

EUC Planet API: `setVolume`

- [ ] Backend: deferred: setVolume returns null; do not route generic volume to panel SND
- [ ] Ui: deferred: generic wheel-volume support unavailable; separate SND settings editor exists
- [ ] Validation: partial: owner reports speed/fall-related warnings remain loud at SND0; global volume mapping unknown
- [ ] Next: Preserve independent warning behavior; LIGHT-SND-01 tests only remote light acknowledgement

### LIGHT-TOGGLE

**Headlight on/off** (priority 1)

EUC Planet API: `setLight`, `setLightFollowup`

Official command evidence: headlight. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [x] Backend: implemented: AeonAcknowledgementPolicy chooses original paired light command for fresh SND>0, ASCII for SND0/unknown/stale; selection captured across primary/followup calls
- [x] Ui: implemented: existing dashboard tile
- [x] Validation: verified: owner confirms app toggles OFF/LOW and remains silent even at SND10%; observations AEON-HEADLIGHT-DASHBOARD and AEON-REMOTE-SETTINGS-SOUND. Not proof of SND-aware acknowledgements.
- [ ] Next: On next requested APK validate new SND-gated selection at0 and10%, including resulting brightness and beep count. Paired-path light mode may differ from ASCII LOW. Do not claim loudness scaling or silent brightness/unit variants.

### LIGHT-LEVEL

**Headlight level readback and dashboard state** (priority 1)

EUC Planet API: `setLight`

Explicit unmapped/gap group: Headlight levels.

- [x] Backend: implemented: page1 byte49 decoder and freshness-aware HeadlightReadback projection, commit3d67c95f
- [x] Ui: implemented: existing dashboard Off/Low/Medium/High label and theme highlight, commit3d67c95f
- [x] Validation: verified: owner confirms dashboard correctly reads all headlight states changed directly on the wheel, in addition to earlier physical cycles and captures
- [ ] Next: Validate disconnect/reconnect and stale-state handling separately; basic all-level dashboard readback is verified. Remote multilevel writing remains LIGHT-LEVEL-WRITE.

### SND

**Panel keypress sound (SND)** (priority 1)

EUC Planet API: `buildSettingChange`, `setVolume`

Official command evidence: key-tone volume. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: KeyTone ([offsets and evidence](README.md#settings-page-8)).

- [x] Backend: implemented: normal WheelPreferenceChange BUTTON_SOUND maps to APK KEY_TONE; existing range/support checks and BLE dispatch, no special confirmation/wait gate
- [x] Ui: implemented: standard numeric Wheel button sounds entry in Speech settings; no vendor card and no global warning-volume substitution
- [ ] Validation: partial: physical SND readback verified; remote setter pending
- [ ] Next: Validate remote SND setter separately; LIGHT-SND-01 owner result is silence at10%, not demonstrated volume tracking.

### DISPLAY-BRT

**Display brightness percentage** (priority 1)

EUC Planet API: `buildSettingChange`

Official command evidence: display brightness. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: ScreenBacklightRate ([offsets and evidence](README.md#settings-page-8)).

- [x] Backend: implemented: normal WheelPreferenceChange DISPLAY_BRIGHTNESS maps to existing APK packet, range0..100 and supported model check
- [x] Ui: implemented: standard numeric Wheel display brightness entry in Display settings; immediate edit dispatch, no separate Apply/confirmation
- [x] Validation: verified: owner reports remote display brightness works, with an acknowledgement beep regardless of tested SND values. Exact values and readback-result UI not reported.
- [ ] Next: Confirm restoration and record tested brightness/SND values; silent variant remains unmapped. No need to re-prove basic brightness operation.

### UNITS

**Wheel display units** (priority 1)

EUC Planet API: `buildSettingChange`

Official command evidence: unit selection. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: Unit ([offsets and evidence](README.md#settings-page-8)).

- [x] Backend: implemented: normal WheelPreferenceChange DISPLAY_UNITS maps to existing APK packet, separate from app units
- [x] Ui: implemented: standard Wheel display units dropdown beside app units in Display settings; immediate selection dispatch
- [x] Validation: verified: owner reports remote unit selection changes wheel units and produces an acknowledgement beep. Direction, exact SND value and restoration not specified.
- [ ] Next: Confirm original units restored; retain acknowledgement-beep observation without inferring both directions or all SND values were tested.

### HORN

**Horn** (priority 1)

EUC Planet API: `horn`, `hornFollowup`

Official command evidence: horn. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [x] Backend: implemented: Aeon single LkAp, no companion
- [x] Ui: implemented: dashboard action
- [ ] Validation: partial: Aeon horn worked; record current single-frame app retest separately
- [ ] Next: Retest current build once; record packet and audible result

### TELEMETRY-CURRENT-POWER

**Separate phase current from estimated battery current and power** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [ ] Backend: open: source discrepancy established; parser assigns phase current to generic current and calculates both power fields without PWM
- [ ] Ui: partial: existing metrics consume these fields; consumer semantics and any existing compensation need audit
- [ ] Validation: partial: official APK and pinned WheelLog provide distinct phase/PWM-derived-current implementations; no new live verification
- [ ] Next: Audit downstream compensation and sign assumptions, including upstream0.19.0 reportsChargeCurrent=true inherited default on Aeon (not physical proof of charger current). Then implement bounded model-policy correction with current/PWM/power, energy and other-model regression tests. Do not apply PWM twice or claim mechanical motor power.

### TELEMETRY-ROLL

**Expose official roll-angle readback with freshness** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [ ] Backend: open: APK page0/4 offset67 signed16 /100 lrAngle is not projected into WheelData.rollAngle
- [ ] Ui: open: existing roll consumers and unknown/stale-state behavior need review before exposure
- [ ] Validation: partial: exact APK decoder found; physical sign, stale-page behavior and firmware applicability not tested
- [ ] Next: Add a model-scoped passive projection with page, length, sign, age, disconnect and model-isolation tests; do not treat intervening-page defaults as observed zero.

### LIGHT-LEVEL-WRITE

**Remote multilevel headlight selection or cycling** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Headlight levels.

- [ ] Backend: open: no supported remote multilevel setter found; owner confirms current app toggles OFF/LOW only
- [ ] Ui: open: do not offer a level selector or cycle gesture until a command is established
- [ ] Validation: partial: owner verifies OFF/LOW app toggle and all-level panel readback; remote medium/high selection remains unmapped, not proven impossible
- [ ] Next: Identify exact command and Aeon applicability; keep completed readback work in LIGHT-LEVEL separate

### PEDAL-HARDNESS

**Continuous pedal hardness percentage** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: pedal hardness. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: PedalHardness ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: readback; matching internal shared builder needs typed routing
- [ ] Ui: partial: read-only setting; no editor
- [ ] Validation: open: readback80 observed; remote execution untested
- [ ] Next: Use APK continuous hardness semantics, not legacy modes; stationary validation plan

### TIME

**Clock synchronization** (priority 2)

EUC Planet API: `initSequence`

Official command evidence: time synchronization. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [x] Backend: implemented: APK clock builder plus one-shot deferred startup after valid model44 data; standard raw timezone offset preserved
- Ui: not applicable: automatic connection initialization, no new dashboard or settings control
- [ ] Validation: partial: exact capture fixture, DST/fractional offsets and connection lifecycle covered by unit tests; wheel clock execution/readback unverified
- [ ] Next: On requested future build, capture one sync after connection and none during steady telemetry; verify wheel clock if observable

### TRIP-RESET

**Wheel trip reset** (priority 2)

EUC Planet API: `resetTripMeter`

Official command evidence: trip reset. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: partial: existing CLEARMETER; official binary alternative documented
- [ ] Ui: partial: existing generic action; review wheel-vs-app reset confirmation
- [ ] Validation: open: Aeon execution not verified
- [ ] Next: Compare vendor binary path; require consent for lost trip mileage

### READ-LOG

**Read wheel diagnostic logs** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: read log. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: open: request known, response parser and lifecycle incomplete
- [ ] Ui: open: proposed local diagnostics action
- [ ] Validation: open: wheel response and stream impact untested
- [ ] Next: Implement bounded response handling and cancellation; no automatic upload

### DRL

**Separate daytime light** (priority 2)

EUC Planet API: `setDRL`

Explicit unmapped/gap group: DRL.

- [ ] Backend: open: no confident readback or setter
- [ ] Ui: open: no toggle
- [ ] Validation: partial: physical on/off observed
- [ ] Next: Correlate captures and search source; no guessed writes

### REAR-ACTIVE

**Rear-light active mode cycle** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Rear-light active mode / LTB startup mode.

- [ ] Backend: open: no confirmed readback or setter
- [ ] Ui: open: planned Lighting menu, not new dashboard buttons
- [ ] Validation: partial: physical cycle observed; motion-dependent meaning unresolved
- [ ] Next: Keep solidred, blueflash, blinkingred, RGB and off distinct

### REAR-STARTUP

**Rear-light startup default (LTB)** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Rear-light active mode / LTB startup mode.

- [ ] Backend: open: mapping unknown
- [ ] Ui: open: proposed Lighting setting
- [ ] Validation: partial: manual/panel evidence only
- [ ] Next: Keep startup preference separate from active rear mode

### AUTO-LIGHT

**Automatic headlight BRT on/off** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Auto headlight BRT on/off.

- [ ] Backend: open: no identified setter or readback
- [ ] Ui: open: proposed Lighting setting
- [ ] Validation: partial: manual documented
- [ ] Next: Do not substitute display BRT percentage command

### DISPLAY-PAGE

**Physical lower-display page selection** (priority 2)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [ ] Backend: open: no remote mapping established
- [ ] Ui: deferred: no need to duplicate physical page selector without command
- [ ] Validation: partial: mileage, temperature, voltage, current and power pages observed
- [ ] Next: Keep display-page selection separate from telemetry measurements

### TELEMETRY-AUDIT

**Common telemetry and BMS compatibility audit** (priority 2)

EUC Planet API: `pollRealtime`, `pollSettings`, `pollStats`, `onRawNotification`, `onDisconnect`, `nominalPackVoltage`, `seriesCells`

- [ ] Backend: partial: shared Veteran parser and existing Aeon adjustments
- [ ] Ui: partial: existing dashboard and battery screens
- [ ] Validation: partial: offline source reconciliation recorded in telemetry_reconciliation; all WheelData slots have explicit dispositions. Not every field or firmware is physically verified.
- [ ] Next: Audit current/power downstream consumers and implement a bounded correction with regression tests; separately add fresh roll-angle readback. Resolve official battery lookup, version high byte, charge-mode enum, BMS sentinels and short-frame handling before declaring full parity.

### REFERENCE-AUDIT

**Shared LeaperKim / WheelLog source reconciliation** (priority 2)

EUC Planet API: `familyId`, `capabilities`, `bleProfile`, `notifyConnectingTo`, `pickAdapterByDiscoveredServices`, `getDiagnosticCommands`, `diagnosticCatalogs`, `familyDisplayName`, `brand`, `inspectMessageTypes`

- [ ] Backend: partial: pinned WheelLog dcf56672 and EUC a1e83af1 against NOSFET1.1.3 decoder in telemetry_reconciliation; control-wide comparison not complete
- Ui: not applicable: research work item
- [ ] Validation: open: no blanket inheritance of other-wheel support
- [ ] Next: For each source difference add a row or explicit exclusion with provenance

### PANEL-STA

**Panel STA setting** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [ ] Backend: open: exact command/readback meaning not reconciled
- [ ] Ui: deferred: no editor until meaning is established
- [ ] Validation: partial: owner observed STA40.0 in advanced panel inventory
- [ ] Next: Resolve manual meaning and source mapping; do not equate with TLT40 or APK StopSpeed just because the numbers match

### PWM

**PWM tiltback threshold** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: PWM threshold. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: StopPowerRate ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: readback and internal shared builder
- [ ] Ui: partial: read-only setting
- [ ] Validation: open: remote execution untested
- [ ] Next: Compare exact APK encoding and bounds; safety review before enabling

### TILTBACK

**Speed tiltback threshold** (priority 3)

EUC Planet API: `setMaxSpeed`, `setMaxSpeedCommit`

Official command evidence: tilt-back speed. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: StopSpeed ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: existing shared commit path and readback; Aeon semantics not validated
- [ ] Ui: partial: existing generic speed control, not validated Aeon support
- [ ] Validation: open: current remote path unverified
- [ ] Next: Compare setter, units, range, readback and restore behavior

### ALARM-SPEED

**Speed alarm threshold** (priority 3)

EUC Planet API: `setMaxSpeed`, `setAlarmSpeedCommit`

Official command evidence: alarm speed. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: partial: existing generic commit path; effectiveness unresolved
- [ ] Ui: partial: existing generic control
- [ ] Validation: partial: owner clarifies the missing sound was the audible acknowledgement when applying the speed-warning setting, not a tested absence of warnings at speed. Setting seemed not to stick, but threshold persistence and BLE/application acknowledgement remain unverified.
- [ ] Next: Diagnose exact command and response; do not mark as fixed

### PEDAL-ANGLE

**Fore/aft pedal angle** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: vertical angle. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: partial: shared internal builder; APK range narrower
- [ ] Ui: open: no dedicated Aeon editor
- [ ] Validation: open: owner reported setting beep; exact current path needs attribution
- [ ] Next: Constrain to APK range; distinguish ANG percent and ANG TLT

### RIDE-CONTINUOUS

**Legacy continuous ride mode** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: continuous ride mode. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: open: feature-gated legacy path, not the pedal hardness builder
- [ ] Ui: deferred: applicability unresolved
- [ ] Validation: open: shared APK presence only
- [ ] Next: Trace continuousSoftHardSet gating and Aeon precedence

### RIDE-DISCRETE

**Legacy soft/medium/hard mode** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: discrete ride mode. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: partial: shared ASCII diagnostics; APK binary alternatives documented
- [ ] Ui: deferred: do not duplicate continuous hardness UI
- [ ] Validation: open: Aeon applicability unverified
- [ ] Next: Determine whether Aeon exposes legacy mode separately

### ASSIST

**Acceleration assistance ANG percent** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Acceleration assistance ANG percent / recenter ANG TLT.

- [ ] Backend: open: no established mapping
- [ ] Ui: open: proposed Ride setting
- [ ] Validation: partial: panel/manual evidence
- [ ] Next: Separate from pedal angle and hardness

### RECENTER

**Recenter ANG TLT** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Acceleration assistance ANG percent / recenter ANG TLT.

- [ ] Backend: open: no established mapping
- [ ] Ui: open: proposed Ride setting
- [ ] Validation: partial: panel/manual evidence
- [ ] Next: Determine exact units and behavior before editor

### TORQUE-ALARM

**Torque ALM on/off** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Torque ALM on/off and ?br / ?bL / ?Adv.

- [ ] Backend: open: no established matching command
- [ ] Ui: open: no editor
- [ ] Validation: partial: panel/manual evidence
- [ ] Next: Do not substitute speed alarm or brake-pressure command

### PANEL-BR

**Unresolved panel ?br** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Torque ALM on/off and ?br / ?bL / ?Adv.

- [ ] Backend: open: mapping unknown
- [ ] Ui: deferred: preserve raw label
- [ ] Validation: open: panel label observed, meaning unresolved
- [ ] Next: Resolve manual label and applicability

### PANEL-BL

**Unresolved panel ?bL** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Torque ALM on/off and ?br / ?bL / ?Adv.

- [ ] Backend: open: mapping unknown
- [ ] Ui: deferred: preserve raw label
- [ ] Validation: open: panel label observed, meaning unresolved
- [ ] Next: Resolve manual label and applicability

### PANEL-ADV

**Unresolved panel ?Adv** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Torque ALM on/off and ?br / ?bL / ?Adv.

- [ ] Backend: open: mapping unknown
- [ ] Ui: deferred: preserve raw label
- [ ] Validation: open: panel label observed, meaning unresolved
- [ ] Next: Resolve manual label and applicability

### PARKING

**Physical parking / lift behavior** (priority 3)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [ ] Backend: open: remote mapping not established
- [ ] Ui: open: do not reuse security-lock button
- [ ] Validation: partial: owner described physical button behavior
- [ ] Next: Record independent semantics before considering remote control

### SILENT-VISUAL-ACK

**Future optional visual acknowledgement for silent commands** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

- [ ] Backend: deferred: idea only; no brake-light blink command or alternative indicator implemented
- [ ] Ui: deferred: no new option in this scope
- [ ] Validation: open: preserve original lamp state and avoid confusing brake/safety indications; exact command and physical behavior unknown
- [ ] Next: Future investigation only: consider a brief rear-light indication or another acknowledgement when sound is disabled. Establish supported commands, restoration, timing and rider meaning first.

### TRANSPORT

**Transport mode** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: transport mode. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: TransportMode ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: readback only; exact APK setter documented
- [ ] Ui: deferred: read-only; no write toggle
- [ ] Validation: open: remote effect/recovery unverified
- [ ] Next: Document entry and recovery; distinguish parking and security lock

### SHUTDOWN

**Delayed wheel shutdown** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: shutdown after 10 seconds. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: open: exact APK action documented
- [ ] Ui: deferred: no user-facing action
- [ ] Validation: open: not wheel-tested
- [ ] Next: Keep disabled unless separately requested and safety reviewed

### GYRO

**Gyroscope / level calibration** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: gyroscope calibration. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: Gyro ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: readback only; APK action documented
- [ ] Ui: deferred: read-only; no calibration action
- [ ] Validation: open: not wheel-tested
- [ ] Next: Maintenance-only, no exploratory execution

### CHARGE-VOLTAGE

**Maximum charge voltage** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: maximum charge voltage. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: MaxChargeVol ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: raw readback with APK interpretation
- [ ] Ui: deferred: read-only; no setter
- [ ] Validation: open: raw62 captured; physical semantics not validated
- [ ] Next: APK bounds are not safe pack limits; maintenance review

### VOLTAGE-ADJUST

**Voltage adjustment** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: voltage adjustment. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: Vol ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: signed raw readback; APK setter documented
- [ ] Ui: deferred: read-only; no setter
- [ ] Validation: open: remote write untested
- [ ] Next: Do not confuse Vol with sound volume; calibration scope

### FALL-ANGLE

**Side-fall motor cutoff angle** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: side-tilt/fall angle. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [ ] Backend: open: exact APK setter; page2 readback needs mapping
- [ ] Ui: deferred: no editor
- [ ] Validation: open: motor-cutoff semantics not tested
- [ ] Next: No physical fall/cutoff tests in ordinary validation

### HIGH-SPEED

**APK high-speed mode** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: high-speed mode. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: HighSpeedMode ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: sentinel-preserving readback
- [ ] Ui: deferred: unsupported readback, no toggle
- [ ] Validation: open: raw0x80 observed, Aeon support unresolved
- [ ] Next: Check feature gates; never treat sentinel as false

### LOW-BATTERY

**APK low-battery mode** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: low-battery mode. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: LowVolMode ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: sentinel-preserving readback
- [ ] Ui: deferred: unsupported readback, no toggle
- [ ] Validation: open: raw0x80 observed, Aeon support unresolved
- [ ] Next: Check feature gates before any implementation

### BRAKE-PRESSURE

**APK brake-pressure alarm** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Official command evidence: brake-pressure alarm. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: BrakePressureAlarm ([offsets and evidence](README.md#settings-page-8)).

- [ ] Backend: partial: retains raw145 outside APK90..125 range
- [ ] Ui: deferred: no writable interpretation
- [ ] Validation: open: encoding or semantics discrepancy
- [ ] Next: Resolve discrepancy; do not equate with ALM on/off

### LOCK

**Security lock/unlock** (priority 4)

EUC Planet API: `setLock`, `setLockFollowup`

Explicit unmapped/gap group: Security lock/unlock.

- [x] Backend: implemented: Aeon capability false and null command
- [x] Ui: implemented: disabled dashboard button after identification
- [ ] Validation: partial: app policy tested; firmware support unresolved
- [ ] Next: Retain conservative policy; do not equate with parking

### AUTH

**Wheel authentication** (priority 4)

EUC Planet API: `requestAuthKey`, `verifyAuth`, `requiresConnectAuth`

Explicit unmapped/gap group: Authentication.

- [ ] Backend: deferred: no Aeon auth path established
- [ ] Ui: deferred: no new prompt
- [ ] Validation: open: no evidence generic flow is required
- [ ] Next: Only revisit if an actual auth requirement is found

### FIRMWARE

**Firmware upgrade / bootloader** (priority 4)

EUC Planet API: No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.

Explicit unmapped/gap group: Firmware update / bootloader.

- [ ] Backend: deferred: excluded from control support
- [ ] Ui: deferred: no action
- [ ] Validation: open: maintenance paths only inventoried
- [ ] Next: No probing, flashing or arbitrary payload execution

## Offline telemetry reconciliation

Reviewed: 2026-09-08. Offline source comparison, not new physical verification. Absolute zero-based offsets from DC5A5C; BE means big-endian. Shared decoder code alone does not establish every Aeon firmware's behavior. Review does not change application behavior.

Evidence: EUC and APK columns are Confirmed in EUC Planet source and Confirmed in NOSFET APK respectively. WheelLog is Confirmed in WheelLog source at the pinned revision, not a claim about its current head. Findings distinguish source differences from unverified physical behavior.

### Source anchors

- euc: EUC Planet fork baseline a1e83af1: ble/VeteranParser.kt parseTelemetry/parseLongFrame, ble/VeteranAdapter.kt decode, ble/VeteranModel.kt; paths relative to app/src/main/java/com/eried/eucplanet/.
- apk: NOSFET 1.1.3, SHA256 f4881479f3c40e2f1d54a8909223af79f5cb18eaf96d9a94f4116ce10e4a5099. com.laoniao.leaperkim.utils.BtManager.handleFullSingleData and parseVersionCode. Fresh JADX export: architecture_as_is/20260907_145845/exports/BtManager-jadx.java in research workspace, not committed. Util.volToBattery checked in earlier CFR export; hardware lookup contents remain open.
- wheellog: [wheellog](https://github.com/Wheellog/Wheellog.Android/commit/dcf56672)
- wheellog_adapter: [wheellog_adapter](https://github.com/Wheellog/Wheellog.Android/blob/dcf56672/app/src/main/java/com/cooper/wheellog/utils/VeteranAdapter.java)
- wheellog_data: [wheellog_data](https://github.com/Wheellog/Wheellog.Android/blob/dcf56672/app/src/main/java/com/cooper/wheellog/WheelData.java)

### Mapping and gaps

| Field group / wire | EUC Planet | Official APK | WheelLog reference | Finding / next action |
|---|---|---|---|---|
| VOLTAGE: 4..5 u16 BE /100 V | Retains hundredths of a volt. | Same scale; rounds to one decimal (JADX294). | Same scale. | Aligned encoding; presentation precision differs. Next: No encoding change indicated. |
| SPEED: 6..7 signed16 BE /10 km/h | applySignMode, default signed. | Absolute magnitude after signed conversion (296..305). | Configurable sign. | Aligned scale; sign policy differs. APK uses >32768 rather than >= for its sign conversion, a boundary quirk not a recommended implementation. Next: Preserve sign policy deliberately; validate direction separately. |
| DISTANCE: Trip low16 at8, high16 at10; total low16 at12, high16 at14; unsigned meters | Combines word-swapped32 and converts to km. | Same word order; separate trip and total (306..307). | Same word order. | Aligned. App-local trip meter is a different quantity. Next: Retain distinction in UI and reset behavior. |
| CURRENT-POWER: Phase current16..17 signed16 /10 A; raw PWM34..35 u16 /10000 fraction | Phase current assigned to current; phaseCurrent remains0. batteryPower and motorPower both voltage*phase current. | mechineCurrent=absolute phase; current=round(mechineCurrent*rawPWM/10000,1); power uses that current and voltage (308..312,333..340). | Dedicated phaseCurrent; PWM-derived current; voltage-derived power (WheelData803..805,783..785). | Confirmed source discrepancy. Battery-current estimate is not direct pack-current measurement, and voltage*phase current is not established mechanical motor power. APK absolute-value policy loses sign. Next: Audit repository, energy integration, charging detection, recording and UI consumers for existing PWM compensation before a bounded fix. Preserve intentional sign semantics and avoid applying PWM twice. |
| CONTROLLER-TEMP: 18..19 signed16 BE /100 C | One temperature plus maxTemperature equal to it. | Same scale, one-decimal rounding (313..317). | Same scale. | Aligned controller measurement, not a maximum over BMS temperatures. Next: No encoding change indicated. |
| SHUTDOWN-TIMER: 20..21 u16 BE | Not surfaced by inspected parser. | setShutDownTime (318). | Sleep timer, seconds. | Missing projection; exact timer behavior and Aeon readback validation open. Not proof of an editable standby setting. Next: Trace official UI units and record passive countdown before offering UI. |
| CHARGE-MODE: APK byte23; WheelLog u16 at22 | byte23 >0 mapped to Boolean charging. | Preserves byte23 as chargeMode (319). | Preserves u16 chargeMode. | EUC loses raw mode distinctions. Byte22 and nonzero enum meanings unresolved; stale WheelData comment contradicts parser behavior. Next: Trace APK mode consumers; retain raw enum if meaningful. Do not assume all nonzero values mean active charging. |
| SPEED-THRESHOLDS: Alarm24..25 and tiltback26..27 u16 BE /10 km/h | wheelAlarmSpeedKmh and wheelMaxSpeedKmh. | dangerSpeed and stopSpeed (320..321). | Same offsets and scale. | Source-confirmed readback mapping, independent of command success, audible feedback or actual riding enforcement. Page8 StopSpeed is a separate encoding. Next: Test legal-mode temporary snapshot using stationary before/on/off readbacks when wheel is available. |
| IDENTITY: Base version28..29; APK additionally uses byte30 as high byte | u16 /1000 identifies model44; full three-byte interpretation absent from inspected parser. | Builds integer from bytes30,28,29, formats six digits into hardware/software sections (322,726..752). | Model44 and36S explicitly added. | Aeon identity already supported; full version interpretation not equivalent if byte30 is nonzero. No claim observed firmware44250 needs a change. Next: Trace identity event formatting and fixtures before changing version handling. |
| LEGACY-RIDE-MODE: APK byte31; WheelLog u16 at30 | Not surfaced by inspected telemetry parser. | setRideMode from byte31 (325). | Pedals mode u16. | Legacy mode must not be equated with page8 continuous hardness. Byte30 also participates in official version encoding. Next: Trace official mode consumers and model branching before adding a generic value. |
| PITCH-PWM: Pitch32..33 signed16; PWM34..35 u16 | pitchAngle=raw/100 degrees; pwm=raw/100 percent, enabled for model44. | Stores signed carPose and raw outPut (326..332); applies output/10000 for derived current. | Pitch/100; hardware PWM/100 percent. | PWM scale aligned. Official carPose presentation scaling remains untraced; physical pitch sign not established. Next: Retain scale evidence without claiming physical orientation proof. |
| BATTERY-ESTIMATE: Derived from voltage, not a confirmed Aeon SoC byte | Model44 linear curve clamp(round((centivolts-11902)/29.03),0,100). | Util.volToBattery selects hardware-specific CarBaseInfo table, with fallback. Table values not reconciled. | Same simple curve; optional alternative curve. | Already on36S curve; parity with official battery percentage is NOT confirmed. Next: Extract matching hardware table and compare offline values. Do not apply Oryx page2 SoC mapping to Aeon. |
| BATTERY-TEMP-MODE: 36..37 u16 BE when available | Not surfaced by inspected parser. | batteryTempMode (341 onwards). | Not reconciled. | Named raw field only; semantics and supported Aeon values unresolved. Next: Trace consumers before assigning units or offering UI. |
| ROLL-PACK-CURRENT: Page0/4: roll67..68 signed16 /100; pack currents69..70 and71..72 signed16 /100 A | Pack currents decoded signed; roll omitted. Pack-current comment incorrectly says0.1 A although code divides100. | lrAngle/100; both pack currents absolute/100 (348..379). | Signed pack currents; no roll projection here. | Roll readback gap and pack-current sign-policy difference. Pack currents are distinct from phase/PWM estimate. Next: Add model-scoped roll readback with age/disconnect handling and tests; preserve signed pack data until conventions verified. |
| BMS-CELLS: Pages1/5 cells1..15 at53;2/6 cells16..30 at53;3/7 cells31..36 at59; u16 /1000 V | First and final blocks use signed16; middle uses unsigned16. Final parser requires12 slots but adapter caps output to36 cells. | Unsigned cell values; final block reads six cells (401..467). | 36S; shared final block reads up to12 slots. | Normal cell-voltage encoding aligned; Aeon36S display cap already implemented. Sentinel and short-final-page acceptance differences remain. Next: Test invalid/sentinel values and frame-length/CRC boundaries; do not display extra six slots as Aeon cells. |
| BMS-TEMPS-FALL: Pages3/7 six signed16 temperatures at47+2*i /100 C; page2 byte47 fallProtectionAngle | Six BMS temperatures decoded; fallProtectionAngle not surfaced by inspected parser. | Six temperatures with rounding; stores page2 fallProtectionAngle (415..467). | Six BMS temperatures. | Temperature offsets aligned. Fall-protection readback is a separate missing field; no write authorized by this review. Next: Add read-only fall-angle mapping only after raw/sentinel/model checks. |
| LIGHT-SETTINGS: Captured page1 byte49 headlight level; page8 settings offsets in existing ledger | Model-specific headlight projection and13 raw settings fields already implemented. | Page8 named settings decoded; see existing command/readback ledger. | Page8 TODO at pinned revision. | Our existing Aeon work exceeds that early WheelLog revision here. Light levels are capture-derived; source presence is not remote multilevel-command proof. Next: Retain existing evidence distinctions and complete pending low-risk write tests. |

## Full expected WheelData field list

Automatically enumerated with an explicit disposition for every field; new fields fail the generator until classified. UNMAPPED means missing evidence or projection, not measured zero, physical absence or support. Source comparisons do not promote shared parser assumptions into Aeon physical facts.

| Field | Current inventory disposition |
|---|---|
| `speed` | Source-aligned offset6 signed16 /10 km/h. EUC preserves configured direction; official app takes absolute magnitude. Physical direction convention remains unverified. |
| `voltage` | Source-aligned offset4 unsigned16 /100 V. Official app rounds to one decimal; EUC retains hundredths. |
| `current` | DISCREPANCY: EUC puts offset16 phase current here. Official app derives current from phase-current magnitude multiplied by raw output /10000. Audit consumers before changing semantics. |
| `batteryPercent` | Estimated, not measured SoC: EUC model44 uses clamp(round((voltageCentivolts-11902)/29.03),0,100). Matches WheelLog simple curve; official app chooses a hardware-configured lookup table, not yet reconciled. |
| `battery1Percent` | UNMAPPED: not populated by inspected Veteran parser. Separate pack SoC has not been established; default0 is not evidence of empty pack. |
| `battery2Percent` | UNMAPPED: not populated by inspected Veteran parser. Separate pack SoC has not been established; default0 is not evidence of empty pack. |
| `pwm` | Source-aligned offset34 unsigned16 /100 percent on model44. Official raw output /10000 is the fractional multiplier used for current. |
| `torque` | UNMAPPED: not populated by inspected Veteran parser; no confirmed Aeon torque wire field in this audit. |
| `phaseCurrent` | MISSING PROJECTION: offset16 provides phase current, but shared parser leaves this dedicated field at default0. Default0 is not a measured zero. |
| `temperatures` | Source-aligned controller temperature at offset18 signed16 /100 C. Official app rounds to one decimal. BMS temperatures are separate slices. |
| `maxTemperature` | Same single controller temperature as temperatures[0], not maximum across controller and BMS sensors. |
| `tripDistance` | Source-aligned low16/high16 word ordering at8/10, meters converted to km. Not the app-local tripMeterKm. |
| `totalDistance` | Source-aligned low16/high16 word ordering at12/14, meters converted to km. |
| `pitchAngle` | Offset32 signed16 /100 in EUC and WheelLog. APK stores signed raw carPose; official presentation scaling not yet traced. Positive-direction meaning remains unverified. |
| `rollAngle` | MISSING: APK page0/4 offset67 signed16 /100 is lrAngle; EUC leaves rollAngle at default0. Need fresh page-aware projection, not copying zero on intervening pages. |
| `latitude` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `longitude` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `externalGpsBatteryPercent` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `externalGpsSpeedKmh` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `gpsSpeedKmh` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `gpsAltitudeM` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `tripMeterKm` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `gForce` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `accelX` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `accelY` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `forwardGFromSpeed` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `batteryPower` | DISCREPANCY: parser computes voltage times phase current, without PWM. Official power uses its PWM-derived current. Repository/energy consumers need separate audit. |
| `motorPower` | UNRESOLVED semantics: currently identical to batteryPower; not independently measured mechanical or motor electrical power. |
| `whConsumed` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `whRegen` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `whPerKmRecent` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `rangeKmEstimate` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `dynamicSpeedLimit` | UNMAPPED: do not equate with configured tiltback threshold wheelMaxSpeedKmh. |
| `dynamicCurrentLimit` | UNMAPPED: no confirmed Aeon current-limit telemetry field in this audit. |
| `lightOn` | Binary projection of reported light level on Aeon; not a complete light-mode representation. |
| `headlightReadback` | Implemented generic projection of captured Aeon level; owner verifies dashboard correctly reads all states changed on the physical panel. Reconnect/staleness behavior not newly physically verified. |
| `aeonLightState` | Captured Aeon page1 offset49 mapping, preserving raw unknown values. |
| `aeonSettings` | 13 official APK page8 fields decoded; historical SND/BRT transitions captured. Owner subsequently verified remote brightness and unit changes; remote SND setter remains pending. |
| `charging` | APK reads raw byte23 as chargeMode; EUC reduces it to >0. Exact nonzero mode meanings remain unresolved. WheelData comment claiming Veteran always leaves false is stale. |
| `tirePressureKpa` | UNMAPPED: no confirmed Aeon TPMS telemetry in this audit; default0 does not establish hardware absence. |
| `pcMode` | UNMAPPED: shared parser leaves -1; do not infer parking or transport mode from this other-family field. |
| `lockedReported` | UNMAPPED: no confirmed Aeon security-lock readback; null remains distinct from unlocked. |
| `wheelMaxSpeedKmh` | SOURCE-CONFIRMED mapping: offset26 unsigned16 /10 km/h, APK stopSpeed. Separate from page8 StopSpeed scalar. Physical enforcement and write/readback validation remain pending. |
| `wheelAlarmSpeedKmh` | SOURCE-CONFIRMED mapping: offset24 unsigned16 /10 km/h, APK dangerSpeed. No audible setting confirmation does not disprove this readback mapping or establish command failure. |
| `rssiDbm` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `timestamp` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |

Smart-BMS slices, identity and settings events outside WheelData also require TELEMETRY-AUDIT; do not assume this data class exhausts all wire telemetry.

## Open validation procedures

### LIGHT-SND-01

Status: **owner result recorded: silent at SND10%; volume-tracking hypothesis not supported for this tested path**. Results are owner-reported where stated; recording this document does not itself perform a live test.

Hypothesis: Original hypothesis: current remote light acknowledgements follow panel SND. Owner reports silence even at10%, so this test does not demonstrate acknowledgement-volume tracking.

Existing evidence: Owner reports SND affects each physical keypress; speed and fall-related warning beeps remain loud at SND0. Historical paired remote light beeped; PC literal ASCII was silent; earlier ASCII beta report differs.

Recorded outcome: AEON-REMOTE-SETTINGS-SOUND: brightness and unit writes work and beep; current headlight path remains silent even at SND10%. No new packet capture, calibrated loudness measurement or universal beep-control mechanism established.

#### Preconditions

- [ ] Stationary, upright and securely supported wheel; no riding, drop or speed-warning test.
- [ ] One BLE client only. Record exact app build/commit, firmware, initial SND and headlight state.
- [ ] Use physical panel to set SND so this test does not also depend on an unverified remote SND setter.
- [ ] Start passive capture explicitly, no overall auto-stop timer. Confirm it is receiving before each block; reconnect and re-establish capture after power loss.

#### Steps

- [ ] At SND0, confirm displayed value and physical keypress sound. Use app light button once ON and once OFF, waiting for each reported transition; note physical light and audible result separately.
- [ ] Repeat at a modest nonzero panel SND value agreed with owner, then at a second modest higher value. Record exact values; do not default to maximum.
- [ ] Record capture markers before instructions and after owner reports done. Use the same ON/OFF command path at every SND setting and do not mix in horn or paired legacy commands.
- [ ] Restore original SND and headlight state. Confirm both on panel/readback, then explicitly stop capture.

#### Results to record

- [ ] For each toggle: SND panel/readback, app build, timestamp, exact outbound bytes, previous/resulting light level, beep count and perceived loudness, and whether transition was physically seen.
- [ ] No beep at any nonzero SND means this path may simply be silent; it does not demonstrate volume compliance.
- [ ] Beep at nonzero and none at zero supports gating; changed loudness at two nonzero levels supports qualitative volume tracking, not a calibrated linear relationship.
- [ ] If loudness does not change, preserve the discrepancy. Leave speed/fall alarm independence as the existing owner observation, not something to provoke again.

Record outcomes in capabilities.json with evidence category, firmware/build and capture references. Never check off physical verification merely because a command was queued or a BLE write succeeded.
