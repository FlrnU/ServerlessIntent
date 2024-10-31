package org.example.model;

public class ServiceLimits {
    private int limit;
    private String unit;

    public ServiceLimits(int limit, String unit) {
        this.limit = limit;
        this.unit = unit;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "ServiceLimits{" +
               "limit=" + limit +
               ", unit='" + unit + '\'' +
               '}';
    }
}
