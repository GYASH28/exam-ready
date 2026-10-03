package com.brace.examverse.widgets;

import android.content.Context;
import android.content.SharedPreferences;

public class WidgetStyleStore {
    private static final String PREFS="examverse_widget_styles";
    public static class Style {
        public String theme; public int opacity; public String density; public long examId;
        Style(String theme,int opacity,String density,long examId){this.theme=theme;this.opacity=opacity;this.density=density;this.examId=examId;}
    }
    public static Style get(Context c,int id){SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);return new Style(p.getString("theme_"+id,"auto"),p.getInt("opacity_"+id,170),p.getString("density_"+id,"detailed"),p.getLong("exam_"+id,-1));}
    public static void save(Context c,int id,String theme,int opacity,String density,long examId){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("theme_"+id,theme).putInt("opacity_"+id,Math.max(70,Math.min(235,opacity))).putString("density_"+id,density).putLong("exam_"+id,examId).apply();}
    public static void delete(Context c,int id){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove("theme_"+id).remove("opacity_"+id).remove("density_"+id).remove("exam_"+id).apply();}
}
