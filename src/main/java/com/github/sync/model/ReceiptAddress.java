package com.github.sync.model;

import com.github.sync.core.IDataSyncInfoHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 接收地址
 *
 * @author echils
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptAddress {

    /**
     * 推送服务器地址: MQ服务器或者API服务器
     */
    private String host;

    /**
     * MQ主题或者API(POST请求，数据结构{@link DataSyncMessage})
     */
    private String channel;

    /**
     * 推送类型
     */
    private IDataSyncInfoHandler.HandlerType type;

    /**
     * 目标数据库名称
     */
    private String schema;

    /**
     * 目标表名称
     */
    private String table;

    /**
     * 用户自定义标签，如果未设置将默认设置为订阅ID
     */
    private String subscriptionTag;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReceiptAddress that = (ReceiptAddress) o;
        return host.equals(that.host) &&
                channel.equals(that.channel) &&
                type == that.type &&
                schema.equals(that.schema) &&
                table.equals(that.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, channel, type, schema, table);
    }

}
