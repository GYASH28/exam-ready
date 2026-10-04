package com.brace.examverse.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;

import com.brace.examverse.data.ExamRepository;

public class ThemeBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final android.graphics.drawable.Drawable artwork;
    private final String theme;
    private final ThemeManager.Palette palette;

    public ThemeBackdropView(Context context) {
        super(context);
        theme = new ExamRepository(context).getTheme();
        palette = ThemeManager.palette(context);
        artwork = ThemeManager.artwork(context);
        artwork.setAlpha(38);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float phase = (SystemClock.uptimeMillis() % 18000L) / 18000f;
        artwork.setBounds(0, 0, getWidth(), getHeight());
        artwork.draw(canvas);
        if ("bleach".equals(theme)) drawBleach(canvas, phase);
        else if ("dragonball".equals(theme)) drawDragonBall(canvas, phase);
        else if ("naruto".equals(theme)) drawNaruto(canvas, phase);
        else drawWorld(canvas, phase);

        // The artwork is deliberately slow and subtle. ~20 fps keeps the premium motion
        // without turning a study screen into a battery-hungry game loop.
        if (isShown() && android.provider.Settings.Global.getFloat(getContext().getContentResolver(), android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0) postInvalidateDelayed(80);
    }

    private void drawNaruto(Canvas c, float phase) {
        ThemeManager.Palette p = palette;
        int w = getWidth(), h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ThemeManager.withAlpha(p.primary, 11));
        c.drawCircle(w * .88f, h * .12f, Math.min(w, h) * .28f, paint);
        paint.setColor(ThemeManager.withAlpha(p.secondary, 7));
        c.drawCircle(w * .04f, h * .82f, Math.min(w, h) * .34f, paint);

        float cx = w * .84f, cy = h * .17f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 1; i <= 7; i++) {
            paint.setStrokeWidth(dp(i % 2 == 0 ? 1.1f : 2.2f));
            paint.setColor(ThemeManager.withAlpha(p.primary, 18 + i * 4));
            c.drawCircle(cx, cy, dp(17 + i * 17), paint);
        }

        // Hand-drawn spiral/seal motion.
        path.reset();
        float turns = 4.4f;
        for (int i = 0; i <= 120; i++) {
            float t = i / 120f;
            float angle = (t * turns + phase * .18f) * (float) (Math.PI * 2);
            float radius = dp(7) + t * dp(67);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        paint.setColor(ThemeManager.withAlpha(p.accent, 28));
        paint.setStrokeWidth(dp(1.4f));
        c.drawPath(path, paint);

        paint.setStrokeWidth(dp(1f));
        paint.setColor(ThemeManager.withAlpha(p.text, 13));
        for (int i = -3; i < 14; i++) {
            float drift = (phase * dp(18));
            c.drawLine(-dp(70), dp(130 + i * 112) + drift,
                    w + dp(90), dp(-20 + i * 112) + drift, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 26; i++) {
            float x = ((i * 83) % Math.max(1, w - dp(16))) + dp(8);
            float baseY = ((i * 149) % Math.max(1, h - dp(20))) + dp(10);
            float y = (baseY + phase * dp(24 + (i % 4) * 7)) % Math.max(1, h);
            paint.setColor(ThemeManager.withAlpha(i % 3 == 0 ? p.secondary : p.primary, 14 + (i % 4) * 3));
            c.drawCircle(x, y, dp(1 + (i % 3)), paint);
        }
    }

    private void drawDragonBall(Canvas c, float phase) {
        ThemeManager.Palette p = palette;
        int w = getWidth(), h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 15; i++) {
            float x = ((i * 71 + phase * 42) % Math.max(1, w - dp(20))) + dp(10);
            float y = ((i * 139 + (1f - phase) * 55) % Math.max(1, h - dp(20))) + dp(10);
            int color = i % 3 == 0 ? p.accent : (i % 3 == 1 ? p.primary : p.secondary);
            paint.setColor(ThemeManager.withAlpha(color, 18 + (i % 4) * 5));
            c.drawCircle(x, y, dp(2 + (i % 3)), paint);
        }

        float orbX = w * .80f;
        float orbY = h * .16f;
        float pulse = 1f + .06f * (float) Math.sin(phase * Math.PI * 2);
        for (int i = 5; i >= 1; i--) {
            paint.setColor(ThemeManager.withAlpha(i % 2 == 0 ? p.primary : p.accent, 9 + i * 4));
            c.drawCircle(orbX, orbY, dp((16 + i * 14) * pulse), paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            float y = dp(80 + i * 170) + ((phase * dp(120 + i * 9)) % dp(170));
            paint.setStrokeWidth(dp(i % 3 == 0 ? 3f : 1.3f));
            paint.setColor(ThemeManager.withAlpha(i % 2 == 0 ? p.secondary : p.accent, 16 + (i % 3) * 6));
            c.drawLine(-dp(30), y + dp(55), w + dp(45), y - dp(70), paint);
        }

        // Energy arcs.
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(ThemeManager.withAlpha(p.primary, 32));
        for (int ring = 0; ring < 4; ring++) {
            float r = dp(50 + ring * 28) + (phase * dp(20));
            c.drawCircle(orbX, orbY, r, paint);
        }
    }

    private void drawBleach(Canvas c, float phase) {
        ThemeManager.Palette p = palette;
        int w = getWidth(), h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ThemeManager.withAlpha(Color.WHITE, 8));
        c.drawCircle(w * .86f, h * .14f, dp(105), paint);
        paint.setColor(ThemeManager.withAlpha(p.primary, 12));
        c.drawCircle(w * .15f, h * .76f, dp(150), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 11; i++) {
            float offset = (phase * dp(60 + i * 3)) % dp(90);
            paint.setStrokeWidth(dp(i % 4 == 0 ? 2.6f : 1f));
            paint.setColor(ThemeManager.withAlpha(i % 3 == 0 ? p.primary : Color.WHITE, 12 + (i % 4) * 5));
            c.drawLine(-dp(20), dp(90 + i * 128) + offset,
                    w + dp(60), dp(10 + i * 128) + offset, paint);
        }

        // Blade-slash geometry.
        path.reset();
        path.moveTo(w * .54f, -dp(20));
        path.lineTo(w * .96f, h * .43f);
        path.lineTo(w * .89f, h * .45f);
        path.lineTo(w * .48f, 0);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ThemeManager.withAlpha(p.primary, 14));
        c.drawPath(path, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(ThemeManager.withAlpha(p.accent, 20));
        float ring = dp(92f + 10f * (float) Math.sin(phase * Math.PI * 2));
        c.drawCircle(w * .86f, h * .14f, ring, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 22; i++) {
            float x = ((i * 97) % Math.max(1, w - dp(12))) + dp(6);
            float y = (((i * 163) + phase * dp(32 + i % 5)) % Math.max(1, h - dp(12))) + dp(6);
            paint.setColor(ThemeManager.withAlpha(i % 5 == 0 ? p.primary : Color.WHITE, 10 + (i % 4) * 4));
            c.drawCircle(x, y, dp(i % 6 == 0 ? 2f : 1f), paint);
        }
    }

    private void drawWorld(Canvas c, float phase) {
        paint.setStyle(Paint.Style.STROKE); paint.setColor(ThemeManager.withAlpha(palette.primary, 48)); paint.setStrokeWidth(dp(1));
        if ("blackclover".equals(theme)) {
            float cx=getWidth()*.82f,cy=getHeight()*.20f,r=dp(80);
            c.drawCircle(cx,cy,r,paint);c.drawCircle(cx,cy,r*.75f,paint);
            for(int i=0;i<5;i++){float angle=(float)(i*Math.PI*2/5+phase*.4); c.drawOval(cx+(float)Math.cos(angle)*r*.3f-dp(12),cy+(float)Math.sin(angle)*r*.3f-dp(20),cx+(float)Math.cos(angle)*r*.3f+dp(12),cy+(float)Math.sin(angle)*r*.3f+dp(20),paint);}
        } else if ("demonslayer".equals(theme)) {
            for(int i=0;i<8;i++){path.reset();float y=getHeight()*.15f+i*dp(95);path.moveTo(0,y);path.cubicTo(getWidth()*.3f,y-dp(60),getWidth()*.65f,y+dp(60),getWidth(),y);c.drawPath(path,paint);}
        } else {
            float cx=getWidth()*.8f,cy=getHeight()*.18f,r=dp(85);c.drawCircle(cx,cy,r,paint);
            for(int i=0;i<8;i++){double a=i*Math.PI/4; c.drawLine(cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,paint);}
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
