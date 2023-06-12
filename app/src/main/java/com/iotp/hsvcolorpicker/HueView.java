package com.iotp.hsvcolorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.iotp.hsvcolorpicker.event.HueColorChangedListener;
import com.iotp.hsvcolorpicker.utils.Constant;
import com.iotp.hsvcolorpicker.utils.DrawingUtils;

public class HueView extends View {

    private final static int DEFAULT_BORDER_COLOR = 0xFF6E6E6E;
    private final static int DEFAULT_SLIDER_COLOR = 0xFFBDBDBD;
    private int roundPx = 0;
    /**
     * The width in pixels of the border
     * surrounding all color panels.
     */
    private final static int BORDER_WIDTH_PX = 1;
    private Paint borderPaint;

    private Paint thumbPaint;

    /*
     * We cache a bitmap of the sat/val panel which is expensive to draw each time.
     * We can reuse it when the user is sliding the circle picker as long as the hue isn't changed.
     */

    /* We cache the hue background to since its also very expensive now. */
    private BitmapCache hueBackgroundCache;

    /* Current values */
    private int alpha = Constant.DEFAULT_ALPHA_VALUE;
    private float hue = 360f;
    private float sat = 1f;
    private float val = 1f;
    private int sliderTrackerColor = DEFAULT_SLIDER_COLOR;
    private int borderColor = DEFAULT_BORDER_COLOR;

    /**
     * The Rect in which we are allowed to draw.
     * Trackers can extend outside slightly,
     * due to the required padding we have set.
     */
    private RectF drawingRect;

    private RectF hueRect;

    private Point startTouchPoint = null;

    private boolean callBack = false;
    private HueColorChangedListener hueColorChangedListener;

    public HueView(Context context) {
        this(context, null);
    }

    public HueView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HueView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("instanceState", super.onSaveInstanceState());
        state.putInt("alpha", alpha);
        state.putFloat("hue", hue);
        state.putFloat("sat", sat);
        state.putFloat("val", val);

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            alpha = bundle.getInt("alpha");
            hue = bundle.getFloat("hue");
            sat = bundle.getFloat("sat");
            val = bundle.getFloat("val");

            state = bundle.getParcelable("instanceState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(Context context, AttributeSet attrs) {
        //Load those if set in xml resource file.
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.HueView);

        //borderColor = a.getColor(R.styleable.ColorPickerView_cpv_borderColor, 0xFF6E6E6E);
        a.recycle();
        applyThemeColors(context);
        initPaintTools();

        //Needed for receiving trackball motion events.
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void applyThemeColors(Context c) {
        // If no specific border/slider color has been
        // set we take the default secondary text color
        // as border/slider color. Thus it will adopt
        // to theme changes automatically.

        final TypedValue value = new TypedValue();
        TypedArray a = c.obtainStyledAttributes(value.data, new int[]{android.R.attr.textColorSecondary});

        if (borderColor == DEFAULT_BORDER_COLOR) {
            borderColor = a.getColor(0, DEFAULT_BORDER_COLOR);
        }

        if (sliderTrackerColor == DEFAULT_SLIDER_COLOR) {
            sliderTrackerColor = a.getColor(0, DEFAULT_SLIDER_COLOR);
        }

        a.recycle();
    }

    private void initPaintTools() {
        borderPaint = new Paint();
        thumbPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawingRect.width() <= 0 || drawingRect.height() <= 0) {
            return;
        }
        drawHuePanel(canvas);
    }

    private void drawHuePanel(Canvas canvas) {
        final RectF rect = hueRect;
        final RectF drawnRecF = new RectF(
                rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX,
                rect.bottom + BORDER_WIDTH_PX);
        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRoundRect(drawnRecF, roundPx, roundPx, borderPaint);
        }
        int width = (int) rect.width();
        int height = (int) rect.height();
        if (hueBackgroundCache == null) {
            hueBackgroundCache = new BitmapCache();
            hueBackgroundCache.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            hueBackgroundCache.canvas = new Canvas(hueBackgroundCache.bitmap);

            for (int x = 0; x < width; x++) {
                float hue = 0;
                if (rect.width() > height) {
                    hue = (x * 360f) / width;
                }
                for (int y = 0; y < height; y++) {
                    if (width <= height) {
                        hue = (y * 360f) / height;
                    }
                    float[] hsv = new float[]{hue, 1, 1};
                    hueBackgroundCache.bitmap.setPixel(x, y, Color.HSVToColor(hsv));
                }
            }
        }

        Resources res = getResources();
        RoundedBitmapDrawable dr =
                RoundedBitmapDrawableFactory.create(res, hueBackgroundCache.bitmap);
        dr.setCornerRadius(roundPx);
        dr.setBounds((int) drawnRecF.left, (int) drawnRecF.top, (int) drawnRecF.right, (int) drawnRecF.bottom);
        dr.draw(canvas);
        DrawingUtils.drawnThumb(canvas, hueToPoint(hue), thumbPaint, drawingRect.height(), getColor());
    }

    /**
     * P(x) = hue * width / 360
     */
    private Point hueToPoint(float hue) {
        final RectF rect = hueRect;
        Point p = new Point();
        float x = hue * rect.width() / 360;
        p.x = (int) x;
        p.y = (int) rect.centerY();
        return p;
    }

    /**
     * hue = P(x) * 360 / width
     */
    private float pointToHue(float x) {
        hue = x * 360f / hueRect.width();
        if (hue < 0) {
            hue = 0;
        } else if (hue > 360) {
            hue = 360;
        }
        return hue;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startTouchPoint = new Point((int) event.getX(), (int) event.getY());
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_MOVE:
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_UP:
                startTouchPoint = null;
                update = moveTrackersIfNeeded(event);
                break;
        }

        if (update) {
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (startTouchPoint == null) {
            return false;
        }
        boolean update = false;
        int startX = startTouchPoint.x;
        int startY = startTouchPoint.y;

        if (hueRect.contains(startX, startY)) {
            hue = pointToHue(event.getX());
            if (hueColorChangedListener != null) {
                hueColorChangedListener.onHueChanged(new ColorEnvelope(getColor()));
            }
            update = true;
        }

        return update;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
        int desiredHeight = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
        int width = measureDimension(desiredWidth, widthMeasureSpec);
        int height = measureDimension(desiredHeight, heightMeasureSpec);
        roundPx = height / 2;
        setMeasuredDimension(width, height);
    }

    private int measureDimension(int desiredSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = desiredSize;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        if (result < desiredSize) {
            Log.e("ErrorView", "The view is too small, the content might get cut");
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawingRect = new RectF();
        drawingRect.left = getPaddingLeft();
        drawingRect.right = w - getPaddingRight();
        drawingRect.top = getPaddingTop();
        drawingRect.bottom = h - getPaddingBottom();
        // Clear those bitmap caches since the size may have changed.
        hueBackgroundCache = null;
        setUpHueRect();
    }

    private void setUpHueRect() {
        //Calculate the size for the hue slider on the left.
        final RectF dRect = drawingRect;
        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        float bottom = dRect.bottom + BORDER_WIDTH_PX;
        float right = dRect.right + BORDER_WIDTH_PX;
        hueRect = new RectF(left, top, right, bottom);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     *
     * @param listener the listener
     */
    public void setOnColorChangedListener(HueColorChangedListener listener) {
        hueColorChangedListener = listener;
    }

    /**
     * Get the current color this view is showing.
     *
     * @return the current color.
     */
    public int getColor() {
        return Color.HSVToColor(alpha, new float[]{hue, sat, val});
    }

    /**
     * Set the color the view should show.
     *
     * @param color The color that should be selected. #argb
     */
    public void setColor(int color) {
        setColor(color, false);
    }

    /**
     * Set the color this view should show.
     *
     * @param color    The color that should be selected. #argb
     * @param callback If you want to get a callback to your OnColorChangedListener.
     */
    public void setColor(int color, boolean callback) {

        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        float[] hsv = new float[3];

        Color.RGBToHSV(red, green, blue, hsv);

        this.alpha = alpha;
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        callBack = callback;
        if (callBack && hueColorChangedListener != null) {
            hueColorChangedListener.onHueChanged(new ColorEnvelope(getColor()));
        }
        invalidate();
    }
}
