package com.brace.examverse;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.theme.ThemeManager;
import com.brace.examverse.widgets.WidgetUpdater;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddExamActivity extends Activity {
    private ExamRepository repo;
    private EditText title, subject, notes, targetScore;
    private Spinner category, priority, difficulty, reminderHours;
    private Switch remind;
    private Button dateButton, timeButton;
    private final Calendar calendar = Calendar.getInstance();
    private long editingId = -1;

    private final String[] cats = {"Exam", "Quiz", "Midterm", "Final", "Practical", "Entrance", "Certification"};
    private final String[] priorities = {"1 · Low", "2 · Normal", "3 · Important", "4 · High", "5 · Critical"};
    private final String[] difficulties = {"1 · Easy", "2 · Light", "3 · Medium", "4 · Hard", "5 · Brutal"};
    private final String[] reminders = {"6 hours before", "12 hours before", "24 hours before", "48 hours before", "72 hours before"};
    private final int[] reminderValues = {6, 12, 24, 48, 72};

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new ExamRepository(this); ThemeManager.applyWindow(this, getWindow());
        editingId = getIntent().getLongExtra("exam_id", -1);
        calendar.add(Calendar.DAY_OF_MONTH, 7); calendar.set(Calendar.HOUR_OF_DAY, 9); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0);
        buildUi(); if (editingId != -1) loadExam(editingId);
    }

    private void buildUi() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(p.bg);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20), dp(18), dp(20), dp(32)); scroll.addView(box);

        TextView heading = label(editingId == -1 ? "Create exam mission" : "Edit exam mission", 28, p.text, true); box.addView(heading);
        TextView sub = label("Give ExamVerse enough detail to calculate pressure, readiness and a smarter revision plan.", 13, p.muted, false);
        LinearLayout.LayoutParams sp = params(); sp.topMargin = dp(5); sp.bottomMargin = dp(18); box.addView(sub, sp);

        box.addView(section("Basics", p));
        title = field("Exam title · Unit Test 2", p); box.addView(title, fieldParams());
        subject = field("Subject · Digital Techniques", p); box.addView(subject, fieldParams());
        category = spinner(cats); box.addView(category, spinnerParams());

        LinearLayout dt = new LinearLayout(this); dt.setOrientation(LinearLayout.HORIZONTAL);
        dateButton = button("Choose date", p.surfaceAlt, p.text); dateButton.setOnClickListener(v -> chooseDate());
        timeButton = button("Choose time", p.surfaceAlt, p.text); timeButton.setOnClickListener(v -> chooseTime());
        LinearLayout.LayoutParams d1 = new LinearLayout.LayoutParams(0, dp(54), 1f); d1.rightMargin = dp(6); dt.addView(dateButton, d1);
        LinearLayout.LayoutParams d2 = new LinearLayout.LayoutParams(0, dp(54), 1f); d2.leftMargin = dp(6); dt.addView(timeButton, d2); box.addView(dt, fieldParams());

        box.addView(section("Pressure profile", p));
        priority = spinner(priorities); priority.setSelection(2); box.addView(priority, spinnerParams());
        difficulty = spinner(difficulties); difficulty.setSelection(2); box.addView(difficulty, spinnerParams());
        targetScore = field("Target score % · 80", p); targetScore.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(targetScore, fieldParams());

        box.addView(section("Reminder & notes", p));
        remind = new Switch(this); remind.setText("Enable exam reminder"); remind.setTextColor(p.text); remind.setChecked(true); box.addView(remind, fieldParams());
        reminderHours = spinner(reminders); reminderHours.setSelection(2); box.addView(reminderHours, spinnerParams());
        notes = field("Room, chapters, instructions, what to bring…", p); notes.setMinLines(4); notes.setGravity(Gravity.TOP); box.addView(notes, fieldParams());

        Button save = button(editingId == -1 ? "Create mission" : "Save changes", p.primary, contrast(p.primary)); save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams sv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)); sv.topMargin = dp(10); box.addView(save, sv);
        Button cancel = button("Cancel", Color.TRANSPARENT, p.muted); cancel.setOnClickListener(v -> finish()); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)); cp.topMargin = dp(4); box.addView(cancel, cp);

        updateDateTimeButtons(); setContentView(scroll);
    }

    private void loadExam(long id) {
        Exam e = repo.getExam(id); if (e == null) return;
        title.setText(e.title); subject.setText(e.subject); notes.setText(e.notes); remind.setChecked(e.remind); calendar.setTimeInMillis(e.timeMillis);
        select(category, cats, e.category); priority.setSelection(Math.max(0, Math.min(4, e.priority - 1))); difficulty.setSelection(Math.max(0, Math.min(4, e.difficulty - 1)));
        targetScore.setText(String.valueOf(e.targetScore));
        int reminderIndex = 2; for (int i = 0; i < reminderValues.length; i++) if (reminderValues[i] == e.reminderHours) reminderIndex = i; reminderHours.setSelection(reminderIndex);
        updateDateTimeButtons();
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> { calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, day); updateDateTimeButtons(); },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void chooseTime() {
        new TimePickerDialog(this, (view, hour, minute) -> { calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0); updateDateTimeButtons(); },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }
    private void updateDateTimeButtons() {
        if (dateButton == null) return;
        dateButton.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(new Date(calendar.getTimeInMillis())));
        timeButton.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(calendar.getTimeInMillis())));
    }

    private void save() {
        String t = title.getText().toString().trim(), s = subject.getText().toString().trim();
        if (t.isEmpty()) { title.setError("Give the exam a name"); return; }
        if (s.isEmpty()) { subject.setError("Add a subject"); return; }
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) { Toast.makeText(this, "Choose a future exam time", Toast.LENGTH_SHORT).show(); return; }
        int target = 80; try { target = Integer.parseInt(targetScore.getText().toString().trim()); } catch (Exception ignored) {}
        target = Math.max(1, Math.min(100, target));
        long id = editingId == -1 ? System.currentTimeMillis() : editingId; Exam old = editingId == -1 ? null : repo.getExam(editingId);
        Exam e = new Exam(id, t, s, category.getSelectedItem().toString(), notes.getText().toString().trim(), calendar.getTimeInMillis(), old != null && old.completed,
                remind.isChecked(), reminderValues[reminderHours.getSelectedItemPosition()], priority.getSelectedItemPosition() + 1, difficulty.getSelectedItemPosition() + 1, target);
        repo.saveExam(e); ReminderScheduler.cancel(this, e.id); if (e.remind) ReminderScheduler.schedule(this, e); WidgetUpdater.updateAll(this);
        Toast.makeText(this, editingId == -1 ? "Mission created" : "Mission updated", Toast.LENGTH_SHORT).show(); finish();
    }

    private Spinner spinner(String[] items) {
        ThemeManager.Palette p = ThemeManager.palette(this); Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) { android.view.View v = super.getView(position, convertView, parent); if (v instanceof TextView) { ((TextView)v).setTextColor(p.text); ((TextView)v).setTextSize(13); v.setPadding(dp(10),0,dp(10),0); } return v; }
            @Override public android.view.View getDropDownView(int position, android.view.View convertView, ViewGroup parent) { android.view.View v = super.getDropDownView(position, convertView, parent); if (v instanceof TextView) { ((TextView)v).setTextColor(p.text); v.setBackgroundColor(p.surface); v.setPadding(dp(12),dp(11),dp(12),dp(11)); } return v; }
        }; s.setAdapter(a); s.setBackground(ThemeManager.rounded(p.surface, dp(16))); return s;
    }
    private void select(Spinner s, String[] items, String value) { for (int i = 0; i < items.length; i++) if (items[i].equals(value)) { s.setSelection(i); return; } }
    private TextView section(String value, ThemeManager.Palette p) { TextView t = label(value.toUpperCase(Locale.getDefault()), 12, p.primary, true); LinearLayout.LayoutParams lp = params(); lp.topMargin = dp(8); lp.bottomMargin = dp(8); t.setLayoutParams(lp); return t; }
    private EditText field(String hint, ThemeManager.Palette p) { EditText e = new EditText(this); e.setHint(hint); e.setTextColor(p.text); e.setHintTextColor(p.muted); e.setTextSize(15); e.setPadding(dp(14), dp(11), dp(14), dp(11)); e.setBackground(ThemeManager.outlined(p.surface, Color.argb(35, 120,120,120), dp(16), dp(1))); return e; }
    private Button button(String s, int bg, int fg) { Button b = new Button(this); b.setText(s); b.setTextColor(fg); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(null, Typeface.BOLD); if (bg == Color.TRANSPARENT) b.setBackgroundColor(Color.TRANSPARENT); else b.setBackground(ThemeManager.rounded(bg, dp(17))); return b; }
    private TextView label(String s, int sp, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL); return t; }
    private LinearLayout.LayoutParams params() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams fieldParams() { LinearLayout.LayoutParams p = params(); p.bottomMargin = dp(12); return p; }
    private LinearLayout.LayoutParams spinnerParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)); p.bottomMargin = dp(12); return p; }
    private int contrast(int bg) { double y = (299 * Color.red(bg) + 587 * Color.green(bg) + 114 * Color.blue(bg)) / 1000.0; return y > 160 ? Color.rgb(17,24,39) : Color.WHITE; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
