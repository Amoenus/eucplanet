# NOSFET Aeon capability ledger

For the bidirectional EUC Planet / Aeon backend, UI and validation checklist, see [INVENTORY.md](INVENTORY.md). Missing mappings are explicit there.

Generated from [capabilities.json](capabilities.json). Edit that file first, then run `python tools/render_aeon_capabilities.py`. Earlier timestamped research reports are historical evidence, not competing current maps.

## First pass

Settings > General > NOSFET Aeon exposes display brightness, menu-key sound level and wheel display units. Changes require confirmation, a connected stationary non-charging wheel, fresh telemetry and a supported current readback. A subsequent matching readback is reported separately from sending; no automatic retry. New writes remain APK-confirmed, not physically verified.

All other extracted settings are read-only. Aeon additionally sends the official clock-sync frame once after receiving valid model44 data. No automatic settings rewrites, command probing, calibration, experimental logging or global beep-volume reinterpretation. Existing light/horn profile and other Veteran behavior remain intact. This is an incremental implementation, not complete firmware support.

## Implementation boundaries

- **clock initialization**: Official BtManager.bluetoothHeatBeatOnce -> syncTime -> Util.getTimeBytes -> sendBytesData/CRC path implemented for Aeon. initSequence stays empty until model evidence; pollRealtime consumes a single deferred command after CRC-valid model44 frame. No periodic resync or semantic retry; rearmed on disconnect/model replacement. Date is generated at dispatch, local wall time plus raw standard timezone hours truncated toward zero. Unrepresentable years skipped. Queue success and firmware clock adjustment are not acknowledged by this implementation.
- **headlight dashboard**: Existing light tile uses model-provided HeadlightReadback: Off/Low/Medium/High, existing active/inactive colors. Unknown, disconnected or older than eight seconds displays Light: ?. Age is not refreshed by unrelated pages. Tap keeps current on/off command; no remote level cycling claimed. Level-reporting models do not optimistically flip light state; other models unchanged.
- **model component**: ble/nosfet/NosfetAeonProtocol.kt owns Aeon readbacks, session identity, capabilities and typed settings dispatch; composed behind VeteranModelProtocol.
- **shared core**: VeteranAdapter retains one parser, model identification, common telemetry/BMS and BLE-family API; no second BLE adapter or duplicated parser.
- **diagnostics**: OEM picker uses NOSFET; Inspector sub-entry is Aeon. Model-selected NOTE prefix NOSFET Aeon distinguishes current Aeon traces from Veteran realtime. Shared Inspector accepts metadata before len=. No extra logging or BLE traffic. OEM grouping does not prove other NOSFET models support these commands.
- **lock policy**: Unavailable for identified Aeon: hasLock=false and setLock returns null. User-requested conservative app policy, not proof that firmware cannot lock.
- **alpha build**: assembleAlpha produces com.eried.eucplanet.alpha, EUC Planet Alpha, version suffix -alpha. Stable/debug IDs and names unchanged; separate local data and grants. Stable Wear companion is not an Alpha companion.

## Evidence policy

Verified on Aeon, Verified from capture, Confirmed in NOSFET APK, Confirmed in EUC Planet source, Confirmed in NOSFET manual, Candidate / hypothesis.

APK 1.1.3 SHA256: `f4881479f3c40e2f1d54a8909223af79f5cb18eaf96d9a94f4116ce10e4a5099`. Model44, observed firmware44250,36S,151.2V class. APK UI ranges are not firmware safety limits.

## Physical observations and captured readbacks

### AEON-LIGHT-LEVELS

Verified on Aeon, Verified from capture.

Two panel off/low/medium/high/off cycles map to0/1/2/3/0. Page1 byte49 (87-byte frame); corroborating page8 byte47 (75-byte frame).

Remote multilevel command unknown. Independent pages are not interchangeable authoritative timestamps.

### AEON-LIGHT-WRITES

Verified on Aeon, Verified from capture.

Original LkAp+LdAp light pair toggled with beep. Later PC literal ASCII SetLightON/OFF toggled silently.

Earlier ASCII-beta beep report differs; keep that history. No proof all BLE light commands beep or respect SND.

### AEON-SND

Verified on Aeon, Verified from capture, Confirmed in NOSFET APK.

SND0->2->0 maps to page8 byte63; restored0. APK KeyToneSettingActivity gives exact setter. Owner subsequently confirms SND affects every physical-panel keypress, while speed warnings and the beep observed when the wheel fell remain loud at SND0. These additional audible effects are owner-reported physical observations, not newly captured packets.

Remote setter and effects on BLE acknowledgement beeps untested.

### AEON-BRT

Verified on Aeon, Verified from capture, Confirmed in NOSFET APK.

BRT30->20 dimmed display. Restored through25 to30 after restart; page8 byte55.

Display brightness, not headlight level or automatic BRT on/off. Remote write untested.

### AEON-UNT

Confirmed in NOSFET APK, Verified from capture.

User explicitly did not change UNT. Page8 byte58 stays0; APK maps0metric/1imperial.

No physically verified units transition or remote write.

### AEON-REAR-DRL

Verified on Aeon.

Separate DRL cycle observed. Rear sequence: solidred, blueflash returningred, blinkingred, smoothRGB, off, solidred.

No confirmed readback/write. Pages0/4 byte48 counter rejected as rear-mode mapping. LTB startup default is distinct.

### AEON-PANEL

Verified on Aeon, Confirmed in NOSFET manual.

Panel inventory preserves distinct MD, ANG, ANG%, ANG TLT, TLT, PWT, ALM speed, ALM on/off, BRT%, BRT on/off, CAL,TRM,UNT,VtA,STA,SND,MxV,LTB and unresolved ?br/?bL/?Adv.

No automatic equation between ambiguous labels and shared APK setters.

## Settings page 8

Absolute zero-based offsets from DC5A5C; selector byte46=8. Unsupported0x80 remains distinct from0. Original receive age is retained when other pages arrive; disconnect clears snapshots.

| Field | Offset | Captured raw values | Physical edits tested | Availability / caution |
|---|---:|---|---|---|
| PedalHardness | 50 | [80] | [] | Non-sentinel field present; remote write not tested |
| StopSpeed | 52 | [40] | [] | Non-sentinel field present; remote write not tested |
| StopPowerRate | 53 | [65] | [] | Non-sentinel field present; remote write not tested |
| ScreenBacklightRate | 55 | [20, 25, 30] | [20, 25, 30] | Non-sentinel field present; remote write not tested |
| Gyro | 56 | [0] | [] | Non-sentinel field present; remote write not tested |
| TransportMode | 57 | [0] | [] | Non-sentinel field present; remote write not tested |
| Unit | 58 | [0] | [] | Non-sentinel field present; remote write not tested |
| Vol | 59 | [0] | [] | Non-sentinel field present; remote write not tested |
| LowVolMode | 60 | [128] | [] | Unsupported sentinel in both captures |
| HighSpeedMode | 61 | [128] | [] | Unsupported sentinel in both captures |
| KeyTone | 63 | [0, 2] | [0, 2] | Non-sentinel field present; remote write not tested |
| MaxChargeVol | 64 | [62] | [] | Non-sentinel field present; remote write not tested |
| BrakePressureAlarm | 65 | [145] | [] | Raw145 exceeds APK UI90..125: semantic/encoding discrepancy unresolved; do not expose as plain writable percentage |

## Command inventory

Every row is Confirmed in NOSFET APK. Templates exclude the CRC32 big-endian trailer; their embedded length includes it. Variable offsets and explicit source references are preserved in JSON. Duplicate sites and on/off variants are intentional.

| Control | Prefix bytes before CRC | Range / transform | First-pass status |
|---|---|---|---|
| brake-pressure alarm | 4C 64 41 70 1E 01 02 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 {by} | Progress0..35 -> raw90..125 percent | Existing behavior or documented only; see first_pass |
| gyroscope calibration | 4C 64 41 70 15 01 02 80 80 80 80 80 80 80 80 80 01 | Same fixed action1 on start and end; UI waits1200ms before allowing end | Existing behavior or documented only; see first_pass |
| key-tone volume | 4C 64 41 70 1C 01 02 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 {by} | Progress 0..100 -> raw unchanged | APK-confirmed setter exposed for opt-in validation |
| maximum charge voltage | 4C 64 41 70 1D 01 02 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 80 {by} | Progress/raw 0..70; display145.0 + raw/10 V (145.0..152.0) | Existing behavior or documented only; see first_pass |
| pedal hardness | 4C 64 41 70 0F 01 02 80 80 80 {by} | UI 0..100, raw u8 unchanged | Existing behavior or documented only; see first_pass |
| display brightness | 4C 64 41 70 14 01 02 80 80 80 80 80 80 80 80 {by} | Progress 0..100 -> raw unchanged; displayed text rounds DOWN to multiple of 5 | APK-confirmed setter exposed for opt-in validation |
| PWM threshold | 4C 64 41 70 12 01 02 80 80 80 80 80 80 {by} | Progress 0..70 -> raw 30..100 percent | Existing behavior or documented only; see first_pass |
| tilt-back speed | 4C 64 41 70 11 01 02 80 80 80 80 80 {by} | Progress 0..110 -> raw km/h 10..120 | Existing behavior or documented only; see first_pass |
| voltage adjustment | 4C 64 41 70 18 01 02 80 80 80 80 80 80 80 80 80 80 80 80 {by} | Progress0..30 -> signed raw -15..15; displayed -1.5..+1.5 percent | Existing behavior or documented only; see first_pass |
| high-speed mode | 4C 64 41 70 1A 01 02 80 80 80 80 80 80 80 80 80 80 80 80 80 80 {by} | Boolean raw 0/1 | Existing behavior or documented only; see first_pass |
| low-battery mode | 4C 64 41 70 19 01 02 80 80 80 80 80 80 80 80 80 80 80 80 80 {by} | Boolean raw 0/1 | Existing behavior or documented only; see first_pass |
| transport mode | 4C 64 41 70 16 01 02 80 80 80 80 80 80 80 80 80 80 {by} | Boolean raw 0/1 | Existing behavior or documented only; see first_pass |
| unit selection | 4C 64 41 70 17 01 02 80 80 80 80 80 80 80 80 80 80 80 {by} | 0 metric / 1 imperial; UnitSwitchActivity inverts isKM Boolean when sending | APK-confirmed setter exposed for opt-in validation |
| alarm speed | 4C 6B 41 70 11 01 80 80 80 80 80 80 {by} | Progress 0..110 -> raw km/h 10..120 | Existing behavior or documented only; see first_pass |
| vertical angle | 4C 6B 41 70 10 01 80 80 80 80 80 {by} | Progress 0..160 -> signed raw -80..80; units tenths of degree (-8..8 degrees) | Existing behavior or documented only; see first_pass |
| side-tilt/fall angle | 4C 6B 41 70 16 01 80 80 80 80 80 80 80 80 80 80 80 {by} | Progress0..40 -> raw35..75 degrees | Existing behavior or documented only; see first_pass |
| continuous ride mode | 4C 6B 41 70 0C 01 80 {by} | UI progress 0..100; command = progress+10 (10..110). UI readback initialization = rideMode-100. | Existing behavior or documented only; see first_pass |
| unit selection | 4C 64 41 70 17 01 02 80 80 80 80 80 80 80 80 80 80 80 {by} | 0 metric / 1 imperial; UnitSwitchActivity inverts isKM Boolean when sending | APK-confirmed setter exposed for opt-in validation |
| horn | 4C 6B 41 70 0E 00 80 80 80 01 | Fixed action value 1 | Existing behavior or documented only; see first_pass |
| headlight | 4C 6B 41 70 0D 01 80 80 01 | 0 off / 1 on only | Existing behavior or documented only; see first_pass |
| headlight | 4C 6B 41 70 0D 01 80 80 00 | 0 off / 1 on only | Existing behavior or documented only; see first_pass |
| trip reset | 4C 6B 41 70 0B 00 01 | Fixed action 1 | Existing behavior or documented only; see first_pass |
| discrete ride mode | 4C 6B 41 70 0C 01 80 01 | 1 soft / 2 medium / 3 hard | Existing behavior or documented only; see first_pass |
| discrete ride mode | 4C 6B 41 70 0C 01 80 02 | 1 soft / 2 medium / 3 hard | Existing behavior or documented only; see first_pass |
| discrete ride mode | 4C 6B 41 70 0C 01 80 03 | 1 soft / 2 medium / 3 hard | Existing behavior or documented only; see first_pass |
| read log | 4C 6B 41 70 14 01 80 80 80 80 80 80 80 80 80 01 | Fixed request1 | Existing behavior or documented only; see first_pass |
| shutdown after 10 seconds | 4C 6B 41 70 16 01 80 80 80 80 80 80 80 80 80 80 01 80 | Fixed action embedded before final0x80; preserve full bytes | Existing behavior or documented only; see first_pass |
| time synchronization | 4C 64 41 70 12 00 05 {(byte)(n2 - 2000)} {(byte)(n3 + 1)} {(byte)n4} {(byte)n5} {(byte)n6} {(byte)n7} {(byte)n8} | year-2000, month1..12, day,hour,minute,second; raw timezone offset in whole hours | Implemented: one official clock-sync attempt after validated model44 data per connection/model session; unit-tested, wheel clock effect unverified |

## Remaining gaps

| Control | APK | EUC Planet | Evidence / qualification |
|---|---|---|---|
| Headlight levels | No multilevel write or parsed light-level field found in inspected app code | AeonTelemetryDecoder uses87-byte page1 byte49; dashboard label and highlight use generic HeadlightReadback projection with freshness, committed in3d67c95f | Verified on Aeon/capture: off0 low1 medium2 high3; page8 byte47 corroborates. Remote level-setting command still unknown. |
| DRL | No separate DRL setter found | setDRL returns null | Physical off/on/off observed; no confident readback or remote command. |
| Rear-light active mode / LTB startup mode | No corresponding setter found | No equivalent API | Physical rear cycle observed; startup setting is a separate manual capability. Counter-like byte48 on pages0/4 rejected as mapping. |
| Auto headlight BRT on/off | No identified setter | No equivalent API | Manual-documented; separate from BRT percentage display brightness. |
| Acceleration assistance ANG percent / recenter ANG TLT | No identified matching setter | No corresponding API | Manual/panel evidence only; do not substitute ordinary angle or hardness command. |
| Torque ALM on/off and ?br / ?bL / ?Adv | No established matches | No equivalent typed API | Preserve labels. Do not assume BrakePressureAlarm or HighSpeedMode is the same control. |
| Security lock/unlock | Homepage lock callback displays a toast; no equivalent25-byte builder found | Unavailable on Aeon: capability false, lock builder returns null, dashboard disabled after model detection. Other Veteran models retain the shared25-byte LdAp command split20+5. | Confirmed EUC code: conservative app policy requested by owner. Firmware lock support remains unresolved, not proven absent. Physical parking/lift/transport are not proven security-lock equivalents. |
| Authentication | No matching wheel-control authentication command identified by this inventory | requestAuthKey/verifyAuth return null | No evidence that Aeon requires this generic authentication flow. |
| Firmware update / bootloader | AT+RINTOPRO, sendBinData and upgrade/recovery paths inventoried in raw transport calls | Outside control-profile scope | Dangerous maintenance, not a candidate for live command enumeration. Byte-array inventory does not reconstruct arbitrary firmware payloads. |

## Validation boundaries

Unit tests validate decoding, model isolation, sentinels, freshness, CRC and splitting. They do not validate firmware execution, alarm behavior, all firmware versions or physical consequences. The ledger retains CRC-valid captured setting frames and file hashes; no raw logs or manufacturer APK are checked into the app repository.

UNT was not touched during panel experiments. SND restored0%; display BRT restored30%. New UI changes are only sent after the rider explicitly confirms them. Cancel resets a draft to the current reported value; no factory default is invented.
