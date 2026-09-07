# EUC Planet / NOSFET Aeon implementation inventory

Generated from [capabilities.json](capabilities.json). Edit the ledger, then run `python tools/render_aeon_capabilities.py`; `--check` verifies coverage and generated-file freshness without writing.

Status date: 2026-09-07. Complete coverage of the current known control ledger, not a claim of all firmware capabilities or a completed per-field telemetry audit. Shared LeaperKim and WheelLog references remain candidates unless Aeon applicability is established.

implemented means code/UI exists, not physical validation. partial/open/deferred are unchecked. Deferred items may intentionally remain unavailable. Record exact packet/build/firmware and separate send success, readback match and physical effect.

Coverage: 44 work items, all 28 APK construction sites / 24 command groups, 13 settings readbacks, 9 gap groups, 34 WheelAdapter members, 8 capability flags, 12 WheelSettings slots and 46 WheelData fields.

An expected API entry is an optional contract, not a requirement that every wheel implement it. Null follow-up packets can be correct. Unmapped, unsupported by app policy and physically absent are different states.

## EUC Planet expected surface mapped to Aeon

Confirmed in EUC Planet source means current code behavior only, not firmware verification. Interface coverage is checked against the current source; newly added methods or flags require an explicit entry.

| API / property | Aeon mapping, missing mapping or deliberate absence | Work items |
|---|---|---|
| `buildSettingChange` | Implemented typed extension for these three guarded settings; other Aeon settings rejected. | [SND](#snd), [DISPLAY-BRT](#display-brt), [UNITS](#units) |
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
| `setLight` | ASCII on/off, not full physical level cycle. SND interaction unresolved. | [LIGHT-TOGGLE](#light-toggle), [LIGHT-LEVEL](#light-level) |
| `setLightFollowup` | Null deliberately for current Aeon path. | [LIGHT-TOGGLE](#light-toggle) |
| `setMaxSpeed` | Returns null; separate commit methods below carry the commands. Do not count this null alone as no support. | [TILTBACK](#tiltback), [ALARM-SPEED](#alarm-speed) |
| `setMaxSpeedCommit` | Shared LdAp builder; Aeon remote effect not verified. | [TILTBACK](#tiltback) |
| `setAlarmSpeedCommit` | Shared LkAp builder; owner reported ineffective setting, unresolved. | [ALARM-SPEED](#alarm-speed) |
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

- [x] Backend: implemented: Aeon single ASCII command
- [x] Ui: implemented: existing dashboard tile
- [ ] Validation: partial: historical remote paths tested; current APK/SND interaction pending
- [ ] Next: Run LIGHT-SND-01 with current app

### LIGHT-LEVEL

**Headlight level readback and remote levels** (priority 1)

EUC Planet API: `setLight`

Explicit unmapped/gap group: Headlight levels.

- [ ] Backend: partial: page1 byte49 decoder; remote level setter unknown
- [ ] Ui: partial: dashboard Off/Low/Medium/High implemented; no level selector
- [ ] Validation: partial: physical cycles and captures verified; new dashboard pending
- [ ] Next: Verify panel changes update dashboard; research remote levels

### SND

**Panel keypress sound (SND)** (priority 1)

EUC Planet API: `buildSettingChange`, `setVolume`

Official command evidence: key-tone volume. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: KeyTone ([offsets and evidence](README.md#settings-page-8)).

- [x] Backend: implemented: typed guarded APK setter and readback
- [x] Ui: implemented: settings editor; current menu-key wording
- [ ] Validation: partial: physical SND readback verified; remote setter pending
- [ ] Next: Validate remote setter separately; run LIGHT-SND-01

### DISPLAY-BRT

**Display brightness percentage** (priority 1)

EUC Planet API: `buildSettingChange`

Official command evidence: display brightness. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: ScreenBacklightRate ([offsets and evidence](README.md#settings-page-8)).

- [x] Backend: implemented: typed guarded APK setter and readback
- [x] Ui: implemented: settings editor
- [ ] Validation: partial: panel brightness effect verified; remote setter pending
- [ ] Next: Change one step, check display and readback, restore

### UNITS

**Wheel display units** (priority 1)

EUC Planet API: `buildSettingChange`

Official command evidence: unit selection. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

Readback fields: Unit ([offsets and evidence](README.md#settings-page-8)).

- [x] Backend: implemented: typed guarded APK setter and readback
- [x] Ui: implemented: settings editor
- [ ] Validation: open: APK mapping and constant readback only; UNT never changed in panel session
- [ ] Next: Verify remote change and restore; distinguish app display units

### HORN

**Horn** (priority 1)

EUC Planet API: `horn`, `hornFollowup`

Official command evidence: horn. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.

- [x] Backend: implemented: Aeon single LkAp, no companion
- [x] Ui: implemented: dashboard action
- [ ] Validation: partial: Aeon horn worked; record current single-frame app retest separately
- [ ] Next: Retest current build once; record packet and audible result

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
- [ ] Ui: not applicable: automatic connection initialization, no new dashboard or settings control
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
- [ ] Validation: open: not a claim every common field is Aeon-verified
- [ ] Next: Enumerate parser fields against NOSFET APK and WheelLog commit; add per-field records

### REFERENCE-AUDIT

**Shared LeaperKim / WheelLog source reconciliation** (priority 2)

EUC Planet API: `familyId`, `capabilities`, `bleProfile`, `notifyConnectingTo`, `pickAdapterByDiscoveredServices`, `getDiagnosticCommands`, `diagnosticCatalogs`, `familyDisplayName`, `brand`, `inspectMessageTypes`

- [ ] Backend: open: pin source revisions and reconcile all candidate mappings
- [ ] Ui: not applicable: research work item
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
- [ ] Validation: open: owner reported speed alarm setting not working
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

## Full expected WheelData field list

Automatically enumerated, including fields with no established Aeon mapping. UNREVIEWED is explicit missing evidence, not a claim of zero, absence or support. Per-field source/capture reconciliation remains TELEMETRY-AUDIT; this section intentionally does not promote shared parser assumptions into Aeon facts.

| Field | Current inventory disposition |
|---|---|
| `speed` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `voltage` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `current` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `batteryPercent` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `battery1Percent` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `battery2Percent` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `pwm` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `torque` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `phaseCurrent` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `temperatures` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `maxTemperature` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `tripDistance` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `totalDistance` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `pitchAngle` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `rollAngle` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
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
| `batteryPower` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `motorPower` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `whConsumed` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `whRegen` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `whPerKmRecent` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `rangeKmEstimate` | App-derived metric; audit source inputs, not a direct wheel setter. |
| `dynamicSpeedLimit` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `dynamicCurrentLimit` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `lightOn` | Binary projection of reported light level on Aeon; not a complete light-mode representation. |
| `headlightReadback` | Implemented generic projection of captured Aeon level; dashboard uses fresh reported state. |
| `aeonLightState` | Captured Aeon page1 offset49 mapping, preserving raw unknown values. |
| `aeonSettings` | 13 official APK page8 fields decoded; SND and BRT physical transitions captured, other writes not verified. |
| `charging` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `tirePressureKpa` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `pcMode` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `lockedReported` | UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet. |
| `wheelMaxSpeedKmh` | Shared reported threshold path; exact Aeon verification still open. |
| `wheelAlarmSpeedKmh` | Aeon reported alarm threshold mapping remains to be audited, not assumed from command presence. |
| `rssiDbm` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |
| `timestamp` | App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments. |

Smart-BMS slices, identity and settings events outside WheelData also require TELEMETRY-AUDIT; do not assume this data class exhausts all wire telemetry.

## Open validation procedures

### LIGHT-SND-01

Status: **open**. No new live test performed when recording this plan.

Hypothesis: The current remote light on/off path produces an acknowledgement at nonzero panel SND and its loudness follows SND. This is not yet verified.

Existing evidence: Owner reports SND affects each physical keypress; speed and fall-related warning beeps remain loud at SND0. Historical paired remote light beeped; PC literal ASCII was silent; earlier ASCII beta report differs.

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
