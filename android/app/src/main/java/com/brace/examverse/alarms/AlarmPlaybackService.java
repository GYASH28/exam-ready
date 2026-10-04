package com.brace.examverse.alarms;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.net.Uri;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.brace.examverse.R;

/** Ring independently of the activity, including when the display is locked. */
public class AlarmPlaybackService extends Service {
    public static final String RING = "com.brace.examverse.RING";
    public static final String DISMISS = "com.brace.examverse.DISMISS";
    public static final String SNOOZE = "com.brace.examverse.SNOOZE";
    public static final String CLOSED = "com.brace.examverse.ALARM_CLOSED";
    public static final String CHANNEL = "examverse_ringing_v5";
    private static final int NOTIFICATION = 501;
    private volatile MediaPlayer player;
    private static volatile AlarmPlaybackService instance;
    public static boolean isRinging(){AlarmPlaybackService current=instance;MediaPlayer playing=current==null?null:current.player;try{return playing!=null&&playing.isPlaying();}catch(IllegalStateException stopped){return false;}}
    @Override public void onCreate(){super.onCreate();instance=this;}
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private AudioFocusRequest audioFocus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long activeId = -1;
    private final Runnable timeout = this::close;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        long id = intent.getLongExtra("alarm_id", -1);
        String action = intent.getAction();
        if (DISMISS.equals(action) || SNOOZE.equals(action)) {
            if (id == activeId) {
                if (SNOOZE.equals(action)) CustomAlarmScheduler.snooze(this, id);
                close();
            }
            if(activeId==-1)stopSelf();
            return START_NOT_STICKY;
        }
        CustomAlarm alarm = new CustomAlarmRepository(this).get(id);
        if (alarm == null) { if (activeId == -1) stopSelf(); return START_NOT_STICKY; }
        if (activeId == id) return START_NOT_STICKY;
        releasePlayback();
        if (activeId != -1) notifyClosed(activeId);
        activeId = id;
        Notification notification = notification(alarm);
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION, notification);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ExamVerse:alarm");
        wakeLock.acquire(15 * 60_000L + 5000);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioFocus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change -> {}).build();
        audio.requestAudioFocus(audioFocus);
        Uri selected = alarm.soundUri.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : Uri.parse(alarm.soundUri);
        if (!play(selected, attrs)) {
            if (!play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), attrs)) play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attrs);
        }
        if (alarm.vibrate) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 600, 250, 600, 250, 900}, 0));
        }
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, 15 * 60_000L);
        return START_NOT_STICKY;
    }

    private boolean play(Uri uri, AudioAttributes attrs) {
        if (uri == null) return false;
        MediaPlayer candidate = new MediaPlayer();
        try {
            candidate.setAudioAttributes(attrs);
            candidate.setDataSource(this, uri);
            candidate.setLooping(true);
            candidate.setOnErrorListener((mp, what, extra) -> { close(); return true; });
            candidate.prepare();
            candidate.start();
            player = candidate;
            return true;
        } catch (Exception error) { candidate.release(); return false; }
    }

    private Notification notification(CustomAlarm alarm) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Alarm controls. The alarm service plays the selected sound.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
        Intent screen = new Intent(this, AlarmRingingActivity.class).putExtra("alarm_id", alarm.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full = PendingIntent.getActivity(this, 501, screen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_app)
                .setContentTitle(alarm.label).setContentText("Alarm ringing · snooze or dismiss")
                .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true).setAutoCancel(false).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(full).setFullScreenIntent(full, true)
                .addAction(0, "Snooze 10m", control(this, alarm.id, SNOOZE))
                .addAction(0, "Dismiss", control(this, alarm.id, DISMISS)).build();
    }

    public static PendingIntent control(Context c, long id, String action) {
        Intent i = new Intent(c, AlarmPlaybackService.class).setAction(action).putExtra("alarm_id", id);
        return PendingIntent.getService(c, (int)(id ^ (id >>> 32)), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private void notifyClosed(long id) { sendBroadcast(new Intent(CLOSED).setPackage(getPackageName()).putExtra("alarm_id", id)); }
    private void close() {
        notifyClosed(activeId);
        activeId = -1;
        releasePlayback();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
    private void releasePlayback() {
        handler.removeCallbacks(timeout);
        if (player != null) { player.release(); player = null; }
        if (vibrator != null) { vibrator.cancel(); vibrator = null; }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        if (audioFocus != null) ((AudioManager)getSystemService(AUDIO_SERVICE)).abandonAudioFocusRequest(audioFocus);
        audioFocus = null;
    }
    @Override public void onDestroy() { releasePlayback(); instance=null;super.onDestroy(); }
}
