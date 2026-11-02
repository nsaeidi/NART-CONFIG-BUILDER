package com.nartion.configbuilder;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkInterfaceHelper {

    public static String getActiveInterfaceName(Context context) {
        try {
            ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                );

            if (cm == null) return null;

            Network active = cm.getActiveNetwork();

            if (active != null) {
                NetworkCapabilities caps =
                    cm.getNetworkCapabilities(active);

                if (caps != null &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {

                    LinkProperties lp = cm.getLinkProperties(active);

                    if (lp != null && lp.getInterfaceName() != null) {
                        return lp.getInterfaceName();
                    }
                }
            }

            for (Network network : cm.getAllNetworks()) {
                NetworkCapabilities caps =
                    cm.getNetworkCapabilities(network);

                if (caps == null ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                    !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    continue;
                }

                LinkProperties lp = cm.getLinkProperties(network);

                if (lp != null && lp.getInterfaceName() != null) {
                    return lp.getInterfaceName();
                }
            }

        } catch (Exception e) {
            android.util.Log.e("NART_NETWORK", "Interface detection failed", e);
        }

        return null;
    }
}
