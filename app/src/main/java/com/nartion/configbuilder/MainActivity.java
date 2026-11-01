package com.nartion.configbuilder;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.VpnService;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;

import org.json.JSONArray;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int PICK_FILE = 1001;
    private static final int SAVE_JSON_FILE = 1003;
    private static final int VPN_PERMISSION = 1002;

    private LinearLayout listLayout;
    private TextView countText;
    private EditText input;
    private EditText searchInput;
    private Spinner filterSpinner;
    private Spinner sortSpinner;
    private CheckBox selectAllBox;
    private Button deleteSelectedButton;

    private ArrayList<String> configs = new ArrayList<>();
    private HashSet<String> unique = new HashSet<>();
    private HashSet<String> selected = new HashSet<>();
    private String activeConfig = null;
    private String pendingConfig = null;

    private SharedPreferences preferences;
    private boolean updatingSelectAll = false;

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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(25), dp(18), dp(25));
        root.setBackgroundColor(background);

        scroll.addView(root);

        TextView title = text(
                "NART CONFIG BUILDER", 23, navy, true);
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

        countText = text("", 17, Color.WHITE, true);
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

        Button fileButton = button(
                "📁 انتخاب فایل TXT و ورود گروهی", navy);
        add(root, fileButton, 0, 12, 0, 8);
        fileButton.setOnClickListener(v -> openFilePicker());

        TextView listTitle = text(
                "جست‌وجو و مدیریت کانفیگ‌ها", 19, navy, true);
        add(root, listTitle, 0, 28, 0, 10);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setHint("جست‌وجو در لینک‌ها یا نوع کانفیگ...");
        searchInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        searchInput.setBackground(round(Color.WHITE, 12));
        root.addView(searchInput, new LinearLayout.LayoutParams(
                -1, dp(50)));

        TextView filterLabel = text(
                "فیلتر نوع کانفیگ", 14, navy, true);
        add(root, filterLabel, 0, 14, 0, 4);

        filterSpinner = new Spinner(this);
        String[] filters = {
                "همه کانفیگ‌ها",
                "VLESS",
                "VMess",
                "TROJAN",
                "SHADOWSOCKS"
        };
        filterSpinner.setAdapter(new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item,
                filters));
        root.addView(filterSpinner);

        TextView sortLabel = text(
                "مرتب‌سازی", 14, navy, true);
        add(root, sortLabel, 0, 12, 0, 4);

        sortSpinner = new Spinner(this);
        String[] sorts = {
                "جدیدترین",
                "قدیمی‌ترین",
                "بر اساس نوع"
        };
        sortSpinner.setAdapter(new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item,
                sorts));
        root.addView(sortSpinner);

        selectAllBox = new CheckBox(this);
        selectAllBox.setText("انتخاب همه موارد نمایش‌داده‌شده");
        selectAllBox.setTextColor(navy);
        add(root, selectAllBox, 0, 12, 0, 4);

        deleteSelectedButton = button(
                "حذف کانفیگ‌های انتخاب‌شده", Color.rgb(190, 55, 55));
        root.addView(deleteSelectedButton);
        deleteSelectedButton.setEnabled(false);
        deleteSelectedButton.setAlpha(0.5f);
        deleteSelectedButton.setOnClickListener(
                v -> confirmDeleteSelected());

        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        add(root, listLayout, 0, 15, 0, 0);

        Button xrayTestButton = button("آزمایش هسته Xray", navy);
        add(root, xrayTestButton, 0, 20, 0, 10);
        xrayTestButton.setOnClickListener(v -> {
            xrayTestButton.setEnabled(false);
            xrayTestButton.setText("در حال آزمایش...");
            new Thread(() -> {
                String result = XrayTest.getVersion();
                runOnUiThread(() -> {
                    xrayTestButton.setEnabled(true);
                    xrayTestButton.setText("آزمایش هسته Xray");
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("نتیجه آزمایش Xray")
                        .setMessage(result)
                        .setPositiveButton("باشه", null)
                        .show();
                });
            }).start();
        });

        Button vlessTestButton = button("آزمایش تبدیل VLESS", blue);
        add(root, vlessTestButton, 0, 8, 0, 10);

        vlessTestButton.setOnClickListener(v -> {
            String selectedVless = null;

            for (String item : configs) {
                if ("VLESS".equals(getType(item))) {
                    selectedVless = item;
                    break;
                }
            }

            if (selectedVless == null) {
                Toast.makeText(this,
                        "هیچ کانفیگ VLESS ذخیره نشده است",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            final String testLink = selectedVless;
            vlessTestButton.setEnabled(false);
            vlessTestButton.setText("در حال تبدیل...");

            new Thread(() -> {
                String result =
                        XrayTest.testVlessConversion(testLink);

                runOnUiThread(() -> {
                    vlessTestButton.setEnabled(true);
                    vlessTestButton.setText("آزمایش تبدیل VLESS");

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("نتیجه آزمایش تبدیل")
                            .setMessage(result)
                            .setNeutralButton("ذخیره JSON", (dialog, which) -> {
                                if (result.contains("JSON خروجی:")) {
                                    String json = result.substring(
                                            result.indexOf("JSON خروجی:") + "JSON خروجی:".length()
                                    ).trim();
                                    saveJsonFile(json);
                                } else {
                                    Toast.makeText(MainActivity.this,
                                            "JSON معتبری برای ذخیره وجود ندارد.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setPositiveButton("باشه", null)
                            .show();
                });
            }).start();
        });

        TextView footer = text(
                "NART CONFIG BUILDER • Version 5.0",
                11, Color.GRAY, false);
        footer.setGravity(Gravity.CENTER);
        add(root, footer, 0, 30, 0, 10);

        setContentView(scroll);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                refreshList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        filterSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view,
                    int position, long id) {
                refreshList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sortSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view,
                    int position, long id) {
                refreshList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        selectAllBox.setOnClickListener(v -> {
            if (updatingSelectAll) return;

            ArrayList<String> visible = getVisibleConfigs();

            if (selectAllBox.isChecked()) {
                selected.addAll(visible);
            } else {
                selected.removeAll(visible);
            }

            refreshList();
        });

        refreshList();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        try {
            startActivityForResult(intent, PICK_FILE);
        } catch (Exception e) {
            Toast.makeText(this,
                    "باز کردن فایل امکان‌پذیر نیست",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_PERMISSION) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "مجوز VPN تأیید شد",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "مجوز VPN رد شد",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == PICK_FILE &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {
            importTextFile(data.getData());
        }

        if (requestCode == SAVE_JSON_FILE &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {
            try {
                if (pendingJson == null) {
                    throw new Exception("JSON برای ذخیره وجود ندارد.");
                }
                java.io.OutputStream stream =
                        getContentResolver().openOutputStream(data.getData());
                if (stream == null) {
                    throw new Exception("فایل قابل نوشتن نیست.");
                }
                try (java.io.OutputStream out = stream) {
                    out.write(pendingJson.getBytes(StandardCharsets.UTF_8));
                }
                pendingJson = null;
                Toast.makeText(this, "فایل JSON ذخیره شد.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "خطای ذخیره: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void importTextFile(Uri uri) {
        int added = 0;
        int duplicate = 0;

        try {
            InputStream stream =
                    getContentResolver().openInputStream(uri);

            if (stream == null) {
                throw new Exception("File unavailable");
            }

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int length;

            while ((length = stream.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }

            stream.close();

            String content = new String(
                    output.toByteArray(),
                    StandardCharsets.UTF_8);

            Pattern pattern = Pattern.compile(
                    "(?i)(vless|vmess|trojan|ss)://[^\\s\"'<>]+");

            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String config = matcher.group().trim();
                config = config.replaceAll("[,.)}\\]]+$", "");

                if (!isValid(config)) continue;

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

            Toast.makeText(this,
                    "ورود فایل انجام شد\nجدید: " + added +
                    " | تکراری: " + duplicate,
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this,
                    "خطا در خواندن فایل: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
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
        if (s == null) return false;

        String lower = s.toLowerCase(Locale.ROOT);

        return lower.startsWith("vless://") ||
                lower.startsWith("vmess://") ||
                lower.startsWith("trojan://") ||
                lower.startsWith("ss://");
    }

    private String getType(String s) {
        String lower = s.toLowerCase(Locale.ROOT);

        if (lower.startsWith("vless://")) return "VLESS";
        if (lower.startsWith("vmess://")) return "VMess";
        if (lower.startsWith("trojan://")) return "TROJAN";
        if (lower.startsWith("ss://")) return "SHADOWSOCKS";

        return "UNKNOWN";
    }

    private ArrayList<String> getVisibleConfigs() {
        ArrayList<String> result = new ArrayList<>();

        String query = searchInput == null ? "" :
                searchInput.getText().toString()
                        .trim().toLowerCase(Locale.ROOT);

        String filter = filterSpinner == null ? "همه کانفیگ‌ها" :
                filterSpinner.getSelectedItem().toString();

        for (String config : configs) {
            String type = getType(config);

            if (!filter.equals("همه کانفیگ‌ها") &&
                    !type.equalsIgnoreCase(filter)) {
                continue;
            }

            if (!query.isEmpty() &&
                    !config.toLowerCase(Locale.ROOT).contains(query) &&
                    !type.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }

            result.add(config);
        }

        int sortPosition = sortSpinner == null ? 0 :
                sortSpinner.getSelectedItemPosition();

        if (sortPosition == 1) {
            Collections.reverse(result);
        } else if (sortPosition == 2) {
            Collections.sort(result, new Comparator<String>() {
                @Override
                public int compare(String a, String b) {
                    return getType(a).compareToIgnoreCase(getType(b));
                }
            });
        }

        return result;
    }

    private void refreshList() {
        if (listLayout == null) return;

        listLayout.removeAllViews();

        ArrayList<String> visible = getVisibleConfigs();

        countText.setText(
                "کل کانفیگ‌ها: " + configs.size() +
                "  |  نمایش: " + visible.size() +
                "  |  انتخاب‌شده: " + selected.size());

        deleteSelectedButton.setEnabled(!selected.isEmpty());
        deleteSelectedButton.setAlpha(
                selected.isEmpty() ? 0.5f : 1f);

        updatingSelectAll = true;

        boolean allSelected = !visible.isEmpty() &&
                selected.containsAll(visible);

        selectAllBox.setChecked(allSelected);
        selectAllBox.setEnabled(!visible.isEmpty());

        updatingSelectAll = false;

        if (visible.isEmpty()) {
            TextView empty = text(
                    configs.isEmpty()
                    ? "هنوز کانفیگی اضافه نشده است."
                    : "موردی با این جست‌وجو یا فیلتر پیدا نشد.",
                    14, Color.GRAY, false);

            empty.setGravity(Gravity.CENTER);
            empty.setPadding(10, dp(25), 10, dp(25));
            listLayout.addView(empty);
            return;
        }

        for (int i = 0; i < visible.size(); i++) {
            final String config = visible.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(
                    dp(14), dp(12), dp(14), dp(14));
            card.setBackground(round(Color.WHITE, 14));

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(-1, -2);

            cardParams.setMargins(0, 0, 0, dp(12));
            listLayout.addView(card, cardParams);

            LinearLayout heading = new LinearLayout(this);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox check = new CheckBox(this);
            check.setChecked(selected.contains(config));
            heading.addView(check);

            TextView name = text(
                    "کانفیگ " + (i + 1) +
                    " • " + getType(config),
                    15, navy, true);

            heading.addView(name, new LinearLayout.LayoutParams(
                    0, -2, 1));

            card.addView(heading);

            check.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    selected.add(config);
                } else {
                    selected.remove(config);
                }
                refreshList();
            });

            TextView preview = text(
                    config, 12, Color.DKGRAY, false);

            preview.setMaxLines(2);
            preview.setTextDirection(View.TEXT_DIRECTION_LTR);
            preview.setGravity(Gravity.LEFT);

            add(card, preview, 0, 6, 0, 10);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button connect = button(
                    config.equals(activeConfig) ? "قطع اتصال" : "اتصال",
                    config.equals(activeConfig) ? Color.rgb(190, 55, 55) : Color.rgb(35, 145, 85));

            Button copy = button("کپی", blue);
            Button delete = button(
                    "حذف", Color.rgb(190, 55, 55));

            LinearLayout.LayoutParams connectParams =
                    new LinearLayout.LayoutParams(0, dp(43), 1);
            connectParams.setMargins(0, 0, dp(6), 0);
            actions.addView(connect, connectParams);

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, dp(43), 1);

            p.setMargins(0, 0, dp(6), 0);
            actions.addView(copy, p);

            LinearLayout.LayoutParams p2 =
                    new LinearLayout.LayoutParams(0, dp(43), 1);

            p2.setMargins(dp(6), 0, 0, 0);
            actions.addView(delete, p2);

            card.addView(actions);

            connect.setOnClickListener(v -> {
                Intent vpnIntent = VpnService.prepare(this);

                if (vpnIntent != null) {
                    startActivityForResult(vpnIntent, VPN_PERMISSION);
                } else {
                    Toast.makeText(this,
                            "مجوز VPN قبلاً تأیید شده است",
                            Toast.LENGTH_SHORT).show();
                }
            });

            copy.setOnClickListener(v -> {
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(
                                Context.CLIPBOARD_SERVICE);

                clipboard.setPrimaryClip(
                        ClipData.newPlainText("VPN Config", config));

                Toast.makeText(this,
                        "کانفیگ کپی شد",
                        Toast.LENGTH_SHORT).show();
            });

            delete.setOnClickListener(v -> {
                unique.remove(config);
                configs.remove(config);
                selected.remove(config);

                saveConfigs();
                refreshList();

                Toast.makeText(this,
                        "کانفیگ حذف شد",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void confirmDeleteSelected() {
        if (selected.isEmpty()) {
            Toast.makeText(this,
                    "ابتدا کانفیگ‌ها را انتخاب کن",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("حذف گروهی")
                .setMessage(
                        "آیا از حذف " + selected.size() +
                        " کانفیگ انتخاب‌شده مطمئن هستی؟")
                .setNegativeButton("انصراف", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("حذف", (dialog, which) -> {
                    int removed = 0;

                    for (String config :
                            new ArrayList<>(selected)) {
                        if (configs.remove(config)) {
                            unique.remove(config);
                            removed++;
                        }
                    }

                    selected.clear();
                    saveConfigs();
                    refreshList();

                    Toast.makeText(this,
                            removed + " کانفیگ حذف شد",
                            Toast.LENGTH_LONG).show();
                })
                .show();
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

    private String pendingJson;


    private void saveJsonFile(String json) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "NART-VLESS-OUTBOUND.txt");
        pendingJson = json;
        startActivityForResult(intent, SAVE_JSON_FILE);
    }

    private int dp(int value) {
        return (int) (
                value *
                getResources().getDisplayMetrics().density
                + 0.5f);
    }
}
