package com.brace.examverse.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;

import com.brace.examverse.data.ExamRepository;

public class ThemeManager {
    public static class Palette {
        public final int bg, bgAlt, surface, surfaceAlt, glass, glassStrong;
        public final int primary, secondary, accent, accent2;
        public final int text, muted, border, danger, success, warning;
        public final int heroStart, heroMid, heroEnd;
        public final boolean dark;

        public Palette(
                int bg, int bgAlt, int surface, int surfaceAlt, int glass, int glassStrong,
                int primary, int secondary, int accent, int accent2,
                int text, int muted, int border, int danger, int success, int warning,
                int heroStart, int heroMid, int heroEnd, boolean dark
        ) {
            this.bg = bg;
            this.bgAlt = bgAlt;
            this.surface = surface;
            this.surfaceAlt = surfaceAlt;
            this.glass = glass;
            this.glassStrong = glassStrong;
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
            this.accent2 = accent2;
            this.text = text;
            this.muted = muted;
            this.border = border;
            this.danger = danger;
            this.success = success;
            this.warning = warning;
            this.heroStart = heroStart;
            this.heroMid = heroMid;
            this.heroEnd = heroEnd;
            this.dark = dark;
        }
    }

    public static Palette palette(Context c) {
        String t = new ExamRepository(c).getTheme();
        switch (t) {
            case "dragonball":
                return new Palette(
                        Color.rgb(4, 10, 28), Color.rgb(8, 18, 48),
                        Color.rgb(16, 29, 63), Color.rgb(24, 43, 88),
                        Color.argb(218, 15, 28, 62), Color.argb(240, 19, 37, 78),
                        Color.rgb(255, 184, 28), Color.rgb(37, 99, 235),
                        Color.rgb(56, 189, 248), Color.rgb(255, 116, 22),
                        Color.rgb(248, 250, 252), Color.rgb(156, 173, 207),
                        Color.rgb(55, 82, 135), Color.rgb(251, 113, 133),
                        Color.rgb(52, 211, 153), Color.rgb(251, 191, 36),
                        Color.rgb(249, 115, 22), Color.rgb(37, 99, 235), Color.rgb(15, 23, 42),
                        true
                );
            case "bleach":
                return new Palette(
                        Color.rgb(5, 5, 7), Color.rgb(14, 14, 17),
                        Color.rgb(22, 22, 26), Color.rgb(34, 34, 40),
                        Color.argb(220, 20, 20, 24), Color.argb(243, 30, 30, 36),
                        Color.rgb(244, 63, 94), Color.rgb(244, 244, 245),
                        Color.rgb(250, 204, 21), Color.rgb(168, 85, 247),
                        Color.rgb(250, 250, 250), Color.rgb(161, 161, 170),
                        Color.rgb(63, 63, 70), Color.rgb(251, 113, 133),
                        Color.rgb(52, 211, 153), Color.rgb(250, 204, 21),
                        Color.rgb(15, 15, 18), Color.rgb(127, 29, 29), Color.rgb(244, 63, 94),
                        true
                );
            case "naruto":
            default:
                return new Palette(
                        Color.rgb(255, 247, 237), Color.rgb(255, 237, 213),
                        Color.rgb(255, 255, 255), Color.rgb(255, 243, 224),
                        Color.argb(232, 255, 255, 255), Color.argb(248, 255, 250, 245),
                        Color.rgb(249, 115, 22), Color.rgb(127, 29, 29),
                        Color.rgb(234, 88, 12), Color.rgb(245, 158, 11),
                        Color.rgb(31, 26, 23), Color.rgb(113, 86, 67),
                        Color.rgb(236, 199, 167), Color.rgb(220, 38, 38),
                        Color.rgb(22, 163, 74), Color.rgb(217, 119, 6),
                        Color.rgb(249, 115, 22), Color.rgb(180, 83, 9), Color.rgb(35, 24, 21),
                        false
                );
        }
    }

    public static String displayName(Context c) {
        switch (new ExamRepository(c).getTheme()) {
            case "dragonball": return "Saiyan Energy";
            case "bleach": return "Soul Reaper Noir";
            default: return "Shinobi Ember";
        }
    }

    public static String themeTagline(Context c) {
        switch (new ExamRepository(c).getTheme()) {
            case "dragonball": return "Train hard. Break limits.";
            case "bleach": return "Precision. Pressure. Resolve.";
            default: return "Quiet focus. Relentless progress.";
        }
    }

    public static String rankName(Context c, int level) {
        String t = new ExamRepository(c).getTheme();
        if ("dragonball".equals(t)) {
            if (level >= 18) return "Ultra Instinct";
            if (level >= 12) return "Super Saiyan";
            if (level >= 7) return "Elite Warrior";
            if (level >= 3) return "Saiyan";
            return "Trainee";
        }
        if ("bleach".equals(t)) {
            if (level >= 18) return "Captain";
            if (level >= 12) return "Bankai";
            if (level >= 7) return "Shikai";
            if (level >= 3) return "Seated Officer";
            return "Soul Reaper";
        }
        if (level >= 18) return "Hokage";
        if (level >= 12) return "Jonin";
        if (level >= 7) return "Chunin";
        if (level >= 3) return "Genin";
        return "Academy";
    }

    public static String motivation(Context c) {
        int streak = new ExamRepository(c).getStreak();
        if (streak >= 14) return streak + " day streak · momentum is now a weapon";
        if (streak >= 7) return streak + " day streak · consistency beats panic";
        if (streak >= 3) return streak + " day streak · keep the chain alive";
        return themeTagline(c);
    }

    public static void applyWindow(Context c, Window w) {
        Palette p = palette(c);
        w.setStatusBarColor(p.bg);
        w.setNavigationBarColor(p.bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = w.getDecorView().getSystemUiVisibility();
            if (!p.dark) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    public static GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    public static GradientDrawable outlined(int fill, int stroke, float radius, int widthPx) {
        GradientDrawable d = rounded(fill, radius);
        d.setStroke(Math.max(1, widthPx), stroke);
        return d;
    }

    public static GradientDrawable gradient(int start, int end, float radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end}
        );
        d.setCornerRadius(radius);
        return d;
    }

    public static GradientDrawable gradient(int start, int mid, int end, float radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, mid, end}
        );
        d.setCornerRadius(radius);
        return d;
    }

    public static GradientDrawable hero(Context c, float radius) {
        Palette p = palette(c);
        GradientDrawable d = gradient(p.heroStart, p.heroMid, p.heroEnd, radius);
        d.setStroke(1, withAlpha(Color.WHITE, p.dark ? 36 : 62));
        return d;
    }

    public static GradientDrawable glass(Context c, float radius, boolean strong) {
        Palette p = palette(c);
        return outlined(strong ? p.glassStrong : p.glass, p.border, radius, 1);
    }

    public static GradientDrawable glowChip(Context c, float radius) {
        Palette p = palette(c);
        return outlined(withAlpha(p.primary, p.dark ? 44 : 28), withAlpha(p.primary, 110), radius, 1);
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(
                Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color)
        );
    }

    public static int widgetBackground(Context c) {
        String t = new ExamRepository(c).getTheme();
        if ("bleach".equals(t)) return R.drawable.widget_bg_bleach;
        if ("dragonball".equals(t)) return R.drawable.widget_bg_dragonball;
        return R.drawable.widget_bg_naruto;
    }
}
