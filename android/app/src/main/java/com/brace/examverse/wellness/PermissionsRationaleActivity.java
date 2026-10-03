package com.brace.examverse.wellness;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.brace.examverse.theme.ThemeManager;

public class PermissionsRationaleActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); ThemeManager.applyWindow(this, getWindow());
        ThemeManager.Palette p = ThemeManager.palette(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22), dp(28), dp(22), dp(28)); root.setBackgroundColor(p.bg);
        root.addView(t("Health data privacy", 28, p.text, true));
        root.addView(t("ExamVerse only reads the Health Connect categories you explicitly grant. The current private build uses health data to show study/recovery context such as sleep, steps, heart-rate average, exercise time and calories.\n\nThe data is cached locally on your device for the dashboard. ExamVerse does not upload it, sell it, use it for advertising, or require a cloud account. You can revoke access at any time in Health Connect settings.\n\nHealth information is never used to diagnose medical conditions. It is shown only as personal context for study planning.", 14, p.muted, false), top(14));
        setContentView(root);
    }
    private TextView t(String s, float sp, int c, boolean b){ TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(b)v.setTypeface(v.getTypeface(),android.graphics.Typeface.BOLD);v.setLineSpacing(0,1.15f);return v; }
    private LinearLayout.LayoutParams top(int d){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(d);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
