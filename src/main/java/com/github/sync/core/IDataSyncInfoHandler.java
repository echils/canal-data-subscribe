package com.github.sync.core;

import com.github.sync.model.ReceiptAddress;

import java.util.List;

import static com.github.sync.core.CanalDataSyncContext.*;

/**
 * 数据同步处理器
 *
 * @author echils
 */
public interface IDataSyncInfoHandler {


    /**
     * 数据处理
     *
     * @param schema             数据库名称
     * @param table              表名称
     * @param sqlInfos           SQL列表
     * @param receiptAddressList 接收列表
     */
    void handle(String schema, String table, List<String> sqlInfos, List<ReceiptAddress> receiptAddressList);


    /**
     * 支持的处理器类型
     */
    boolean match(HandlerType handlerType);


    /**
     * 将本地数据库和表名替换成目标数据库和目标表名
     *
     * @param sourceSql    源SQL
     * @param sourceSchema 源数据库名称
     * @param sourceTable  源数据库表名
     * @param targetSchema 目标数据库名称
     * @param targetTable  目标数据库表名
     */
    default String conversion(String sourceSql, String sourceSchema, String sourceTable,
                              String targetSchema, String targetTable) {

        return sourceSql.replaceAll(filedTagFunction.apply(sourceSchema)
                        + SCHEMA_TABLE_SEPARATOR + filedTagFunction.apply(sourceTable),
                filedTagFunction.apply(targetSchema)
                        + SCHEMA_TABLE_SEPARATOR + filedTagFunction.apply(targetTable));
    }


    /**
     * 处理器类型
     */
    enum HandlerType {

        /**
         * Kafka消息队列
         */
        KAFKA,

        /**
         * Http协议
         */
        HTTP
    }


}
