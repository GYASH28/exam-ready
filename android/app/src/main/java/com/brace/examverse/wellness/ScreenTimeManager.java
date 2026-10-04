package com.brace.examverse.wellness;

import android.app.AppOpsManager;
import android.app.usage.EventStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScreenTimeManager {
    public static class AppUsage {
        public final String packageName;
        public final String label;
        public final long millis;
        public AppUsage(String packageName, String label, long millis) {
            this.packageName = packageName; this.label = label; this.millis = millis;
        }
    }

    public static class Snapshot {
        public final long screenInteractiveMillis;
        public final long appForegroundMillis;
        public final int unlocks;
        public final List<AppUsage> topApps;
        public Snapshot(long screenInteractiveMillis, long appForegroundMillis, int unlocks, List<AppUsage> topApps) {
            this.screenInteractiveMillis = screenInteractiveMillis;
            this.appForegroundMillis = appForegroundMillis;
            this.unlocks = unlocks;
            this.topApps = topApps;
        }
    }

    public static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) { return false; }
    }

    public static void openUsageAccessSettings(Context context) {
        Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public static Snapshot today(Context context) {
        if (!hasUsageAccess(context)) return new Snapshot(0, 0, 0, new ArrayList<>());
        long end = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager pm = context.getPackageManager();
        long totalForeground = 0;
        List<AppUsage> apps = new ArrayList<>();
        try {
            Map<String, UsageStats> map = manager.queryAndAggregateUsageStats(start, end);
            if (map != null) {
                for (UsageStats s : map.values()) {
                    long ms = Math.max(0, s.getTotalTimeInForeground());
                    if (ms < 60_000L) continue;
                    String pkg = s.getPackageName();
                    if (pkg.equals(context.getPackageName())) continue;
                    String label = pkg;
                    try {
                        ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                        label = pm.getApplicationLabel(info).toString();
                    } catch (Exception ignored) {}
                    totalForeground += ms;
                    apps.add(new AppUsage(pkg, label, ms));
                }
            }
        } catch (Exception ignored) {}
        Collections.sort(apps, (a,b) -> Long.compare(b.millis, a.millis));
        if (apps.size() > 8) apps = new ArrayList<>(apps.subList(0,8));

        long interactive = 0;
        int unlocks = 0;
        if (android.os.Build.VERSION.SDK_INT >= 28) try {
            List<EventStats> events = manager.queryEventStats(UsageStatsManager.INTERVAL_DAILY, start, end);
            if (events != null) for (EventStats e : events) {
                if (e.getEventType() == UsageEvents.Event.SCREEN_INTERACTIVE) interactive += e.getTotalTime();
                else if (e.getEventType() == UsageEvents.Event.KEYGUARD_HIDDEN) unlocks += e.getCount();
            }
        } catch (Exception ignored) {}
        if (interactive <= 0) interactive = totalForeground;
        return new Snapshot(interactive, totalForeground, unlocks, apps);
    }

    public static String formatDuration(long millis) {
        long mins = Math.max(0, millis) / 60_000L;
        long h = mins / 60; long m = mins % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }
}
