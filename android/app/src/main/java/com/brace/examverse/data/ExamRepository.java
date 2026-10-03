package com.brace.examverse.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExamRepository {
    private static final String PREFS = "examverse_data";
    private static final String KEY_EXAMS = "exams";
    private static final String KEY_TOPICS = "topics";
    private static final String KEY_SESSIONS = "sessions_v2";
    private static final String KEY_TASKS = "tasks_v2";
    private static final String KEY_THEME = "theme";
    private static final String KEY_FOCUS = "focus_minutes";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_STUDY = "last_study";
    private static final String KEY_XP = "xp_v2";
    private static final String KEY_DAILY_GOAL = "daily_goal_v2";

    private final SharedPreferences prefs;

    public ExamRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Exam> getExams() {
        List<Exam> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_EXAMS, "[]"));
            for (int i = 0; i < a.length(); i++) list.add(Exam.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        Collections.sort(list, Comparator.comparingLong(e -> e.timeMillis));
        return list;
    }

    public Exam getExam(long id) {
        for (Exam e : getExams()) if (e.id == id) return e;
        return null;
    }

    public List<Exam> getUpcomingExams() {
        long now = System.currentTimeMillis();
        List<Exam> result = new ArrayList<>();
        for (Exam e : getExams()) if (!e.completed && e.timeMillis >= now - 60_000) result.add(e);
        return result;
    }

    public Exam getNextExam() {
        List<Exam> e = getUpcomingExams();
        return e.isEmpty() ? null : e.get(0);
    }

    public void saveExam(Exam exam) {
        List<Exam> list = getExams();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == exam.id) { list.set(i, exam); found = true; break; }
        }
        if (!found) list.add(exam);
        writeExams(list);
    }

    public void deleteExam(long id) {
        List<Exam> exams = getExams();
        exams.removeIf(e -> e.id == id);
        writeExams(exams);
        List<Topic> topics = getTopics();
        topics.removeIf(t -> t.examId == id);
        writeTopics(topics);
        List<StudyTask> tasks = getTasks();
        tasks.removeIf(t -> t.examId == id);
        writeTasks(tasks);
    }

    private void writeExams(List<Exam> list) {
        JSONArray a = new JSONArray();
        try { for (Exam e : list) a.put(e.toJson()); } catch (Exception ignored) {}
        prefs.edit().putString(KEY_EXAMS, a.toString()).apply();
    }

    public List<Topic> getTopics() {
        List<Topic> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_TOPICS, "[]"));
            for (int i = 0; i < a.length(); i++) list.add(Topic.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    public List<Topic> getTopicsForExam(long examId) {
        List<Topic> result = new ArrayList<>();
        for (Topic t : getTopics()) if (t.examId == examId) result.add(t);
        return result;
    }

    public Topic getTopic(long id) {
        for (Topic t : getTopics()) if (t.id == id) return t;
        return null;
    }

    public void saveTopic(Topic topic) {
        List<Topic> list = getTopics();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == topic.id) { list.set(i, topic); found = true; break; }
        }
        if (!found) list.add(topic);
        writeTopics(list);
    }

    public void deleteTopic(long id) {
        List<Topic> list = getTopics();
        list.removeIf(t -> t.id == id);
        writeTopics(list);
        List<StudyTask> tasks = getTasks();
        tasks.removeIf(t -> t.topicId == id);
        writeTasks(tasks);
    }

    private void writeTopics(List<Topic> list) {
        JSONArray a = new JSONArray();
        try { for (Topic t : list) a.put(t.toJson()); } catch (Exception ignored) {}
        prefs.edit().putString(KEY_TOPICS, a.toString()).apply();
    }

    public int completedTopicCount() {
        int n = 0; for (Topic t : getTopics()) if (t.done) n++; return n;
    }

    public int readinessForExam(long examId) {
        List<Topic> topics = getTopicsForExam(examId);
        if (topics.isEmpty()) return 0;
        float completion = 0f;
        float confidence = 0f;
        float weightedDifficulty = 0f;
        for (Topic t : topics) {
            completion += t.done ? 1f : 0f;
            confidence += t.confidence / 5f;
            weightedDifficulty += t.done ? 1f : Math.max(.25f, 1f - (t.difficulty - 1) * .12f);
        }
        float c1 = completion / topics.size();
        float c2 = confidence / topics.size();
        float c3 = weightedDifficulty / topics.size();
        int sessionMinutes = focusMinutesForExam(examId);
        float practice = Math.min(1f, sessionMinutes / Math.max(90f, topics.size() * 35f));
        return Math.max(0, Math.min(100, Math.round((c1 * .45f + c2 * .25f + c3 * .10f + practice * .20f) * 100f)));
    }

    public List<StudySession> getSessions() {
        List<StudySession> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_SESSIONS, "[]"));
            for (int i = 0; i < a.length(); i++) list.add(StudySession.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        Collections.sort(list, (a, b) -> Long.compare(b.startedAt, a.startedAt));
        return list;
    }

    private void writeSessions(List<StudySession> list) {
        JSONArray a = new JSONArray();
        try { for (StudySession s : list) a.put(s.toJson()); } catch (Exception ignored) {}
        prefs.edit().putString(KEY_SESSIONS, a.toString()).apply();
    }

    public void recordFocus(int minutes) { recordFocus(minutes, -1, -1, "Focus"); }

    public void recordFocus(int minutes, long examId, long topicId, String mode) {
        int safeMinutes = Math.max(1, minutes);
        List<StudySession> sessions = getSessions();
        sessions.add(new StudySession(System.currentTimeMillis(), examId, topicId, System.currentTimeMillis(), safeMinutes, mode));
        writeSessions(sessions);

        String today = dayKey(System.currentTimeMillis());
        String last = prefs.getString(KEY_LAST_STUDY, "");
        int streak = prefs.getInt(KEY_STREAK, 0);
        if (!today.equals(last)) {
            long gap = dayDistance(last, today);
            streak = gap == 1 ? Math.max(1, streak + 1) : 1;
        }
        int gainedXp = Math.max(10, safeMinutes * 2);
        prefs.edit()
                .putInt(KEY_FOCUS, prefs.getInt(KEY_FOCUS, 0) + safeMinutes)
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_STUDY, today)
                .putInt(KEY_XP, prefs.getInt(KEY_XP, 0) + gainedXp)
                .apply();
        if (topicId > 0) {
            Topic t = getTopic(topicId);
            if (t != null) { t.lastReviewedAt = System.currentTimeMillis(); t.confidence = Math.min(5, t.confidence + 1); saveTopic(t); }
        }
    }

    public int getFocusMinutes() { return prefs.getInt(KEY_FOCUS, 0); }
    public int getStreak() { return prefs.getInt(KEY_STREAK, 0); }
    public int getXp() { return prefs.getInt(KEY_XP, 0); }
    public int getLevel() { return 1 + getXp() / 500; }
    public int getXpIntoLevel() { return getXp() % 500; }

    public int getDailyGoal() { return prefs.getInt(KEY_DAILY_GOAL, 60); }
    public void setDailyGoal(int minutes) { prefs.edit().putInt(KEY_DAILY_GOAL, Math.max(15, Math.min(360, minutes))).apply(); }

    public int focusMinutesToday() { return focusMinutesForDay(System.currentTimeMillis()); }

    public int focusMinutesForDay(long dayMillis) {
        String key = dayKey(dayMillis); int sum = 0;
        for (StudySession s : getSessions()) if (dayKey(s.startedAt).equals(key)) sum += s.minutes;
        return sum;
    }

    public int focusMinutesForExam(long examId) {
        int sum = 0; for (StudySession s : getSessions()) if (s.examId == examId) sum += s.minutes; return sum;
    }

    public int[] last7DaysMinutes() {
        return focusMinutesForRecentDays(7);
    }

    public int[] last28DaysMinutes() {
        return focusMinutesForRecentDays(28);
    }

    private int[] focusMinutesForRecentDays(int days) {
        int[] out = new int[Math.max(1, days)];
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_MONTH, -(out.length - 1));
        for (int i = 0; i < out.length; i++) {
            out[i] = focusMinutesForDay(c.getTimeInMillis());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return out;
    }

    public int averageFocusLast7Days() {
        int[] values = last7DaysMinutes();
        int total = 0;
        for (int value : values) total += value;
        return Math.round(total / 7f);
    }

    public int activeDaysLast28() {
        int active = 0;
        for (int value : last28DaysMinutes()) if (value > 0) active++;
        return active;
    }

    public int averageReadiness() {
        List<Exam> exams = getUpcomingExams();
        if (exams.isEmpty()) return 0;
        int total = 0;
        for (Exam e : exams) total += readinessForExam(e.id);
        return Math.round(total / (float) exams.size());
    }

    public int examsNeedingAttention() {
        int count = 0;
        long now = System.currentTimeMillis();
        for (Exam e : getUpcomingExams()) {
            float days = Math.max(0f, (e.timeMillis - now) / 86_400_000f);
            int readiness = readinessForExam(e.id);
            if ((days <= 7 && readiness < 70) || (days <= 3 && readiness < 85)) count++;
        }
        return count;
    }

    public Map<String, Integer> focusMinutesBySubject() {
        Map<String, Integer> map = new HashMap<>();
        for (StudySession s : getSessions()) {
            Exam e = getExam(s.examId);
            String subject = e == null ? "General" : e.subject;
            map.put(subject, map.getOrDefault(subject, 0) + s.minutes);
        }
        return map;
    }

    public List<StudyTask> getTasks() {
        List<StudyTask> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_TASKS, "[]"));
            for (int i = 0; i < a.length(); i++) list.add(StudyTask.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        Collections.sort(list, (a, b) -> {
            if (a.done != b.done) return a.done ? 1 : -1;
            if (a.dueAt != b.dueAt) return Long.compare(a.dueAt, b.dueAt);
            return Integer.compare(b.priority, a.priority);
        });
        return list;
    }

    public void saveTask(StudyTask task) {
        List<StudyTask> list = getTasks(); boolean found = false;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id == task.id) { list.set(i, task); found = true; break; }
        if (!found) list.add(task); writeTasks(list);
    }

    public void deleteTask(long id) { List<StudyTask> list = getTasks(); list.removeIf(t -> t.id == id); writeTasks(list); }

    public void toggleTask(long id) {
        List<StudyTask> list = getTasks();
        for (StudyTask t : list) if (t.id == id) {
            boolean wasDone = t.done; t.done = !t.done;
            if (!wasDone && t.done) prefs.edit().putInt(KEY_XP, getXp() + 25).apply();
            break;
        }
        writeTasks(list);
    }

    private void writeTasks(List<StudyTask> list) {
        JSONArray a = new JSONArray();
        try { for (StudyTask t : list) a.put(t.toJson()); } catch (Exception ignored) {}
        prefs.edit().putString(KEY_TASKS, a.toString()).apply();
    }

    public List<StudyTask> getTodayTasks() {
        String today = dayKey(System.currentTimeMillis()); List<StudyTask> out = new ArrayList<>();
        for (StudyTask t : getTasks()) if (!t.done && dayKey(t.dueAt).equals(today)) out.add(t);
        return out;
    }

    public int pendingTaskCount() { int n = 0; for (StudyTask t : getTasks()) if (!t.done) n++; return n; }

    public int generateSmartPlan() {
        List<StudyTask> existing = getTasks();
        existing.removeIf(t -> t.autoGenerated && !t.done);
        long now = System.currentTimeMillis(); long seed = now;
        int created = 0;
        for (Exam exam : getUpcomingExams()) {
            List<Topic> topics = getTopicsForExam(exam.id);
            if (topics.isEmpty()) continue;
            long days = Math.max(1, (exam.timeMillis - now) / 86_400_000L);
            int slot = 0;
            for (Topic topic : topics) {
                if (topic.done) continue;
                int repetitions = topic.difficulty >= 4 ? 2 : 1;
                for (int r = 0; r < repetitions; r++) {
                    long dayOffset = Math.min(Math.max(0, days - 1), slot % Math.max(1, Math.min(days, 14)));
                    Calendar due = Calendar.getInstance();
                    due.setTimeInMillis(now); due.add(Calendar.DAY_OF_MONTH, (int)dayOffset);
                    due.set(Calendar.HOUR_OF_DAY, 18); due.set(Calendar.MINUTE, 0); due.set(Calendar.SECOND, 0); due.set(Calendar.MILLISECOND, 0);
                    if (due.getTimeInMillis() >= exam.timeMillis) due.setTimeInMillis(Math.max(now + 60_000L, exam.timeMillis - 6 * 60 * 60_000L));
                    int mins = Math.max(15, Math.min(90, topic.estimatedMinutes + (topic.difficulty - 3) * 10));
                    int priority = Math.max(exam.priority, topic.difficulty);
                    String prefix = r == 0 ? "Learn" : "Revise";
                    existing.add(new StudyTask(++seed, exam.id, topic.id, prefix + " · " + topic.name, due.getTimeInMillis(), mins, priority, false, true));
                    created++; slot++;
                    if (created >= 60) break;
                }
                if (created >= 60) break;
            }
            if (created >= 60) break;
        }
        writeTasks(existing);
        return created;
    }

    public String getTheme() { return prefs.getString(KEY_THEME, "naruto"); }
    public void setTheme(String theme) { prefs.edit().putString(KEY_THEME, theme).apply(); }

    private static String dayKey(long millis) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis)); }

    private static long dayDistance(String from, String to) {
        try {
            Date a = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(from);
            Date b = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(to);
            if (a == null || b == null) return 999;
            return (b.getTime() - a.getTime()) / 86_400_000L;
        } catch (Exception e) { return 999; }
    }
}
