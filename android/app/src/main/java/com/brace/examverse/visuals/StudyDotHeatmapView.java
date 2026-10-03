package com.brace.examverse.visuals;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.brace.examverse.theme.ThemeManager;

public class StudyDotHeatmapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] values = new int[28];

    public StudyDotHeatmapView(Context context) {
        super(context);
        setMinimumHeight(dp(165));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public StudyDotHeatmapView setData(int[] data) {
        if (data != null && data.length > 0) values = data.clone();
        invalidate();
        return this;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ThemeManager.Palette p = ThemeManager.palette(getContext());

        int count = Math.min(28, values.length);
        int max = 1;
        int active = 0;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, values[i]);
            if (values[i] > 0) active++;
        }

        float left = dp(20), right = getWidth() - dp(18);
        float top = dp(28), bottom = getHeight() - dp(26);
        float colW = (right - left) / 7f;
        float rowH = (bottom - top) / 4f;

        paint.setTextSize(dp(9f));
        paint.setColor(p.muted);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("4 weeks ago", left, dp(12), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("today", right, dp(12), paint);

        for (int i = 0; i < count; i++) {
            int row = i / 7;
            int col = i % 7;
            float x = left + colW * col + colW / 2f;
            float y = top + rowH * row + rowH / 2f;
            float ratio = values[i] / (float) max;
            float r = dp(4.5f) + dp(7.5f) * (float) Math.sqrt(ratio);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ThemeManager.withAlpha(p.surfaceAlt, p.dark ? 190 : 210));
            canvas.drawCircle(x, y, dp(11.5f), paint);

            if (values[i] > 0) {
                int a = 85 + Math.round(150 * ratio);
                paint.setColor(ThemeManager.withAlpha(p.primary, a));
                canvas.drawCircle(x, y, r, paint);
                if (ratio > .72f) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1.2f));
                    paint.setColor(ThemeManager.withAlpha(p.accent, 175));
                    canvas.drawCircle(x, y, dp(13.5f), paint);
                }
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(9f));
        paint.setColor(p.muted);
        canvas.drawText(active + "/28 active days", left, getHeight() - dp(5), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("dot size = study minutes", right, getHeight() - dp(5), paint);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
