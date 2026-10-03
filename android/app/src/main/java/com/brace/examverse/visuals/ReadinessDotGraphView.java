package com.brace.examverse.visuals;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.brace.examverse.data.Exam;
import com.brace.examverse.data.ExamRepository;
import com.brace.examverse.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class ReadinessDotGraphView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ExamRepository repo;
    private List<Exam> exams = new ArrayList<>();

    public ReadinessDotGraphView(Context context) {
        super(context);
        repo = new ExamRepository(context);
        setMinimumHeight(dp(220));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public ReadinessDotGraphView setExams(List<Exam> items) {
        exams = items == null ? new ArrayList<>() : new ArrayList<>(items);
        invalidate();
        return this;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ThemeManager.Palette p = ThemeManager.palette(getContext());

        float left = dp(40), right = getWidth() - dp(15);
        float top = dp(22), bottom = getHeight() - dp(42);
        float w = Math.max(dp(40), right - left);
        float h = Math.max(dp(40), bottom - top);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(ThemeManager.withAlpha(p.muted, 34));
        for (int i = 0; i <= 4; i++) {
            float y = top + h * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
        }
        for (int i = 0; i <= 3; i++) {
            float x = left + w * i / 3f;
            canvas.drawLine(x, top, x, bottom, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(8.5f));
        paint.setColor(p.muted);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("100", left - dp(7), top + dp(3), paint);
        canvas.drawText("50", left - dp(7), top + h * .5f + dp(3), paint);
        canvas.drawText("0", left - dp(7), bottom + dp(3), paint);

        if (exams.isEmpty()) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(11f));
            canvas.drawText("Add exams to unlock the readiness map", getWidth()/2f, getHeight()/2f, paint);
            return;
        }

        long now = System.currentTimeMillis();
        float maxDays = 1f;
        for (Exam e : exams) {
            float days = Math.max(0f, (e.timeMillis - now) / 86_400_000f);
            maxDays = Math.max(maxDays, days);
        }

        for (Exam e : exams) {
            float days = Math.max(0f, (e.timeMillis - now) / 86_400_000f);
            int readiness = repo.readinessForExam(e.id);
            float x = left + (days / maxDays) * w;
            float y = bottom - (readiness / 100f) * h;
            float radius = dp(5.5f + Math.max(1, e.priority) * 1.35f);

            int color = readiness >= 70 ? p.success : (readiness >= 40 ? p.warning : p.danger);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ThemeManager.withAlpha(color, 42));
            canvas.drawCircle(x, y, radius + dp(5), paint);
            paint.setColor(color);
            canvas.drawCircle(x, y, radius, paint);
            paint.setColor(p.surface);
            canvas.drawCircle(x, y, Math.max(dp(2), radius * .35f), paint);

            if (exams.size() <= 7) {
                String label = e.subject == null || e.subject.trim().isEmpty() ? e.title : e.subject;
                if (label.length() > 8) label = label.substring(0, 8);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(dp(7.8f));
                paint.setColor(p.text);
                canvas.drawText(label, x, Math.max(top + dp(8), y - radius - dp(6)), paint);
            }
        }

        paint.setTextSize(dp(8.5f));
        paint.setColor(p.muted);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("SOON", left, getHeight() - dp(12), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("LATER", right, getHeight() - dp(12), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("x = time to exam  ·  y = readiness  ·  dot = priority", getWidth()/2f, getHeight() - dp(1), paint);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
