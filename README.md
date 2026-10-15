# Turbo Roni

Project author: Sandeep Pariyar
Copyright (c) 2026 Sandeep Pariyar

# Turbo Roni — Android source project

Native Kotlin + Jetpack Compose app for Roni. No external server, ads, or accounts.

## Open and build

1. Open this folder in Android Studio (recent version supporting Android Gradle Plugin 8.7.3 and JDK 17).
2. Allow Gradle to sync and download dependencies.
3. Connect an Android device or start an emulator (Android 8.0+, API 26+).
4. Run the `app` configuration. For APK: **Build > Build APK(s)**.

A Gradle wrapper is not included because Gradle was unavailable in the file-generation environment. Android Studio can use its installed Gradle or generate a wrapper. This project has not been compiled or tested here.

## Features

- Six built-in quests and custom quests
- Ready / Set / Go countdown before the clock starts
- Full-screen quest UI with animated circular progress and touch-reactive particle field
- Foreground-service-backed monotonic countdown with ongoing notification
- Pause / resume / finish; encouraging speech, 30-second tick, voice coach and three selectable tone families
- Best records, speed tiers, one badge per qualifying completion
- Ten-badge family reward redemption and configurable wish titles
- Stopwatch with laps
- Player name and five avatar choices
- Local SharedPreferences + JSON persistence

## Important limitations

- **No guarantee of permanent lock-screen digits:** Android lock-screen restrictions mean the countdown is primarily available in a persistent notification when locked. Opening the app shows the large digits.
- **Foreground service:** Android 14/15 imposes additional foreground-service rules. `specialUse` is declared with an explanation, but Google Play distribution may require policy review; verify on your target Android versions.
- **Background process death:** The countdown is monotonic while the service runs, but process death/reboot recovery is not implemented.
- **Sound:** Three packs are currently distinct tone families, not original produced music. They do not use copyrighted Singing Monsters recordings.
- **Profile photo:** Avatar emoji choices are implemented; custom image/photo picker is not yet implemented.
- **Reward confirmation:** A simple parent confirmation dialog is included, not a secure parental PIN.
- **No full test suite, no verified APK:** Source is provided for Android Studio compilation and device testing. Check edge cases before relying on it daily.
- **Eating/homework:** These default to completion-based badges to discourage unhealthy rushing.

## Main files

- `MainActivity.kt`: Compose UI, quest screens, rewards, stopwatch, settings
- `QuestService.kt`: countdown foreground service, notifications, audio/TTS
- `Model.kt`: quest model, records, badges, local storage

## Next recommended engineering tasks

1. Build and fix any version-specific compiler issues on your Android Studio setup.
2. Add tests for exact badge boundaries, clock pause/resume, and one-time rewards.
3. Add process-death recovery, explicit notification permissions guidance, and parent PIN.
4. Replace ToneGenerator cues with original musical sound packs and add audio focus handling.
5. Add persistent photo selection using Android Photo Picker.

## Version 2: Interactive clock + sound worlds

- Animated orbital particles inside the countdown. Tap or drag on the circle to create playful spark bursts.
- Cleaner active screen: countdown, mission name, personal best, Pause/Play, I'm Done.
- Five original synthesized sound worlds: Monster Choir, Arcade Rush, Jungle Beat, Classic Clock, Cartoon Boing.
- Twenty included WAV cues (tick, start, warning, finish per pack).
- Ticking options: final 30 seconds, entire mission, or final 60 seconds.
- Preview ticking sounds from Settings using the play control.

### Limitations
Not compiled or run on an Android device here. Android background audio and lock-screen behavior vary by device and OS version. SoundPool samples load asynchronously, so the first sound may not play immediately. Original generated audio is not sourced from existing games.

## Quest World illustrated home screen
- Static jungle hero artwork and independently transparent PNG quest illustrations under `app/src/main/res/drawable-nodpi/`.
- Large 82dp icons, two-column quest tiles, and static home-screen interactions.
- Continuous particle animation is reserved for the active timer screen; menus use only direct touch feedback.
- Original WAV sound packs are included under `res/raw`.
- Concept art is a reference-derived crop; standalone transparent icons are newly drawn for this package.
- Source project has not been compiled or verified on an Android device.
