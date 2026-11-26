/*
 * RoqiGram local modes. GPLv3, part of RoqiGram (fork of exteraGram).
 */
package com.exteragram.messenger.roqi;

import android.content.Context;
import android.content.SharedPreferences;

public final class RoqiMods {
    private static final String PREF = "roqi_mods";

    private RoqiMods() {}

    public static boolean get(Context c, String key, boolean fallback) {
        if (c == null) return fallback;
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(key, fallback);
    }

    public static void set(Context c, String key, boolean value) {
        if (c == null) return;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static boolean coloredName(Context c) { return get(c, "colored_name", true); }
    public static boolean nameBadge(Context c) { return get(c, "name_badge", true); }
    public static boolean transparentProfile(Context c) { return get(c, "transparent_profile", true); }
}
