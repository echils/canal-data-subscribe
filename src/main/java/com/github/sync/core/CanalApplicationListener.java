package com.github.sync.core;

import com.github.sync.model.SubscribeStatus;
import com.github.sync.model.Subscription;
import com.github.sync.service.ISyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Canal事件监听，服务启动后自动重启之前已经发布的同步任务
 *
 * @author echils
 */
@Slf4j
@Component
public class CanalApplicationListener implements ApplicationListener<ApplicationStartedEvent> {


    @Autowired
    private CanalProperties canalProperties;

    @Autowired
    private ISyncService syncService;

    @Autowired
    private CanalDataSyncExecutor canalDataSyncExecutor;


    @Override
    public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
        canalDataSyncExecutor.addSubscriptions(obtainConfiguredSubscriptions());
    }

    /**
     * 获取之前已经配置过的订阅信息，针对本次实例列表进行过滤
     */
    private List<Subscription> obtainConfiguredSubscriptions() {

        List<Subscription> subscriptions = syncService.findAllSubscriptions(
                null, null, null, null, SubscribeStatus.NORMAL);
        if (!CollectionUtils.isEmpty(subscriptions)) {
            List<String> destinations = canalProperties.getDestinations();
            List<Subscription> eligibleSubscriptions = subscriptions.stream().filter(subscription
                    -> destinations.contains(subscription.getDestination())).collect(Collectors.toList());
            subscriptions.removeAll(eligibleSubscriptions);
            if (!CollectionUtils.isEmpty(subscriptions)) {
                log.warn("The previously configured subscription does not meet" +
                        " the subscription conditions this time：{}", subscriptions);
            }
            log.info("The eligible subscriptions:{}", eligibleSubscriptions);
            return eligibleSubscriptions;
        }
        return Collections.emptyList();
    }

}
