# Sateenkaarikello (Android)

Natiivi Android-sovellus (Kotlin, Jetpack Compose, ei WebViewiä), joka opettaa 8-vuotiasta lukemaan pyöreää kellotaulua. Siirretty web-versiosta `C:\Dev\Kellotaulu\sateenkaarikello.html`. Web-kansio on vain luettavaa lähdeaineistoa, eikä sitä muokata.

- Suunnitelma: `docs/android-apk-plan.md` (kohta 11 = lopulliset päätökset).
- Toimitus: allekirjoitettu release-APK käsin asennettavaksi. Ei Play Storea, verkkoa eikä oikeuksia (manifestissa ei yhtään `uses-permission`-riviä; TTS vaatii vain `<queries>`-merkinnän).
- `minSdk 26`, `compileSdk`/`targetSdk 37`. Kaikki API 26:ta uudempi suojataan tai se ohittuu itsestään (`values-v31`, teemoitettu kuvake, `enableOnBackInvokedCallback`).

## Versiot

`gradle/libs.versions.toml`: AGP 9.4.1, Gradle 9.8.1 (jakelun SHA-256 kiinnitetty wrapperiin), Kotlin (KGP + Compose-kääntäjä + serialization) 2.4.21, Compose BOM 2026.09.00 (UI 1.12.1, Material3 1.4.0), Navigation 3 1.2.0, Lifecycle 2.11.0, DataStore 1.2.1, espresso-core 3.7.0 (vanhempi ei toimi API 37:llä). AGP 9:n sisäänrakennettu Kotlin; uudempi KGP on juuren `buildscript`-classpathissa. KGP 2.4.21 on virallisesti testattu AGP 9.3.1:een ja Gradle 9.7.0:aan asti. AGP 9.4.1 ja Gradle 9.8.1 on varmennettu tässä projektissa (build, R8-release, yksikkö- ja UI-testit API 26:lla ja 37:llä, 2026-10-14). Dependabot: viikon viive uusille julkaisuille, ja compose-rules-detekt pidetään 0.4.x:ssä, koska 0.5+ vaatii detekt 2:n.

## Rakenne (`app/src/main/java/fi/sateenkaarikello`)

- `MainActivity.kt`: edge-to-edge, tummat järjestelmäpalkin kuvakkeet, äänet pois `onPause`-vaiheessa.
- `SateenkaariApplication.kt`: `AppServices`-rajapinta ja `AppContainer` (tallennus, äänet, puhe).
- `SateenkaariApp.kt`: teema, konfettikerros, Navigation 3 -paluupino ja näkymien kytkentä.
- `Screen.kt`: näkymien `@Serializable`-avaimet ja `ScreenSavedState` (polymorfinen sarjallistus ilman reflektiota, toimii R8:n kanssa).
- `Navigator.kt`: paluupinon muutokset (push, pop, replace, popTo, tason aloitus oppituntien kautta).
- `domain/`: puhdas Kotlin ilman Android-importteja, JVM-yksikkötestattu.
  - `FinnishTime.kt`: `timeWords`, `digital`, `dayPart`, `spoken24`, `fullWords`.
  - `ClockTime.kt`: ympyrälaskenta, `snap`, `minuteDrag`, `hourDrag`, `pickHand`, animaation kesto.
  - `Levels.kt`: 5 tasoa, `makeTasks`, `optionsFor`, tähdet, haasteen kysymykset. Tasoja ja Minuuttihaastetta ei lukita: ne voi pelata missä järjestyksessä tahansa (päätös 2026-10-14, sama webissä).
  - `Hints.kt`: vinkit tyypitettynä datana (`Hint`); tekstit strings.xml:ssä.
  - `Wardrobe.kt`: 18 vaatetta, 5 paikkaa, `Outfit`.
  - `Progress.kt`: `Progress` (nollattava) ja `Settings` (säilyy nollauksessa).
  - `Lessons.kt`: oppituntien tyypit, `demo { }` -DSL, `Text`-viittaukset, tasojen oppitunnit, `introPlan`.
- `data/ProgressRepository.kt`: Preferences DataStore, viallinen arvo korvataan oletuksella.
- `audio/`: `ToneSynth` (webin sävelet PCM:ksi, testattu), `Sfx` (AudioTrack MODE_STATIC), `Speech` (TTS, `available` vain suomenkielisellä äänellä).
- `ui/`
  - `theme/`: kaikki suunnittelumerkit (`SkColors`, `SkDollColors`, `SkType`, `SkShapes`, `SkDimens`), `dotBackground()`.
  - `components/`: napit (`DepthSurface`, `SkButton`, `ChipButton`, `SegmentedControl`, `BigMenuButton`), `OptionButton`, `ProgressDots` (täyttyy pompahtaen, kun tähti saapuu), `StarRow`, `FeedbackBox`, `TopBar`, `ScreenScaffold` (kellon koko min(leveys − 32 dp, korkeus × 0,42)), `WordSafeText`, SVG-kuvakkeet, `Shapes.kt` (`starPath`, `HeartPath`).
  - `Confetti.kt`: koko näytön kerros. `burst` (suorakulmiot, tähdet, sydämet, sateenkaarenkaaret), `sparkle` (kultainen tähtisuihku), `flyStar` (tähti lentää kaarena edistymispalloon), `rememberCelebrationAnchors`. Laskenta `ConfettiMath.kt`:ssä (testattu).
  - `clock/`: `ClockFace` (Canvas 400×400, SVG:n piirtojärjestys, raahaus, korostus, saavutettavuustoiminnot), `ClockState`, `ClockCommand`, `ClockSpec`.
  - `doll/`: `Doll` (Canvas 200×340, webin SVG-polut, asento `pose`-lambdana piirtovaiheessa), `DollParts`, `DollScreen` (vaatekaappi), `LivelyDoll` (räpyttää ja keinuu; tulosnäkymässä pyörähtää uuteen asuun ja hyppää vilkuttaen) ja `DollMotion.kt` (liikkeet puhtaina ajan funktioina, testattu).
  - `AppViewModel.kt`, `Texts.kt` (domainin tekstit, vinkit ja nimet resursseista), `Motion.kt` (animaatiot pois -asetus).
  - Näkymät: `home/`, `freeclock/`, `levels/`, `game/` (peli ja tulos), `challenge/`, `ask/`, `lesson/` (`LessonCatalog` = 9 oppituntia datana, `LessonViewModel` ajaa animaatiot korutiinina).

## Arkkitehtuurin periaatteet

- Kellon animaatio ja raahaus elävät koosteessa (`ClockFace`), koska animaatio tarvitsee ruutukellon. Näkymämallit lähettävät vain `ClockCommand`-käskyjä (aika, kesto, tarkka), joten ne ovat JVM-testattavia.
- Näkymämallit (`GameViewModel`, `ChallengeViewModel`, `LessonViewModel`) saavat `AppServices`-rajapinnan; testeissä `FakeServices` + `InMemoryPreferencesStore`.
- Compose-vakaus: `config/compose-stability.conf` merkitsee domainin tyypit ja Kotlinin kokoelmat vakaiksi (domain pysyy ilman Compose-annotaatioita). Sama tiedosto on kytketty Compose-kääntäjälle ja vakausanalysaattorille. Näkymämallit ja `AppServices` ovat `@Stable`, ja `SvgIcon` ja `Screen.Lesson` ovat `@Immutable`. Lähtötaso on `app/stability/`; tarkoituksellisen muutoksen jälkeen aja `./gradlew :app:stabilityDump`.

## Säännöt

- Kaikki käyttäjälle näkyvät tekstit ovat `res/values/strings.xml`:ssä suomeksi. Poikkeus on `FinnishTime`:n tuottamat kellonajat. Domain viittaa resursseihin `Text.Res`-arvoilla, ja UI ratkaisee ne. Huom: Android poistaa resurssien alku- ja loppuvälilyönnit, joten lauseet yhdistetään koodissa välilyönnillä.
- Värit, koot ja muodot tulevat `ui/theme`-tiedostoista. Projektin koukku estää `Color(0x…)`-arvot muualla.
- Ei `·`-, `•`- tai `|`-erottimia, eikä värillisiä pystypalkkeja korttien reunassa.
- Fontit: staattiset Fredoka (500/600/700) ja Nunito (600/700/800) `res/font`-kansiossa, lisenssit `assets/licenses/`.

## Komennot

- Yksikkötestit: `./gradlew :app:testDebugUnitTest`
- Debug-APK: `./gradlew :app:assembleDebug`
- UI-testit emulaattorilla (aseta aina sarjanumero, jottei testejä ajeta kytketyllä puhelimella): `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`
- Kuvagalleria (nukke kaikissa vaatteissa, kello eri tiloissa): sama komento ja lisäksi `-Pandroid.testInstrumentationRunnerArguments.class=fi.sateenkaarikello.GalleryTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`, sitten `adb exec-out run-as fi.sateenkaarikello cat files/gallery/nukke.png > nukke.png`.
- Emulaattorit: `Sk_API26` (720×1280 @ 320 = 360×640 dp) ja `Sk_API37` (1080×2400 @ 420 ≈ 411×914 dp).
- Release: `./gradlew :app:assembleRelease`. Allekirjoitus luetaan `~/.gradle/gradle.properties`-tiedostosta (`SATEENKAARI_KEYSTORE`, `SATEENKAARI_KEYSTORE_PASSWORD`, `SATEENKAARI_KEY_ALIAS`, `SATEENKAARI_KEY_PASSWORD`). Ilman niitä syntyy allekirjoittamaton APK. `keytool` löytyy Android Studion `jbr/bin`-kansiosta.
- Lint ja tietoturva: käyttäjä ajaa `lc` ja `sc` itse; tulokset tulevat `reports/`-kansioon.

## Animaatiot

- Jatkuvat lepoliikkeet käyttävät `withInfiniteAnimationFrameNanos`-funktiota, jotta Compose-testit pääsevät lepotilaan. Asento luetaan vasta piirrettäessä, joten liike ei koosta näkymää uudelleen joka ruudulla.
- Kun järjestelmän animaatiot on poistettu (`rememberReduceMotion`), konfetti, tähden lento ja nuken liike jätetään pois: pallo täyttyy heti, ja uusi asu näkyy heti.
