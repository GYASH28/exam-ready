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
import com.brace.examverse.visuals.FocusTrendView;
import com.brace.examverse.visuals.StudyDotHeatmapView;
import com.brace.examverse.visuals.ReadinessDotGraphView;

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
        ThemeManager.applyWindow(this, getWindow());
        ThemeManager.Palette p = ThemeManager.palette(this);

        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(p.bg);
        outer.addView(new ThemeBackdropView(this),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout app = column();
        app.setPadding(dp(14), dp(6), dp(14), dp(9));
        outer.addView(app, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        app.addView(buildTopBar());

        pageContainer = new FrameLayout(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cp.topMargin = dp(6);
        app.addView(pageContainer, cp);

        app.addView(buildBottomNav());
        setContentView(outer);
        renderCurrentPage();
    }

    private View buildTopBar() {
        ThemeManager.Palette p = ThemeManager.palette(this);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(9), dp(4), dp(8));

        LinearLayout titles = column();
        TextView brand = text("EXAMVERSE", 10.5f, p.primary, true);
        brand.setLetterSpacing(.16f);
        titles.addView(brand);
        titles.addView(text("Your exam readiness OS", 20.5f, p.text, true), top(1));
        titles.addView(text(ThemeManager.displayName(this) + "  ·  " + ThemeManager.motivation(this), 10.5f, p.muted, false), top(2));
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout rank = column();
        rank.setGravity(Gravity.CENTER);
        rank.setPadding(dp(12), dp(7), dp(12), dp(7));
        rank.setBackground(ThemeManager.glowChip(this, dp(18)));
        TextView lv = text("LV " + repo.getLevel(), 11.5f, p.primary, true);
        lv.setGravity(Gravity.CENTER);
        rank.addView(lv);
        TextView rankName = text(ThemeManager.rankName(this, repo.getLevel()), 9.3f, p.text, true);
        rankName.setGravity(Gravity.CENTER);
        rank.addView(rankName, top(1));
        rank.setOnClickListener(v -> { currentTab = 4; renderShell(); });
        row.addView(rank);
        return row;
    }

    private View buildBottomNav() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(5), dp(5), dp(5), dp(5));
        nav.setBackground(ThemeManager.glass(this, dp(24), true));
        nav.setElevation(dp(5));

        String[] icons = {"⌂", "✓", "⚡", "◉", "✦"};
        String[] labels = {"Home", "Plan", "Focus", "Life", "Studio"};

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(3), dp(5), dp(3), dp(5));
            if (currentTab == i) item.setBackground(ThemeManager.glowChip(this, dp(18)));

            TextView icon = text(icons[i], 16, currentTab == i ? p.primary : p.muted, true);
            icon.setGravity(Gravity.CENTER);
            item.addView(icon);
            TextView label = text(labels[i], 9.2f, currentTab == i ? p.primary : p.muted, currentTab == i);
            label.setGravity(Gravity.CENTER);
            item.addView(label, top(1));

            item.setOnClickListener(v -> { currentTab = index; renderShell(); });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1f));
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
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout box = column();
        box.setPadding(0, dp(5), 0, dp(26));
        scroll.addView(box);

        Exam next = repo.getNextExam();
        int nextReadiness = next == null ? 0 : repo.readinessForExam(next.id);

        LinearLayout hero = column();
        hero.setPadding(dp(20), dp(18), dp(20), dp(19));
        hero.setBackground(ThemeManager.hero(this, dp(30)));
        hero.setElevation(dp(5));

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setOrientation(LinearLayout.HORIZONTAL);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = text(next == null ? "READINESS MODE" : next.subject.toUpperCase(Locale.getDefault()),
                10.8f, Color.WHITE, true);
        eyebrow.setLetterSpacing(.13f);
        heroTop.addView(eyebrow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView mode = text(ThemeManager.displayName(this), 9.5f, Color.WHITE, true);
        mode.setAlpha(.88f);
        mode.setPadding(dp(9), dp(5), dp(9), dp(5));
        mode.setBackground(ThemeManager.outlined(alpha(Color.BLACK, 28), alpha(Color.WHITE, 58), dp(14), dp(1)));
        heroTop.addView(mode);
        hero.addView(heroTop);

        hero.addView(text(next == null ? "No exam pressure yet" : next.title, 24.5f, Color.WHITE, true), top(7));
        heroCountdown = text(next == null ? "Create your first exam" : formatRemaining(next.timeMillis, true),
                next == null ? 18 : 35, Color.WHITE, true);
        hero.addView(heroCountdown, top(8));

        if (next != null) {
            TextView date = text(formatDate(next.timeMillis), 11.2f, Color.WHITE, false);
            date.setAlpha(.80f);
            hero.addView(date, top(5));

            LinearLayout readinessRow = new LinearLayout(this);
            readinessRow.setOrientation(LinearLayout.HORIZONTAL);
            readinessRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView ready = text("READINESS  " + nextReadiness + "%", 10.2f, Color.WHITE, true);
            ready.setLetterSpacing(.06f);
            readinessRow.addView(ready, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            readinessRow.addView(text("P" + next.priority + "  ·  target " + next.targetScore + "%", 10.2f, Color.WHITE, true));
            hero.addView(readinessRow, top(12));

            ProgressBar readyBar = progress(nextReadiness, Color.WHITE);
            hero.addView(readyBar, topHeight(6, 7));
        } else {
            Button create = actionButton("Create exam mission", alpha(Color.WHITE, 235), p.heroEnd);
            create.setOnClickListener(v -> startActivity(new Intent(this, AddExamActivity.class)));
            hero.addView(create, topHeight(14, 48));
        }
        box.addView(hero);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statTile("TODAY", repo.focusMinutesToday() + "m", "goal " + repo.getDailyGoal() + "m", p), weightMargin(1f, 11, 5));
        stats.addView(statTile("STREAK", repo.getStreak() + "d", "consistency", p), weightMargin(1f, 11, 5));
        stats.addView(statTile("READY", repo.averageReadiness() + "%", repo.examsNeedingAttention() + " need care", p), weightMargin(1f, 11, 0));
        box.addView(stats);

        List<StudyTask> today = repo.getTodayTasks();
        LinearLayout command = card();
        LinearLayout commandHead = new LinearLayout(this);
        commandHead.setOrientation(LinearLayout.HORIZONTAL);
        commandHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout commandText = column();
        commandText.addView(text("TODAY'S COMMAND", 9.5f, p.primary, true));
        if (today.isEmpty()) {
            commandText.addView(text(repo.getTasks().isEmpty() ? "Build your revision map" : "Your board is clear", 17, p.text, true), top(2));
            commandText.addView(text(repo.getTasks().isEmpty() ? "Add topics and let Smart Plan split the workload." : "Use the free space for a focused review sprint.", 10.5f, p.muted, false), top(2));
        } else {
            StudyTask first = today.get(0);
            commandText.addView(text(first.title, 17, p.text, true), top(2));
            commandText.addView(text(today.size() + " mission" + (today.size() == 1 ? "" : "s") + " today  ·  " + first.minutes + " min first sprint", 10.5f, p.muted, false), top(2));
        }
        commandHead.addView(commandText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button commandAction = actionButton(today.isEmpty() && repo.getTasks().isEmpty() ? "PLAN" : "START", p.primary, contrastText(p.primary));
        commandAction.setOnClickListener(v -> {
            currentTab = (today.isEmpty() && repo.getTasks().isEmpty()) ? 1 : 2;
            renderShell();
        });
        LinearLayout.LayoutParams cap = new LinearLayout.LayoutParams(dp(76), dp(44));
        cap.leftMargin = dp(10);
        commandHead.addView(commandAction, cap);
        command.addView(commandHead);
        box.addView(command, top(10));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        Button add = actionButton("＋ Exam", p.surfaceAlt, p.text);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddExamActivity.class)));
        Button plan = actionButton("✦ Smart plan", p.surfaceAlt, p.text);
        plan.setOnClickListener(v -> {
            int n = repo.generateSmartPlan();
            Toast.makeText(this, n == 0 ? "Add syllabus topics first" : "Built " + n + " revision missions", Toast.LENGTH_SHORT).show();
            currentTab = 1;
            renderShell();
        });
        quick.addView(add, weightMargin(1f, 10, 5));
        quick.addView(plan, weightMargin(1f, 10, 0));
        box.addView(quick);

        List<Exam> exams = repo.getUpcomingExams();
        if (!exams.isEmpty()) {
            sectionTitle(box, "Readiness map", "Sooner exams move left. Higher readiness moves up. Larger dots mean higher priority.", p);
            LinearLayout graphCard = card();
            ReadinessDotGraphView graph = new ReadinessDotGraphView(this).setExams(exams);
            graphCard.addView(graph, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
            box.addView(graphCard);
        }

        sectionTitle(box, "Today's missions", "The smallest useful actions, already ordered for you.", p);
        if (today.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text(repo.getTasks().isEmpty()
                    ? "No revision plan yet. Add syllabus topics, then generate a Smart Plan."
                    : "Today's board is clear. Start a focus sprint or pull a mission forward.",
                    12.5f, p.muted, false));
            box.addView(empty);
        } else {
            int limit = Math.min(5, today.size());
            for (int i = 0; i < limit; i++) box.addView(taskCard(today.get(i), true), marginBottom(9));
        }

        sectionTitle(box, "Upcoming exams", "A clean deadline view with readiness and target score.", p);
        if (exams.isEmpty()) {
            LinearLayout empty = card();
            empty.setGravity(Gravity.CENTER);
            empty.addView(text("Nothing is chasing you yet. Add an exam when you are ready.", 13, p.muted, false));
            box.addView(empty);
        } else {
            for (int i = 0; i < Math.min(exams.size(), 7); i++) box.addView(examCard(exams.get(i)), marginBottom(9));
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
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout box = column();
        box.setPadding(0, dp(7), 0, dp(26));
        scroll.addView(box);

        TextView kicker = text("FOCUS ENGINE", 10, p.primary, true);
        kicker.setLetterSpacing(.14f);
        box.addView(kicker);
        box.addView(text("One block. One target. No chaos.", 24, p.text, true), top(2));
        box.addView(text("Attach the session to an exam or topic so every minute improves your readiness map.", 11.7f, p.muted, false), top(3));

        LinearLayout timer = column();
        timer.setGravity(Gravity.CENTER_HORIZONTAL);
        timer.setPadding(dp(18), dp(18), dp(18), dp(18));
        timer.setBackground(ThemeManager.hero(this, dp(30)));
        timer.setElevation(dp(5));

        LinearLayout timerHead = new LinearLayout(this);
        timerHead.setOrientation(LinearLayout.HORIZONTAL);
        timerHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView mode = text(focusMode.toUpperCase(Locale.getDefault()), 10.3f, Color.WHITE, true);
        mode.setLetterSpacing(.12f);
        timerHead.addView(mode, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView status = text(focusRunning ? "IN SESSION" : "READY", 9.2f, Color.WHITE, true);
        status.setPadding(dp(9), dp(5), dp(9), dp(5));
        status.setBackground(ThemeManager.outlined(alpha(Color.BLACK, 28), alpha(Color.WHITE, 65), dp(13), dp(1)));
        timerHead.addView(status);
        timer.addView(timerHead);

        focusTimeText = text(formatFocus(focusRemainingMs), 50, Color.WHITE, true);
        focusTimeText.setGravity(Gravity.CENTER);
        timer.addView(focusTimeText, top(8));

        int today = repo.focusMinutesToday(), goal = repo.getDailyGoal();
        LinearLayout todayRow = new LinearLayout(this);
        todayRow.setOrientation(LinearLayout.HORIZONTAL);
        todayRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView todayText = text("TODAY  " + today + " / " + goal + " min", 10.2f, Color.WHITE, true);
        todayText.setAlpha(.88f);
        todayRow.addView(todayText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView streak = text(repo.getStreak() + "d streak", 10.2f, Color.WHITE, true);
        streak.setAlpha(.88f);
        todayRow.addView(streak);
        timer.addView(todayRow, top(9));

        ProgressBar goalBar = progress(Math.min(100, Math.round(today * 100f / Math.max(1, goal))), Color.WHITE);
        timer.addView(goalBar, topHeight(6, 7));
        box.addView(timer, top(12));

        LinearLayout target = card();
        TextView targetLabel = text("SESSION TARGET", 9.5f, p.primary, true);
        targetLabel.setLetterSpacing(.10f);
        target.addView(targetLabel);

        List<Exam> exams = repo.getUpcomingExams();
        List<String> examLabels = new ArrayList<>();
        examLabels.add("General study");
        for (Exam e : exams) examLabels.add(e.subject + " · " + e.title);
        Spinner examSpinner = spinner(examLabels.toArray(new String[0]));

        int examIndex = 0;
        for (int i = 0; i < exams.size(); i++) if (exams.get(i).id == focusExamId) examIndex = i + 1;
        examSpinner.setSelection(examIndex);
        examSpinner.setOnItemSelectedListener(new SimpleItemSelected(pos -> {
            long nextId = pos == 0 ? -1 : exams.get(pos - 1).id;
            if (nextId != focusExamId) {
                focusExamId = nextId;
                focusTopicId = -1;
                handler.post(this::renderCurrentPage);
            }
        }));
        target.addView(examSpinner, topHeight(9, 50));

        List<Topic> focusTopics = focusExamId > 0 ? repo.getTopicsForExam(focusExamId) : new ArrayList<>();
        List<String> topicLabels = new ArrayList<>();
        topicLabels.add("No specific topic");
        for (Topic t : focusTopics) topicLabels.add(t.name);
        Spinner topicSpinner = spinner(topicLabels.toArray(new String[0]));

        int topicIndex = 0;
        for (int i = 0; i < focusTopics.size(); i++) if (focusTopics.get(i).id == focusTopicId) topicIndex = i + 1;
        topicSpinner.setSelection(topicIndex);
        topicSpinner.setOnItemSelectedListener(new SimpleItemSelected(pos -> {
            focusTopicId = pos == 0 ? -1 : focusTopics.get(pos - 1).id;
        }));
        target.addView(topicSpinner, topHeight(7, 50));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button start = actionButton(focusRunning ? "Pause session" : "Start focus", p.primary, contrastText(p.primary));
        start.setOnClickListener(v -> {
            if (focusRunning) pauseFocus(); else startFocus();
            renderCurrentPage();
        });
        Button finish = actionButton("Finish + log", p.surfaceAlt, p.text);
        finish.setOnClickListener(v -> finishFocusEarly());
        controls.addView(start, weightHeight(1f, 50));
        controls.addView(finish, weightLeftHeight(1f, 8, 50));
        target.addView(controls, top(11));

        if (!focusRunning && focusRemainingMs != focusPresetMs) {
            Button reset = actionButton("Reset timer", p.surfaceAlt, p.muted);
            reset.setOnClickListener(v -> {
                focusRemainingMs = focusPresetMs;
                renderCurrentPage();
            });
            target.addView(reset, topHeight(7, 44));
        }
        box.addView(target, top(10));

        sectionTitle(box, "Training presets", "Pick the amount of attention you can realistically protect.", p);
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        int[] mins = {25, 50, 90};
        String[] names = {"25 / 5", "50 / 10", "90 / 20"};
        String[] modes = {"Pomodoro", "Long Focus", "Deep Work"};

        for (int i = 0; i < mins.length; i++) {
            final int m = mins[i];
            final String mo = modes[i];
            boolean selected = focusPresetMs == m * 60_000L;
            Button b = actionButton(names[i], selected ? p.primary : p.surfaceAlt, selected ? contrastText(p.primary) : p.text);
            b.setOnClickListener(v -> {
                if (!focusRunning) {
                    focusPresetMs = m * 60_000L;
                    focusRemainingMs = focusPresetMs;
                    focusMode = mo;
                    renderCurrentPage();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            if (i > 0) lp.leftMargin = dp(7);
            presets.addView(b, lp);
        }
        box.addView(presets);

        sectionTitle(box, "Recent sessions", "Proof of work, not just plans.", p);
        List<StudySession> sessions = repo.getSessions();
        if (sessions.isEmpty()) {
            box.addView(emptyMessage("Complete your first focus block and it will appear here.", p));
        } else {
            for (int i = 0; i < Math.min(7, sessions.size()); i++) box.addView(sessionCard(sessions.get(i)), marginBottom(7));
        }
        return scroll;
    }

    private View sessionCard(StudySession s){ThemeManager.Palette p=ThemeManager.palette(this);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(13),dp(10),dp(13),dp(10));row.setBackground(ThemeManager.rounded(p.surface,dp(17)));Exam e=repo.getExam(s.examId);Topic t=repo.getTopic(s.topicId);LinearLayout info=column();info.addView(text((e==null?"General":e.subject)+(t==null?"":" · "+t.name),13,p.text,true));info.addView(text(s.mode+" · "+new SimpleDateFormat("d MMM, h:mm a",Locale.getDefault()).format(new Date(s.startedAt)),10.5f,p.muted,false));row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));row.addView(text(s.minutes+"m",18,p.primary,true));return row;}
    private void startFocus(){focusRunning=true;if(focusTimer!=null)focusTimer.cancel();focusTimer=new CountDownTimer(focusRemainingMs,1000){@Override public void onTick(long m){focusRemainingMs=m;if(focusTimeText!=null)focusTimeText.setText(formatFocus(m));}@Override public void onFinish(){focusRunning=false;focusRemainingMs=0;repo.recordFocus((int)(focusPresetMs/60_000L),focusExamId,focusTopicId,focusMode);Toast.makeText(MainActivity.this,"Focus mission complete ⚡ +XP",Toast.LENGTH_LONG).show();focusRemainingMs=focusPresetMs;if(currentTab==2)renderCurrentPage();}}.start();}
    private void pauseFocus(){focusRunning=false;if(focusTimer!=null)focusTimer.cancel();}
    private void finishFocusEarly(){long elapsed=focusPresetMs-focusRemainingMs;int mins=(int)(elapsed/60_000L);if(mins<1){Toast.makeText(this,"Study for at least one minute before logging",Toast.LENGTH_SHORT).show();return;}pauseFocus();repo.recordFocus(mins,focusExamId,focusTopicId,focusMode+" · partial");focusRemainingMs=focusPresetMs;Toast.makeText(this,mins+" focused minutes logged",Toast.LENGTH_SHORT).show();renderCurrentPage();}

    // ---------------- Life OS: screen time + health + study intelligence ----------------
    private View buildLifePage() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout box = column();
        box.setPadding(0, dp(7), 0, dp(26));
        scroll.addView(box);

        TextView kicker = text("BALANCE OS", 10, p.primary, true);
        kicker.setLetterSpacing(.14f);
        box.addView(kicker);
        box.addView(text("Study load meets real life.", 24, p.text, true), top(2));
        box.addView(text("Screen time and recovery context stay permission-based. ExamVerse works fully even when you keep them disconnected.", 11.7f, p.muted, false), top(3));

        boolean hasUsage = ScreenTimeManager.hasUsageAccess(this);
        ScreenTimeManager.Snapshot st = hasUsage ? ScreenTimeManager.today(this) : null;

        LinearLayout balanceHero = column();
        balanceHero.setPadding(dp(18), dp(17), dp(18), dp(17));
        balanceHero.setBackground(ThemeManager.hero(this, dp(28)));
        balanceHero.setElevation(dp(4));

        LinearLayout bh = new LinearLayout(this);
        bh.setOrientation(LinearLayout.HORIZONTAL);
        bh.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout bht = column();
        bht.addView(text("TODAY'S BALANCE", 10, Color.WHITE, true));
        bht.addView(text(repo.focusMinutesToday() + " focused minutes", 25, Color.WHITE, true), top(4));
        String balanceSub;
        if (hasUsage && st != null) {
            balanceSub = ScreenTimeManager.formatDuration(st.screenInteractiveMillis) + " screen time  ·  " + st.unlocks + " unlocks";
        } else {
            balanceSub = "Connect screen-time access for device context";
        }
        TextView bs = text(balanceSub, 10.8f, Color.WHITE, false);
        bs.setAlpha(.82f);
        bht.addView(bs, top(3));
        bh.addView(bht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView streak = text(repo.getStreak() + "d\nstreak", 11.5f, Color.WHITE, true);
        streak.setGravity(Gravity.CENTER);
        streak.setPadding(dp(11), dp(8), dp(11), dp(8));
        streak.setBackground(ThemeManager.outlined(alpha(Color.BLACK, 24), alpha(Color.WHITE, 62), dp(17), dp(1)));
        bh.addView(streak);
        balanceHero.addView(bh);
        box.addView(balanceHero, top(12));

        sectionTitle(box, "Digital wellbeing", "Android Usage Access powers this view locally on your phone.", p);
        if (!hasUsage) {
            LinearLayout permission = card();
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView lock = text("◌", 25, p.primary, true);
            titleRow.addView(lock, new LinearLayout.LayoutParams(dp(38), dp(38)));
            LinearLayout pt = column();
            pt.addView(text("Screen-time access is off", 15, p.text, true));
            pt.addView(text("Optional · can be removed anytime in Android settings", 10.2f, p.muted, false), top(2));
            titleRow.addView(pt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            permission.addView(titleRow);
            permission.addView(text("Turn it on to see today's screen time, unlock count and top-used apps next to your study data.", 11.5f, p.muted, false), top(9));
            Button grant = actionButton("Open Usage Access settings", p.primary, contrastText(p.primary));
            grant.setOnClickListener(v -> ScreenTimeManager.openUsageAccessSettings(this));
            permission.addView(grant, topHeight(12, 48));
            box.addView(permission);
        } else if (st != null) {
            LinearLayout stats = new LinearLayout(this);
            stats.setOrientation(LinearLayout.HORIZONTAL);
            stats.addView(statTile("SCREEN", ScreenTimeManager.formatDuration(st.screenInteractiveMillis), "today", p), weightMargin(1f, 0, 5));
            stats.addView(statTile("UNLOCKS", String.valueOf(st.unlocks), "today", p), weightMargin(1f, 0, 5));
            stats.addView(statTile("FOCUS", repo.focusMinutesToday() + "m", "ExamVerse", p), weightMargin(1f, 0, 0));
            box.addView(stats);

            if (!st.topApps.isEmpty()) {
                LinearLayout apps = card();
                LinearLayout ah = new LinearLayout(this);
                ah.setOrientation(LinearLayout.HORIZONTAL);
                ah.setGravity(Gravity.CENTER_VERTICAL);
                ah.addView(text("Top apps today", 14.5f, p.text, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                ah.addView(text("local data", 9.5f, p.muted, true));
                apps.addView(ah);

                long maxUsage = Math.max(1, st.topApps.get(0).millis);
                int max = Math.min(5, st.topApps.size());
                for (int i = 0; i < max; i++) {
                    ScreenTimeManager.AppUsage u = st.topApps.get(i);
                    LinearLayout appRow = column();
                    LinearLayout textRow = new LinearLayout(this);
                    textRow.setOrientation(LinearLayout.HORIZONTAL);
                    textRow.setGravity(Gravity.CENTER_VERTICAL);
                    textRow.addView(text((i + 1) + "  " + u.label, 11.8f, p.text, i == 0), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    textRow.addView(text(ScreenTimeManager.formatDuration(u.millis), 10.8f, p.primary, true));
                    appRow.addView(textRow);
                    appRow.addView(progress(Math.round(u.millis * 100f / maxUsage), p.primary), topHeight(5, 5));
                    apps.addView(appRow, top(9));
                }
                box.addView(apps, top(10));
            }
        }

        sectionTitle(box, "Health & recovery", "Optional Health Connect context — you choose each health category.", p);
        LinearLayout health = card();
        boolean connected = HealthSnapshotStore.isConnected(this);

        if (connected) {
            long sm = HealthSnapshotStore.sleepMinutes(this);
            LinearLayout hs = new LinearLayout(this);
            hs.setOrientation(LinearLayout.HORIZONTAL);
            hs.addView(statTile("STEPS", String.valueOf(HealthSnapshotStore.steps(this)), "today", p), weightMargin(1f, 0, 5));
            hs.addView(statTile("SLEEP", (sm / 60) + "h " + (sm % 60) + "m", "recent", p), weightMargin(1f, 0, 5));
            hs.addView(statTile("HEART", Math.round(HealthSnapshotStore.heartBpm(this)) + " bpm", "average", p), weightMargin(1f, 0, 0));
            health.addView(hs);
            health.addView(text(Math.round(HealthSnapshotStore.calories(this)) + " kcal burned  ·  " + HealthSnapshotStore.exerciseMinutes(this) + " min exercise", 10.8f, p.muted, false), top(9));
            long updated = HealthSnapshotStore.updatedAt(this);
            if (updated > 0) health.addView(text("Last health sync  " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(updated)), 9.8f, p.muted, false), top(3));
        } else {
            health.addView(text("Health Connect is not connected", 14.5f, p.text, true));
            health.addView(text("Steps, sleep, heart rate, calories and exercise can appear here after you explicitly grant access.", 11.2f, p.muted, false), top(4));
        }

        Button sync = actionButton(connected ? "Manage / sync Health Connect" : "Connect Health Connect", p.surfaceAlt, p.text);
        sync.setOnClickListener(v -> startActivity(new Intent(this, HealthHubActivity.class)));
        health.addView(sync, topHeight(11, 48));
        box.addView(health);

        sectionTitle(box, "Readiness pulse", "A compact look at how your current study effort is translating into exam readiness.", p);
        LinearLayout pulse = card();
        LinearLayout pulseTop = new LinearLayout(this);
        pulseTop.setOrientation(LinearLayout.HORIZONTAL);
        pulseTop.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout pulseText = column();
        pulseText.addView(text(repo.averageReadiness() + "% average readiness", 18, p.text, true));
        pulseText.addView(text(repo.examsNeedingAttention() == 0 ? "No urgent readiness gaps detected" : repo.examsNeedingAttention() + " exam" + (repo.examsNeedingAttention() == 1 ? "" : "s") + " need attention soon", 10.5f, p.muted, false), top(2));
        pulseTop.addView(pulseText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView level = text("LV " + repo.getLevel(), 12, p.primary, true);
        level.setPadding(dp(10), dp(7), dp(10), dp(7));
        level.setBackground(ThemeManager.glowChip(this, dp(15)));
        pulseTop.addView(level);
        pulse.addView(pulseTop);
        pulse.addView(progress(repo.averageReadiness(), repo.averageReadiness() >= 70 ? p.success : p.primary), topHeight(10, 8));
        pulse.addView(text(repo.getSessions().size() + " sessions  ·  " + repo.getFocusMinutes() + " total focus min  ·  " + repo.activeDaysLast28() + "/28 active days", 10.5f, p.muted, false), top(7));
        box.addView(pulse);

        TextView privacy = text("ExamVerse never needs screen-time or Health Connect access to run the planner. Those permissions only enrich this Balance OS screen.", 9.8f, p.muted, false);
        box.addView(privacy, top(14));

        return scroll;
    }

    // ---------------- Insights ----------------

    // ---------------- Insights ----------------
    private View buildInsightsPage() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout box = column();
        box.setPadding(0, dp(7), 0, dp(26));
        scroll.addView(box);

        TextView kicker = text("STUDY INTELLIGENCE", 10, p.primary, true);
        kicker.setLetterSpacing(.14f);
        box.addView(kicker);
        box.addView(text("See what your effort is actually doing.", 24, p.text, true), top(2));
        box.addView(text("Trends, consistency, readiness and pressure — without spreadsheet energy.", 11.7f, p.muted, false), top(3));

        int xp = repo.getXp();
        int lvl = repo.getLevel();
        LinearLayout rank = card();
        rank.setPadding(dp(18), dp(17), dp(18), dp(17));
        rank.setBackground(ThemeManager.hero(this, dp(26)));
        rank.addView(text("LEVEL " + lvl + "  ·  " + ThemeManager.rankName(this, lvl), 10.5f, Color.WHITE, true));
        rank.addView(text(xp + " XP", 29, Color.WHITE, true), top(4));
        TextView next = text((500 - repo.getXpIntoLevel()) + " XP to next level  ·  " + repo.activeDaysLast28() + "/28 active days", 10.5f, Color.WHITE, false);
        next.setAlpha(.82f);
        rank.addView(next, top(2));
        ProgressBar xpbar = progress(Math.round(repo.getXpIntoLevel() * 100f / 500f), Color.WHITE);
        rank.addView(xpbar, topHeight(9, 7));
        box.addView(rank, top(12));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statTile("TOTAL", repo.getFocusMinutes() + "m", repo.getSessions().size() + " sessions", p), weightMargin(1f, 10, 5));
        stats.addView(statTile("7D AVG", repo.averageFocusLast7Days() + "m", "per day", p), weightMargin(1f, 10, 5));
        stats.addView(statTile("ATTENTION", String.valueOf(repo.examsNeedingAttention()), "exam risk", p), weightMargin(1f, 10, 0));
        box.addView(stats);

        sectionTitle(box, "Focus trend", "Seven days of real focused minutes with your average line.", p);
        LinearLayout trendCard = card();
        FocusTrendView trend = new FocusTrendView(this).setData(repo.last7DaysMinutes());
        trendCard.addView(trend, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(195)));
        box.addView(trendCard);

        sectionTitle(box, "Consistency dots", "Twenty-eight days at a glance. Bigger dots mean more focused minutes.", p);
        LinearLayout dotsCard = card();
        StudyDotHeatmapView dots = new StudyDotHeatmapView(this).setData(repo.last28DaysMinutes());
        dotsCard.addView(dots, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(165)));
        box.addView(dotsCard);

        List<Exam> exams = repo.getUpcomingExams();
        sectionTitle(box, "Pressure vs readiness", "This is the dot graph: deadline on X, readiness on Y, priority in dot size.", p);
        LinearLayout readyMap = card();
        ReadinessDotGraphView graph = new ReadinessDotGraphView(this).setExams(exams);
        readyMap.addView(graph, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230)));
        box.addView(readyMap);

        sectionTitle(box, "Exam readiness", "Completion + confidence + practice time for every active exam.", p);
        if (exams.isEmpty()) {
            box.addView(emptyMessage("Add exams and syllabus topics to unlock readiness intelligence.", p));
        } else {
            for (Exam e : exams) {
                int ready = repo.readinessForExam(e.id);
                LinearLayout card = card();

                LinearLayout head = new LinearLayout(this);
                head.setOrientation(LinearLayout.HORIZONTAL);
                head.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout names = column();
                names.addView(text(e.subject, 14.5f, p.text, true));
                names.addView(text(e.title + "  ·  " + formatRemaining(e.timeMillis, false) + " left", 10.3f, p.muted, false), top(2));
                head.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                int rc = ready >= 70 ? p.success : (ready >= 40 ? p.warning : p.danger);
                TextView badge = text(ready + "%", 14.5f, rc, true);
                badge.setPadding(dp(9), dp(5), dp(9), dp(5));
                badge.setBackground(ThemeManager.outlined(alpha(rc, 22), alpha(rc, 80), dp(14), dp(1)));
                head.addView(badge);
                card.addView(head);
                card.addView(progress(ready, rc), topHeight(9, 8));
                box.addView(card, marginBottom(8));
            }
        }

        Map<String,Integer> bySubject = repo.focusMinutesBySubject();
        if (!bySubject.isEmpty()) {
            sectionTitle(box, "Study balance", "Your focus-time distribution by subject.", p);
            int maxMinutes = 1;
            for (int v : bySubject.values()) maxMinutes = Math.max(maxMinutes, v);

            for (Map.Entry<String,Integer> entry : bySubject.entrySet()) {
                LinearLayout row = card();
                LinearLayout rr = new LinearLayout(this);
                rr.setOrientation(LinearLayout.HORIZONTAL);
                rr.addView(text(entry.getKey(), 13, p.text, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                rr.addView(text(entry.getValue() + "m", 13, p.primary, true));
                row.addView(rr);
                row.addView(progress(Math.round(entry.getValue() * 100f / maxMinutes), p.primary), topHeight(7, 6));
                box.addView(row, marginBottom(7));
            }
        }

        return scroll;
    }

    // ---------------- Themes + settings ----------------

    // ---------------- Themes + settings ----------------
    private View buildMorePage() {
        ThemeManager.Palette p = ThemeManager.palette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout box = column();
        box.setPadding(0, dp(7), 0, dp(28));
        scroll.addView(box);

        TextView kicker = text("STUDIO", 10, p.primary, true);
        kicker.setLetterSpacing(.15f);
        box.addView(kicker);
        box.addView(text("Make ExamVerse feel like yours.", 24, p.text, true), top(2));
        box.addView(text("Themes, alarms, widgets and training preferences — all in one place.", 11.7f, p.muted, false), top(3));

        sectionTitle(box, "Quick tools", "The stuff you actually need, without hunting through settings.", p);
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);

        Button alarmsBtn = actionButton("⏰  Alarms", p.surfaceAlt, p.text);
        alarmsBtn.setOnClickListener(v -> startActivity(new Intent(this, AlarmHubActivity.class)));
        Button widgetBtn = actionButton("▦  Widgets", p.surfaceAlt, p.text);
        widgetBtn.setOnClickListener(v -> startActivity(new Intent(this, WidgetStudioActivity.class)));
        tools.addView(alarmsBtn, weightMargin(1f, 0, 5));
        tools.addView(widgetBtn, weightMargin(1f, 0, 0));
        box.addView(tools);

        LinearLayout tools2 = new LinearLayout(this);
        tools2.setOrientation(LinearLayout.HORIZONTAL);
        Button lifeBtn = actionButton("◉  Wellness", p.surfaceAlt, p.text);
        lifeBtn.setOnClickListener(v -> { currentTab = 3; renderShell(); });
        Button focusBtn = actionButton("⚡  Focus now", p.primary, contrastText(p.primary));
        focusBtn.setOnClickListener(v -> { currentTab = 2; renderShell(); });
        tools2.addView(lifeBtn, weightMargin(1f, 8, 5));
        tools2.addView(focusBtn, weightMargin(1f, 8, 0));
        box.addView(tools2);

        sectionTitle(box, "Anime battle modes", "Each mode now changes the full atmosphere: color system, glass, motion, rank language and widgets.", p);
        box.addView(themeCard("naruto", "Shinobi Ember", "Warm parchment, ember chakra and ink-seal motion", "Academy → Genin → Chunin → Jonin → Hokage",
                Color.rgb(249,115,22), Color.rgb(127,29,29)), top(12));
        box.addView(themeCard("dragonball", "Saiyan Energy", "Deep cosmic blue, gold energy and training-beam motion", "Trainee → Saiyan → Elite → Super Saiyan → Ultra Instinct",
                Color.rgb(255,184,28), Color.rgb(37,99,235)), top(10));
        box.addView(themeCard("bleach", "Soul Reaper Noir", "Ink black, crimson slash geometry and blade-light accents", "Soul Reaper → Seated Officer → Shikai → Bankai → Captain",
                Color.rgb(244,63,94), Color.rgb(24,24,27)), top(10));

        sectionTitle(box, "Daily training goal", "Mission Control and Focus use this to pace the day.", p);
        LinearLayout goal = card();
        LinearLayout gr = new LinearLayout(this);
        gr.setOrientation(LinearLayout.HORIZONTAL);
        gr.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = actionButton("−", p.surfaceAlt, p.text);
        TextView g = text(repo.getDailyGoal() + " min / day", 20, p.text, true);
        g.setGravity(Gravity.CENTER);
        Button plus = actionButton("＋", p.surfaceAlt, p.text);
        minus.setOnClickListener(v -> { repo.setDailyGoal(repo.getDailyGoal() - 15); renderCurrentPage(); });
        plus.setOnClickListener(v -> { repo.setDailyGoal(repo.getDailyGoal() + 15); renderCurrentPage(); });
        gr.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(44)));
        gr.addView(g, new LinearLayout.LayoutParams(0, dp(44), 1f));
        gr.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(44)));
        goal.addView(gr);
        box.addView(goal);

        sectionTitle(box, "Privacy & performance", "Useful permissions stay optional and the core planner remains offline-first.", p);
        LinearLayout privacy = card();
        privacy.addView(text("✓ Offline exams, plans, topics and sessions\n✓ No account required\n✓ No ads or analytics SDK\n✓ Health access is opt-in\n✓ Usage access is opt-in\n✓ Lightweight custom charts — no heavy chart library\n✓ Widgets can use independent glass styles",
                12.2f, p.text, false));
        box.addView(privacy);

        TextView note = text("Fan-style theme names are for this private build. No copied character art, franchise screenshots or logos are bundled.", 10.3f, p.muted, false);
        box.addView(note, top(14));
        return scroll;
    }

    private View themeCard(String key, String name, String subtitle, String ranks, int c1, int c2) {
        ThemeManager.Palette p = ThemeManager.palette(this);
        boolean selected = repo.getTheme().equals(key);

        LinearLayout card = column();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(ThemeManager.gradient(c1, c2, dp(25)));
        card.setElevation(selected ? dp(7) : dp(3));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout names = column();
        names.addView(text(name, 20.5f, Color.WHITE, true));
        TextView sub = text(subtitle, 10.8f, Color.WHITE, false);
        sub.setAlpha(.86f);
        names.addView(sub, top(3));
        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView status = text(selected ? "ACTIVE" : "APPLY", 9.2f, Color.WHITE, true);
        status.setLetterSpacing(.08f);
        status.setPadding(dp(10), dp(6), dp(10), dp(6));
        status.setBackground(ThemeManager.outlined(alpha(Color.BLACK, 28), alpha(Color.WHITE, 76), dp(14), dp(1)));
        top.addView(status);
        card.addView(top);

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setGravity(Gravity.CENTER_VERTICAL);
        int[] colors = {c1, c2, Color.WHITE, alpha(Color.WHITE, 120)};
        for (int color : colors) {
            View dot = new View(this);
            dot.setBackground(ThemeManager.rounded(color, dp(7)));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(14), dp(14));
            dlp.rightMargin = dp(6);
            swatches.addView(dot, dlp);
        }
        TextView r = text(ranks, 9.6f, Color.WHITE, true);
        r.setAlpha(.78f);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rp.leftMargin = dp(4);
        swatches.addView(r, rp);
        card.addView(swatches, top(11));

        card.setOnClickListener(v -> {
            repo.setTheme(key);
            WidgetUpdater.updateAll(this);
            renderShell();
        });
        return card;
    }

    // ---------------- UI helpers ----------------

    // ---------------- UI helpers ----------------
    private void sectionTitle(LinearLayout box,String title,String subtitle,ThemeManager.Palette p){TextView h=text(title,17.5f,p.text,true);LinearLayout.LayoutParams hp=top(20);box.addView(h,hp);box.addView(text(subtitle,10.8f,p.muted,false),top(2));}
    private LinearLayout emptyMessage(String msg,ThemeManager.Palette p){LinearLayout l=card();l.addView(text(msg,12.5f,p.muted,false));return l;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(){ThemeManager.Palette p=ThemeManager.palette(this);LinearLayout l=column();l.setPadding(dp(16),dp(15),dp(16),dp(15));l.setBackground(ThemeManager.glass(this,dp(22),false));l.setElevation(dp(3));return l;}
    private Button actionButton(String label,int bg,int fg){Button b=new Button(this);b.setText(label);b.setTextSize(12.2f);b.setTextColor(fg);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setBackground(ThemeManager.outlined(bg,alpha(Color.WHITE,38),dp(17),dp(1)));b.setPadding(dp(9),0,dp(9),0);b.setElevation(dp(1));b.setStateListAnimator(null);return b;}
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
