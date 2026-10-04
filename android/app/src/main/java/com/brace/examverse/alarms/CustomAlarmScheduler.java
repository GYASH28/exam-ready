package com.brace.examverse.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.util.Calendar;

public class CustomAlarmScheduler {
    public static boolean canExact(Context c) {
        return Build.VERSION.SDK_INT < 31 || ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
    }
    public static void requestExact(Context c) {
        if (Build.VERSION.SDK_INT >= 31) c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    public static long nextTrigger(CustomAlarm a) { return nextTrigger(a, System.currentTimeMillis()); }
    public static long nextTrigger(CustomAlarm a, long now) {
        Calendar time = Calendar.getInstance(); time.setTimeInMillis(now);
        time.set(Calendar.HOUR_OF_DAY, a.hour); time.set(Calendar.MINUTE, a.minute); time.set(Calendar.SECOND, 0); time.set(Calendar.MILLISECOND, 0);
        if (time.getTimeInMillis() <= now) time.add(Calendar.DAY_OF_MONTH, 1);
        if ("weekdays".equals(a.repeat)) while (time.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || time.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) time.add(Calendar.DAY_OF_MONTH, 1);
        return time.getTimeInMillis();
    }
    public static void schedule(Context c, CustomAlarm a) { if (a.enabled) scheduleAt(c, a.id, nextTrigger(a), false); }
    public static void snooze(Context c, long id) {
        long at = System.currentTimeMillis() + 10 * 60_000L;
        c.getSharedPreferences("alarm_snooze", 0).edit().putLong("at_" + id, at).apply();
        scheduleAt(c, id, at, true);
    }
    public static long snoozeTime(Context c, long id) { return c.getSharedPreferences("alarm_snooze", 0).getLong("at_" + id, 0); }
    public static void clearSnooze(Context c, long id) { c.getSharedPreferences("alarm_snooze", 0).edit().remove("at_" + id).apply(); }
    public static void scheduleAt(Context c, long id, long at, boolean snoozed) {
        AlarmManager manager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent ring = pending(c, id, snoozed);
        Intent show = new Intent(c, AlarmHubActivity.class);
        PendingIntent screen = PendingIntent.getActivity(c, 500, show, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (canExact(c)) manager.setAlarmClock(new AlarmManager.AlarmClockInfo(at, screen), ring);
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, ring);
        } catch (SecurityException denied) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, ring); }
    }
    public static void cancel(Context c, long id) {
        AlarmManager manager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pending(c, id, false)); manager.cancel(pending(c, id, true)); clearSnooze(c, id);
    }
    public static void rescheduleAll(Context c) {
        for (CustomAlarm a : new CustomAlarmRepository(c).getAll()) {
            if (a.enabled) schedule(c, a);
            long at = snoozeTime(c, a.id);
            if (at > 0) scheduleAt(c, a.id, Math.max(System.currentTimeMillis() + 1000, at), true);
        }
    }
    private static PendingIntent pending(Context c, long id, boolean snoozed) {
        Intent i = new Intent(c, CustomAlarmReceiver.class).setAction(snoozed ? "snooze" : "scheduled").setData(Uri.parse("examverse://alarm/" + id)).putExtra("alarm_id", id).putExtra("snoozed", snoozed);
        return PendingIntent.getBroadcast(c, (int)(id ^ (id >>> 32)), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
