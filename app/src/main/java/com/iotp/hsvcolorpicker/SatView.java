package com.iotp.hsvcolorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.iotp.hsvcolorpicker.event.OnSatChangedListener;
import com.iotp.hsvcolorpicker.utils.Constant;
import com.iotp.hsvcolorpicker.utils.DrawingUtils;

public class SatView extends View {

    private final static int DEFAULT_BORDER_COLOR = 0xFF6E6E6E;
    private final static int DEFAULT_SLIDER_COLOR = 0xFFBDBDBD;
    private final static int CIRCLE_TRACKER_RADIUS_DP = 10;

    /**
     * The width in pixels of the border
     * surrounding all color panels.
     */
    private final static int BORDER_WIDTH_PX = 1;

    /**
     * The radius in px of the color palette tracker circle.
     */
    private int circleTrackerRadiusPx;

    private Paint satValPaint;
    private Paint satValTrackerPaint;

    private Paint borderPaint;

    private Shader valShader;
    private Shader satShader;

    private Paint thumbPaint;
    /*
     * We cache a bitmap of the sat/val panel which is expensive to draw each time.
     * We can reuse it when the user is sliding the circle picker as long as the hue isn't changed.
     */
    private BitmapCache satValBackgroundCache;

    /* Current values */
    private int alpha = Constant.DEFAULT_ALPHA_VALUE;
    private float hue = 360f;
    private float sat = 0f;
    private float val = 0f;
    private int sliderTrackerColor = DEFAULT_SLIDER_COLOR;
    private int borderColor = DEFAULT_BORDER_COLOR;

    private boolean callBack = false;
    /**
     * Minimum required padding. The offset from the
     * edge we must have or else the finger tracker will
     * get clipped when it's drawn outside of the view.
     */
    private int mRequiredPadding;

    /**
     * The Rect in which we are allowed to draw.
     * Trackers can extend outside slightly,
     * due to the required padding we have set.
     */
    private Rect drawingRect;

    private Rect satValRect;
    private Point startTouchPoint = null;
    private OnSatChangedListener onSatChangedListener;

    public SatView(Context context) {
        this(context, null);
    }

    public SatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SatView(Context context, AttributeSet attrs, int defStyle) {
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
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SatView);
        a.recycle();
        applyThemeColors(context);
        circleTrackerRadiusPx = DrawingUtils.dpToPx(getContext(), CIRCLE_TRACKER_RADIUS_DP);
        mRequiredPadding = getResources().getDimensionPixelSize(R.dimen.cpv_required_padding);
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

        satValPaint = new Paint();
        satValTrackerPaint = new Paint();
        borderPaint = new Paint();

        satValTrackerPaint.setStyle(Paint.Style.STROKE);
        satValTrackerPaint.setStrokeWidth(DrawingUtils.dpToPx(getContext(), 2));
        satValTrackerPaint.setAntiAlias(true);
        thumbPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawingRect.width() <= 0 || drawingRect.height() <= 0) {
            return;
        }
        drawSatValPanel(canvas);
    }

    private void drawSatValPanel(Canvas canvas) {
        final Rect rect = satValRect;

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRect(drawingRect.left, drawingRect.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX,
                    borderPaint);
        }

        if (valShader == null) {
            //Black gradient has either not been created or the view has been resized.
            valShader =
                    new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, 0xffffffff, 0xff000000, Shader.TileMode.CLAMP);
        }

        //If the hue has changed we need to recreate the cache.
        if (satValBackgroundCache == null || satValBackgroundCache.value != hue) {

            if (satValBackgroundCache == null) {
                satValBackgroundCache = new BitmapCache();
            }

            //We create our bitmap in the cache if it doesn't exist.
            if (satValBackgroundCache.bitmap == null) {
                satValBackgroundCache.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
            }

            //We create the canvas once so we can draw on our bitmap and the hold on to it.
            if (satValBackgroundCache.canvas == null) {
                satValBackgroundCache.canvas = new Canvas(satValBackgroundCache.bitmap);
            }

            int rgb = Color.HSVToColor(new float[]{hue, 1f, 1f});

            satShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, 0xffffffff, rgb, Shader.TileMode.CLAMP);

            ComposeShader mShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
            satValPaint.setShader(mShader);

            // Finally we draw on our canvas, the result will be
            // stored in our bitmap which is already in the cache.
            // Since this is drawn on a canvas not rendered on
            // screen it will automatically not be using the
            // hardware acceleration. And this was the code that
            // wasn't supported by hardware acceleration which mean
            // there is no need to turn it of anymore. The rest of
            // the view will still be hw accelerated.
            satValBackgroundCache.canvas.drawRect(0, 0, satValBackgroundCache.bitmap.getWidth(),
                    satValBackgroundCache.bitmap.getHeight(), satValPaint);

            //We set the hue value in our cache to which hue it was drawn with,
            //then we know that if it hasn't changed we can reuse our cached bitmap.
            satValBackgroundCache.value = hue;
        }

        // We draw our bitmap from the cached, if the hue has changed
        // then it was just recreated otherwise the old one will be used.
        canvas.drawBitmap(satValBackgroundCache.bitmap, null, rect, null);

        Point p = satValToPoint(sat, val);

        DrawingUtils.drawnThumb(canvas, p, thumbPaint, circleTrackerRadiusPx, getColor());
//        satValTrackerPaint.setColor(0xff000000);
//        canvas.drawCircle(p.x, p.y, circleTrackerRadiusPx - DrawingUtils.dpToPx(getContext(), 1), satValTrackerPaint);
//
//        satValTrackerPaint.setColor(0xffdddddd);
//        canvas.drawCircle(p.x, p.y, circleTrackerRadiusPx, satValTrackerPaint);
    }

    private Point satValToPoint(float sat, float val) {

        final Rect rect = satValRect;
        final float height = rect.height();
        final float width = rect.width();

        Point p = new Point();

        p.x = (int) (sat * width + rect.left);
        p.y = (int) ((1f - val) * height + rect.top);

        return p;
    }

    private float[] pointToSatVal(float x, float y) {

        final Rect rect = satValRect;
        float[] result = new float[2];

        float width = rect.width();
        float height = rect.height();

        if (x < rect.left) {
            x = 0f;
        } else if (x > rect.right) {
            x = width;
        } else {
            x = x - rect.left;
        }

        if (y < rect.top) {
            y = 0f;
        } else if (y > rect.bottom) {
            y = height;
        } else {
            y = y - rect.top;
        }

        result[0] = 1.f / width * x;
        result[1] = 1.f - (1.f / height * y);

        return result;
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
            if (onSatChangedListener != null && callBack) {
                onSatChangedListener.onSatChange(new ColorEnvelope(Color.HSVToColor(alpha, new float[]{hue, sat, val})), false);
            }
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void updateHue(ColorEnvelope colorEnvelope, boolean callBack) {
        this.hue = colorEnvelope.getHueColor();
        this.callBack = callBack;
        invalidate();
        if (callBack) {
            onSatChangedListener.onSatChange(new ColorEnvelope(Color.HSVToColor(alpha, new float[]{hue, sat, val})), false);
        }
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (startTouchPoint == null) {
            return false;
        }
        boolean update = false;
        int startX = startTouchPoint.x;
        int startY = startTouchPoint.y;

        if (satValRect.contains(startX, startY)) {
            float[] result = pointToSatVal(event.getX(), event.getY());
            sat = result[0];
            val = result[1];
            update = true;
            callBack = true;
        }

        return update;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.v("SAT onMeasure w", MeasureSpec.toString(widthMeasureSpec));
        Log.v("SAT onMeasure h", MeasureSpec.toString(heightMeasureSpec));
        int desiredWidth = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
        int desiredHeight = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
        int width = measureDimension(desiredWidth, widthMeasureSpec);
        int height = measureDimension(desiredHeight, heightMeasureSpec);
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
    public int getPaddingTop() {
        return Math.max(super.getPaddingTop(), mRequiredPadding);
    }

    @Override
    public int getPaddingBottom() {
        return Math.max(super.getPaddingBottom(), mRequiredPadding);
    }

    @Override
    public int getPaddingLeft() {
        return Math.max(super.getPaddingLeft(), mRequiredPadding);
    }

    @Override
    public int getPaddingRight() {
        return Math.max(super.getPaddingRight(), mRequiredPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawingRect = new Rect();
        drawingRect.left = getPaddingLeft();
        drawingRect.right = w - getPaddingRight();
        drawingRect.top = getPaddingTop();
        drawingRect.bottom = h - getPaddingBottom();

        //The need to be recreated because they depend on the size of the view.
        valShader = null;
        satShader = null;
        // Clear those bitmap caches since the size may have changed.
        satValBackgroundCache = null;
        setUpSatValRect();
    }

    private void setUpSatValRect() {
        //Calculate the size for the big color rectangle.
        final Rect dRect = drawingRect;
        int left = dRect.left + BORDER_WIDTH_PX;
        int top = dRect.top + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX;
        int right = dRect.right - BORDER_WIDTH_PX;
        satValRect = new Rect(left, top, right, bottom);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     *
     * @param listener the listener
     */
    public void setOnColorChangedListener(OnSatChangedListener listener) {
        onSatChangedListener = listener;
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
     * Set the color this view should show.
     *
     * @param color    The color that should be selected. #argb
     * @param callback If you want to get a callback to your OnColorChangedListener.
     */
    public void setColor(int color, boolean callback) {
        this.callBack = callback;
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

        if (callBack && onSatChangedListener != null) {
            onSatChangedListener.onSatChange(new ColorEnvelope(Color.HSVToColor(alpha, new float[]{hue, sat, val})), true);
        }

        invalidate();
    }
}
