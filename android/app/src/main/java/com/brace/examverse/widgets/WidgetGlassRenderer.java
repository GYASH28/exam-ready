package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Bundle;

import com.brace.examverse.theme.ThemeManager;

class WidgetGlassRenderer {
    static Bitmap render(Context c, int widgetId, WidgetStyleStore.Style style) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        Bundle o = m.getAppWidgetOptions(widgetId);
        int minW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260);
        int minH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120);

        float d = c.getResources().getDisplayMetrics().density;
        int w = Math.max(40, Math.min(900, Math.round(minW * d)));
        int h = Math.max(40, Math.min(650, Math.round(minH * d)));

        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);

        int accent = accent(c, style.theme);
        boolean dark = isDark(c, style.theme);
        int base = dark ? Color.rgb(9, 13, 24) : Color.rgb(250, 250, 252);
        int secondary = secondaryAccent(c, style.theme);
        int accentAlpha = Math.round(28 + (style.accentStrength / 100f) * 72);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = style.corner * d;

        // Multi-stop glass body: neutral base -> theme tint -> secondary edge tint.
        int a0 = style.opacity;
        int a1 = Math.max(70, style.opacity - 34);
        int a2 = Math.max(55, style.opacity - 62);
        p.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{
                        Color.argb(a0, Color.red(base), Color.green(base), Color.blue(base)),
                        Color.argb(a1, mix(Color.red(base), Color.red(accent), .16f),
                                mix(Color.green(base), Color.green(accent), .16f),
                                mix(Color.blue(base), Color.blue(accent), .16f)),
                        Color.argb(a2, mix(Color.red(base), Color.red(secondary), .22f),
                                mix(Color.green(base), Color.green(secondary), .22f),
                                mix(Color.blue(base), Color.blue(secondary), .22f))
                },
                new float[]{0f, .58f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(2 * d, 2 * d, w - 2 * d, h - 2 * d, r, r, p);
        p.setShader(null);

        // Accent aura in the far corner.
        p.setShader(new RadialGradient(
                w * .88f, h * .18f, Math.max(w, h) * .45f,
                Color.argb(accentAlpha, Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(2 * d, 2 * d, w - 2 * d, h - 2 * d, r, r, p);
        p.setShader(null);

        if (!"frost".equals(style.theme)) {
            String key = "auto".equals(style.theme) ? new com.brace.examverse.data.ExamRepository(c).getTheme() : style.theme;
            android.graphics.drawable.Drawable art = ThemeManager.artwork(c,key,r);
            art.setBounds(0,0,w,h); art.setAlpha(Math.min(230,style.opacity)); art.draw(canvas);
        }

        // Frost highlight and premium edge.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.25f * d);
        p.setColor(Color.argb(dark ? 70 : 125, 255, 255, 255));
        canvas.drawRoundRect(3 * d, 3 * d, w - 3 * d, h - 3 * d, Math.max(0, r - d), Math.max(0, r - d), p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(dark ? 24 : 40, 255, 255, 255));
        canvas.drawRoundRect(14 * d, 11 * d, w - 14 * d, 13 * d, 4 * d, 4 * d, p);

        // Theme energy orbs / glass bloom.
        p.setColor(Color.argb(Math.max(10, accentAlpha / 3), Color.red(accent), Color.green(accent), Color.blue(accent)));
        canvas.drawCircle(w * .90f, h * .76f, Math.max(w, h) * .24f, p);

        p.setColor(Color.argb(dark ? 16 : 22, 255, 255, 255));
        canvas.drawCircle(w * .12f, h * .02f, Math.max(w, h) * .20f, p);

        // Tiny deterministic "frost dust" points. No randomness means no visual flicker.
        for (int i = 0; i < 12; i++) {
            float x = (17 + i * 79) % Math.max(1, w - 18);
            float y = (31 + i * 53) % Math.max(1, h - 18);
            p.setColor(Color.argb(dark ? 16 : 24, 255, 255, 255));
            canvas.drawCircle(x, y, (1 + (i % 3)) * d, p);
        }

        return b;
    }

    static int accent(Context c, String theme) {
        if ("naruto".equals(theme)) return Color.rgb(249, 115, 22);
        if ("dragonball".equals(theme)) return Color.rgb(255, 184, 28);
        if ("bleach".equals(theme)) return Color.rgb(244, 63, 94);
        if ("blackclover".equals(theme)) return 0xff65e3a1;
        if ("demonslayer".equals(theme)) return 0xff55d8db;
        if ("onepiece".equals(theme)) return 0xffffbe63;
        if ("frost".equals(theme)) return Color.rgb(56, 189, 248);
        return ThemeManager.palette(c).primary;
    }

    static int secondaryAccent(Context c, String theme) {
        if ("naruto".equals(theme)) return Color.rgb(127, 29, 29);
        if ("dragonball".equals(theme)) return Color.rgb(37, 99, 235);
        if ("bleach".equals(theme)) return Color.rgb(250, 250, 250);
        if ("frost".equals(theme)) return Color.rgb(129, 140, 248);
        return ThemeManager.palette(c).secondary;
    }

    static int textColor(Context c, String theme) {
        return "frost".equals(theme)?Color.rgb(15,23,42):Color.WHITE;
    }

    static int mutedColor(Context c, String theme) {
        return "frost".equals(theme)?Color.rgb(71,85,105):Color.rgb(222,232,245);
    }

    private static boolean isDark(Context c, String theme) {
        return "blackclover".equals(theme) || "demonslayer".equals(theme) || "onepiece".equals(theme) || "bleach".equals(theme) || "dragonball".equals(theme) ||
                ("auto".equals(theme) && ThemeManager.palette(c).dark);
    }

    private static int mix(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
