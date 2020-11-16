package com.marungbukid.charts.line;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewConfiguration;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.marungbukid.charts.BaseChart;
import com.marungbukid.charts.BaseChartAdapter;
import com.marungbukid.charts.R;
import com.marungbukid.charts.gestures.ScrubGestureDetector;
import com.marungbukid.util.ColorUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LineChartView extends BaseChart {
    private static final String TAG = "LineChartView";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            FillType.NONE,
            FillType.UP,
            FillType.DOWN,
            FillType.TOWARD_ZERO,
    })
    public @interface FillType {
        /**
         * Fill type constant for having no fill on the graph
         */
        int NONE = 0;

        /**
         * Fill type constant for always filling the area above the line.
         */
        int UP = 1;

        /**
         * Fill type constant for always filling the area below the line
         */
        int DOWN = 2;

        /**
         * Fill type constant for filling toward zero. This will fill downward if your line is
         * positive, or upward if your line is negative. If your line intersects zero,
         * each segment will still color toward zero.
         */
        int TOWARD_ZERO = 3;
    }

    // styleable values
    @ColorInt
    private int lineColor;
    private float lineWidth;
    @ColorInt
    private int fillColor;
    @FillType
    private int fillType = FillType.NONE;
    private boolean scrubEnabled = false;
    @ColorInt
    private int scrubLineColor;
    private float scrubLineWidth;
    @ColorInt
    private int
            topColorGradientFill;
    @ColorInt
    private int bottomColorGradientFill;
    private LinearGradient linearGradientFill;
    private boolean lastPointMarkerEnabled = false;
    @ColorInt
    private int lastPointMarkerColor;
    @ColorInt
    private int scrubPointMarkerColor;

    // paths for the onDraw data
    private final Path renderPath = new Path();
    private final Path linePath = new Path();
    private final Path baseLinePath = new Path();
    private final Path scrubLinePath = new Path();
    private final Path gradientPath = new Path();
    private Path lastPointMarkerPath;
    private Path lastPointMarkerRipplePath;
    private Path scrubPointMarkerPath;
    private Path scrubPointMarkerRipplePath;

    // misc fields
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baseLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrubLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint lastPointMarkerPaint;
    private Paint lastPointMarkerRipplePaint;
    private Paint scrubPointMarkerPaint;
    private Paint scrubPointMarkerRipplePaint;
    private final RectF contentRect = new RectF();
    private List<Float> xPoints;
    private List<Float> yPoints;

    public LineChartView(Context context) {
        super(context);
        init(context, null, R.attr.stockCharts_LineChartViewStyle, R.style.stockCharts_Base_LineChart);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.stockCharts_LineChartViewStyle, R.style.stockCharts_Base_LineChart);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.stockCharts_Base_LineChart);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateContentRect();
        populatePath();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(baseLinePath, baseLinePaint);

        if (fillType != FillType.NONE) {
            canvas.drawPath(gradientPath, fillPaint);
        }

        canvas.drawPath(renderPath, linePaint);

        if (lastPointMarkerEnabled) {
            canvas.drawPath(lastPointMarkerRipplePath, lastPointMarkerRipplePaint);
            canvas.drawPath(lastPointMarkerPath, lastPointMarkerPaint);
        }

        if (scrubEnabled) {
            canvas.drawPath(scrubPointMarkerRipplePath, scrubPointMarkerRipplePaint);
            canvas.drawPath(scrubPointMarkerPath, scrubPointMarkerPaint);
        }

        canvas.drawPath(scrubLinePath, scrubLinePaint);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        updateContentRect();
        populatePath();
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LineChartView,
                defStyleAttr, defStyleRes);

        lineColor = a.getColor(R.styleable.LineChartView_charts_lineColor, 0);
        fillColor = a.getColor(R.styleable.LineChartView_charts_fillColor, 0);
        lineWidth = a.getDimension(R.styleable.LineChartView_charts_lineWidth, 0);

        scrubEnabled = a.getBoolean(R.styleable.BaseChart_charts_scrubEnabled, false);
        scrubLineColor = a.getColor(R.styleable.BaseChart_charts_scrubLineColor, 0);
        scrubLineWidth = a.getDimension(R.styleable.BaseChart_charts_scrubLineWidth, lineWidth);
        lastPointMarkerEnabled = a.getBoolean(R.styleable.LineChartView_charts_lastPointMarkerEnabled, false);

        int fillType = a.getInt(R.styleable.LineChartView_charts_fillType, FillType.NONE);
        setFillType(fillType);

        if (fillType != FillType.NONE) {
            topColorGradientFill = a.getColor(R.styleable.BaseChart_charts_topColorGradientFill, 0);
            bottomColorGradientFill = a.getColor(R.styleable.BaseChart_charts_bottomColorGradientFill, 0);

            topColorGradientFill = ColorUtil.colorOpacity(topColorGradientFill, 0.6f);
            bottomColorGradientFill = ColorUtil.colorOpacity(bottomColorGradientFill, 0.6f);
        } else {
            fillPaint.setColor(fillColor);
        }

        if (lastPointMarkerEnabled) {
            lastPointMarkerColor = a.getColor(R.styleable.LineChartView_charts_lastPointMarkerColor, lineColor);
        }

        if (scrubEnabled) {
            scrubPointMarkerColor = a.getColor(R.styleable.LineChartView_charts_scrubPointMarkerColor, lineColor);
        }

        a.recycle();

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(lineColor);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setPathEffect(new CornerPathEffect(4f));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setStrokeWidth(6);

        scrubLinePaint.setStyle(Paint.Style.STROKE);
        scrubLinePaint.setStrokeWidth(scrubLineWidth);
        scrubLinePaint.setColor(ColorUtil.colorOpacity(scrubLineColor, 0.8f));

        if (lastPointMarkerEnabled) {
            lastPointMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            lastPointMarkerRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            lastPointMarkerPath = new Path();
            lastPointMarkerRipplePath = new Path();

            lastPointMarkerPaint.setStyle(Paint.Style.FILL);
            lastPointMarkerPaint.setColor(lastPointMarkerColor);

            lastPointMarkerRipplePaint.setStyle(Paint.Style.FILL);
            lastPointMarkerRipplePaint.setColor(ColorUtil.colorOpacity(lastPointMarkerColor, 0.4f));
        }

        if (scrubEnabled) {
            final Handler handler = new Handler();
            final float touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            scrubGestureDetector = new ScrubGestureDetector(this, handler, touchSlop);
            scrubGestureDetector.setEnabled(scrubEnabled);
            setOnTouchListener(scrubGestureDetector);

            scrubPointMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scrubPointMarkerRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scrubPointMarkerPath = new Path();
            scrubPointMarkerRipplePath = new Path();

            scrubPointMarkerPaint.setStyle(Paint.Style.FILL);
            scrubPointMarkerPaint.setColor(scrubPointMarkerColor);

            scrubPointMarkerRipplePaint.setStyle(Paint.Style.FILL);
            scrubPointMarkerRipplePaint.setColor(ColorUtil.colorOpacity(scrubPointMarkerColor, 0.4f));
        }


        xPoints = new ArrayList<>();
        yPoints = new ArrayList<>();

        // if animate //create animator
    }

    @Override
    protected void populatePath() {
        if (adapter == null) return;
        if (getWidth() == 0 || getHeight() == 0) return;

        final int adapterCount = adapter.getCount();

        if (adapterCount < 2) {
            clearData();
            return;
        }

        scaleHelper = new ScaleHelper(adapter, contentRect, lineWidth, true);

        xPoints.clear();
        yPoints.clear();

        linePath.reset();
        for (int i = 0; i < adapterCount; i++) {
            final float x = scaleHelper.getX(adapter.getX(i));
            final float y = scaleHelper.getY(adapter.getY(i));

            xPoints.add(x);
            yPoints.add(y);

            if (i == 0) {
                linePath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
            }
        }

        gradientPath.reset();
        gradientPath.addPath(linePath);
        final Float fillEdge = getFillEdge();
        if (fillEdge != null) {
            final float lastX = scaleHelper.getX(adapter.getX(adapter.getCount()) - 1);

            // line up or down to the fill edge
            gradientPath.lineTo(lastX, fillEdge);

            // line straight left to far edge of view
            gradientPath.lineTo(getPaddingStart(), fillEdge);

            // closes line back on the first point
            gradientPath.close();
        }

        baseLinePath.reset();
        if (adapter.hasBaseLine()) {
            float scaledBaseLine = scaleHelper.getY(adapter.getBaseLine());
            baseLinePath.moveTo(0, scaledBaseLine);
            baseLinePath.lineTo(getWidth(), scaledBaseLine);
        }

        renderPath.reset();
        renderPath.addPath(linePath);

        updateFill();
        updatePointerLocation(-1, -1);
        invalidate();
    }

    private void updateContentRect() {
        if (contentRect == null) return;

        contentRect.set(
                getPaddingStart(),
                getPaddingTop() + lastPointMarkerSpacing(),
                getWidth() - (getPaddingEnd() + lastPointMarkerSpacing()),
                getHeight() - getPaddingBottom()
        );
    }

    private void updatePointerLocation(float x, float y) {
        if (adapter == null) return;
        if (adapter.getCount() < 2) {
            clearData();
            return;
        }

        boolean isReset = x < 0 && y < 0;

        final float lastX =
                x < 0
                        ? scaleHelper.getX(adapter.getX(adapter.getCount()) - 1)
                        : x;
        final float lastY =
                y < 0
                        ? scaleHelper.getY(adapter.getY(adapter.getCount() - 1))
                        : y;

        if (lastPointMarkerEnabled) {
            lastPointMarkerPath.reset();
            lastPointMarkerRipplePath.reset();

            lastPointMarkerRipplePath.addCircle(lastX, lastY, 16f, Path.Direction.CW);
            lastPointMarkerPath.addCircle(lastX, lastY, 8f, Path.Direction.CW);
        }


        if (scrubEnabled && !isReset) {
            scrubPointMarkerPath.reset();
            scrubPointMarkerRipplePath.reset();

            scrubPointMarkerRipplePath.addCircle(lastX, lastY, 16f, Path.Direction.CW);
            scrubPointMarkerPath.addCircle(lastX, lastY, 8f, Path.Direction.CW);
        }
    }

    private void updateFill() {
        linearGradientFill = new LinearGradient(
                0f,
                0f,
                0f,
                getHeight(),
                new int[]{topColorGradientFill, bottomColorGradientFill},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(linearGradientFill);
    }

    public float getScaledX(float x) {
        if (scaleHelper == null) {
            Log.w(TAG, "getScaledX() - no scale available yet.");
            return x;
        }
        return scaleHelper.getX(x);
    }

    public float getScaledY(float y) {
        if (scaleHelper == null) {
            Log.w(TAG, "getScaledX() - no scale available yet.");
            return y;
        }
        return scaleHelper.getY(y);
    }

    private int lastPointMarkerSpacing() {
        return lastPointMarkerEnabled
                ? createSpacing(8)
                : 0;
    }

    @Nullable
    private Float getFillEdge() {
        switch (fillType) {
            case FillType.NONE:
                return null;
            case FillType.UP:
                return (float) getPaddingTop();
            case FillType.DOWN:
                return (float) getHeight() - getPaddingBottom();
            case FillType.TOWARD_ZERO:
                float zero = scaleHelper.getY(0F);
                float bottom = (float) getHeight() - getPaddingBottom();
                return Math.min(zero, bottom);
            default:
                throw new IllegalStateException(
                        String.format(Locale.US, "Unknown fill-type: %d", fillType)
                );
        }
    }

    private void clearData() {
        scaleHelper = null;
        renderPath.reset();
        linePath.reset();
        baseLinePath.reset();
        gradientPath.reset();

        if (lastPointMarkerEnabled) {
            lastPointMarkerPath.reset();
            lastPointMarkerRipplePath.reset();
        }

        invalidate();
    }

    public void setFillType(@FillType int fillType) {
        if (this.fillType != fillType) {
            this.fillType = fillType;
            populatePath();
        }
    }

    public void setAdapter(@Nullable BaseChartAdapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterDataSetObserver(dataSetObserver);
        }
        this.adapter = adapter;
        if (this.adapter != null) {
            this.adapter.registerDataSetObserver(dataSetObserver);
        }
        populatePath();
    }

    private void setScrubLine(float x) {
        x = resolveBoundedScrubLine(x);
        scrubLinePath.reset();
        scrubLinePath.moveTo(x, getPaddingTop());
        scrubLinePath.lineTo(x, getHeight() - getPaddingBottom());
        invalidate();
    }

    private float resolveBoundedScrubLine(float x) {
        float scrubLineOffset = scrubLineWidth / 2;

        float leftBound = getPaddingStart() + scrubLineOffset;
        if (x < leftBound) {
            return leftBound;
        }

        float rightBound = getWidth() - getPaddingEnd() - scrubLineOffset;
        if (x > rightBound) {
            return rightBound;
        }

        return x;
    }

    @Override
    public void onScrubbed(float x, float y) {
        if (adapter == null || adapter.getCount() == 0) return;

        int index = getNearestIndex(xPoints, x);

        if (scrubListener != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (scrubListener != null) {
                scrubListener.onScrubbed(adapter.getItem(index));
            }
        }

        setScrubLine(x);
        updatePointerLocation(scaleHelper.getX(index), scaleHelper.getY(adapter.getY(index)));
    }

    @Override
    public void onScrubEnded() {
        scrubLinePath.reset();
        scrubPointMarkerPath.reset();
        scrubPointMarkerRipplePath.reset();
        if (scrubListener != null) scrubListener.onScrubbed(null);
        if (lastPointMarkerPath != null) lastPointMarkerPath.reset();
        if (lastPointMarkerRipplePath != null) lastPointMarkerRipplePath.reset();
        updatePointerLocation(-1, -1);
        invalidate();
    }


    private final DataSetObserver dataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            populatePath();

//			if (sparkAnimator != null) {
//				doPathAnimation();
//			}
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            clearData();
        }
    };


}
