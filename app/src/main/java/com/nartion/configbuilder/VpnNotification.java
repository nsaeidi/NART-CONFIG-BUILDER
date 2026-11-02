package com.nartion.configbuilder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class VpnNotification {

    public static final String CHANNEL_ID = "nart_vpn_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "NART VPN",
                NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("وضعیت اتصال VPN");
            channel.setShowBadge(false);

            NotificationManager manager =
                context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static Notification build(Context context) {
        createChannel(context);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        return builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NART VPN")
            .setContentText("سرویس VPN فعال است")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build();
    }
}
