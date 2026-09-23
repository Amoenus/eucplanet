# next-experimental

Where new features are built first. Things here work on the wheels they were
written against and may not work on yours yet, and settings can move or reset
between builds. Fine for a ride you are happy to cut short, not for one you
need to get home from.

For riders who are happy to report what broke. Everyone else wants the Play
Store build.

## What to check in this build

**Watch map (Wear OS).** Contributed by ZiraiMode (PR #25). Settings, Watch,
Watch map on: the watch gets a third page with your position, the route while
navigating, and minus and plus zoom buttons on the dial. Map orientation
(north up or heading up) and the telemetry strip along the top are settings on
the phone. Tiles come from the phone over the Data Layer, so it works with no
watch data plan; the first tiles take a few seconds. Advanced, Map cache, sets
how much the phone keeps. Report if the map lags the wheel, if the route is
missing while the phone shows one, or if the watch drains noticeably faster
with the page open.

**Begode PWM sign.** On a T4 (or any Begode whose speed used to read negative
before the app fixed that) the PWM tile now reads a positive number under
load, and a PWM alarm set at, say, 60 % fires when you push past it. Same
source on the wheel, just without the minus (issue #24).

**KingSong lock.** The Lock Wheel tile and the horn now work on KingSong,
from a tester's captures of the official app on a KS-18XL and confirmed by
him: nothing to type, the wheel takes the default six digits. If you set a
password in the KingSong app, the wheel ignores lock and unlock until the app
has sent it, so enter the same four digits in Settings, Advanced, Wheel
controls, KingSong app password; leave 0000 if you never set one. Confirmed
working with a password on 2026-09-23. If yours refuses, say which model and
firmware, and whether a password is set.

**Tyre sensors.** Settings, Integration, TPMS sensors, Scan for sensors. A
screw-on valve cap should be found within a minute and stay in the list after
the scan stops. Its pressure should now reach the dashboard tile, an alarm
rule, the trip graphs, the overlay and the HUD, not just the settings row. Let
some air out: the reading should fall and reach 0 on a flat tyre, and a low
pressure alarm should fire there. The wheel's own sensor (InMotion P6) still
works and steps aside when a cap is talking.

**Battery (est).** New dashboard tile, and an alarm metric filed under
Battery. It is the battery percentage with the load taken out, so on an 84 V
pack it should sit still while the plain Battery number dives under
acceleration and comes back when you coast. Two rules to hold it to: it never
goes up while you ride (only on the charger), and it only steps down once two
half minutes in a row agree the pack really dropped. If you see it climb
mid-ride, or dive on a launch, that is the bug to report. Turn on Settings,
Wheel parameters, Override the wheel's percentage: both numbers should follow
the override together. Also available on overlay elements and, with an updated
HUD build, on the glasses.

No wheel handy? Service Mode (hold the logo on the About screen) adds a
"Virtual Begode Master (sagging pack)" to the wheel picker: a pack that sags
ten points on the throttle, jitters, and loses a point a minute. A plain
Battery alarm at 30 % fires on the first burst; Battery (est) at 30 % should
fire only once the resting level is actually there, about seven minutes in.

**Voice commands.** Hold the Voice button on the dashboard and pick Listen
for voice command, then ask for a metric: "battery", "controller temp", "max
speed", "what can I say". Bind it to a Flic, the volume keys, your watch or
the HUD if you would rather not hold anything, or put the Listen tile on the
dashboard for a one tap start.

Four things worth pushing on. Settings, Voice, Cue when listening: Off should
leave the session completely silent at both ends, which is what a headset with
its own tone needs. When not understood: Off and Beep should replace the "I
did not catch that" sentence without slowing the session down. Command
language lets you speak one language while the app is in another, so try the
app in English with a Russian voice and Russian commands. And Headset voice
button, off by default, makes your headset's voice button reach the app:
Android will ask which app should answer it the first time you press it.

Say "voice off" to stop the periodic announcements mid-ride, "voice on" to
bring them back, and "voice report" for one now. Five new report items too,
all off until you switch them on in Customize voice report: Battery (est),
Range, Voltage, Odometer and Consumption.

**Speed splits button.** Settings, Dashboard layout, drag the Speed splits
action onto a slot. Each tap walks Splits off, accel, brake, both, and the tile
says which; it works with no wheel connected. Off pauses: the session's best
and last times stay, and Settings, Voice, Speed splits now lists them with a
Reset. A different wheel connecting clears them; a reconnect of the same wheel
does not.

**Pressure units.** Pick psi, bar, kPa, kgf/cm2 or MPa and check every screen
agrees: the tile, its graph, the alarm threshold, the settings row and the
overlay.

These three are in the Play beta as 0.20.3 (272) as well, so a rider who does
not want a CI build can test the same things from there.

## Reporting back

Say which wheel, which firmware and what you expected instead. A trip
recording is worth more than a description, and if it crashed, Share crash log
on the About screen.

---

Each branch keeps its own BRANCH.md, and this one changes as the work does.
