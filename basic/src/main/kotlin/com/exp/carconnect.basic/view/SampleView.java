package com.exp.carconnect.basic.view;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SampleView extends View {
    private static final String TAG = "SampleView";

    private static final int INDICATOR_COLOR = Color.parseColor("#770000ff");
    private final Paint GAUGE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint TICK_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint INDICATOR_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final float GAUGE_STROKE_WIDTH = 20f;
    private static final float INDICATOR_RADIUS = 40f;
    private static final float SMALL_TICK_LENGTH = 50f;
    private static final float BIG_TICK_LENGTH = 80f;
    private static final float BIG_TICK_WIDTH = 10f;
    private static final float SMALL_TICK_WIDTH = 5f;
    private final int NUMBER_OF_BIG_TICKS = 6;
    private final int NUMBER_OF_SMALL_TICKS_BW_BIG_TICKS = 3;
    private final int NUMBER_OF_SMALL_TICKS = (NUMBER_OF_BIG_TICKS - 1) * NUMBER_OF_SMALL_TICKS_BW_BIG_TICKS;
    private final int TOTAL_NUMBER_OF_TICKS = NUMBER_OF_BIG_TICKS + NUMBER_OF_SMALL_TICKS;
    private final int TOTAL_DATA_POINTS = 101;
    private final float TOTAL_DEGREES_IN_USE = 270f;
    private final float DEGREES_PER_POINT = TOTAL_DEGREES_IN_USE / TOTAL_DATA_POINTS;
    private float AVERAGE_DATA_POINT_WIDTH = (NUMBER_OF_BIG_TICKS * BIG_TICK_WIDTH + NUMBER_OF_SMALL_TICKS * SMALL_TICK_WIDTH) / TOTAL_DATA_POINTS;


    private final int START_DEGREE = 135;
    private final int END_DEGREE = START_DEGREE + 270;

    private final float TICK_GAP_IN_DEGREES = TOTAL_DEGREES_IN_USE / (TOTAL_NUMBER_OF_TICKS - 1);

    private final Point DRIBBLE_RANGE = new Point(-3, 3);

    private ObjectAnimator DRIBBLE_ANIMATOR;
    private ObjectAnimator INDICATOR_ANIMATOR;
    private boolean enableDribble = true;
    private float gaugeCircumference = 0f;
    private float gaugeRadius = 0f;
    private float degreesToCircumference = 0f;
    private float indicatorDegrees = 0f;
    private PointF gaugeCenter = null;


    public SampleView(Context context) {
        this(context, null);
    }

    public SampleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public SampleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public SampleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        GAUGE_PAINT.setColor(getContext().getResources().getColor(android.R.color.holo_blue_bright));
        GAUGE_PAINT.setStyle(Paint.Style.STROKE);
        GAUGE_PAINT.setStrokeWidth(GAUGE_STROKE_WIDTH);

        INDICATOR_PAINT.setColor(INDICATOR_COLOR);
        INDICATOR_PAINT.setStyle(Paint.Style.FILL);

        TICK_PAINT.setColor(getContext().getResources().getColor(android.R.color.holo_blue_bright));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        gaugeRadius = size * .5f - INDICATOR_RADIUS;
        gaugeCircumference = (float) (2 * Math.PI * gaugeRadius);
        degreesToCircumference = 360 / gaugeCircumference;
        gaugeCenter = new PointF(size * .5f, size * .5f);

        drawGauge(canvas, size);
        drawIndicator(canvas, size);
        drawTicks(canvas, size);
        startDribble();

    }


    private void drawGauge(Canvas canvas, int size) {
        RectF rectF = new RectF(0, 0, size, size);
        RectF rectF1 = new RectF(0 + GAUGE_STROKE_WIDTH, 0 + GAUGE_STROKE_WIDTH, size - GAUGE_STROKE_WIDTH, size - GAUGE_STROKE_WIDTH);
        canvas.drawArc(rectF, START_DEGREE, TOTAL_DEGREES_IN_USE, true, INDICATOR_PAINT);
        canvas.drawArc(rectF1, START_DEGREE, TOTAL_DEGREES_IN_USE, false, GAUGE_PAINT);
        canvas.drawCircle(gaugeCenter.x, gaugeCenter.y, INDICATOR_RADIUS, INDICATOR_PAINT);
    }

    private void drawIndicator(Canvas canvas, int size) {
        canvas.save();
        canvas.rotate(indicatorDegrees, gaugeCenter.x, gaugeCenter.y);
        canvas.drawCircle(size - (INDICATOR_RADIUS), gaugeCenter.y, INDICATOR_RADIUS, INDICATOR_PAINT);
        canvas.restore();
    }


    private void drawTicks(Canvas canvas, int size) {
        canvas.save();
        float tickLength;
        canvas.rotate(START_DEGREE, gaugeCenter.x, gaugeCenter.y);
        for (int i = 0; i < TOTAL_NUMBER_OF_TICKS; i++) {
            if (i % 4 == 0) {
                tickLength = BIG_TICK_LENGTH;
                TICK_PAINT.setStrokeWidth(BIG_TICK_WIDTH);
            } else {
                tickLength = SMALL_TICK_LENGTH;
                TICK_PAINT.setStrokeWidth(SMALL_TICK_WIDTH);
            }
            canvas.drawCircle(size - (INDICATOR_RADIUS), gaugeCenter.y, 10f, INDICATOR_PAINT);
            canvas.drawLine(size - (INDICATOR_RADIUS), gaugeCenter.y, size - (INDICATOR_RADIUS + tickLength), gaugeCenter.y, TICK_PAINT);
            canvas.rotate(TICK_GAP_IN_DEGREES, gaugeCenter.x, gaugeCenter.y);
        }
        canvas.restore();
    }

    public void setPoint(int point) {
        Log.d(TAG, "setPoint: " + point);
        animateIndicatorTo(getRotationForPoint(point));
    }

    private float getRotationForPoint(int point) {
        float degreesFromReference = (float) ((point * DEGREES_PER_POINT) + START_DEGREE);
        float indicatorDegrees = this.indicatorDegrees;
        if (indicatorDegrees <= 45) {
            indicatorDegrees += 360;
        }
        return degreesFromReference - indicatorDegrees;
    }

    public void setIndicatorDegree(float degress) {
        if (degress > 360) {
            degress = degress - 360;
        }
        if (degress < -360) {
            degress = degress + 360;
        }
        indicatorDegrees = degress;
        invalidate();
        requestLayout();
        Log.d(TAG, "setIndicatorDegree: " + indicatorDegrees);
    }

    private void startDribbleAnimation() {
        DRIBBLE_ANIMATOR = ObjectAnimator
                .ofFloat(this, "indicatorDegree", indicatorDegrees,
                        indicatorDegrees + DRIBBLE_RANGE.y, indicatorDegrees,
                        indicatorDegrees - DRIBBLE_RANGE.x, indicatorDegrees);
        DRIBBLE_ANIMATOR.setRepeatMode(ObjectAnimator.RESTART);
        DRIBBLE_ANIMATOR.setRepeatCount(ObjectAnimator.INFINITE);
        DRIBBLE_ANIMATOR.setDuration(4000);

        DRIBBLE_ANIMATOR.setInterpolator(new LinearInterpolator());
        DRIBBLE_ANIMATOR.start();
    }

    private void cancelDribbleAnimation() {
        DRIBBLE_ANIMATOR.cancel();
    }


    private void animateIndicatorTo(float degress) {
        cancelIndicatorAnimation();
        INDICATOR_ANIMATOR = ObjectAnimator
                .ofFloat(this, "indicatorDegree", indicatorDegrees,
                        indicatorDegrees + degress)
                .setDuration((long) (Math.abs(degress) * 2));
        INDICATOR_ANIMATOR.setInterpolator(new LinearInterpolator());
        INDICATOR_ANIMATOR.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                INDICATOR_ANIMATOR = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                INDICATOR_ANIMATOR = null;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        stopDribble();
        INDICATOR_ANIMATOR.start();
    }


    private void cancelIndicatorAnimation() {
        if (isIndicatorAnimationActive()) {
            INDICATOR_ANIMATOR.cancel();
        }
    }

    private boolean isIndicatorAnimationActive() {
        return (null != INDICATOR_ANIMATOR &&
                (INDICATOR_ANIMATOR.isRunning() || INDICATOR_ANIMATOR.isPaused()));
    }

    private boolean isDribbleAnimationActive() {
        return (null != DRIBBLE_ANIMATOR &&
                (DRIBBLE_ANIMATOR.isRunning() || DRIBBLE_ANIMATOR.isPaused()));
    }

    private void startDribble() {
        if (enableDribble &&
                !isIndicatorAnimationActive() &&
                !isDribbleAnimationActive()) {
            startDribbleAnimation();
        }
    }

    private void stopDribble() {
        if (enableDribble && isDribbleAnimationActive()) {
            cancelDribbleAnimation();
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


}
