# Calorie Tracker

A personal Android calorie, nutrition and weight tracker. No account, no login, no cloud, no ads, no analytics. Everything lives in a Room database on the phone; the internet is only touched when you search a food or scan a barcode.

Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, CameraX + ML Kit, Retrofit and Glance.

---

## Getting an APK without installing anything

Push this repo to GitHub and Actions builds the APK for you — no Android Studio, no SDK, no JDK on your machine.

| Workflow | Trigger | Output |
|---|---|---|
| `.github/workflows/build.yml` | every push, or **Actions → Build debug APK → Run workflow** | debug APK as a downloadable artifact (kept 30 days) |
| `.github/workflows/release.yml` | pushing a tag like `v1.0` | signed release APK attached to a GitHub Release |

```bash
git remote add origin https://github.com/<you>/CalorieTracker.git
git push -u origin main
```

Then **Actions** tab → newest run → **Artifacts** → `CalorieTracker-debug-apk`. Unzip on your phone and open the APK (Android will ask you to allow "install unknown apps" for your browser or file manager the first time).

For a release build you can update over the top later, add these repository secrets under **Settings → Secrets and variables → Actions**, then push a tag:

```
KEYSTORE_BASE64      base64 -i my-release.jks | pbcopy
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

Without them the release job signs with a generated debug key — still installable, but switching to a real key later needs an uninstall first.

Make the repo **private** unless you want the code public. Nothing in it contains personal data or secrets (`local.properties` is git-ignored), but there's no reason for it to be public either.

---

## Opening the project

1. Open the `CalorieTracker` folder in **Android Studio Ladybug (2024.2) or newer**.
2. Android Studio will generate the Gradle wrapper JAR and write `sdk.dir` into `local.properties` on first sync. (If you prefer the command line and have Gradle 8.9+ installed: `gradle wrapper` once, then `./gradlew assembleDebug`.)
3. Let it sync, then **Run**.

Requirements: JDK 17, Android SDK 35, minimum device Android 8.0 (API 26).

No API key is needed. Open Food Facts — the default food database — requires no account and no key.

### Optional: USDA FoodData Central

Copy `local.properties.example` to `local.properties` and add a free key if you want better coverage of generic whole foods. The provider stays switched off until a key is present, and there's an explanation of the client-side-key trade-off in that file and in **Settings › Food database**.

---

## What's in the app

**Home** — greeting, week strip, calorie ring, macro dials, water and weight cards, and one card per meal. Swipe an entry left to delete (with undo). `+ Add food` opens a sheet with search, scan, favourites, recent, my foods, recipes, quick add and create-custom.

**Meals** — your library: favourites, custom foods, recipes and recently logged, each with a one-tap log button.

**Progress** — weight (line chart with goal line, start/current/goal, weekly and monthly change), nutrition (calorie bars against target, averages, highest/lowest day, logging streak) and water, over 7 days to all time.

**History** — a month calendar colour-coded green / amber / red against your calorie target, with the selected day's totals, macros, water, weight and every food logged.

**Settings** — profile and the Mifflin-St Jeor calculator, manually overridable goals, metric/imperial, reminders, theme and accent, food database providers, export/import, and a plain-English privacy and permissions page.

**Widgets** — six of them: Calories, Weight, Water (with a +250 ml button that doesn't open the app), Nutrition, Quick Add (Food / Water / Weight / Scan) and Daily Summary. All resizable, all configurable (theme, accent, compact mode, transparency) per instance.

---

## Architecture

```
Compose UI
    ↓
ViewModel  (StateFlow, one immutable UI state per screen)
    ↓
Repository (Diary, Food, Weight, Water, Recipe, Stats, Backup)
    ↓
FoodProviderRegistry ──► FoodDataProvider
    ↓                       ├── LocalFoodDatabaseProvider   (always, offline)
Room + DataStore            ├── OpenFoodFactsProvider       (no key)
                            └── UsdaProvider                (optional key)
```

**Adding a food database** is one file and one line: implement `FoodDataProvider`, add it to the `remoteProviders` list in `di/AppContainer.kt`. Nothing in the UI, ViewModels or DAOs changes, and it shows up in Settings automatically.

**Dependency injection** is a hand-written `AppContainer` (~60 lines) hanging off the `Application`. For a single-process personal app that's less machinery than Hilt and every wiring decision is visible in one place.

### Key decisions worth knowing

- **Diary rows store absolute nutrition.** Editing a food later never rewrites history.
- **Remote results are cached into the local catalog on first sight**, so the second lookup of the same food is instant and works in flight mode.
- **Settings are one JSON blob in DataStore.** Writes are atomic and adding a setting never needs a migration.
- **Reminders use inexact alarms** (`setAndAllowWhileIdle`), so the app never asks for `SCHEDULE_EXACT_ALARM`. A nudge that lands within a few minutes of 13:30 is fine.
- **Midnight rollover is an alarm, not a service.** Nothing is deleted — each day simply has its own rows and the widgets refresh.
- **Widgets update on write**, from the midnight alarm and from a weekly `WorkManager` job. No polling, no foreground service.
- **Charts are drawn on a Compose `Canvas`** — no charting dependency, and they handle single points, flat series and tiny ranges without looking dramatic.

### Layout

```
app/src/main/java/com/satya/calorietracker/
├── CalorieTrackerApp.kt        Application + AppContainer accessor
├── MainActivity.kt             single activity, handles widget/notification intents
├── di/AppContainer.kt          all wiring
├── domain/                     models, unit conversion, goal calculator
├── data/
│   ├── db/                     entities, DAOs, mappers, AppDatabase
│   ├── prefs/                  UserPreferences, reminders, DataStore
│   ├── remote/                 FoodDataProvider + OFF / USDA / local
│   ├── repository/             Diary, Food, Weight, Water, Recipe, Stats
│   ├── backup/                 JSON + CSV export, JSON import
│   └── seed/SeedFoods.kt       ~100 offline foods incl. Indian staples
├── ui/
│   ├── theme/  components/     design system, rings, bars, charts
│   ├── home/ meals/ progress/ history/ settings/
│   ├── addfood/                search, portion editor, quick add, custom food
│   ├── recipe/  scanner/
│   └── navigation/             routes + NavHost
├── widget/                     6 Glance widgets + config activity
├── notifications/              reminders, daily reset, boot receiver
└── work/                       weekly maintenance worker
```

---

## Privacy and permissions

| Permission | Why | When |
|---|---|---|
| `CAMERA` | Read barcodes. Frames are analysed on-device and never stored or transmitted. | First time you tap Scan |
| `POST_NOTIFICATIONS` | Show reminders. | First time you enable a reminder |
| `INTERNET` | Ask Open Food Facts about a search term or barcode. | Only during a search or scan |
| `ACCESS_NETWORK_STATE` | Tell "offline" apart from "the API is down" so the error message is honest. | — |
| `RECEIVE_BOOT_COMPLETED` | Re-arm reminders after a reboot. | — |

Not requested: location, contacts, microphone, storage, phone. No advertising or analytics SDK is present in the dependency list — you can check `app/build.gradle.kts`.

Deny any permission and the rest of the app is unaffected. Search sends only what you typed; a barcode lookup sends only the barcode number.

---

## Backup

**Settings › Data › Export as JSON** writes a complete, human-readable backup wherever you point the system file picker. **Import** restores it, with a merge-or-replace choice. CSV export is also available for spreadsheets (food log, weight, water and your custom foods in one file); it is a read-only format — import expects the JSON.

Nothing is uploaded anywhere, ever.

---

## Offline behaviour

Works with no connection at all: today's log, adding saved / custom / recent / favourite foods, quick-add calories, weight, water, history, statistics, widgets and reminders. A previously scanned barcode resolves from cache.

Needs a connection only for: new text searches and barcodes the phone hasn't seen before. When one fails you get a specific message ("You're offline — showing foods saved on this phone") and a one-tap route to adding the food manually, which then saves it for next time.

---

## Testing the full flow

Install → set goals in Settings › Profile → search a food → add it → scan a barcode → add the scanned food → tap +250 ml water → log your weight → check the dashboard → open History and pick a day → open Progress and switch ranges → turn on a reminder → long-press the home screen and add each widget → resize one → turn on flight mode and log a saved food → export JSON → clear data → import it back.

---

## Known limitations

- Not compiled end-to-end before delivery (the environment it was written in had no Android SDK and no access to the Maven/Google artifact repositories). It is written against AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Glance 1.1.1, but expect to fix the odd import or signature on first sync rather than a clean first build.
- The Gradle wrapper JAR isn't included — Android Studio regenerates it on first sync.
- The bundled seed foods are rounded reference values from standard composition tables, not lab measurements. Fine for trends; edit any of them or create your own.
- Mass ↔ volume conversion assumes a density of 1 g/ml. Exact for water, close enough for milk and juice, wrong for oil — log oils by weight or by their own serving.
- Recipes store resolved nutrition, so editing an ingredient food later doesn't retroactively change saved recipes. That's deliberate, but it means you re-add an ingredient to pick up a correction.
