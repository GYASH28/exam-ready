package com.brace.examverse.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.brace.examverse.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class WidgetStudioActivity extends Activity {
    static class Item {
        final int id;
        final String name;
        Item(int id, String name) { this.id = id; this.name = name; }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ThemeManager.applyWindow(this, getWindow());
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));
        root.setBackgroundColor(p.bg);
        sc.addView(root);

        TextView kicker = t("WIDGET STUDIO", 10, p.primary, true);
        kicker.setLetterSpacing(.14f);
        root.addView(kicker);
        root.addView(t("Your home screen, upgraded.", 28, p.text, true), top(2));
        root.addView(t("Every installed widget gets an independent glass look — theme, opacity, glow, corner softness, density and exam target.", 12.5f, p.muted, false), top(5));

        List<Item> items = items();

        LinearLayout summary = card(p);
        LinearLayout sr = new LinearLayout(this);
        sr.setOrientation(LinearLayout.HORIZONTAL);
        sr.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout st = new LinearLayout(this);
        st.setOrientation(LinearLayout.VERTICAL);
        st.addView(t(items.size() + " installed", 20, p.text, true));
        st.addView(t(items.isEmpty() ? "Add one from your Android home screen." : "Tap a tile below to tune it.", 10.8f, p.muted, false), top(2));
        sr.addView(st, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = t("GLASS V4", 9.5f, p.primary, true);
        badge.setLetterSpacing(.08f);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));
        badge.setBackground(ThemeManager.glowChip(this, dp(14)));
        sr.addView(badge);
        summary.addView(sr);
        root.addView(summary, top(16));

        if (items.isEmpty()) {
            LinearLayout empty = card(p);
            empty.addView(t("No ExamVerse widgets are installed yet.", 14.5f, p.text, true));
            empty.addView(t("Long-press your Android home screen → Widgets → ExamVerse. Pick a widget and its customizer will open automatically.", 11.5f, p.muted, false), top(5));
            root.addView(empty, top(10));
        } else {
            for (Item it : items) {
                WidgetStyleStore.Style s = WidgetStyleStore.get(this, it.id);
                LinearLayout c = card(p);

                LinearLayout head = new LinearLayout(this);
                head.setOrientation(LinearLayout.HORIZONTAL);
                head.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout names = new LinearLayout(this);
                names.setOrientation(LinearLayout.VERTICAL);
                names.addView(t(it.name, 15, p.text, true));
                names.addView(t(themeName(s.theme) + "  ·  " + s.density, 10.8f, p.muted, false), top(2));
                head.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView id = t("#" + it.id, 9.5f, p.muted, true);
                head.addView(id);
                c.addView(head);

                LinearLayout specs = new LinearLayout(this);
                specs.setOrientation(LinearLayout.HORIZONTAL);
                specs.setGravity(Gravity.CENTER_VERTICAL);
                specs.addView(spec("opacity", String.valueOf(s.opacity), p), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                specs.addView(spec("glow", s.accentStrength + "%", p), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                specs.addView(spec("corner", s.corner + "dp", p), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                c.addView(specs, top(10));

                Button edit = button("Customize glass style", p.primary, contrast(p.primary));
                edit.setOnClickListener(v -> {
                    Intent i = new Intent(this, WidgetConfigActivity.class);
                    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, it.id);
                    startActivity(i);
                });
                c.addView(edit, topHeight(11, 46));
                root.addView(c, top(10));
            }
        }

        LinearLayout help = card(p);
        help.addView(t("Widget tips", 13, p.text, true));
        help.addView(t("• Use Compact for small 2×1 tiles\n• Raise opacity on busy wallpapers\n• Lower glow for a clean minimal setup\n• Use softer corners for a true glass-card look", 11.2f, p.muted, false), top(5));
        root.addView(help, top(12));

        setContentView(sc);
    }

    private LinearLayout spec(String label, String value, ThemeManager.Palette p) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(t(label.toUpperCase(), 8.5f, p.muted, true));
        l.addView(t(value, 13.5f, p.text, true), top(1));
        return l;
    }

    private List<Item> items() {
        List<Item> out = new ArrayList<>();
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        add(out, m, NextExamWidgetProvider.class, "Next Exam");
        add(out, m, LiveExamWidgetProvider.class, "Live Countdown");
        add(out, m, UpcomingExamWidgetProvider.class, "Upcoming Exams");
        add(out, m, DailyMissionWidgetProvider.class, "Daily Mission");
        return out;
    }

    private void add(List<Item> out, AppWidgetManager m, Class<?> cls, String name) {
        int[] ids = m.getAppWidgetIds(new ComponentName(this, cls));
        for (int id : ids) out.add(new Item(id, name));
    }

    private String themeName(String t) {
        if ("naruto".equals(t)) return "Shinobi Ember";
        if ("dragonball".equals(t)) return "Saiyan Energy";
        if ("bleach".equals(t)) return "Soul Reaper Noir";
        if ("frost".equals(t)) return "Clear Frost";
        return "Auto";
    }

    private LinearLayout card(ThemeManager.Palette p) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(15), dp(16), dp(15));
        l.setBackground(ThemeManager.glass(this, dp(22), false));
        l.setElevation(dp(2));
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
        b.setBackground(ThemeManager.rounded(bg, dp(16)));
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
