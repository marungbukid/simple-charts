package com.marungbukid.util;

public class ColorUtil {

    public static int colorOpacity(int color, float opacity) {
        final int baseAlpha = color & 0xff000000 >>> 24;
        final int imag = (int) (baseAlpha * opacity);

        return imag << 24 | (color & 0xffffff);
    }

}
