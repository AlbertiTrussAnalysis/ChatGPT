from pathlib import Path
import re

main_file = Path("app/src/main/java/com/thomas/punktetracker/MainActivity.java")
build_file = Path("app/build.gradle")

main_text = main_file.read_text(encoding="utf-8")

# Keep the cumulative quick-booking behavior idempotent.
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

# Remove the complete exact-day-value feature from the UI and source.
for fragment in [
    "    private EditText exactInput;\n",
    "    private Button exactEntryButton;\n",
    "        buildExactEntryCard(root);\n",
    "        exactInput.setText(\"\");\n",
    "        exactInput.setEnabled(enabled);\n",
    "        exactInput.setAlpha(enabled ? 1f : 0.45f);\n",
    "        setButtonEnabled(exactEntryButton, enabled);\n",
]:
    main_text = main_text.replace(fragment, "")

main_text, exact_card_count = re.subn(
    r"\n    private void buildExactEntryCard\(LinearLayout root\) \{.*?\n    \}\n\n    private void buildBookingsSection",
    "\n    private void buildBookingsSection",
    main_text,
    count=1,
    flags=re.DOTALL,
)

main_text, exact_booking_count = re.subn(
    r"\n    private void addExactBooking\(\) \{.*?\n    \}\n\n    private void addBooking",
    "\n    private void addBooking",
    main_text,
    count=1,
    flags=re.DOTALL,
)

if "buildExactEntryCard" in main_text or "addExactBooking" in main_text or "exactInput" in main_text or "exactEntryButton" in main_text:
    raise RuntimeError("Exact-day-value feature was not fully removed")

main_file.write_text(main_text, encoding="utf-8")

build_text = build_file.read_text(encoding="utf-8")
version_replacements = [
    ("versionCode 2", "versionCode 4"),
    ("versionCode 3", "versionCode 4"),
    ("versionName '2.0'", "versionName '2.2'"),
    ("versionName '2.1'", "versionName '2.2'"),
]
for old, new in version_replacements:
    if old in build_text:
        build_text = build_text.replace(old, new)

if "versionCode 4" not in build_text or "versionName '2.2'" not in build_text:
    raise RuntimeError("Could not set app version 2.2")

build_file.write_text(build_text, encoding="utf-8")
