# Voice commands

Ask the wheel a question out loud and hear the answer. "What is my battery",
"PWM", "consumption", "last split".

Built. The sections below are the design as it was argued; **Built** and
**What is unverified** at the end record where it ended up, and where it differs.

## Why this is smaller than it looks

Most of it is already built, which is what shaped every decision below.

- **The answers exist.** `VoiceReportPlan` already speaks Speed, Battery,
  PhoneBattery, Temp, PWM, Current, Power, Distance, Recording, Time and
  Navigation, and already separates a periodic announcement from a
  button-triggered one. `VoiceService` is a full text-to-speech layer: voice and
  locale picking, audio focus, output channel.
- **The vocabulary exists, in every language.** `MetricCatalog` holds 52
  metrics, each with a stable key and a localised name, and there are 78 metric
  label strings translated into all 23 locales. The abbreviations on the
  dashboard tiles are the renderer shortening them for space; the catalog
  strings underneath are the full words a rider would say: Consumption,
  Verbrauch, Расход, 電費.
- **The dispatch exists.** `ActionCatalog` is the single source of truth for
  rider-triggerable commands, and it already declares `ActionSurface.VOICE`,
  commented "Future: voice command shortcut".
- **The permission exists.** `RECORD_AUDIO` is already declared, for Studio
  video.

So the new parts are only: opening the microphone, turning what was said into a
catalog key, and deciding when to listen.

## How a rider asks

A press, not a wake word.

Android has no public always-on hotword API for an ordinary app:
`AlwaysOnHotwordDetector` needs the system assistant role. Doing it ourselves
means holding the microphone open for the whole ride, which blocks Studio
recording (the same microphone), shows a permanent microphone indicator on
Android 14 and later, invites store scrutiny for continuous audio, and costs
battery on a device already running GPS, BLE and a screen. Not worth it for a
feature whose answer is two seconds long.

So listening is one new action in `ActionCatalog`, which makes it bindable
everywhere the catalog already reaches: Flic, volume keys, the watch, the HUD,
and a dashboard tile. A press opens the microphone for a few seconds, and the
window stays open briefly after an answer so a follow-up needs no second press.

### The dashboard tile

The voice tile is a pair that shares one slot and one icon. Tapping it fires
whichever mode the rider has chosen. Holding it opens the existing
`ActionGroupPopover` with both modes and a switch:

```
Listen for voice command
Voice report
Switch to voice command        (or: Switch to voice report)
```

Both modes stay one hold away whichever is selected, so the switch only decides
what a tap does and what the eyes-free surfaces bind. Switching rewrites one
entry in `dashboardActionOrder`, which is already a plain ordered list of action
keys, so there is no new persistence and the change is per slot rather than
global.

### What the rider sees and hears

No dialog. A dialog is wrong for the surface that matters: fired from a Flic or
a volume key the rider is moving and cannot look at the screen, and a modal is
something they could leave open at speed.

Instead, the same path for every trigger: a short tone to say the microphone is
open, the tile itself showing a listening state, and the live transcript in a
snackbar pill, which is what rule 3 already asks for transients. Eyes-free
triggers lean on the tone and never need the pill. The answer is spoken.

The listening prompt is fixed wording with a choice of style: beep, voice or
nothing, as a segmented row like the "Announce: Always / Connected / When
riding" row the section already has. Beep is the default, for a practical
reason: a spoken prompt can bleed into the microphone and the recogniser hears
its own prompt. A tone is shorter, cuts through wind better, and the microphone
opens sooner.

There is deliberately no second setting for a confirmation sound. The answer is
the confirmation.

## What a rider can ask

Three families, all generated from what the app already has, none of it
hand-maintained:

- **Any of the 52 metrics**, by the name in the rider's own language.
- **Splits.** `AccelSplitVoice.splitText()` already renders one into speech, so
  "last split" only has to choose which.
- **Session state**, the 11 `VoiceReportPlan` reports, which are also the right
  answer to a vague question.

Because the list is generated, the "What can I say" viewer in settings cannot
drift from what actually works, which is what rule 10 asks of any preview
surface.

### When names collide

52 names spoken into wind, and several are near neighbours: there are three
temperatures (motor, controller, battery) and two speed limits. "Temperature"
alone is ambiguous.

The rider's own dashboard is the disambiguator. If exactly one temperature is on
their tiles, that is the one they meant. If several are, or none is, fall back
to the general Temp report that `VoiceReportPlan` already has. The same rule
covers every ambiguous word, and it needs no configuration.

### When it cannot answer

Every answerable thing declares its own availability, and an unavailable answer
says what to do about it. Four reasons, four sentences:

- Off in settings: "Acceleration splits are off. You can turn them on in Voice
  settings."
- Not supported by this wheel: "Your V8S does not report tyre pressure."
- No data yet: "No GPS fix yet."
- Needs setup: "I need your pack size for range. It is in Wheel parameters."

A rider who gets a useful refusal learns the app. One who gets silence assumes
it is broken, which is exactly what the blank CONSUMPTION tile taught us.

A no-match teaches too: not a bare "I did not catch that" but "I did not catch
that. Try battery, or consumption."

## Questions only

This version answers and does not act. Nothing it can mishear changes what the
wheel does, so a misrecognition costs a wrong answer and nothing more. Control
actions are a separate decision, taken once recognition is proven in wind rather
than at the same time.

## Listening

The system `SpeechRecognizer`, not an embedded model.

It takes whichever microphone the system has routed, so speaking to the phone
and speaking through earbuds are the same code path, and a rider who pulls their
earbuds out mid-ride keeps working without touching a setting. On API 31 and
later `createOnDeviceSpeechRecognizer` runs offline, which matters because
riders lose signal constantly. The intended source is `VOICE_RECOGNITION`, the
one Android tunes for speech noise suppression.

The alternative, raw `AudioRecord` with an embedded model such as Vosk, would
give control of the microphone and the audio source and would work on minSdk 29
too, at the cost of shipping tens of megabytes of model per language. Worth
revisiting only if wind proves fatal.

Two things to be honest about in the settings row rather than mid-ride:

- On API 29 and 30 there is no on-device recogniser, so it falls back to the
  networked one and needs data.
- A classic Bluetooth earbud microphone needs an SCO handshake that briefly
  drops music to call quality and costs about a second. The phone microphone
  avoids it, so automatic means the phone microphone unless a rider asks
  otherwise. That setting is not in this version.

## Settings

No new top-level section. The inventory is one toggle, one segmented row,
possibly one number and a viewer, which would sit nearly empty beside a section
that already has a dozen rows. It goes at the end of Voice and announcements,
after the existing voice settings, under its own heading:

- Voice commands on or off
- Listening prompt: beep, voice or nothing
- Listen window, and whether it stays open for a follow-up
- What can I say, a generated viewer rather than a setting

Which also means the section keeps its name.

## Interactions to respect

- **Legal Mode Lockdown** silences `announceEvent` and `announceTrigger` by
  design. Listening is disabled entirely while armed: the point of that mode is
  that no surface reaches past it.
- **Studio video recording** owns the microphone. The two cannot both have it,
  and the refusal has to be spoken rather than silent.
- **Media** is already paused and audio focus already taken for speech.
  Listening ducks rather than fights.

## Testing

The matcher is a pure Kotlin class with no Android in it, in the shape
`RideEfficiency` and `ChargeRiseDetector` already use, so a phrase in and a
catalog key out is a plain unit test: exact names, the ambiguous-temperature
rule, near misses, and every unavailable reason.

Rule 13 asks every registry for a drift guard, so one test walks `MetricCatalog`
and asserts every metric is reachable by its own localised name. A metric added
later without a spoken form fails that test rather than going quietly missing.

## Built

Everything below is on the branch and running. Fourteen commits, 1236 unit
tests.

Three things only showed up once it ran on a device, which is the argument for
having made the What can I say list tappable rather than leaving it a list:

- **Every report-backed question answered "no data yet".** Speed, Battery,
  Amps, Power, PWM, Temp, Trip and the report-only ones like Time. Their
  reading came back null so the report could phrase them later, but null also
  means nothing is known, so the answer became Unavailable and the branch that
  was meant to rescue them only ran for a Say it could never receive. The
  sentence is now fetched up front through `VoiceService.reportText`, and
  carried as its own answer kind so it is spoken whole: a report already says
  its own name.
- **The fallback never fell back.** `start()` refuses while the state says
  Listening, which it does right up to the error that triggers the fallback, so
  the recursive call returned immediately after logging its intention.
- **The language tag was `en_US`, not `en-US`.** A recogniser does not throw on
  that, it quietly uses the device language, so a Spanish rider asking for
  consumo would have been listened to in English.

One design change came out of use rather than out of the plan: a rider says
"controller temp", not "controller temperature". Names are matched word by word
with the end of a word allowed to be missing and the order free, so
"estimated battery" finds the estimate rather than plain battery. Deliberately
a rule and not a list of phrasings, so a metric added later inherits it from its
label with nothing to keep in sync and nothing new to translate. Spelling
variants (tyre/tire) are the part a rule cannot reach; they would need a
per-locale synonym list, which is real upkeep for an additive gain, so they wait
until a rider reports a miss.

The What can I say list is tappable: each name speaks its real answer, from the
rider's own wheel, units and language. Rule 10 asks a preview to show the real
configuration, and this is the real path with only the acoustics left out.

## Verified

On device, across four emulators and two languages:

- Every answer route: a report-backed metric, a report with no metric behind
  it, a plain metric with its unit, and a genuine no-data refusal.
- German end to end, including the report route that was broken:
  "Akku 48 Prozent", "Fahrt 0.0 Meilen", "Drehmoment, 0,0Nm" with the decimal
  comma.
- The microphone genuinely opening, confirmed by Android's own privacy
  indicator, and the on-device to network fallback firing on a device with no
  language pack.
- The not-understood answer offering two examples taken from the rider's own
  tiles.

## What is unverified

**A person speaking to it.** Not for want of trying: no emulator on the build
machine has a working recognition engine at all. The default on-device one
reports a missing language pack, and the only alternative reports
SERVER_DISCONNECTED, both regardless of audio routing - which was itself ruled
out by booting with `-allow-host-audio` and confirming the guest input threads
were live. It wants a real phone, which is a few minutes of work and not a risk
to the design.

Whether a phone microphone is usable at 30 km/h. Wind is broadband and brutal,
and no amount of design settles it. It wants fifteen minutes on a real wheel,
trying the phone in a pocket, the phone in hand, and earbuds. If the phone
microphone turns out to be hopeless at speed the feature still stands up, it
just becomes an earbuds feature in practice, and the settings should say so.
