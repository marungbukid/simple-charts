package com.marungbukid.charts;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collection;

public abstract class BaseChartAdapter<T extends BaseChartEntry> {
    private final DataSetObservable observable = new DataSetObservable();
    private OnDataCharts<T> onDataCharts;

    /**
     * @return the number of points to be drawn.
     */
    public abstract int getCount();

    /**
     * @return the object at the given index.
     */
    public abstract T getItem(int index);

    /**
     * @return the float representation of the X value of the point at the given index.
     */
    public float getX(int index) {
        return (float) index;
    }

    /**
     * @return the float representation of the Y value of the point at the given index.
     */
    public abstract float getY(int index);

    public void setOnDataCharts(OnDataCharts<T> onDataCharts) {
        this.onDataCharts = onDataCharts;
    }

    public OnDataCharts<T> getOnDataCharts() {
        return onDataCharts;
    }

    @NonNull
    public RectF getDataBounds() {
        final int count = getCount();
        final boolean hasBaseLine = hasBaseLine();

        float minY = hasBaseLine ? getBaseLine() : Float.MAX_VALUE;
        float maxY = hasBaseLine ? minY : -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            final float x = getX(i);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);

            final float y = getY(i);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        // set values on the return object
        return createRectF(minX, minY, maxX, maxY);
    }

    @VisibleForTesting
    RectF createRectF(float left, float top, float right, float bottom) {
        return new RectF(left, top, right, bottom);
    }

    public boolean hasBaseLine() {
        return false;
    }

    public float getBaseLine() {
        return 0;
    }

    public final void notifyDataSetChanged() {
        observable.notifyChanged();
    }

    public final void notifyDataSetInvalidated() {
        observable.notifyInvalidated();
    }

    public final void registerDataSetObserver(DataSetObserver observer) {
        observable.registerObserver(observer);
    }

    public final void unregisterDataSetObserver(DataSetObserver observer) {
        observable.unregisterObserver(observer);
    }
}
