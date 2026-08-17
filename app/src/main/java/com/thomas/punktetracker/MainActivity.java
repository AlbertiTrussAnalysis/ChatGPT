package com.thomas.punktetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 16);
    private static final long DAILY_LIMIT = 30L;

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter DAY_NAME =
            DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN);
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final int COLOR_BACKGROUND = Color.rgb(245, 247, 250);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_TEXT = Color.rgb(32, 38, 48);
    private static final int COLOR_TEXT_MUTED = Color.rgb(96, 103, 115);
    private static final int COLOR_PRIMARY = Color.rgb(49, 92, 155);
    private static final int COLOR_PRIMARY_LIGHT = Color.rgb(226, 235, 248);
    private static final int COLOR_GREEN = Color.rgb(34, 121, 67);
    private static final int COLOR_GREEN_LIGHT = Color.rgb(221, 245, 227);
    private static final int COLOR_RED = Color.rgb(166, 49, 49);
    private static final int COLOR_RED_LIGHT = Color.rgb(255, 226, 226);
    private static final int COLOR_BORDER = Color.rgb(218, 224, 233);
    private static final int COLOR_NEUTRAL = Color.rgb(237, 240, 245);

    private SharedPreferences prefs;
    private LocalDate currentDate;
    private LocalDate selectedDate;

    private ScrollView pageScroll;
    private Button previousDayButton;
    private Button nextDayButton;
    private TextView relativeDateText;
    private TextView dateText;
    private TextView dayValueText;
    private TextView runningText;
    private TextView limitText;
    private TextView differenceText;
    private TextView bookingTitleText;
    private TextView quickSelectionText;
    private LinearLayout statusCard;
    private LinearLayout bookingsContainer;
    private Button quickEntryButton;

    private final Map<Integer, Button> quickButtons = new HashMap<>();
    private int pendingQuickAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        prefs = getSharedPreferences("daily_values", MODE_PRIVATE);
        currentDate = LocalDate.now();
        selectedDate = currentDate.isBefore(START_DATE) ? START_DATE : currentDate;

        buildUi();
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();

        currentDate = LocalDate.now();
        if (!currentDate.isBefore(START_DATE) && selectedDate.isAfter(currentDate)) {
            selectedDate = currentDate;
        }
        refreshAll();
    }

    private void buildUi() {
        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setBackgroundColor(COLOR_BACKGROUND);
        pageScroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(32));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    dp(16),
                    dp(18) + insets.getSystemWindowInsetTop(),
                    dp(16),
                    dp(32) + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        pageScroll.addView(
                root,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        buildDateNavigation(root);
        buildStatusCard(root);
        buildQuickEntryCard(root);
        buildBookingsSection(root);
        buildFooter(root);

        setContentView(pageScroll);
    }

    private void buildDateNavigation(LinearLayout root) {
        LinearLayout navigationCard = createCard(COLOR_SURFACE);
        navigationCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(navigationCard, matchWrap());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        navigationCard.addView(row, matchWrap());

        previousDayButton = createNavigationButton("←");
        previousDayButton.setContentDescription("Vorheriger Tag");
        previousDayButton.setOnClickListener(v -> navigateByDays(-1));
        row.addView(previousDayButton, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(8), 0, dp(8), 0);
        row.addView(center, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView appTitle = text("Punkte Tracker", 13, Typeface.BOLD);
        appTitle.setTextColor(COLOR_PRIMARY);
        appTitle.setGravity(Gravity.CENTER);
        appTitle.setLetterSpacing(0.08f);
        center.addView(appTitle, matchWrap());

        relativeDateText = text("", 24, Typeface.BOLD);
        relativeDateText.setTextColor(COLOR_TEXT);
        relativeDateText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams relativeLp = matchWrap();
        relativeLp.topMargin = dp(2);
        center.addView(relativeDateText, relativeLp);

        dateText = text("", 15, Typeface.NORMAL);
        dateText.setTextColor(COLOR_TEXT_MUTED);
        dateText.setGravity(Gravity.CENTER);
        center.addView(dateText, matchWrap());

        nextDayButton = createNavigationButton("→");
        nextDayButton.setContentDescription("Nächster Tag");
        nextDayButton.setOnClickListener(v -> navigateByDays(1));
        row.addView(nextDayButton, new LinearLayout.LayoutParams(dp(52), dp(52)));
    }

    private void buildStatusCard(LinearLayout root) {
        statusCard = createCard(COLOR_GREEN_LIGHT);
        LinearLayout.LayoutParams statusLp = matchWrap();
        statusLp.topMargin = dp(14);
        root.addView(statusCard, statusLp);

        LinearLayout headline = new LinearLayout(this);
        headline.setOrientation(LinearLayout.HORIZONTAL);
        headline.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.addView(headline, matchWrap());

        LinearLayout headlineText = new LinearLayout(this);
        headlineText.setOrientation(LinearLayout.VERTICAL);
        headline.addView(
                headlineText,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView dayLabel = text("Tageswert", 14, Typeface.BOLD);
        dayLabel.setTextColor(COLOR_TEXT_MUTED);
        headlineText.addView(dayLabel, matchWrap());

        TextView helper = text("Summe aller Buchungen dieses Tages", 12, Typeface.NORMAL);
        helper.setTextColor(COLOR_TEXT_MUTED);
        LinearLayout.LayoutParams helperLp = matchWrap();
        helperLp.topMargin = dp(2);
        headlineText.addView(helper, helperLp);

        dayValueText = text("0", 44, Typeface.BOLD);
        dayValueText.setTextColor(COLOR_TEXT);
        dayValueText.setGravity(Gravity.END);
        headline.addView(
                dayValueText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(35, 50, 60, 75));
        LinearLayout.LayoutParams dividerLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerLp.topMargin = dp(14);
        dividerLp.bottomMargin = dp(14);
        statusCard.addView(divider, dividerLp);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        statusCard.addView(stats, matchWrap());

        runningText = addStat(stats, "Gesamtsumme", "0");
        limitText = addStat(stats, "Grenze", "30");
        differenceText = addStat(stats, "Rest", "30");
    }

    private void buildQuickEntryCard(LinearLayout root) {
        LinearLayout card = createCard(COLOR_SURFACE);
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.topMargin = dp(14);
        root.addView(card, cardLp);

        TextView title = text("Schnell buchen", 20, Typeface.BOLD);
        title.setTextColor(COLOR_TEXT);
        card.addView(title, matchWrap());

        TextView description = text(
                "Mehrere Beträge nacheinander drücken. Die angezeigte Summe wird mit „Eintragen“ als eine Buchung übernommen.",
                13,
                Typeface.NORMAL
        );
        description.setTextColor(COLOR_TEXT_MUTED);
        LinearLayout.LayoutParams descriptionLp = matchWrap();
        descriptionLp.topMargin = dp(3);
        descriptionLp.bottomMargin = dp(12);
        card.addView(description, descriptionLp);

        LinearLayout plusRow = createHorizontalRow();
        card.addView(plusRow, matchWrap());
        addQuickButton(plusRow, "+1", 1);
        addQuickButton(plusRow, "+5", 5);
        addQuickButton(plusRow, "+10", 10);

        LinearLayout minusRow = createHorizontalRow();
        LinearLayout.LayoutParams minusLp = matchWrap();
        minusLp.topMargin = dp(8);
        card.addView(minusRow, minusLp);
        addQuickButton(minusRow, "-1", -1);
        addQuickButton(minusRow, "-5", -5);
        addQuickButton(minusRow, "-10", -10);

        quickSelectionText = text("Aktuelle Summe: 0", 14, Typeface.BOLD);
        quickSelectionText.setTextColor(COLOR_TEXT_MUTED);
        quickSelectionText.setGravity(Gravity.CENTER);
        quickSelectionText.setPadding(dp(10), dp(10), dp(10), dp(10));
        quickSelectionText.setBackground(roundedBackground(COLOR_NEUTRAL, 12, 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams selectionLp = matchWrap();
        selectionLp.topMargin = dp(12);
        card.addView(quickSelectionText, selectionLp);

        quickEntryButton = createPrimaryButton("Eintragen");
        quickEntryButton.setOnClickListener(v -> addQuickBooking());
        LinearLayout.LayoutParams entryLp = matchWrapHeight(dp(54));
        entryLp.topMargin = dp(10);
        card.addView(quickEntryButton, entryLp);
    }

    private void buildBookingsSection(LinearLayout root) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams headerLp = matchWrap();
        headerLp.topMargin = dp(22);
        headerLp.bottomMargin = dp(10);
        root.addView(header, headerLp);

        bookingTitleText = text("Buchungen", 21, Typeface.BOLD);
        bookingTitleText.setTextColor(COLOR_TEXT);
        header.addView(
                bookingTitleText,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView listHint = text("Neueste zuerst", 12, Typeface.NORMAL);
        listHint.setTextColor(COLOR_TEXT_MUTED);
        listHint.setGravity(Gravity.END);
        header.addView(listHint);

        bookingsContainer = new LinearLayout(this);
        bookingsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(bookingsContainer, matchWrap());
    }

    private void buildFooter(LinearLayout root) {
        TextView info = text(
                "Startdatum: 16.08.2026  •  Tagesgrenze: 30\n"
                        + "Alle Tage beginnen automatisch bei 0. "
                        + "Grün bedeutet: innerhalb der laufenden Grenze. "
                        + "Rot bedeutet: Grenze überschritten.",
                12,
                Typeface.NORMAL
        );
        info.setTextColor(COLOR_TEXT_MUTED);
        info.setLineSpacing(0, 1.18f);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infoLp = matchWrap();
        infoLp.topMargin = dp(22);
        root.addView(info, infoLp);
    }

    private void navigateByDays(int days) {
        currentDate = LocalDate.now();
        LocalDate candidate = selectedDate.plusDays(days);

        if (candidate.isBefore(START_DATE) || candidate.isAfter(currentDate)) {
            return;
        }

        selectedDate = candidate;
        pendingQuickAmount = 0;
        refreshAll();
        pageScroll.post(() -> pageScroll.smoothScrollTo(0, 0));
    }

    private void refreshAll() {
        if (selectedDate == null) {
            return;
        }

        currentDate = LocalDate.now();
        updateDateNavigation();

        List<Booking> bookings = getBookings(selectedDate);
        long dayTotal = sumBookings(bookings);
        long runningTotal = getRunningSum(selectedDate);
        long limit = getLimit(selectedDate);
        long remaining = limit - runningTotal;

        dayValueText.setText(formatNumber(dayTotal));
        runningText.setText(formatNumber(runningTotal));
        limitText.setText(formatNumber(limit));
        differenceText.setText(formatNumber(remaining));

        boolean exceeded = runningTotal > limit;
        statusCard.setBackground(
                roundedBackground(
                        exceeded ? COLOR_RED_LIGHT : COLOR_GREEN_LIGHT,
                        18,
                        0,
                        Color.TRANSPARENT
                )
        );
        differenceText.setTextColor(exceeded ? COLOR_RED : COLOR_GREEN);

        boolean editable =
                !selectedDate.isBefore(START_DATE) && !selectedDate.isAfter(currentDate);
        setEditingEnabled(editable);
        refreshQuickSelection();
        renderBookings(bookings, dayTotal);
    }

    private void updateDateNavigation() {
        relativeDateText.setText(relativeDateLabel(selectedDate));
        dateText.setText(DISPLAY_DATE.format(selectedDate));

        boolean canGoBack = selectedDate.isAfter(START_DATE);
        boolean canGoForward = selectedDate.isBefore(currentDate);
        setButtonEnabled(previousDayButton, canGoBack);
        setButtonEnabled(nextDayButton, canGoForward);
    }

    private String relativeDateLabel(LocalDate date) {
        if (date.equals(currentDate)) {
            return "Heute";
        }
        if (date.equals(currentDate.minusDays(1))) {
            return "Gestern";
        }

        String day = DAY_NAME.format(date);
        if (day.isEmpty()) {
            return DISPLAY_DATE.format(date);
        }
        return day.substring(0, 1).toUpperCase(Locale.GERMAN) + day.substring(1);
    }

    private void setEditingEnabled(boolean enabled) {
        for (Button button : quickButtons.values()) {
            setButtonEnabled(button, enabled);
        }
        setButtonEnabled(quickEntryButton, enabled && pendingQuickAmount != 0);
    }

    private void addQuickButton(LinearLayout row, String label, int amount) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(19);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setStateListAnimator(null);
        button.setOnClickListener(v -> {
            pendingQuickAmount += amount;
            refreshQuickSelection();
        });

        quickButtons.put(amount, button);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        row.addView(button, lp);
    }

    private void refreshQuickSelection() {
        boolean editable = !selectedDate.isAfter(currentDate) && !selectedDate.isBefore(START_DATE);

        if (pendingQuickAmount == 0) {
            quickSelectionText.setText("Aktuelle Summe: 0");
            quickSelectionText.setTextColor(COLOR_TEXT_MUTED);
            quickSelectionText.setBackground(
                    roundedBackground(COLOR_NEUTRAL, 12, 0, Color.TRANSPARENT)
            );
        } else {
            quickSelectionText.setText(
                    "Aktuelle Summe: " + formatSignedNumber(pendingQuickAmount)
            );
            boolean positive = pendingQuickAmount > 0;
            quickSelectionText.setTextColor(positive ? COLOR_GREEN : COLOR_RED);
            quickSelectionText.setBackground(
                    roundedBackground(
                            positive ? COLOR_GREEN_LIGHT : COLOR_RED_LIGHT,
                            12,
                            0,
                            Color.TRANSPARENT
                    )
            );
        }

        for (Map.Entry<Integer, Button> entry : quickButtons.entrySet()) {
            int amount = entry.getKey();
            Button button = entry.getValue();
            boolean selected = false;
            boolean positive = amount > 0;

            int fill;
            int textColor;
            if (selected) {
                fill = positive ? COLOR_GREEN : COLOR_RED;
                textColor = Color.WHITE;
            } else {
                fill = positive ? COLOR_GREEN_LIGHT : COLOR_RED_LIGHT;
                textColor = positive ? COLOR_GREEN : COLOR_RED;
            }

            button.setTextColor(textColor);
            button.setBackground(rippleBackground(fill, 12));
            button.setAlpha(editable ? 1f : 0.38f);
        }

        setButtonEnabled(quickEntryButton, editable && pendingQuickAmount != 0);
    }

    private void addQuickBooking() {
        if (pendingQuickAmount == 0) {
            Toast.makeText(this, "Die aktuelle Summe ist 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        addBooking(
                selectedDate,
                pendingQuickAmount,
                "Schnellbuchung " + formatSignedNumber(pendingQuickAmount)
        );
        pendingQuickAmount = 0;
        refreshAll();
        Toast.makeText(this, "Buchung eingetragen.", Toast.LENGTH_SHORT).show();
    }

    private void addBooking(LocalDate date, long amount, String label) {
        List<Booking> bookings = getBookings(date);
        bookings.add(
                new Booking(
                        UUID.randomUUID().toString(),
                        amount,
                        label,
                        System.currentTimeMillis()
                )
        );
        saveBookings(date, bookings);
    }

    private void renderBookings(List<Booking> bookings, long dayTotal) {
        bookingsContainer.removeAllViews();

        int count = bookings.size();
        bookingTitleText.setText(
                count == 1 ? "1 Buchung" : count + " Buchungen"
        );

        if (bookings.isEmpty()) {
            LinearLayout emptyCard = createCard(COLOR_SURFACE);
            emptyCard.setGravity(Gravity.CENTER);
            emptyCard.setPadding(dp(18), dp(28), dp(18), dp(28));
            bookingsContainer.addView(emptyCard, matchWrap());

            TextView zero = text("0", 34, Typeface.BOLD);
            zero.setTextColor(COLOR_PRIMARY);
            zero.setGravity(Gravity.CENTER);
            emptyCard.addView(zero, matchWrap());

            TextView emptyTitle = text("Noch keine Buchungen", 16, Typeface.BOLD);
            emptyTitle.setTextColor(COLOR_TEXT);
            emptyTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleLp = matchWrap();
            titleLp.topMargin = dp(4);
            emptyCard.addView(emptyTitle, titleLp);

            TextView emptyText = text(
                    "Dieser Tag beginnt automatisch mit dem Tageswert 0.",
                    13,
                    Typeface.NORMAL
            );
            emptyText.setTextColor(COLOR_TEXT_MUTED);
            emptyText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams textLp = matchWrap();
            textLp.topMargin = dp(4);
            emptyCard.addView(emptyText, textLp);
            return;
        }

        for (int index = bookings.size() - 1; index >= 0; index--) {
            Booking booking = bookings.get(index);
            int displayNumber = index + 1;
            addBookingCard(booking, displayNumber);
        }

        LinearLayout totalCard = createCard(COLOR_PRIMARY_LIGHT);
        LinearLayout.LayoutParams totalLp = matchWrap();
        totalLp.topMargin = dp(10);
        bookingsContainer.addView(totalCard, totalLp);

        LinearLayout totalRow = new LinearLayout(this);
        totalRow.setOrientation(LinearLayout.HORIZONTAL);
        totalRow.setGravity(Gravity.CENTER_VERTICAL);
        totalCard.addView(totalRow, matchWrap());

        TextView totalLabel = text("Tageswert aus allen Buchungen", 14, Typeface.BOLD);
        totalLabel.setTextColor(COLOR_PRIMARY);
        totalRow.addView(
                totalLabel,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView totalValue = text(formatNumber(dayTotal), 25, Typeface.BOLD);
        totalValue.setTextColor(COLOR_PRIMARY);
        totalValue.setGravity(Gravity.END);
        totalRow.addView(totalValue);
    }

    private void addBookingCard(Booking booking, int displayNumber) {
        LinearLayout card = createCard(COLOR_SURFACE);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));

        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.bottomMargin = dp(10);
        bookingsContainer.addView(card, cardLp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, matchWrap());

        TextView amountText = text(formatSignedNumber(booking.amount), 29, Typeface.BOLD);
        amountText.setTextColor(
                booking.amount > 0 ? COLOR_GREEN : booking.amount < 0 ? COLOR_RED : COLOR_TEXT_MUTED
        );
        amountText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        top.addView(
                amountText,
                new LinearLayout.LayoutParams(dp(104), ViewGroup.LayoutParams.WRAP_CONTENT)
        );

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(8), 0, 0, 0);
        top.addView(
                details,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView bookingNumber = text("Buchung " + displayNumber, 12, Typeface.BOLD);
        bookingNumber.setTextColor(COLOR_PRIMARY);
        details.addView(bookingNumber, matchWrap());

        TextView labelText = text(booking.label, 14, Typeface.BOLD);
        labelText.setTextColor(COLOR_TEXT);
        labelText.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams labelLp = matchWrap();
        labelLp.topMargin = dp(2);
        details.addView(labelText, labelLp);

        if (booking.createdAt > 0L) {
            TextView timeText = text(
                    "Eingetragen um " + formatTime(booking.createdAt) + " Uhr",
                    12,
                    Typeface.NORMAL
            );
            timeText.setTextColor(COLOR_TEXT_MUTED);
            LinearLayout.LayoutParams timeLp = matchWrap();
            timeLp.topMargin = dp(2);
            details.addView(timeText, timeLp);
        }

        LinearLayout actions = createHorizontalRow();
        LinearLayout.LayoutParams actionsLp = matchWrap();
        actionsLp.topMargin = dp(12);
        card.addView(actions, actionsLp);

        Button editButton = createSecondaryButton("Bearbeiten", COLOR_PRIMARY, COLOR_PRIMARY_LIGHT);
        editButton.setOnClickListener(v -> showEditDialog(booking));
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        editLp.rightMargin = dp(5);
        actions.addView(editButton, editLp);

        Button deleteButton = createSecondaryButton("Löschen", COLOR_RED, COLOR_RED_LIGHT);
        deleteButton.setOnClickListener(v -> showDeleteDialog(booking));
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        deleteLp.leftMargin = dp(5);
        actions.addView(deleteButton, deleteLp);
    }

    private void showEditDialog(Booking booking) {
        EditText input = createDialogNumberInput(booking.amount);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(dp(22), dp(4), dp(22), 0);
        wrapper.addView(input, matchWrapHeight(dp(56)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Buchung bearbeiten")
                .setMessage(
                        "Neuen Betrag für " + DISPLAY_DATE.format(selectedDate) + " eingeben."
                )
                .setView(wrapper)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(COLOR_PRIMARY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(COLOR_TEXT_MUTED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String raw = input.getText().toString().trim();
                if (raw.isEmpty() || raw.equals("-") || raw.equals("+")) {
                    input.setError("Bitte eine ganze Zahl eingeben.");
                    return;
                }

                try {
                    long newAmount = Long.parseLong(raw);
                    updateBooking(booking.id, newAmount);
                    dialog.dismiss();
                    refreshAll();
                    Toast.makeText(this, "Buchung gespeichert.", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException exception) {
                    input.setError("Ungültige Zahl.");
                }
            });
            input.requestFocus();
            input.selectAll();
        });

        dialog.show();
    }

    private void showDeleteDialog(Booking booking) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Buchung löschen?")
                .setMessage(
                        formatSignedNumber(booking.amount)
                                + " vom "
                                + DISPLAY_DATE.format(selectedDate)
                                + " wird dauerhaft entfernt."
                )
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (ignored, which) -> {
                    deleteBooking(booking.id);
                    refreshAll();
                    Toast.makeText(this, "Buchung gelöscht.", Toast.LENGTH_SHORT).show();
                })
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(COLOR_RED);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(COLOR_TEXT_MUTED);
        });
        dialog.show();
    }

    private EditText createDialogNumberInput(long value) {
        EditText input = new EditText(this);
        input.setText(String.valueOf(value));
        input.setTextSize(20);
        input.setTextColor(COLOR_TEXT);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED
        );
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackground(roundedBackground(COLOR_SURFACE, 12, 1, COLOR_BORDER));
        return input;
    }

    private void updateBooking(String bookingId, long newAmount) {
        List<Booking> bookings = getBookings(selectedDate);
        for (Booking booking : bookings) {
            if (booking.id.equals(bookingId)) {
                booking.amount = newAmount;
                booking.label = "Manuell bearbeitete Buchung";
                booking.createdAt = System.currentTimeMillis();
                break;
            }
        }
        saveBookings(selectedDate, bookings);
    }

    private void deleteBooking(String bookingId) {
        List<Booking> bookings = getBookings(selectedDate);
        bookings.removeIf(booking -> booking.id.equals(bookingId));
        saveBookings(selectedDate, bookings);
    }

    private List<Booking> getBookings(LocalDate date) {
        String key = entriesKey(date);

        if (!prefs.contains(key)) {
            return migrateLegacyValue(date);
        }

        String raw = prefs.getString(key, "[]");
        List<Booking> bookings = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return bookings;
        }

        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject object = array.optJSONObject(index);
                if (object == null) {
                    continue;
                }
                Booking booking = Booking.fromJson(object);
                if (booking != null) {
                    bookings.add(booking);
                }
            }
        } catch (JSONException ignored) {
            Toast.makeText(
                    this,
                    "Eine gespeicherte Buchung konnte nicht gelesen werden.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        return bookings;
    }

    private List<Booking> migrateLegacyValue(LocalDate date) {
        List<Booking> bookings = new ArrayList<>();
        String legacyKey = legacyDayKey(date);

        if (prefs.contains(legacyKey)) {
            long legacyValue = prefs.getInt(legacyKey, 0);
            if (legacyValue != 0L) {
                bookings.add(
                        new Booking(
                                "legacy-" + KEY_DATE.format(date),
                                legacyValue,
                                "Übernommener Tageswert aus Version 1",
                                0L
                        )
                );
            }
            saveBookings(date, bookings);
        }

        return bookings;
    }

    private void saveBookings(LocalDate date, List<Booking> bookings) {
        JSONArray array = new JSONArray();
        for (Booking booking : bookings) {
            array.put(booking.toJson());
        }
        prefs.edit().putString(entriesKey(date), array.toString()).apply();
    }

    private long getDayTotal(LocalDate date) {
        return sumBookings(getBookings(date));
    }

    private long sumBookings(List<Booking> bookings) {
        long sum = 0L;
        for (Booking booking : bookings) {
            sum += booking.amount;
        }
        return sum;
    }

    private long getRunningSum(LocalDate throughDate) {
        if (throughDate.isBefore(START_DATE)) {
            return 0L;
        }

        long sum = 0L;
        LocalDate date = START_DATE;
        while (!date.isAfter(throughDate)) {
            sum += getDayTotal(date);
            date = date.plusDays(1);
        }
        return sum;
    }

    private long getLimit(LocalDate throughDate) {
        if (throughDate.isBefore(START_DATE)) {
            return 0L;
        }
        long days = ChronoUnit.DAYS.between(START_DATE, throughDate) + 1L;
        return days * DAILY_LIMIT;
    }

    private String entriesKey(LocalDate date) {
        return "entries_" + KEY_DATE.format(date);
    }

    private String legacyDayKey(LocalDate date) {
        return "day_" + KEY_DATE.format(date);
    }

    private String formatTime(long timestamp) {
        return DISPLAY_TIME.format(
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        );
    }

    private String formatNumber(long value) {
        return String.format(Locale.GERMAN, "%,d", value);
    }

    private String formatSignedNumber(long value) {
        if (value > 0L) {
            return "+" + formatNumber(value);
        }
        return formatNumber(value);
    }

    private LinearLayout createCard(int fillColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundedBackground(fillColor, 18, 1, Color.argb(22, 60, 70, 90)));
        card.setElevation(dp(2));
        return card;
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private Button createNavigationButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(27);
        button.setTextColor(COLOR_PRIMARY);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, dp(2));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setStateListAnimator(null);
        button.setBackground(rippleBackground(COLOR_PRIMARY_LIGHT, 16));
        return button;
    }

    private Button createPrimaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setStateListAnimator(null);
        button.setBackground(rippleBackground(COLOR_PRIMARY, 13));
        return button;
    }

    private Button createSecondaryButton(String label, int textColor, int fillColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setStateListAnimator(null);
        button.setBackground(rippleBackground(fillColor, 12));
        return button;
    }

    private TextView addStat(LinearLayout row, String label, String initialValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(3), 0, dp(3), 0);
        row.addView(
                box,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView labelText = text(label, 12, Typeface.NORMAL);
        labelText.setTextColor(COLOR_TEXT_MUTED);
        labelText.setGravity(Gravity.CENTER);
        box.addView(labelText, matchWrap());

        TextView valueText = text(initialValue, 19, Typeface.BOLD);
        valueText.setTextColor(COLOR_TEXT);
        valueText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams valueLp = matchWrap();
        valueLp.topMargin = dp(2);
        box.addView(valueText, valueLp);
        return valueText;
    }

    private TextView text(String value, int sizeSp, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.38f);
    }

    private Drawable roundedBackground(
            int fillColor,
            int radiusDp,
            int strokeWidthDp,
            int strokeColor
    ) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0) {
            background.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return background;
    }

    private Drawable rippleBackground(int fillColor, int radiusDp) {
        Drawable content = roundedBackground(fillColor, radiusDp, 0, Color.TRANSPARENT);
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(45, 20, 30, 45)),
                content,
                null
        );
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWrapHeight(int heightPx) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Booking {
        private final String id;
        private long amount;
        private String label;
        private long createdAt;

        private Booking(String id, long amount, String label, long createdAt) {
            this.id = id;
            this.amount = amount;
            this.label = label;
            this.createdAt = createdAt;
        }

        private JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("amount", amount);
                object.put("label", label);
                object.put("createdAt", createdAt);
            } catch (JSONException ignored) {
                // All stored values are supported JSON primitives.
            }
            return object;
        }

        private static Booking fromJson(JSONObject object) {
            String id = object.optString("id", "");
            if (id.isEmpty()) {
                return null;
            }

            long amount = object.optLong("amount", 0L);
            String label = object.optString("label", "Buchung");
            long createdAt = object.optLong("createdAt", 0L);
            return new Booking(id, amount, label, createdAt);
        }
    }
}
