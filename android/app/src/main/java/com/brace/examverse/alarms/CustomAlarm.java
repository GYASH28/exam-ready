package com.brace.examverse.alarms;

import org.json.JSONObject;

public class CustomAlarm {
    public long id;
    public String label;
    public int hour;
    public int minute;
    public String repeat; // once, daily, weekdays
    public boolean enabled;
    public boolean vibrate;
    public String soundUri;

    public CustomAlarm(long id, String label, int hour, int minute, String repeat, boolean enabled, boolean vibrate, String soundUri) {
        this.id=id; this.label=label; this.hour=hour; this.minute=minute; this.repeat=repeat; this.enabled=enabled; this.vibrate=vibrate; this.soundUri=soundUri == null ? "" : soundUri;
    }
    public JSONObject toJson(){
        JSONObject o=new JSONObject();
        try{o.put("id",id);o.put("label",label);o.put("hour",hour);o.put("minute",minute);o.put("repeat",repeat);o.put("enabled",enabled);o.put("vibrate",vibrate);o.put("soundUri",soundUri);}catch(Exception ignored){}
        return o;
    }
    public static CustomAlarm fromJson(JSONObject o){return new CustomAlarm(o.optLong("id"),o.optString("label","Study alarm"),o.optInt("hour",7),o.optInt("minute",0),o.optString("repeat","once"),o.optBoolean("enabled",true),o.optBoolean("vibrate",true),o.optString("soundUri",""));}
}
