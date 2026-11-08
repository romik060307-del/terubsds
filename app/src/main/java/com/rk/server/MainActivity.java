package com.rk.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(244, 247, 246);
    private static final int INK = Color.rgb(24, 43, 42);
    private static final int MUTED = Color.rgb(99, 116, 113);
    private static final int GREEN = Color.rgb(36, 122, 102);
    private static final int WHITE = Color.WHITE;

    private TextView statusText;
    private TextView addressText;
    private TextView detailText;
    private Button startButton;
    private Button stopButton;
    private Button inboxButton;
    private long inboxFileModified = Long.MIN_VALUE;
    private final Handler handler = new Handler();
    private boolean foreground;

    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refreshStatus();
            refreshInboxBadge();
            if (foreground) handler.postDelayed(this, 1000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (android.os.Build.VERSION.SDK_INT >= 21) getWindow().setStatusBarColor(Color.rgb(16, 45, 43));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(buildLayout());
        refreshStatus();
        refreshInboxBadge();
    }

    private View buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(24));
        scroll.addView(root);

        TextView eyebrow = new TextView(this);
        eyebrow.setText("OHLI  /  SOCIAL SERVER");
        eyebrow.setTextColor(GREEN);
        eyebrow.setTextSize(12);
        if (android.os.Build.VERSION.SDK_INT >= 21) eyebrow.setLetterSpacing(0.08f);
        root.addView(eyebrow, matchWrap());

        TextView title = new TextView(this);
        title.setText("OHLI Social Server");
        title.setTextColor(INK);
        title.setTextSize(30);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(7);
        root.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Host OHLI Social untuk chat, feed, Story, media, LIVE, dan verifikasi OTP. Permintaan login/daftar akan muncul sebagai notifikasi pribadi berisi nomor dan kode OTP; teruskan kode secara manual melalui WhatsApp. Akses dari luar jaringan wajib memakai HTTPS tunnel tepercaya.");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(15);
        subtitle.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams subParams = matchWrap();
        subParams.topMargin = dp(8);
        root.addView(subtitle, subParams);

        inboxButton = makeButton("🔔 PUSAT NOTIFIKASI (0)", Color.rgb(229, 239, 235), INK);
        LinearLayout.LayoutParams inboxParams = matchWrap();
        inboxParams.topMargin = dp(16);
        root.addView(inboxButton, inboxParams);
        inboxButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showServerInbox(); }
        });

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundColor(WHITE);
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.topMargin = dp(22);
        root.addView(card, cardParams);

        statusText = new TextView(this);
        statusText.setText("SERVER MATI");
        statusText.setTextColor(MUTED);
        statusText.setTextSize(12);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(statusText, matchWrap());

        addressText = new TextView(this);
        addressText.setText("Belum ada alamat server");
        addressText.setTextColor(INK);
        addressText.setTextSize(18);
        addressText.setTypeface(null, android.graphics.Typeface.BOLD);
        addressText.setTextIsSelectable(true);
        LinearLayout.LayoutParams addressParams = matchWrap();
        addressParams.topMargin = dp(9);
        card.addView(addressText, addressParams);

        detailText = new TextView(this);
        detailText.setText("Port 8080 · web dan data chat berjalan dari tablet ini");
        detailText.setTextColor(MUTED);
        detailText.setTextSize(12);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.topMargin = dp(8);
        card.addView(detailText, detailParams);

        startButton = makeButton("MULAI SERVER", GREEN, WHITE);
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = dp(18);
        card.addView(startButton, startParams);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ServerService.class);
                intent.setAction(ServerService.ACTION_START);
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
                Toast.makeText(MainActivity.this, "Server sedang dinyalakan…", Toast.LENGTH_SHORT).show();
                handler.postDelayed(new Runnable() { @Override public void run() { refreshStatus(); } }, 800L);
            }
        });

        stopButton = makeButton("MATIKAN SERVER", Color.rgb(232, 238, 236), INK);
        LinearLayout.LayoutParams stopParams = matchWrap();
        stopParams.topMargin = dp(9);
        card.addView(stopButton, stopParams);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ServerService.class);
                intent.setAction(ServerService.ACTION_STOP);
                if (android.os.Build.VERSION.SDK_INT >= 26 && ServerService.isRunning) startForegroundService(intent); else startService(intent);
                refreshStatus();
                Toast.makeText(MainActivity.this, "Permintaan mematikan server dikirim", Toast.LENGTH_SHORT).show();
            }
        });

        Button openButton = makeButton("BUKA HALAMAN CHAT", Color.rgb(232, 238, 236), INK);
        LinearLayout.LayoutParams openParams = matchWrap();
        openParams.topMargin = dp(9);
        card.addView(openButton, openParams);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String url = getServerUrl();
                if (url == null) {
                    Toast.makeText(MainActivity.this, "Nyalakan server dan sambungkan tablet ke Wi-Fi terlebih dahulu", Toast.LENGTH_LONG).show();
                    return;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "Browser tidak ditemukan", Toast.LENGTH_LONG).show(); }
            }
        });

        Button copyButton = makeButton("SALIN ALAMAT SERVER", Color.rgb(232, 238, 236), INK);
        LinearLayout.LayoutParams copyParams = matchWrap();
        copyParams.topMargin = dp(9);
        card.addView(copyButton, copyParams);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String url = getServerUrl();
                if (url == null) {
                    Toast.makeText(MainActivity.this, "Belum ada alamat IP. Pastikan Wi-Fi tersambung", Toast.LENGTH_LONG).show();
                    return;
                }
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Alamat RKM Server", url));
                Toast.makeText(MainActivity.this, "Alamat disalin: " + url, Toast.LENGTH_LONG).show();
            }
        });

        TextView stepsTitle = new TextView(this);
        stepsTitle.setText("CARA MENGUJI");
        stepsTitle.setTextColor(INK);
        stepsTitle.setTextSize(12);
        stepsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams stp = matchWrap();
        stp.topMargin = dp(22);
        root.addView(stepsTitle, stp);

        TextView steps = new TextView(this);
        steps.setText(
                "1. Sambungkan tablet ke Wi-Fi.\n" +
                "2. Tekan MULAI SERVER.\n" +
                "3. Uji alamat lokal yang muncul.\n" +
                "4. Konfigurasikan tunnel HTTPS tepercaya ke port 8080 jika perlu akses dari internet.\n" +
                "5. Buka tautan OHLI Social dari perangkat lain; jangan ekspos HTTP mentah ke internet.\n" +
                "6. Saat pengguna meminta login/daftar, buka notifikasi OTP pada perangkat server dan kirim kode secara pribadi melalui WhatsApp. Kode berlaku 5 menit dan hanya sekali pakai."
        );
        steps.setTextColor(MUTED);
        steps.setTextSize(14);
        steps.setLineSpacing(dp(5), 1.0f);
        LinearLayout.LayoutParams stepsParams = matchWrap();
        stepsParams.topMargin = dp(10);
        root.addView(steps, stepsParams);

        TextView warning = new TextView(this);
        warning.setText("DATA: Akun, pesan, foto, Story, dan pengaturan disimpan di penyimpanan internal tablet ini dengan file cadangan. Data tetap ada setelah server/Wi-Fi mati atau IP berubah; setelah jaringan pulih, nyalakan server dan buka alamat IP terbaru. Jangan hapus data aplikasi atau uninstall jika ingin mempertahankan akun. Untuk akses luar Wi-Fi gunakan HTTPS tunnel tepercaya ke port 8080.");
        warning.setTextColor(Color.rgb(130, 76, 25));
        warning.setTextSize(12);
        warning.setPadding(dp(13), dp(13), dp(13), dp(13));
        warning.setBackgroundColor(Color.rgb(255, 242, 222));
        LinearLayout.LayoutParams warningParams = matchWrap();
        warningParams.topMargin = dp(20);
        root.addView(warning, warningParams);

        return scroll;
    }

    private void refreshInboxBadge() {
        if (inboxButton == null) return;
        File file = new File(getFilesDir(), "ohli-server-inbox.json");
        long modified = file.exists() ? file.lastModified() : 0L;
        if (modified == inboxFileModified) return;
        inboxFileModified = modified;
        int unread = 0;
        try {
            JSONArray items = new JSONArray(ServerService.readServerInbox(this));
            long now = System.currentTimeMillis();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null && !item.optBoolean("read", false)
                        && (item.optLong("expiresAt", 0) == 0 || item.optLong("expiresAt", 0) > now)) unread++;
            }
        } catch (Exception ignored) { }
        inboxButton.setText("🔔 PUSAT NOTIFIKASI (" + unread + ")");
    }

    private void showServerInbox() {
        JSONArray items;
        try { items = new JSONArray(ServerService.readServerInbox(this)); }
        catch (Exception e) { items = new JSONArray(); }
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));
        long now = System.currentTimeMillis();
        if (items.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada notifikasi server. Permintaan login dan pendaftaran akan tersimpan di sini.");
            empty.setTextColor(MUTED); empty.setTextSize(14); empty.setPadding(dp(8), dp(18), dp(8), dp(18));
            list.addView(empty, matchWrap());
        } else {
            for (int i = items.length() - 1; i >= 0; i--) {
                final JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(13), dp(12), dp(13), dp(12));
                card.setBackgroundColor(Color.rgb(244, 247, 246));
                LinearLayout.LayoutParams cp = matchWrap(); cp.bottomMargin = dp(9); list.addView(card, cp);
                TextView title = new TextView(this); title.setText(item.optString("title", "Notifikasi OHLI")); title.setTextColor(INK); title.setTextSize(15); title.setTypeface(null, android.graphics.Typeface.BOLD); card.addView(title, matchWrap());
                TextView message = new TextView(this); message.setText(item.optString("message", "")); message.setTextColor(MUTED); message.setTextSize(13); LinearLayout.LayoutParams mp = matchWrap(); mp.topMargin = dp(5); card.addView(message, mp);
                long time = item.optLong("time", 0);
                TextView date = new TextView(this); date.setText(time > 0 ? new SimpleDateFormat("dd MMM yyyy · HH:mm", new Locale("id", "ID")).format(new Date(time)) : "Waktu tidak tersedia"); date.setTextColor(MUTED); date.setTextSize(11); LinearLayout.LayoutParams dpms = matchWrap(); dpms.topMargin = dp(5); card.addView(date, dpms);
                final String code = item.optString("otp", "");
                long expiry = item.optLong("expiresAt", 0);
                if (code.matches("[0-9]{6}") && expiry > now) {
                    TextView codeText = new TextView(this); codeText.setText("Kode OTP: " + code + " · berlaku sampai " + new SimpleDateFormat("HH:mm:ss", new Locale("id", "ID")).format(new Date(expiry))); codeText.setTextColor(INK); codeText.setTextSize(15); codeText.setTypeface(null, android.graphics.Typeface.BOLD); LinearLayout.LayoutParams otpParams = matchWrap(); otpParams.topMargin = dp(8); card.addView(codeText, otpParams);
                    Button copy = makeButton("SALIN OTP", GREEN, WHITE); LinearLayout.LayoutParams copyParams = matchWrap(); copyParams.topMargin = dp(7); card.addView(copy, copyParams);
                    copy.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("OTP OHLI", code)); Toast.makeText(MainActivity.this, "Kode OTP disalin. Kirim hanya ke pemilik nomor.", Toast.LENGTH_LONG).show(); } });
                } else if ("auth_otp".equals(item.optString("type"))) {
                    TextView expired = new TextView(this); expired.setText("OTP sudah kedaluwarsa. Minta pengguna mengirim permintaan baru."); expired.setTextColor(Color.rgb(170, 68, 55)); expired.setTextSize(12); LinearLayout.LayoutParams ep = matchWrap(); ep.topMargin = dp(6); card.addView(expired, ep);
                }
            }
        }
        ScrollView scroll = new ScrollView(this); scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pusat Notifikasi Server").setView(scroll).setPositiveButton("Tutup", null).create();
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, dp(540));
        ServerService.markServerInboxRead(this);
        inboxFileModified = Long.MIN_VALUE;
        refreshInboxBadge();
    }

    private Button makeButton(String text, int background, int foreground) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setTextColor(foreground);
        button.setBackgroundColor(background);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(7), dp(12), dp(7));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String getServerUrl() {
        String ip = getLocalIpAddress();
        if (TextUtils.isEmpty(ip) || "0.0.0.0".equals(ip)) return null;
        return "http://" + ip + ":" + ServerService.PORT + "/";
    }

    private String getLocalIpAddress() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null && wifi.isWifiEnabled()) {
                int address = wifi.getConnectionInfo().getIpAddress();
                if (address != 0) {
                    return String.format(Locale.US, "%d.%d.%d.%d", (address & 0xff), (address >> 8 & 0xff), (address >> 16 & 0xff), (address >> 24 & 0xff));
                }
            }
        } catch (Exception ignored) { }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) return address.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private void refreshStatus() {
        boolean running = ServerService.isRunning;
        String url = getServerUrl();
        if (running) {
            statusText.setText("●  SERVER AKTIF");
            statusText.setTextColor(GREEN);
            addressText.setText(url == null ? "Server aktif · cek koneksi Wi-Fi" : url);
            detailText.setText("Port " + ServerService.PORT + " · Buka alamat ini dari HP pada Wi-Fi yang sama");
            startButton.setEnabled(false);
            startButton.setAlpha(0.55f);
            stopButton.setEnabled(true);
        } else {
            statusText.setText("○  SERVER BELUM AKTIF");
            statusText.setTextColor(MUTED);
            String error = ServerService.lastError;
            addressText.setText(url == null ? "IP belum terbaca · sambungkan Wi-Fi" : url);
            detailText.setText(!TextUtils.isEmpty(error) ? "Error terakhir: " + error : "Port " + ServerService.PORT + " · Server berhenti sampai kamu menyalakannya");
            startButton.setEnabled(true);
            startButton.setAlpha(1.0f);
            stopButton.setEnabled(false);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override protected void onPause() {
        foreground = false;
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }
}
