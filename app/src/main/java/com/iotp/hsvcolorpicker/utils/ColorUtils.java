package com.iotp.hsvcolorpicker.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import java.util.Locale;

public class ColorUtils {
    public static String getHexCode(@ColorInt int color) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return String.format(Locale.getDefault(), "%02X%02X%02X%02X", a, r, g, b);
    }

    /** changes color to argb integer array. */
    public static int[] getColorARGB(@ColorInt int color) {
        int[] argb = new int[4];
        argb[0] = Color.alpha(color);
        argb[1] = Color.red(color);
        argb[2] = Color.green(color);
        argb[3] = Color.blue(color);
        return argb;
    }
    public  static float[] argbToHsv(int argbColor){
        float[]  hsv = new float[3];
        Color.RGBToHSV(Color.red(argbColor), Color.green(argbColor), Color.blue(argbColor), hsv);
        return  hsv;
    }
}
