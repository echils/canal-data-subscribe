package com.github.sync.service;

import com.github.sync.model.SubscribeStatus;
import com.github.sync.model.Subscription;

import java.util.List;

/**
 * 同步业务
 *
 * @author echils
 */
public interface ISyncService {


    /**
     * 订阅目标分隔符
     */
    String SCHEMA_TABLE_SEPARATOR = ".";


    /**
     * 新增订阅
     *
     * @param subscription 订阅信息
     */
    String subscribe(Subscription subscription);


    /**
     * 暂停订阅
     *
     * @param subscriptionId 订阅唯一标识
     */
    void pause(String subscriptionId);


    /**
     * 开启订阅
     *
     * @param subscriptionId 订阅唯一标识
     */
    void restart(String subscriptionId);


    /**
     * 取消订阅
     *
     * @param subscriptionId 订阅唯一标识
     */
    void unsubscribe(String subscriptionId);


    /**
     * 查询所有订阅
     *
     * @param id          唯一标识
     * @param destination Canal实例名称
     * @param schema      数据库名称
     * @param table       表名称
     * @param status      监测状态
     */
    List<Subscription> findAllSubscriptions(String id,
                                            String destination,
                                            String schema,
                                            String table,
                                            SubscribeStatus status);

}
