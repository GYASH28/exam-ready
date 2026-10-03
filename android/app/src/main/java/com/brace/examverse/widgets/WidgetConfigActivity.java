package com.brace.examverse.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class WidgetConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean live = false;
    private List<Exam> exams = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ThemeManager.applyWindow(this, getWindow());
        setResult(RESULT_CANCELED);

        Bundle e = getIntent().getExtras();
        if (e != null) widgetId = e.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
        live = info != null && info.provider.getClassName().contains("LiveExamWidgetProvider");
        build();
    }

    private void build() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        WidgetStyleStore.Style current = WidgetStyleStore.get(this, widgetId);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.setBackgroundColor(p.bg);
        scroll.addView(root);

        TextView kicker = t("WIDGET STUDIO", 10, p.primary, true);
        kicker.setLetterSpacing(.14f);
        root.addView(kicker);
        root.addView(t("Build your glass tile", 28, p.text, true), top(2));
        root.addView(t("Every widget can have its own theme, transparency, accent strength, corner shape and information density.", 12.5f, p.muted, false), top(5));

        root.addView(label("Glass theme", p), top(18));
        String[] themes = {
                "Auto · follows app",
                "Shinobi Ember",
                "Saiyan Energy",
                "Soul Reaper Noir",
                "Clear Frost"
        };
        String[] keys = {"auto", "naruto", "dragonball", "bleach", "frost"};
        Spinner theme = spinner(themes, p);
        int ti = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(current.theme)) ti = i;
        theme.setSelection(ti);
        root.addView(theme, topHeight(6, 52));

        LinearLayout glassCard = card(p);

        TextView opacityLabel = t("Glass opacity  ·  " + current.opacity, 12, p.text, true);
        glassCard.addView(opacityLabel);
        SeekBar opacity = new SeekBar(this);
        opacity.setMax(170);
        opacity.setProgress(Math.max(0, current.opacity - 70));
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                opacityLabel.setText("Glass opacity  ·  " + (v + 70));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        glassCard.addView(opacity, top(3));

        TextView accentLabel = t("Theme glow  ·  " + current.accentStrength + "%", 12, p.text, true);
        glassCard.addView(accentLabel, top(10));
        SeekBar accent = new SeekBar(this);
        accent.setMax(100);
        accent.setProgress(current.accentStrength);
        accent.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                accentLabel.setText("Theme glow  ·  " + v + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        glassCard.addView(accent, top(3));

        TextView cornerLabel = t("Corner softness  ·  " + current.corner + "dp", 12, p.text, true);
        glassCard.addView(cornerLabel, top(10));
        SeekBar corner = new SeekBar(this);
        corner.setMax(34);
        corner.setProgress(Math.max(0, current.corner - 14));
        corner.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                cornerLabel.setText("Corner softness  ·  " + (v + 14) + "dp");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        glassCard.addView(corner, top(3));
        root.addView(glassCard, top(10));

        root.addView(label("Information density", p), top(16));
        String[] densityLabels = {"Detailed", "Compact"};
        Spinner density = spinner(densityLabels, p);
        density.setSelection("compact".equals(current.density) ? 1 : 0);
        root.addView(density, topHeight(6, 52));

        Spinner examSpinner = null;
        if (live) {
            exams = new ExamRepository(this).getUpcomingExams();
            List<String> names = new ArrayList<>();
            for (Exam x : exams) names.add(x.subject + " — " + x.title);

            examSpinner = spinner(
                    (names.isEmpty() ? java.util.Collections.singletonList("No exam available") : names).toArray(new String[0]),
                    p
            );
            for (int i = 0; i < exams.size(); i++) if (exams.get(i).id == current.examId) examSpinner.setSelection(i);

            root.addView(label("Countdown exam", p), top(16));
            root.addView(examSpinner, topHeight(6, 52));
        }

        LinearLayout hint = card(p);
        hint.addView(t("Preview logic", 12.5f, p.text, true));
        hint.addView(t("Opacity controls the frosted base. Theme glow changes the colored aura. Corner softness changes the silhouette. Compact mode hides secondary details.", 11, p.muted, false), top(4));
        root.addView(hint, top(14));

        Spinner finalExam = examSpinner;
        Button save = button("Save widget style", p.primary, contrast(p.primary));
        save.setOnClickListener(v -> {
            long examId = current.examId;
            if (live && !exams.isEmpty() && finalExam != null) {
                examId = exams.get(finalExam.getSelectedItemPosition()).id;
            }

            WidgetStyleStore.save(
                    this,
                    widgetId,
                    keys[theme.getSelectedItemPosition()],
                    opacity.getProgress() + 70,
                    density.getSelectedItemPosition() == 1 ? "compact" : "detailed",
                    examId,
                    corner.getProgress() + 14,
                    accent.getProgress()
            );

            WidgetUpdater.updateAll(this);
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
            finish();
        });
        root.addView(save, topHeight(20, 58));

        setContentView(scroll);
    }

    private TextView label(String s, ThemeManager.Palette p) {
        TextView v = t(s.toUpperCase(), 10, p.primary, true);
        v.setLetterSpacing(.10f);
        return v;
    }

    private Spinner spinner(String[] values, ThemeManager.Palette p) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(p.text);
                    ((TextView) v).setTextSize(12.5f);
                    v.setPadding(dp(12), 0, dp(12), 0);
                }
                return v;
            }

            @Override public android.view.View getDropDownView(int position, android.view.View convertView, ViewGroup parent) {
                android.view.View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(p.text);
                    v.setBackgroundColor(p.surface);
                    v.setPadding(dp(12), dp(11), dp(12), dp(11));
                }
                return v;
            }
        };
        s.setAdapter(a);
        s.setBackground(ThemeManager.glass(this, dp(16), true));
        return s;
    }

    private LinearLayout card(ThemeManager.Palette p) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(15), dp(14), dp(15), dp(14));
        l.setBackground(ThemeManager.glass(this, dp(20), false));
        return l;
    }

    private TextView t(String s, float sp, int c, boolean b) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(c);
        if (b) v.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        return v;
    }

    private Button button(String s, int bg, int fg) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        b.setBackground(ThemeManager.rounded(bg, dp(18)));
        b.setStateListAnimator(null);
        return b;
    }

    private LinearLayout.LayoutParams top(int d) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(d);
        return p;
    }

    private LinearLayout.LayoutParams topHeight(int d, int h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(h));
        p.topMargin = dp(d);
        return p;
    }

    private int contrast(int color) {
        double y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000.0;
        return y > 160 ? Color.rgb(15, 23, 42) : Color.WHITE;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
