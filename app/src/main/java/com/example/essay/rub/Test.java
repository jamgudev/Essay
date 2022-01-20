package com.example.essay.rub;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by jamgu on 2021/11/02 
 */
public class Test {

    public static void analysisString(String text) {
        if (null == text || "".equals(text)) {
            return;
        }
        String[] infoSplits = text.trim().split(";");

        if (null == infoSplits || infoSplits.length == 0) {
            return;
        }
        // 存储11.5度的年份
        ArrayList<YearInfo> yearOf11 = new ArrayList<>();

        // 解析字符串
        ArrayList<YearInfo> yearInfos = new ArrayList<>(infoSplits.length);
        for (String infoSplit : infoSplits) {
            YearInfo info = YearInfo.parseInfo(infoSplit);
            insert(yearInfos, info);

            // find year of 11.5
            if (info.temp == 11.5f) {
                yearOf11.add(info);
            }
        }

        if (yearInfos.isEmpty()) {
            return;
        }

        // 输出最高和最低温度年份
        System.out.println(String.format(Locale.getDefault(), "最高温度年份：%s, 最低温度年份：%s",
                yearInfos.get(0).year, yearInfos.get(Math.max(yearInfos.size() - 1, 0)).year));

        // 输出11.5度的年份
        System.out.println("11.5度年份有：");
        for (YearInfo yearInfo : yearOf11) {
            System.out.println(yearInfo.year);
        }

        // 顺序输出
        System.out.println("温度从高到低顺序输出：");
        for (YearInfo yearInfo : yearInfos) {
            System.out.println(yearInfo.year + ", " + yearInfo.temp);
        }
    }

    /**
     * 根据温度升序插入
     */
    private static void insert(ArrayList<YearInfo> yearInfos, YearInfo newOne) {
        if (newOne == null) return;

        if (yearInfos == null) {
            yearInfos = new ArrayList<>();
        }

        if (yearInfos.isEmpty()) {
            yearInfos.add(newOne);
        } else {
            int insertIdx = -1;
            // 从列表中找到第一个比newOne温度小的位置，记录它的索引
            for (int i = 0; i < yearInfos.size(); i++) {
                YearInfo curYearInfo = yearInfos.get(i);
                if (curYearInfo.temp <= newOne.temp) {
                    insertIdx = i;
                    break;
                }
            }

            // 将newOne插入到指定位置
            if (insertIdx >= 0) {
                yearInfos.add(insertIdx, newOne);
            } else {
                // 没找到比newOne小的，直接插入到最后面
                yearInfos.add(newOne);
            }
        }
    }

}

