package com.brace.examverse.alarms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.brace.examverse.theme.ThemeManager;

public class AddAlarmActivity extends Activity {
    private static final int PICK_RINGTONE=410;
    private long alarmId=-1; private EditText label; private TimePicker time; private Spinner repeat; private CheckBox vibrate; private String soundUri=""; private TextView soundText;
    @Override protected void onCreate(Bundle b){super.onCreate(b);ThemeManager.applyWindow(this,getWindow());alarmId=getIntent().getLongExtra("alarm_id",-1);build();}
    private void build(){ThemeManager.Palette p=ThemeManager.palette(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(p.bg);root.addView(t(alarmId>0?"Edit alarm":"New custom alarm",28,p.text,true));root.addView(t("A real Android alarm that can wake the screen, ring, vibrate and snooze.",13,p.muted,false),top(5));label=new EditText(this);label.setHint("Alarm label — e.g. Wake + revise DBMS");label.setTextColor(p.text);label.setHintTextColor(p.muted);label.setBackground(ThemeManager.outlined(p.surface,alpha(p.muted,50),dp(16),dp(1)));label.setPadding(dp(14),dp(12),dp(14),dp(12));root.addView(label,topHeight(18,54));time=new TimePicker(this);time.setIs24HourView(false);root.addView(time,top(10));String[] reps={"Once","Every day","Weekdays"};repeat=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,reps);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);repeat.setAdapter(a);root.addView(repeat,topHeight(8,52));vibrate=new CheckBox(this);vibrate.setText("Vibrate");vibrate.setTextColor(p.text);vibrate.setChecked(true);root.addView(vibrate,top(8));Button sound=button("Choose alarm sound",p.surfaceAlt,p.text);sound.setOnClickListener(v->pickRingtone());root.addView(sound,topHeight(8,50));soundText=t("System alarm tone",11,p.muted,false);root.addView(soundText,top(4));Button save=button("Save & schedule alarm",p.primary,Color.WHITE);save.setOnClickListener(v->save());root.addView(save,topHeight(18,58));if(alarmId>0){CustomAlarm x=new CustomAlarmRepository(this).get(alarmId);if(x!=null){label.setText(x.label);time.setHour(x.hour);time.setMinute(x.minute);repeat.setSelection("daily".equals(x.repeat)?1:"weekdays".equals(x.repeat)?2:0);vibrate.setChecked(x.vibrate);soundUri=x.soundUri;soundText.setText(soundUri.isEmpty()?"System alarm tone":"Custom ringtone selected");}}setContentView(root);}
    private void pickRingtone(){Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);startActivityForResult(i,PICK_RINGTONE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PICK_RINGTONE&&resultCode==RESULT_OK&&data!=null){Uri u=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);soundUri=u==null?"":u.toString();soundText.setText(u==null?"System alarm tone":"Custom ringtone selected");}}
    private void save(){String name=label.getText().toString().trim();if(name.isEmpty())name="Study alarm";String r=repeat.getSelectedItemPosition()==1?"daily":repeat.getSelectedItemPosition()==2?"weekdays":"once";long id=alarmId>0?alarmId:System.currentTimeMillis();CustomAlarm x=new CustomAlarm(id,name,time.getHour(),time.getMinute(),r,true,vibrate.isChecked(),soundUri);CustomAlarmRepository repo=new CustomAlarmRepository(this);repo.save(x);CustomAlarmScheduler.cancel(this,id);CustomAlarmScheduler.schedule(this,x);if(!CustomAlarmScheduler.canExact(this)){new AlertDialog.Builder(this).setTitle("Allow precise alarms?").setMessage("Android currently allows only an approximate fallback. Grant Alarms & reminders access if you want ExamVerse alarms to ring at the exact minute.").setNegativeButton("Later",(d,w)->finish()).setPositiveButton("Open settings",(d,w)->{CustomAlarmScheduler.requestExact(this);finish();}).show();}else{Toast.makeText(this,"Alarm scheduled",Toast.LENGTH_SHORT).show();finish();}}
    private TextView t(String s,float sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(b)v.setTypeface(v.getTypeface(),android.graphics.Typeface.BOLD);return v;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(fg);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setBackground(ThemeManager.rounded(bg,dp(16)));return b;}
    private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}
    private LinearLayout.LayoutParams topHeight(int d,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(h));p.topMargin=dp(d);return p;}
    private int alpha(int c,int a){return Color.argb(a,Color.red(c),Color.green(c),Color.blue(c));}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
