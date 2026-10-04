package com.brace.examverse.focus;

import android.app.*;
import android.content.*;
import android.os.Build;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.alarms.CustomAlarmScheduler;

/** Persist the deadline and attribution so leaving the activity cannot lose a session. */
public class FocusEngine {
    public static class State {
        public long id,preset,remaining,deadline,examId,topicId;
        public boolean running;
        public String mode;
    }
    private final Context context;
    private final android.content.SharedPreferences prefs;
    public FocusEngine(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("examverse_focus",0);}
    public State state(){State s=new State();s.id=prefs.getLong("id",0);s.preset=prefs.getLong("preset",25*60_000L);s.running=prefs.getBoolean("running",false);s.deadline=prefs.getLong("deadline",0);s.remaining=s.running?Math.max(0,s.deadline-System.currentTimeMillis()):prefs.getLong("remaining",s.preset);s.examId=prefs.getLong("exam",-1);s.topicId=prefs.getLong("topic",-1);s.mode=prefs.getString("mode","Pomodoro");return s;}
    public void start(long preset,long remaining,long exam,long topic,String mode){State old=state();if(old.running)return;long id=old.id>0?old.id:System.currentTimeMillis();long deadline=System.currentTimeMillis()+remaining;prefs.edit().putLong("id",id).putLong("preset",preset).putLong("remaining",remaining).putLong("deadline",deadline).putLong("exam",exam).putLong("topic",topic).putString("mode",mode).putBoolean("running",true).apply();schedule(deadline);}
    public void pause(){State s=state();prefs.edit().putBoolean("running",false).putLong("remaining",s.remaining).apply();cancel();}
    public boolean completeIfDue(){State s=state();if(!s.running||s.remaining>0)return false;log(s,(int)(s.preset/60_000L),false);reset();return true;}
    public int finishEarly(){State s=state();int minutes=(int)((s.preset-s.remaining)/60_000L);if(minutes<1)return 0;log(s,minutes,true);reset();return minutes;}
    private void log(State s,int minutes,boolean partial){if("Break".equals(s.mode))return;new ExamRepository(context).recordFocusOnce(s.id,minutes,s.examId,s.topicId,s.mode+(partial?" · partial":""));}
    public void reset(){cancel();prefs.edit().remove("id").putBoolean("running",false).remove("deadline").remove("remaining").apply();}
    public void configure(long preset,String mode){reset();prefs.edit().putLong("preset",preset).putLong("remaining",preset).putString("mode",mode).apply();}
    private PendingIntent pending(){return PendingIntent.getBroadcast(context,71,new Intent(context,FocusCompleteReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private void schedule(long deadline){AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);try{if(CustomAlarmScheduler.canExact(context))manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,deadline,pending());else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,deadline,pending());}catch(SecurityException e){manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,deadline,pending());}}
    private void cancel(){((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).cancel(pending());}
    public void reschedule(){State s=state();if(s.running)schedule(Math.max(System.currentTimeMillis()+1000,s.deadline));}
}
