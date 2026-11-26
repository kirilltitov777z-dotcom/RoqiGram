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

    // The first three groups use the human-friendly alphabet (no 0/O/1/I).
    // The last group is the HMAC checksum and is hexadecimal, so it may contain 0 and 1.
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^(RF|RM)(-[A-HJ-NP-Z2-9]{4}){3}-[0-9A-F]{4}$");
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
            return hex(md.digest((SECRET_SALT + SECRET_A + SECRET_B + SECRET_C)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return SECRET_A + SECRET_B + SECRET_C;
        }
    }

    /**
     * Checksum input is body + prefix + months. Forever keys use 0 as the month marker,
     * monthly keys carry their real duration so the client does not have to trust a caller.
     */
    private static String hmac4(String body, String prefix, int months) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = body + prefix + ("RM".equals(prefix) ? months : 0);
            byte[] out = mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
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

    private static int normalizeMonths(int months) {
        return months == 3 || months == 6 || months == 12 ? months : 1;
    }

    public static String generateKey(boolean forever, int months) {
        String prefix = forever ? "RF" : "RM";
        int duration = forever ? 0 : normalizeMonths(months);
        SecureRandom rnd = new SecureRandom();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 12; i++) body.append(ALPHABET[rnd.nextInt(ALPHABET.length)]);
        return prefix + "-" + group(body, 0) + "-" + group(body, 4) + "-" + group(body, 8)
                + "-" + hmac4(body.toString(), prefix, duration);
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

    /**
     * Returns 0 for an invalid key, -1 for Forever and 1/3/6/12 for Monthly.
     */
    public static int verifyMonths(String key) {
        if (!isValidFormat(key)) return 0;
        String k = key.trim().toUpperCase(Locale.US);
        String raw = k.replace("-", "");
        String prefix = raw.substring(0, 2);
        String body = raw.substring(2, 14);
        String supplied = raw.substring(14);
        if ("RF".equals(prefix)) {
            return hmac4(body, prefix, 0).equalsIgnoreCase(supplied) ? -1 : 0;
        }
        for (int months : new int[]{1, 3, 6, 12}) {
            if (hmac4(body, prefix, months).equalsIgnoreCase(supplied)) return months;
        }
        return 0;
    }

    public static boolean verify(String key) {
        return verifyMonths(key) != 0;
    }

    private static String hwid() {
        try {
            String androidId = Settings.Secure.getString(ctx().getContentResolver(), Settings.Secure.ANDROID_ID);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((Build.FINGERPRINT + "|" + androidId).getBytes(StandardCharsets.UTF_8));
            return hex(digest).substring(0, 16);
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
    public static String activate(String key) {
        int months = verifyMonths(key);
        if (months == 0) {
            return "Неверный ключ. Формат: RF-XXXX-XXXX-XXXX-XXXX";
        }
        String k = key.trim().toUpperCase(Locale.US);
        if (isRevoked(k)) return "Ключ отозван";
        boolean forever = months == -1;
        SharedPreferences.Editor e = prefs().edit();
        e.putString(K_KEY, obfuscate(k));
        e.putString(K_TYPE, forever ? "F" : "M");
        e.putLong(K_ACTIVATED, System.currentTimeMillis());
        e.putString(K_HWID, hwid());
        long tg = 0;
        try {
            org.telegram.tgnet.TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            if (user != null) tg = user.id;
        } catch (Throwable ignored) {}
        e.putLong(K_TGID, tg);
        if (forever) {
            e.remove(K_EXPIRES);
        } else {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, months);
            e.putLong(K_EXPIRES, c.getTimeInMillis());
        }
        e.apply();
        return null;
    }

    /** Kept for source compatibility with older call sites; the key itself carries the duration. */
    public static String activate(String key, int ignoredMonths) {
        return activate(key);
    }

    public static void clear() {
        prefs().edit().remove(K_KEY).remove(K_TYPE).remove(K_EXPIRES)
                .remove(K_ACTIVATED).remove(K_HWID).remove(K_TGID).apply();
    }

    public static boolean isForever() {
        return true;
    }


    public static boolean isActive() {
        return true;
    }


    public static boolean isPremium() {
        return true;
    }


    public static int daysLeft() {
        if (!"M".equals(deobfuscate(prefs().getString(K_TYPE, null)))) return -1;
        long diff = prefs().getLong(K_EXPIRES, 0) - System.currentTimeMillis();
        return diff <= 0 ? 0 : (int) (diff / 86400000L);
    }

    public static String statusText() {
        return "Все функции RoqiGram бесплатны";
    }


    private static String dateText() {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return f.format(new java.util.Date(prefs().getLong(K_EXPIRES, 0)));
    }

    public static boolean showBadge() {
        return true;
    }


    public static void setShowBadge(boolean value) {
        prefs().edit().putBoolean(K_BADGE, value).apply();
    }
}
