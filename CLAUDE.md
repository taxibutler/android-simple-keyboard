# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fork of [Simple Keyboard](https://github.com/nicehash/simple-keyboard) (AOSP LatinIME-based Android IME) customized for use as the system keyboard on Butler United POS/kiosk devices. Key customizations over upstream:
- **BroadcastReceiver for theme changes** (`KeyboardSettingsReceiver`) — the main Butler United app can change keyboard theme and surface color at runtime via `rkr.simplekeyboard.CHANGE_THEME` intent.
- **Number row for password fields** — when input type is password, a number row is shown at the top.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (minified with ProGuard)
./gradlew assembleRelease
```

Single-module project (`app`). No tests are configured. Requires JDK 17. Compile SDK 36, min SDK 23, target SDK 36.

## Architecture

Pure Java Android app — single module, no Kotlin, no Compose. Based on AOSP `InputMethodService`.

- **`latin/LatinIME.java`** — Main IME service. Handles input connection, keyboard lifecycle, key events, and settings changes. Entry point for the keyboard.
- **`keyboard/`** — Keyboard rendering and input handling. `KeyboardSwitcher` manages keyboard state transitions (alphabet/symbols/shift). `MainKeyboardView` is the custom `View` that draws keys and handles touch. `KeyboardLayoutSet` loads XML-defined layouts.
- **`keyboard/internal/`** — Keyboard XML parsing (`KeyboardBuilder`, `KeyboardRow`, `KeyboardParams`) and layout geometry.
- **`latin/inputlogic/`** — Text input processing (`InputLogic`) and non-text input handling.
- **`latin/settings/`** — `SettingsActivity` and preference fragments for appearance, key press behavior, themes, and language selection.
- **`event/`** — Key event model (`Event`, `InputTransaction`).
- **`compat/`** — Android API compatibility wrappers.

## Keyboard Layouts

Keyboard layouts are defined in XML resources under `res/xml/` (portrait) and `res/xml-land/` (landscape), with tablet variants in `res/xml-sw600dp/`. Each layout (e.g., `rows_qwerty.xml`, `rows_symbols.xml`) references key definitions from `res/xml/key_styles_common.xml`.

## Theming

Themes are managed by `KeyboardTheme.java`. The app supports system-following themes (Material You on API 31+, Material Design on API 24+) and a custom theme. Theme colors and drawables are split across `res/values/`, `res/values-v24/`, `res/values-v31/`, and `res/values-night*` resource qualifiers.

## External Integration

The `KeyboardSettingsReceiver` BroadcastReceiver (registered in AndroidManifest) allows external apps to change the keyboard theme by sending a broadcast with action `rkr.simplekeyboard.CHANGE_THEME` and extras `theme_id` (int) and `surface_variant` (int color).
