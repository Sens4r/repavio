# repavio – Projektregeln für Claude

Minimalistisches, rein lokales Android-Trainingstagebuch (Kotlin, Jetpack Compose, Material 3) mit ILB-Phasen.
Antworten auf Deutsch, direkt und sachlich, ohne Gendersprache. Der Nutzer entscheidet über das Produkt und testet auf einem Pixel 8 Pro.

## Produktgrundsätze (nicht verwässern)
- „So wenig Bedienung wie möglich, so viele Informationen wie nötig.“ Neue Funktionen nur nach Absprache.
- Alle Daten lokal: kein Konto, keine Cloud, kein Backend, keine KI.
- Pro Übung **ein** Gewichts- und **ein** Wiederholungswert für alle Sätze; keine Satz-für-Satz-Erfassung.
- Alternativübungen sind nur Ersatz, kein zusätzlicher Block.
- Nur gespeicherte Trainings zählen. Keine Nullwerte oder Lücken für Pausen, kein „verpasst“, keine Kalenderlogik für Phasen.
- ILB-Phase: Ziel = Wochen × aktive Tage; Abschluss nur manuell nach Erreichen des Ziels. Neue Phase leert die Übungskacheln, Tage und Muskelgruppen bleiben.
- „Keine Phase“ (`Phase.openEnded`): kein Ziel, kein Wochenzähler, kein Abschluss; Wechsel zu ILB archiviert den Zeitraum.
- Ein Wochentagsplan ist pro Kalenderwoche nur einmal abschließbar.
- Gleichnamige Übungen teilen eine Identität (`exerciseKey`, normalisierter Name). Identitätslogik nicht ohne Rücksprache ändern.
- Feste Muskelgruppen: Bauch, Beine, Bizeps, Brust, Rücken, Schulter. Nicht ohne Wunsch erweitern.
- Sound: nur der abschaltbare Wisch-Sound. Kein Klick- oder Abschlusssound.
- Design: Minimal Dark, Material You. Keine Bildschirmtitel. Seit V1.4.1 (an die MCI-App angelehnt, aber ohne deren Branding): kräftige Typografie, große Rundungen (Karten 20–24 dp), neutrale Flächen, Akzente nur aus dem System (`primary`, `primaryContainer`), Tab-Leiste nur mit Icons, Vollbildmodus im Training (nur Übungen + Button, nicht gespeichert, Zurück verlässt ihn).
- Nicht umsetzen ohne ausdrücklichen Wunsch: KI- oder Fragebogen-Trainingsplaner, Pausentimer, Einzelsatz-Erfassung, Cloud, Konten, Social, Werbung.

## Technische Fixpunkte
- `applicationId = "de.gymminimal.app"` **nie ändern** (sonst andere App, Datenverlust).
- Release-Signatur über GitHub Secrets (`REPAVIO_KEYSTORE_BASE64`, `REPAVIO_KEYSTORE_PASSWORD`, `REPAVIO_KEY_ALIAS`, `REPAVIO_KEY_PASSWORD`). Keystore nie neu erzeugen, Secrets nie ausgeben.
- **Kein `REQUEST_INSTALL_PACKAGES`** und kein In-App-Installer: Das löste Play-Protect-Sperren aus (V1.3.0/1.3.1). Der Workflow prüft das.
- Update-Weg: `RepavioUpdates.kt` fragt `/releases/latest` ab (Vorabversionen werden ignoriert), ab Android 10 Download über den Android-DownloadManager mit SHA-256-Prüfung gegen den Asset-`digest`, danach in Downloads; Installation bestätigt der Nutzer. Android 8/9: Browser.
- Datenhaltung: JSON in `SharedPreferences` (`LocalStore.kt`), Schema 6. Neue Felder optional und abwärtsverträglich lesen; Speichersystem nicht ohne Tests wechseln.
- Logik in `Models.kt` mit Unit-Tests absichern (`app/src/test/.../ModelsTest.kt`); CI führt sie aus.

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
| 1.4.1 | 22.11.2026 | Neues UI im MCI-Stil (Vorschau vom Nutzer freigegeben), Vollbildmodus, Icon-Tab-Leiste, Balkendiagramm im Übungsdetail; auf dem Pixel noch nicht bestätigt |

Offen: nichts Dringendes. Ungetestet sind Android 8/9 (Browser-Fallback) und Geräte anderer Hersteller.
