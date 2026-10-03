package com.brace.examverse.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.brace.examverse.data.ExamRepository;

public class ThemeBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public ThemeBackdropView(Context context) { super(context); setClickable(false); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        String theme = new ExamRepository(getContext()).getTheme();
        if ("bleach".equals(theme)) drawBleach(canvas);
        else if ("dragonball".equals(theme)) drawDragonBall(canvas);
        else drawNaruto(canvas);
    }

    private void drawNaruto(Canvas c) {
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
        float cx = getWidth() * .86f, cy = getHeight() * .15f;
        for (int i = 1; i <= 8; i++) {
            paint.setStrokeWidth(i % 2 == 0 ? 2f : 4f);
            paint.setColor(Color.argb(10 + i * 2, 249, 115, 22)); c.drawCircle(cx, cy, i * 24f, paint);
        }
        paint.setStrokeWidth(2f); paint.setColor(Color.argb(15, 17, 24, 39));
        for (int i = -4; i < 14; i++) c.drawLine(-80, 150 + i * 115, getWidth() + 120, -20 + i * 115, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(10, 249,115,22));
        c.drawCircle(getWidth()*.12f, getHeight()*.77f, 120, paint);
    }

    private void drawDragonBall(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 11; i++) {
            float x = ((i * 67) % Math.max(1, getWidth() - 40)) + 20;
            float y = 90 + i * 145;
            paint.setColor(Color.argb(12 + (i % 3) * 4, 245,158,11)); c.drawCircle(x, y, 24 + (i % 4) * 9, paint);
        }
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(6f); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.argb(18,37,99,235));
        for (int i = 0; i < 7; i++) c.drawLine(-40, 210 + i * 250, getWidth() + 40, 20 + i * 250, paint);
        paint.setColor(Color.argb(13,249,115,22)); paint.setStrokeWidth(2f);
        for (int i = 1; i < 6; i++) c.drawCircle(getWidth()*.83f, getHeight()*.35f, i*32f, paint);
    }

    private void drawBleach(Canvas c) {
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = -4; i < 15; i++) {
            paint.setStrokeWidth(i % 3 == 0 ? 6f : 2f);
            paint.setColor(i % 3 == 0 ? Color.argb(35,239,68,68) : Color.argb(20,255,255,255));
            c.drawLine(-100, 120 + i * 120, getWidth() + 100, -70 + i * 120, paint);
        }
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(18,239,68,68));
        path.reset(); path.moveTo(getWidth()*.65f, 40); path.lineTo(getWidth(), 40); path.lineTo(getWidth(), 330); path.close(); c.drawPath(path, paint);
    }
}
