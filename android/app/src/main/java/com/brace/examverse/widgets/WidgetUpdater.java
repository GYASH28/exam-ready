package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

public class WidgetUpdater {
    public static void updateAll(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        int[] a = m.getAppWidgetIds(new ComponentName(context, NextExamWidgetProvider.class));
        if (a.length > 0) NextExamWidgetProvider.updateWidgets(context, m, a);
        int[] b = m.getAppWidgetIds(new ComponentName(context, LiveExamWidgetProvider.class));
        if (b.length > 0) LiveExamWidgetProvider.updateWidgets(context, m, b);
        int[] c = m.getAppWidgetIds(new ComponentName(context, UpcomingExamWidgetProvider.class));
        if (c.length > 0) UpcomingExamWidgetProvider.updateWidgets(context, m, c);
        int[] d = m.getAppWidgetIds(new ComponentName(context, DailyMissionWidgetProvider.class));
        if (d.length > 0) DailyMissionWidgetProvider.updateWidgets(context, m, d);
    }
}
