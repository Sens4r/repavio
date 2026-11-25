# repavio – Projektregeln für Claude

Minimalistisches, rein lokales Android-Trainingstagebuch (Kotlin, Jetpack Compose, Material 3) mit ILB-Phasen.
Antworten auf Deutsch, direkt und sachlich, ohne Gendersprache. Der Nutzer entscheidet über das Produkt und testet auf einem Pixel 8 Pro.

## Produktgrundsätze (nicht verwässern)
- „So wenig Bedienung wie möglich, so viele Informationen wie nötig.“ Neue Funktionen nur nach Absprache.
- Alle Daten lokal: kein Konto, keine Cloud, kein Backend, keine KI.
- Profil (seit V1.4.2): Name, Geburtsdatum (Alter wird berechnet, nie gespeichert), Geschlecht m/w, Größe, Aktivität, Gewichtsverlauf (ein Eintrag pro Tag), Foto oder Avatar. **Verschlüsselt** mit Android-Keystore (AES-GCM) in `ProfileStore.kt`. In Backups nur, wenn ein Backup-Passwort gesetzt ist (PBKDF2-SHA256, Feld `profileEncrypted`); Trainingsdaten bleiben unverschlüsselt.
- Ernährung (seit V1.4.6 Tagebuch): Bedarf nach Mifflin-St Jeor × Aktivität × Ziel (Aufbau +10 %, Halten, Definition −20 %); Protein 2,0 g/kg, Fett 0,9 g/kg, Rest Kohlenhydrate. Tagebuch mit 4 Mahlzeiten, Grundliste (`BaseFoods.kt`, über 200 Einträge, gerundete Richtwerte je 100 g, KH ohne Ballaststoffe, jeder Eintrag mit Gramm je TL und EL; IDs `b:…` stabil halten), eigene Lebensmittel (`c:…`), Rezepte (`r:…`, Zutaten als Schnappschuss, Portion = Gesamtgewicht / Portionen), Mengen in g/ml, TL, EL oder Stück (seit V1.5.3; `gramsPerUnit`, Löffel ohne Angabe 5 / 15, „Stück“ nur mit `Food.piece`; eigene Lebensmittel mit optionalem TL/EL/Stück-Gewicht; gespeichert wird immer in g/ml), Verlauf, Favoriten, Schnelleingabe, Mahlzeit kopieren, Wasser (35 ml/kg), Ballaststoffe/Zucker/Salz. Einträge speichern absolute Werte (Schnappschuss). Gespeichert verschlüsselt in `ProfileStore` (`food_log`), im Backup nur im `profileEncrypted`-Block. Logik in `FoodLog.kt` mit `FoodLogTest.kt`. Kein Barcode-Scanner, keine Online-Datenbank.
- Adaptiver Bedarf (Stufe 3, seit V1.5.3, `Adaptive.kt`): Gewicht exponentiell geglättet (10 %/Tag), Fenster 28 Tage, bereit ab ≥ 14 Tagen Gewichtsspanne, ≥ 3 Wiegungen und ≥ 10 Tagen mit Tagebuch; Verbrauch = Ø Aufnahme (nur Tage mit Einträgen) − Trend × 7700 kcal/kg; Korrektur zur Formel gewichtet (volle Wirkung ab 21 Tagen), höchstens ±25 %. Ziel ändert sich nur montags (Daten bis Vorsonntag); „Neues Ziel ab Montag“ als Vorschau. Seite „Bedarf & Trend“ über „Ziel ›“.
- Pro Übung **ein** Gewichts- und **ein** Wiederholungswert für alle Sätze; keine Satz-für-Satz-Erfassung.
- Eigenkörpergewicht (seit V1.5.0, `Bodyweight.kt`): `bodyweight` an Übung/Alternative, `null` = automatisch für Calisthenics-Namen. Eingabewert `"0"` = nur EK, Zahl = Zusatzgewicht, `"band:<id>"` = Band. Gespeichert wird die effektive Last (Profilgewicht + Zusatz − Band, nie negativ) plus Aufschlüsselung. Bänder in `AppData.bands` (gemeinsame Liste), seit V1.5.2 direkt im Belastungs-Sheet der Übung angelegt/bearbeitet/gelöscht (nicht mehr unter Profil): Farbe, Spanne vom Etikett (von–bis, lb oder kg), gerechnet wird mit der Mitte in kg (`Band.kg`).
- Prognose (seit V1.5.0): Epley-1RM der letzten ≤ 6 Einheiten, linearer Trend ±5 %, ab 2 Einheiten; Gewichtserhöhung (2,5 kg, unter 20 kg 1 kg) ab 2 Wdh. über dem letzten Wert; bei EK nur Wiederholungen. Seit V1.5.2 als Pfeil + Zahl direkt im kg-/Wdh.-Feld, nur bei Abweichung; höher grün (#7BD88F), niedriger rot (#FF8A80) – bewusste Ausnahme von „Akzente nur aus dem System“. Antippen der Zahl übernimmt den Wert.
- Trainingsdauer: Stoppuhrzeit wird beim Abschluss als `Workout.durationMs` gespeichert (1 min bis 12 h).
- Neuigkeiten-Pop-up (seit V1.5.2, `News.kt`): Einträge in `RELEASE_NEWS` nur für Versionen mit neuen Funktionen (kein Eintrag bei Bugfix/Performance → kein Pop-up), höchstens 5–6 kurze Punkte mit Symbol. Einmal pro Update, nicht nach Neuinstallation; übersprungene Versionen werden zusammengefasst. „Nicht mehr anzeigen“ bzw. Schalter unter Profil → Bedienung. Darstellung seit V1.5.3: starke Abdunklung, Dialog #24272D, Fließtext #D2D5DC 15 sp. Erstes Update ohne gespeicherten Stand zeigt alle Einträge der aktuellen Hauptversion (x.y). Bei jedem Feature-Release Eintrag ergänzen; Korrektur-/Sicherheitsversionen mit `kind` eintragen (lösen kein Pop-up aus, erscheinen nur beim Blättern). Seit V1.6.0 mit ‹ › durch den Versionsverlauf blätterbar, Seite 0 = „Neu in repavio“. Kein Einführungs-Tutorial mehr.
- Übungsinfo (seit V1.6.0, `ExerciseCatalog.kt`, `ExerciseInfoScreen.kt`): Katalog für alle `EXERCISE_SUGGESTIONS` (Test erzwingt Vollständigkeit) mit Haupt-/Mitmuskeln, Ausführung, häufigen Fehlern; Aliasse mit `=`. Muskelkarte im ersten, vereinfachten Vorschau-Design (Ellipsen/Rechtecke, vorne/hinten, ohne Geschlecht); detailliertere/„menschliche“ Figuren wurden vom Nutzer abgelehnt, KI-Bilder bisher nicht eingebaut. Öffnet über den Übungsnamen (ⓘ). Keine Bewegungsbilder (vom Nutzer so entschieden).
- Analyse „Alle Phasen“ (seit V1.6.0, `Overall.kt`): Kraftindex über geschätztes 1RM (Epley), Übungen über den Namen zusammengeführt, Phasenstreifen nach Phasentyp, Phasen im Vergleich (Ø Zuwachs je Phase), Bestwerte, Zuwachs je Muskelgruppe.
- Gewichtsabfrage (seit V1.6.0, `Weighing.kt`, `WeighDialog.kt`): Pop-up beim Öffnen, wenn fällig (aus/täglich/3 Tage/wöchentlich/2 Wochen, Wochentag; Standard wöchentlich montags), nur wenn schon ein Gewicht im Profil steht; „Heute nicht“ gilt für den Tag. Einstellung in `LocalStore` (`weigh_config`, `weigh_snoozed`), nicht im Backup.
- Bottom-Sheets immer mit `skipPartiallyExpanded = true` und scrollbarem Inhalt; keine festen Höhen für Inhalte mit Text (Textgröße auf dem Pixel ist größer als im Emulator).
- Tabs (seit V1.5.0): Reihenfolge und Ausblenden unter Profil → Bedienung, gespeichert in `LocalStore` (`tabs`, nicht im Backup). Profil nie ausblendbar.
- Alternativübungen sind nur Ersatz, kein zusätzlicher Block.
- Nur gespeicherte Trainings zählen. Keine Nullwerte oder Lücken für Pausen, kein „verpasst“, keine Kalenderlogik für Phasen.
- ILB-Phase: Ziel = Wochen × aktive Tage; Abschluss nur manuell nach Erreichen des Ziels. Neue Phase leert die Übungskacheln, Tage und Muskelgruppen bleiben.
- „Keine Phase“ (`Phase.openEnded`): kein Ziel, kein Wochenzähler, kein Abschluss; Wechsel zu ILB archiviert den Zeitraum.
- Ein Wochentagsplan ist pro Kalenderwoche nur einmal abschließbar.
- Gleichnamige Übungen teilen eine Identität (`exerciseKey`, normalisierter Name). Identitätslogik nicht ohne Rücksprache ändern.
- Feste Muskelgruppen: Bauch, Beine, Bizeps, Brust, Rücken, Schulter. Nicht ohne Wunsch erweitern.
- Sound: abschaltbarer Wisch-Sound und (seit V1.4.5) Stoppuhr-Pieptöne im Vollbild (kurz/doppelt, Abstände einstellbar, abschaltbar, Musik-Ducking, nur bei geöffneter App). Kein Klick- oder Abschlusssound.
- Design: Minimal Dark, Material You. Keine Bildschirmtitel. Seit V1.4.1 (an die MCI-App angelehnt, aber ohne deren Branding): kräftige Typografie, große Rundungen (Karten 20–24 dp), neutrale Flächen, Akzente nur aus dem System (`primary`, `primaryContainer`), Tab-Leiste nur mit Icons (Training, Analyse, Ernährung, Profil; anordnen/ausblendbar; Einstellungen liegen im Profil), Vollbildmodus im Training (nur Übungen, Stoppuhr-Kachel ohne Statuszeile + Button, nicht gespeichert, Zurück verlässt ihn).
- Nicht umsetzen ohne ausdrücklichen Wunsch: KI- oder Fragebogen-Trainingsplaner, Pausentimer (die Stoppuhr seit V1.4.5 ist kein Pausentimer: kein Countdown, keine Satzlogik), Einzelsatz-Erfassung, Cloud, Konten, Social, Werbung.

## Technische Fixpunkte
- `applicationId = "de.gymminimal.app"` **nie ändern** (sonst andere App, Datenverlust).
- Release-Signatur über GitHub Secrets (`REPAVIO_KEYSTORE_BASE64`, `REPAVIO_KEYSTORE_PASSWORD`, `REPAVIO_KEY_ALIAS`, `REPAVIO_KEY_PASSWORD`). Keystore nie neu erzeugen, Secrets nie ausgeben.
- **Kein `REQUEST_INSTALL_PACKAGES`** und kein In-App-Installer: Das löste Play-Protect-Sperren aus (V1.3.0/1.3.1). Der Workflow prüft das.
- Update-Weg: `RepavioUpdates.kt` fragt `/releases/latest` ab (Vorabversionen werden ignoriert), ab Android 10 Download über den Android-DownloadManager mit SHA-256-Prüfung gegen den Asset-`digest`, danach in Downloads; Installation bestätigt der Nutzer. Android 8/9: Browser.
- Datenhaltung: JSON in `SharedPreferences` (`LocalStore.kt`), Schema 6. Neue Felder optional und abwärtsverträglich lesen; Speichersystem nicht ohne Tests wechseln.
- Logik in `Models.kt` mit Unit-Tests absichern (`app/src/test/.../ModelsTest.kt`); CI führt sie aus.

- App-Icon: adaptiv. Ab Android 12 `mipmap-anydpi-v31` mit Systemfarben (`system_accent1_700` Hintergrund, Glyph in `system_accent1_100`); `monochrome`-Ebene für „Designsymbole“ (Android 13+). Davor schwarz auf weiß.

## Sicherheit (Prüfung 24.11.2026)
- Signieren nur im GitHub-Environment `signing` mit Pflichtfreigabe durch den Nutzer (Release-Job `environment: signing`). Secrets dort, nicht auf Repo-Ebene. Nach dem Merge den Nutzer auf die ausstehende Freigabe hinweisen.
- Update-Download: SHA-256 und Kopie nach Downloads in einem Durchgang in einen `IS_PENDING`-Eintrag; erst nach Übereinstimmung sichtbar.
- Importierte Backups sind nicht vertrauenswürdig: Dateigröße ≤ 25 MB, PBKDF2-Iterationen 100 000–2 000 000, Salt 16–64 Byte, Profilfoto ≤ 1,5 MB und ≤ 1024 px. Neue Backup-Passwörter: 600 000 Iterationen.
- `data_extraction_rules.xml`: kein Cloud-Backup, kein Geräte-Umzug durch Android (`allowBackup=false` allein verhindert den Umzug nicht).
- Website ohne Drittanbieter: Schriften lokal (`docs/assets/fonts`, SIL OFL), CSP-Meta erlaubt nur eigene Dateien und `api.github.com`; Links aus der API nur, wenn sie ins eigene Repo zeigen.
- Keine Google Fonts, CDNs oder Tracker einbauen.
- Abhängigkeiten: CI erzwingt SHA-256-Prüfsummen aus `ci/verification-metadata.xml` (wird vor dem Build nach `gradle/` kopiert). Bei neuen oder aktualisierten Bibliotheken/Plugins die Datei neu erzeugen (`gradle --write-verification-metadata sha256 :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest` in CI, Ausgabe über das Job-Log holen) und die Änderungen im PR begründen. Nie einfach Einträge ergänzen, ohne die Quelle zu prüfen.

## Website
- `docs/index.html` → GitHub Pages aus `main`/`docs`, Adresse https://sens4r.github.io/repavio/. Eine Datei, kein Build, keine externen Abhängigkeiten (Schriften lokal).
- Stil wie https://sens4r.github.io (Repo `Sens4r/sens4r.github.io`, raycast-inspiriert): Inter/JetBrains Mono, fast schwarz `#07060B`, Sensa-Logofarben, `prefers-reduced-motion` beachten. Hero-Hintergrund bewusst eigenes Motiv (Diagrammraster, atmende Balken, steigende Fortschrittslinien in App-Blau), nicht die Sensa-Lichtstrahlen.
- Versionen und APK-Link werden live aus der GitHub-Releases-API geladen; Funktionstexte nur aus Code/Release-Notes ableiten, nichts erfinden. Bei neuen Funktionen Karten/FAQ mitpflegen.
- Kein Impressum (privates, nicht geschäftliches Projekt, wie die Sensa-Seite).

## Release-Ablauf
1. Arbeitsbranch von `main`, nur gewünschte Änderungen, keine unbeteiligten Umbauten.
2. `versionName` und `versionCode` erhöhen; Release-Notizen in `release-notes/vX.Y.Z.md` (Deutsch, knapp). Fehlt die Datei, bricht der Release-Job ab.
3. Pull Request gegen `main`, CI abwarten.
4. **Mergen nur auf ausdrückliche Anweisung** des Nutzers. Der Merge veröffentlicht automatisch ein reguläres Release (`latest`).
5. Danach prüfen: Release-Job erfolgreich, Asset `repavio-vX.Y.Z-release.apk`, `digest` vorhanden, Tag zeigt auf den Merge-Commit, APK-Hash selbst nachrechnen, keine Installationsberechtigung.
6. Status getrennt melden: getestet / gebaut / veröffentlicht / auf dem Handy bestätigt. Keine Test-APK (Debug-Signatur) zum Installieren empfehlen.
7. Bei jedem Release den Abschnitt „Stand“ unten aktualisieren.

Nur `Sens4r/repavio` verändern. Vor Updates, die Daten betreffen, ein externes Backup empfehlen.

## Stand
| Version | Datum | Status |
|---|---|---|
| 1.3.5 | 20.11.2026 | Downloadmanager eingeführt; Installation ohne Play-Protect-Sperre bestätigt |
| 1.3.6 | 21.11.2026 | Update 1.3.5 → 1.3.6 über den Downloadmanager auf dem Pixel bestätigt |
| 1.4.0 | 21.11.2026 | „Keine Phase“, Analyse-Drill-down mit Fortschrittsgrafik, Calisthenics-Übungen, weniger empfindliches Wischen; Update und Funktionen auf dem Pixel bestätigt |
| 1.4.1 | 22.11.2026 | Neues UI im MCI-Stil (Vorschau vom Nutzer freigegeben), Vollbildmodus, Icon-Tab-Leiste, Balkendiagramm im Übungsdetail, Einstellungen als Kategorie-Kacheln, schlankes Update-Popup; Update und UI auf dem Pixel bestätigt (Text in Einstellungs-Kacheln war abgeschnitten → in 1.4.2 behoben) |
| 1.4.2 | 22.11.2026 | Profil (verschlüsselt, Avatar/Foto, Gewichtsverlauf, Alter automatisch), Ernährungs-Tab, Backup-Passwort, Einstellungen als Liste im Profil; auf dem Pixel bestätigt |
| 1.4.3 | 23.11.2026 | App-Icon in Systemfarben + Designsymbol; Website unter `docs/`; auf dem Pixel bestätigt |
| 1.4.4 | 24.11.2026 | Sicherheitsupdate (Update-Download in einem Durchgang, Import-Grenzen, PBKDF2 600k, kein Geräte-Umzug, Website ohne Google Fonts, Signier-Freigabe); Update und Signier-Freigabe bestätigt |
| 1.4.5 | 24.11.2026 | Stoppuhr im Vollbild (Pieptöne einstellbar, Ducking, Bildschirm bleibt an, Reset bei „Training beenden“); auf dem Pixel bestätigt |
| 1.5.0 | 24.11.2026 | Vorabversion: Ernährungstagebuch mit Rezepten (1.4.6 nie veröffentlicht, hier enthalten), Eigengewicht mit Zusatz/Bändern, Prognose, Trainingsdauer, Tabs anordnen/ausblenden |
| 1.5.1 | 24.11.2026 | Kacheltext in „Hinzufügen“, Gewicht im Profil-Dialog änderbar |
| 1.5.2 | 24.11.2026 | Vorschau-Änderungen: Prognose im Feld (grün/rot), Bänder direkt in der Übung mit Spanne lb/kg, Stoppuhr ohne Statuszeile, Neuigkeiten-Pop-up statt Einführung |
| 1.5.3 | 25.11.2026 | Bedarf & Trend (adaptiver Bedarf, Stufe 3), Mengen in TL/EL/Stück (Tagebuch, Rezept-Zutaten, eigene Lebensmittel), Grundliste über 200 Einträge mit TL/EL, Neuigkeiten-Pop-up besser lesbar; auf dem Pixel installiert, Darstellungsfehler gemeldet → 1.5.4 |
| 1.5.4 | 25.11.2026 | Bugfix (Bottom-Sheets, Einheiten-Auswahl, Wochenbalken, Makro-Punktfarben); nicht einzeln veröffentlicht, in 1.6.0 enthalten |
| 1.6.0 | 25.11.2026 | Übungsinfo für alle Vorschlagsübungen, Analyse über alle Phasen, Gewichtsabfrage mit Häufigkeit, Neuigkeiten mit Versionsverlauf; auf dem Pixel noch nicht bestätigt |

Offen: nichts Geplantes. Ungetestet sind Android 8/9 (Browser-Fallback) und Geräte anderer Hersteller.
