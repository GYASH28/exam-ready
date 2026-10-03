package com.brace.examverse.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import com.brace.examverse.theme.ThemeManager;

class WidgetGlassRenderer {
    static Bitmap render(Context c,int widgetId,WidgetStyleStore.Style style){
        AppWidgetManager m=AppWidgetManager.getInstance(c);Bundle o=m.getAppWidgetOptions(widgetId);int minW=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,260);int minH=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,120);float d=c.getResources().getDisplayMetrics().density;int w=Math.max(320,Math.min(900,Math.round(minW*d)));int h=Math.max(180,Math.min(650,Math.round(minH*d)));Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);int accent=accent(c,style.theme);boolean dark="bleach".equals(style.theme)||("auto".equals(style.theme)&&ThemeManager.palette(c).dark);int base=dark?Color.rgb(11,12,16):Color.rgb(248,250,252);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setShader(new LinearGradient(0,0,w,h,Color.argb(style.opacity,Color.red(base),Color.green(base),Color.blue(base)),Color.argb(Math.max(55,style.opacity-60),Color.red(accent),Color.green(accent),Color.blue(accent)),Shader.TileMode.CLAMP));float r=32*d;canvas.drawRoundRect(2*d,2*d,w-2*d,h-2*d,r,r,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f*d);p.setColor(Color.argb(dark?80:125,255,255,255));canvas.drawRoundRect(3*d,3*d,w-3*d,h-3*d,r,r,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(25,255,255,255));canvas.drawCircle(w*.18f,h*.03f,w*.35f,p);p.setColor(Color.argb(24,Color.red(accent),Color.green(accent),Color.blue(accent)));canvas.drawCircle(w*.92f,h*.72f,w*.42f,p);p.setColor(Color.argb(55,255,255,255));canvas.drawRoundRect(16*d,12*d,w-16*d,13*d,3*d,3*d,p);return b;
    }
    static int accent(Context c,String theme){if("naruto".equals(theme))return Color.rgb(249,115,22);if("dragonball".equals(theme))return Color.rgb(37,99,235);if("bleach".equals(theme))return Color.rgb(220,38,38);if("frost".equals(theme))return Color.rgb(14,165,233);return ThemeManager.palette(c).primary;}
    static int textColor(Context c,String theme){boolean dark="bleach".equals(theme)||("auto".equals(theme)&&ThemeManager.palette(c).dark);return dark?Color.WHITE:Color.rgb(15,23,42);}
    static int mutedColor(Context c,String theme){boolean dark="bleach".equals(theme)||("auto".equals(theme)&&ThemeManager.palette(c).dark);return dark?Color.rgb(212,212,216):Color.rgb(71,85,105);}
}
