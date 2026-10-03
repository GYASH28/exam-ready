package com.brace.examverse.alarms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.brace.examverse.R;

public class CustomAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL="examverse_alarm_clock";
    @Override public void onReceive(Context c,Intent intent){
        long id=intent.getLongExtra("alarm_id",-1);CustomAlarmRepository repo=new CustomAlarmRepository(c);CustomAlarm a=repo.get(id);if(a==null||!a.enabled)return;
        Intent ring=new Intent(c,AlarmRingingActivity.class);ring.putExtra("alarm_id",id);ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full=PendingIntent.getActivity(c,(int)id,ring,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"ExamVerse alarms",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Custom study and wake alarms");ch.enableVibration(a.vibrate);ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);nm.createNotificationChannel(ch);}
        NotificationCompat.Builder b=new NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_app).setContentTitle(a.label).setContentText("ExamVerse alarm · tap to dismiss or snooze").setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM).setAutoCancel(false).setOngoing(true).setFullScreenIntent(full,true).setContentIntent(full).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        nm.notify((int)(id&0x7fffffff),b.build());
        if("once".equals(a.repeat)){a.enabled=false;repo.save(a);}else CustomAlarmScheduler.schedule(c,a);
    }
}
