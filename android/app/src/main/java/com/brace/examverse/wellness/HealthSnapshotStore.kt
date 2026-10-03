package com.brace.examverse.wellness

import android.content.Context

object HealthSnapshotStore {
    private const val PREFS = "examverse_health_snapshot"
    private const val KEY_CONNECTED = "connected"
    private const val KEY_STEPS = "steps"
    private const val KEY_SLEEP = "sleep_minutes"
    private const val KEY_HEART = "heart_bpm"
    private const val KEY_CALORIES = "calories"
    private const val KEY_EXERCISE = "exercise_minutes"
    private const val KEY_UPDATED = "updated"

    @JvmStatic fun save(context: Context, steps: Long, sleepMinutes: Long, heartBpm: Double, calories: Double, exerciseMinutes: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_STEPS, steps)
            .putLong(KEY_SLEEP, sleepMinutes)
            .putFloat(KEY_HEART, heartBpm.toFloat())
            .putFloat(KEY_CALORIES, calories.toFloat())
            .putLong(KEY_EXERCISE, exerciseMinutes)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
    }

    @JvmStatic fun isConnected(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CONNECTED, false)
    @JvmStatic fun steps(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_STEPS, 0)
    @JvmStatic fun sleepMinutes(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SLEEP, 0)
    @JvmStatic fun heartBpm(context: Context): Double = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_HEART, 0f).toDouble()
    @JvmStatic fun calories(context: Context): Double = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_CALORIES, 0f).toDouble()
    @JvmStatic fun exerciseMinutes(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_EXERCISE, 0)
    @JvmStatic fun updatedAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_UPDATED, 0)
}
