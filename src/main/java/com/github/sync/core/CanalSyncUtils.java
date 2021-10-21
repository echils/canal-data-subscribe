package com.github.sync.core;

/**
 * 数据同步工具类
 *
 * @author echils
 */
public class CanalSyncUtils {


    /**
     * 数据库字段转类属性名
     *
     * @param str 源字符串
     */
    public static String camelUnderscoreToCase(String str) {
        return camelUnderscoreToCase(str, false);
    }


    /**
     * 下划线转驼峰
     *
     * @param str                源字符串
     * @param firstCharUpperCase 首字母是否大写
     */
    public static String camelUnderscoreToCase(String str, boolean firstCharUpperCase) {
        char[] chars = str.toCharArray();
        StringBuilder result = new StringBuilder();
        boolean beforeUnderscore = firstCharUpperCase;
        for (char c : chars) {
            if (c == '_') {
                beforeUnderscore = true;
            } else {
                result.append(beforeUnderscore ? Character.toUpperCase(c) : c);
                beforeUnderscore = false;
            }
        }
        return result.toString();
    }

}
