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

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        ExamRepository repo = new ExamRepository(c);
        int today = repo.focusMinutesToday();
        int goal = repo.getDailyGoal();
        int pending = repo.getTodayTasks().size();
        int pct = Math.min(100, Math.round(today * 100f / Math.max(1, goal)));
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_mission); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id); WidgetUtil.applyDensity(rv, style);
            rv.setTextViewText(R.id.widget_kicker, ThemeManager.displayName(c).toUpperCase());
            rv.setTextViewText(R.id.widget_title, today + " / " + goal + " focused minutes");
            rv.setProgressBar(R.id.widget_progress, 100, pct, false);
            rv.setTextViewText(R.id.widget_countdown, pending == 0 ? "Mission board clear" : pending + (pending == 1 ? " revision mission today" : " revision missions today"));
            rv.setTextViewText(R.id.widget_date, repo.getStreak() + " day streak · Level " + repo.getLevel());
            m.updateAppWidget(id, rv);
        }
    }
}
