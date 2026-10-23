# Pet Food Calculator (Android, Kotlin + Jetpack Compose)

Daily calories and exact food portions (cups, grams, cans) for dogs and cats by breed, age, weight, body condition and activity.

- Package: `com.superdevlabs.petfood.portioncalc`
- AGP 9.4.0 (built-in Kotlin), Gradle 9.6.0, JDK 21, compileSdk 37, targetSdk 36, minSdk 26, KSP (Room)

## Features
- Pet profiles (dogs & cats, 66 breeds) with real breed photos, or your own photo via the system photo picker
- Calorie engine: RER = 70 × kg^0.75 × life-stage/neuter/activity factor (WSAVA/NRC); weight loss/gain uses target weight
- Portions per day and per meal for one or two foods (wet + dry mix by calories), treat budget, water estimate
- Food library with typical presets and custom foods entered straight from the label (kcal/kg, /100 g, /cup, /can)
- Daily feeding checklist, meal-time reminders (inexact alarms, survive reboot)
- Weight tracker with trend chart; food switch planner (7 or 10 days)
- Breed guide with Wikipedia photo, intro and calorie table (source credited)
- Animated splash, exit dialog (Exit / Rate us / Cancel), kg/lb setting, in-app privacy policy

## Ads (AdMob)
- Consent via Google UMP before any ad request; privacy options entry in Settings
- Banner: first failure retries immediately; after 2 failed requests every retry waits 15 s
- Interstitial only after major actions (save pet, save food, create switch plan), max 1 per 30 s
- Native ads in free gaps (Home, Plan, Food library, Breed guide, Food switch, Exit dialog)
- Debug builds use Google test IDs; release builds use the live IDs

## Build
Open in Android Studio and run, or `./gradlew assembleRelease` (add your signing config first).
Unit tests for the calculator and ad policies: `./gradlew testDebugUnitTest`.
