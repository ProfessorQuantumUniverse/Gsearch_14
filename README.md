<div align="center">

# 🔍 Gsearch14

### Search for human information.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white) ![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) ![License](https://img.shields.io/badge/License-GPL%20v3-blue?style=for-the-badge)

<!-- INSTALL BUTTONS START -->
<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.olafsapp.gsearch14"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80"></a>
  <a href="https://github.com/ProfessorQuantumUniverse/Gsearch_14/releases/latest"><img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80"></a>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/ProfessorQuantumUniverse/Gsearch_14"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a>
</p>
<!-- INSTALL BUTTONS END -->

</div>

---

Eine Android-Such-App für Ergebnisse, die von Menschen geschrieben wurden. Statt einer
KI-Zusammenfassung oben auf der Seite bekommst du Links — und die Wahl, welche
Suchmaschine sie liefert.

## ✨ Features

### Suche ohne KI
Jede Suchmaschine bringt ihren eigenen Weg mit, generierte Antworten abzuschalten, und die
App sagt ehrlich, welcher das ist:

| Suchmaschine | KI-frei über | Kategorien |
|---|---|---|
| **Google** | `udm=14` — der reine Web-Filter, ohne AI Overview | Web · Bilder · Videos · News |
| **Wikipedia** | hat gar keine KI-Antworten | Web · Bilder |
| **Marginalia** | findet kleine, nicht-kommerzielle Textseiten | Web |

Wikipedia springt bei einem exakten Titeltreffer direkt in den Artikel; die Bildersuche
bleibt auf der Trefferliste und filtert auf den Datei-Namensraum.

Wo eine Suchmaschine gar keine KI-Antworten kennt, sagt der Schalter das und bleibt
gesperrt, statt eine Wirkung vorzutäuschen.

### Weiteres
- 🔤 **Suchvorschläge** live beim Tippen, von der aktiven Suchmaschine; Treffer aus dem
  eigenen Verlauf stehen oben
- 🕘 **Verlauf** mit Volltextfilter, Anheften, Wischen zum Löschen und Rückgängig
- 🔖 **Lesezeichen** für Ergebnisseiten
- 🕵️ **Inkognito** — suchen, ohne den Verlauf zu füllen
- 🎨 **Material 3 Expressive**: Material-You-Farben aus dem Hintergrundbild oder fünf eigene
  Akzentpaletten, tiefschwarzer Dunkelmodus, federnde Animationen und haptisches Feedback
- 📖 **Drei Wege zum Ergebnis**: App-Reader mit Ziehen zum Aktualisieren, Chrome Custom Tab
  oder der Standardbrowser
- 🏠 **Homescreen-Widget** (Glance) mit Material-You-Farben
- 🌍 **Deutsch und Englisch**, inklusive per-App-Sprachwahl in den Systemeinstellungen

## 🛠️ Tech-Stack

`Kotlin 2.2` · `Jetpack Compose` · `Material 3` · `DataStore` · `Glance` · `Navigation Compose`
· `androidx.graphics.shapes`

Einzelnes Modul, MVVM mit einem ViewModel, handgeschriebener Dependency-Container statt
DI-Framework. Kein Netzwerk-Framework: die einzige eigene HTTP-Anfrage — Autocomplete —
läuft über `HttpURLConnection`.

| | |
|---|---|
| minSdk | 26 (Android 8.0) |
| targetSdk | 36 (Android 16) |
| compileSdk | 37 (Android 17) |
| AGP / Gradle | 9.3.1 / 9.7.1 |

## 🚀 Build

```bash
./gradlew assembleDebug
```

Tests und statische Analyse:

```bash
./gradlew testDebugUnitTest lintDebug
```

Für einen signierten Release-Build eine `keystore.properties` im Projektwurzelverzeichnis
anlegen (sie ist in `.gitignore`):

```properties
storeFile=/pfad/zum/keystore.jks
storePassword=…
keyAlias=…
keyPassword=…
```

```bash
./gradlew bundleRelease
```

## 🔄 Update von Version 3

Verlauf und Einstellungen aus älteren Versionen werden beim ersten Start einmalig aus den
SharedPreferences übernommen — Suchbegriffe, Kategorie, Design und die Browser-Wahl bleiben
erhalten.

---

<div align="center">

Teil meiner Projektsammlung · [**Alle Projekte ansehen →**](https://professorquantumuniverse.github.io/My-Projects/)

Made with ☕ & curiosity by **Lorenzo Bay-Müller** ([@ProfessorQuantumUniverse](https://github.com/ProfessorQuantumUniverse))

</div>
