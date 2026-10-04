package com.brace.examverse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.widgets.WidgetUpdater;
import com.brace.examverse.alarms.CustomAlarmScheduler;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            for (Exam e : new ExamRepository(context).getUpcomingExams()) if (e.remind) ReminderScheduler.schedule(context, e);
            CustomAlarmScheduler.rescheduleAll(context);
            new com.brace.examverse.focus.FocusEngine(context).reschedule();
            WidgetUpdater.updateAll(context);
        }
    }
}
