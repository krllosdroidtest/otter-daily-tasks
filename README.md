# otter

**otter** is a 100% offline Android personal organizer built with Kotlin and Jetpack Compose. It combines task scheduling with full-screen alarms, folder-organized shopping lists, and a monthly expense tracker with installments, recurring bills, and visual spending summaries—keeping all user data strictly on-device.

## About the Project

This entire app was created through **Vibe coding** in **Google AI Studio**. The creator has no coding background whatsoever—this project was simply built to bring an idea to life for personal everyday needs and shared in case it helps someone else too.

## Features

- **Tasks & Calendar**
  - Interactive monthly/collapsed calendar with daily badges and future recurrence projections (`Daily`, `Weekly`, `Monthly`, `Yearly`).
  - Task editor with due date/time, reminder offsets, file/photo attachments, and priority alarm settings (`System`, `Custom Audio`, `Silent` + vibration).
  - Exact alarm scheduling (`AlarmManager`) with full-screen lock-screen alert UI, snooze/dismiss actions, and automatic rescheduling after device reboot.
  - Quick task rescheduling and filters (`Selected Day`, `All Pending`, `Completed`).

- **Shopping Lists**
  - Custom shopping folders with create, rename, and delete options.
  - Fast item creation, check-off tracking, and item counts per folder.

- **Expenses & Installments**
  - Calendar-linked expense tracking supporting **One-time**, **Installments**, and **Recurring** (`Daily`, `Weekly`, `Monthly`, `Yearly`) expenses.
  - Individual paid/unpaid toggles for installments and recurring occurrences, plus expandable installment schedules showing payment progress.
  - Monthly spending breakdown chart and totals across one-time, installment, and recurring expenses.
  - Persistent, shared category history with custom colors and icons available across all expense screens and filters.

- **Themes, Localization & Backups**
  - Material 3 UI with Light/Dark/System theme modes and multiple color palettes.
  - Automatic device language support (`English`, `Portuguese`, `Spanish`).
  - Local JSON backup export and import (merge or replace) without any cloud dependency.

## Privacy & Offline Architecture

- **No network access:** The app does not request `INTERNET` permission and performs zero external network calls or analytics tracking.
- **Local storage:** Structured data is stored on-device using **Room** (SQLite) and preferences via **Jetpack DataStore**.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material Design 3, Navigation Compose
- **Persistence:** Room (KSP), DataStore Preferences
- **Concurrency:** Kotlin Coroutines & StateFlow
- **Images:** Coil
- **Testing:** JUnit 4, Robolectric, Coroutines Test
- **Min / Target SDK:** Android 7.0 (API 24) / API 36

## Build & Test

Run unit and Robolectric tests:

```bash
gradle :app:testDebugUnitTest
```

Build the debug APK:

```bash
gradle :app:assembleDebug
```
