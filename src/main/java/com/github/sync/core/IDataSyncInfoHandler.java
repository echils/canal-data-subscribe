package com.github.sync.core;

import com.github.sync.model.DataOperation;
import com.github.sync.model.ReceiptAddress;

import java.util.List;


/**
 * 数据同步处理器
 *
 * @author echils
 */
public interface IDataSyncInfoHandler {


    /**
     * 数据最大推送量
     */
    int DATA_BATCH_SIZE = 20;


    /**
     * 数据处理
     *
     * @param schema             数据库名称
     * @param table              表名称
     * @param dataList           数据列表
     * @param receiptAddressList 接收列表
     */
    void handle(String schema, String table, List<DataOperation> dataList, List<ReceiptAddress> receiptAddressList);


    /**
     * 支持的处理器类型
     */
    boolean match(HandlerType handlerType);


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
