package com.github.sync.core;

import org.apache.commons.lang.StringUtils;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 数据解析全局上下文
 *
 * @author echils
 */
public class CanalDataSyncContext {

    /**
     * 实例过滤条件分隔符
     */
    public static final String SUBSCRIBE_FILTER_SEPARATOR = ",";

    /**
     * 推送列表KEY分隔符
     */
    public static final String SCHEMA_TABLE_SEPARATOR = ".";

    /**
     * 定义空变量
     */
    public static final String BLANK = "";

    /**
     * SQL中用于标记为字段的符号
     */
    public static final String SQL_FIELD_SEPARATOR = "`";

    /**
     * SQL字段标签函数
     */
    public static final Function<String, String> filedTagFunction = (filed) -> SQL_FIELD_SEPARATOR + filed + SQL_FIELD_SEPARATOR;

    /**
     * SQL字段值类型包装函数
     * T: column mysqlType
     * U: column source value
     * R: column wrapped value
     */
    public static final BiFunction<String, String, String> valueWrapFunction = (jdbcType, value) -> {
        if (StringUtils.isBlank(value)) {
            return "NULL";
        }
        if (jdbcType.contains("bit")) {
            return "b" + "'" + value + "'";
        }
        if (jdbcType.contains("varchar") || jdbcType.contains("char") || jdbcType.contains("date")
                || jdbcType.contains("enum") || jdbcType.contains("time") || jdbcType.contains("text")) {
            return "'" + value + "'";
        }
//        if (jdbcType.contains("bigint") || jdbcType.contains("tinyint") || jdbcType.contains("decimal")
//                || jdbcType.contains("double") || jdbcType.contains("float") || jdbcType.contains("int")
//                || jdbcType.contains("year") || jdbcType.contains("smallint")) {
//            return value;
//        }
//        if (jdbcType.contains("binary")) {
//            return "BINARY(" + value + ")";
//        }
//        if (jdbcType.contains("varbinary")) {
//            return "VARBINARY(" + value + ")";
//        }
//        if (jdbcType.contains("blob")) {
//            return value;
//        }
//        if (jdbcType.contains("geometry")) {
//            return value;
//        }
//        if (jdbcType.contains("linestring")) {
//            return value;
//        }
//        if (jdbcType.contains("multipoint")) {
//            return value;
//        }
//        if (jdbcType.contains("polygon")) {
//            return value;
//        }
//        if (jdbcType.contains("set")) {
//            return value;
//        }
        return value;
    };

}
