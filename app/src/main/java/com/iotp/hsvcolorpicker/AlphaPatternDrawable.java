package com.iotp.hsvcolorpicker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class AlphaPatternDrawable extends Drawable {

    private int rectangleSize = 10;

    private final Paint paint = new Paint();
    private final Paint paintWhite = new Paint();
    private final Paint paintGray = new Paint();

    private int numRectanglesHorizontal;
    private int numRectanglesVertical;

    /**
     * Bitmap in which the pattern will be cached.
     * This is so the pattern will not have to be recreated each time draw() gets called.
     * Because recreating the pattern i rather expensive. I will only be recreated if the size changes.
     */
    private Bitmap bitmap;
    private int roundDx;

    public AlphaPatternDrawable(int rectangleSize) {
        this.rectangleSize = rectangleSize;
        paintWhite.setColor(0xFFFFFFFF);
        paintGray.setColor(0xFFCBCBCB);
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, null, getBounds(), paint);
        }
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void setAlpha(int alpha) {
        throw new UnsupportedOperationException("Alpha is not supported by this drawable.");
    }

    public void setRoundDx(int roundDx) {
        this.roundDx = roundDx;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        throw new UnsupportedOperationException("ColorFilter is not supported by this drawable.");
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        int height = bounds.height();
        int width = bounds.width();
        numRectanglesHorizontal = (int) Math.ceil((width / rectangleSize));
        numRectanglesVertical = (int) Math.ceil(height / rectangleSize);
        generatePatternBitmap();
    }

    /**
     * This will generate a bitmap with the pattern as big as the rectangle we were allow to draw on.
     * We do this to chache the bitmap so we don't need to recreate it each time draw() is called since it takes a few
     * milliseconds
     */
    private void generatePatternBitmap() {
        if (getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        bitmap = Bitmap.createBitmap(getBounds().width(), getBounds().height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect r = new Rect();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawRoundRect(rectF, roundDx, roundDx, paint);
        paintWhite.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        paintGray.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        boolean verticalStartWhite = true;
        for (int i = 0; i <= numRectanglesVertical; i++) {
            boolean isWhite = verticalStartWhite;
            for (int j = 0; j <= numRectanglesHorizontal; j++) {
                r.top = i * rectangleSize;
                r.left = j * rectangleSize;
                r.bottom = r.top + rectangleSize;
                r.right = r.left + rectangleSize;
                canvas.drawRect(r, isWhite ? paintWhite : paintGray);
                isWhite = !isWhite;
            }
            verticalStartWhite = !verticalStartWhite;
        }
    }
}
