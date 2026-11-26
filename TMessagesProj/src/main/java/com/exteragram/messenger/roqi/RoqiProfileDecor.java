/*
 * RoqiGram profile decoration: local banner behind the profile header, animated music wave.
 * Distributed under GPLv3 as part of RoqiGram, a fork of exteraGram.
 */
package com.exteragram.messenger.roqi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class RoqiProfileDecor {
    private static final String PREF = "roqi_profile_v1";
    private static final String TAG_BANNER = "roqi_profile_banner";
    private static final String TAG_MUSIC = "roqi_profile_music";

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

    public static boolean musicEnabled(Context c) {
        return prefs(c).getBoolean("music", true);
    }

    public static boolean nftEnabled(Context c) {
        return prefs(c).getBoolean("nft", true);
    }

    public static boolean badgeEnabled(Context c) {
        return prefs(c).getBoolean("badge", true);
    }

    public static void setBannerEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("banner", value).apply();
    }

    public static void setMusicEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("music", value).apply();
    }

    public static void setNftEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("nft", value).apply();
    }

    public static void setBadgeEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("badge", value).apply();
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

    public static int waveColor(Context c) {
        return prefs(c).getInt("wave_color", 0xFFC9A227);
    }

    public static void setWaveColor(Context c, int color) {
        prefs(c).edit().putInt("wave_color", color).apply();
    }

    public static String trackTitle(Context c) {
        String stored = prefs(c).getString("track_title", null);
        if (!TextUtils.isEmpty(stored)) return stored;
        String[] file = readTrackFile();
        return file != null && !TextUtils.isEmpty(file[0]) ? file[0] : "RoqiGram Theme";
    }

    public static String trackArtist(Context c) {
        String stored = prefs(c).getString("track_artist", null);
        if (!TextUtils.isEmpty(stored)) return stored;
        String[] file = readTrackFile();
        return file != null && !TextUtils.isEmpty(file[1]) ? file[1] : "Roqi";
    }

    public static void setTrack(Context c, String title, String artist) {
        prefs(c).edit()
                .putString("track_title", title == null ? "" : title.trim())
                .putString("track_artist", artist == null ? "" : artist.trim())
                .apply();
    }

    private static String[] readTrackFile() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "roqi_music.txt");
        if (!file.isFile()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String title = reader.readLine();
            String artist = reader.readLine();
            return new String[]{title == null ? "" : title.trim(), artist == null ? "" : artist.trim()};
        } catch (Throwable ignored) {
            return null;
        }
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
     * The banner is inserted right above the header background (TopView) and below the avatar,
     * so it behaves like a Telegram profile background: the list scrolls under it, the avatar,
     * name and action bar stay readable on top. The music card floats above the list.
     */
    public static void decorate(View view, View listView, Context c, boolean myProfile) {
        if (!myProfile || !(view instanceof FrameLayout) || c == null) return;
        FrameLayout root = (FrameLayout) view;

        removeTagged(root, TAG_BANNER);
        removeTagged(root, TAG_MUSIC);

        if (bannerEnabled(c)) {
            View topView = findTopView(root);
            int extra = listView != null ? listView.getPaddingTop() : 0;
            if (extra <= 0) extra = (int) dp(c, 280);
            int height = extra + AndroidUtilities.statusBarHeight
                    + ActionBar.getCurrentActionBarHeight();
            FrameLayout banner = buildBanner(c);
            banner.setTag(TAG_BANNER);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height);
            params.gravity = Gravity.TOP;
            if (topView != null && topView.getParent() == root) {
                root.addView(banner, root.indexOfChild(topView) + 1, params);
            } else {
                root.addView(banner, params);
            }
        }

        if (musicEnabled(c)) {
            FrameLayout musicLayer = new FrameLayout(c);
            musicLayer.setTag(TAG_MUSIC);
            musicLayer.setClickable(false);
            musicLayer.setFocusable(false);
            musicLayer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addMusic(musicLayer, c);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.BOTTOM;
            params.bottomMargin = (int) dp(c, 96);
            root.addView(musicLayer, params);
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

    private static FrameLayout buildBanner(Context c) {
        FrameLayout box = new FrameLayout(c);
        box.setClickable(false);
        box.setFocusable(false);
        box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        boolean dark = Theme.getActiveTheme().isDark();
        int page = Theme.getColor(Theme.key_windowBackgroundGray);

        Bitmap bitmap = loadBitmap(bannerPath(c));
        if (bitmap != null) {
            ImageView image = new ImageView(c);
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setAlpha(dark ? 0.9f : 0.95f);
            box.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            int[] colors = dark
                    ? new int[]{0xFF0B1020, 0xFF3B1D6E, 0xFFC9A227}
                    : new int[]{0xFFE8F0FF, 0xFFD8CCFF, 0xFFFFE9A8};
            box.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors));
        }

        // top scrim: keeps status bar icons readable on a bright photo
        View topScrim = new View(c);
        topScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66000000, 0x00000000}));
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(c, 90));
        topParams.gravity = Gravity.TOP;
        box.addView(topScrim, topParams);

        // bottom fade into the list background
        View fade = new View(c);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{page, page & 0x00FFFFFF}));
        FrameLayout.LayoutParams fadeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(c, 64));
        fadeParams.gravity = Gravity.BOTTOM;
        box.addView(fade, fadeParams);

        int textColor = dark ? Color.WHITE : 0xFF151A28;

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(c);
        label.setText("RoqiGram");
        label.setTextColor(textColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        label.setTypeface(AndroidUtilities.bold());
        if (dark) label.setShadowLayer(dp(c, 5), 0, dp(c, 2), 0x99000000);
        row.addView(label);

        if (RoqiPremium.isPremium() && badgeEnabled(c)) {
            TextView chip = new TextView(c);
            chip.setText("♛ PREMIUM");
            chip.setTextColor(0xFF1A1206);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setPadding((int) dp(c, 8), (int) dp(c, 3),
                    (int) dp(c, 8), (int) dp(c, 3));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(0xFFC9A227);
            chipBg.setCornerRadius(dp(c, 20));
            chip.setBackground(chipBg);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipParams.leftMargin = (int) dp(c, 8);
            row.addView(chip, chipParams);
        }

        FrameLayout.LayoutParams rowParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.gravity = Gravity.BOTTOM | Gravity.START;
        rowParams.leftMargin = (int) dp(c, 16);
        rowParams.bottomMargin = (int) dp(c, 20);
        box.addView(row, rowParams);

        if (RoqiPremium.isPremium() && nftEnabled(c)) {
            TextView nft = new TextView(c);
            nft.setText("Roqi NFT #001 · локальный декор");
            nft.setTextColor(0xFFEDE7FF);
            nft.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
            nft.setPadding((int) dp(c, 9), (int) dp(c, 4),
                    (int) dp(c, 9), (int) dp(c, 4));
            GradientDrawable nftBg = new GradientDrawable();
            nftBg.setColor(0xB33B1D6E);
            nftBg.setCornerRadius(dp(c, 12));
            nftBg.setStroke((int) dp(c, 1), 0x66FFFFFF);
            nft.setBackground(nftBg);
            FrameLayout.LayoutParams nftParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nftParams.gravity = Gravity.BOTTOM | Gravity.END;
            nftParams.rightMargin = (int) dp(c, 16);
            nftParams.bottomMargin = (int) dp(c, 20);
            box.addView(nft, nftParams);
        }

        return box;
    }

    private static void addMusic(FrameLayout layer, Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int) dp(c, 12), (int) dp(c, 10),
                (int) dp(c, 14), (int) dp(c, 10));

        int accent = waveColor(c);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xE61A1F2E);
        cardBg.setCornerRadius(dp(c, 20));
        cardBg.setStroke((int) dp(c, 1), 0x66C9A227);
        card.setBackground(cardBg);

        RoqiWaveView wave = new RoqiWaveView(c);
        wave.setBarColor(accent);
        LinearLayout.LayoutParams waveParams = new LinearLayout.LayoutParams(
                (int) dp(c, 48), (int) dp(c, 28));
        waveParams.rightMargin = (int) dp(c, 10);
        card.addView(wave, waveParams);

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(c);
        title.setText(trackTitle(c));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(AndroidUtilities.bold());
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        TextView artist = new TextView(c);
        artist.setText(trackArtist(c));
        artist.setTextColor(0xFFB9C0D4);
        artist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        texts.addView(artist, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView note = new TextView(c);
        note.setText("♫");
        note.setTextColor(accent);
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        card.addView(note);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        params.leftMargin = (int) dp(c, 16);
        params.rightMargin = (int) dp(c, 16);
        layer.addView(card, params);
    }

    private static final class RoqiWaveView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private ValueAnimator animator;
        private float phase;

        RoqiWaveView(Context context) {
            super(context);
            paint.setColor(0xFFC9A227);
        }

        void setBarColor(int color) {
            paint.setColor(color);
            invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
            animator.setDuration(1500);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                phase = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int count = 7;
            float width = getWidth();
            float height = getHeight();
            float gap = width / (count * 2f);
            float barWidth = gap;
            float minHeight = height * 0.22f;
            float maxHeight = height * 0.95f;
            for (int i = 0; i < count; i++) {
                float value = (float) (0.5 + 0.5 * Math.sin(phase + i * 0.85));
                float barHeight = minHeight + (maxHeight - minHeight) * value;
                float left = i * gap * 2f + gap * 0.5f;
                float top = (height - barHeight) / 2f;
                rect.set(left, top, left + barWidth, top + barHeight);
                canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, paint);
            }
        }
    }
}
