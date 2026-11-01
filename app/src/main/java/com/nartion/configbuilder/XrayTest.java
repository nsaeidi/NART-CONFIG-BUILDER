package com.nartion.configbuilder;

import libXray.LibXray;
import org.json.JSONObject;
import org.json.JSONArray;

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

    public static String testVlessConversion(String link) {
        try {
            String response = XrayController.convertShareLink(link);

            JSONObject result = new JSONObject(response);

            if (!result.optBoolean("success", false)) {
                return "تبدیل ناموفق بود:\n" +
                        result.optString("error", "خطای نامشخص");
            }

            JSONObject data = result.optJSONObject("data");

            if (data == null) {
                return "پاسخ کتابخانه فاقد data است.";
            }

            JSONArray outbounds = data.optJSONArray("outbounds");

            if (outbounds == null || outbounds.length() == 0) {
                return "هیچ outbound معتبری تولید نشد.";
            }

            return "تبدیل VLESS موفق بود.\n" +
                    "تعداد outbound: " + outbounds.length();

        } catch (Exception e) {
            return "خطای تست تبدیل: " + e.getMessage();
        }
    }
}
