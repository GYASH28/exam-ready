package com.brace.examverse.visuals;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import com.brace.examverse.theme.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FocusTrendView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final Path fill = new Path();
    private int[] values = new int[]{0,0,0,0,0,0,0};

    public FocusTrendView(Context context) {
        super(context);
        setMinimumHeight(dp(190));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public FocusTrendView setData(int[] data) {
        if (data != null && data.length > 0) values = data.clone();
        invalidate();
        return this;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ThemeManager.Palette p = ThemeManager.palette(getContext());

        float left = dp(18), right = getWidth() - dp(14);
        float top = dp(18), bottom = getHeight() - dp(34);
        float chartH = Math.max(dp(40), bottom - top);
        float chartW = Math.max(dp(40), right - left);

        int max = 1;
        int sum = 0;
        for (int v : values) { max = Math.max(max, v); sum += v; }
        float avg = sum / (float) Math.max(1, values.length);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setPathEffect(null);
        for (int i = 0; i <= 3; i++) {
            float y = top + chartH * i / 3f;
            paint.setColor(ThemeManager.withAlpha(p.muted, 28));
            canvas.drawLine(left, y, right, y, paint);
        }

        float avgY = bottom - (avg / max) * chartH;
        paint.setColor(ThemeManager.withAlpha(p.accent, 90));
        paint.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(5)}, 0));
        canvas.drawLine(left, avgY, right, avgY, paint);
        paint.setPathEffect(null);

        line.reset();
        fill.reset();
        float step = values.length <= 1 ? chartW : chartW / (values.length - 1f);
        for (int i = 0; i < values.length; i++) {
            float x = left + i * step;
            float y = bottom - (values[i] / (float) max) * chartH;
            if (i == 0) { line.moveTo(x, y); fill.moveTo(x, bottom); fill.lineTo(x, y); }
            else { line.lineTo(x, y); fill.lineTo(x, y); }
        }
        fill.lineTo(right, bottom);
        fill.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
                0, top, 0, bottom,
                ThemeManager.withAlpha(p.primary, 92),
                ThemeManager.withAlpha(p.primary, 3),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fill, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(p.primary);
        canvas.drawPath(line, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(9.5f));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -(values.length - 1));
        SimpleDateFormat fmt = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < values.length; i++) {
            float x = left + i * step;
            float y = bottom - (values[i] / (float) max) * chartH;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(p.surface);
            canvas.drawCircle(x, y, dp(5.4f), paint);
            paint.setColor(p.primary);
            canvas.drawCircle(x, y, dp(3.2f), paint);

            paint.setColor(p.muted);
            canvas.drawText(fmt.format(cal.getTime()), x, getHeight() - dp(9), paint);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(8.5f));
        paint.setColor(ThemeManager.withAlpha(p.muted, 190));
        canvas.drawText("avg " + Math.round(avg) + "m", left, Math.max(dp(10), avgY - dp(5)), paint);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
