package com.aus.framework.common.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;

public class NumberUtils {

    /**
     * 数字转换字符串
     * @param number
     * @return
     */
    public static String formatNumberString(long number) {
        if (number < 10000) {
            return String.valueOf(number);  // 小于 10000 显示原始数字
        } else if (number < 100000000) {
            // 小于 1 亿，显示万单位
            double result = number / 10000.0;
            DecimalFormat df = new DecimalFormat("#.#");  // 保留 1 位小数
            df.setRoundingMode(RoundingMode.DOWN);  // 向下取整
            String formatted = df.format(result);
            return formatted + "万";
        } else {
            return "9999万"; // 超过 1 亿，统一显示 9999万
        }
    }

}