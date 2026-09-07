"""Render the canonical Aeon capability ledger, without APK access or BLE.
Scan capabilities.json and this script for secrets before running.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
folder = root / 'docs/protocols/aeon'
data = json.loads((folder / 'capabilities.json').read_text(encoding='utf-8'))
def cell(value):
    return str(value).replace('|', '\\|').replace('\n', ' ')
lines = ['# NOSFET Aeon capability ledger', '',
    'Generated from [capabilities.json](capabilities.json). Edit that file first, then run `python tools/render_aeon_capabilities.py`. Earlier timestamped research reports are historical evidence, not competing current maps.', '',
    '## First pass', '',
    'Settings > General > NOSFET Aeon exposes display brightness, menu-key sound level and wheel display units. Changes require confirmation, a connected stationary non-charging wheel, fresh telemetry and a supported current readback. A subsequent matching readback is reported separately from sending; no automatic retry. New writes remain APK-confirmed, not physically verified.', '',
    'All other extracted settings are read-only. No automatic writes, command probing, calibration, experimental logging or global beep-volume reinterpretation. Existing light/horn profile and shared Veteran behavior remain intact. This is an incremental implementation, not complete firmware support.', '',
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
    'UNT was not touched during panel experiments. SND restored0%; display BRT restored30%. New UI changes are only sent after the rider explicitly confirms them. Cancel resets a draft to the current reported value; no factory default is invented.', '']
(folder / 'README.md').write_text('\n'.join(lines), encoding='utf-8')
print(folder / 'README.md')
