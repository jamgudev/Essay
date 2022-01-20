package com.example.essay.rub;

public class YearInfo {


    public YearInfo(String year, float temp) {
        this.year = year;
        this.temp = temp;
    }

    String year = "";
    float temp = 0.0f;

    public static YearInfo parseInfo(String infoStr) {
        if (null == infoStr || "".equals(infoStr)) {
            return null;
        }

        String[] infoSplits = infoStr.split(",");
        if (null != infoSplits && infoSplits.length == 2) {
            return new YearInfo(infoSplits[0], Float.parseFloat(infoSplits[1]));
        }

        return null;
    }


    @Override
    public String toString() {
        return "YearInfo{" +
                "year='" + year + '\'' +
                ", temp=" + temp +
                '}';
    }
}
    