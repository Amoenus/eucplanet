# Aeon support development

## Current source of truth

The durable bidirectional [implementation inventory](aeon/INVENTORY.md) maps the
EUC Planet interface, capability flags and generic settings to known Aeon controls,
with separate backend/UI/validation checklists and explicit unmapped entries.
It is generated from `aeon/capabilities.json`, not maintained independently.
Run `python tools/render_aeon_capabilities.py --check` to catch inventory drift.
The pending `LIGHT-SND-01` procedure compares the current remote light command at
zero and two modest nonzero physical-panel SND levels, then restores the baseline.
Its hypothesis is not a confirmed relationship between SND and BLE acknowledgements.

### Dashboard headlight readback

The existing light tile displays Off, Low, Medium or High from the captured
page-1 readback. It keeps the existing active/inactive theme colors and layout.
Unknown values, disconnects and readings older than eight seconds show `Light: ?`.
This eight-second UI freshness policy is not a firmware guarantee. Unrelated
telemetry pages do not refresh the light reading's age.

Tap still sends the existing binary on/off action, not a brightness-cycle command.
Models providing `HeadlightReadback` use telemetry rather than optimistic light
state updates, including during command cooldown. Other models retain the original
label and behavior. No new wheel command or additional logging was added here.

### Build and delivery policy

Create a new APK only when the user explicitly requests one. Validation may run
compilation and unit tests (for example `:app:testAlphaUnitTest`) without packaging
an APK. Do not automatically upload builds to Drive.

### Service Mode naming

The large OEM picker uses `NOSFET` in Commands, Inspect and raw presets. Within
Inspect, the smaller entry is `Aeon`, backed by the existing telemetry NOTE stream
now named `NOSFET Aeon`. Other Veteran models keep `Veteran realtime`. This changes
the stream label, not the amount of logging or any BLE traffic. Historical Aeon
logs written with the generic Veteran label remain under that historical prefix.
Inspector accepts speed/page metadata before `len=` and rejects malformed byte
tokens rather than shifting displayed offsets. OEM grouping does not establish
command compatibility with other NOSFET models.

### Model ownership and Alpha packaging

Aeon protocol code lives in `ble/nosfet/`. `NosfetAeonProtocol` owns its
settings/light readbacks, session identity and capabilities behind the composed
`VeteranModelProtocol` boundary. `AeonControlProfile` explicitly selects command
semantics using shared builders. `VeteranAdapter` keeps the single frame parser,
identification and common telemetry/BMS path. Disconnect/model changes invalidate
model state and any pending lock tail. Additional NOSFET models do not inherit
Aeon support just because they share a brand.

Service Mode enumerates model catalogues through `WheelAdapter.diagnosticCatalogs`;
the diagnostics ViewModel has no NOSFET-specific insertion. No duplicate adapter
instance or second BLE connection is created to display the NOSFET option.

Remote lock is now unavailable on identified Aeon: the capability is false and
the command returns null. The dashboard refreshes this capability after telemetry
identifies the model, not just on initial connection. This is conservative app
policy at the owner's request, not proof that firmware lacks a lock command.
Existing speed/alarm paths remain unchanged and do not gain verified status.

Build `./gradlew :app:assembleAlpha` for the side-by-side phone app:
`com.eried.eucplanet.alpha`, launcher name `EUC Planet Alpha`, version suffix
`-alpha`. Normal debug/release package IDs and names are unchanged. Alpha uses
debug signing and separate local settings, trips and Android permission grants.
The stable Wear OS companion is not an Alpha companion; watch pairing and online
services tied to package/signing identity have not been validated for Alpha.
Dropbox currently shares the existing OAuth redirect scheme, so both installations
can appear as handlers. Do not connect both apps to the same wheel simultaneously.

The combined official-app and physical reverse-engineering ledger is now
[aeon/capabilities.json](aeon/capabilities.json), with a generated readable
[capability report](aeon/README.md). Update the ledger first; the earlier
research steps below are retained as history.

The first pass adds 13 settings readbacks with raw/sentinel preservation,
and explicit APK-derived display brightness, menu-key sound and wheel-units
controls under Settings > General > NOSFET Aeon. Other extracted settings
remain read-only. New writes require a fresh supported readback, model44,
stationary non-charging state and confirmation. They await a later matching
readback and otherwise report an unknown result without retrying.

SND is not wired to generic setVolume. Physical headlight levels remain
read-only; current profile light/horn bytes are unchanged. No new telemetry
current/power scaling, calibration, transport, alarm/PWM limits, exploratory
logging or unsafe commands are enabled in this increment.

Branch `aeon-full-support` extends `aeon-control-profile`. It is incremental
work, not a claim of complete Aeon support. Keep shared Veteran transport,
reassembly and common telemetry; isolate model-specific control policy and
telemetry interpretation. No exploratory logging or undocumented writes.

## Physical light readback

Evidence: NF7445, raw firmware version 44250, passive PC capture on 2026-09-07
11:33:32–11:35:32 Europe/Riga. 595 reconstructed CRC-valid frames, zero CRC
failures. User performed the requested off/low/medium/high/off panel cycle.

Zero-based offsets from DC 5A 5C; page selector at 46:

| State interpretation | Wire value | Page 01 offset 49 first observed | Page 08 offset 47 first observed |
|---|---:|---|---|
| Off | 0 | 11:33:32.633 | 11:33:33.633 |
| Low | 1 | 11:33:39.495 | 11:33:41.714 |
| Medium | 2 | 11:33:51.412 | 11:33:53.853 |
| High | 3 | 11:34:03.336 | 11:34:05.953 |
| Off | 0 | 11:34:13.433 | 11:34:14.034 |

Verified from capture: both fields follow 0/1/2/3/0 with stable plateaus.
The level interpretation is supported by the controlled physical sequence on
this Aeon. Other firmware versions and additional values remain unverified.
No direct brightness write command follows from these readback offsets.

Implementation uses only page 01, 87-byte frames, on the Aeon model. Page 08
is corroborating evidence, not a second writer to the cached state. Unknown
values remain raw with no invented level or Boolean interpretation. Other
pages retain the last observation; disconnect clears it. Existing Boolean
consumers get known on/off state, while WheelData retains all levels.

Tests use synthetic CRC-valid frames, not mislabeled captured fixtures.
Source capture is retained locally as aeon-pc-capture-20260907-113324.txt.

## Next increments

1. Replay complete capture fixtures and verify the phone UI follows panel changes.
2. Expose a read-only level label; do not present brightness selection yet.
3. Trace manufacturer APK builders, model-44 gates, checksums and readbacks
   for light modes, alarm speed, tiltback, lock, ride mode, pedal stiffness,
   transport, PWM, angle, trip reset and horn. Classify each separately as
   source-confirmed, capture-confirmed, wheel-tested or candidate.
4. Add typed capabilities and model policies for supported settings, then
   controlled opt-in wheel tests before enabling new writes. Safety-critical
   calibration and command fuzzing are excluded.

Remote SetLightON/OFF and single-frame horn policy are inherited from the
existing control-profile branch. This increment neither changes those bytes
nor adds beep-volume configuration or a new brightness command.
