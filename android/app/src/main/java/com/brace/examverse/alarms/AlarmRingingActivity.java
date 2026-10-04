package com.brace.examverse.alarms;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.brace.examverse.theme.ThemeManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmRingingActivity extends Activity {
    private long alarmId;
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);ThemeManager.applyWindow(this,getWindow());alarmId=getIntent().getLongExtra("alarm_id",-1);CustomAlarm a=new CustomAlarmRepository(this).get(alarmId);if(a==null){finish();return;}androidx.core.content.ContextCompat.registerReceiver(this,closed,new android.content.IntentFilter(AlarmPlaybackService.CLOSED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);if(getIntent().getBooleanExtra("fallback",false))startForegroundService(new Intent(this,AlarmPlaybackService.class).setAction(AlarmPlaybackService.RING).putExtra("alarm_id",alarmId));render(a);}
    private final android.content.BroadcastReceiver closed = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) { if (i.getLongExtra("alarm_id", -1) == alarmId) finish(); }
    };
    private void render(CustomAlarm a){ThemeManager.Palette p=ThemeManager.palette(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(26),dp(40),dp(26),dp(40));root.setBackground(ThemeManager.artwork(this));TextView time=t(new SimpleDateFormat("h:mm",Locale.getDefault()).format(new Date()),64,Color.WHITE,true);time.setGravity(Gravity.CENTER);root.addView(time);TextView ap=t(new SimpleDateFormat("a",Locale.getDefault()).format(new Date()),18,Color.WHITE,true);ap.setAlpha(.8f);ap.setGravity(Gravity.CENTER);root.addView(ap);TextView label=t(a.label,26,Color.WHITE,true);label.setGravity(Gravity.CENTER);root.addView(label,top(18));TextView repeat=t("ExamVerse custom alarm · "+a.repeat,13,Color.WHITE,false);repeat.setAlpha(.75f);repeat.setGravity(Gravity.CENTER);root.addView(repeat,top(6));Button snooze=button("Snooze 10 minutes",Color.argb(45,255,255,255),Color.WHITE);snooze.setOnClickListener(v->snooze(a));root.addView(snooze,topHeight(40,58));Button dismiss=button("Dismiss",Color.WHITE,p.primary);dismiss.setOnClickListener(v->dismiss());root.addView(dismiss,topHeight(12,58));setContentView(root);}
    private void snooze(CustomAlarm a){control(AlarmPlaybackService.SNOOZE);}
    private void dismiss(){control(AlarmPlaybackService.DISMISS);}
    private void control(String action){try{AlarmPlaybackService.control(this,alarmId,action).send();}catch(android.app.PendingIntent.CanceledException ignored){}((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel((int)(alarmId&0x7fffffff));finish();}
    @Override protected void onDestroy(){try{unregisterReceiver(closed);}catch(IllegalArgumentException ignored){}super.onDestroy();}
    private TextView t(String s,float sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(b)v.setTypeface(v.getTypeface(),android.graphics.Typeface.BOLD);return v;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(fg);b.setTextSize(14);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setBackground(ThemeManager.rounded(bg,dp(20)));return b;}
    private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}
    private LinearLayout.LayoutParams topHeight(int d,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(h));p.topMargin=dp(d);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private static class CalendarHour{static int tenMinutesHour(){java.util.Calendar c=java.util.Calendar.getInstance();c.add(java.util.Calendar.MINUTE,10);return c.get(java.util.Calendar.HOUR_OF_DAY);}static int tenMinutesMinute(){java.util.Calendar c=java.util.Calendar.getInstance();c.add(java.util.Calendar.MINUTE,10);return c.get(java.util.Calendar.MINUTE);}}
}
