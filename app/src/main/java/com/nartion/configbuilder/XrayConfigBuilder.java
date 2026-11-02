package com.nartion.configbuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public final class XrayConfigBuilder {
    private static final String TUN_TAG = "nart-tun";
    private static final String PROXY_TAG = "nart-proxy";

    private XrayConfigBuilder() {}

    public static String build(
            String convertedResponse,
            int tunFd,
            String physicalInterface
    ) throws Exception {

        if (convertedResponse == null || convertedResponse.trim().isEmpty())
            throw new IllegalArgumentException("پاسخ تبدیل کانفیگ خالی است.");

        if (tunFd < 0)
            throw new IllegalArgumentException("شماره TUN معتبر نیست.");

        if (physicalInterface == null || physicalInterface.trim().isEmpty())
            throw new IllegalArgumentException("رابط فیزیکی اینترنت شناسایی نشد.");

        JSONObject conversion = new JSONObject(convertedResponse);

        if (!conversion.optBoolean("success", false))
            throw new IllegalArgumentException(
                conversion.optString("error", "تبدیل ناموفق بود.")
            );

        JSONObject data = conversion.optJSONObject("data");
        if (data == null)
            throw new IllegalArgumentException("داده خروجی مبدل وجود ندارد.");

        JSONArray convertedOutbounds = data.optJSONArray("outbounds");

        if (convertedOutbounds == null || convertedOutbounds.length() == 0)
            throw new IllegalArgumentException(
                "هیچ سرور معتبری در اشتراک پیدا نشد."
            );

        JSONObject outbound = convertedOutbounds.getJSONObject(0);

        if (!outbound.has("protocol") || !outbound.has("settings"))
            throw new IllegalArgumentException("ساختار outbound ناقص است.");

        outbound.put("tag", PROXY_TAG);

        JSONObject tunSettings = new JSONObject();
        tunSettings.put("name", "nart0");
        tunSettings.put("mtu", 1500);
        tunSettings.put("autoOutboundsInterface", physicalInterface.trim());

        JSONObject tunInbound = new JSONObject();
        tunInbound.put("port", 0);
        tunInbound.put("protocol", "tun");
        tunInbound.put("tag", TUN_TAG);
        tunInbound.put("settings", tunSettings);

        JSONArray inbounds = new JSONArray();
        inbounds.put(tunInbound);

        JSONArray outbounds = new JSONArray();
        outbounds.put(outbound);

        JSONArray inboundTags = new JSONArray();
        inboundTags.put(TUN_TAG);

        JSONObject routeRule = new JSONObject();
        routeRule.put("inboundTag", inboundTags);
        routeRule.put("outboundTag", PROXY_TAG);

        JSONArray rules = new JSONArray();
        rules.put(routeRule);

        JSONObject routing = new JSONObject();
        routing.put("rules", rules);

        JSONObject env = new JSONObject();
        env.put("xray.tun.fd", String.valueOf(tunFd));

        JSONObject config = new JSONObject();
        config.put("env", env);
        config.put("inbounds", inbounds);
        config.put("outbounds", outbounds);
        config.put("routing", routing);

        return config.toString();
    }
}
