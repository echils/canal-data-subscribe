package com.github.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
 * 订阅信息
 *
 * @author echils
 */
@Data
public class Subscription {

    /**
     * 唯一标识
     */
    private String id;

    /**
     * Canal实例名称
     */
    private String destination;

    /**
     * 数据库名称
     */
    private String schema;

    /**
     * 表名称
     */
    private String table;

    /**
     * 消息推送地址
     */
    private ReceiptAddress receiptAddress;

    /**
     * 订阅状态
     */
    private SubscribeStatus status;

    /**
     * 创建时间
     */
    private Date createTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return destination.equals(that.destination) &&
                schema.equals(that.schema) &&
                table.equals(that.table) &&
                receiptAddress.equals(that.receiptAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, schema, table, receiptAddress);
    }

    @JsonIgnore
    public boolean isBlank(){
        return StringUtils.isBlank(destination) || StringUtils.isBlank(schema) ||
                StringUtils.isBlank(table) || receiptAddress == null;
    }

}
