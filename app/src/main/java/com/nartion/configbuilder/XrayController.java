package com.nartion.configbuilder;

import libXray.LibXray;
import org.json.JSONObject;

public class XrayController {

    private static final int API_VERSION = 3;

    private static String invoke(String method, JSONObject payload) {
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

    public static String test(String xrayJson) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("xrayJson", xrayJson);
            return invoke("testXray", payload);
        } catch (Exception e) {
            return error(e);
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
