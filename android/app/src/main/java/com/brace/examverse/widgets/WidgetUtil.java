package com.brace.examverse.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import com.brace.examverse.MainActivity;
import com.brace.examverse.R;
import java.text.SimpleDateFormat;
import java.util.*;

class WidgetUtil {
    static WidgetStyleStore.Style base(Context c, RemoteViews rv, int widgetId) {
        WidgetStyleStore.Style style = WidgetStyleStore.get(c, widgetId);
        rv.setImageViewBitmap(R.id.widget_glass_bg, WidgetGlassRenderer.render(c, widgetId, style));
        int text = WidgetGlassRenderer.textColor(c, style.theme), muted = WidgetGlassRenderer.mutedColor(c, style.theme);
        for (int id : new int[]{R.id.widget_title,R.id.widget_countdown,R.id.widget_chronometer,R.id.upcoming_1,R.id.upcoming_2,R.id.upcoming_3}) rv.setTextColor(id,text);
        for (int id : new int[]{R.id.widget_kicker,R.id.widget_date}) rv.setTextColor(id,muted);
        Intent i = new Intent(c, MainActivity.class).putExtra("open_tab",0);
        rv.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,widgetId,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return style;
    }
    static void applyDensity(Context c, RemoteViews rv, WidgetStyleStore.Style style, int widgetId, String kind) {
        Bundle options = AppWidgetManager.getInstance(c).getAppWidgetOptions(widgetId);
        int w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,180), h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,110);
        boolean tiny = w < 110 || h < 75, compact = tiny || w < 180 || h < 115 || "compact".equals(style.density);
        int padding = Math.round((tiny?5:compact?9:14)*c.getResources().getDisplayMetrics().density);
        rv.setViewPadding(R.id.widget_content,padding,padding,padding,padding);
        rv.setViewVisibility(R.id.widget_kicker,tiny?View.GONE:View.VISIBLE);
        rv.setViewVisibility(R.id.widget_date,compact?View.GONE:View.VISIBLE);
        rv.setTextViewTextSize(R.id.widget_title,TypedValue.COMPLEX_UNIT_SP,tiny?10:compact?13:17);
        rv.setTextViewTextSize(R.id.widget_countdown,TypedValue.COMPLEX_UNIT_SP,tiny?16:compact?21:28);
        rv.setTextViewTextSize(R.id.widget_chronometer,TypedValue.COMPLEX_UNIT_SP,compact?18:26);
        if ("live".equals(kind)) {
            rv.setViewVisibility(R.id.widget_chronometer,tiny?View.GONE:View.VISIBLE);
            rv.setViewVisibility(R.id.widget_countdown,tiny?View.VISIBLE:View.GONE);
        }
        if ("mission".equals(kind)) {
            if(tiny){com.brace.examverse.data.ExamRepository repo=new com.brace.examverse.data.ExamRepository(c);rv.setTextViewText(R.id.widget_countdown,Math.min(100,Math.round(repo.focusMinutesToday()*100f/Math.max(1,repo.getDailyGoal())))+"%");}
            rv.setViewVisibility(R.id.widget_progress,h<55?View.GONE:View.VISIBLE);
            rv.setTextViewTextSize(R.id.widget_countdown,TypedValue.COMPLEX_UNIT_SP,tiny?13:15);
            Intent i=new Intent(c,MainActivity.class).putExtra("open_tab",2);
            rv.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,widgetId,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }
        if ("upcoming".equals(kind)) {
            rv.setViewVisibility(R.id.widget_title,tiny?View.VISIBLE:View.GONE);
            rv.setViewVisibility(R.id.widget_countdown,tiny?View.VISIBLE:View.GONE);
            rv.setViewVisibility(R.id.upcoming_1,tiny?View.GONE:View.VISIBLE);
            rv.setViewVisibility(R.id.upcoming_2,!compact && h>=155?View.VISIBLE:View.GONE);
            rv.setViewVisibility(R.id.upcoming_3,!compact && h>=220?View.VISIBLE:View.GONE);
        }
        if(h<52)rv.setViewVisibility(R.id.widget_title,View.GONE);
    }
    static String remaining(long when) {
        long diff=Math.max(0,when-System.currentTimeMillis()),days=diff/86_400_000L,hours=diff%86_400_000L/3_600_000L,mins=diff%3_600_000L/60_000L;
        if(days>0)return days+"d "+hours+"h";if(hours>0)return hours+"h "+mins+"m";return mins+"m";
    }
    static String date(long t){return new SimpleDateFormat("EEE d MMM · h:mm a",Locale.getDefault()).format(new Date(t));}
}
