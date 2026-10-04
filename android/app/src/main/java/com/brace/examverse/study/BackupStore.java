package com.brace.examverse.study;

import android.content.Context;
import android.content.SharedPreferences;
import com.brace.examverse.data.*;
import org.json.*;
import java.util.*;

/** Versioned study backup; permissions, health data and launcher IDs stay device-local. */
public class BackupStore {
    private static final String[] STORES={"examverse_data","examverse_learning"};
    private static final Set<String> ARRAYS=new HashSet<>(Arrays.asList("exams","topics","sessions_v2","tasks_v2","cards","mistakes"));
    private static final Set<String> INTS=new HashSet<>(Arrays.asList("focus_minutes","streak","xp_v2","daily_goal_v2"));
    public static String export(Context c) throws JSONException {
        JSONObject file=new JSONObject();file.put("format","examverse-study-backup");file.put("version",1);file.put("createdAt",System.currentTimeMillis());JSONObject data=new JSONObject();
        for(String name:STORES){JSONObject values=new JSONObject();for(Map.Entry<String,?> e:c.getSharedPreferences(name,0).getAll().entrySet())if(allowed(e.getKey()))values.put(e.getKey(),e.getValue());data.put(name,values);}file.put("data",data);return file.toString(2);
    }
    private static boolean allowed(String key){return ARRAYS.contains(key)||INTS.contains(key)||"theme".equals(key)||"last_study".equals(key);}
    public static JSONObject validate(String text) throws JSONException {
        if(text.length()>4_000_000)throw new JSONException("Backup exceeds 4 MB");
        JSONObject file=new JSONObject(text);if(!"examverse-study-backup".equals(file.optString("format"))||file.optInt("version")!=1)throw new JSONException("Unsupported backup format");
        JSONObject data=file.getJSONObject("data");
        for(String name:STORES){JSONObject values=data.getJSONObject(name);Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();if(!allowed(key))throw new JSONException("Unknown backup field");Object value=values.get(key);
            if(ARRAYS.contains(key)){if(!(value instanceof String))throw new JSONException("Invalid collection");JSONArray a=new JSONArray((String)value);if(a.length()>20000)throw new JSONException("Too many records");Set<Long> ids=new HashSet<>();for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);long id=x.getLong("id");if(!ids.add(id))throw new JSONException("Duplicate record ID");switch(key){case "exams":Exam.fromJson(x);x.getLong("timeMillis");break;case "topics":Topic.fromJson(x);break;case "sessions_v2":StudySession.fromJson(x);break;case "tasks_v2":StudyTask.fromJson(x);break;case "cards":case "mistakes":x.getString("front");x.getString("back");break;}}}
            else if(INTS.contains(key)){if(!(value instanceof Number)||((Number)value).longValue()<0||((Number)value).longValue()>Integer.MAX_VALUE)throw new JSONException("Invalid progress value");}
            else if(!(value instanceof String))throw new JSONException("Invalid setting");
        }}return data;
    }
    public static void restore(Context c, JSONObject data) throws JSONException {
        // Validate all content before changing either preference store.
        JSONObject envelope=new JSONObject();envelope.put("format","examverse-study-backup");envelope.put("version",1);envelope.put("data",data);validate(envelope.toString());
        for(String name:STORES){JSONObject values=data.getJSONObject(name);SharedPreferences.Editor editor=c.getSharedPreferences(name,0).edit().clear();Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();Object value=values.get(key);if(value instanceof Number)editor.putInt(key,((Number)value).intValue());else editor.putString(key,(String)value);}editor.apply();}
    }
}
