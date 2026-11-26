/*
 * RoqiGram profile decoration: local banner + music wave with track text.
 * GPLv3, part of RoqiGram (fork of exteraGram).
 */
package com.exteragram.messenger.roqi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class RoqiProfileDecor {
    private static final String TAG = "roqi_profile";
    private static final String PREF = "roqi_profile_v1";

    private RoqiProfileDecor() {}

    private static float dp(Context c, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics());
    }

    public static boolean bannerEnabled(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("banner", true);
    }

    public static boolean musicEnabled(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("music", true);
    }

    public static void setBannerEnabled(Context c, boolean value) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("banner", value).apply();
    }

    public static void setMusicEnabled(Context c, boolean value) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("music", value).apply();
    }

    private static File downloads(String name) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(dir, name);
    }

    public static void decorate(android.view.View view, Context c) {
        if (!(view instanceof FrameLayout) || c == null) return;
        FrameLayout root = (FrameLayout) view;
        try {
            if (bannerEnabled(c)) {
                File banner = downloads("roqi_banner.jpg");
                if (banner.exists()) {
                    ImageView image = new ImageView(c);
                    image.setImageBitmap(BitmapFactory.decodeFile(banner.getAbsolutePath()));
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setAlpha(0.55f);
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, (int) dp(c, 180));
                    lp.gravity = Gravity.TOP;
                    image.setLayoutParams(lp);
                    root.addView(image, 0);
                }
            }
        } catch (Throwable ignored) {}

        try {
            if (musicEnabled(c)) {
                File track = downloads("roqi_music.txt");
                if (track.exists()) {
                    String title = "", artist = "";
                    BufferedReader reader = new BufferedReader(new FileReader(track));
                    String line = reader.readLine();
                    if (line != null) title = line.trim();
                    line = reader.readLine();
                    if (line != null) artist = line.trim();
                    reader.close();

                    TextView wave = new TextView(c);
                    wave.setText("\u266b  \u2581\u2583\u2585\u2587\u2585\u2583\u2581");
                    wave.setTextColor(Color.rgb(201, 162, 39));
                    wave.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    wave.setGravity(Gravity.CENTER);

                    TextView label = new TextView(c);
                    label.setText(title + (artist.isEmpty() ? "" : " \u2014 " + artist));
                    label.setTextColor(Color.WHITE);
                    label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    label.setGravity(Gravity.CENTER);

                    android.widget.LinearLayout panel = new android.widget.LinearLayout(c);
                    panel.setOrientation(android.widget.LinearLayout.VERTICAL);
                    panel.setBackground(new ColorDrawable(Color.argb(120, 20, 25, 30)));
                    panel.addView(wave, new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    panel.addView(label, new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            (int) dp(c, 220), ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    lp.bottomMargin = (int) dp(c, 92);
                    panel.setLayoutParams(lp);
                    root.addView(panel);

                    ValueAnimator animator = ValueAnimator.ofFloat(0.45f, 1f);
                    animator.setDuration(1400);
                    animator.setRepeatCount(ValueAnimator.INFINITE);
                    animator.setRepeatMode(ValueAnimator.REVERSE);
                    animator.addUpdateListener(a -> wave.setAlpha((Float) a.getAnimatedValue()));
                    animator.start();
                    panel.setTag(TAG, animator);
                }
            }
        } catch (Throwable ignored) {}
    }
}
