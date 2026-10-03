package com.brace.examverse.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Topic {
    public long id;
    public long examId;
    public String name;
    public boolean done;
    public int difficulty;
    public int confidence;
    public int estimatedMinutes;
    public long lastReviewedAt;

    public Topic(long id, long examId, String name, boolean done) {
        this(id, examId, name, done, 3, done ? 5 : 2, 30, 0L);
    }

    public Topic(long id, long examId, String name, boolean done, int difficulty,
                 int confidence, int estimatedMinutes, long lastReviewedAt) {
        this.id = id;
        this.examId = examId;
        this.name = name;
        this.done = done;
        this.difficulty = clamp(difficulty, 1, 5);
        this.confidence = clamp(confidence, 1, 5);
        this.estimatedMinutes = Math.max(5, estimatedMinutes);
        this.lastReviewedAt = lastReviewedAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("examId", examId);
        o.put("name", name);
        o.put("done", done);
        o.put("difficulty", difficulty);
        o.put("confidence", confidence);
        o.put("estimatedMinutes", estimatedMinutes);
        o.put("lastReviewedAt", lastReviewedAt);
        return o;
    }

    public static Topic fromJson(JSONObject o) {
        boolean done = o.optBoolean("done", false);
        return new Topic(
                o.optLong("id", System.currentTimeMillis()),
                o.optLong("examId", -1),
                o.optString("name", "Topic"),
                done,
                o.optInt("difficulty", 3),
                o.optInt("confidence", done ? 5 : 2),
                o.optInt("estimatedMinutes", 30),
                o.optLong("lastReviewedAt", 0L)
        );
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
