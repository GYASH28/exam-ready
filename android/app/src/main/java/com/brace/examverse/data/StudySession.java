package com.brace.examverse.data;

import org.json.JSONException;
import org.json.JSONObject;

public class StudySession {
    public long id;
    public long examId;
    public long topicId;
    public long startedAt;
    public int minutes;
    public String mode;

    public StudySession(long id, long examId, long topicId, long startedAt, int minutes, String mode) {
        this.id = id;
        this.examId = examId;
        this.topicId = topicId;
        this.startedAt = startedAt;
        this.minutes = Math.max(0, minutes);
        this.mode = mode == null ? "Focus" : mode;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("examId", examId); o.put("topicId", topicId);
        o.put("startedAt", startedAt); o.put("minutes", minutes); o.put("mode", mode);
        return o;
    }

    public static StudySession fromJson(JSONObject o) {
        return new StudySession(o.optLong("id", System.currentTimeMillis()), o.optLong("examId", -1),
                o.optLong("topicId", -1), o.optLong("startedAt", System.currentTimeMillis()),
                o.optInt("minutes", 0), o.optString("mode", "Focus"));
    }
}
