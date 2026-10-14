# Sateenkaarikello: native Android app (Jetpack Compose)

**Date:** 2026-10-13
**Goal:** Rebuild Sateenkaarikello as a fully native Android app in Kotlin and Jetpack Compose, with no WebView and no web code. It ships as a signed APK that is installed by hand on the niece's phone.
**Target device:** an older, large-screen Samsung phone, not on Android 17.
**Non-goals:** Google Play, accounts, logins, analytics, ads, cloud sync, or any network access.

The web version (`sateenkaarikello.html`) and its spec (`docs/superpowers/specs/2026-10-13-sateenkaarikello-design.md`) serve as the **functional specification**. The app must match their behavior, Finnish wording, and visual identity. Where this plan does not say otherwise, the web version is the reference.

## 1. Versions (checked 2026-10-13)

Re-check each of these on the day the project is created.

- **Android Gradle Plugin:** 9.2.x (9.2.0 shipped in April 2026, with a 9.2.1 patch). It needs Gradle 9.4.1 or newer and JDK 17 or newer.
- **Kotlin:** AGP 9 has built-in Kotlin, so do **not** apply `org.jetbrains.kotlin.android`. AGP bundles Kotlin Gradle Plugin (KGP) 2.2.10 by default.
- **Compose compiler:** apply `org.jetbrains.kotlin.plugin.compose`. Its version must equal the KGP version. The current Compose docs use 2.3.21, so add KGP 2.3.21 to the top-level classpath and use the Compose plugin at 2.3.21. Check AGP 9.2's supported KGP range before pinning.
- **Compose:** UI and foundation 1.12.x are stable (August 2026), and Material3 1.4.0. Use the latest `androidx.compose:compose-bom`; the last release was on 26 August 2026, and the exact BOM number is on Google Maven. **Compose 1.12 requires `compileSdk = 37` and AGP 9.**
- **SDK levels:**
  - `compileSdk = 37` (required by Compose 1.12).
  - `targetSdk = 37`.
  - `minSdk = 26` (Android 8.0). The owner has confirmed that the phone runs at least Android 8. The exact version is unknown and will not be checked, so the app must work on every version from 8 upward.
  - `compileSdk` and `targetSdk` do not limit which phones can install the app; only `minSdk` does.
  - **No API-level-gated behavior in the core experience.** Anything newer than API 26 (predictive back, themed icons, splash attributes) must degrade gracefully, with version checks where needed.
- **Navigation 3:** `androidx.navigation3:navigation3-runtime` and `navigation3-ui`, version 1.1.3 stable.
- **Preferences DataStore:** latest 1.2.x stable. The release page is stale, so take the exact patch number from Google Maven.
- **Other libraries:** use the latest stable `activity-compose`, `lifecycle-viewmodel-compose`, and `lifecycle-runtime-compose`. These were not checked for this plan.
- **Android Studio:** a release whose compatibility table lists AGP 9.2.

## 2. Architecture

The app is a single `:app` module. It is small enough that multi-module overhead is not worth it, but it is split into packages with clear boundaries. The domain layer is pure Kotlin with no Android imports, so it can be unit-tested on the JVM.

```
fi.sateenkaarikello
  MainActivity.kt                 edge-to-edge, setContent { SateenkaariApp() }
  SateenkaariApp.kt               theme + Navigation 3 back stack + NavDisplay
  domain/                         pure Kotlin, 100% unit-tested
    ClockTime.kt                  minutes since midnight, period 720/1440, snapping, circular math
    FinnishTime.kt                timeWords, dayPart, digital, spoken24 (port of the web functions)
    Levels.kt                     5 levels, task generation, distractors, hints, star scoring
    Lessons.kt                    9 lessons as data + declarative demo steps
    Wardrobe.kt                   doll items, slots, unlock thresholds
  data/
    ProgressRepository.kt         Preferences DataStore, exposes Flow<Progress>
  audio/
    Sfx.kt                        synthesized tones (ok, bad, win, pop) via AudioTrack
    Speech.kt                     TextToSpeech wrapper, Finnish availability as StateFlow
  ui/
    theme/                        Color.kt, Type.kt, Shape.kt, Dimens.kt (all design tokens)
    components/                   BigMenuButton, ChipButton, PillButton, OptionButton,
                                  ProgressDots, StarRow, FeedbackBox, Confetti, TopBar
    clock/                        ClockFace (Canvas), ClockState, ClockMarks, drag handling
    doll/                         Doll (Canvas, layered), DollParts (path data)
    screens/                      Home, FreeClock, LessonList, Lesson, Levels, Game,
                                  Result, Challenge, Ask, Doll
```

**State:**
- `AppViewModel` owns `Progress` (collected from DataStore) and the settings.
- One ViewModel per stateful screen: `GameViewModel`, `ChallengeViewModel`, `LessonViewModel`, and `AskViewModel`.
- Screens are stateless composables that receive a UI state and callbacks.

**Navigation:** Navigation 3, with a back stack the app owns: `SnapshotStateList<Screen>`, where `Screen` is a sealed interface of `@Serializable` keys.
- System back and predictive back pop the stack automatically.
- The lesson flow that ends in "start the level" pushes `Lesson(queue, thenLevel = i)`. When the flow finishes, it replaces itself with `Game(i)`.

## 3. Domain layer (port from the web version)

Port line by line from the `<script>` in `sateenkaarikello.html`. Keep the same names in English-style Kotlin, and add KDoc in Finnish where the user reads the code.

- **`FinnishTime`**
  - `timeWords(total)` gives the 12 h spoken form, for example "viittä vaille puoli neljä".
  - `dayPart(total)` returns `name`, `adverb`, and `isSun`.
  - `digital(total, h24)` gives forms like "3.30" or "15.30".
  - `spoken24(total)` gives "viisitoista kolmekymmentä".
  - `fullWords(total, h24)` combines the words with the day part.
- **`ClockTime`** holds the circular arithmetic that is currently inline in `makeClock`:
  - `snap(raw, step, period)`
  - `circularDelta(a, b, period)`
  - `minuteDrag(raw, angleDeg, period)`, where the minute hand drives the hour hand
  - `hourDrag(raw, angleDeg, period)`, which picks the half of the day closest to the current time in 24 h mode
- **`Levels`:**
  - Level definitions: name, `step` (60, 30, 15, 5, 1), sample time, card color, and the minute-weighting function.
  - `makeTasks(level, h24, random)`: 10 unique times, alternating *Mitä kello on?* and *Laita kello*.
  - `optionsFor(t, level, h24, random)`: the correct time plus typical confusions. These are the hour ±1, *yli*/*vaille* mirrored, hands swapped, and in 24 h mode the opposite half of the day (70 %).
  - `hintRead`, `hintSet`, and the praise list, using the exact Finnish texts from the web version.
  - `stars(mistakes)`: 0–1 mistakes give 3 stars, 2–3 give 2, otherwise 1.
  - ~~Unlock rules: level *i* opens when level *i−1* has a star, and the Challenge opens when Vartit has a star.~~ **Changed 2026-10-14:** no unlock rules. All five levels and the Challenge can be played in any order, in both the web and the Android version.
  - Pass `Random` in as a parameter, so tests are deterministic.
- **`Lessons`:** lesson content becomes data, not code. Each page has a start time, an optional period and help flag, text paragraphs, initial marks, and a list of demo steps:
  ```kotlin
  sealed interface DemoStep {
      data class Wait(val ms: Long) : DemoStep
      data class Move(val to: Int, val ms: Int, val exact: Boolean = false) : DemoStep
      data class Highlight(val hand: Hand?) : DemoStep
      data class Under(val marks: List<Mark>) : DemoStep
      data class Over(val marks: List<Mark>) : DemoStep
      data class Say(val text: String) : DemoStep
  }
  sealed interface Mark {
      data class Sector(val fromMin: Int, val toMin: Int, val color: MarkColor) : Mark
      data class NumberGlow(val n: Int) : Mark
      data class TickGlow(val minute: Int) : Mark
      data class Bubble(val minute: Int, val text: String) : Mark
  }
  ```
  A small builder DSL (`demo { wait(700); highlight(Hand.HOUR) }`) keeps the lesson file readable. Loops, such as lighting the 12 numbers, are ordinary Kotlin loops inside the builder. Copy all 9 lessons, their quizzes, and their hints verbatim from the web version.
- **`Wardrobe`:** 18 items (3 free, 15 unlocked at 1–15 stars), 5 slots. The `head` and `extra` slots are optional.

## 4. Persistence

Use Preferences DataStore through `ProgressRepository`, with a single `Progress` data class:

- **Stars per level:** stored as five int keys.
- **Challenge best score**
- **Outfit:** one string key per slot. Empty means none.
- **Doll name**
- **Settings:** sound on/off, help on/off, and 12 h / 24 h.
- **Lessons:** completed lesson ids, and the levels whose lessons were already shown automatically (string sets).
- **Reset:** "Aloita alusta" clears progress but keeps the settings, as on the web.

## 5. UI

### Theme tokens (single source of truth)

All colors, sizes, and shapes come from `ui/theme`. Never hardcode them in screens. Values come from the web version's `:root`:

- **Surfaces:**
  - background `#FFF5FA` with a dot pattern `#FFE1EF`
  - surface `#FFFFFF`
  - line `#F1DDEA`
- **Text:** ink `#3A2453`, inkSoft `#76638C`
- **Hands:** hour `#E64980`, minute `#228BE6`
- **Gold:** gold `#FCC419`, goldDeep `#F59F00`
- **Feedback:** good `#2B8A3E` on `#E3F8E7`, try `#B35000` on `#FFF0E0`
- **Clock halves:** *yli* `#FFF3BF`, *vaille* `#DCEEFF`
- **Rainbow ring:** 12 colors. Rainbow-7 is used for confetti, progress dots, and hair and dress stripes.
- **Menu buttons** (fill and darker shadow):
  - Kellokoulu `#FFE8A3` / `#FCC419`
  - Vapaa kello `#BFE3FF` / `#74C0FC`
  - Pelaa `#FFD1E3` / `#F783AC`
  - Kysy siskolta `#C3F2CB` / `#69DB7C`
  - Nukke `#E5DBFF` / `#B197FC`
- **Level card colors:** `#FFE3E3`, `#FFE8CC`, `#FFF3BF`, `#D3F9D8`, `#D0EBFF`, and Challenge `#E5DBFF`.
- **Type:** Fredoka (display, weights 500, 600, 700) and Nunito (body, weights 600, 700, 800), bundled in `res/font`. Use either the variable TTFs with `FontVariation.Settings` (supported from API 26, which equals `minSdk`) or static TTF files, one per weight, whichever renders more reliably on the API 26 emulator. Both fonts are under the SIL Open Font License, so include `OFL.txt`. Downloadable Google Fonts are not used, because they need Play Services and network.
- **Light theme only:** the design is a single light look, as on the web. Set the status-bar and navigation-bar icons to dark explicitly.

**UI rules carried over from the project owner:**
- Never use `·`, `•`, or `|` as separators between words or values.
- Never add a colored vertical bar on the left edge of cards, rows, or panels. Show state with pills, color, icons, or a whole-card tint.
- All user-facing strings live in `strings.xml` in Finnish. The generated time phrases come from `FinnishTime`.

### Clock (`ui/clock`)

The clock is the heart of the app, so it gets the most care:

- **Drawing:** one `Canvas` in a 400×400 logical coordinate space, scaled to the available size. The drawing order is the same as the SVG:
  1. rainbow ring (12 arcs)
  2. face
  3. help shading (*yli*/*vaille* halves) and help labels
  4. under-marks
  5. ticks
  6. numbers (`TextMeasurer`, Fredoka)
  7. 24 h inner numbers when help is on
  8. sun or moon and the day-part label in 24 h mode
  9. hands with soft shadow and knobs when interactive
  10. center pin
  11. over-marks
- **`ClockState`:**
  - holds `value` (snapped), `raw` (animated `Float`), `period`, `step`, and `interactive`
  - `suspend fun animateTo(t, ms, exact)` uses an `Animatable` along the shortest circular path
- **Drag:** `Modifier.pointerInput` with `awaitEachGesture`.
  - On touch down, pick the hand with the same rule as the web: inner radius plus a small angle to the hour hand selects the hour hand; otherwise the closer angle wins.
  - Moving the minute hand drives the hour hand (`minuteDrag`). Moving the hour hand sets the time from its angle (`hourDrag`).
  - On release, snap with a 160 ms animation.
  - Consume the pointer events so the screen never scrolls while a hand is being dragged.
- **Highlight pulse:** an `rememberInfiniteTransition` animates the stroke width of the highlighted hand, while the other hand fades to 22 % alpha.
- **Accessibility:**
  - `contentDescription` is the full Finnish time phrase.
  - When interactive, add custom accessibility actions "Lisää 5 minuuttia" and "Vähennä 5 minuuttia" that use the current step.

### Doll (`ui/doll`)

The doll is drawn with `Canvas` in the web's 200×340 coordinate space. Reuse the exact SVG path strings from `dollSVG()` through Compose's `PathParser().parsePathString(d).toPath()`, so the doll looks identical to the web version. The layers come in the same order: wings, back hair, legs, shoes, neck, dress, arms and sleeves, head and face, bangs, head accessory, bag.

- **Patterns:** the dot and star dresses use a `clipPath` with the dress path, then draw the pattern. The rainbow dress and hair use stripes or a vertical gradient inside the clip.
- **Thumbnails:** the wardrobe tiles reuse the same composable with a crop window, for example the head area for hair.

### Screens

The screens match the web version one to one:

- **Home:**
  - rainbow title
  - current-time card (mini clock and words)
  - doll card with the star count
  - full-width Kellokoulu button above a 2×2 grid: Vapaa kello, Pelaa, Kysy siskolta, Nukke
  - footer with the 12 h / 24 h segmented control, sound toggle, and two-step reset
- **Kellokoulu:** the lesson list with "Osattu" pills. The 24 h lesson only appears in 24 h mode.
- **Lesson:**
  - layout: progress dots, clock, text, caption pill, buttons (Kuuntele, Näytä uudestaan, Edellinen, Seuraava / Kokeile itse)
  - quiz with hints
  - "Ohita ja pelaa" when the lesson was opened automatically before a level
  - the `LessonViewModel` runs the demo steps in a coroutine and cancels it on page change or when the screen is left
- **Levels:** cards with a mini clock, stars, a lock note, a **Pelaa** button and a **Katso opetus** button. The Challenge card is last.
- **Game:**
  - 10 tasks with progress dots and the help toggle
  - *Mitä kello on?* shows three options with the words and a digital pill
  - *Laita kello* shows the target words, "kello 15.30" in 24 h mode, and a Valmis button
  - hint after the first mistake, answer reveal after the second
  - confetti and a sound on success
- **Result:** animated stars, newly unlocked doll items, and buttons (Pelaa uudestaan, Seuraava taso, Pue nukke, Tasot).
- **Challenge:** a 60-second rainbow timer bar, multiple choice only, and the best score saved.
- **Ask (Kysy siskolta):** the big sister sets the clock, taps Kysy, and taps Näytä vastaus to reveal the answer with an optional Kuuntele.
- **Doll:** the large doll, name field, and wardrobe grouped by slot. Locked items show a lock and the stars needed.
- **Free clock (Vapaa kello):** a draggable clock with the words, digital time, Kuuntele, and Arvo aika.

**Layout for a large phone:**
- Portrait-locked. Use `BoxWithConstraints` to size the clock to `min(width − 32.dp, height × 0.42)`, so the clock and three answers fit one screen without scrolling.
- Edge-to-edge with `safeDrawing` insets.
- Touch targets are at least 48 dp.

**Confetti:** a `Canvas` particle system driven by `withFrameNanos`, using the same physics as the web. It is skipped when the system "remove animations" setting is on (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`). The same check shortens the clock animations.

## 6. Audio and speech

- **Sound effects:** synthesize the same tones as the web's WebAudio into short PCM buffers once at startup, and play them with `AudioTrack` in `MODE_STATIC`:
  - ok: 659, 784, 1047 Hz
  - bad: 330 to 262 Hz
  - win: a five-note arpeggio
  - pop: 880 Hz
  No audio files are needed. Sounds respect the in-app mute and stop in `onPause`.
- **Speech:** `TextToSpeech` with `Locale.forLanguageTag("fi-FI")` at rate 0.9.
  - `Speech.available` is true only when `setLanguage` returns `LANG_AVAILABLE` or better. When it is false, all Kuuntele buttons are hidden.
  - In 24 h mode, the spoken text is "Kello on puoli neljä iltapäivällä, eli kello viisitoista kolmekymmentä", the same as the web.
  - Samsung phones default to the Samsung TTS engine, which may lack Finnish (see section 9).

## 7. Testing

- **Unit tests (JUnit, JVM)** for the whole domain layer. Port the existing Node checks:
  - all 720 times give non-empty, unique `timeWords`
  - all 1,440 times give unique full phrases
  - the key expectations (`205` gives "viittä vaille puoli neljä", `930` gives "puoli neljä iltapäivällä", `spoken24(930)` gives "viisitoista kolmekymmentä")
  - `t` and `t + 720` always differ in `isSun`
  - `makeTasks` gives 10 unique times that are valid for the level step
  - `optionsFor` gives 3 distinct, step-valid options that include the correct one
  - star thresholds (unlock rules were removed on 2026-10-14)
  - `minuteDrag` and `hourDrag`: wrap-around at 12, day switching in 24 h mode, and snapping per step
- **Compose UI tests** with a fixed `Random`: play Tasatunnit start to finish (correct and wrong answers), open a lesson and pass its quiz, equip a doll item, and check that progress survives recreating the activity.
- **Screenshot check:** take Compose previews of the clock (12 h, 24 h, help on and off, with marks) and the doll in all outfits, then compare them by eye with the web version.
- **Emulators:** API 26 (the oldest supported level) and API 37, both in portrait at 360×640 dp and 412×915 dp. Every feature must work on API 26: drag, sounds, fonts, persistence, and speech when a Finnish voice exists.
- **On the Samsung:** offline install, portrait layout, drag feel, sounds, the Finnish voice, the back button and predictive back, and persistence after force-stop and reboot.
- **Static analysis:** run the owner's existing `lc` (ktlint, detekt, Android lint) and `sc` (semgrep, OWASP) scripts. The owner runs them; results land in `reports/`.

## 8. Signing and building

1. Create a private release keystore once:
   ```
   keytool -genkeypair -v -keystore sateenkaarikello.jks -alias sateenkaarikello -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Keep the keystore and passwords **outside the repository**, for example in `~/.gradle/gradle.properties`. Back the keystore up: every update must use the same key, or the phone refuses the update and the child's progress is lost on reinstall.
3. Enable R8 (`isMinifyEnabled = true`, `isShrinkResources = true`). The expected release APK size is roughly 3–6 MB, mostly fonts and Compose.
   ```
   ./gradlew :app:assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`.
4. For every update, raise `versionCode` and set `versionName`.

The manifest has **no permissions at all**: no `INTERNET`, and `allowBackup="false"`.

## 9. Installing on the Samsung

**Option A: USB with adb (recommended).**
```
adb install -r app-release.apk
```
`-r` updates in place and keeps progress. Google's developer verification keeps adb installs exempt.

**Option B: copy the file.**
1. Copy the APK to the phone.
2. Open it in My Files.
3. Allow "Install unknown apps" for My Files.
4. If Play Protect warns about an unrecognized app, choose to install anyway.

**Samsung specifics:**
- **Auto Blocker:** newer One UI versions have *Auto Blocker* under *Settings > Security and privacy*. When it is on, it blocks installs from outside official stores. Use adb, or switch Auto Blocker off for the install and back on afterwards.
- **Finnish voice:** the default Samsung TTS engine may lack Finnish. If so:
  1. Install *Speech Services by Google* from Play Store.
  2. Choose it under *Settings > General management > Text-to-speech > Preferred engine*.
  3. Download the Finnish voice there.
- **Device details:** the phone runs at least Android 8; the exact Android and One UI version are unknown. The app is built to work on every version from 8 upward (`minSdk` 26), so no check is needed before installing.

**Developer verification status:** enforcement began on 30 September 2026, but only for participating app stores in Brazil, Indonesia, Singapore, and Thailand. Global enforcement on certified devices is planned for 2027. A hand-installed APK in Finland is unaffected today, and adb stays exempt.

## 10. Work order and estimate

1. Project setup: versions, theme tokens, fonts, Navigation 3 skeleton with empty screens. About half a day.
2. Domain layer port with full unit tests. About half a day.
3. Clock: drawing, drag, animation, marks, 24 h, accessibility. About 1 day.
4. Game, Result, Levels, Challenge, Free clock, Ask. About 1 day.
5. Lessons: data, DSL, player, quiz, auto-open before a level. About half a day.
6. Doll and wardrobe. About half a day.
7. Audio and speech, persistence, home screen, polish, confetti. About half a day.
8. Signing, release build, install, and testing on the Samsung. About half a day.

**Total:** roughly 4–5 working days.

## 11. Decisions (no open questions)

These decisions are final. Do not ask the owner about them.

- **Phone version:** at least Android 8; the exact version is unknown and will not be checked. Support everything from `minSdk` 26 upward, and test on an API 26 emulator.
- **App name:** "Sateenkaarikello" for both the launcher label and the in-app title. One UI shows long labels on two lines.
- **Progress:** progress from the web version is not carried over. The app starts fresh.
- **Default mode:** 12 h, with the 12 h / 24 h switch on the home screen, as on the web.
- **Application id:** `fi.sateenkaarikello`.

## Sources

- [Android Gradle plugin 9.2.0 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes)
- [AGP 9.0 built-in Kotlin](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [JetBrains: update your projects for AGP 9](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/)
- [Set up the Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler)
- [Compose release notes](https://developer.android.com/jetpack/androidx/releases/compose)
- [Navigation 3 release notes](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [DataStore release notes](https://developer.android.com/jetpack/androidx/releases/datastore)
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
- [Android Developers Blog: developer verification](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html)
- [Help Net Security: verification timeline](https://www.helpnetsecurity.com/2026/06/19/android-developer-verification-rollout-markets/)
