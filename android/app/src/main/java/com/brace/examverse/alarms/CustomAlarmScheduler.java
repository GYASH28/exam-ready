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
    public static boolean canExact(Context c){
        if(Build.VERSION.SDK_INT<31)return true;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        return am.canScheduleExactAlarms();
    }
    public static void requestExact(Context c){
        if(Build.VERSION.SDK_INT>=31){Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+c.getPackageName()));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(i);}
    }
    public static long nextTrigger(CustomAlarm a){
        Calendar now=Calendar.getInstance();Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,a.hour);c.set(Calendar.MINUTE,a.minute);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        if(c.getTimeInMillis()<=now.getTimeInMillis())c.add(Calendar.DAY_OF_MONTH,1);
        if("weekdays".equals(a.repeat)){while(c.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY||c.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY)c.add(Calendar.DAY_OF_MONTH,1);}
        return c.getTimeInMillis();
    }
    public static void schedule(Context c,CustomAlarm a){
        if(!a.enabled)return;AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);PendingIntent pi=pending(c,a.id);long at=nextTrigger(a);
        if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setAlarmClock(new AlarmManager.AlarmClockInfo(at,pi),pi);
        else if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.set(AlarmManager.RTC_WAKEUP,at,pi);
    }
    public static void cancel(Context c,long id){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);am.cancel(pending(c,id));}
    public static void rescheduleAll(Context c){for(CustomAlarm a:new CustomAlarmRepository(c).getAll())if(a.enabled)schedule(c,a);}
    private static PendingIntent pending(Context c,long id){Intent i=new Intent(c,CustomAlarmReceiver.class);i.putExtra("alarm_id",id);return PendingIntent.getBroadcast(c,(int)(id^(id>>>32)),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
}
