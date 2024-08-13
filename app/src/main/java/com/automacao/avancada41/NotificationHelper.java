package com.automacao.avancada41;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "location_updates_channel";
    private static final String CHANNEL_NAME = "Location Updates";
    private static final String CHANNEL_DESCRIPTION = "Notifications for location updates";

    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Ícone da notificação
                .setContentTitle(title) // Título da notificação
                .setContentText(message) // Mensagem da notificação
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridade alta para exibir a notificação de forma destacada
                .setAutoCancel(true) // Remove a notificação quando clicada
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Define os padrões para a notificação (som, vibração, etc.)

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }
}
