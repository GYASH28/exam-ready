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

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        Exam e = new ExamRepository(c).getNextExam();
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_next); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id); WidgetUtil.applyDensity(rv, style);
            if (e == null) {
                rv.setTextViewText(R.id.widget_kicker, "NEXT EXAM"); rv.setTextViewText(R.id.widget_title, "No exams yet"); rv.setTextViewText(R.id.widget_countdown, "You’re clear ✨"); rv.setTextViewText(R.id.widget_date, "Tap to add an exam");
            } else {
                rv.setTextViewText(R.id.widget_kicker, e.subject.toUpperCase()); rv.setTextViewText(R.id.widget_title, e.title); rv.setTextViewText(R.id.widget_countdown, WidgetUtil.remaining(e.timeMillis)); rv.setTextViewText(R.id.widget_date, WidgetUtil.date(e.timeMillis));
            }
            m.updateAppWidget(id, rv);
        }
    }
}
