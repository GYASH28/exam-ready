package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.brace.examverse.R;
import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;

import java.util.List;

public class UpcomingExamWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { updateWidgets(context, manager, ids); }

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        List<Exam> exams = new ExamRepository(c).getUpcomingExams();
        int[] rows = {R.id.upcoming_1, R.id.upcoming_2, R.id.upcoming_3};
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_upcoming); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id); WidgetUtil.applyDensity(rv, style);
            for (int i = 0; i < rows.length; i++) {
                if (i < exams.size()) { Exam e = exams.get(i); rv.setTextViewText(rows[i], e.subject + " · " + e.title + "\n" + WidgetUtil.remaining(e.timeMillis)); }
                else rv.setTextViewText(rows[i], i == 0 ? "No upcoming exams ✨" : "");
            }
            m.updateAppWidget(id, rv);
        }
    }
}
