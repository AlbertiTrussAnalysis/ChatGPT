# Punkte Tracker

Native Android-App für die bisherige ODS-Logik.

## Verhalten
- Startdatum: 16.08.2026
- Tagesgrenze: 30
- Schnellbuttons: +1, +5, +10, -1, -5, -10
- Exakter Tageswert kann direkt gesetzt werden
- Werte werden lokal in SharedPreferences gespeichert
- Laufende Summe = Summe aller Tageswerte seit dem Startdatum
- Laufender Grenzwert = Anzahl Tage seit Start (inklusive) × 30
- Status grün bei Summe <= Grenzwert, rot bei Summe > Grenzwert
- Keine Internet-Berechtigung

## APK
Die GitHub Action `Build Android APK` baut bei Änderungen auf `main` automatisch eine Debug-APK und speichert sie als Artefakt `PunkteTracker-APK`.
