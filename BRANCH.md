# HUD: the hotspot card stops claiming a fact it cannot check

Fixes the false "Phone hotspot is off" a tester saw on 2026-09-18, with the
hotspot plainly on and the HUD already connected to it.

**HUD APK: unchanged. Phone side only.**

## The problem

The card in Settings, Integration, HUD asserted a state it had no way to read.
`detectHotspotEnabled` tried two things and trusted whatever came back first:

1. Reflection into `WifiManager.isWifiApEnabled()`.
2. Failing that, looking for an interface named `ap*`, `softap*`, `swlan*` or
   `wlan1`, and returning false when it found none.

Both are wrong in the same way, and the doc comment admitted the second one:
"false when nothing answered (so the UI defaults to surfacing the hint)". Not
finding something is not the same fact as it being absent, and only one of
those is safe to print.

Stage 1 is the one that actually bit him, and it is worse than being blocked.
Querying AP state is privileged, so for an ordinary app the call returns false
whether the hotspot is off OR we are simply not allowed to know. Reproduced on
an emulator: it returns false with no hidden-API denial logged at all, so the
card stated "Phone hotspot is off" with complete confidence and no way for the
rider to argue.

Cosmetic. The card never affected the link, which is why the same report says
the connection itself recovered quickly.

## What changed

**Only a positive counts now.** `isWifiApEnabled()` returning true means the
hotspot is up, and an `ap*`-style interface existing means the same. Neither
returning anything else means it is off, so neither is allowed to say so.

That leaves two states the card can honestly be in, and "off" is not one of
them, so `hud_hotspot_off_hint` is gone from all 23 string files:

- confirmed on: "Phone hotspot is broadcasting. Connect the HUD to this phone's
  WiFi to enable the link." (unchanged)
- cannot tell: "Make sure the HUD and the phone are on the same WiFi network.
  If you use the phone hotspot, turn it on first."

The second one is new (`hud_hotspot_unknown_hint`), translated into all 22
locales. It keeps the half of the old copy that was true in every state, so a
rider setting the HUD up for the first time still gets told what to do.

Deliberately not "Detecting..." or anything else transitional. On a modern
phone this never resolves, so a transitional label would sit there forever and
read as broken rather than as honest.

## Verified

Emulator, API 36, no hotspot, walking the real UI to Settings, Integration,
HUD:

- before: "Phone hotspot is off. Make sure the HUD and the phone are on the
  same WiFi network."
- after: "Make sure the HUD and the phone are on the same WiFi network. If you
  use the phone hotspot, turn it on first."

The text flipped exactly when the code stopped trusting a false from
`isWifiApEnabled()`, which is also the proof that stage 1 was the one answering
wrongly rather than being blocked.

`:app:lintDebug` runs with `MissingTranslation` and `ExtraTranslation` as
errors, so the build itself checks that the new key reached every locale and
that the removed one left cleanly.
