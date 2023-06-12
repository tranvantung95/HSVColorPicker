package com.iotp.hsvcolorpicker;

import androidx.annotation.ColorInt;

import com.iotp.hsvcolorpicker.utils.ColorUtils;
import com.iotp.hsvcolorpicker.utils.Constant;

public class ColorEnvelope {

    @ColorInt
    private int color;
    private String hexCode;
    private int[] argb;

    public ColorEnvelope(@ColorInt int color) {
        this.color = color;
        this.hexCode = ColorUtils.getHexCode(color);
        this.argb = ColorUtils.getColorARGB(color);
    }

    /**
     * gets envelope's color.
     *
     * @return color.
     */
    public @ColorInt int getColor() {
        return color;
    }

    /**
     * gets envelope's hex code value.
     *
     * @return hex code.
     */
    public String getHexCode() {
        return hexCode;
    }

    /**
     * gets envelope's argb color.
     *
     * @return argb integer array.
     */
    public int[] getArgb() {
        return argb;
    }

    public int getAlphaColor() {
        return argb[0];
    }

    /**
     * @return Red color
     */
    public int getHueColor() {
        return argb[1];
    }

    /**
     * @return green color
     */
    public int getSatColor() {
        return argb[2];
    }

    /**
     * @return blue color
     */
    public int getValColor() {
        return argb[3];
    }

    public String formatAlphaValue() {
        double result = Math.ceil(getAlphaColor() / (double) Constant.DEFAULT_ALPHA_VALUE * 100);
        return String.valueOf((int) result);
    }
}

