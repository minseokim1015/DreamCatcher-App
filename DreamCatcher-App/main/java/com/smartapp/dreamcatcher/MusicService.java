package com.smartapp.dreamcatcher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import static com.smartapp.dreamcatcher.MainActivity.SERVER_HOST;

public class MusicService extends Service {
    PowerManager.WakeLock wakeLock;
    MediaPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 100, notificationIntent, 0);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "music_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        String musicTitle = intent.getStringExtra("title");
        String notiContent = "Click here to open the app.";
        if (musicTitle != null && !musicTitle.equals("")) {
            notiContent = String.format("Now playing %s", musicTitle);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Playing music")
                .setContentText(notiContent)
                .setContentIntent(pendingIntent);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag2");
        wakeLock.acquire();

        player = MediaPlayer.create(this, intent.getIntExtra("id", 0));
        player.setLooping(true);
        player.setVolume(1.0f, 1.0f);
        player.start();

        startForeground(2, builder.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        player.stop();
    }
}
