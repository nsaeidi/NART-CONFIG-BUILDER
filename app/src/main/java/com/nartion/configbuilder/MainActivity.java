package com.nartion.configbuilder;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends Activity {

    private LinearLayout listLayout;
    private TextView countText;
    private EditText input;

    private ArrayList<String> configs = new ArrayList<>();
    private HashSet<String> unique = new HashSet<>();

    private SharedPreferences preferences;

    private int navy = Color.rgb(20, 40, 80);
    private int blue = Color.rgb(38, 105, 190);
    private int background = Color.rgb(244, 247, 252);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                "NART_CONFIG_DATA", MODE_PRIVATE);

        loadConfigs();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(25), dp(18), dp(25));
        root.setBackgroundColor(background);

        scroll.addView(root);

        TextView title = text("NART CONFIG BUILDER", 23, navy, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = text(
                "مدیریت هوشمند کانفیگ‌های VPN",
                15, Color.DKGRAY, false);
        subtitle.setGravity(Gravity.CENTER);
        add(root, subtitle, 0, 5, 0, 20);

        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(dp(15), dp(18), dp(15), dp(18));
        stats.setBackground(round(navy, 18));

        countText = text(
                "تعداد کانفیگ‌ها: " + configs.size(),
                19, Color.WHITE, true);
        countText.setGravity(Gravity.CENTER);
        stats.addView(countText);
        root.addView(stats);

        TextView inputTitle = text(
                "افزودن کانفیگ", 19, navy, true);
        add(root, inputTitle, 0, 25, 0, 10);

        input = new EditText(this);
        input.setHint("لینک کانفیگ را اینجا وارد یا Paste کنید...");
        input.setTextSize(14);
        input.setGravity(Gravity.TOP | Gravity.RIGHT);
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setBackground(round(Color.WHITE, 14));

        root.addView(input, new LinearLayout.LayoutParams(
                -1, dp(135)));

        Button addButton = button("＋ افزودن کانفیگ", blue);
        add(root, addButton, 0, 12, 0, 8);
        addButton.setOnClickListener(v -> addConfigs());

        Button clearInput = button(
                "پاک کردن کادر ورودی", Color.DKGRAY);
        root.addView(clearInput);
        clearInput.setOnClickListener(v -> input.setText(""));

        TextView listTitle = text(
                "فهرست کانفیگ‌ها", 19, navy, true);
        add(root, listTitle, 0, 28, 0, 12);

        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listLayout);

        TextView footer = text(
                "NART CONFIG BUILDER • Version 3.0",
                11, Color.GRAY, false);
        footer.setGravity(Gravity.CENTER);
        add(root, footer, 0, 30, 0, 10);

        setContentView(scroll);
        refreshList();
    }

    private void loadConfigs() {
        configs.clear();
        unique.clear();

        String saved = preferences.getString("configs", "[]");

        try {
            JSONArray array = new JSONArray(saved);

            for (int i = 0; i < array.length(); i++) {
                String config = array.getString(i);

                if (isValid(config) && unique.add(config)) {
                    configs.add(config);
                }
            }

        } catch (Exception e) {
            configs.clear();
            unique.clear();
        }
    }

    private void saveConfigs() {
        JSONArray array = new JSONArray();

        for (String config : configs) {
            array.put(config);
        }

        preferences.edit()
                .putString("configs", array.toString())
                .apply();
    }

    private void addConfigs() {
        String raw = input.getText().toString().trim();

        if (raw.isEmpty()) {
            Toast.makeText(this,
                    "لطفاً لینک کانفیگ را وارد کنید",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] lines = raw.split("\\r?\\n");

        int added = 0;
        int duplicate = 0;
        int invalid = 0;

        for (String line : lines) {
            String config = line.trim();

            if (config.isEmpty()) continue;

            if (!isValid(config)) {
                invalid++;
                continue;
            }

            if (unique.contains(config)) {
                duplicate++;
                continue;
            }

            unique.add(config);
            configs.add(config);
            added++;
        }

        saveConfigs();
        refreshList();
        input.setText("");

        Toast.makeText(this,
                "افزوده شد: " + added +
                " | تکراری: " + duplicate +
                " | نامعتبر: " + invalid,
                Toast.LENGTH_LONG).show();
    }

    private boolean isValid(String s) {
        String lower = s.toLowerCase();

        return lower.startsWith("vless://") ||
               lower.startsWith("vmess://") ||
               lower.startsWith("trojan://") ||
               lower.startsWith("ss://");
    }

    private String getType(String s) {
        String lower = s.toLowerCase();

        if (lower.startsWith("vless://")) return "VLESS";
        if (lower.startsWith("vmess://")) return "VMess";
        if (lower.startsWith("trojan://")) return "TROJAN";
        if (lower.startsWith("ss://")) return "SHADOWSOCKS";

        return "UNKNOWN";
    }

    private void refreshList() {
        listLayout.removeAllViews();

        countText.setText(
                "تعداد کانفیگ‌ها: " + configs.size());

        if (configs.isEmpty()) {
            TextView empty = text(
                    "هنوز کانفیگی اضافه نشده است.\n" +
                    "لینک خود را در کادر بالا وارد کنید.",
                    14, Color.GRAY, false);

            empty.setGravity(Gravity.CENTER);
            empty.setPadding(10, dp(25), 10, dp(25));
            listLayout.addView(empty);
            return;
        }

        for (int i = 0; i < configs.size(); i++) {
            final String config = configs.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(
                    dp(14), dp(14), dp(14), dp(14));
            card.setBackground(round(Color.WHITE, 14));

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(-1, -2);

            cardParams.setMargins(0, 0, 0, dp(12));
            listLayout.addView(card, cardParams);

            TextView name = text(
                    "کانفیگ " + (i + 1) +
                    " • " + getType(config),
                    16, navy, true);

            card.addView(name);

            TextView preview = text(
                    config, 12, Color.DKGRAY, false);

            preview.setMaxLines(2);
            preview.setTextDirection(
                    View.TEXT_DIRECTION_LTR);
            preview.setGravity(Gravity.LEFT);

            add(card, preview, 0, 8, 0, 10);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button copy = button("کپی", blue);
            Button delete = button(
                    "حذف", Color.rgb(190, 55, 55));

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(
                            0, dp(43), 1);

            p.setMargins(0, 0, dp(6), 0);
            actions.addView(copy, p);

            LinearLayout.LayoutParams p2 =
                    new LinearLayout.LayoutParams(
                            0, dp(43), 1);

            p2.setMargins(dp(6), 0, 0, 0);
            actions.addView(delete, p2);

            card.addView(actions);

            copy.setOnClickListener(v -> {
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(
                                Context.CLIPBOARD_SERVICE);

                clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                                "VPN Config", config));

                Toast.makeText(this,
                        "کانفیگ کپی شد",
                        Toast.LENGTH_SHORT).show();
            });

            delete.setOnClickListener(v -> {
                unique.remove(config);
                configs.remove(config);

                saveConfigs();
                refreshList();

                Toast.makeText(this,
                        "کانفیگ حذف شد",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private TextView text(
            String value, int size,
            int color, boolean bold) {

        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(null, Typeface.BOLD);
        }

        return t;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackground(round(color, 12));
        return b;
    }

    private GradientDrawable round(
            int color, int radius) {

        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void add(
            LinearLayout parent, View child,
            int left, int top, int right, int bottom) {

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(
                dp(left), dp(top),
                dp(right), dp(bottom));

        parent.addView(child, p);
    }

    private int dp(int value) {
        return (int) (
                value *
                getResources().getDisplayMetrics().density
                + 0.5f);
    }
}
