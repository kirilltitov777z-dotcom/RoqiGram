/*
 * RoqiGram Premium core.
 * Distributed under GPLv3 as part of RoqiGram, a fork of exteraGram.
 */
package com.exteragram.messenger.roqi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class RoqiPremium {
    private static final String PREF = "roqi_premium_v1";
    private static final Pattern KEY_PATTERN = Pattern.compile("^(RF|RM)-[A-HJ-NP-Z2-9]{4}(-[A-HJ-NP-Z2-9]{4}){3}$");
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    // Split so plain string scanning does not reveal the admin id. R8 folds this back together.
    private static final long ADMIN_ID = Long.parseLong("883" + "564" + "1983");

    private static final String SECRET_A = "8accfb8a7d";
    private static final String SECRET_B = "10b998aa81";
    private static final String SECRET_C = "13263e37b539";
    private static final String SECRET_SALT = "roqi.premium.hmac.v1";

    private static final String K_KEY = "key";
    private static final String K_TYPE = "type";
    private static final String K_ACTIVATED = "activated";
    private static final String K_EXPIRES = "expires";
    private static final String K_HWID = "hwid";
    private static final String K_TGID = "tgid";
    private static final String K_REVOKED = "revoked";
    private static final String K_BADGE = "badge";

    private RoqiPremium() {}

    private static Context ctx() {
        return ApplicationLoader.applicationContext;
    }

    private static SharedPreferences prefs() {
        return ctx().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String secret() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((SECRET_SALT + SECRET_A + SECRET_B + SECRET_C).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return SECRET_A + SECRET_B + SECRET_C;
        }
    }

    private static String hmac4(String body, String prefix) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal((body + prefix).getBytes(StandardCharsets.US_ASCII));
            return hex(out).substring(0, 4);
        } catch (Exception e) {
            return "";
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format(Locale.US, "%02x", b & 0xff));
        return sb.toString();
    }

    private static String obfuscate(String s) {
        if (s == null) return null;
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        byte[] x = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) x[i] = (byte) (raw[i] ^ (0x5A + (i & 7)));
        return Base64.encodeToString(x, Base64.NO_WRAP);
    }

    private static String deobfuscate(String s) {
        if (s == null) return null;
        try {
            byte[] x = Base64.decode(s, Base64.NO_WRAP);
            byte[] raw = new byte[x.length];
            for (int i = 0; i < x.length; i++) raw[i] = (byte) (x[i] ^ (0x5A + (i & 7)));
            return new String(raw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static String generateKey(boolean forever, int months) {
        String prefix = forever ? "RF" : "RM";
        SecureRandom rnd = new SecureRandom();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 12; i++) body.append(ALPHABET[rnd.nextInt(ALPHABET.length)]);
        return prefix + "-" + group(body, 0) + "-" + group(body, 4) + "-" + group(body, 8) + "-" + hmac4(body.toString(), prefix);
    }

    private static String group(StringBuilder s, int from) {
        return s.substring(from, from + 4);
    }

    public static boolean isAdmin() {
        try {
            org.telegram.tgnet.TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            return user != null && user.id == ADMIN_ID;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static long getAdminId() {
        return ADMIN_ID;
    }

    public static boolean isValidFormat(String key) {
        return key != null && KEY_PATTERN.matcher(key.trim().toUpperCase(Locale.US)).matches();
    }

    public static boolean verify(String key) {
        if (!isValidFormat(key)) return false;
        String k = key.trim().toUpperCase(Locale.US);
        String body = k.replace("-", "");
        String expected = hmac4(body.substring(0, 12), body.substring(0, 2));
        return expected.equalsIgnoreCase(body.substring(12));
    }

    private static String hwid() {
        try {
            String androidId = Settings.Secure.getString(ctx().getContentResolver(), Settings.Secure.ANDROID_ID);
            return MessageDigest.getInstance("SHA-256")
                    .digest((Build.FINGERPRINT + "|" + androidId).getBytes(StandardCharsets.UTF_8)).length > 0
                    ? hex(MessageDigest.getInstance("SHA-256")
                    .digest((Build.FINGERPRINT + "|" + androidId).getBytes(StandardCharsets.UTF_8))).substring(0, 16)
                    : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isRevoked(String key) {
        return prefs().getStringSet(K_REVOKED, new HashSet<String>()).contains(key);
    }

    public static void revoke(String key) {
        Set<String> revoked = new HashSet<>(prefs().getStringSet(K_REVOKED, new HashSet<String>()));
        revoked.add(key);
        prefs().edit().putStringSet(K_REVOKED, revoked).apply();
        String active = deobfuscate(prefs().getString(K_KEY, null));
        if (key != null && key.equals(active)) clear();
    }

    /** Returns null on success or a localized error description. */
    public static String activate(String key, int months) {
        if (!verify(key)) return "Неверный ключ";
        String k = key.trim().toUpperCase(Locale.US);
        if (isRevoked(k)) return "Ключ отозван";
        SharedPreferences.Editor e = prefs().edit();
        e.putString(K_KEY, obfuscate(k));
        e.putString(K_TYPE, k.startsWith("RF") ? "F" : "M");
        e.putLong(K_ACTIVATED, System.currentTimeMillis());
        e.putString(K_HWID, hwid());
        long tg = 0;
        try {
            tg = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id;
        } catch (Throwable ignored) {}
        e.putLong(K_TGID, tg);
        if (k.startsWith("RM")) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, months <= 0 ? 1 : months);
            e.putLong(K_EXPIRES, c.getTimeInMillis());
        } else {
            e.remove(K_EXPIRES);
        }
        e.apply();
        return null;
    }

    public static void clear() {
        prefs().edit().remove(K_KEY).remove(K_TYPE).remove(K_EXPIRES).remove(K_ACTIVATED).remove(K_HWID).remove(K_TGID).apply();
    }

    public static boolean isForever() {
        return "F".equals(deobfuscate(prefs().getString(K_TYPE, null))) && isActive();
    }

    public static boolean isActive() {
        String key = deobfuscate(prefs().getString(K_KEY, null));
        if (key == null || isRevoked(key)) return false;
        long tg = prefs().getLong(K_TGID, -1);
        try {
            long current = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id;
            if (tg != current) return false;
        } catch (Throwable ignored) {
            return false;
        }
        String type = deobfuscate(prefs().getString(K_TYPE, null));
        if ("F".equals(type)) return true;
        if ("M".equals(type)) {
            long expires = prefs().getLong(K_EXPIRES, 0);
            return expires > System.currentTimeMillis();
        }
        return false;
    }

    public static boolean isPremium() {
        return isActive();
    }

    public static int daysLeft() {
        if (!"M".equals(deobfuscate(prefs().getString(K_TYPE, null)))) return -1;
        long diff = prefs().getLong(K_EXPIRES, 0) - System.currentTimeMillis();
        return diff <= 0 ? 0 : (int) (diff / 86400000L);
    }

    public static String statusText() {
        if (isForever()) return "Premium Forever — активен бессрочно";
        if (isActive()) return "Premium Monthly — активен до " + dateText() + " (осталось " + daysLeft() + " дн.)";
        if ("M".equals(deobfuscate(prefs().getString(K_TYPE, null))) && daysLeft() == 0)
            return "Premium Monthly — истёк. Продлить 10 ₽/мес → @Cafulu";
        return "Premium не активирован";
    }

    private static String dateText() {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return f.format(new java.util.Date(prefs().getLong(K_EXPIRES, 0)));
    }

    public static boolean showBadge() {
        return isActive() && prefs().getBoolean(K_BADGE, true);
    }

    public static void setShowBadge(boolean value) {
        prefs().edit().putBoolean(K_BADGE, value).apply();
    }
}
