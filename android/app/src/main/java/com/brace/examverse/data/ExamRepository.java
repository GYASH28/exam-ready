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

    public void recordFocus(int minutes, long examId, long topicId, String mode) { recordFocusOnce(System.currentTimeMillis(),minutes,examId,topicId,mode); }

    public synchronized void recordFocusOnce(long id, int minutes, long examId, long topicId, String mode) {
        for(StudySession session:getSessions())if(session.id==id)return;
        int safeMinutes = Math.max(1, minutes);
        List<StudySession> sessions = getSessions();
        sessions.add(new StudySession(id, examId, topicId, System.currentTimeMillis()-safeMinutes*60_000L, safeMinutes, mode));
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
    public int getStreak() { long gap=dayDistance(prefs.getString(KEY_LAST_STUDY,""),dayKey(System.currentTimeMillis()));return gap>=0&&gap<=1?prefs.getInt(KEY_STREAK,0):0; }
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
            if (!wasDone && t.done && !t.rewarded) {t.rewarded=true;prefs.edit().putInt(KEY_XP, getXp() + 25).apply();}
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
        for (StudyTask t : getTasks()) if (!t.done && dayKey(t.dueAt).compareTo(today)<=0) out.add(t);
        return out;
    }

    public int pendingTaskCount() { int n = 0; for (StudyTask t : getTasks()) if (!t.done) n++; return n; }

    public int generateSmartPlan() {
        List<StudyTask> existing=getTasks();existing.removeIf(t->t.autoGenerated&&!t.done);
        long now=System.currentTimeMillis(),seed=now;int created=0;
        Map<String,Integer> load=new HashMap<>();
        for(StudyTask task:existing)if(!task.done)load.put(dayKey(task.dueAt),load.getOrDefault(dayKey(task.dueAt),0)+task.minutes);
        for(Exam exam:getUpcomingExams()) {
            if(exam.timeMillis<=now+60_000L)continue;
            List<Topic> topics=getTopicsForExam(exam.id);
            topics.sort((a,b)->Integer.compare((b.difficulty*2+6-b.confidence),(a.difficulty*2+6-a.confidence)));
            for(Topic topic:topics){if(topic.done)continue;int reps=topic.difficulty>=4||topic.confidence<=2?2:1;long firstDay=now;
                for(int repetition=0;repetition<reps;repetition++){
                    int minutes=Math.max(10,Math.min(90,topic.estimatedMinutes+(topic.difficulty-3)*10));
                    Calendar day=Calendar.getInstance();day.setTimeInMillis(now);if(repetition>0)day.setTimeInMillis(firstDay+2*86_400_000L);
                    day.set(Calendar.HOUR_OF_DAY,18);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
                    long chosen=Math.min(exam.timeMillis-60_000L,Math.max(now+60_000L,day.getTimeInMillis()));
                    int minLoad=Integer.MAX_VALUE;
                    for(int offset=0;offset<14;offset++){
                        long candidate=Math.max(now+60_000L,day.getTimeInMillis());if(candidate>=exam.timeMillis)break;
                        int used=load.getOrDefault(dayKey(candidate),0);
                        if(used<minLoad){chosen=candidate;minLoad=used;}
                        if(used+minutes<=getDailyGoal()){chosen=candidate;break;}
                        day.add(Calendar.DAY_OF_MONTH,1);
                    }
                    if(repetition==0)firstDay=chosen;
                    load.put(dayKey(chosen),load.getOrDefault(dayKey(chosen),0)+minutes);
                    existing.add(new StudyTask(++seed,exam.id,topic.id,(repetition==0?"Learn":"Recall")+" · "+topic.name,chosen,minutes,Math.max(exam.priority,topic.difficulty),false,true));created++;
                    if(created>=60)break;
                }if(created>=60)break;
            }if(created>=60)break;
        }writeTasks(existing);return created;
    }

    public String getTheme() { return prefs.getString(KEY_THEME, "naruto"); }
    public void setTheme(String theme) { prefs.edit().putString(KEY_THEME, theme).apply(); }

    private static String dayKey(long millis) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis)); }

    private static long dayDistance(String from, String to) {
        try { return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(from),java.time.LocalDate.parse(to)); }
        catch(Exception invalid){return 999;}
    }
}
