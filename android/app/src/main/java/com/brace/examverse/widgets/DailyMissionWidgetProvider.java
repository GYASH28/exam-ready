package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.brace.examverse.R;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.theme.ThemeManager;

public class DailyMissionWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { updateWidgets(context, manager, ids); }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, android.os.Bundle options) { updateWidgets(c,m,new int[]{id}); }
    @Override public void onDeleted(Context c, int[] ids) { for(int id:ids) WidgetStyleStore.delete(c,id); }

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        ExamRepository repo = new ExamRepository(c);
        int today = repo.focusMinutesToday();
        int goal = repo.getDailyGoal();
        int pending = repo.getTodayTasks().size();
        int pct = Math.min(100, Math.round(today * 100f / Math.max(1, goal)));
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_mission); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id);
            rv.setTextViewText(R.id.widget_kicker, ThemeManager.displayName(c).toUpperCase());
            rv.setTextViewText(R.id.widget_title, "Focus · " + today + "/" + goal + "m");
            rv.setProgressBar(R.id.widget_progress, 100, pct, false);
            rv.setTextViewText(R.id.widget_countdown, pending == 0 ? "Board clear" : pending + " missions");
            rv.setTextViewText(R.id.widget_date, repo.getStreak() + "d streak  ·  " + repo.averageReadiness() + "% avg readiness  ·  LV " + repo.getLevel());
            WidgetUtil.applyDensity(c,rv,style,id,"mission");
            m.updateAppWidget(id, rv);
        }
    }
}
