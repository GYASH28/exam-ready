package com.brace.examverse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.data.StudySession;
import com.brace.examverse.data.StudyTask;
import com.brace.examverse.data.Topic;
import com.brace.examverse.theme.ThemeBackdropView;
import com.brace.examverse.theme.ThemeManager;
import com.brace.examverse.widgets.WidgetUpdater;
import com.brace.examverse.widgets.WidgetStudioActivity;
import com.brace.examverse.alarms.AlarmHubActivity;
import com.brace.examverse.alarms.CustomAlarm;
import com.brace.examverse.alarms.CustomAlarmRepository;
import com.brace.examverse.wellness.ScreenTimeManager;
import com.brace.examverse.wellness.HealthHubActivity;
import com.brace.examverse.wellness.HealthSnapshotStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private ExamRepository repo;
    private FrameLayout pageContainer;
    private int currentTab = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView heroCountdown;
    private TextView focusTimeText;
    private CountDownTimer focusTimer;
    private long focusPresetMs = 25 * 60_000L;
    private long focusRemainingMs = focusPresetMs;
    private boolean focusRunning = false;
    private long focusExamId = -1;
    private long focusTopicId = -1;
    private String focusMode = "Pomodoro";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new ExamRepository(this);
        ThemeManager.applyWindow(this, getWindow());
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 91);
        }
        renderShell(); handler.post(ticker);
    }

    @Override protected void onResume() { super.onResume(); if (repo != null) renderCurrentPage(); }
    @Override protected void onDestroy() { handler.removeCallbacks(ticker); if (focusTimer != null) focusTimer.cancel(); super.onDestroy(); }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (heroCountdown != null && currentTab == 0) {
                Exam e = repo.getNextExam(); heroCountdown.setText(e == null ? "No exam scheduled" : formatRemaining(e.timeMillis, true));
            }
            if (focusRunning && focusTimeText != null) focusTimeText.setText(formatFocus(focusRemainingMs));
            handler.postDelayed(this, 1000);
        }
    };

    private void renderShell() {
        ThemeManager.applyWindow(this, getWindow()); ThemeManager.Palette p = ThemeManager.palette(this);
        FrameLayout outer = new FrameLayout(this); outer.setBackgroundColor(p.bg);
        outer.addView(new ThemeBackdropView(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout app = column(); app.setPadding(dp(14), dp(7), dp(14), dp(9));
        outer.addView(app, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        app.addView(buildTopBar());
        pageContainer = new FrameLayout(this); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); cp.topMargin = dp(4); app.addView(pageContainer, cp);
        app.addView(buildBottomNav()); setContentView(outer); renderCurrentPage();
    }

    private View buildTopBar() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(2), dp(8), dp(2), dp(7));
        LinearLayout titles = column(); titles.addView(text("ExamVerse", 24, p.text, true)); titles.addView(text(ThemeManager.motivation(this), 11.5f, p.muted, false));
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout rank = column(); rank.setGravity(Gravity.CENTER); rank.setPadding(dp(11), dp(6), dp(11), dp(6)); rank.setBackground(ThemeManager.outlined(p.surface, alpha(p.primary, 55), dp(18), dp(1)));
        rank.addView(text("LV " + repo.getLevel(), 12, p.primary, true));
        TextView rankName = text(ThemeManager.rankName(this, repo.getLevel()), 9.5f, p.muted, true); rankName.setGravity(Gravity.CENTER); rank.addView(rankName);
        rank.setOnClickListener(v -> { currentTab = 4; renderShell(); }); row.addView(rank);
        return row;
    }

    private View buildBottomNav() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(dp(3), dp(4), dp(3), dp(4)); nav.setBackground(ThemeManager.rounded(p.surface, dp(22)));
        String[] labels = {"⌂\nHome", "✓\nPlan", "⚡\nFocus", "◉\nLife", "✦\nMore"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i; Button b = new Button(this); b.setText(labels[i]); b.setTextSize(9.5f); b.setAllCaps(false); b.setGravity(Gravity.CENTER);
            b.setTextColor(currentTab == i ? p.primary : p.muted); b.setTypeface(null, currentTab == i ? Typeface.BOLD : Typeface.NORMAL); b.setBackgroundColor(Color.TRANSPARENT); b.setPadding(0, 0, 0, 0);
            b.setOnClickListener(v -> { currentTab = index; renderShell(); }); nav.addView(b, new LinearLayout.LayoutParams(0, dp(57), 1f));
        }
        return nav;
    }

    private void renderCurrentPage() {
        if (pageContainer == null) return; pageContainer.removeAllViews(); heroCountdown = null; focusTimeText = null;
        View v; if (currentTab == 1) v = buildPlanPage(); else if (currentTab == 2) v = buildFocusPage(); else if (currentTab == 3) v = buildLifePage(); else if (currentTab == 4) v = buildMorePage(); else v = buildMissionPage();
        pageContainer.setAlpha(0f); pageContainer.setTranslationY(dp(8)); pageContainer.animate().alpha(1f).translationY(0).setDuration(180).start();
        pageContainer.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ---------------- Mission Control ----------------
    private View buildMissionPage() {
        ThemeManager.Palette p = ThemeManager.palette(this); ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); LinearLayout box = column(); box.setPadding(0, dp(7), 0, dp(20)); scroll.addView(box);
        Exam next = repo.getNextExam();
        LinearLayout hero = column(); hero.setPadding(dp(20), dp(18), dp(20), dp(18)); hero.setBackground(ThemeManager.gradient(p.primary, p.secondary, dp(28)));
        TextView eyebrow = text(next == null ? "MISSION CONTROL" : next.subject.toUpperCase(Locale.getDefault()), 11.5f, contrastText(p.primary), true); eyebrow.setAlpha(.78f); hero.addView(eyebrow);
        hero.addView(text(next == null ? "No active deadline" : next.title, 24, contrastText(p.primary), true), top(5));
        heroCountdown = text(next == null ? "Add an exam to start" : formatRemaining(next.timeMillis, true), next == null ? 19 : 34, contrastText(p.primary), true); hero.addView(heroCountdown, top(10));
        if (next != null) {
            int readiness = repo.readinessForExam(next.id); LinearLayout meta = new LinearLayout(this); meta.setOrientation(LinearLayout.HORIZONTAL); meta.setGravity(Gravity.CENTER_VERTICAL);
            TextView when = text(formatDate(next.timeMillis), 11.5f, contrastText(p.primary), false); when.setAlpha(.85f); meta.addView(when, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView ready = text(readiness + "% ready", 12, contrastText(p.primary), true); ready.setPadding(dp(9), dp(5), dp(9), dp(5)); ready.setBackground(ThemeManager.rounded(alpha(Color.WHITE, 35), dp(14))); meta.addView(ready); hero.addView(meta, top(8));
        }
        box.addView(hero);

        LinearLayout quick = new LinearLayout(this); quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.addView(statTile("TODAY", repo.focusMinutesToday() + "m", "of " + repo.getDailyGoal() + "m", p), weightMargin(1f, 10, 4));
        quick.addView(statTile("STREAK", repo.getStreak() + "d", "keep it alive", p), weightMargin(1f, 10, 4));
        quick.addView(statTile("MISSIONS", String.valueOf(repo.pendingTaskCount()), "remaining", p), weightMargin(1f, 10, 0)); box.addView(quick);

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add = actionButton("＋ Add exam", p.primary, contrastText(p.primary)); add.setOnClickListener(v -> startActivity(new Intent(this, AddExamActivity.class)));
        Button plan = actionButton("✦ Smart plan", p.surfaceAlt, p.text); plan.setOnClickListener(v -> { int n = repo.generateSmartPlan(); Toast.makeText(this, n == 0 ? "Add syllabus topics first" : "Built " + n + " revision missions", Toast.LENGTH_SHORT).show(); currentTab = 1; renderShell(); });
        actions.addView(add, weightMargin(1f, 11, 5)); actions.addView(plan, weightMargin(1f, 11, 0)); box.addView(actions);

        sectionTitle(box, "Today's missions", "Small wins that reduce exam pressure.", p);
        List<StudyTask> today = repo.getTodayTasks();
        if (today.isEmpty()) {
            LinearLayout empty = card(); empty.addView(text(repo.getTasks().isEmpty() ? "No revision plan yet. Add syllabus topics, then generate a smart plan." : "Today's board is clear. You can pull tomorrow's mission forward or start a focus session.", 13, p.muted, false));
            Button go = actionButton(repo.getTasks().isEmpty() ? "Open planner" : "Start focus", p.surfaceAlt, p.text); go.setOnClickListener(v -> { currentTab = repo.getTasks().isEmpty() ? 1 : 2; renderShell(); }); empty.addView(go, topHeight(10, 46)); box.addView(empty);
        } else {
            int limit = Math.min(4, today.size()); for (int i = 0; i < limit; i++) box.addView(taskCard(today.get(i), true), marginBottom(9));
        }

        sectionTitle(box, "Upcoming exams", "Pressure is sorted by the real deadline.", p);
        List<Exam> exams = repo.getUpcomingExams();
        if (exams.isEmpty()) {
            LinearLayout empty = card(); empty.setGravity(Gravity.CENTER); empty.addView(text("Nothing is chasing you yet 😌\nCreate your first exam mission.", 13.5f, p.muted, false)); box.addView(empty);
        } else {
            for (int i = 0; i < Math.min(exams.size(), 6); i++) box.addView(examCard(exams.get(i)), marginBottom(9));
        }
        return scroll;
    }

    private View statTile(String label, String value, String sub, ThemeManager.Palette p) {
        LinearLayout l = column(); l.setPadding(dp(11), dp(11), dp(10), dp(11)); l.setBackground(ThemeManager.rounded(p.surface, dp(18)));
        l.addView(text(label, 9, p.muted, true)); l.addView(text(value, 20, p.text, true), top(2)); l.addView(text(sub, 9.5f, p.muted, false), top(1)); return l;
    }

    private View examCard(Exam e) {
        ThemeManager.Palette p = ThemeManager.palette(this); LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(13), dp(12), dp(8), dp(12)); card.setBackground(ThemeManager.rounded(p.surface, dp(21)));
        TextView badge = text(String.valueOf(e.priority), 13, contrastText(p.primary), true); badge.setGravity(Gravity.CENTER); badge.setBackground(ThemeManager.rounded(p.primary, dp(15))); card.addView(badge, new LinearLayout.LayoutParams(dp(32), dp(32)));
        LinearLayout info = column(); info.addView(text(e.title, 15.5f, p.text, true)); info.addView(text(e.subject + " · " + e.category + " · " + formatDateCompact(e.timeMillis), 11.5f, p.muted, false));
        int ready = repo.readinessForExam(e.id); info.addView(text(ready + "% readiness · target " + e.targetScore + "%", 10.5f, ready >= 70 ? p.success : p.primary, true), top(3));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); ip.leftMargin = dp(10); card.addView(info, ip);
        LinearLayout right = column(); right.setGravity(Gravity.END); right.addView(text(formatRemaining(e.timeMillis, false), 12.5f, p.primary, true));
        Button menu = new Button(this); menu.setText("⋮"); menu.setTextSize(21); menu.setTextColor(p.muted); menu.setBackgroundColor(Color.TRANSPARENT); menu.setMinWidth(0); menu.setMinimumWidth(0); menu.setPadding(dp(8), 0, 0, 0); menu.setOnClickListener(v -> showExamMenu(menu, e)); right.addView(menu, new LinearLayout.LayoutParams(dp(40), dp(38))); card.addView(right); card.setOnClickListener(v -> showExamDetail(e)); return card;
    }

    private void showExamMenu(View anchor, Exam e) {
        PopupMenu m = new PopupMenu(this, anchor); m.getMenu().add("Edit"); m.getMenu().add(e.completed ? "Mark upcoming" : "Mark complete"); m.getMenu().add("Delete");
        m.setOnMenuItemClickListener(item -> { String s = item.getTitle().toString();
            if (s.equals("Edit")) { Intent i = new Intent(this, AddExamActivity.class); i.putExtra("exam_id", e.id); startActivity(i); }
            else if (s.startsWith("Mark")) { e.completed = !e.completed; repo.saveExam(e); if (e.completed) ReminderScheduler.cancel(this, e.id); else if (e.remind) ReminderScheduler.schedule(this, e); WidgetUpdater.updateAll(this); renderCurrentPage(); }
            else { new AlertDialog.Builder(this).setTitle("Delete exam mission?").setMessage(e.title + " and its syllabus/tasks will be removed.").setNegativeButton("Cancel", null).setPositiveButton("Delete", (d,w)->{ ReminderScheduler.cancel(this, e.id); repo.deleteExam(e.id); WidgetUpdater.updateAll(this); renderCurrentPage(); }).show(); }
            return true; }); m.show();
    }

    private void showExamDetail(Exam e) {
        ThemeManager.Palette p = ThemeManager.palette(this); LinearLayout box = column(); box.setPadding(dp(20), dp(8), dp(20), dp(5));
        box.addView(text(e.subject.toUpperCase(Locale.getDefault()), 11, p.primary, true)); box.addView(text(e.title, 24, p.text, true), top(3)); box.addView(text(formatDate(e.timeMillis), 12.5f, p.muted, false), top(4));
        int ready = repo.readinessForExam(e.id); ProgressBar bar = progress(ready, p.primary); box.addView(text("Readiness " + ready + "%", 13, p.text, true), top(13)); box.addView(bar, topHeight(5, 8));
        List<Topic> topics = repo.getTopicsForExam(e.id); int done = 0; for (Topic t : topics) if (t.done) done++;
        box.addView(text(done + "/" + topics.size() + " syllabus topics · " + repo.focusMinutesForExam(e.id) + " focused min", 12, p.muted, false), top(8));
        box.addView(text("Priority " + e.priority + "/5 · Difficulty " + e.difficulty + "/5 · Target " + e.targetScore + "%", 12, p.muted, false), top(3));
        if (!e.notes.trim().isEmpty()) { box.addView(text("Notes", 12, p.primary, true), top(14)); box.addView(text(e.notes, 13, p.text, false), top(3)); }
        new AlertDialog.Builder(this).setView(box).setNegativeButton("Close", null).setPositiveButton("Edit", (d,w)->{ Intent i = new Intent(this, AddExamActivity.class); i.putExtra("exam_id", e.id); startActivity(i); }).show();
    }

    // ---------------- Smart Planner ----------------
    private View buildPlanPage() {
        ThemeManager.Palette p = ThemeManager.palette(this); ScrollView scroll = new ScrollView(this); LinearLayout box = column(); box.setPadding(0, dp(8), 0, dp(24)); scroll.addView(box);
        box.addView(text("Revision command center", 24, p.text, true)); box.addView(text("Turn deadlines + syllabus into concrete daily missions.", 12.5f, p.muted, false), top(3));
        LinearLayout hero = card(); hero.setBackground(ThemeManager.gradient(p.surfaceAlt, p.surface, dp(22))); hero.addView(text("SMART PLAN", 10.5f, p.primary, true)); hero.addView(text(repo.pendingTaskCount() + " missions remaining", 21, p.text, true), top(4));
        hero.addView(text("ExamVerse weights deadline, exam priority and topic difficulty when generating work.", 11.5f, p.muted, false), top(3));
        LinearLayout actionRow = new LinearLayout(this); actionRow.setOrientation(LinearLayout.HORIZONTAL);
        Button gen = actionButton("✦ Regenerate", p.primary, contrastText(p.primary)); gen.setOnClickListener(v->{ int n=repo.generateSmartPlan(); Toast.makeText(this,n==0?"Add syllabus topics first":"Generated " + n + " missions",Toast.LENGTH_SHORT).show(); renderCurrentPage(); });
        Button addTopic = actionButton("＋ Topic", p.surface, p.text); addTopic.setOnClickListener(v->showAddTopicDialog()); actionRow.addView(gen, weightHeight(1f,46)); actionRow.addView(addTopic, weightLeftHeight(1f,8,46)); hero.addView(actionRow, top(12)); box.addView(hero, top(12));

        sectionTitle(box, "Mission queue", "Tap the checkbox when a study block is done.", p);
        List<StudyTask> tasks = repo.getTasks();
        if (tasks.isEmpty()) {
            LinearLayout empty = card(); empty.addView(text("Your queue is empty. Add syllabus topics under an exam, then tap Regenerate.", 13, p.muted, false)); box.addView(empty);
        } else {
            int shown = 0; for (StudyTask t : tasks) { if (shown >= 12) break; box.addView(taskCard(t, false), marginBottom(8)); shown++; }
        }

        sectionTitle(box, "Syllabus map", "Track confidence, difficulty and completion per exam.", p);
        List<Exam> exams = repo.getUpcomingExams();
        if (exams.isEmpty()) box.addView(emptyMessage("Add an exam first to attach syllabus topics.", p));
        for (Exam e : exams) box.addView(syllabusExamCard(e), marginBottom(11));
        return scroll;
    }

    private View taskCard(StudyTask task, boolean compact) {
        ThemeManager.Palette p = ThemeManager.palette(this); LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(10), dp(9), dp(8), dp(9)); row.setBackground(ThemeManager.rounded(p.surface, dp(18)));
        CheckBox cb = new CheckBox(this); cb.setChecked(task.done); cb.setOnClickListener(v->{ repo.toggleTask(task.id); renderCurrentPage(); }); row.addView(cb, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout info = column(); Exam e = repo.getExam(task.examId); info.addView(text(task.title, compact ? 13.5f : 14.5f, task.done ? p.muted : p.text, true));
        String meta = (e == null ? "General" : e.subject) + " · " + task.minutes + "m · P" + task.priority + " · " + taskDateLabel(task.dueAt); info.addView(text(meta, 10.5f, p.muted, false), top(2)); row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!compact) { Button more = new Button(this); more.setText("⋮"); more.setTextSize(19); more.setTextColor(p.muted); more.setBackgroundColor(Color.TRANSPARENT); more.setOnClickListener(v->{ PopupMenu menu = new PopupMenu(this, more); menu.getMenu().add("Delete task"); menu.setOnMenuItemClickListener(i->{repo.deleteTask(task.id);renderCurrentPage();return true;}); menu.show(); }); row.addView(more, new LinearLayout.LayoutParams(dp(40),dp(40))); }
        return row;
    }

    private View syllabusExamCard(Exam e) {
        ThemeManager.Palette p = ThemeManager.palette(this); LinearLayout card = card();
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout t = column(); t.addView(text(e.subject, 17, p.text, true)); t.addView(text(e.title + " · " + repo.readinessForExam(e.id) + "% ready", 11, p.muted, false)); header.addView(t, new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button add = actionButton("＋", p.surfaceAlt, p.text); add.setOnClickListener(v->showAddTopicDialog(e.id)); header.addView(add, new LinearLayout.LayoutParams(dp(44),dp(40))); card.addView(header);
        List<Topic> topics = repo.getTopicsForExam(e.id); if (topics.isEmpty()) card.addView(text("No topics yet — add chapters or concepts here.", 12, p.muted, false), top(10));
        for (Topic topic : topics) card.addView(topicRow(topic), top(8)); return card;
    }

    private View topicRow(Topic topic) {
        ThemeManager.Palette p = ThemeManager.palette(this); LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(8), dp(7), dp(7), dp(7)); row.setBackground(ThemeManager.rounded(p.surfaceAlt, dp(14)));
        CheckBox cb = new CheckBox(this); cb.setChecked(topic.done); cb.setOnClickListener(v->{ topic.done=cb.isChecked(); if (topic.done) {topic.confidence=Math.max(topic.confidence,4);topic.lastReviewedAt=System.currentTimeMillis();} repo.saveTopic(topic); renderCurrentPage(); }); row.addView(cb,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout info=column(); info.addView(text(topic.name,13,p.text,true)); info.addView(text(topic.estimatedMinutes+"m · difficulty "+topic.difficulty+"/5 · confidence "+topic.confidence+"/5",10,p.muted,false)); row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        row.setOnLongClickListener(v->{ new AlertDialog.Builder(this).setTitle("Delete topic?").setMessage(topic.name).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{repo.deleteTopic(topic.id);renderCurrentPage();}).show();return true;}); return row;
    }

    private void showAddTopicDialog() {
        List<Exam> exams=repo.getUpcomingExams(); if(exams.isEmpty()){Toast.makeText(this,"Add an exam first",Toast.LENGTH_SHORT).show();return;}
        showAddTopicDialog(exams.get(0).id);
    }
    private void showAddTopicDialog(long preferredExamId) {
        ThemeManager.Palette p=ThemeManager.palette(this); List<Exam> exams=repo.getUpcomingExams(); if(exams.isEmpty()){Toast.makeText(this,"Add an exam first",Toast.LENGTH_SHORT).show();return;}
        LinearLayout box=column(); box.setPadding(dp(4),0,dp(4),0); EditText name=field("Topic / chapter name",p); box.addView(name);
        String[] labels=new String[exams.size()]; int sel=0; for(int i=0;i<exams.size();i++){labels[i]=exams.get(i).subject+" · "+exams.get(i).title;if(exams.get(i).id==preferredExamId)sel=i;}
        Spinner examSpinner=spinner(labels); examSpinner.setSelection(sel); box.addView(examSpinner,topHeight(8,52)); Spinner diff=spinner(new String[]{"Difficulty 1 · Easy","Difficulty 2","Difficulty 3 · Medium","Difficulty 4","Difficulty 5 · Hard"}); diff.setSelection(2); box.addView(diff,topHeight(8,52));
        Spinner estimate=spinner(new String[]{"15 min","30 min","45 min","60 min","90 min"}); estimate.setSelection(1); box.addView(estimate,topHeight(8,52)); Spinner conf=spinner(new String[]{"Confidence 1 · New","Confidence 2","Confidence 3","Confidence 4","Confidence 5 · Mastered"}); conf.setSelection(1); box.addView(conf,topHeight(8,52));
        int[] mins={15,30,45,60,90};
        new AlertDialog.Builder(this).setTitle("Add syllabus topic").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty())return;Exam e=exams.get(examSpinner.getSelectedItemPosition());repo.saveTopic(new Topic(System.currentTimeMillis(),e.id,n,false,diff.getSelectedItemPosition()+1,conf.getSelectedItemPosition()+1,mins[estimate.getSelectedItemPosition()],0L));renderCurrentPage();}).show();
    }

    // ---------------- Focus ----------------
    private View buildFocusPage() {
        ThemeManager.Palette p=ThemeManager.palette(this); ScrollView scroll=new ScrollView(this); LinearLayout box=column();box.setPadding(0,dp(8),0,dp(22));scroll.addView(box);
        box.addView(text("Focus dojo",24,p.text,true));box.addView(text("Choose what you are training, then disappear for one clean block.",12.5f,p.muted,false),top(3));
        LinearLayout timer=card();timer.setGravity(Gravity.CENTER_HORIZONTAL);timer.setPadding(dp(18),dp(20),dp(18),dp(20)); TextView mode=text(focusMode.toUpperCase(Locale.getDefault()),10.5f,p.primary,true);timer.addView(mode);
        focusTimeText=text(formatFocus(focusRemainingMs),48,p.text,true);focusTimeText.setGravity(Gravity.CENTER);timer.addView(focusTimeText,top(6));
        int today=repo.focusMinutesToday(),goal=repo.getDailyGoal();timer.addView(text("Today " + today + " / " + goal + " minutes",11.5f,p.muted,false),top(2)); ProgressBar goalBar=progress(Math.min(100,Math.round(today*100f/Math.max(1,goal))),p.primary);timer.addView(goalBar,topHeight(8,8));
        List<Exam> exams=repo.getUpcomingExams(); List<String> examLabels=new ArrayList<>();examLabels.add("General study");for(Exam e:exams)examLabels.add(e.subject+" · "+e.title);Spinner examSpinner=spinner(examLabels.toArray(new String[0]));
        int examIndex=0;for(int i=0;i<exams.size();i++)if(exams.get(i).id==focusExamId)examIndex=i+1;examSpinner.setSelection(examIndex);examSpinner.setOnItemSelectedListener(new SimpleItemSelected(pos->{long nextId=pos==0?-1:exams.get(pos-1).id;if(nextId!=focusExamId){focusExamId=nextId;focusTopicId=-1;handler.post(this::renderCurrentPage);}}));timer.addView(examSpinner,topHeight(12,50));
        List<Topic> focusTopics=focusExamId>0?repo.getTopicsForExam(focusExamId):new ArrayList<>(); List<String> topicLabels=new ArrayList<>();topicLabels.add("No specific topic");for(Topic t:focusTopics)topicLabels.add(t.name); Spinner topicSpinner=spinner(topicLabels.toArray(new String[0]));int topicIndex=0;for(int i=0;i<focusTopics.size();i++)if(focusTopics.get(i).id==focusTopicId)topicIndex=i+1;topicSpinner.setSelection(topicIndex);topicSpinner.setOnItemSelectedListener(new SimpleItemSelected(pos->{focusTopicId=pos==0?-1:focusTopics.get(pos-1).id;}));timer.addView(topicSpinner,topHeight(6,50));
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL); Button start=actionButton(focusRunning?"Pause":"Start",p.primary,contrastText(p.primary));start.setOnClickListener(v->{if(focusRunning)pauseFocus();else startFocus();renderCurrentPage();});Button finish=actionButton("Finish + log",p.surfaceAlt,p.text);finish.setOnClickListener(v->finishFocusEarly());controls.addView(start,weightHeight(1f,50));controls.addView(finish,weightLeftHeight(1f,8,50));timer.addView(controls,top(12));box.addView(timer,top(12));
        sectionTitle(box,"Training presets","Use short sprints or deep-work blocks.",p); LinearLayout presets=new LinearLayout(this);presets.setOrientation(LinearLayout.HORIZONTAL);int[] mins={25,50,90};String[] names={"25/5","50/10","90/20"};String[] modes={"Pomodoro","Long Focus","Deep Work"};
        for(int i=0;i<mins.length;i++){final int m=mins[i];final String mo=modes[i];Button b=actionButton(names[i],focusPresetMs==m*60_000L?p.primary:p.surface,focusPresetMs==m*60_000L?contrastText(p.primary):p.text);b.setOnClickListener(v->{if(!focusRunning){focusPresetMs=m*60_000L;focusRemainingMs=focusPresetMs;focusMode=mo;renderCurrentPage();}});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1f);if(i>0)lp.leftMargin=dp(7);presets.addView(b,lp);}box.addView(presets);
        sectionTitle(box,"Recent sessions","Your work, not just your intentions.",p);List<StudySession> sessions=repo.getSessions();if(sessions.isEmpty())box.addView(emptyMessage("Complete your first focus block and it will appear here.",p));else for(int i=0;i<Math.min(6,sessions.size());i++)box.addView(sessionCard(sessions.get(i)),marginBottom(7));return scroll;
    }

    private View sessionCard(StudySession s){ThemeManager.Palette p=ThemeManager.palette(this);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(13),dp(10),dp(13),dp(10));row.setBackground(ThemeManager.rounded(p.surface,dp(17)));Exam e=repo.getExam(s.examId);Topic t=repo.getTopic(s.topicId);LinearLayout info=column();info.addView(text((e==null?"General":e.subject)+(t==null?"":" · "+t.name),13,p.text,true));info.addView(text(s.mode+" · "+new SimpleDateFormat("d MMM, h:mm a",Locale.getDefault()).format(new Date(s.startedAt)),10.5f,p.muted,false));row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));row.addView(text(s.minutes+"m",18,p.primary,true));return row;}
    private void startFocus(){focusRunning=true;if(focusTimer!=null)focusTimer.cancel();focusTimer=new CountDownTimer(focusRemainingMs,1000){@Override public void onTick(long m){focusRemainingMs=m;if(focusTimeText!=null)focusTimeText.setText(formatFocus(m));}@Override public void onFinish(){focusRunning=false;focusRemainingMs=0;repo.recordFocus((int)(focusPresetMs/60_000L),focusExamId,focusTopicId,focusMode);Toast.makeText(MainActivity.this,"Focus mission complete ⚡ +XP",Toast.LENGTH_LONG).show();focusRemainingMs=focusPresetMs;if(currentTab==2)renderCurrentPage();}}.start();}
    private void pauseFocus(){focusRunning=false;if(focusTimer!=null)focusTimer.cancel();}
    private void finishFocusEarly(){long elapsed=focusPresetMs-focusRemainingMs;int mins=(int)(elapsed/60_000L);if(mins<1){Toast.makeText(this,"Study for at least one minute before logging",Toast.LENGTH_SHORT).show();return;}pauseFocus();repo.recordFocus(mins,focusExamId,focusTopicId,focusMode+" · partial");focusRemainingMs=focusPresetMs;Toast.makeText(this,mins+" focused minutes logged",Toast.LENGTH_SHORT).show();renderCurrentPage();}

    // ---------------- Life OS: screen time + health + study intelligence ----------------
    private View buildLifePage(){
        ThemeManager.Palette p=ThemeManager.palette(this);ScrollView scroll=new ScrollView(this);LinearLayout box=column();box.setPadding(0,dp(8),0,dp(24));scroll.addView(box);
        box.addView(text("Life OS",24,p.text,true));box.addView(text("Study effort beside your real device usage and recovery context — all permission based.",12.5f,p.muted,false),top(3));

        sectionTitle(box,"Digital wellbeing","Screen time comes from Android Usage Access and stays on-device.",p);
        if(!ScreenTimeManager.hasUsageAccess(this)){
            LinearLayout permission=card();permission.addView(text("Screen-time access is off",15,p.text,true));permission.addView(text("Enable Usage Access to see today's screen time, unlock count and top-used apps.",11.5f,p.muted,false),top(4));Button grant=actionButton("Enable screen-time access",p.primary,contrastText(p.primary));grant.setOnClickListener(v->ScreenTimeManager.openUsageAccessSettings(this));permission.addView(grant,topHeight(11,48));box.addView(permission);
        }else{
            ScreenTimeManager.Snapshot st=ScreenTimeManager.today(this);LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stats.addView(statTile("SCREEN",ScreenTimeManager.formatDuration(st.screenInteractiveMillis),"today",p),weightMargin(1f,0,4));stats.addView(statTile("UNLOCKS",String.valueOf(st.unlocks),"today",p),weightMargin(1f,0,4));stats.addView(statTile("FOCUS",repo.focusMinutesToday()+"m","ExamVerse",p),weightMargin(1f,0,0));box.addView(stats);
            if(!st.topApps.isEmpty()){LinearLayout apps=card();apps.addView(text("Top apps today",14,p.text,true));int max=(int)Math.min(5,st.topApps.size());for(int i=0;i<max;i++){ScreenTimeManager.AppUsage u=st.topApps.get(i);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setPadding(0,dp(8),0,dp(4));r.addView(text((i+1)+"  "+u.label,12.5f,p.text,i==0),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));r.addView(text(ScreenTimeManager.formatDuration(u.millis),12,p.primary,true));apps.addView(r);}box.addView(apps,top(10));}
        }

        sectionTitle(box,"Health & recovery","Optional Health Connect context. You decide which categories ExamVerse can read.",p);
        LinearLayout health=card();boolean connected=HealthSnapshotStore.isConnected(this);
        if(connected){long sm=HealthSnapshotStore.sleepMinutes(this);LinearLayout hs=new LinearLayout(this);hs.setOrientation(LinearLayout.HORIZONTAL);hs.addView(statTile("STEPS",String.valueOf(HealthSnapshotStore.steps(this)),"today",p),weightMargin(1f,0,4));hs.addView(statTile("SLEEP",(sm/60)+"h "+(sm%60)+"m","recent",p),weightMargin(1f,0,4));hs.addView(statTile("HEART",Math.round(HealthSnapshotStore.heartBpm(this))+" bpm","avg",p),weightMargin(1f,0,0));health.addView(hs);health.addView(text(Math.round(HealthSnapshotStore.calories(this))+" kcal · "+HealthSnapshotStore.exerciseMinutes(this)+" min exercise",11.5f,p.muted,false),top(8));}
        else health.addView(text("Health Connect is not connected yet.",13,p.muted,false));
        Button sync=actionButton(connected?"Manage / sync health":"Connect Health Connect",p.surfaceAlt,p.text);sync.setOnClickListener(v->startActivity(new Intent(this,HealthHubActivity.class)));health.addView(sync,topHeight(10,48));box.addView(health);

        sectionTitle(box,"Study intelligence","Focus trends and exam readiness stay part of the same dashboard.",p);
        LinearLayout rank=card();rank.setBackground(ThemeManager.gradient(p.primary,p.secondary,dp(24)));int xp=repo.getXp(),lvl=repo.getLevel();rank.addView(text("LEVEL "+lvl+" · "+ThemeManager.rankName(this,lvl),11,contrastText(p.primary),true));rank.addView(text(xp+" XP",27,contrastText(p.primary),true),top(3));rank.addView(text(repo.getStreak()+" day streak · "+repo.getSessions().size()+" sessions · "+repo.getFocusMinutes()+" total focused min",11,contrastText(p.primary),false),top(3));box.addView(rank);
        List<Exam> exams=repo.getUpcomingExams();for(int i=0;i<Math.min(3,exams.size());i++){Exam e=exams.get(i);int ready=repo.readinessForExam(e.id);LinearLayout c=card();LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.addView(text(e.subject,13,p.text,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));r.addView(text(ready+"% ready",13,ready>=70?p.success:p.primary,true));c.addView(r);c.addView(progress(ready,ready>=70?p.success:p.primary),topHeight(7,7));box.addView(c,top(8));}
        return scroll;
    }

    // ---------------- Insights ----------------
    private View buildInsightsPage(){ThemeManager.Palette p=ThemeManager.palette(this);ScrollView scroll=new ScrollView(this);LinearLayout box=column();box.setPadding(0,dp(8),0,dp(24));scroll.addView(box);box.addView(text("Study intelligence",24,p.text,true));box.addView(text("See where your time goes and whether effort is matching exam pressure.",12.5f,p.muted,false),top(3));
        LinearLayout rank=card();rank.setBackground(ThemeManager.gradient(p.primary,p.secondary,dp(24)));int xp=repo.getXp(),lvl=repo.getLevel();rank.addView(text("LEVEL "+lvl+" · "+ThemeManager.rankName(this,lvl),11,contrastText(p.primary),true));rank.addView(text(xp+" XP",29,contrastText(p.primary),true),top(4));TextView next=text((500-repo.getXpIntoLevel())+" XP to next level",11,contrastText(p.primary),false);next.setAlpha(.8f);rank.addView(next);ProgressBar xpbar=progress(Math.round(repo.getXpIntoLevel()*100f/500f),Color.WHITE);rank.addView(xpbar,topHeight(8,8));box.addView(rank,top(12));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stats.addView(statTile("TOTAL",repo.getFocusMinutes()+"m","focused",p),weightMargin(1f,10,4));stats.addView(statTile("SESSIONS",String.valueOf(repo.getSessions().size()),"completed",p),weightMargin(1f,10,4));stats.addView(statTile("STREAK",repo.getStreak()+"d","current",p),weightMargin(1f,10,0));box.addView(stats);
        sectionTitle(box,"Last 7 days","Focused minutes per day.",p);LinearLayout chart=card();int[] values=repo.last7DaysMinutes();int max=1;for(int v:values)max=Math.max(max,v);LinearLayout bars=new LinearLayout(this);bars.setOrientation(LinearLayout.HORIZONTAL);bars.setGravity(Gravity.BOTTOM);Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_MONTH,-6);
        for(int i=0;i<7;i++){LinearLayout c=column();c.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);TextView val=text(String.valueOf(values[i]),9,p.muted,true);val.setGravity(Gravity.CENTER);c.addView(val);View bar=new View(this);bar.setBackground(ThemeManager.rounded(values[i]>0?p.primary:p.surfaceAlt,dp(8)));int h=Math.max(dp(6),Math.round(dp(110)*(values[i]/(float)max)));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(20),h);bp.topMargin=dp(5);c.addView(bar,bp);TextView day=text(new SimpleDateFormat("EEE",Locale.getDefault()).format(cal.getTime()),9,p.muted,false);day.setGravity(Gravity.CENTER);c.addView(day,top(5));bars.addView(c,new LinearLayout.LayoutParams(0,dp(158),1f));cal.add(Calendar.DAY_OF_MONTH,1);}chart.addView(bars);box.addView(chart);
        sectionTitle(box,"Exam readiness","Completion + confidence + practice time.",p);List<Exam> exams=repo.getUpcomingExams();if(exams.isEmpty())box.addView(emptyMessage("Add exams and topics to unlock readiness insights.",p));for(Exam e:exams){int ready=repo.readinessForExam(e.id);LinearLayout c=card();LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.addView(text(e.subject,14,p.text,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));r.addView(text(ready+"%",16,ready>=70?p.success:p.primary,true));c.addView(r);c.addView(progress(ready,ready>=70?p.success:p.primary),topHeight(7,8));c.addView(text(e.title+" · "+formatRemaining(e.timeMillis,false)+" left",10.5f,p.muted,false),top(6));box.addView(c,marginBottom(8));}
        Map<String,Integer> bySubject=repo.focusMinutesBySubject();if(!bySubject.isEmpty()){sectionTitle(box,"Time by subject","Where your study time has actually gone.",p);for(Map.Entry<String,Integer> entry:bySubject.entrySet()){LinearLayout r=card();LinearLayout rr=new LinearLayout(this);rr.setOrientation(LinearLayout.HORIZONTAL);rr.addView(text(entry.getKey(),13,p.text,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));rr.addView(text(entry.getValue()+"m",14,p.primary,true));r.addView(rr);box.addView(r,marginBottom(7));}}
        return scroll;}

    // ---------------- Themes + settings ----------------
    private View buildMorePage(){ThemeManager.Palette p=ThemeManager.palette(this);ScrollView scroll=new ScrollView(this);LinearLayout box=column();box.setPadding(0,dp(8),0,dp(24));scroll.addView(box);box.addView(text("Control center",24,p.text,true));box.addView(text("Alarms, widgets, themes and privacy controls live here.",12.5f,p.muted,false),top(3));
        sectionTitle(box,"Alarm studio","Wake-up, study-start and revision alarms with snooze + custom sound.",p);LinearLayout alarm=card();List<CustomAlarm> alarms=new CustomAlarmRepository(this).getAll();alarm.addView(text(alarms.isEmpty()?"No custom alarms yet":alarms.size()+" custom alarm"+(alarms.size()==1?"":"s")+" configured",14,p.text,true));alarm.addView(text("Exact timing uses Android's Alarms & reminders access. Repeat rules: once, daily or weekdays.",11,p.muted,false),top(4));Button alarmsBtn=actionButton("Open Alarm Studio",p.primary,contrastText(p.primary));alarmsBtn.setOnClickListener(v->startActivity(new Intent(this,AlarmHubActivity.class)));alarm.addView(alarmsBtn,topHeight(11,48));box.addView(alarm);
        sectionTitle(box,"Widget studio","Every widget has its own glass theme, opacity, density and exam selection.",p);LinearLayout widgets=card();widgets.addView(text("Frosted glass widgets · per-widget styles · compact/detailed layouts",13,p.text,true));Button widgetBtn=actionButton("Customize installed widgets",p.surfaceAlt,p.text);widgetBtn.setOnClickListener(v->startActivity(new Intent(this,WidgetStudioActivity.class)));widgets.addView(widgetBtn,topHeight(10,48));box.addView(widgets);
        sectionTitle(box,"Anime battle modes","Change the app's visual identity, ranks and default widget accent.",p);
        box.addView(themeCard("naruto","Shinobi Mode","Naruto-inspired · chakra orange · ink + paper","Genin → Chunin → Jonin → Hokage",Color.rgb(249,115,22),Color.rgb(17,24,39)),top(12));
        box.addView(themeCard("dragonball","Saiyan Mode","Dragon Ball-inspired · gold energy · cobalt training","Training → Elite → Super Saiyan → Ultra Instinct",Color.rgb(245,158,11),Color.rgb(37,99,235)),top(10));
        box.addView(themeCard("bleach","Soul Reaper Mode","Bleach-inspired · monochrome · crimson blade","Soul Reaper → Shikai → Bankai → Captain",Color.rgb(20,20,22),Color.rgb(220,38,38)),top(10));
        sectionTitle(box,"Daily training goal","Used by Mission Control and Focus mode.",p);LinearLayout goal=card();LinearLayout gr=new LinearLayout(this);gr.setOrientation(LinearLayout.HORIZONTAL);gr.setGravity(Gravity.CENTER_VERTICAL);Button minus=actionButton("−",p.surfaceAlt,p.text);TextView g=text(repo.getDailyGoal()+" min / day",20,p.text,true);g.setGravity(Gravity.CENTER);Button plus=actionButton("＋",p.surfaceAlt,p.text);minus.setOnClickListener(v->{repo.setDailyGoal(repo.getDailyGoal()-15);renderCurrentPage();});plus.setOnClickListener(v->{repo.setDailyGoal(repo.getDailyGoal()+15);renderCurrentPage();});gr.addView(minus,new LinearLayout.LayoutParams(dp(48),dp(44)));gr.addView(g,new LinearLayout.LayoutParams(0,dp(44),1f));gr.addView(plus,new LinearLayout.LayoutParams(dp(48),dp(44)));goal.addView(gr);box.addView(goal);
        sectionTitle(box,"Offline by design","Your exams, topics, plans and sessions stay on-device in this build.",p);LinearLayout privacy=card();privacy.addView(text("✓ No account required\n✓ No ads\n✓ No analytics SDK\n✓ Works without internet\n✓ Health and usage access are opt-in\n✓ Home-screen widgets can be customized independently",12.5f,p.text,false));box.addView(privacy);
        TextView note=text("Fan-style theme names are included for a private build. The app ships no copied anime screenshots, logos or character art. Use licensed branding or rename themes before public store distribution.",10.5f,p.muted,false);box.addView(note,top(14));return scroll;}

    private View themeCard(String key,String name,String subtitle,String ranks,int c1,int c2){ThemeManager.Palette p=ThemeManager.palette(this);boolean selected=repo.getTheme().equals(key);LinearLayout card=column();card.setPadding(dp(18),dp(16),dp(18),dp(16));card.setBackground(ThemeManager.gradient(c1,c2,dp(24)));TextView title=text(name+(selected?"  ✓":""),21,Color.WHITE,true);card.addView(title);TextView sub=text(subtitle,11.5f,Color.WHITE,false);sub.setAlpha(.86f);card.addView(sub,top(3));TextView r=text(ranks,10,Color.WHITE,true);r.setAlpha(.78f);card.addView(r,top(8));card.setOnClickListener(v->{repo.setTheme(key);WidgetUpdater.updateAll(this);renderShell();});return card;}

    // ---------------- UI helpers ----------------
    private void sectionTitle(LinearLayout box,String title,String subtitle,ThemeManager.Palette p){TextView h=text(title,17.5f,p.text,true);LinearLayout.LayoutParams hp=top(20);box.addView(h,hp);box.addView(text(subtitle,10.8f,p.muted,false),top(2));}
    private LinearLayout emptyMessage(String msg,ThemeManager.Palette p){LinearLayout l=card();l.addView(text(msg,12.5f,p.muted,false));return l;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(){ThemeManager.Palette p=ThemeManager.palette(this);LinearLayout l=column();l.setPadding(dp(16),dp(15),dp(16),dp(15));l.setBackground(ThemeManager.outlined(alpha(p.surface,p.dark?220:232),alpha(p.primary,45),dp(22),dp(1)));l.setElevation(dp(2));return l;}
    private Button actionButton(String label,int bg,int fg){Button b=new Button(this);b.setText(label);b.setTextSize(12.5f);b.setTextColor(fg);b.setAllCaps(false);b.setTypeface(null,Typeface.BOLD);b.setBackground(ThemeManager.outlined(bg,alpha(Color.WHITE,45),dp(17),dp(1)));b.setPadding(dp(8),0,dp(8),0);b.setElevation(dp(1));return b;}
    private TextView text(String value,float sp,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(null,bold?Typeface.BOLD:Typeface.NORMAL);t.setLineSpacing(0,1.08f);return t;}
    private EditText field(String hint,ThemeManager.Palette p){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(p.text);e.setHintTextColor(p.muted);e.setTextSize(14);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(ThemeManager.outlined(p.surfaceAlt,alpha(p.muted,45),dp(14),dp(1)));return e;}
    private Spinner spinner(String[] labels){
        ThemeManager.Palette p=ThemeManager.palette(this); Spinner s=new Spinner(this);
        ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){
            @Override public View getView(int position,View convertView,ViewGroup parent){View v=super.getView(position,convertView,parent);if(v instanceof TextView){((TextView)v).setTextColor(p.text);((TextView)v).setTextSize(12.5f);v.setPadding(dp(8),0,dp(8),0);}return v;}
            @Override public View getDropDownView(int position,View convertView,ViewGroup parent){View v=super.getDropDownView(position,convertView,parent);if(v instanceof TextView){((TextView)v).setTextColor(p.text);v.setBackgroundColor(p.surface);v.setPadding(dp(12),dp(11),dp(12),dp(11));}return v;}
        }; s.setAdapter(a); s.setBackground(ThemeManager.rounded(p.surfaceAlt,dp(14))); return s;
    }
    private ProgressBar progress(int value,int color){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(Math.max(0,Math.min(100,value)));if(Build.VERSION.SDK_INT>=21)bar.getProgressDrawable().setTint(color);return bar;}
    private int alpha(int color,int a){return Color.argb(a,Color.red(color),Color.green(color),Color.blue(color));}
    private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}
    private LinearLayout.LayoutParams topHeight(int d,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(h));p.topMargin=dp(d);return p;}
    private LinearLayout.LayoutParams marginBottom(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.bottomMargin=dp(d);return p;}
    private LinearLayout.LayoutParams weightMargin(float w,int top,int right){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,w);p.topMargin=dp(top);p.rightMargin=dp(right);return p;}
    private LinearLayout.LayoutParams weightHeight(float w,int h){return new LinearLayout.LayoutParams(0,dp(h),w);}
    private LinearLayout.LayoutParams weightLeftHeight(float w,int left,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(h),w);p.leftMargin=dp(left);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private int contrastText(int bg){double y=(299*Color.red(bg)+587*Color.green(bg)+114*Color.blue(bg))/1000.0;return y>160?Color.rgb(17,24,39):Color.WHITE;}
    private String formatRemaining(long when,boolean detailed){long diff=Math.max(0,when-System.currentTimeMillis());long days=diff/86_400_000L;diff%=86_400_000L;long hours=diff/3_600_000L;diff%=3_600_000L;long mins=diff/60_000L;long sec=(diff%60_000L)/1000L;if(detailed){if(days>0)return days+"d  "+hours+"h  "+mins+"m";return hours+"h  "+mins+"m  "+sec+"s";}if(days>0)return days+"d "+hours+"h";if(hours>0)return hours+"h "+mins+"m";return mins+"m";}
    private String formatFocus(long ms){long total=Math.max(0,ms)/1000;return String.format(Locale.US,"%02d:%02d",total/60,total%60);}
    private String formatDate(long time){return new SimpleDateFormat("EEE, d MMM yyyy · h:mm a",Locale.getDefault()).format(new Date(time));}
    private String formatDateCompact(long time){return new SimpleDateFormat("d MMM · h:mm a",Locale.getDefault()).format(new Date(time));}
    private String taskDateLabel(long time){String today=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());String target=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date(time));if(today.equals(target))return"Today";Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_MONTH,1);if(new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime()).equals(target))return"Tomorrow";return new SimpleDateFormat("d MMM",Locale.getDefault()).format(new Date(time));}

    private static class SimpleItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        interface Callback { void onSelect(int position); } private final Callback callback; SimpleItemSelected(Callback c){callback=c;}
        @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){callback.onSelect(position);}
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
    }
}
