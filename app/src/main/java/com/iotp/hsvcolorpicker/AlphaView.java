package com.iotp.hsvcolorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.iotp.hsvcolorpicker.event.AlphaColorChangedListener;
import com.iotp.hsvcolorpicker.utils.Constant;
import com.iotp.hsvcolorpicker.utils.DrawingUtils;

public class AlphaView extends View {

    private final static int DEFAULT_BORDER_COLOR = 0xFF6E6E6E;
    private final static int DEFAULT_SLIDER_COLOR = 0xFFBDBDBD;

    private int roundPx = 0;
    /**
     * The width in pixels of the border
     * surrounding all color panels.
     */
    private final static int BORDER_WIDTH_PX = 1;

    private Paint alphaPaint;
    private Paint alphaTextPaint;
    private Paint alphaTrackerPaint;

    private Paint thumbPaint;
    private Paint borderPaint;
    private Shader alphaShader;

    /*
     * We cache a bitmap of the sat/val panel which is expensive to draw each time.
     * We can reuse it when the user is sliding the circle picker as long as the hue isn't changed.
     */
    /* Current values */
    private int alpha = Constant.DEFAULT_ALPHA_VALUE;
    private float hue = 360f;
    private float sat = 1f;
    private float val = 1f;

    private boolean showAlphaPanel = false;
    private String alphaSliderText = null;
    private int sliderTrackerColor = DEFAULT_SLIDER_COLOR;
    private int borderColor = DEFAULT_BORDER_COLOR;

    /**
     * The Rect in which we are allowed to draw.
     * Trackers can extend outside slightly,
     * due to the required padding we have set.
     */
    private RectF drawingRect;

    private RectF alphaRect;

    private Point startTouchPoint = null;

    private AlphaPatternDrawable alphaPatternDrawable;
    private AlphaColorChangedListener onAlphaColorChangedListener;
    private boolean callBack = false;

    public AlphaView(Context context) {
        this(context, null);
    }

    public AlphaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphaView(Context context, AttributeSet attrs, int defStyle) {
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
        state.putBoolean("show_alpha", showAlphaPanel);
        state.putString("alpha_text", alphaSliderText);

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
            showAlphaPanel = bundle.getBoolean("show_alpha");
            alphaSliderText = bundle.getString("alpha_text");

            state = bundle.getParcelable("instanceState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(Context context, AttributeSet attrs) {
        //Load those if set in xml resource file.
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AlphaView);
        showAlphaPanel = a.getBoolean(R.styleable.AlphaView_alphaChannelVisible, false);
        alphaSliderText = a.getString(R.styleable.AlphaView_alphaChannelText);
        sliderTrackerColor = a.getColor(R.styleable.AlphaView_sliderColor, 0xFFBDBDBD);
        borderColor = a.getColor(R.styleable.AlphaView_cpv_borderColor, 0xFF6E6E6E);
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
        alphaTrackerPaint = new Paint();
        alphaPaint = new Paint();
        alphaTextPaint = new Paint();
        borderPaint = new Paint();
        thumbPaint = new Paint();
        alphaTrackerPaint.setColor(sliderTrackerColor);
        alphaTrackerPaint.setStyle(Paint.Style.STROKE);
        alphaTrackerPaint.setStrokeWidth(DrawingUtils.dpToPx(getContext(), 2));
        alphaTrackerPaint.setAntiAlias(true);

        alphaTextPaint.setColor(0xff1c1c1c);
        alphaTextPaint.setTextSize(DrawingUtils.dpToPx(getContext(), 14));
        alphaTextPaint.setAntiAlias(true);
        alphaTextPaint.setTextAlign(Paint.Align.CENTER);
        alphaTextPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawingRect.width() <= 0 || drawingRect.height() <= 0) {
            return;
        }
        drawAlphaPanel(canvas);
    }

    private void drawAlphaPanel(Canvas canvas) {
        /*
         * Will be drawn with hw acceleration, very fast.
         * Also the AlphaPatternDrawable is backed by a bitmap
         * generated only once if the size does not change.
         */

        if (!showAlphaPanel || alphaRect == null || alphaPatternDrawable == null) {
            return;
        }
        final RectF rect = alphaRect;
        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRoundRect(new RectF(
                    rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right - BORDER_WIDTH_PX,
                    rect.bottom + BORDER_WIDTH_PX), roundPx, roundPx, borderPaint);
        }

        alphaPatternDrawable.draw(canvas);
        float[] hsv = new float[]{hue, sat, val};
        int color = Color.HSVToColor(hsv);
        int alphaColor = Color.HSVToColor(0, hsv);

        alphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, alphaColor, color, Shader.TileMode.CLAMP);

        alphaPaint.setShader(alphaShader);

        canvas.drawRoundRect(rect, roundPx, roundPx, alphaPaint);

        if (alphaSliderText != null && !alphaSliderText.equals("")) {
            canvas.drawText(alphaSliderText, rect.centerX(), rect.centerY() + DrawingUtils.dpToPx(getContext(), 4),
                    alphaTextPaint);
        }
        Point p = alphaToPoint(alpha);
        p.y = p.y * 2;
        DrawingUtils.drawnThumb(canvas, p, thumbPaint, drawingRect.height(), getColor());
    }

    public void updateHue(int hue, boolean callBack) {
        this.callBack = callBack;
        this.hue = hue;
        invalidate();
    }

    public void updateAlpha(int color) {
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
        invalidate();
    }

    private Point alphaToPoint(int alpha) {
        final RectF rect = alphaRect;
        final float width = rect.width();
        Point p = new Point();
        p.x = (int) ((alpha * width / Constant.DEFAULT_ALPHA_VALUE) + rect.left);
        p.y = (int) rect.top;

        return p;
    }

    private int pointToAlpha(int x) {
        final RectF rect = alphaRect;
        final int width = (int) rect.width();
        final int right = (int) rect.right;
        final int left = (int) rect.left;
        if (x < left) {
            x = 0;
        } else if (x > right) {
            x = width;
        }
        float result = (x / rect.width()) * Constant.DEFAULT_ALPHA_VALUE;
        return (int) result;
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
            if (onAlphaColorChangedListener != null && callBack) {
                onAlphaColorChangedListener.onAlphaChanged(Color.HSVToColor(this.alpha, new float[]{hue, sat, val}));
            }
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
        if (alphaRect != null) {
            alpha = pointToAlpha((int) event.getX());
            Log.v("Alpha onMeasure", alpha + "");
            update = true;
            callBack = true;
        }
        return update;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.v("Alpha onMeasure w", MeasureSpec.toString(widthMeasureSpec));
        Log.v("Alpha onMeasure h", MeasureSpec.toString(heightMeasureSpec));

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

        //The need to be recreated because they depend on the size of the view.
        alphaShader = null;
        setUpAlphaRect();
    }

    private void setUpAlphaRect() {

        if (!showAlphaPanel) return;

        final RectF dRect = drawingRect;

        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        float bottom = dRect.bottom + BORDER_WIDTH_PX;
        float right = dRect.right + BORDER_WIDTH_PX;

        alphaRect = new RectF(left, top, right, bottom);
        alphaPatternDrawable = new AlphaPatternDrawable(DrawingUtils.dpToPx(getContext(), 4));
        alphaPatternDrawable.setRoundDx(roundPx);
        alphaPatternDrawable.setBounds(
                Math.round(alphaRect.left),
                Math.round(alphaRect.top),
                Math.round(alphaRect.right),
                Math.round(alphaRect.bottom));
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     *
     * @param listener the listener
     */
    public void setOnColorChangedListener(AlphaColorChangedListener listener) {
        onAlphaColorChangedListener = listener;
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
        if (this.callBack && onAlphaColorChangedListener != null) {
            onAlphaColorChangedListener.onAlphaChanged(Color.HSVToColor(this.alpha, new float[]{hue, sat, val}));
        }
        invalidate();
    }
}
