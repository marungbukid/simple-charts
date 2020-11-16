package com.marungbukid.charts;

public abstract class BaseChartEntry {
    public int getIndex() {
        return Integer.MIN_VALUE;
    }

    public float getValue() {
        return Float.MIN_VALUE;
    }

    public long getDateTime() {
        return Long.MIN_VALUE;
    }

}
