"""Render the canonical Aeon capability ledger, without APK access or BLE.
Scan capabilities.json, this script, WheelAdapter.kt, WheelData.kt and WheelSettings.kt before running.
For --check, also scan the generated README.md and INVENTORY.md before reading them.
"""
import json
import re
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
folder = root / 'docs/protocols/aeon'
data = json.loads((folder / 'capabilities.json').read_text(encoding='utf-8'))

inventory = data['inventory']
items = inventory['items']
expected = data['euc_expected_surface']
ids = {item['id'] for item in items}
assert len(ids) == len(items), 'Duplicate inventory IDs'
for refs, source, key in [('command_refs', 'commands', 'semantic'),
                          ('gap_refs', 'gaps', 'control'),
                          ('readback_refs', 'readbacks', 'field')]:
    covered = {ref for item in items for ref in item[refs]}
    required = {row[key] for row in data[source]}
    assert covered == required, f'{source} coverage drift: missing={required-covered}, unknown={covered-required}'

adapter_file = root / 'app/src/main/java/com/eried/eucplanet/ble/WheelAdapter.kt'
wheel_file = root / 'app/src/main/java/com/eried/eucplanet/data/model/WheelData.kt'
adapter_source = adapter_file.read_text(encoding='utf-8')
interface = adapter_source.split('interface WheelAdapter {', 1)[1].split('sealed class DecodeResult', 1)[0]
members = re.findall(r'^    (?:fun|val) (\w+)', interface, re.M)
documented = [entry['member'] for entry in expected['api']]
assert len(documented) == len(set(documented)), 'Duplicate API entries'
assert set(members) == set(documented), f'WheelAdapter drift: {set(members)^set(documented)}'
flags_source = adapter_source.split('data class WheelCapabilities(', 1)[1].split(') {', 1)[0]
flags = re.findall(r'val (\w+): Boolean', flags_source)
assert set(flags) == {entry['name'] for entry in expected['capability_flags']}, 'Capability flag drift'
for entry in expected['api']:
    assert set(entry['inventory_refs']) <= ids, f'Unknown inventory reference: {entry}'
for entry in expected['capability_flags']:
    assert entry['inventory_ref'] in ids
wheel_fields = re.findall(r'^    val (\w+):', wheel_file.read_text(encoding='utf-8'), re.M)
settings_file = root / 'app/src/main/java/com/eried/eucplanet/data/model/WheelSettings.kt'
settings_fields = re.findall(r'^    val (\w+):', settings_file.read_text(encoding='utf-8'), re.M)
assert set(settings_fields) == set(expected['wheel_settings']), 'WheelSettings coverage drift'
field_groups = [set(expected[key]) for key in ['telemetry_notes', 'non_wheel_fields', 'derived_fields']]
assert set.union(*field_groups) == set(wheel_fields), 'WheelData disposition coverage drift'
assert sum(map(len, field_groups)) == len(wheel_fields), 'Overlapping WheelData dispositions'
telemetry = data['telemetry_reconciliation']
assert len({row['id'] for row in telemetry['rows']}) == len(telemetry['rows']), 'Duplicate telemetry rows'
assert all(all(row[key] for key in ['id', 'wire', 'euc', 'apk', 'wheellog', 'finding', 'next'])
           for row in telemetry['rows']), 'Incomplete telemetry row'

def emit(path, content):
    if '--check' in sys.argv:
        assert path.exists() and path.read_text(encoding='utf-8') == content, f'Stale generated file: {path}'
    else:
        path.write_text(content, encoding='utf-8')
def cell(value):
    return str(value).replace('|', '\\|').replace('\n', ' ')
lines = ['# NOSFET Aeon capability ledger', '',
    'For the bidirectional EUC Planet / Aeon backend, UI and validation checklist, see [INVENTORY.md](INVENTORY.md). Missing mappings are explicit there.', '',
    'Generated from [capabilities.json](capabilities.json). Edit that file first, then run `python tools/render_aeon_capabilities.py`. Earlier timestamped research reports are historical evidence, not competing current maps.', '',
    '## First pass', '',
    'Settings > General > NOSFET Aeon exposes display brightness, menu-key sound level and wheel display units. Changes require confirmation, a connected stationary non-charging wheel, fresh telemetry and a supported current readback. A subsequent matching readback is reported separately from sending; no automatic retry. Owner reports confirm remote brightness and unit changes work and beep. Remote SND remains unverified; see the observations and per-control checklist.', '',
    'All other extracted settings are read-only. Aeon additionally sends the official clock-sync frame once after receiving valid model44 data. No automatic settings rewrites, command probing, calibration, experimental logging or global beep-volume reinterpretation. Existing light/horn profile and other Veteran behavior remain intact. This is an incremental implementation, not complete firmware support.', '',
    '## Implementation boundaries', '',
    *[f'- **{key.replace("_", " ")}**: {value}' for key, value in data.get('implementation', {}).items()], '',
    '## Evidence policy', '', ', '.join(data['evidence_policy'])+'.', '',
    f"APK 1.1.3 SHA256: `{data['apk']['sha256']}`. Model44, observed firmware44250,36S,151.2V class. APK UI ranges are not firmware safety limits.", '',
    '## Physical observations and captured readbacks', '']
for obs in data['observations']:
    lines += [f"### {obs['id']}", '', ', '.join(obs['evidence'])+'.', '', obs['fact'], '', obs.get('limitation',''), '']
lines += ['## Settings page 8', '', 'Absolute zero-based offsets from DC5A5C; selector byte46=8. Unsupported0x80 remains distinct from0. Original receive age is retained when other pages arrive; disconnect clears snapshots.', '',
    '| Field | Offset | Captured raw values | Physical edits tested | Availability / caution |', '|---|---:|---|---|---|']
for r in data['readbacks']:
    lines.append('| '+' | '.join(map(cell,[r['field'],r['absolute_byte_offset'],r['captured_raw_values'],r['tested_values'],r['source_based_availability']]))+' |')
lines += ['', '## Command inventory', '', 'Every row is Confirmed in NOSFET APK. Templates exclude the CRC32 big-endian trailer; their embedded length includes it. Variable offsets and explicit source references are preserved in JSON. Duplicate sites and on/off variants are intentional.', '',
    '| Control | Prefix bytes before CRC | Range / transform | First-pass status |', '|---|---|---|---|']
for r in data['commands']:
    lines.append('| '+' | '.join(map(cell,[r['semantic'],r['prefix_hex_template'],r['app_transform'],r['first_pass_status']]))+' |')
lines += ['', '## Remaining gaps', '', '| Control | APK | EUC Planet | Evidence / qualification |', '|---|---|---|---|']
for r in data['gaps']:
    lines.append('| '+' | '.join(cell(r[k]) for k in ['control','apk','euc','evidence'])+' |')
lines += ['', '## Validation boundaries', '',
    'Unit tests validate decoding, model isolation, sentinels, freshness, CRC and splitting. They do not validate firmware execution, alarm behavior, all firmware versions or physical consequences. The ledger retains CRC-valid captured setting frames and file hashes; no raw logs or manufacturer APK are checked into the app repository.', '',
    'In the earlier capture session, UNT was not touched, SND was restored0% and display BRT restored30%. Later owner tests verified remote unit and brightness changes; their final restored values were not reported. New UI changes are only sent after the rider explicitly confirms them. Cancel resets a draft to the current reported value; no factory default is invented.', '']
emit(folder / 'README.md', '\n'.join(lines))

lines = ['# EUC Planet / NOSFET Aeon implementation inventory', '',
    'Generated from [capabilities.json](capabilities.json). Edit the ledger, then run `python tools/render_aeon_capabilities.py`; `--check` verifies coverage and generated-file freshness without writing.', '',
    f"Status date: {inventory['as_of']}. {inventory['scope']}", '',
    inventory['status_policy'], '',
    f"Coverage: {len(items)} work items, all {len(data['commands'])} APK construction sites / {len(set(r['semantic'] for r in data['commands']))} command groups, {len(data['readbacks'])} settings readbacks, {len(data['gaps'])} gap groups, {len(members)} WheelAdapter members, {len(flags)} capability flags, {len(settings_fields)} WheelSettings slots and {len(wheel_fields)} WheelData fields.", '',
    'An expected API entry is an optional contract, not a requirement that every wheel implement it. Null follow-up packets can be correct. Unmapped, unsupported by app policy and physically absent are different states.', '',
    '## Implemented progress', '',
    'These parts are already in source. Pending physical tests do not make backend/UI work unimplemented. N/A means no UI is needed, not unfinished UI. Partial items remain in the detailed checklist.', '',
    '| Work item | Backend | UI | Physical validation / remaining work |', '|---|---|---|---|']
for item in items:
    if not any(item[stage].startswith('implemented:') for stage in ['backend', 'ui']):
        continue
    def progress(state):
        if state.startswith('implemented:'):
            return 'Implemented'
        if state.startswith('not applicable:'):
            return 'N/A'
        return state.split(':', 1)[0].capitalize()
    ref = item['id']
    lines.append(f"| [{ref}](#{ref.lower()}) | {progress(item['backend'])} | {progress(item['ui'])} | {cell(item['validation'])} |")
lines += ['', '## EUC Planet expected surface mapped to Aeon', '',
    'Confirmed in EUC Planet source means current code behavior only, not firmware verification. Interface coverage is checked against the current source; newly added methods or flags require an explicit entry.', '',
    '| API / property | Aeon mapping, missing mapping or deliberate absence | Work items |', '|---|---|---|']
for entry in expected['api']:
    links = ', '.join(f'[{ref}](#{ref.lower()})' for ref in entry['inventory_refs'])
    lines.append(f"| `{entry['member']}` | {cell(entry['status'])} | {links} |")
lines += ['', '### Declared capability flags', '',
    'These are current app declarations. In particular, speed/alarm true does not establish working Aeon execution; false does not prove the physical feature is absent.', '',
    '| Flag | Declared on Aeon | Work item |', '|---|---|---|']
for entry in expected['capability_flags']:
    ref = entry['inventory_ref']
    lines.append(f"| `{entry['name']}` | `{str(entry['declared']).lower()}` | [{ref}](#{ref.lower()}) |")
lines += ['', '### Generic WheelSettings slots', '',
    'These generic fields include other-family settings. Defaults are not Aeon observations, and similarly named values must not be equated without evidence.', '',
    '| Expected slot | Aeon mapping or explicit gap |', '|---|---|']
for field in settings_fields:
    lines.append(f"| `{field}` | {cell(expected['wheel_settings'][field])} |")
lines += ['', '## Backend / UI / validation checklist', '',
    'Priority 1: validate existing low-risk support. Priority 2: passive features and bounded additions. Priority 3: ambiguous or riding-affecting behavior. Priority 4: deferred maintenance/security/safety scope. Priority is not authorization to send commands.', '']
for item in sorted(items, key=lambda item: item['priority']):
    lines += [f"### {item['id']}", '', f"**{item['title']}** (priority {item['priority']})", '']
    api = [entry['member'] for entry in expected['api'] if item['id'] in entry['inventory_refs']]
    lines += ['EUC Planet API: ' + (', '.join(f'`{name}`' for name in api) if api else 'No dedicated generic control member. Typed settings/readback or a future extension is needed where applicable.'), '']
    if item['command_refs']:
        lines += ['Official command evidence: ' + ', '.join(item['command_refs']) + '. Exact construction sites, transforms and provenance are in the [command ledger](README.md#command-inventory). APK presence alone does not establish Aeon applicability.', '']
    if item['gap_refs']:
        lines += ['Explicit unmapped/gap group: ' + '; '.join(item['gap_refs']) + '.', '']
    if item['readback_refs']:
        lines += ['Readback fields: ' + ', '.join(item['readback_refs']) + ' ([offsets and evidence](README.md#settings-page-8)).', '']
    for stage in ['backend', 'ui', 'validation']:
        state = item[stage]
        if state.startswith('not applicable:'):
            lines.append(f'- {stage.capitalize()}: {state}')
            continue
        tick = 'x' if state.startswith('implemented:') or state.startswith('verified:') else ' '
        lines.append(f"- [{tick}] {stage.capitalize()}: {state}")
    lines += [f"- [ ] Next: {item['next_action']}", '']
lines += ['## Offline telemetry reconciliation', '',
    f"Reviewed: {telemetry['as_of']}. {telemetry['scope']}", '',
    'Evidence: EUC and APK columns are Confirmed in EUC Planet source and Confirmed in NOSFET APK respectively. WheelLog is Confirmed in WheelLog source at the pinned revision, not a claim about its current head. Findings distinguish source differences from unverified physical behavior.', '',
    '### Source anchors', '']
for key, value in telemetry['sources'].items():
    rendered = f'[{key}]({value})' if value.startswith('https://') else value
    lines.append(f'- {key}: {rendered}')
lines += ['', '### Mapping and gaps', '',
    '| Field group / wire | EUC Planet | Official APK | WheelLog reference | Finding / next action |',
    '|---|---|---|---|---|']
for row in telemetry['rows']:
    lines.append('| ' + ' | '.join(map(cell, [row['id'] + ': ' + row['wire'], row['euc'],
        row['apk'], row['wheellog'], row['finding'] + ' Next: ' + row['next']])) + ' |')
lines += ['', '## Full expected WheelData field list', '',
    'Automatically enumerated with an explicit disposition for every field; new fields fail the generator until classified. UNMAPPED means missing evidence or projection, not measured zero, physical absence or support. Source comparisons do not promote shared parser assumptions into Aeon physical facts.', '',
    '| Field | Current inventory disposition |', '|---|---|']
for field in wheel_fields:
    if field in expected['telemetry_notes']:
        note = expected['telemetry_notes'][field]
    elif field in expected['non_wheel_fields']:
        note = 'App/phone/external-sensor/connection metadata, not an Aeon wheel-command gap. Source: WheelData declaration/comments.'
    elif field in expected['derived_fields']:
        note = 'App-derived metric; audit source inputs, not a direct wheel setter.'
    else:
        note = 'UNREVIEWED: exact Aeon field/source/units/applicability and UI validation not reconciled in this inventory yet.'
    lines.append(f'| `{field}` | {cell(note)} |')
lines += ['', 'Smart-BMS slices, identity and settings events outside WheelData also require TELEMETRY-AUDIT; do not assume this data class exhausts all wire telemetry.', '',
    '## Open validation procedures', '']
for plan in inventory['validation_plans']:
    lines += [f"### {plan['id']}", '', f"Status: **{plan['status']}**. Results are owner-reported where stated; recording this document does not itself perform a live test.", '',
        'Hypothesis: ' + plan['hypothesis'], '', 'Existing evidence: ' + plan['known'], '']
    if plan.get('outcome'):
        lines += ['Recorded outcome: ' + plan['outcome'], '']
    for key in ['preconditions', 'steps', 'results_to_record']:
        lines += [f"#### {key.replace('_', ' ').capitalize()}", '']
        lines += [f'- [ ] {step}' for step in plan[key]]
        lines.append('')
lines += ['Record outcomes in capabilities.json with evidence category, firmware/build and capture references. Never check off physical verification merely because a command was queued or a BLE write succeeded.', '']
emit(folder / 'INVENTORY.md', '\n'.join(lines))
print(f"Inventory coverage OK: {len(items)} items, {len(members)} API members, {len(wheel_fields)} WheelData fields")
