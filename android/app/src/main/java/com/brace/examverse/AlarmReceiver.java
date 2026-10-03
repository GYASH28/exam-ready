package com.brace.examverse;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import com.brace.examverse.theme.ThemeManager;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "exam_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "ExamVerse missions", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Exam countdown and revision reminders"); nm.createNotificationChannel(ch);
        }
        String title = intent.getStringExtra("title"); String subject = intent.getStringExtra("subject"); int hours = intent.getIntExtra("hours", 24);
        Intent open = new Intent(context, MainActivity.class); open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(context, CHANNEL) : new android.app.Notification.Builder(context);
        b.setSmallIcon(R.drawable.ic_app)
                .setContentTitle(hours + "h until " + (title == null ? "your exam" : title))
                .setContentText((subject == null ? "ExamVerse" : subject) + " · " + ThemeManager.motivation(context))
                .setContentIntent(pi).setAutoCancel(true).setColor(ThemeManager.palette(context).primary);
        nm.notify((int)(intent.getLongExtra("exam_id", 1) & 0x7fffffff), b.build());
    }
}
