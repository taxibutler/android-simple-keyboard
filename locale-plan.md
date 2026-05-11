# Locale Layout Switching — Feature Plan

## Overview

Three related capabilities:
- **A.** External apps set the keyboard locale/layout via broadcast
- **B.** User can manually switch layout via a dropdown button on the keyboard
- **C.** External apps can reset the layout back to the externally-set locale (discarding user preference)

## A. Receive locale via broadcast

**New broadcast action:** `rkr.simplekeyboard.CHANGE_LOCALE`

- **Extra:** `locale` (String, e.g. `"de"`, `"ar"`, `"fr"`)
- On receive: look up the locale in `SubtypeLocaleUtils` to find the matching subtype/layout. If no match, fall back to QWERTY (`en_US`).
- Save two things to SharedPreferences:
  - `external_locale` — the locale received from the broadcast (the "default" for reset purposes)
  - `active_locale` — also set to the same value (what the keyboard currently uses)
- Immediately switch the active keyboard layout via `RichInputMethodManager` without requiring user interaction.

## B. Layout-switch button with dropdown

**UI:** A small button in the bottom corner of the keyboard. Always visible regardless of whether an external locale param has been received.

- On tap: show a `ListPopupWindow` anchored to the button, listing all available locales/layouts from `SubtypeLocaleUtils`.
- Each item shows the locale display name (e.g. "English (US)", "Deutsch", "العربية").
- On selection: switch the keyboard to that layout immediately and save the choice as `active_locale` in SharedPreferences. The `external_locale` value stays unchanged.
- The selected layout persists across app restarts and reboots until a reset broadcast is received.

**Implementation approach:**
- Add a new key to the bottom row of keyboard layouts (all `rows_*.xml` variants, plus landscape/tablet).
- The button shows the current locale's short code (e.g. "EN", "DE", "AR") so the user knows what's active.
- `MainKeyboardView` handles the tap and shows the popup.

## C. Reset broadcast

**New broadcast action:** `rkr.simplekeyboard.RESET_LOCALE`

- On receive: read `external_locale` from SharedPreferences, set `active_locale` to that value, and switch the keyboard layout immediately.
- If no `external_locale` was ever set, fall back to QWERTY.
- Discards whatever the user picked via the dropdown.

## State diagram

```
App sends CHANGE_LOCALE("de")
  → external_locale = "de", active_locale = "de"
  → keyboard shows German QWERTZ

User taps button, picks "ar" (Arabic)
  → external_locale = "de" (unchanged), active_locale = "ar"
  → keyboard shows Arabic layout

App sends RESET_LOCALE
  → active_locale = "de" (copied from external_locale)
  → keyboard shows German QWERTZ again
```

## Key files to modify

| File | Change |
|---|---|
| `KeyboardSettingsReceiver.java` | Handle new actions/extras |
| `Settings.java` | New pref keys for `external_locale` / `active_locale` |
| `RichInputMethodManager.java` | Method to switch subtype programmatically by locale string |
| `SubtypeLocaleUtils.java` | Helper to find subtype by locale (may already exist) |
| `MainKeyboardView.java` | Dropdown popup trigger + rendering |
| `rows_*.xml` (all variants) | Add locale-switch button to bottom row |
| `key_styles_common.xml` | Define the new key style |
| `AndroidManifest.xml` | Register new intent actions |

## Open items (waiting on designer)

- Exact button placement (bottom-left vs bottom-right corner)
- Button appearance (icon, text label like "EN", or both)
- Dropdown styling and size
- Whether the dropdown groups layouts by script/region or shows a flat list
