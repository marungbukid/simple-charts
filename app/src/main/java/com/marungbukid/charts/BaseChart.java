package com.marungbukid.charts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.FontRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.marungbukid.charts.gestures.ScrubGestureDetector;
import com.marungbukid.charts.line.LineChartEntry;
import com.marungbukid.util.ColorUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class BaseChart extends View implements ScrubGestureDetector.ScrubListener {
    private static final String TAG = "BaseChart";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ChartRange.YTD,
            ChartRange.ONE_DAY,
            ChartRange.ONE_MONTH,
            ChartRange.THREE_MONTHS,
            ChartRange.SIX_MONTHS,
            ChartRange.ONE_YEAR,
            ChartRange.THREE_YEARS,
            ChartRange.FIVE_YEARS
    })
    public @interface ChartRange {
        int YTD = -1;
        int ONE_DAY = 0;
        int ONE_MONTH = 1;
        int THREE_MONTHS = 3;
        int SIX_MONTHS = 6;
        int ONE_YEAR = 12;
        int THREE_YEARS = 36;
        int FIVE_YEARS = 60;
    }

    private static final int MIN_TEXT_SIZE = 12;
    private static final String PRICE_FORMAT = "##,##0.00";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private boolean textInvolved = false;

    private boolean hasPriceAxis = false;
    private boolean hasVolumeBars = false;
    private boolean hasDateTimeIndicators = false;

    private final int priceSteps = 4;
    private final int dateTimeSteps = 3;

    private int contentPaddingEnd = 0;
    private int contentPaddingBottom = 0;

    @ChartRange
    private int chartRange = ChartRange.YTD;

    // styleable values
    @ColorInt
    private int priceAxisTextColor;
    private float priceAxisTextSize;
    @ColorInt
    private int priceAxisDividerColor = 0;
    private int dateTimeTextColor;
    private float dateTimeTextSize;
    @ColorInt
    private int dateTimeDividerColor = 0;
    @ColorInt
    private int volumeBarColor;
    @FontRes
    private int fontId;

    // misc fields
    protected ScaleHelper scaleHelper;
    @NonNull
    private final TextPaint priceAxisTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final TextPaint dateTimeAxisTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    protected @NonNull
    ScrubGestureDetector scrubGestureDetector;
    protected @Nullable
    OnScrubListener scrubListener;

    protected BaseChartAdapter adapter;


    public interface OnScrubListener {
        /**
         * Indicates the user is currently scrubbing over the given value. A null value indicates
         * that the user has stopped scrubbing.
         */
        void onScrubbed(@Nullable Object value);

    }

    public BaseChart(Context context) {
        super(context);
        _init(context, null, R.attr.stockCharts_BaseChartStyle, R.style.stockCharts_Base);
    }

    public BaseChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        _init(context, attrs, R.attr.stockCharts_BaseChartStyle, R.style.stockCharts_Base);
    }

    public BaseChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init(context, attrs, defStyleAttr, R.style.stockCharts_Base);
    }

    public BaseChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        _init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void _init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.BaseChart, defStyleAttr, defStyleRes);

        int defaultColor =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? context.getResources().getColor(android.R.color.black, null)
                        : context.getResources().getColor(android.R.color.black);

        hasPriceAxis = a.getBoolean(R.styleable.BaseChart_charts_hasPriceAxis, false);
        hasDateTimeIndicators = a.getBoolean(R.styleable.BaseChart_charts_hasDateTimeIndicator, false);
        hasVolumeBars = a.getBoolean(R.styleable.BaseChart_charts_hasVolumeBars, false);
        fontId = a.getResourceId(R.styleable.BaseChart_android_fontFamily, 0);

        if (hasPriceAxis) {
            priceAxisDividerColor = a.getColor(R.styleable.BaseChart_charts_priceAxisDividerColor, 0);
            priceAxisTextColor = a.getColor(R.styleable.BaseChart_charts_priceAxisTextColor, defaultColor);
            priceAxisTextSize = (float) a.getDimensionPixelSize(R.styleable.BaseChart_charts_priceAxisTextSize, 0);
        }

        if (hasDateTimeIndicators) {
            dateTimeDividerColor = a.getColor(R.styleable.BaseChart_charts_dateTimeAxisDividerColor, 0);
            dateTimeTextColor = a.getColor(R.styleable.BaseChart_charts_dateTimeTextColor, defaultColor);
            dateTimeTextSize = (float) a.getDimensionPixelSize(R.styleable.BaseChart_charts_dateTimeTextSize, 0);
        }

        if (hasVolumeBars) {
            volumeBarColor = a.getColor(R.styleable.BaseChart_charts_volumeBarColor, 0);
        }

        a.recycle();

        textInvolved = hasPriceAxis || hasDateTimeIndicators;

        if (hasPriceAxis) {
            priceAxisTextPaint.setStyle(Paint.Style.FILL);
            priceAxisTextPaint.setColor(priceAxisTextColor);
            if (fontId != 0 && !isInEditMode()) {
                priceAxisTextPaint.setTypeface(ResourcesCompat.getFont(context, fontId));
            }

            if (priceAxisTextSize > 0) {
                priceAxisTextPaint.setTextSize(priceAxisTextSize);
            } else {
                priceAxisTextPaint.setTextSize(getMinTextWidth());
            }

            contentPaddingEnd = createTextLayout(PRICE_FORMAT, priceAxisTextPaint)
                    .getWidth();
        }

        if (hasDateTimeIndicators) {
            dateTimeAxisTextPaint.setStyle(Paint.Style.FILL);
            dateTimeAxisTextPaint.setColor(dateTimeTextColor);
            if (fontId != 0 && !isInEditMode()) {
                dateTimeAxisTextPaint.setTypeface(ResourcesCompat.getFont(context, fontId));
            }


            if (dateTimeTextSize > 0) {
                dateTimeAxisTextPaint.setTextSize(dateTimeTextSize);
            } else {
                dateTimeAxisTextPaint.setTextSize(getMinTextWidth());
            }

            contentPaddingBottom = createTextLayout(DATE_TIME_FORMAT, dateTimeAxisTextPaint)
                    .getHeight();
        }
    }

    protected abstract void populatePath();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (scaleHelper != null) {
            drawPriceMarkers(canvas);
//			drawDateTimeIndicators(canvas);
//			drawVolumeBars(canvas);
        }
    }

    private StaticLayout createTextLayout(String text, TextPaint textPaint) {
        int width = (int) textPaint.measureText(text);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return StaticLayout.Builder
                    .obtain(text, 0, text.length(), textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .build();
        } else {
            return new StaticLayout(
                    text,
                    textPaint,
                    width,
                    Layout.Alignment.ALIGN_NORMAL,
                    1f,
                    0,
                    false
            );
        }
    }

    public boolean isHasPriceAxis() {
        return hasPriceAxis;
    }

    public void setHasPriceAxis(boolean hasPriceAxis) {
        this.hasPriceAxis = hasPriceAxis;
    }

    public boolean isHasVolumeBars() {
        return hasVolumeBars;
    }

    public void setHasVolumeBars(boolean hasVolumeBars) {
        this.hasVolumeBars = hasVolumeBars;
    }

    public boolean isHasDateTimeIndicators() {
        return hasDateTimeIndicators;
    }

    public int getContentPaddingEnd() {
        return contentPaddingEnd;
    }

    public int getContentPaddingBottom() {
        return contentPaddingBottom;
    }

    public void setHasDateTimeIndicators(boolean hasDateTimeIndicators) {
        this.hasDateTimeIndicators = hasDateTimeIndicators;
    }

    /**
     * If hasPriceAxis is enabled in XML, display prices in right side
     */
    private void drawPriceMarkers(Canvas canvas) {
        if (!hasPriceAxis) return;
        if (adapter.getOnDataCharts() == null) return;

        float maxPrice = -1;
        float minPrice = -1;

        DecimalFormat format = new DecimalFormat(PRICE_FORMAT);

        // get additional height spacing for texts
        int textHeight = createTextLayout("0", priceAxisTextPaint).getHeight();

        // position of price data in the charts
        int priceMarkersXPos = getWidth()
                - getPaddingEnd()
                + dataSpacing();

        // initialize divider paint
        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(priceAxisDividerColor);
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(0.5f);

        // initialize grid-Y paint
        Paint gridYPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridYPaint.setColor(ColorUtil.colorOpacity(priceAxisDividerColor, 0.4f));
        gridYPaint.setStyle(Paint.Style.STROKE);
        gridYPaint.setStrokeWidth(0.5f);

        Path gridYPath = new Path();
        Path dividerYPath = new Path();

        // set ingress point for price marker divider
        int dividerXPos = priceMarkersXPos - dataSpacing();

        // create price marker divider with spacing
        canvas.drawLine(
                dividerXPos,
                0,
                dividerXPos,
                getHeight(),
                dividerPaint
        );

        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i) instanceof LineChartEntry) {
                LineChartEntry entry = (LineChartEntry) adapter.getItem(i);

                if (maxPrice < 0) maxPrice = entry.getValue();
                if (minPrice < 0) minPrice = entry.getValue();

                maxPrice = Math.max(maxPrice, entry.getValue());
                minPrice = Math.min(minPrice, entry.getValue());
            }
//			else {
//				CandlestickChartEntry entry = (CandlestickChartEntry) adapter.getItem(i);
//
//				if (maxPrice < 0) maxPrice = entry.getHigh();
//				if (minPrice < 0) minPrice = entry.getLow();
//
//				maxPrice = Math.max(Math.max(Math.max(maxPrice, entry.getHigh()), entry.getLow()), Math.max(entry.getLow(), entry.getClose()));
//				minPrice = Math.min(Math.min(Math.min(minPrice, entry.getHigh()), entry.getLow()), Math.min(entry.getLow(), entry.getClose()));
//			}
        }

        final int markerSize = 6;
        float bucketSize = (maxPrice - minPrice) / markerSize;
        float curPrice = minPrice + bucketSize;

        for (int i = 0; i < markerSize; i++) {
            if (i > 0 && i < markerSize - 1) {
                curPrice += bucketSize;
                float yPos = scaleHelper.getY(curPrice) - getPaddingBottom();

                gridYPath.moveTo(0, yPos);
                gridYPath.lineTo(dividerXPos, yPos);

                canvas.save();
                canvas.translate(priceMarkersXPos, yPos - (float) (textHeight / 2));
                createTextLayout(format.format(curPrice), priceAxisTextPaint)
                        .draw(canvas);
                canvas.restore();

                dividerYPath.moveTo(dividerXPos, yPos);
                dividerYPath.lineTo(dividerXPos + createSpacing(4), yPos);
            }
        }

        canvas.drawPath(gridYPath, gridYPaint);
        canvas.drawPath(dividerYPath, dividerPaint);
    }

    /**
     * If hasDateTimeIndicator is enabled in XML, display date/time in bottom
     */
//	private void drawDateTimeIndicators(Canvas canvas) {
//		if (!hasDateTimeIndicators) return;
//		if (adapter.getOnDataCharts() == null) return;
//
//		SimpleDateFormat dateFormat = new SimpleDateFormat(getDateTimeFormatByChartRange(), Locale.getDefault());
//
//		int dateMarkersYPos = getHeight() - getContentPaddingBottom();
//
//		int dividerYPos = dateMarkersYPos - createSpacing(4);
//
//		// initialize date divider axis
//		Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		dividerPaint.setColor(dateTimeDividerColor);
//		dividerPaint.setStyle(Paint.Style.STROKE);
//		dividerPaint.setStrokeWidth(0.5f);
//
//		// initialize grid paint
//		Paint gridXPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		gridXPaint.setColor(ColorUtil.colorOpacity(dateTimeDividerColor, 0.4f));
//		gridXPaint.setStyle(Paint.Style.STROKE);
//		gridXPaint.setStrokeWidth(0.5f);
//		gridXPaint.setStrokeCap(Paint.Cap.ROUND);
//		gridXPaint.setPathEffect(new DashPathEffect(new float[] { 4f, 8f }, 0));
//
//		Path gridXPath = new Path();
//		Path dividerPath = new Path();
//
//		// draw date time line divider
//		canvas.drawLine(
//			0,
//			dividerYPos,
//			getWidth(),
//			dividerYPos,
//			dividerPaint
//		);
//
//		List dateEntries = adapter.getOnDataCharts().getDatePoints();
//
//		if (dateEntries.size() < 2) return;
//
//		for (int i = 0; i < dateEntries.size(); i++) {
//			BaseChartEntry entry = (BaseChartEntry) dateEntries.get(i);
//
//			if (entry != null) {
//				float xPos = scaleHelper.getX(entry.getIndex());
//				xPos = entry instanceof CandlestickChartEntry ? xPos + 4f : xPos;
//
//				// draw date time line markers
//				dividerPath.moveTo(xPos, dividerYPos);
//				dividerPath.lineTo(xPos,dividerYPos + createSpacing(4));
//
//				// draw date time grid
//				gridXPath.moveTo(xPos, 0);
//				gridXPath.lineTo(xPos, dividerYPos);
//
//				// create date/time static text layout
//				StaticLayout dateTimeText = createTextLayout(dateFormat.format(new Date(entry.getDateTime())), dateTimeAxisTextPaint);
//
//				canvas.save();
//				canvas.translate(xPos - (float) (dateTimeText.getWidth() / 2), dateMarkersYPos);
//
//				// draw date time
//				dateTimeText.draw(canvas);
//
//				canvas.restore();
//			}
//		}
//
//		canvas.drawPath(dividerPath, dividerPaint);
//		canvas.drawPath(gridXPath, gridXPaint);
//	}

//	private void drawVolumeBars(Canvas canvas) {
//		if (!hasVolumeBars) return;
//
//		Paint volumeBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		volumeBarPaint.setColor(ColorUtil.colorOpacity(volumeBarColor, 0.4f));
//		volumeBarPaint.setStyle(Paint.Style.FILL_AND_STROKE);
//
//		Path volumeBarPath = new Path();
//
//		final float yPos = (getHeight() - getContentPaddingBottom()) - createSpacing(4);
//		final float maxVolumeBarHeight = createSpacing(64);
//
//		float maxVolumeValue = 0f;
//
//		for (int i = 0; i < adapter.getCount(); i++) {
//			final BaseChartEntry item = adapter.getItem(i);
//
//			maxVolumeValue = Math.max(maxVolumeValue, item.getValue());
//		}
//
//		for (int i = 0; i < adapter.getCount(); i++) {
//			final BaseChartEntry item = adapter.getItem(i);
//			if (!(item instanceof CandlestickChartEntry)) return;
//
//			final CandlestickChartEntry entry = (CandlestickChartEntry) item;
//			final float xPos = scaleHelper.getX(entry.getIndex());
//			final float volume = entry.getValue();
//
//			volumeBarPath.addRect(
//				xPos,
//				yPos - ((volume / maxVolumeValue) * maxVolumeBarHeight),
//				xPos + 8f,
//				yPos,
//				Path.Direction.CW
//			);
//		}
//
//		canvas.drawPath(volumeBarPath, volumeBarPaint);
//	}
    @ChartRange
    public int getChartRange() {
        return chartRange;
    }

    public void setChartRange(@ChartRange int chartRange) {
        this.chartRange = chartRange;
        invalidate();
    }

    private String getDateTimeFormatByChartRange() {
        switch (chartRange) {
            case ChartRange.ONE_MONTH:
            case ChartRange.ONE_YEAR:
            case ChartRange.SIX_MONTHS:
            case ChartRange.THREE_MONTHS:
                return "MMM-yyyy";

            case ChartRange.THREE_YEARS:
            case ChartRange.FIVE_YEARS:
                return "yyyy";

            case ChartRange.YTD:
                return "MMM dd";

            case ChartRange.ONE_DAY:
                return "hh:mm a";
            default:
                throw new IllegalStateException(
                        String.format(Locale.US, "Unknown chart range: %d", chartRange)
                );
        }
    }


    @Override
    public int getPaddingEnd() {
        return super.getPaddingEnd()

                // create additional spacing for price axis (x-axis on right) if enabled
                + (hasPriceAxis
                ? getContentPaddingEnd() + dataSpacing()
                : 0);
    }

    public int getPaddingBottom() {
        return super.getPaddingBottom()

                // create additional spacing for date time axis (y-axis on bottom)
                + (hasDateTimeIndicators
                ? getContentPaddingBottom() + dataSpacing()
                : 0);
    }

    public int dataSpacing() {
        return textInvolved
                ? createSpacing(4)
                : 0;
    }

    public int createSpacing(int sizeInDensity) {
        return (int) (sizeInDensity * getResources().getDisplayMetrics().density);
    }

    private float getMinTextWidth() {
        return MIN_TEXT_SIZE * getResources().getDisplayMetrics().density;
    }

    @Nullable
    public OnScrubListener getScrubListener() {
        return scrubListener;
    }

    public void setScrubListener(@Nullable OnScrubListener scrubListener) {
        this.scrubListener = scrubListener;
    }

    public static class ScaleHelper {
        // the width and height of the view
        final float width, height;
        final int size;
        // the scale factor for the Y values
        final float xScale, yScale;
        // translates the Y values back into the bounding rect after being scaled
        final float xTranslation, yTranslation;

        final float topPadding, leftPadding, rightPadding;

        public ScaleHelper(BaseChartAdapter adapter, RectF contentRect, float lineWidth, boolean fill) {
            leftPadding = contentRect.left;
            topPadding = contentRect.top;
            rightPadding = contentRect.right;

            // subtract lineWidth to offset for 1/2 of the line bleeding out of the content box on
            // either side of the view
            final float lineWidthOffset = fill ? 0 : lineWidth;
            this.width = contentRect.width() - lineWidthOffset;
            this.height = contentRect.height() - lineWidthOffset;

            this.size = adapter.getCount();

            // get data bounds from adapter
            RectF bounds = adapter.getDataBounds();

            // if data is a line (which technically has no size), expand bounds to center the data
            bounds.inset(bounds.width() == 0 ? -1 : 0, bounds.height() == 0 ? -1 : 0);

            final float minX = bounds.left;
            final float maxX = bounds.right;
            final float minY = bounds.top;
            final float maxY = bounds.bottom;

            // xScale will compress or expand the min and max x values to be just inside the view
            this.xScale = width / (maxX - minX);
            // xTranslation will move the x points back between 0 - width
            this.xTranslation = leftPadding - (minX * xScale) + (lineWidthOffset / 2);
            // yScale will compress or expand the min and max y values to be just inside the view
            this.yScale = height / (maxY - minY);
            // yTranslation will move the y points back between 0 - height
            this.yTranslation = minY * yScale + topPadding + (lineWidthOffset / 2);
        }

        /**
         * Given the 'raw' X value, scale it to fit within our view.
         */
        public float getX(float rawX) {
            return rawX * xScale + xTranslation;
        }

        /**
         * Given the 'raw' Y value, scale it to fit within our view. This method also 'flips' the
         * value to be ready for drawing.
         */
        public float getY(float rawY) {
            return height - (rawY * yScale) + yTranslation;
        }

        public float getTopPadding() {
            return topPadding;
        }

        public float getRightPadding() {
            return rightPadding;
        }
    }

    public static int getNearestIndex(List<Float> points, float point) {
        int index = Collections.binarySearch(points, point);

        // if binary search returns positive, we had an exact match, return that index
        if (index >= 0) return index;

        // otherwise, calculate the binary search's specified insertion index
        index = -1 - index;

        // if we're inserting at 0, then our guaranteed nearest index is 0
        if (index == 0) return index;

        // if we're inserting at the very end, then our guaranteed nearest index is the final one
        if (index == points.size()) return --index;

        // otherwise we need to check which of our two neighbors we're closer to
        final float deltaUp = points.get(index) - point;
        final float deltaDown = point - points.get(index - 1);
        if (deltaUp > deltaDown) {
            // if the below neighbor is closer, decrement our index
            index--;
        }

        return index;
    }

}
