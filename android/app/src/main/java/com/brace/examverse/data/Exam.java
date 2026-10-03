package com.brace.examverse.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Exam {
    public long id;
    public String title;
    public String subject;
    public String category;
    public String notes;
    public long timeMillis;
    public boolean completed;
    public boolean remind;
    public int reminderHours;
    public int priority;
    public int difficulty;
    public int targetScore;

    public Exam(long id, String title, String subject, String category, String notes,
                long timeMillis, boolean completed, boolean remind) {
        this(id, title, subject, category, notes, timeMillis, completed, remind, 24, 3, 3, 80);
    }

    public Exam(long id, String title, String subject, String category, String notes,
                long timeMillis, boolean completed, boolean remind, int reminderHours,
                int priority, int difficulty, int targetScore) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.category = category;
        this.notes = notes;
        this.timeMillis = timeMillis;
        this.completed = completed;
        this.remind = remind;
        this.reminderHours = reminderHours;
        this.priority = clamp(priority, 1, 5);
        this.difficulty = clamp(difficulty, 1, 5);
        this.targetScore = clamp(targetScore, 1, 100);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("subject", subject);
        o.put("category", category);
        o.put("notes", notes);
        o.put("timeMillis", timeMillis);
        o.put("completed", completed);
        o.put("remind", remind);
        o.put("reminderHours", reminderHours);
        o.put("priority", priority);
        o.put("difficulty", difficulty);
        o.put("targetScore", targetScore);
        return o;
    }

    public static Exam fromJson(JSONObject o) {
        return new Exam(
                o.optLong("id", System.currentTimeMillis()),
                o.optString("title", "Exam"),
                o.optString("subject", "General"),
                o.optString("category", "Exam"),
                o.optString("notes", ""),
                o.optLong("timeMillis", System.currentTimeMillis()),
                o.optBoolean("completed", false),
                o.optBoolean("remind", true),
                o.optInt("reminderHours", 24),
                o.optInt("priority", 3),
                o.optInt("difficulty", 3),
                o.optInt("targetScore", 80)
        );
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
