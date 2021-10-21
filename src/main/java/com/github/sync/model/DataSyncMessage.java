package com.github.sync.model;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 消息体
 *
 * @author echils
 */
@Data
public class DataSyncMessage {

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 采集时间
     */
    private Date createTime;

    /**
     * 消息体
     */
    private Meta data;


    @Data
    public static class Meta {

        /**
         * 用户自定义标签，如果未设置将默认设置为订阅ID
         */
        private String subscriptionTag;

        /**
         * 数据
         */
        private List<DataOperation> dataList;

        public Meta(String subscriptionTag, List<DataOperation> dataList) {
            this.subscriptionTag = subscriptionTag;
            this.dataList = dataList;
        }

    }

    public static DataSyncMessage newInstance(String subscriptionTag, List<DataOperation> dataList) {
        DataSyncMessage dataSyncMessage = new DataSyncMessage();
        dataSyncMessage.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        dataSyncMessage.setCreateTime(new Date());
        dataSyncMessage.setData(new Meta(subscriptionTag, dataList));
        return dataSyncMessage;
    }

}
