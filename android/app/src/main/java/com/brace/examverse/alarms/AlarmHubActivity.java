package com.brace.examverse.alarms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.brace.examverse.theme.ThemeManager;
import java.util.List;
import java.util.Locale;

public class AlarmHubActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);ThemeManager.applyWindow(this,getWindow());render();}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){ThemeManager.Palette p=ThemeManager.palette(this);ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(28));root.setBackgroundColor(p.bg);sc.addView(root);root.addView(t("Alarm studio",29,p.text,true));root.addView(t("Build wake-up, study-start and revision alarms with snooze, custom sound and repeat rules.",13,p.muted,false),top(5));if(!CustomAlarmScheduler.canExact(this)){LinearLayout warn=card(p);warn.addView(t("Precise alarm access is off",14,p.text,true));warn.addView(t("Grant Alarms & reminders access for reliable continuous ringing while the app is closed.",11.5f,p.muted,false),top(3));Button grant=button("Enable precise alarms",p.primary,Color.WHITE);grant.setOnClickListener(v->CustomAlarmScheduler.requestExact(this));warn.addView(grant,topHeight(10,48));root.addView(warn,top(16));}root.addView(readiness(p),top(14));Button add=button("＋ Create custom alarm",p.primary,Color.WHITE);add.setOnClickListener(v->startActivity(new Intent(this,AddAlarmActivity.class)));root.addView(add,topHeight(16,56));List<CustomAlarm> list=new CustomAlarmRepository(this).getAll();if(list.isEmpty()){LinearLayout empty=card(p);empty.addView(t("No custom alarms yet. Try a wake-up alarm, a ‘start focus’ alarm, or a nightly revision reminder.",13,p.muted,false));root.addView(empty,top(14));}else for(CustomAlarm a:list){root.addView(alarmCard(a,p),top(10));}setContentView(sc);}
    private LinearLayout readiness(ThemeManager.Palette p) {
        LinearLayout panel=card(p);
        android.app.NotificationManager nm=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        boolean notify=nm.areNotificationsEnabled();
        boolean full=android.os.Build.VERSION.SDK_INT<34||nm.canUseFullScreenIntent();
        panel.addView(t("Alarm readiness",17,p.text,true));
        panel.addView(t((CustomAlarmScheduler.canExact(this)?"✓":"○")+" Precise scheduling\n"+(notify?"✓":"○")+" Notifications\n"+(full?"✓":"○")+" Lock-screen alarm screen\nSound follows your phone's alarm volume.",12,p.muted,false),top(6));
        if(!notify){Button b=button("Enable alarm notifications",p.surfaceAlt,p.text);b.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName())));panel.addView(b,topHeight(8,48));}
        if(!full){Button b=button("Allow lock-screen alarm screen",p.surfaceAlt,p.text);b.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,android.net.Uri.parse("package:"+getPackageName()))));panel.addView(b,topHeight(8,48));}
        Button test=button("Test alarm in 20 seconds",p.surfaceAlt,p.text);
        test.setOnClickListener(v->{if(!CustomAlarmScheduler.canExact(this)){CustomAlarmScheduler.requestExact(this);return;}long id=System.currentTimeMillis();CustomAlarm a=new CustomAlarm(id,"20-second alarm test",0,0,"once",true,true,"");new CustomAlarmRepository(this).save(a);CustomAlarmScheduler.scheduleAt(this,id,System.currentTimeMillis()+20_000,false);Toast.makeText(this,"Test scheduled. Lock your screen to check automatic ringing.",Toast.LENGTH_LONG).show();});
        panel.addView(test,topHeight(10,48)); return panel;
    }
    private LinearLayout alarmCard(CustomAlarm a,ThemeManager.Palette p){LinearLayout card=card(p);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);String time=String.format(Locale.US,"%02d:%02d",a.hour,a.minute);info.addView(t(time,27,p.text,true));info.addView(t(a.label+" · "+repeatLabel(a.repeat),12,p.muted,false),top(2));row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Switch sw=new Switch(this);sw.setChecked(a.enabled);sw.setOnCheckedChangeListener((b,on)->{a.enabled=on;new CustomAlarmRepository(this).save(a);CustomAlarmScheduler.cancel(this,a.id);if(on)CustomAlarmScheduler.schedule(this,a);});row.addView(sw);card.addView(row);card.setOnClickListener(v->{Intent i=new Intent(this,AddAlarmActivity.class);i.putExtra("alarm_id",a.id);startActivity(i);});card.setOnLongClickListener(v->{new CustomAlarmRepository(this).delete(a.id);CustomAlarmScheduler.cancel(this,a.id);Toast.makeText(this,"Alarm deleted",Toast.LENGTH_SHORT).show();render();return true;});return card;}
    private String repeatLabel(String r){return "daily".equals(r)?"Every day":"weekdays".equals(r)?"Weekdays":"Once";}
    private LinearLayout card(ThemeManager.Palette p){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(14),dp(16),dp(14));l.setBackground(ThemeManager.outlined(p.surface,Color.argb(45,Color.red(p.primary),Color.green(p.primary),Color.blue(p.primary)),dp(22),dp(1)));return l;}
    private TextView t(String s,float sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(b)v.setTypeface(v.getTypeface(),android.graphics.Typeface.BOLD);return v;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(fg);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setBackground(ThemeManager.rounded(bg,dp(18)));return b;}
    private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}
    private LinearLayout.LayoutParams topHeight(int d,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(h));p.topMargin=dp(d);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
