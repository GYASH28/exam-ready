package com.brace.examverse.study;

import android.content.Context;
import org.json.*;
import java.util.*;

public class LearningStore {
    private final android.content.SharedPreferences prefs;
    public LearningStore(Context c) { prefs=c.getSharedPreferences("examverse_learning",0); }
    public List<JSONObject> items(String key) {
        List<JSONObject> list=new ArrayList<>();
        try { JSONArray array=new JSONArray(prefs.getString(key,"[]"));for(int i=0;i<array.length();i++)list.add(array.getJSONObject(i)); } catch(JSONException ignored){}
        return list;
    }
    public void save(String key, JSONObject item) {
        List<JSONObject> list=items(key);list.removeIf(x->x.optLong("id")==item.optLong("id"));list.add(item);write(key,list);
    }
    public void delete(String key,long id){List<JSONObject> list=items(key);list.removeIf(x->x.optLong("id")==id);write(key,list);}
    private void write(String key,List<JSONObject> list){JSONArray a=new JSONArray();for(JSONObject x:list)a.put(x);prefs.edit().putString(key,a.toString()).apply();}
    public List<JSONObject> dueCards(){List<JSONObject> list=items("cards");long now=System.currentTimeMillis();list.removeIf(x->x.optLong("due")>now);list.sort(Comparator.comparingLong(x->x.optLong("due")));return list;}
    public void grade(JSONObject card,int rating) {
        try {
            int previous=card.optInt("interval",0);
            int interval=rating==0?0:rating==1?Math.max(1,previous*2):Math.max(3,previous*3);
            interval=Math.min(180,interval);
            card.put("interval",interval);card.put("due",System.currentTimeMillis()+(rating==0?600_000L:interval*86_400_000L));card.put("reviews",card.optInt("reviews")+1);save("cards",card);
        }catch(JSONException ignored){}
    }
    public int unresolvedMistakes(){int count=0;for(JSONObject x:items("mistakes"))if(!x.optBoolean("resolved"))count++;return count;}
}
