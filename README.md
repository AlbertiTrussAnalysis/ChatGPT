# Punkte Tracker

Native Android-App für die bisherige ODS-Logik, optimiert für die Nutzung im Hochformat auf einem Pixel mit GrapheneOS.

## Version 2.0

- Startdatum: 16.08.2026
- Navigation zwischen allen Tagen vom Startdatum bis heute
- Beim Start wird immer der aktuelle Tag angezeigt
- Jeder Tag beginnt automatisch bei 0
- Schnellbuchungen: +1, +5, +10, -1, -5, -10
- Schnellbuchungen werden erst mit dem Button **Eintragen** gespeichert
- Ein exakter Tageswert kann gesetzt werden; gespeichert wird die dafür notwendige Differenz
- Mehrere einzelne Buchungen pro Tag
- Scrollbare Buchungsliste, neueste Buchung zuerst
- Jede Buchung kann bearbeitet oder gelöscht werden
- Laufende Summe und laufende Grenze von 30 pro Tag
- Grüner Status innerhalb der Grenze, roter Status bei Überschreitung
- Lokale Speicherung in SharedPreferences
- Automatische Übernahme vorhandener Tageswerte aus Version 1, sofern die App als signiertes Update installiert werden kann
- Keine Internet-Berechtigung

## APK-Build

Die GitHub Action `Build Android APK` baut bei Änderungen auf `main` automatisch eine Debug-APK und speichert sie als Artefakt `PunkteTracker-v2-APK`.

Ab Version 2 wird der Debug-Signierschlüssel im GitHub-Actions-Cache wiederverwendet, damit nachfolgende Builds als Updates installiert werden können. Der erste Wechsel von Version 1 auf Version 2 kann wegen des früher nicht dauerhaft gespeicherten Signierschlüssels eine Neuinstallation erfordern.
