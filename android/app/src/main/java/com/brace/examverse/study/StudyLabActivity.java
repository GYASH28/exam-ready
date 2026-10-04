package com.brace.examverse.study;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.brace.examverse.data.*;
import com.brace.examverse.theme.ThemeManager;
import org.json.*;
import java.util.*;

/** Active recall and a correction notebook, stored entirely on device. */
public class StudyLabActivity extends Activity {
    private LearningStore store;
    private int tab;
    private String query="";
    private LinearLayout results;
    private ThemeManager.Palette p;
    @Override public void onCreate(Bundle b){super.onCreate(b);store=new LearningStore(this);tab=getIntent().getIntExtra("study_tab",0);render();}
    private void render(){
        p=ThemeManager.palette(this);ThemeManager.applyWindow(this,getWindow());
        ScrollView scroll=new ScrollView(this);LinearLayout root=column();root.setPadding(dp(18),dp(32),dp(18),dp(32));root.setBackgroundColor(p.bg);scroll.addView(root);
        root.addView(t("STUDY LAB",11,p.primary,true));root.addView(t("Turn recall into readiness.",28,p.text,true),top(4));
        root.addView(t("Practice what you know. Capture what you missed. Return when it is time to review.",13,p.muted,false),top(5));
        LinearLayout banner=card();banner.setBackground(ThemeManager.hero(this,dp(22)));banner.addView(t(store.dueCards().size()+" cards due  ·  "+store.unresolvedMistakes()+(store.unresolvedMistakes()==1?" open correction":" open corrections"),19,Color.WHITE,true));banner.addView(t("Small repetitions. Stronger memory.",12,Color.WHITE,false),top(8));root.addView(banner,top(18));
        LinearLayout tabs=new LinearLayout(this);for(int i=0;i<2;i++){final int index=i;Button b=button(i==0?"Flashcards":"Mistake notebook",tab==i?p.primary:p.surfaceAlt,tab==i?contrast(p.primary):p.text);b.setOnClickListener(v->{tab=index;query="";render();});tabs.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}root.addView(tabs,top(14));
        LinearLayout actions=new LinearLayout(this);Button add=button(tab==0?"＋ Create card":"＋ Capture mistake",p.surfaceAlt,p.text);add.setOnClickListener(v->editor(null));actions.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));
        if(tab==0){Button review=button("Review due",p.primary,contrast(p.primary));review.setOnClickListener(v->reviewNext());actions.addView(review,new LinearLayout.LayoutParams(0,dp(48),1));}root.addView(actions,top(10));
        EditText search=field("Search subject, question or correction");search.setSingleLine();search.setText(query);root.addView(search,top(14));results=column();root.addView(results,top(12));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();renderResults();}public void afterTextChanged(Editable e){}});
        Button back=button("Back to Mission Control",p.surfaceAlt,p.text);back.setOnClickListener(v->finish());root.addView(back,top(18));setContentView(scroll);renderResults();
    }
    private void renderResults(){results.removeAllViews();String key=tab==0?"cards":"mistakes";List<JSONObject> items=store.items(key);items.sort((a,b)->Long.compare(b.optLong("id"),a.optLong("id")));int shown=0;
        for(JSONObject item:items){String title=item.optString("front"),body=item.optString("back"),subject=item.optString("subject","General");if(!(title+" "+body+" "+subject).toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault())))continue;shown++;
            LinearLayout tile=card();tile.addView(t(subject.toUpperCase(Locale.getDefault()),10,p.primary,true));tile.addView(t(title,17,p.text,true),top(5));
            if(tab==1)tile.addView(t(body,13,p.muted,false),top(6));
            boolean resolved=item.optBoolean("resolved");String status=tab==0?(item.optLong("due")<=System.currentTimeMillis()?"Due for recall":"Next review · "+new java.text.SimpleDateFormat("d MMM, h:mm a",Locale.getDefault()).format(new Date(item.optLong("due")))):resolved?"✓ Corrected":"Needs another attempt";
            tile.addView(t(status,11,tab==1&&resolved?p.success:p.muted,true),top(8));
            tile.setOnClickListener(v->{if(tab==0)review(item);else{try{item.put("resolved",!resolved);store.save("mistakes",item);render();}catch(JSONException ignored){}}});
            tile.setOnLongClickListener(v->{String[] options=tab==1?new String[]{"Edit","Turn into flashcard","Delete"}:new String[]{"Edit","Delete"};new AlertDialog.Builder(this).setTitle("Manage entry").setItems(options,(d,which)->{if(which==0)editor(item);else if(tab==1&&which==1){try{JSONObject c=new JSONObject(item.toString());c.put("id",System.currentTimeMillis());c.put("due",0);c.put("interval",0);c.put("reviews",0);store.save("cards",c);Toast.makeText(this,"Flashcard created",Toast.LENGTH_SHORT).show();}catch(JSONException ignored){}}else new AlertDialog.Builder(this).setTitle("Delete this entry?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,w)->{store.delete(key,item.optLong("id"));render();}).show();}).show();return true;});results.addView(tile,top(8));
        }
        if(shown==0){LinearLayout empty=card();empty.addView(t(query.isEmpty()?(tab==0?"Build your first recall card. A question on the front, an explanation on the back.":"Capture a missed question and explain the correct reasoning in your own words."):"No matching entries.",14,p.muted,false));results.addView(empty);}
    }
    private void editor(JSONObject existing){
        LinearLayout fields=column();fields.setPadding(dp(20),dp(8),dp(20),dp(8));
        EditText front=field(tab==0?"Question or prompt":"What did you get wrong?");EditText back=field(tab==0?"Answer and explanation":"Correct reasoning / how to avoid it");front.setMinLines(2);back.setMinLines(3);fields.addView(front);fields.addView(back,top(8));
        List<Exam> exams=new ExamRepository(this).getExams();List<String> subjects=new ArrayList<>();subjects.add("General");for(Exam e:exams)if(!subjects.contains(e.subject))subjects.add(e.subject);
        Spinner subject=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,subjects);subject.setAdapter(adapter);fields.addView(subject,top(8));
        if(existing!=null){front.setText(existing.optString("front"));back.setText(existing.optString("back"));int index=subjects.indexOf(existing.optString("subject"));if(index>=0)subject.setSelection(index);}
        ScrollView wrap=new ScrollView(this);wrap.addView(fields);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?(tab==0?"New recall card":"Capture a mistake"):"Edit entry").setView(wrap).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String f=front.getText().toString().trim(),a=back.getText().toString().trim();if(f.isEmpty()||a.isEmpty()){Toast.makeText(this,"Add both the prompt and explanation",Toast.LENGTH_SHORT).show();return;}try{JSONObject item=existing==null?new JSONObject():existing;item.put("id",existing==null?System.currentTimeMillis():existing.optLong("id"));item.put("front",f);item.put("back",a);item.put("subject",subjects.get(subject.getSelectedItemPosition()));if(existing==null)item.put("due",0);store.save(tab==0?"cards":"mistakes",item);dialog.dismiss();render();}catch(JSONException ignored){}}));dialog.show();
    }
    private void reviewNext(){List<JSONObject> due=store.dueCards();if(due.isEmpty()){Toast.makeText(this,"No cards due. Your next review will appear here.",Toast.LENGTH_LONG).show();render();return;}review(due.get(0));}
    private void review(JSONObject item){
        LinearLayout content=column();content.setPadding(dp(22),dp(18),dp(22),dp(18));content.addView(t(item.optString("subject"),11,p.primary,true));content.addView(t(item.optString("front"),23,p.text,true),top(10));
        TextView answer=t(item.optString("back"),17,p.text,false);answer.setVisibility(View.GONE);content.addView(answer,top(18));
        Button reveal=button("Reveal answer",p.primary,contrast(p.primary));content.addView(reveal,top(18));LinearLayout grades=new LinearLayout(this);grades.setVisibility(View.GONE);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Active recall").setView(content).setNegativeButton("Close",(d,w)->render()).create();
        String[] labels={"Again · 10m","Good","Easy"};for(int i=0;i<3;i++){final int rating=i;Button b=button(labels[i],p.surfaceAlt,p.text);b.setTextSize(11);b.setOnClickListener(v->{store.grade(item,rating);dialog.dismiss();reviewNext();});grades.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}content.addView(grades,top(12));
        reveal.setOnClickListener(v->{answer.setVisibility(View.VISIBLE);grades.setVisibility(View.VISIBLE);reveal.setVisibility(View.GONE);});dialog.show();
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(ThemeManager.glass(this,dp(22),false));return l;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(p.text);e.setHintTextColor(p.muted);e.setTextSize(14);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(ThemeManager.rounded(p.surfaceAlt,dp(14)));return e;}
    private TextView t(String text,float size,int color,boolean bold){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setTypeface(null,bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);return v;}
    private Button button(String text,int bg,int fg){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextColor(fg);b.setTextSize(13);b.setBackground(ThemeManager.rounded(bg,dp(14)));return b;}
    private LinearLayout.LayoutParams top(int n){LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-1,-2);l.topMargin=dp(n);return l;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private int contrast(int c){return ThemeManager.foreground(c);}
}
