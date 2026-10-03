package com.brace.examverse.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;

public class WidgetConfigActivity extends Activity {
    private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID; private boolean live=false; private List<Exam> exams=new ArrayList<>();
    @Override protected void onCreate(Bundle b){super.onCreate(b);ThemeManager.applyWindow(this,getWindow());setResult(RESULT_CANCELED);Bundle e=getIntent().getExtras();if(e!=null)widgetId=e.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);live=info!=null&&info.provider.getClassName().contains("LiveExamWidgetProvider");build();}
    private void build(){ThemeManager.Palette p=ThemeManager.palette(this);WidgetStyleStore.Style current=WidgetStyleStore.get(this,widgetId);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(26));root.setBackgroundColor(p.bg);root.addView(t("Customize widget",29,p.text,true));root.addView(t("Glass style, opacity, information density and exam can be different for every widget.",13,p.muted,false),top(5));
        String[] themes={"Auto · current app theme","Shinobi glass","Saiyan glass","Soul Reaper glass","Clear frost"};String[] keys={"auto","naruto","dragonball","bleach","frost"};Spinner theme=new Spinner(this);ArrayAdapter<String> ta=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,themes);ta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);theme.setAdapter(ta);int ti=0;for(int i=0;i<keys.length;i++)if(keys[i].equals(current.theme))ti=i;theme.setSelection(ti);root.addView(theme,topHeight(18,52));
        TextView opacityLabel=t("Glass opacity · "+current.opacity,12,p.text,true);root.addView(opacityLabel,top(14));SeekBar opacity=new SeekBar(this);opacity.setMax(165);opacity.setProgress(current.opacity-70);opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean f){opacityLabel.setText("Glass opacity · "+(v+70));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});root.addView(opacity);
        String[] densityLabels={"Detailed","Compact"};Spinner density=new Spinner(this);ArrayAdapter<String> da=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,densityLabels);da.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);density.setAdapter(da);density.setSelection("compact".equals(current.density)?1:0);root.addView(density,topHeight(10,52));
        Spinner examSpinner=null;if(live){exams=new ExamRepository(this).getUpcomingExams();List<String> names=new ArrayList<>();for(Exam x:exams)names.add(x.subject+" — "+x.title);examSpinner=new Spinner(this);ArrayAdapter<String> ea=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names.isEmpty()?java.util.Collections.singletonList("No exam available"):names);ea.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);examSpinner.setAdapter(ea);for(int i=0;i<exams.size();i++)if(exams.get(i).id==current.examId)examSpinner.setSelection(i);root.addView(t("Countdown exam",12,p.text,true),top(12));root.addView(examSpinner,topHeight(5,52));}
        Spinner finalExam=examSpinner;Button save=button("Save glass widget",p.primary,Color.WHITE);save.setOnClickListener(v->{long examId=current.examId;if(live&&!exams.isEmpty()&&finalExam!=null)examId=exams.get(finalExam.getSelectedItemPosition()).id;WidgetStyleStore.save(this,widgetId,keys[theme.getSelectedItemPosition()],opacity.getProgress()+70,density.getSelectedItemPosition()==1?"compact":"detailed",examId);WidgetUpdater.updateAll(this);Intent result=new Intent();result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);setResult(RESULT_OK,result);finish();});root.addView(save,topHeight(20,58));setContentView(root);}
    private TextView t(String s,float sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(b)v.setTypeface(v.getTypeface(),android.graphics.Typeface.BOLD);return v;}private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(fg);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setBackground(ThemeManager.rounded(bg,dp(18)));return b;}private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}private LinearLayout.LayoutParams topHeight(int d,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(h));p.topMargin=dp(d);return p;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
