# Aeon Alpha wheel test checklist

Build scope: Aeon branch merged with upstream main fac53425 (0.19.0). The new current/power correction and roll-angle projection remain unimplemented. Source-confirmed settings are not yet physically verified writes.

## Before testing

- Keep the wheel stationary, upright and securely supported, with the charger disconnected. No riding, deliberate falls, motor spin-up or calibration tests.
- Connect only EUC Planet Alpha; disconnect other BLE clients. Turn off automatic headlight rules for the test so they cannot override manual changes.
- Do not enable the new upstream Lockdown mode during these tests.
- Record firmware, app version/build stamp, original SND, display BRT, units, headlight state and both configured speed thresholds. Restore changed values afterwards.
- If collecting diagnostics, record timestamps and export the report afterwards. No PC capture is automatically started by installing this APK.

## 1. Headlight acknowledgement versus SND (priority)

Use the physical panel to change SND first, isolating this experiment from the remote SND setter.

1. Set SND to 0%. Start with headlights off. Use the app button for ON, then OFF, waiting for each actual light transition.
2. Repeat at SND 2%, then 10%, or lower comfortable nonzero values. Confirm the exact panel value at each step.
3. For each toggle record: SND, starting and ending light level, app label, beep count and perceived loudness. Separate physical keypress beeps from app-toggle beeps.
4. Restore the original SND and light state.

No beep at all three levels suggests this command path is silent; it does not prove volume tracking. Silence at zero plus sound at nonzero supports gating; different loudness at two nonzero levels supports qualitative level tracking. Do not provoke speed or fall warnings to test independence.

## 2. Headlight readback

Use the panel to cycle OFF -> low -> medium -> high -> OFF. Pause at each level for telemetry. Check that the existing dashboard light button shows the corresponding label. The app button remains binary ON/OFF, not a remote four-level selector.

Disconnect and reconnect. Check that old light state is not presented as a fresh measurement and that labels recover after telemetry arrives.

## 3. Official-app-derived settings

Under Settings > General > NOSFET Aeon, test one control at a time, apply/confirm, then check both the app readback result and the physical panel:

- Menu sounds: modest change from 0 to 2%, then restore. Check ordinary physical keypress sound. This is not global alarm volume.
- Display brightness: 30 to 20%, then restore the original. Confirm the display dims, not the headlight.
- Optional units: km/h to mph and back, checking the panel's unit label. No riding needed.

Record “readback matched”, “unconfirmed” or “not sent” separately from physical effect. Stop rather than repeatedly retrying an unconfirmed write.

## 4. Optional stationary Legal-mode restoration

Only proceed after noting both original tiltback and alarm thresholds on the panel. Check the app's stored normal values agree before enabling Legal mode; an older build may already have overwritten them.

With Lockdown disabled, switch Legal ON, wait past the app cooldown and record both reported thresholds. Switch OFF and verify both original values return on the panel. A beep, button highlight or successful BLE write alone is not confirmation. If either value fails to restore, restore it on the panel and report the discrepancy before riding.

This tests setting/restoration only, not alarm audibility or tiltback enforcement while riding.

## Report format

`Test | timestamp | before -> requested -> panel/readback after | sound/light effect | app result`

Attach screenshots or diagnostics where possible. Do not reset trip/metrics, change PWM, calibrate, or test undocumented controls as part of this checklist.
