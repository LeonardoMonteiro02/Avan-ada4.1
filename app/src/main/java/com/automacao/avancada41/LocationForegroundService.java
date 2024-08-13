package com.automacao.avancada41;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class LocationForegroundService extends Service implements LocationCallbackListener {
    public static final String CHANNEL_ID = "LocationServiceChannel";
    private LocationManager customLocationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        customLocationManager = new LocationManager(this);
        customLocationManager.setLocationCallbackListener(this);
        customLocationManager.startLocationUpdatesInBackground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Serviço de Localização")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent);
        startForeground(1, notification.build());

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        customLocationManager.stopLocationUpdates();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Exemplo de Serviço de Localização",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onNewLocationReceived(Location location) {
        Intent intent = new Intent("LOCATION_UPDATE");
        intent.putExtra("location", location);
        sendBroadcast(intent);
    }
}
