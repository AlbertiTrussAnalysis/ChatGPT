package com.thomas.punktetracker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MainActivity extends Activity {
    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 16);
    private static final int DAILY_LIMIT = 30;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private SharedPreferences prefs;
    private LocalDate today;
    private TextView dateText, todayValueText, runningText, limitText, differenceText;
    private LinearLayout statusCard;
    private EditText exactInput;
    private Button[] changeButtons;
    private Button setExactButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("daily_values", MODE_PRIVATE);
        buildUi();
        refreshDateAndValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDateAndValues();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 250));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Punkte Tracker", 28, Typeface.BOLD);
        title.setTextColor(Color.rgb(30, 36, 45));
        root.addView(title);

        dateText = text("", 17, Typeface.NORMAL);
        dateText.setTextColor(Color.rgb(90, 97, 108));
        LinearLayout.LayoutParams dateLp = lpMatchWrap();
        dateLp.topMargin = dp(4); dateLp.bottomMargin = dp(18);
        root.addView(dateText, dateLp);

        statusCard = verticalCard();
        root.addView(statusCard, lpMatchWrap());
        TextView todayLabel = text("Heute", 15, Typeface.BOLD);
        todayLabel.setTextColor(Color.rgb(75, 81, 91));
        statusCard.addView(todayLabel);
        todayValueText = text("0", 46, Typeface.BOLD);
        todayValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        todayValueText.setTextColor(Color.rgb(25, 30, 38));
        statusCard.addView(todayValueText, lpMatchWrap());

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL); stats.setGravity(Gravity.CENTER);
        statusCard.addView(stats, lpMatchWrap());
        runningText = addStat(stats, "Summe", "0");
        limitText = addStat(stats, "Grenze", "30");
        differenceText = addStat(stats, "Differenz", "30");

        TextView quickTitle = text("Schnell ändern", 18, Typeface.BOLD);
        quickTitle.setTextColor(Color.rgb(40, 46, 56));
        LinearLayout.LayoutParams sectionLp = lpMatchWrap();
        sectionLp.topMargin = dp(22); sectionLp.bottomMargin = dp(10);
        root.addView(quickTitle, sectionLp);

        LinearLayout plusRow = buttonRow(); root.addView(plusRow, lpMatchWrap());
        Button plus1 = actionButton("+1", true, 1), plus5 = actionButton("+5", true, 5), plus10 = actionButton("+10", true, 10);
        plusRow.addView(plus1, weightedButtonLp()); plusRow.addView(plus5, weightedButtonLp()); plusRow.addView(plus10, weightedButtonLp());

        LinearLayout minusRow = buttonRow();
        LinearLayout.LayoutParams minusLp = lpMatchWrap(); minusLp.topMargin = dp(10); root.addView(minusRow, minusLp);
        Button minus1 = actionButton("−1", false, -1), minus5 = actionButton("−5", false, -5), minus10 = actionButton("−10", false, -10);
        minusRow.addView(minus1, weightedButtonLp()); minusRow.addView(minus5, weightedButtonLp()); minusRow.addView(minus10, weightedButtonLp());
        changeButtons = new Button[]{plus1, plus5, plus10, minus1, minus5, minus10};

        TextView exactTitle = text("Exakten Tageswert setzen", 18, Typeface.BOLD);
        exactTitle.setTextColor(Color.rgb(40, 46, 56));
        LinearLayout.LayoutParams exactTitleLp = lpMatchWrap(); exactTitleLp.topMargin = dp(24); exactTitleLp.bottomMargin = dp(10);
        root.addView(exactTitle, exactTitleLp);

        LinearLayout exactRow = new LinearLayout(this); exactRow.setOrientation(LinearLayout.HORIZONTAL); exactRow.setGravity(Gravity.CENTER_VERTICAL); root.addView(exactRow, lpMatchWrap());
        exactInput = new EditText(this); exactInput.setTextSize(20); exactInput.setSingleLine(true); exactInput.setHint("z. B. 12");
        exactInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED); exactInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, dp(58), 1f); inputLp.rightMargin = dp(10); exactRow.addView(exactInput, inputLp);

        setExactButton = new Button(this); setExactButton.setText("SETZEN"); setExactButton.setTextColor(Color.WHITE); setExactButton.setTextSize(15);
        setExactButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD); setExactButton.setBackgroundResource(R.drawable.button_primary); setExactButton.setOnClickListener(v -> setExactValue());
        exactRow.addView(setExactButton, new LinearLayout.LayoutParams(dp(116), dp(58)));

        TextView info = text("Start: 16.08.2026 · Tagesgrenze: 30\nGrün = laufende Summe liegt innerhalb der Grenze. Rot = Grenze überschritten.", 14, Typeface.NORMAL);
        info.setTextColor(Color.rgb(95, 102, 113)); info.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams infoLp = lpMatchWrap(); infoLp.topMargin = dp(24); root.addView(info, infoLp);
        setContentView(scroll);
    }

    private LinearLayout verticalCard() { LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18), dp(16), dp(18), dp(18)); card.setBackgroundResource(R.drawable.card); return card; }
    private LinearLayout buttonRow() { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER); return row; }
    private Button actionButton(String label, boolean plus, int amount) { Button b = new Button(this); b.setText(label); b.setTextSize(22); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setTextColor(Color.rgb(35, 40, 48)); b.setBackgroundResource(plus ? R.drawable.button_plus : R.drawable.button_minus); b.setOnClickListener(v -> changeToday(amount)); return b; }
    private TextView addStat(LinearLayout row, String label, String initialValue) { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); row.addView(box, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)); TextView l = text(label, 13, Typeface.NORMAL); l.setTextColor(Color.rgb(90, 97, 108)); l.setGravity(Gravity.CENTER); box.addView(l); TextView value = text(initialValue, 21, Typeface.BOLD); value.setTextColor(Color.rgb(35, 40, 48)); value.setGravity(Gravity.CENTER); box.addView(value); return value; }
    private TextView text(String value, int sp, int style) { TextView t = new TextView(this); t.setText(value); t.setTextSize(sp); t.setTypeface(Typeface.DEFAULT, style); return t; }

    private void refreshDateAndValues() {
        today = LocalDate.now(); dateText.setText("Heute · " + DISPLAY_DATE.format(today));
        boolean active = !today.isBefore(START_DATE);
        for (Button b : changeButtons) b.setEnabled(active); exactInput.setEnabled(active); setExactButton.setEnabled(active);
        if (!active) { todayValueText.setText("–"); runningText.setText("0"); limitText.setText("0"); differenceText.setText("0"); setStatusColor(Color.rgb(232, 234, 238)); return; }
        int todayValue = getValue(today), running = getRunningSum(today), limit = getLimit(today), remaining = limit - running;
        todayValueText.setText(String.valueOf(todayValue)); runningText.setText(String.valueOf(running)); limitText.setText(String.valueOf(limit)); differenceText.setText(String.valueOf(remaining));
        if (running > limit) { setStatusColor(Color.rgb(255, 221, 221)); differenceText.setTextColor(Color.rgb(155, 35, 35)); }
        else { setStatusColor(Color.rgb(218, 244, 224)); differenceText.setTextColor(Color.rgb(32, 115, 61)); }
    }

    private void changeToday(int delta) { if (today.isBefore(START_DATE)) return; saveValue(today, getValue(today) + delta); refreshDateAndValues(); }
    private void setExactValue() { String raw = exactInput.getText().toString().trim(); if (raw.isEmpty() || raw.equals("-") || raw.equals("+")) { Toast.makeText(this, "Bitte eine ganze Zahl eingeben.", Toast.LENGTH_SHORT).show(); return; } try { saveValue(today, Integer.parseInt(raw)); exactInput.setText(""); refreshDateAndValues(); } catch (NumberFormatException e) { Toast.makeText(this, "Ungültige Zahl.", Toast.LENGTH_SHORT).show(); } }
    private int getValue(LocalDate date) { return prefs.getInt("day_" + KEY_DATE.format(date), 0); }
    private void saveValue(LocalDate date, int value) { prefs.edit().putInt("day_" + KEY_DATE.format(date), value).apply(); }
    private int getRunningSum(LocalDate through) { int sum = 0; LocalDate d = START_DATE; while (!d.isAfter(through)) { sum += getValue(d); d = d.plusDays(1); } return sum; }
    private int getLimit(LocalDate through) { return (int) (ChronoUnit.DAYS.between(START_DATE, through) + 1) * DAILY_LIMIT; }
    private void setStatusColor(int color) { GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(18)); statusCard.setBackground(bg); }
    private LinearLayout.LayoutParams weightedButtonLp() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(64), 1f); p.leftMargin = dp(4); p.rightMargin = dp(4); return p; }
    private LinearLayout.LayoutParams lpMatchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
