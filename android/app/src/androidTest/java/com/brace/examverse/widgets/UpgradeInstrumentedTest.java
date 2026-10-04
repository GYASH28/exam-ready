package com.brace.examverse.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.brace.examverse.*;
import com.brace.examverse.alarms.*;
import com.brace.examverse.data.*;
import com.brace.examverse.focus.*;
import com.brace.examverse.study.*;
import com.brace.examverse.theme.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UpgradeInstrumentedTest {
    private Context c;
    private Instrumentation instrumentation;
    @Before public void setup(){instrumentation=InstrumentationRegistry.getInstrumentation();c=instrumentation.getTargetContext();for(String name:new String[]{"examverse_data","examverse_learning","examverse_focus","examverse_custom_alarms","alarm_snooze"})c.getSharedPreferences(name,0).edit().clear().commit();}
    @After public void cleanup(){c.stopService(new Intent(c,AlarmPlaybackService.class));new FocusEngine(c).reset();}

    @Test public void sixThemeImagesDecodeAndRender(){
        Set<Integer> ids=new HashSet<>();for(String key:ThemeManager.KEYS){int id=ThemeManager.artworkResource(key);assertTrue(ids.add(id));Bitmap art=BitmapFactory.decodeResource(c.getResources(),id);assertNotNull(art);assertTrue(art.getWidth()>=1000);Bitmap sample=Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable d=ThemeManager.artwork(c,key,20);d.setBounds(0,0,320,180);d.draw(new Canvas(sample));assertNotEquals(0,sample.getPixel(100,100));art.recycle();sample.recycle();}
    }
    @Test public void nativeDialogsUseReadableDarkTheme(){
        new ExamRepository(c).setTheme("blackclover");Activity activity=instrumentation.startActivitySync(new Intent(c,StudyLabActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.runOnMainSync(()->{AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Recall").setMessage("Dark theme readability").create();android.util.TypedValue background=new android.util.TypedValue();assertTrue(dialog.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground,background,true));assertTrue("Native dialogs must have a dark background",androidx.core.graphics.ColorUtils.calculateLuminance(background.data)<.3);dialog.show();dialog.dismiss();activity.finish();});
    }
    @Test public void spacedRecallAndMistakeConversionPersist() throws Exception {
        LearningStore store=new LearningStore(c);JSONObject card=new JSONObject().put("id",1).put("front","TCP vs UDP?").put("back","TCP guarantees ordered delivery.").put("subject","Networks").put("due",0);
        store.save("cards",card);assertEquals(1,store.dueCards().size());store.grade(card,1);assertEquals(0,new LearningStore(c).dueCards().size());assertEquals(1,store.items("cards").get(0).getInt("interval"));store.grade(card,2);assertEquals(3,store.items("cards").get(0).getInt("interval"));store.save("mistakes",new JSONObject(card.toString()).put("resolved",false));assertEquals(1,store.unresolvedMistakes());store.save("mistakes",card.put("resolved",true));assertEquals(0,store.unresolvedMistakes());
    }
    @Test public void backupRoundTripAndInvalidInputDoNotLoseData() throws Exception {
        ExamRepository repo=new ExamRepository(c);repo.saveExam(new Exam(101,"Operating Systems","OS","Exam","",System.currentTimeMillis()+5*86_400_000L,false,false));repo.saveTopic(new Topic(201,101,"Deadlocks",false));
        LearningStore store=new LearningStore(c);store.save("cards",new JSONObject().put("id",1).put("front","Conditions?").put("back","Four conditions").put("due",0));
        String saved=BackupStore.export(c);repo.deleteExam(101);assertEquals(0,repo.getExams().size());BackupStore.restore(c,BackupStore.validate(saved));assertEquals(1,repo.getExams().size());assertEquals(1,repo.getTopics().size());assertEquals(1,store.items("cards").size());
        JSONObject bad=new JSONObject(saved);bad.getJSONObject("data").getJSONObject("examverse_data").put("xp_v2",-100);try{BackupStore.validate(bad.toString());fail("Invalid data accepted");}catch(JSONException expected){}assertEquals(1,repo.getExams().size());
    }
    @Test public void focusRecoversAndCompletesOnlyOnce() {
        FocusEngine first=new FocusEngine(c);first.configure(25*60_000L,"Pomodoro");first.start(25*60_000L,25*60_000L,11,22,"Pomodoro");FocusEngine recovered=new FocusEngine(c);assertTrue(recovered.state().running);assertEquals(11,recovered.state().examId);recovered.pause();assertFalse(first.state().running);assertTrue(first.state().remaining>24*60_000L);
        c.getSharedPreferences("examverse_focus",0).edit().putBoolean("running",true).putLong("deadline",System.currentTimeMillis()-1).commit();assertTrue(recovered.completeIfDue());assertFalse(recovered.completeIfDue());ExamRepository repo=new ExamRepository(c);assertEquals(1,repo.getSessions().size());assertEquals(25,repo.getFocusMinutes());
        recovered.configure(5*60_000L,"Break");recovered.start(5*60_000L,1,-1,-1,"Break");c.getSharedPreferences("examverse_focus",0).edit().putLong("deadline",0).commit();assertTrue(recovered.completeIfDue());assertEquals(25,repo.getFocusMinutes());
    }
    @Test public void plannerKeepsDeadlinesAndRewardsOnlyOnce(){
        ExamRepository repo=new ExamRepository(c);long examTime=System.currentTimeMillis()+6*86_400_000L;repo.saveExam(new Exam(11,"Networks","CN","Exam","",examTime,false,false));repo.saveTopic(new Topic(12,11,"Routing",false,5,1,30,0));repo.saveTopic(new Topic(13,11,"Models",false,1,5,15,0));repo.setDailyGoal(60);assertEquals(3,repo.generateSmartPlan());for(StudyTask task:repo.getTasks()){assertTrue(task.dueAt<examTime);assertTrue(task.dueAt>System.currentTimeMillis()-1000);}
        StudyTask task=repo.getTasks().get(0);repo.toggleTask(task.id);repo.toggleTask(task.id);repo.toggleTask(task.id);assertEquals(25,repo.getXp());repo.saveTask(new StudyTask(50,-1,-1,"Overdue",System.currentTimeMillis()-86_400_000L,25,3,false,false));assertTrue(repo.getTodayTasks().stream().anyMatch(t->t.id==50));
    }
    @Test public void alarmSoundsWhileLockedWithoutNotificationTapAndSnoozes() throws Exception {
        CustomAlarm alarm=new CustomAlarm(50001,"Background alarm regression",7,0,"once",true,true,"");new CustomAlarmRepository(c).save(alarm);
        c.startActivity(new Intent(c,AlarmHubActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SystemClock.sleep(500);
        CustomAlarmScheduler.scheduleAt(c,alarm.id,System.currentTimeMillis()+3000,false);
        shell("input keyevent KEYCODE_HOME");shell("input keyevent KEYCODE_SLEEP");
        long deadline=SystemClock.elapsedRealtime()+12_000;while(!AlarmPlaybackService.isRinging()&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
        assertTrue("Alarm must play without any notification click",AlarmPlaybackService.isRinging());assertFalse(new CustomAlarmRepository(c).get(alarm.id).enabled);
        long before=System.currentTimeMillis();AlarmPlaybackService.control(c,alarm.id,AlarmPlaybackService.SNOOZE).send();SystemClock.sleep(500);assertFalse(AlarmPlaybackService.isRinging());long snooze=CustomAlarmScheduler.snoozeTime(c,alarm.id);assertTrue(snooze>=before+600_000L&&snooze<before+602_000L);CustomAlarmScheduler.cancel(c,alarm.id);assertEquals(0,CustomAlarmScheduler.snoozeTime(c,alarm.id));shell("input keyevent KEYCODE_WAKEUP");shell("wm dismiss-keyguard");
    }
    @Test public void widgetsApplyAtSingleCellWithoutRemoteViewsErrors() throws Exception {
        instrumentation.getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
        AppWidgetHost host=new AppWidgetHost(c,551);AppWidgetManager manager=AppWidgetManager.getInstance(c);
        try {
            Class<?>[] providers={NextExamWidgetProvider.class,LiveExamWidgetProvider.class,DailyMissionWidgetProvider.class,UpcomingExamWidgetProvider.class};String[] kinds={"next","live","mission","upcoming"};int[] layouts={R.layout.widget_next,R.layout.widget_live,R.layout.widget_mission,R.layout.widget_upcoming};
            for(int i=0;i<providers.length;i++){int widget=host.allocateAppWidgetId();assertTrue(manager.bindAppWidgetIdIfAllowed(widget,new ComponentName(c,providers[i])));Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,40);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,40);manager.updateAppWidgetOptions(widget,options);RemoteViews rv=new RemoteViews(c.getPackageName(),layouts[i]);WidgetStyleStore.Style style=WidgetUtil.base(c,rv,widget);WidgetUtil.applyDensity(c,rv,style,widget,kinds[i]);
                instrumentation.runOnMainSync(()->{View view=rv.apply(c,new FrameLayout(c));assertEquals(View.GONE,view.findViewById(R.id.widget_date).getVisibility());assertEquals(View.GONE,view.findViewById(R.id.widget_kicker).getVisibility());int px=Math.round(40*c.getResources().getDisplayMetrics().density);view.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY));view.layout(0,0,px,px);LinearLayout content=view.findViewById(R.id.widget_content);int total=content.getPaddingTop()+content.getPaddingBottom();for(int n=0;n<content.getChildCount();n++){View child=content.getChildAt(n);if(child.getVisibility()!=View.GONE){total+=child.getMeasuredHeight();if(child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)child.getLayoutParams();total+=lp.topMargin+lp.bottomMargin;}}}assertTrue("Single-cell content must fit its height",total<=px);});host.deleteAppWidgetId(widget);
            }
        }finally{host.deleteHost();instrumentation.getUiAutomation().dropShellPermissionIdentity();}
    }
    private void shell(String command)throws Exception{try(ParcelFileDescriptor fd=instrumentation.getUiAutomation().executeShellCommand(command);java.io.FileInputStream in=new java.io.FileInputStream(fd.getFileDescriptor())){byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}}}
}
