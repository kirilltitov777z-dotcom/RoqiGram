/*
 * RoqiGram profile decoration: banner behind the whole profile and in the header.
 * Distributed under GPLv3 as part of RoqiGram, a fork of exteraGram.
 */
package com.exteragram.messenger.roqi;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;

public final class RoqiProfileDecor {
    private static final String PREF = "roqi_profile_v1";
    private static final String TAG_PAGE = "roqi_profile_page_banner";
    private static final String TAG_HEADER = "roqi_profile_header_banner";

    private RoqiProfileDecor() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static float dp(Context c, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics());
    }

    public static boolean bannerEnabled(Context c) {
        return prefs(c).getBoolean("banner", true);
    }

    public static void setBannerEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("banner", value).apply();
    }

    public static boolean fullProfile(Context c) {
        return prefs(c).getBoolean("full_profile", true);
    }

    public static void setFullProfile(Context c, boolean value) {
        prefs(c).edit().putBoolean("full_profile", value).apply();
    }

    public static boolean badgeEnabled(Context c) {
        return prefs(c).getBoolean("badge", true);
    }

    public static void setBadgeEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("badge", value).apply();
    }

    public static boolean nftEnabled(Context c) {
        return prefs(c).getBoolean("nft", true);
    }

    public static void setNftEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("nft", value).apply();
    }

    public static String bannerPath(Context c) {
        String stored = prefs(c).getString("banner_path", null);
        if (!TextUtils.isEmpty(stored) && new File(stored).isFile()) return stored;
        File legacy = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "roqi_banner.jpg");
        return legacy.isFile() ? legacy.getAbsolutePath() : null;
    }

    public static void setBannerPath(Context c, String path) {
        prefs(c).edit().putString("banner_path", path).apply();
    }

    public static void clearBanner(Context c) {
        prefs(c).edit().remove("banner_path").apply();
        File local = new File(c.getFilesDir(), "roqi_banner.jpg");
        if (local.isFile()) local.delete();
    }

    private static Bitmap loadBitmap(String path) {
        if (TextUtils.isEmpty(path)) return null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 1440 || bounds.outHeight / sample > 1440) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, options);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Two layers: one behind the list (the banner fills the whole profile), and one in the
     * header above TopView but below the avatar. Both are non-clickable.
     */
    public static void decorate(View view, View listView, Context c, boolean myProfile) {
        if (!myProfile || !(view instanceof FrameLayout) || c == null) return;
        FrameLayout root = (FrameLayout) view;

        removeTagged(root, TAG_PAGE);
        removeTagged(root, TAG_HEADER);

        if (!bannerEnabled(c)) {
            if (listView != null) {
                listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            }
            return;
        }

        Bitmap bitmap = loadBitmap(bannerPath(c));
        int pageColor = Theme.getColor(Theme.key_windowBackgroundGray);
        boolean dark = Theme.getActiveTheme().isDark();

        if (fullProfile(c)) {
            FrameLayout page = buildPageBanner(c, bitmap, dark);
            page.setTag(TAG_PAGE);
            root.addView(page, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (listView != null) {
                listView.setBackgroundColor(Color.TRANSPARENT);
                if (RoqiMods.transparentProfile(c)) {
                    final ViewGroup lv = (ViewGroup) listView;
                    lv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            for (int i = 0; i < lv.getChildCount(); i++) {
                                View child = lv.getChildAt(i);
                                if (child.getBackground() != null) {
                                    child.setBackgroundColor(Color.TRANSPARENT);
                                }
                            }
                            return true;
                        }
                    });
                }
            }
        }

        View topView = findTopView(root);
        int extra = listView != null ? listView.getPaddingTop() : 0;
        if (extra <= 0) extra = (int) dp(c, 280);
        int height = extra + AndroidUtilities.statusBarHeight
                + ActionBar.getCurrentActionBarHeight();
        FrameLayout header = buildHeaderBanner(c, bitmap, pageColor, dark);
        header.setTag(TAG_HEADER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.gravity = Gravity.TOP;
        if (topView != null && topView.getParent() == root) {
            root.addView(header, root.indexOfChild(topView) + 1, params);
        } else {
            root.addView(header, params);
        }
    }

    private static void removeTagged(FrameLayout root, String tag) {
        View old = root.findViewWithTag(tag);
        if (old != null && old.getParent() instanceof ViewGroup) {
            ((ViewGroup) old.getParent()).removeView(old);
        }
    }

    private static View findTopView(FrameLayout root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            String name = child.getClass().getName();
            if (name.endsWith("$TopView") || name.endsWith(".TopView")) {
                return child;
            }
        }
        return null;
    }

    private static void attachImage(FrameLayout box, Context c, Bitmap bitmap, boolean dark) {
        if (bitmap != null) {
            ImageView image = new ImageView(c);
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setAlpha(dark ? 0.85f : 0.95f);
            box.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            int[] colors = dark
                    ? new int[]{0xFF0B1020, 0xFF3B1D6E, 0xFFC9A227}
                    : new int[]{0xFFE8F0FF, 0xFFD8CCFF, 0xFFFFE9A8};
            box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors));
        }
    }

    /** Background behind the list; profile rows keep their own cards on top of it. */
    private static FrameLayout buildPageBanner(Context c, Bitmap bitmap, boolean dark) {
        FrameLayout box = new FrameLayout(c);
        box.setClickable(false);
        box.setFocusable(false);
        box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        attachImage(box, c, bitmap, dark);
        View scrim = new View(c);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                dark ? new int[]{0x66000000, 0x22000000}
                        : new int[]{0x33FFFFFF, 0x22FFFFFF}));
        box.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return box;
    }

    /** Banner in the header, under the avatar and name, over TopView. */
    private static FrameLayout buildHeaderBanner(Context c, Bitmap bitmap, int pageColor, boolean dark) {
        FrameLayout box = new FrameLayout(c);
        box.setClickable(false);
        box.setFocusable(false);
        box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        attachImage(box, c, bitmap, dark);

        View topScrim = new View(c);
        topScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66000000, 0x00000000}));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(c, 90));
        topParams.gravity = Gravity.TOP;
        box.addView(topScrim, topParams);

        View fade = new View(c);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{pageColor, pageColor & 0x00FFFFFF}));
        FrameLayout.LayoutParams fadeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(c, 64));
        fadeParams.gravity = Gravity.BOTTOM;
        box.addView(fade, fadeParams);

        return box;
    }
}
