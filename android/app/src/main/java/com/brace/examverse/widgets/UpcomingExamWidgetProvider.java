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

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, android.os.Bundle options) { updateWidgets(c,m,new int[]{id}); }
    @Override public void onDeleted(Context c, int[] ids) { for(int id:ids) WidgetStyleStore.delete(c,id); }

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        ExamRepository repo = new ExamRepository(c); List<Exam> exams = repo.getUpcomingExams();
        int[] rows = {R.id.upcoming_1, R.id.upcoming_2, R.id.upcoming_3};
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_upcoming); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id);
            rv.setTextViewText(R.id.widget_title,exams.isEmpty()?"Add exam":exams.get(0).subject);
            rv.setTextViewText(R.id.widget_countdown,exams.isEmpty()?"—":WidgetUtil.remaining(exams.get(0).timeMillis));
            for (int i = 0; i < rows.length; i++) {
                if (i < exams.size()) { Exam e = exams.get(i); rv.setTextViewText(rows[i], e.subject + " · " + e.title + "\n" + WidgetUtil.remaining(e.timeMillis) + "  ·  " + repo.readinessForExam(e.id) + "% ready"); }
                else rv.setTextViewText(rows[i], i == 0 ? "No upcoming exams ✨" : "");
            }
            WidgetUtil.applyDensity(c,rv,style,id,"upcoming");
            m.updateAppWidget(id, rv);
        }
    }
}
