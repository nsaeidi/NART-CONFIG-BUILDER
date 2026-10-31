package com.nartion.configbuilder;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.content.Intent;
import android.util.Log;

public class NartVpnService extends VpnService {

    private static final String TAG = "NART_VPN";
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Builder builder = new Builder();
            builder.setSession("NART VPN");
            builder.addAddress("10.10.10.2", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");

            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                Log.e(TAG, "VPN interface creation failed");
                stopSelf();
                return START_NOT_STICKY;
            }

            Log.i(TAG, "TUN interface created");
        } catch (Exception e) {
            Log.e(TAG, "VPN service error", e);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        closeVpnInterface();
        super.onDestroy();
    }

    private void closeVpnInterface() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                Log.e(TAG, "TUN close error", e);
            }
            vpnInterface = null;
        }
    }
}
