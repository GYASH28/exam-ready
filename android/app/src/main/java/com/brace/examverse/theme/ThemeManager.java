package com.brace.examverse.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Window;

import com.brace.examverse.R;
import com.brace.examverse.data.ExamRepository;

public class ThemeManager {
    public static class Palette {
        public final int bg, surface, surfaceAlt, primary, secondary, accent, text, muted, danger, success;
        public final boolean dark;
        public Palette(int bg, int surface, int surfaceAlt, int primary, int secondary, int accent,
                       int text, int muted, int danger, int success, boolean dark) {
            this.bg = bg; this.surface = surface; this.surfaceAlt = surfaceAlt; this.primary = primary;
            this.secondary = secondary; this.accent = accent; this.text = text; this.muted = muted;
            this.danger = danger; this.success = success; this.dark = dark;
        }
    }

    public static Palette palette(Context c) {
        String t = new ExamRepository(c).getTheme();
        switch (t) {
            case "dragonball":
                return new Palette(Color.rgb(255,249,238), Color.WHITE, Color.rgb(255,241,211),
                        Color.rgb(245,158,11), Color.rgb(37,99,235), Color.rgb(249,115,22),
                        Color.rgb(15,23,42), Color.rgb(100,116,139), Color.rgb(220,38,38), Color.rgb(22,163,74), false);
            case "bleach":
                return new Palette(Color.rgb(8,8,10), Color.rgb(24,24,27), Color.rgb(39,39,42),
                        Color.rgb(239,68,68), Color.rgb(244,244,245), Color.rgb(250,204,21),
                        Color.rgb(250,250,250), Color.rgb(161,161,170), Color.rgb(248,113,113), Color.rgb(52,211,153), true);
            case "naruto":
            default:
                return new Palette(Color.rgb(255,248,240), Color.WHITE, Color.rgb(255,237,213),
                        Color.rgb(249,115,22), Color.rgb(17,24,39), Color.rgb(234,88,12),
                        Color.rgb(31,41,55), Color.rgb(107,114,128), Color.rgb(220,38,38), Color.rgb(22,163,74), false);
        }
    }

    public static String displayName(Context c) {
        switch (new ExamRepository(c).getTheme()) {
            case "dragonball": return "Saiyan Mode";
            case "bleach": return "Soul Reaper Mode";
            default: return "Shinobi Mode";
        }
    }

    public static String rankName(Context c, int level) {
        String t = new ExamRepository(c).getTheme();
        if ("dragonball".equals(t)) {
            if (level >= 10) return "Ultra Instinct";
            if (level >= 7) return "Super Saiyan";
            if (level >= 4) return "Elite Saiyan";
            return "Training Arc";
        }
        if ("bleach".equals(t)) {
            if (level >= 10) return "Captain";
            if (level >= 7) return "Bankai";
            if (level >= 4) return "Shikai";
            return "Soul Reaper";
        }
        if (level >= 10) return "Hokage";
        if (level >= 7) return "Jonin";
        if (level >= 4) return "Chunin";
        return "Genin";
    }

    public static String motivation(Context c) {
        switch (new ExamRepository(c).getTheme()) {
            case "dragonball": return "Train past yesterday's limit.";
            case "bleach": return "Sharpen the blade. Clear the syllabus.";
            default: return "One mission at a time. Keep your word to yourself.";
        }
    }

    public static void applyWindow(Context c, Window w) {
        Palette p = palette(c);
        w.setStatusBarColor(p.bg); w.setNavigationBarColor(p.bg);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = w.getDecorView().getSystemUiVisibility();
            if (!p.dark) flags |= 0x00002000; else flags &= ~0x00002000;
            if (Build.VERSION.SDK_INT >= 26) { if (!p.dark) flags |= 0x00000010; else flags &= ~0x00000010; }
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    public static GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }

    public static GradientDrawable outlined(int fill, int stroke, float radius, int widthPx) {
        GradientDrawable d = rounded(fill, radius); d.setStroke(widthPx, stroke); return d;
    }

    public static GradientDrawable gradient(int start, int end, float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(radius); return d;
    }

    public static int widgetBackground(Context c) {
        String t = new ExamRepository(c).getTheme();
        if ("dragonball".equals(t)) return R.drawable.widget_bg_dragonball;
        if ("bleach".equals(t)) return R.drawable.widget_bg_bleach;
        if ("naruto".equals(t)) return R.drawable.widget_bg_naruto;
        return R.drawable.widget_bg_default;
    }
}
