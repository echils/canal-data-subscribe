package com.github.sync.core;

import com.alibaba.otter.canal.client.CanalConnector;
import com.github.sync.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据同步中心
 *
 * @author echils
 */
@Slf4j
@Component
public class CanalDataSyncExecutor {


    @Autowired
    private CanalConnectorFactory canalConnectorFactory;

    @Autowired
    private CanalProperties canalProperties;

    @Autowired
    private List<IDataSyncInfoHandler> dataSyncInfoHandlers = new ArrayList<>();

    /**
     * 缓存每个实例的同步任务
     */
    private static final Map<String, DefaultCanalTask> destinationTaskMap = new ConcurrentHashMap<>();


    /**
     * 添加订阅，需要时添加同步任务
     */
    public void addSubscription(Subscription subscription) {
        if (subscription != null) {
            String destination = subscription.getDestination();
            if (!destinationTaskMap.containsKey(destination)) {
                CanalConnector canalConnector = canalConnectorFactory.create(
                        canalProperties.getIpv4(),
                        canalProperties.getPort(),
                        canalProperties.getUsername(),
                        canalProperties.getPassword(), destination);
                destinationTaskMap.put(destination, new DefaultCanalTask(canalConnector,
                        subscription, dataSyncInfoHandlers, canalProperties.getBatchSize()));
                return;
            }
            destinationTaskMap.get(destination).addSubscription(subscription);
        }
    }


    /**
     * 批量添加添加订阅，需要时添加同步任务
     */
    public void addSubscriptions(List<Subscription> subscriptions) {
        if (!CollectionUtils.isEmpty(subscriptions)) {
            subscriptions.forEach(this::addSubscription);
        }
    }


    /**
     * 删除订阅，需要时删除同步任务
     */
    public void delSubscription(Subscription subscription) {
        if (subscription != null) {
            String destination = subscription.getDestination();
            if (destinationTaskMap.containsKey(destination)) {
                DefaultCanalTask defaultCanalTask = destinationTaskMap.get(destination);
                defaultCanalTask.delSubscription(subscription);
                if (defaultCanalTask.isIdle()) {
                    defaultCanalTask.release();
                    destinationTaskMap.remove(destination);
                }
            }
        }
    }

}
