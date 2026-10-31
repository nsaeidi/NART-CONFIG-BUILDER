package com.nartion.configbuilder;

import libXray.LibXray;
import org.json.JSONObject;

public class XrayTest {

    public static String getVersion() {
        try {
            JSONObject request = new JSONObject();
            request.put("apiVersion", 3);
            request.put("method", "xrayVersion");
            request.put("payload", new JSONObject());

            LibXray.touch();

            return LibXray.invoke(request.toString());

        } catch (Exception e) {
            return "XRAY ERROR: " + e.getMessage();
        }
    }
}
