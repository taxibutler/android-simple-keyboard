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

## Existing issues to fix first

1. **Missing `break` in switch statement** — `KeyboardSettingsReceiver.java` line 30: the `ACTION_CHANGE_THEME` case falls through to `default`. Harmless now since `default` does nothing, but will cause bugs when `CHANGE_LOCALE` and `RESET_LOCALE` cases are added.
2. **Misleading constant name** — `KeyboardSettingsReceiver.SURFACE_VARIANT` holds the value `"primary_color"`. The constant name should match the actual extra key to avoid confusion when adding more extras.
3. **Duplicated constants in tb-plus** — `KeyboardConstants.kt` is copy-pasted in both `app-plus` and `app-kiosk`. Should be moved to the shared `domain` module before adding locale-related constants.

## Key files to modify (keyboard app)

| File | Change |
|---|---|
| `KeyboardSettingsReceiver.java` | Handle new actions/extras, fix switch fallthrough |
| `Settings.java` | New pref keys for `external_locale` / `active_locale` |
| `RichInputMethodManager.java` | Method to switch subtype programmatically by locale string |
| `SubtypeLocaleUtils.java` | Helper to find subtype by locale (may already exist) |
| `MainKeyboardView.java` | Dropdown popup trigger + rendering |
| `rows_*.xml` (all variants) | Add locale-switch button to bottom row |
| `key_styles_common.xml` | Define the new key style |
| `AndroidManifest.xml` | Register new intent actions |

## Required changes in tb-plus

The `tb-plus` project (`custom-keyboard-features` branch) currently only sends `CHANGE_THEME` broadcasts. The following changes are needed to support the locale features:

### Move keyboard constants to shared module

- Move `KeyboardConstants.kt` from `app-plus` and `app-kiosk` into the `domain` module.
- Add new constants for broadcast actions and extras:
  - `ACTION_CHANGE_LOCALE = "rkr.simplekeyboard.CHANGE_LOCALE"`
  - `ACTION_RESET_LOCALE = "rkr.simplekeyboard.RESET_LOCALE"`
  - `EXTRA_LOCALE = "locale"`
  - `KEYBOARD_PACKAGE = "rkr.simplekeyboard.inputmethod"`
- The existing theme constants (`KEYBOARD_THEME_LIGHT`, `KEYBOARD_THEME_DARK`) and the hardcoded package/action strings in `ThemeViewModel` should also use these shared constants.

### Add locale broadcast methods

Add methods to send locale broadcasts, either in `ThemeViewModel` (renamed to something like `KeyboardViewModel`) or in a new dedicated class. Following the existing `changeKeyboardTheme()` pattern:

- `changeKeyboardLocale(context, locale: String)` — sends `CHANGE_LOCALE` broadcast with `locale` extra.
- `resetKeyboardLocale(context)` — sends `RESET_LOCALE` broadcast (no extras needed).

These need to be called from both `app-plus` and `app-kiosk`, so the implementation should live in the `domain` module.

### Locale source

The locale value comes from the remote config that tb-plus receives:
- On app start
- When the user presses "Reload Config"
- Periodically on refresh

**TBD:** The specific config field to use for the locale has not been decided yet. Update this section when the field name is known.

### Integration points

- **On config load / reload:** Send `CHANGE_LOCALE` with the locale value from the config.
- **On session end / booking completion:** Send `RESET_LOCALE` to discard any user locale preference and revert to the config default.
- The exact trigger points depend on the app's booking/session lifecycle — to be determined during implementation.

## Open items (waiting on designer)

- Exact button placement (bottom-left vs bottom-right corner)
- Button appearance (icon, text label like "EN", or both)
- Dropdown styling and size
- Whether the dropdown groups layouts by script/region or shows a flat list
