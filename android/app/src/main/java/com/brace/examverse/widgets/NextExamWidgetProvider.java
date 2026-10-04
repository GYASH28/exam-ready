package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.brace.examverse.R;
import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;

public class NextExamWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) { updateWidgets(context, appWidgetManager, appWidgetIds); }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, android.os.Bundle options) { updateWidgets(c,m,new int[]{id}); }
    @Override public void onDeleted(Context c, int[] ids) { for(int id:ids) WidgetStyleStore.delete(c,id); }

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        ExamRepository repo = new ExamRepository(c); Exam e = repo.getNextExam();
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_next); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id);
            if (e == null) {
                rv.setTextViewText(R.id.widget_kicker, "NEXT EXAM"); rv.setTextViewText(R.id.widget_title, "No exams yet"); rv.setTextViewText(R.id.widget_countdown, "You’re clear ✨"); rv.setTextViewText(R.id.widget_date, "Tap to add an exam");
            } else {
                rv.setTextViewText(R.id.widget_kicker, e.subject.toUpperCase() + "  ·  " + repo.readinessForExam(e.id) + "% READY"); rv.setTextViewText(R.id.widget_title, e.title); rv.setTextViewText(R.id.widget_countdown, WidgetUtil.remaining(e.timeMillis)); rv.setTextViewText(R.id.widget_date, WidgetUtil.date(e.timeMillis) + "  ·  P" + e.priority);
            }
            WidgetUtil.applyDensity(c,rv,style,id,"next");
            m.updateAppWidget(id, rv);
        }
    }
}
