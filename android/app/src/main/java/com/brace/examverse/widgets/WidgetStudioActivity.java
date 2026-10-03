package com.brace.examverse.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.brace.examverse.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class WidgetStudioActivity extends Activity {
    static class Item { final int id; final String name; Item(int id,String name){this.id=id;this.name=name;} }
    @Override protected void onCreate(Bundle b){super.onCreate(b);ThemeManager.applyWindow(this,getWindow());render();}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){ThemeManager.Palette p=ThemeManager.palette(this);ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(28));root.setBackgroundColor(p.bg);sc.addView(root);root.addView(t("Widget studio",29,p.text,true));root.addView(t("Change each installed widget independently: glass theme, transparency, information density and countdown exam.",13,p.muted,false),top(5));List<Item> items=items();if(items.isEmpty()){LinearLayout empty=card(p);empty.addView(t("No ExamVerse widgets are installed yet.",14,p.text,true));empty.addView(t("Long-press your Android home screen → Widgets → ExamVerse. When you add one, its customizer opens automatically.",12,p.muted,false),top(5));root.addView(empty,top(18));}else for(Item it:items){WidgetStyleStore.Style s=WidgetStyleStore.get(this,it.id);LinearLayout c=card(p);c.addView(t(it.name,15,p.text,true));c.addView(t("Glass: "+themeName(s.theme)+" · opacity "+s.opacity+" · "+s.density,11.5f,p.muted,false),top(4));Button edit=button("Customize",p.primary,Color.WHITE);edit.setOnClickListener(v->{Intent i=new Intent(this,WidgetConfigActivity.class);i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,it.id);startActivity(i);});c.addView(edit,topHeight(10,46));root.addView(c,top(10));}setContentView(sc);}
    private List<Item> items(){List<Item> out=new ArrayList<>();AppWidgetManager m=AppWidgetManager.getInstance(this);add(out,m,NextExamWidgetProvider.class,"Next Exam");add(out,m,LiveExamWidgetProvider.class,"Live Countdown");add(out,m,UpcomingExamWidgetProvider.class,"Upcoming Exams");add(out,m,DailyMissionWidgetProvider.class,"Daily Mission");return out;}
    private void add(List<Item> out,AppWidgetManager m,Class<?> cls,String name){int[] ids=m.getAppWidgetIds(new ComponentName(this,cls));for(int id:ids)out.add(new Item(id,name+" · #"+id));}
    private String themeName(String t){if("naruto".equals(t))return"Shinobi";if("dragonball".equals(t))return"Saiyan";if("bleach".equals(t))return"Soul Reaper";if("frost".equals(t))return"Clear frost";return"Auto";}
    private LinearLayout card(ThemeManager.Palette p){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(15),dp(16),dp(15));l.setBackground(ThemeManager.outlined(Color.argb(p.dark?220:232,Color.red(p.surface),Color.green(p.surface),Color.blue(p.surface)),Color.argb(50,Color.red(p.primary),Color.green(p.primary),Color.blue(p.primary)),dp(22),dp(1)));return l;}
    private TextView t(String s,float sp,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(b)v.setTypeface(v.getTypeface(),android.graphics.Typeface.BOLD);return v;}private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(fg);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setBackground(ThemeManager.rounded(bg,dp(16)));return b;}private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}private LinearLayout.LayoutParams topHeight(int d,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(h));p.topMargin=dp(d);return p;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
