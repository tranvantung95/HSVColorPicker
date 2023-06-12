package com.iotp.hsvcolorpicker.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class DrawingUtils {

    public static int dpToPx(Context c, float dipValue) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        float val = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
        int res = (int) (val + 0.5); // Round
        // Ensure at least 1 pixel if val was > 0
        return res == 0 && val > 0 ? 1 : res;
    }

    public static void drawnThumb(Canvas canvas, Point point, Paint paint, float radius, int secondColor) {
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(point.x, point.y, radius, paint);
        paint.setColor(secondColor);
        canvas.drawCircle(point.x, point.y, radius * 0.5f, paint);
    }
}

