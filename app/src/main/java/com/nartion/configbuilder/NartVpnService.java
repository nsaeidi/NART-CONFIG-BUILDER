package com.nartion.configbuilder;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.app.Notification;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NartVpnService extends VpnService {
    public static final String EXTRA_CONFIG = "com.nartion.configbuilder.EXTRA_CONFIG";
    public static final String ACTION_DISCONNECT = "com.nartion.configbuilder.DISCONNECT";

    private static final String TAG = "NART_VPN";
    private static final String PREFS = "NART_CONFIG_DATA";
    private static final String ACTIVE_CONFIG = "active_config";

    private ParcelFileDescriptor vpnInterface;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (ACTION_DISCONNECT.equals(intent.getAction())) {
            worker.execute(() -> stopTunnel(startId));
            return START_NOT_STICKY;
        }

        String shareLink = intent.getStringExtra(EXTRA_CONFIG);
        if (shareLink == null || shareLink.trim().isEmpty()) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        try {
            Notification notification = VpnNotification.build(this);
            startForeground(VpnNotification.NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Foreground start failed", e);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        worker.execute(() -> startTunnel(shareLink.trim(), startId));
        return START_NOT_STICKY;
    }

    private void startTunnel(String shareLink, int startId) {
        boolean xrayStartAttempted = false;

        try {
            stopXrayAndCloseTun();

            String physicalInterface =
                    NetworkInterfaceHelper.getActiveInterfaceName(this);

            if (physicalInterface == null || physicalInterface.trim().isEmpty()) {
                throw new IllegalStateException("رابط اینترنت فیزیکی پیدا نشد.");
            }

            if (!XrayController.prepareVpnController(this)) {
                throw new IllegalStateException("آماده‌سازی کنترل‌کننده Xray ناموفق بود.");
            }

            String converted = XrayController.convertShareLink(shareLink);
            if (!XrayController.isSuccess(converted)) {
                throw new IllegalStateException("تبدیل کانفیگ ناموفق بود: " + converted);
            }

            Builder builder = new Builder();
            builder.setSession("NART VPN");
            builder.setMtu(1500);

            builder.addAddress("10.10.10.2", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");

            builder.addAddress("fd00:4e41:5254::2", 128);
            builder.addRoute("::", 0);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                throw new IllegalStateException("ساخت رابط TUN ناموفق بود.");
            }

            String xrayJson = XrayConfigBuilder.build(
                    converted,
                    vpnInterface.getFd(),
                    physicalInterface
            );

            xrayStartAttempted = true;
            String response = XrayController.start(xrayJson);

            if (!XrayController.isSuccess(response) || !XrayController.isRunning()) {
                throw new IllegalStateException("اجرای Xray ناموفق بود: " + response);
            }

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(ACTIVE_CONFIG, shareLink)
                    .apply();

            Log.i(TAG, "Xray started and TUN is active.");

        } catch (Exception e) {
            Log.e(TAG, "VPN startup failed", e);

            if (xrayStartAttempted) {
                XrayController.stop();
            }

            closeVpnInterface();
            XrayController.resetVpnDns();

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .remove(ACTIVE_CONFIG)
                    .apply();

            stopForeground(true);
            stopSelf(startId);
        }
    }

    private void stopTunnel(int startId) {
        stopXrayAndCloseTun();

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .remove(ACTIVE_CONFIG)
                .apply();

        stopForeground(true);
        stopSelf(startId);
    }

    private void stopXrayAndCloseTun() {
        try {
            XrayController.stop();
        } catch (Exception e) {
            Log.e(TAG, "Xray stop failed", e);
        }

        closeVpnInterface();
        XrayController.resetVpnDns();
    }

    private void closeVpnInterface() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                Log.e(TAG, "TUN close failed", e);
            }
            vpnInterface = null;
        }
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        worker.execute(() -> stopXrayAndCloseTun());
        worker.shutdown();
        super.onDestroy();
        Log.i(TAG, "VPN service destroyed.");
    }
}
