package com.brace.examverse.widgets;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.brace.examverse.MainActivity;
import com.brace.examverse.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class WidgetUtil {
    static WidgetStyleStore.Style base(Context c, RemoteViews rv, int widgetId) {
        WidgetStyleStore.Style style = WidgetStyleStore.get(c, widgetId);
        rv.setImageViewBitmap(R.id.widget_glass_bg, WidgetGlassRenderer.render(c, widgetId, style));
        int text = WidgetGlassRenderer.textColor(c, style.theme);
        int muted = WidgetGlassRenderer.mutedColor(c, style.theme);
        int accent = WidgetGlassRenderer.accent(c, style.theme);
        for (int id : new int[]{R.id.widget_title, R.id.widget_countdown, R.id.upcoming_1, R.id.upcoming_2, R.id.upcoming_3}) {
            try { rv.setTextColor(id, text); } catch (Exception ignored) {}
        }
        for (int id : new int[]{R.id.widget_kicker, R.id.widget_date}) {
            try { rv.setTextColor(id, muted); } catch (Exception ignored) {}
        }
        try { rv.setTextColor(R.id.widget_chronometer, text); } catch (Exception ignored) {}
        Intent i = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, widgetId, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root, pi);
        return style;
    }

    static void applyDensity(RemoteViews rv, WidgetStyleStore.Style style) {
        boolean compact = "compact".equals(style.density);
        try { rv.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, compact ? 16 : 20); } catch (Exception ignored) {}
        try { rv.setTextViewTextSize(R.id.widget_countdown, TypedValue.COMPLEX_UNIT_SP, compact ? 22 : 28); } catch (Exception ignored) {}
        try { rv.setTextViewTextSize(R.id.widget_chronometer, TypedValue.COMPLEX_UNIT_SP, compact ? 22 : 28); } catch (Exception ignored) {}
        try { rv.setViewVisibility(R.id.widget_date, compact ? View.GONE : View.VISIBLE); } catch (Exception ignored) {}
        try { rv.setViewVisibility(R.id.upcoming_3, compact ? View.GONE : View.VISIBLE); } catch (Exception ignored) {}
    }

    static String remaining(long when) {
        long diff = Math.max(0, when - System.currentTimeMillis());
        long days = diff / 86_400_000L; diff %= 86_400_000L;
        long hours = diff / 3_600_000L; diff %= 3_600_000L;
        long mins = diff / 60_000L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    static String date(long t) { return new SimpleDateFormat("EEE d MMM · h:mm a", Locale.getDefault()).format(new Date(t)); }
}
