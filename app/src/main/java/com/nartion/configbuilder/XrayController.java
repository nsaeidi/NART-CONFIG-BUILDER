package com.nartion.configbuilder;

import libXray.LibXray;
import libXray.DialerController;
import android.net.VpnService;
import org.json.JSONObject;

public class XrayController {

    private static final int API_VERSION = 3;

    private static DialerController vpnDialerController;
    private static boolean controllerRegistered = false;
    private static volatile VpnService activeVpnService;

    public static synchronized boolean prepareVpnController(VpnService vpnService) {
        try {
            LibXray.touch();
            activeVpnService = vpnService;

            if (!controllerRegistered) {
                vpnDialerController = fd -> {
                    VpnService service = activeVpnService;
                    if (service == null || fd < 0 || fd > Integer.MAX_VALUE) {
                        return false;
                    }
                    return service.protect((int) fd);
                };

                LibXray.registerDialerController(vpnDialerController);
                controllerRegistered = true;
            }

            LibXray.setDNS(vpnDialerController, "1.1.1.1:53");
            return true;
        } catch (Exception e) {
            android.util.Log.e("NART_XRAY", "VPN controller setup failed", e);
            activeVpnService = null;
            return false;
        }
    }

    public static synchronized void resetVpnDns() {
        try {
            LibXray.resetDNS();
        } catch (Exception e) {
            android.util.Log.e("NART_XRAY", "DNS reset failed", e);
        }
        activeVpnService = null;
    }

    private static synchronized String invoke(String method, JSONObject payload) {
        try {
            LibXray.touch();

            JSONObject request = new JSONObject();
            request.put("apiVersion", API_VERSION);
            request.put("method", method);
            request.put("payload", payload);

            return LibXray.invoke(request.toString());

        } catch (Exception e) {
            return error(e);
        }
    }


    public static String getVersion() {
        return invoke("xrayVersion", new JSONObject());
    }

    public static String convertShareLink(String shareLink) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("text", shareLink);
            return invoke("convertShareLinksToXrayJson", payload);
        } catch (Exception e) {
            return error(e);
        }
    }

    public static String start(String xrayJson) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("xrayJson", xrayJson);
            return invoke("runXray", payload);
        } catch (Exception e) {
            return error(e);
        }
    }

    public static String stop() {
        return invoke("stopXray", new JSONObject());
    }

    public static String getState() {
        return invoke("getXrayState", new JSONObject());
    }

    public static boolean isRunning() {
        try {
            String response = getState();
            JSONObject result = new JSONObject(response);

            if (!result.optBoolean("success", false)) {
                return false;
            }

            JSONObject data = result.optJSONObject("data");
            return data != null && data.optBoolean("running", false);
        } catch (Exception e) {
            android.util.Log.e("NART_XRAY", "State check failed", e);
            return false;
        }
    }

    public static String test(String xrayJson) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("xrayJson", xrayJson);
            return invoke("testXray", payload);
        } catch (Exception e) {
            return error(e);
        }
    }

    public static boolean isSuccess(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                return false;
            }
            JSONObject result = new JSONObject(response);
            return result.optBoolean("success", false);
        } catch (Exception e) {
            android.util.Log.e("NART_XRAY", "Invalid response JSON", e);
            return false;
        }
    }

    private static String error(Exception e) {
        JSONObject result = new JSONObject();
        try {
            result.put("success", false);
            result.put("error", e.getMessage());
        } catch (Exception ignored) {
        }
        return result.toString();
    }
}
