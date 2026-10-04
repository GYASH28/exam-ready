package com.brace.examverse.focus;

import android.app.*;
import android.content.*;
import androidx.core.app.NotificationCompat;
import com.brace.examverse.*;
import com.brace.examverse.widgets.WidgetUpdater;

public class FocusCompleteReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        FocusEngine engine=new FocusEngine(c);boolean recovery="Break".equals(engine.state().mode);
        if(!engine.completeIfDue())return;
        WidgetUpdater.updateAll(c);
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel=new NotificationChannel("focus_complete_v5","Focus completion",NotificationManager.IMPORTANCE_DEFAULT);nm.createNotificationChannel(channel);
        Intent screen=new Intent(c,MainActivity.class).putExtra("open_tab",2);PendingIntent pi=PendingIntent.getActivity(c,71,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(nm.areNotificationsEnabled())nm.notify(71,new NotificationCompat.Builder(c,channel.getId()).setSmallIcon(R.drawable.ic_app).setContentTitle(recovery?"Recovery break complete":"Focus block complete").setContentText(recovery?"Ready for the next study block?":"Progress saved. Take a breath and a short break.").setContentIntent(pi).setAutoCancel(true).build());
    }
}
