package com.brace.examverse.alarms;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomAlarmRepository {
    private final SharedPreferences prefs;
    private static final String KEY="custom_alarms";
    public CustomAlarmRepository(Context c){prefs=c.getSharedPreferences("examverse_custom_alarms",Context.MODE_PRIVATE);}
    public List<CustomAlarm> getAll(){
        List<CustomAlarm> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(prefs.getString(KEY,"[]"));for(int i=0;i<a.length();i++)out.add(CustomAlarm.fromJson(a.getJSONObject(i)));}catch(Exception ignored){}
        Collections.sort(out,(a,b)->{int x=a.hour*60+a.minute,y=b.hour*60+b.minute;return Integer.compare(x,y);});return out;
    }
    public CustomAlarm get(long id){for(CustomAlarm a:getAll())if(a.id==id)return a;return null;}
    public void save(CustomAlarm alarm){List<CustomAlarm> list=getAll();boolean found=false;for(int i=0;i<list.size();i++)if(list.get(i).id==alarm.id){list.set(i,alarm);found=true;break;}if(!found)list.add(alarm);write(list);}
    public void delete(long id){List<CustomAlarm> list=getAll();list.removeIf(a->a.id==id);write(list);}
    private void write(List<CustomAlarm> list){JSONArray a=new JSONArray();for(CustomAlarm x:list)a.put(x.toJson());prefs.edit().putString(KEY,a.toString()).apply();}
}
