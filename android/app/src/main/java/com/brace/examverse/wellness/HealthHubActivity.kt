package com.brace.examverse.wellness

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
import com.brace.examverse.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class HealthHubActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var status: TextView
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyWindow(this, window)
        permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.isEmpty()) {
                status.text = "No health categories were granted. ExamVerse will keep working without them."
            } else syncHealth()
        }
        render()
        refreshPermissionState()
    }

    private fun render() {
        val p = ThemeManager.palette(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(p.bg)
        }
        root.addView(label("Health & recovery", 29f, p.text, true))
        root.addView(label("Connect only the categories you want. ExamVerse uses them locally to show recovery context beside your study load.", 14f, p.muted, false), top(8))

        val data = label("Requested read access\n• Steps\n• Sleep duration\n• Heart-rate average\n• Calories burned\n• Exercise time", 13f, p.text, false)
        data.setPadding(dp(16), dp(16), dp(16), dp(16))
        data.background = ThemeManager.outlined(p.surface, withAlpha(p.primary, 55), dp(22).toFloat(), dp(1))
        root.addView(data, top(18))

        status = label("Checking Health Connect…", 13f, p.muted, false)
        root.addView(status, top(16))

        val connect = button("Connect / change health access", p.primary, if (p.dark) Color.WHITE else Color.WHITE)
        connect.setOnClickListener { requestOrSync() }
        root.addView(connect, topHeight(18, 56))

        val sync = button("Sync today’s health snapshot", p.surfaceAlt, p.text)
        sync.setOnClickListener { syncHealth() }
        root.addView(sync, topHeight(10, 52))

        val privacy = button("How ExamVerse uses health data", Color.TRANSPARENT, p.primary)
        privacy.setOnClickListener { startActivity(Intent(this, PermissionsRationaleActivity::class.java)) }
        root.addView(privacy, topHeight(6, 48))
        setContentView(root)
    }

    private fun requestOrSync() {
        val sdk = HealthConnectClient.getSdkStatus(this)
        if (sdk == HealthConnectClient.SDK_AVAILABLE) {
            scope.launch {
                try {
                    val client = HealthConnectClient.getOrCreate(this@HealthHubActivity)
                    val granted = client.permissionController.getGrantedPermissions()
                    if (granted.containsAll(permissions)) syncHealth() else permissionLauncher.launch(permissions)
                } catch (e: Exception) { status.text = "Health Connect couldn’t open: ${e.message ?: "unknown error"}" }
            }
        } else {
            status.text = "Health Connect isn’t available on this device yet."
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun refreshPermissionState() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            status.text = "Health Connect isn’t available. On Android 13 or lower it may need to be installed first."
            return
        }
        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@HealthHubActivity)
                val granted = client.permissionController.getGrantedPermissions()
                status.text = if (granted.isEmpty()) "Not connected yet." else "Connected to ${granted.size} health permissions. Tap Sync to refresh today."
            } catch (e: Exception) { status.text = "Couldn’t check Health Connect permissions." }
        }
    }

    private fun syncHealth() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            status.text = "Health Connect isn’t available on this device."
            return
        }
        status.text = "Syncing today’s health data…"
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = HealthConnectClient.getOrCreate(this@HealthHubActivity)
                    val granted = client.permissionController.getGrantedPermissions()
                    if (granted.isEmpty()) return@withContext null
                    val now = Instant.now()
                    val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val metrics = mutableSetOf<androidx.health.connect.client.aggregate.AggregateMetric<*>>()
                    if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) metrics.add(StepsRecord.COUNT_TOTAL)
                    if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) metrics.add(HeartRateRecord.BPM_AVG)
                    if (granted.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))) metrics.add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                    if (granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) metrics.add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
                    val today = if (metrics.isEmpty()) null else client.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(start, now)))
                    val sleep = if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
                        client.aggregate(AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), TimeRangeFilter.between(now.minus(Duration.ofHours(36)), now)))[SleepSessionRecord.SLEEP_DURATION_TOTAL]
                    } else null
                    val steps = today?.get(StepsRecord.COUNT_TOTAL) ?: 0L
                    val heart = today?.get(HeartRateRecord.BPM_AVG) ?: 0.0
                    val calories = today?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories ?: 0.0
                    val exercise = today?.get(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)?.toMinutes() ?: 0L
                    longArrayOf(steps, sleep?.toMinutes() ?: 0L, heart.toLong(), calories.toLong(), exercise)
                }
                if (result == null) {
                    status.text = "Grant at least one health category first."
                    return@launch
                }
                HealthSnapshotStore.save(this@HealthHubActivity, result[0], result[1], result[2].toDouble(), result[3].toDouble(), result[4])
                status.text = "Synced: ${result[0]} steps · ${result[1] / 60}h ${result[1] % 60}m sleep · ${result[2]} bpm · ${result[4]}m exercise"
                setResult(Activity.RESULT_OK)
            } catch (e: SecurityException) {
                status.text = "A health permission was revoked. Tap Connect to choose access again."
            } catch (e: Exception) {
                status.text = "Health sync failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun label(s: String, sp: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = s; textSize = sp; setTextColor(color); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD); setLineSpacing(0f, 1.08f)
    }
    private fun button(s: String, bg: Int, fg: Int) = Button(this).apply {
        text = s; isAllCaps = false; textSize = 13.5f; setTextColor(fg); setTypeface(typeface, android.graphics.Typeface.BOLD); background = ThemeManager.rounded(bg, dp(18).toFloat())
    }
    private fun top(v: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(v) }
    private fun topHeight(v: Int, h: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(h)).apply { topMargin = dp(v) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun withAlpha(color: Int, a: Int) = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
}
