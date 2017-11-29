package com.exp.carconnect.basic.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.exp.carconnect.basic.R;

import java.util.Locale;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Dashboard extends View {
    private static final int MIDDLE_GAUGE_START_ANGLE = 135;
    private static final int MIDDLE_GAUGE_SWEEP_ANGLE = 270;
    private static final int LEFT_GAUGE_START_ANGLE = 55;
    private static final int LEFT_GAUGE_SWEEP_ANGLE = 250;
    private static final int RIGHT_GAUGE_START_ANGLE = 235;
    private static final int RIGHT_GAUGE_SWEEP_ANGLE = 250;
    private static final float GAUGE_STROKE_WIDTH = 20f;
    private static final float COMPASS_STRIKE_WIDTH = 10f;
    private static final int FUEL_GAUGE_START_ANGLE = 70;
    private static final int FUEL_GAUGE_SWEEP_ANGLE = -140;
    private static final int FUEL_GAUGE_BOUNDARY_STROKE_WIDTH = 10;
    private static final float FUEL_GAUGE_DEFAULT_WIDTH_PERCENTAGE = .15f;
    private static final float COMPASS_TEXT_SIZE_PERCENTAGE = .40f;
    private static final float COMPASS_TEXT_MARGIN_FROM_GAUGE = 10f;
    private static final float COMPASS_GAUGE_WIDTH_PERCENTAGE = .30f;

    private static final float MIDDLE_GAUGE_BIG_TICK_LENGTH_PERCENTAGE = .08f;
    private static final float MIDDLE_GAUGE_BIG_TICK_WIDTH_PERCENTAGE = .009f;
    private static final float MIDDLE_GAUGE_SMALL_TICK_LENGTH_PERCENTAGE = .06f;
    private static final float MIDDLE_GAUGE_SMALL_TICK_WIDTH_PERCENTAGE = .007f;
    private static final float MIDDLE_GAUGE_TICK_MARKER_TEXT_SIZE_PERCENTAGE = .05f;
    private static final float MIDDLE_GAUGE_TICK_MARKER_MARGIN_PERCENTAGE = .025f;
    private static final float MIDDLE_GAUGE_INNER_CIRCLE_WIDTH_PERCENTAGE = .45f;
    private static final float MIDDLE_GAUGE_INDICATOR_DIMEN_PERCENTAGE = .1f;
    private static final float MIDDLE_GAUGE_CHECK_ENGINE_LIGHT_DIMEN_PERCENTAGE = .08f;
    private static final float MIDDLE_GAUGE_IGNITION_DIMEN_PERCENTAGE = .08f;

    private static final int MIDDLE_GAUGE_INNER_CIRCLE_STROKE_WIDTH = 10;
    private static final int MIDDLE_GAUGE_TOTAL_NO_OF_TICKS = 33;
    private static final int MIDDLE_GAUGE_BIG_TICK_MULTIPLE = 2;
    private static final int MIDDLE_GAUGE_TICK_MARKER_START = 0;
    private static final int MIDDLE_GAUGE_TICK_MARKER_DIFF = 20;
    private static final float MIDDLE_GAUGE_CURRENT_SPEED_TEXT_SIZE_PERCENTAGE = .05f;
    private static final String DEFAULT_SPEED_UNIT = "km/h";

    private final int MINIMUM_WIDTH = 800;
    private final int MINIMUM_HEIGHT = 400; // fifty percent of height
    private static final float MIDDLE_GAUGE_WIDTH_PERCENTAGE = 0.4f;
    private static final float LEFT_GAUGE_WIDTH_PERCENTAGE = 0.3f;
    private static final float RIGHT_GAUGE_WIDTH_PERCENTAGE = LEFT_GAUGE_WIDTH_PERCENTAGE;

    private final VectorDrawableCompat DEFAULT_FUEL_ICON = VectorDrawableCompat.create(getResources(), R.drawable.ic_local_gas_station_black_24dp, null);
    private final VectorDrawableCompat DEFAULT_COMPASS_ICON = VectorDrawableCompat.create(getResources(), R.drawable.ic_compass, null);
    private final VectorDrawableCompat DEFAULT_INDICATOR_ICON = VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_right_24dp, null);
    private final VectorDrawableCompat DEFAULT_CHECK_ENGINE_LIGHT_ICON = VectorDrawableCompat.create(getResources(), R.drawable.ic_check_engine_light, null);
    private final VectorDrawableCompat DEFAULT_IGNITION_ICON = VectorDrawableCompat.create(getResources(), R.drawable.ic_ignition, null);


    private final int DEFAULT_DASHBOARD_OFFLINE_COLOR = getContext().getResources().getColor(android.R.color.darker_gray);
    private final int DEFAULT_DASHBOARD_ONLINE_COLOR = getContext().getResources().getColor(android.R.color.white);
    private final int DEFAULT_CRITICAL_ZONE_COLOR = getContext().getResources().getColor(android.R.color.holo_red_dark);


    private final Paint GAUGE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint FUEL_GAUGE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint FUEL_GAUGE_BOUNDARY_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint COMPASS_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint COMPASS_TEXT_PAINT = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint TICK_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint TICK_MARKER_TEXT_PAINT = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint MIDDLE_GAUGE_INNER_CIRCLE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint MIDDLE_GAUGE_SPEED_TEXT_PAINT = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint MIDDLE_GAUGE_SPEED_STRIP_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);


    private PointF viewCenter = null;
    private float middleGaugeRadius;
    private float sideGaugeRadius;
    private float fuelPercentage = .5f;
    private float currentSpeed = 0f;
    private boolean showCheckEngineLight = false;
    private boolean showIgnitionIcon = false;
    private boolean online = false;
    private int onlineColor = DEFAULT_DASHBOARD_ONLINE_COLOR;
    private int offlineColor = DEFAULT_DASHBOARD_OFFLINE_COLOR;
    private int criticalZoneColor = DEFAULT_CRITICAL_ZONE_COLOR;

    private VectorDrawableCompat fuelIcon = DEFAULT_FUEL_ICON;
    private VectorDrawableCompat compassIcon = DEFAULT_COMPASS_ICON;
    private VectorDrawableCompat indicatorIcon = DEFAULT_INDICATOR_ICON;
    private VectorDrawableCompat checkEngineLightIcon = DEFAULT_CHECK_ENGINE_LIGHT_ICON;
    private VectorDrawableCompat ignitionIcon = DEFAULT_IGNITION_ICON;


    public Dashboard(Context context) {
        this(context, null);
    }

    public Dashboard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Dashboard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public Dashboard(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        if (online) {
            GAUGE_PAINT.setColor(onlineColor);
            MIDDLE_GAUGE_SPEED_STRIP_PAINT.setColor(onlineColor);
            TICK_PAINT.setColor(onlineColor);
            MIDDLE_GAUGE_INNER_CIRCLE_PAINT.setColor(onlineColor);
            TICK_MARKER_TEXT_PAINT.setColor(onlineColor);
            FUEL_GAUGE_BOUNDARY_PAINT.setColor(onlineColor);
            FUEL_GAUGE_PAINT.setColor(onlineColor);
            COMPASS_TEXT_PAINT.setColor(onlineColor);
            COMPASS_PAINT.setColor(onlineColor);

            fuelIcon.setTint(onlineColor);
            indicatorIcon.setTint(onlineColor);
        } else {
            MIDDLE_GAUGE_SPEED_STRIP_PAINT.setColor(offlineColor);
            TICK_PAINT.setColor(offlineColor);
            MIDDLE_GAUGE_INNER_CIRCLE_PAINT.setColor(offlineColor);
            TICK_MARKER_TEXT_PAINT.setColor(offlineColor);
            FUEL_GAUGE_BOUNDARY_PAINT.setColor(offlineColor);
            FUEL_GAUGE_PAINT.setColor(offlineColor);
            COMPASS_TEXT_PAINT.setColor(offlineColor);
            COMPASS_PAINT.setColor(offlineColor);
            GAUGE_PAINT.setColor(offlineColor);

            fuelIcon.setTint(offlineColor);
            indicatorIcon.setTint(offlineColor);
        }
        GAUGE_PAINT.setStyle(Paint.Style.STROKE);
        GAUGE_PAINT.setStrokeWidth(GAUGE_STROKE_WIDTH);
        GAUGE_PAINT.setMaskFilter(new BlurMaskFilter(.5f, BlurMaskFilter.Blur.OUTER));

        COMPASS_PAINT.setStyle(Paint.Style.STROKE);
        COMPASS_PAINT.setStrokeWidth(COMPASS_STRIKE_WIDTH);

        COMPASS_TEXT_PAINT.setTextLocale(Locale.US);
        COMPASS_TEXT_PAINT.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        COMPASS_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);


        FUEL_GAUGE_PAINT.setStyle(Paint.Style.STROKE);
        FUEL_GAUGE_PAINT.setPathEffect(new DashPathEffect(new float[]{40, 15}, 0));


        FUEL_GAUGE_BOUNDARY_PAINT.setStyle(Paint.Style.STROKE);
        FUEL_GAUGE_BOUNDARY_PAINT.setStrokeWidth(FUEL_GAUGE_BOUNDARY_STROKE_WIDTH);


        TICK_MARKER_TEXT_PAINT.setTextLocale(Locale.US);
        TICK_MARKER_TEXT_PAINT.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        TICK_MARKER_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);

        MIDDLE_GAUGE_INNER_CIRCLE_PAINT.setStyle(Paint.Style.STROKE);
        MIDDLE_GAUGE_INNER_CIRCLE_PAINT.setStrokeWidth(MIDDLE_GAUGE_INNER_CIRCLE_STROKE_WIDTH);
        MIDDLE_GAUGE_INNER_CIRCLE_PAINT.setPathEffect(new DashPathEffect(new float[]{100, 15}, 0));


        MIDDLE_GAUGE_SPEED_TEXT_PAINT.setColor(getContext().getResources().getColor(android.R.color.black));
        MIDDLE_GAUGE_SPEED_TEXT_PAINT.setTextLocale(Locale.US);
        MIDDLE_GAUGE_SPEED_TEXT_PAINT.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        MIDDLE_GAUGE_SPEED_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);

        MIDDLE_GAUGE_SPEED_STRIP_PAINT.setStyle(Paint.Style.FILL_AND_STROKE);
        MIDDLE_GAUGE_SPEED_STRIP_PAINT.setStrokeWidth(10f);


        setBackgroundColor(getContext().getResources().getColor(android.R.color.black));

        checkEngineLightIcon.setTint(getContext().getResources().getColor(android.R.color.darker_gray));
        ignitionIcon.setTint(getContext().getResources().getColor(android.R.color.darker_gray));


    }

    //todo improve this and avoid allocation in onDraw
    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float remainingWidthAfterPadding = width - getPaddingLeft() - getPaddingRight();
        float remainingHeihtAfterPadding = height - getPaddingTop() - getPaddingBottom();
        viewCenter = new PointF(width / 2, height / 2);

        middleGaugeRadius = (remainingWidthAfterPadding * MIDDLE_GAUGE_WIDTH_PERCENTAGE) / 2;
        sideGaugeRadius = (remainingWidthAfterPadding * LEFT_GAUGE_WIDTH_PERCENTAGE) / 2;

        RectF middleGaugeBounds = new RectF(viewCenter.x - middleGaugeRadius, viewCenter.y - middleGaugeRadius,
                viewCenter.x + middleGaugeRadius, viewCenter.y + middleGaugeRadius);

        int leftSideGaugeStartPoint = (int) Math.ceil(middleGaugeBounds.left - (sideGaugeRadius * Math.sqrt(2)));
        RectF leftSideGaugeBounds = new RectF(leftSideGaugeStartPoint, viewCenter.y - sideGaugeRadius,
                leftSideGaugeStartPoint + (2 * sideGaugeRadius), viewCenter.y + sideGaugeRadius);

        int rightSideGaudeEndpoint = (int) Math.ceil(middleGaugeBounds.right + (sideGaugeRadius * Math.sqrt(2)));
        RectF rightSideGaugeBounds = new RectF(rightSideGaudeEndpoint - (2 * sideGaugeRadius), viewCenter.y - sideGaugeRadius,
                rightSideGaudeEndpoint, viewCenter.y + sideGaugeRadius);


        drawMiddleGauge(canvas, middleGaugeBounds);
        drawLeftGauge(canvas, leftSideGaugeBounds);
        drawRightGauge(canvas, rightSideGaugeBounds);

    }


    private void drawMiddleGauge(Canvas canvas, RectF bounds) {
        canvas.drawArc(bounds, MIDDLE_GAUGE_START_ANGLE, MIDDLE_GAUGE_SWEEP_ANGLE, false, GAUGE_PAINT);
        float gaugeCircumference = (float) (2 * Math.PI * (bounds.width() / 2));

        //draw ticks
        float bigTickLength = bounds.width() * MIDDLE_GAUGE_BIG_TICK_LENGTH_PERCENTAGE;
        float bigTickWidth = gaugeCircumference * MIDDLE_GAUGE_BIG_TICK_WIDTH_PERCENTAGE;

        float smallTickLength = bounds.width() * MIDDLE_GAUGE_SMALL_TICK_LENGTH_PERCENTAGE;
        float smallTickWidth = gaugeCircumference * MIDDLE_GAUGE_SMALL_TICK_WIDTH_PERCENTAGE;

        float textSize = bounds.width() * MIDDLE_GAUGE_TICK_MARKER_TEXT_SIZE_PERCENTAGE;
        float testSizeMargin = bounds.width() * MIDDLE_GAUGE_TICK_MARKER_MARGIN_PERCENTAGE;

        drawTicks(canvas, bounds, MIDDLE_GAUGE_START_ANGLE,
                (float) MIDDLE_GAUGE_SWEEP_ANGLE / (MIDDLE_GAUGE_TOTAL_NO_OF_TICKS - 1),
                MIDDLE_GAUGE_TOTAL_NO_OF_TICKS, MIDDLE_GAUGE_BIG_TICK_MULTIPLE,
                bigTickLength, bigTickWidth, smallTickLength, smallTickWidth,
                MIDDLE_GAUGE_TICK_MARKER_START, MIDDLE_GAUGE_TICK_MARKER_DIFF,
                textSize, testSizeMargin);


        drawMiddleGaugeInnerCircle(canvas, bounds);


    }


    private void drawMiddleGaugeInnerCircle(Canvas canvas, RectF bounds) {

        //draw inner circle
        float reduceWidthAndHeightBy = (bounds.width() * (1 - MIDDLE_GAUGE_INNER_CIRCLE_WIDTH_PERCENTAGE)) / 2;
        RectF innerCircleBound = new RectF(bounds);
        innerCircleBound.inset(reduceWidthAndHeightBy, reduceWidthAndHeightBy);
        canvas.drawCircle(innerCircleBound.centerX(), innerCircleBound.centerY(),
                innerCircleBound.width() / 2, MIDDLE_GAUGE_INNER_CIRCLE_PAINT);


        //draw strip and current speed
        float innerCircleRadius = innerCircleBound.width() / 2;

        //calculate Text Bounds
        String currentSpeedText = currentSpeed + " " + DEFAULT_SPEED_UNIT;
        Rect textBound = new Rect();
        MIDDLE_GAUGE_SPEED_TEXT_PAINT.setTextSize(bounds.width() * MIDDLE_GAUGE_CURRENT_SPEED_TEXT_SIZE_PERCENTAGE);
        MIDDLE_GAUGE_SPEED_TEXT_PAINT.getTextBounds(currentSpeedText, 0, currentSpeedText.length(), textBound);


        float textHeightToRadiusRatio = textBound.height() / innerCircleRadius;

        //2r = 180 degrees, i.e. r/2 = 45 degrees
        //todo startdegree is suppose to be 45 degrees, not sure why 33 degrees making it work properly
        float startDegree = 33;
        float sweep = 90 * textHeightToRadiusRatio;
        //draw strip
        Path path = new Path();
        path.arcTo(innerCircleBound, startDegree, -sweep);
        path.arcTo(innerCircleBound, 180 - startDegree + sweep, -sweep);
        path.close();

        canvas.drawPath(path, MIDDLE_GAUGE_SPEED_STRIP_PAINT);
        canvas.drawText(currentSpeedText, innerCircleBound.centerX(),
                innerCircleBound.centerY() + innerCircleRadius / 2, MIDDLE_GAUGE_SPEED_TEXT_PAINT);


        //draw check engine light
        float ignitionIconDimen = bounds.width() * MIDDLE_GAUGE_IGNITION_DIMEN_PERCENTAGE;
        Rect ignitionIconBounds = new Rect((int) (innerCircleBound.centerX() - (innerCircleRadius / 2) - (ignitionIconDimen / 2)),
                (int) (innerCircleBound.centerY() - (innerCircleRadius / 2) - (ignitionIconDimen / 2)),
                (int) (innerCircleBound.centerX() - (innerCircleRadius / 2) + (ignitionIconDimen / 2)),
                (int) (innerCircleBound.centerY() - (innerCircleRadius / 2) + (ignitionIconDimen / 2)));
        ignitionIcon.setBounds(ignitionIconBounds);
        if (showIgnitionIcon) {
            ignitionIcon.setTint(getContext().getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            ignitionIcon.setTint(getContext().getResources().getColor(android.R.color.darker_gray));
        }
        ignitionIcon.draw(canvas);


        //draw check engine light
        float checkEngineLightDimen = bounds.width() * MIDDLE_GAUGE_CHECK_ENGINE_LIGHT_DIMEN_PERCENTAGE;
        Rect checkEngineLightBounds = new Rect((int) (innerCircleBound.centerX() + (innerCircleRadius / 2) - (checkEngineLightDimen / 2)),
                (int) (innerCircleBound.centerY() - (innerCircleRadius / 2) - (checkEngineLightDimen / 2)),
                (int) (innerCircleBound.centerX() + (innerCircleRadius / 2) + (checkEngineLightDimen / 2)),
                (int) (innerCircleBound.centerY() - (innerCircleRadius / 2) + (checkEngineLightDimen / 2)));
        checkEngineLightIcon.setBounds(checkEngineLightBounds);
        if (showCheckEngineLight) {
            checkEngineLightIcon.setTint(getContext().getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            checkEngineLightIcon.setTint(getContext().getResources().getColor(android.R.color.darker_gray));
        }
        checkEngineLightIcon.draw(canvas);


        //draw indicator
        canvas.save();
        canvas.rotate(getDegreeForCurrentSpeed(), innerCircleBound.centerX(), innerCircleBound.centerY());
        float indicatorDimen = bounds.width() * MIDDLE_GAUGE_INDICATOR_DIMEN_PERCENTAGE;
        Rect indicatorBound = new Rect((int) (innerCircleBound.centerX() + innerCircleBound.width() / 2),
                (int) (innerCircleBound.centerY() - indicatorDimen / 2),
                (int) (innerCircleBound.centerX() + innerCircleBound.width() / 2 + indicatorDimen),
                (int) (innerCircleBound.centerY() + indicatorDimen / 2));
        indicatorIcon.setBounds(indicatorBound);
        indicatorIcon.draw(canvas);
        canvas.restore();
    }


    private float getDegreeForCurrentSpeed() {
        return 135;
    }

    private void drawLeftGauge(Canvas canvas, RectF bounds) {
        canvas.drawArc(bounds, LEFT_GAUGE_START_ANGLE, LEFT_GAUGE_SWEEP_ANGLE, false, GAUGE_PAINT);

        int CRITICAL_ANGLE_SWEEP = 45;
        int normalZoneStartAngle = LEFT_GAUGE_START_ANGLE;
        int normalZoneSweep = LEFT_GAUGE_SWEEP_ANGLE - CRITICAL_ANGLE_SWEEP;
        int criticalZoneStartAngle = LEFT_GAUGE_START_ANGLE + normalZoneSweep;
        int criticalZoneSweep = CRITICAL_ANGLE_SWEEP;
        canvas.drawArc(bounds, normalZoneStartAngle, normalZoneSweep, false, GAUGE_PAINT);
        int color = GAUGE_PAINT.getColor();
        GAUGE_PAINT.setColor(criticalZoneColor);
        canvas.drawArc(bounds, criticalZoneStartAngle, criticalZoneSweep, false, GAUGE_PAINT);
        GAUGE_PAINT.setColor(color);
    }


    private void drawRightGauge(Canvas canvas, RectF bounds) {
        canvas.drawArc(bounds, RIGHT_GAUGE_START_ANGLE, RIGHT_GAUGE_SWEEP_ANGLE, false, GAUGE_PAINT);

        RectF boundsAfterStrokeWidth = new RectF(bounds);
        boundsAfterStrokeWidth.inset(GAUGE_STROKE_WIDTH / 2, GAUGE_STROKE_WIDTH / 2);
        drawFuelGauge(canvas, boundsAfterStrokeWidth);
//        drawCompass(canvas, boundsAfterStrokeWidth);
        drawCompassDrawable(canvas, boundsAfterStrokeWidth);
    }


    private void drawFuelGauge(Canvas canvas, RectF bounds) {
        float fuelGaugeWidth = bounds.width() * FUEL_GAUGE_DEFAULT_WIDTH_PERCENTAGE;


        RectF fuelGaugeInnerBound = new RectF(bounds);
        fuelGaugeInnerBound.inset(fuelGaugeWidth + FUEL_GAUGE_BOUNDARY_STROKE_WIDTH / 2, fuelGaugeWidth + FUEL_GAUGE_BOUNDARY_STROKE_WIDTH / 2);
        canvas.drawArc(fuelGaugeInnerBound, FUEL_GAUGE_START_ANGLE, FUEL_GAUGE_SWEEP_ANGLE, false, FUEL_GAUGE_BOUNDARY_PAINT);

        RectF fuelGaugeOuterBound = new RectF(bounds);
        FUEL_GAUGE_PAINT.setStrokeWidth(fuelGaugeWidth);
        fuelGaugeOuterBound.inset(fuelGaugeWidth / 2, fuelGaugeWidth / 2);

        float sweepAngle = Math.abs(FUEL_GAUGE_SWEEP_ANGLE) * fuelPercentage;
        canvas.drawArc(fuelGaugeOuterBound, FUEL_GAUGE_START_ANGLE, -sweepAngle, false, FUEL_GAUGE_PAINT);


        float fuelIndicatorDimen = fuelGaugeWidth * .8f;
        float fuelIndicatorMarginFromGauge = fuelGaugeWidth * .2f;
        Rect rect = new Rect((int) (bounds.centerX() - (fuelIndicatorDimen / 2)),
                (int) (bounds.bottom - (fuelIndicatorDimen + fuelIndicatorMarginFromGauge)),
                (int) (bounds.centerX() + (fuelIndicatorDimen / 2)),
                (int) (bounds.bottom - fuelIndicatorMarginFromGauge));
        fuelIcon.setBounds(rect);
        fuelIcon.draw(canvas);

    }

    private void drawCompass(Canvas canvas, RectF bounds) {
        float compassRadius = (bounds.width() * COMPASS_GAUGE_WIDTH_PERCENTAGE) / 2;
        RectF compassBounds = new RectF(bounds.centerX() - compassRadius, bounds.centerY() - compassRadius,
                bounds.centerX() + compassRadius, bounds.centerY() + compassRadius);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), compassRadius, COMPASS_PAINT);

        float compassTextSize = compassRadius * COMPASS_TEXT_SIZE_PERCENTAGE;
        COMPASS_TEXT_PAINT.setTextSize(compassTextSize);
        float strokeAndMarginFactor = (COMPASS_STRIKE_WIDTH / 2) + (COMPASS_TEXT_MARGIN_FROM_GAUGE / 2);
        RectF compassBoundAfterStrokeAndMargin = new RectF(compassBounds);
        compassBoundAfterStrokeAndMargin.inset(strokeAndMarginFactor, strokeAndMarginFactor);

        float eastDirectionFromOrigin = 35;
        canvas.save();
        canvas.rotate(eastDirectionFromOrigin, compassBoundAfterStrokeAndMargin.centerX(), compassBoundAfterStrokeAndMargin.centerY());
        COMPASS_TEXT_PAINT.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("E", compassBoundAfterStrokeAndMargin.right, compassBoundAfterStrokeAndMargin.centerY() + compassTextSize / 2, COMPASS_TEXT_PAINT);
        COMPASS_TEXT_PAINT.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("W", compassBoundAfterStrokeAndMargin.left, compassBoundAfterStrokeAndMargin.centerY() + compassTextSize / 2, COMPASS_TEXT_PAINT);

        COMPASS_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("S", compassBoundAfterStrokeAndMargin.centerX(), compassBoundAfterStrokeAndMargin.bottom, COMPASS_TEXT_PAINT);
        canvas.drawText("N", compassBoundAfterStrokeAndMargin.centerX(), compassBoundAfterStrokeAndMargin.top + compassTextSize, COMPASS_TEXT_PAINT);
        canvas.restore();
    }

    private void drawCompassDrawable(Canvas canvas, RectF bounds) {
        float compassRadius = (bounds.width() * COMPASS_GAUGE_WIDTH_PERCENTAGE) / 2;
        Rect compassBounds = new Rect((int) (bounds.centerX() - compassRadius), (int) (bounds.centerY() - compassRadius),
                (int) (bounds.centerX() + compassRadius), (int) (bounds.centerY() + compassRadius));
        compassIcon.setBounds(compassBounds);
        compassIcon.draw(canvas);

    }


    private void drawTicks(Canvas canvas, RectF bounds,
                           int startDegree, float tickGapInDegrees,
                           int totalNoOfTicks, int bigTickMultiple,
                           float bigTickLength, float bigTickWidth,
                           float smallTickLength, float smallTickWidth,
                           int tickMarkerStart, int tickMarkerDiff,
                           float textSize, float textMarkerMargin) {
        TICK_MARKER_TEXT_PAINT.setTextSize(textSize);
        canvas.save();
        float tickLength;
        int tickMarker = tickMarkerStart;
        String tickMarkerText;
        float totalRotationSoFar = startDegree;
        canvas.rotate(startDegree, bounds.centerX(), bounds.centerY());
        Rect textBounds = new Rect();
        Rect textDrawingBounds = new Rect();
        RectF lineBounds = new RectF();

        for (int i = 0; i < totalNoOfTicks; i++) {
            if (i % bigTickMultiple == 0) {
                tickLength = bigTickLength;
                TICK_PAINT.setStrokeWidth(bigTickWidth);
            } else {
                tickLength = smallTickLength;
                TICK_PAINT.setStrokeWidth(smallTickWidth);
            }
            lineBounds.set((bounds.right - tickLength), bounds.centerY(), bounds.right, bounds.centerY());
            canvas.drawLine(lineBounds.left, lineBounds.top, lineBounds.right, lineBounds.bottom, TICK_PAINT);

            if (i % bigTickMultiple == 0) {
                canvas.save();
                tickMarkerText = tickMarker + "";
                TICK_MARKER_TEXT_PAINT.getTextBounds(tickMarkerText, 0, tickMarkerText.length(), textBounds);

                textDrawingBounds.set((int) (lineBounds.left - textBounds.width() - textMarkerMargin), (int) (bounds.centerY() - (textBounds.height() / 2)),
                        (int) (lineBounds.left - textMarkerMargin), (int) (bounds.centerY()));
                canvas.rotate(-totalRotationSoFar, textDrawingBounds.centerX(), textDrawingBounds.centerY());
                canvas.drawText(tickMarkerText, textDrawingBounds.centerX(), textDrawingBounds.centerY(), TICK_MARKER_TEXT_PAINT);
                tickMarker += tickMarkerDiff;
                canvas.restore();

            }

            canvas.rotate(tickGapInDegrees, bounds.centerX(), bounds.centerY());
            totalRotationSoFar += tickGapInDegrees;


        }
        canvas.restore();
    }


}
