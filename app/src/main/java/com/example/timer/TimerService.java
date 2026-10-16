package com.example.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class TimerService extends Service {

    public static final String ACTION_UPDATE = "com.example.timer.TIMER_UPDATE";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_STATUS = "status";

    private static final String CHANNEL_ONGOING = "timer_ongoing";
    private static final String CHANNEL_ALERT   = "timer_alert";

    private static final int NOTIFICATION_ONGOING_ID = 1;
    private static final int NOTIFICATION_ALERT_ID   = 2;

    private MediaPlayer player;
    private Thread timerThread;
    private volatile boolean running = false;

    private String mode = "stopwatch";
    private int seconds = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        player = MediaPlayer.create(this, R.raw.shade);
        if (player != null) player.setLooping(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = false;
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }

        if (intent != null) {
            String newMode = intent.getStringExtra("mode");
            if (newMode != null) mode = newMode;
            seconds = intent.getIntExtra("seconds", 0);
        }

        Notification ongoing = buildOngoingNotification("Сервис запущен: " + formatTime(seconds));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ONGOING_ID, ongoing,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ONGOING_ID, ongoing);
        }

        sendUpdate(formatTime(seconds), "Работает");

        running = true;
        timerThread = new Thread(() -> {
            while (running) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }

                if (mode.equals("timer")) {
                    if (seconds > 0) {
                        seconds--;
                    } else {
                        sendUpdate("00:00", "Время вышло!");
                        showFinalNotification("Таймер завершён!", "Время вышло");
                        playSound();
                        running = false;
                        break;
                    }
                } else {
                    seconds++;
                }

                String time = formatTime(seconds);
                updateOngoingNotification(time);
                sendUpdate(time, "Работает");
            }
        });
        timerThread.start();

        return START_STICKY;
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            NotificationChannel ongoing = new NotificationChannel(
                    CHANNEL_ONGOING, "Работающий сервис",
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ongoing);

            NotificationChannel alert = new NotificationChannel(
                    CHANNEL_ALERT, "Оповещения таймера",
                    NotificationManager.IMPORTANCE_HIGH);
            alert.enableVibration(true);
            alert.setBypassDnd(true);
            nm.createNotificationChannel(alert);
        }
    }

    private Notification buildOngoingNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ONGOING)
                .setContentTitle("Таймер/Секундомер")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private Notification buildFinalNotification(String title, String text) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ALERT)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();
    }

    private void updateOngoingNotification(String time) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ONGOING_ID, buildOngoingNotification(time));
    }

    private void showFinalNotification(String title, String text) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.notify(NOTIFICATION_ALERT_ID, buildFinalNotification(title, text));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void playSound() {
        try {
            if (player != null && !player.isPlaying()) {
                player.start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendUpdate(String time, String status) {
        Intent intent = new Intent(ACTION_UPDATE);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_TIME, time);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public void onDestroy() {
        running = false;
        if (timerThread != null) timerThread.interrupt();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
        }
        sendUpdate("00:00", "Сервис остановлен");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}