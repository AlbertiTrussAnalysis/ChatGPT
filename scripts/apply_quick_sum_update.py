from pathlib import Path

main_file = Path("app/src/main/java/com/thomas/punktetracker/MainActivity.java")
build_file = Path("app/build.gradle")

main_text = main_file.read_text(encoding="utf-8")

replacements = [
    ("Betrag auswählen und anschließend auf „Eintragen“ tippen.", "Mehrere Beträge nacheinander drücken. Die angezeigte Summe wird mit „Eintragen“ als eine Buchung übernommen."),
    ('quickSelectionText = text("Noch keinen Betrag ausgewählt", 14, Typeface.BOLD);', 'quickSelectionText = text("Aktuelle Summe: 0", 14, Typeface.BOLD);'),
    ("pendingQuickAmount = amount;", "pendingQuickAmount += amount;"),
    ('quickSelectionText.setText("Noch keinen Betrag ausgewählt");', 'quickSelectionText.setText("Aktuelle Summe: 0");'),
    ('"Ausgewählt: " + formatSignedNumber(pendingQuickAmount)', '"Aktuelle Summe: " + formatSignedNumber(pendingQuickAmount)'),
    ("boolean selected = amount == pendingQuickAmount;", "boolean selected = false;"),
    ('"Bitte zuerst einen Betrag auswählen."', '"Die aktuelle Summe ist 0."'),
]

for old, new in replacements:
    if old in main_text:
        main_text = main_text.replace(old, new)
    elif new not in main_text:
        raise RuntimeError(f"Expected source fragment was not found: {old}")

main_file.write_text(main_text, encoding="utf-8")

build_text = build_file.read_text(encoding="utf-8")
for old, new in [("versionCode 2", "versionCode 3"), ("versionName '2.0'", "versionName '2.1'")]:
    if old in build_text:
        build_text = build_text.replace(old, new)
    elif new not in build_text:
        raise RuntimeError(f"Expected build fragment was not found: {old}")

build_file.write_text(build_text, encoding="utf-8")
