package com.brace.examverse.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.brace.examverse.R;
import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class LiveWidgetConfigActivity extends Activity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); ThemeManager.applyWindow(this, getWindow());
        setResult(RESULT_CANCELED);
        Intent intent = getIntent(); Bundle extras = intent.getExtras(); if (extras != null) appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        build();
    }

    private void build() {
        ThemeManager.Palette p = ThemeManager.palette(this); List<Exam> exams = new ExamRepository(this).getUpcomingExams();
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(26), dp(22), dp(26)); box.setBackgroundColor(p.bg);
        TextView h = new TextView(this); h.setText("Choose the exam"); h.setTextSize(26); h.setTextColor(p.text); box.addView(h);
        TextView sub = new TextView(this); sub.setText(exams.isEmpty() ? "Add an exam in ExamVerse first." : "This widget will tick down live to the selected exam."); sub.setTextColor(p.muted); sub.setTextSize(14); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); sp.topMargin = dp(8); sp.bottomMargin = dp(18); box.addView(sub, sp);
        if (exams.isEmpty()) { Button close = new Button(this); close.setText("Close"); close.setOnClickListener(v -> finish()); box.addView(close); setContentView(box); return; }
        Spinner spinner = new Spinner(this); List<String> names = new ArrayList<>(); for (Exam e : exams) names.add(e.subject + " — " + e.title); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names); a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(a); box.addView(spinner);
        Button save = new Button(this); save.setText("Add live widget"); save.setAllCaps(false); save.setTextColor(android.graphics.Color.WHITE); save.setBackground(ThemeManager.rounded(p.primary, dp(18))); save.setOnClickListener(v -> {
            Exam e = exams.get(spinner.getSelectedItemPosition()); SharedPreferences prefs = getSharedPreferences(LiveExamWidgetProvider.PREFS, MODE_PRIVATE); prefs.edit().putLong("exam_" + appWidgetId, e.id).apply();
            AppWidgetManager manager = AppWidgetManager.getInstance(this); LiveExamWidgetProvider.updateWidgets(this, manager, new int[]{appWidgetId}); Intent result = new Intent(); result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId); setResult(RESULT_OK, result); finish();
        }); LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)); bp.topMargin = dp(20); box.addView(save, bp);
        setContentView(box);
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
