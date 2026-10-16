package com.example.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView textViewTimer;
    private TextView textViewStatus;

    private final BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String time = intent.getStringExtra(TimerService.EXTRA_TIME);
            String status = intent.getStringExtra(TimerService.EXTRA_STATUS);
            if (time != null) textViewTimer.setText(time);
            if (status != null) textViewStatus.setText(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTimer = findViewById(R.id.textViewTimer);
        textViewStatus = findViewById(R.id.textViewStatus);

        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(TimerService.ACTION_UPDATE);
        ContextCompat.registerReceiver(
                this,
                timerReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(timerReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void startMyService(Intent i) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    public void startTimer(View v) {
        Intent i = new Intent(this, TimerService.class);
        i.putExtra("mode", "timer");
        i.putExtra("seconds", 10);
        startMyService(i);
    }

    public void startStopwatch(View v) {
        Intent i = new Intent(this, TimerService.class);
        i.putExtra("mode", "stopwatch");
        i.putExtra("seconds", 0);
        startMyService(i);
    }

    public void stopServiceClick(View v) {
        stopService(new Intent(this, TimerService.class));
        textViewTimer.setText("00:00");
        textViewStatus.setText("Сервис остановлен");
    }
}