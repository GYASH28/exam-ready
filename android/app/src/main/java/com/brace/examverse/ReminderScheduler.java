package com.brace.examverse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.brace.examverse.data.Exam;

public class ReminderScheduler {
    public static void schedule(Context c, Exam e) {
        int hours = Math.max(1, e.reminderHours);
        long trigger = e.timeMillis - hours * 60L * 60L * 1000L;
        if (trigger <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(c, e.id, e.title, e.subject, hours, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    public static void schedule24HoursBefore(Context c, Exam e) { schedule(c, e); }

    public static void cancel(Context c, long examId) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            PendingIntent pi = pending(c, examId, "", "", 24, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
    }

    private static PendingIntent pending(Context c, long id, String title, String subject, int hours, int flags) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("exam_id", id); i.putExtra("title", title); i.putExtra("subject", subject); i.putExtra("hours", hours);
        return PendingIntent.getBroadcast(c, (int)(id ^ (id >>> 32)), i, flags);
    }
}
