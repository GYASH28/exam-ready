package com.brace.examverse.alarms;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import androidx.core.app.NotificationCompat;
import com.brace.examverse.R;

public class CustomAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        long id = intent.getLongExtra("alarm_id", -1);
        boolean snoozed = intent.getBooleanExtra("snoozed", false);
        CustomAlarmRepository repo = new CustomAlarmRepository(c);
        CustomAlarm a = repo.get(id);
        if (a == null || (!a.enabled && !snoozed) || (snoozed && CustomAlarmScheduler.snoozeTime(c, id) == 0)) return;
        if (snoozed) CustomAlarmScheduler.clearSnooze(c, id);
        try { c.startForegroundService(new Intent(c, AlarmPlaybackService.class).setAction(AlarmPlaybackService.RING).putExtra("alarm_id", id)); }
        catch (RuntimeException blocked) {
            // Inexact alarms cannot always start an FGS from the background. A sounding
            // notification is a fallback; Alarm Studio explains how to grant exact access.
            NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("examverse_alarm_fallback_v5", "Approximate alarm fallback", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            channel.enableVibration(a.vibrate); nm.createNotificationChannel(channel);
            Intent screen = new Intent(c, AlarmRingingActivity.class).putExtra("alarm_id", id).putExtra("fallback", true);
            PendingIntent full = PendingIntent.getActivity(c, (int)id, screen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            nm.notify((int)(id & 0x7fffffff), new NotificationCompat.Builder(c, channel.getId()).setSmallIcon(R.drawable.ic_app).setContentTitle(a.label).setContentText("Enable precise alarms for continuous automatic ringing").setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(full).setFullScreenIntent(full,true).build());
        }
        if (!snoozed) {
            if ("once".equals(a.repeat)) { a.enabled = false; repo.save(a); }
            else CustomAlarmScheduler.schedule(c, a);
        }
    }
}
