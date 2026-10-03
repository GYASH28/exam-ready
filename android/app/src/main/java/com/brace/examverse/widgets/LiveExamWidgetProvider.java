package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.brace.examverse.R;
import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;

public class LiveExamWidgetProvider extends AppWidgetProvider {
    static final String PREFS = "live_widget_prefs";
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { updateWidgets(context, manager, ids); }
    @Override public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int id : appWidgetIds) e.remove("exam_" + id); e.apply();
    }

    public static void updateWidgets(Context c, AppWidgetManager m, int[] ids) {
        ExamRepository repo = new ExamRepository(c); SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (int id : ids) {
            WidgetStyleStore.Style configured = WidgetStyleStore.get(c, id); long examId = configured.examId != -1 ? configured.examId : prefs.getLong("exam_" + id, -1); Exam exam = examId == -1 ? repo.getNextExam() : repo.getExam(examId);
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_live); WidgetStyleStore.Style style = WidgetUtil.base(c, rv, id); WidgetUtil.applyDensity(rv, style);
            if (exam == null) {
                rv.setTextViewText(R.id.widget_kicker, "LIVE COUNTDOWN"); rv.setTextViewText(R.id.widget_title, "No exam selected");
                rv.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime(), "00:00:00", false); rv.setTextViewText(R.id.widget_date, "Open ExamVerse to add an exam");
            } else {
                long base = SystemClock.elapsedRealtime() + (exam.timeMillis - System.currentTimeMillis());
                rv.setTextViewText(R.id.widget_kicker, exam.subject.toUpperCase()); rv.setTextViewText(R.id.widget_title, exam.title);
                rv.setChronometer(R.id.widget_chronometer, base, null, true); rv.setChronometerCountDown(R.id.widget_chronometer, true); rv.setTextViewText(R.id.widget_date, WidgetUtil.date(exam.timeMillis));
            }
            m.updateAppWidget(id, rv);
        }
    }
}
