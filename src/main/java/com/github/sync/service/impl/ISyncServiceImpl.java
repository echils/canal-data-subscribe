package com.github.sync.service.impl;

import com.github.sync.core.CanalDataSyncExecutor;
import com.github.sync.model.ReceiptAddress;
import com.github.sync.model.SubscribeStatus;
import com.github.sync.model.Subscription;
import com.github.sync.model.SyncException;
import com.github.sync.service.ISyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 同步业务实现类
 *
 * @author echils
 */
@Slf4j
@Service
public class ISyncServiceImpl implements ISyncService {

    @Autowired
    private CanalDataSyncExecutor canalDataSyncExecutor;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RedisTemplate<String, Subscription> redisTemplate;


    @Override
    public String subscribe(Subscription subscription) {
        if (subscription.isBlank()) {
            throw new SyncException(messageSource.getMessage(
                    "REQUIRED-PARAMETERS-CANNOT-BE-EMPTY", null, LocaleContextHolder.getLocale()));
        }
        List<Subscription> configuredSubscriptions = findAllSubscriptions(null,
                subscription.getDestination(), subscription.getSchema(), subscription.getTable(), null);
        if (configuredSubscriptions.contains(subscription)) {
            throw new SyncException(messageSource.getMessage(
                    "REPEATED-SUBSCRIPTION", null, LocaleContextHolder.getLocale()));
        }

        subscription.setId(uuid());
        subscription.setStatus(SubscribeStatus.NORMAL);
        subscription.setCreateTime(new Date());
        ReceiptAddress receiptAddress = subscription.getReceiptAddress();
        if (StringUtils.isBlank(receiptAddress.getSubscriptionTag())) {
            receiptAddress.setSubscriptionTag(uuid());
        }
        redisTemplate.opsForValue().set(subscription.getId(), subscription);
        canalDataSyncExecutor.addSubscription(subscription);
        log.info("Sync subscribe success:{}", subscription);
        return subscription.getId();
    }

    @Override
    public void pause(String subscriptionId) {
        Subscription subscription = redisTemplate.opsForValue().get(subscriptionId);
        if (subscription != null && subscription.getStatus() == SubscribeStatus.NORMAL) {
            subscription.setStatus(SubscribeStatus.PAUSE);
            redisTemplate.opsForValue().set(subscriptionId, subscription);
            canalDataSyncExecutor.delSubscription(subscription);
            log.info("Sync pause success:{}", subscription);
        }
    }

    @Override
    public void restart(String subscriptionId) {
        Subscription subscription = redisTemplate.opsForValue().get(subscriptionId);
        if (subscription != null && subscription.getStatus() == SubscribeStatus.PAUSE) {
            subscription.setStatus(SubscribeStatus.NORMAL);
            redisTemplate.opsForValue().set(subscriptionId, subscription);
            canalDataSyncExecutor.addSubscription(subscription);
            log.info("Sync restart success:{}", subscription);
        }
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        Subscription subscription = redisTemplate.opsForValue().get(subscriptionId);
        if (subscription != null) {
            redisTemplate.delete(subscriptionId);
            canalDataSyncExecutor.delSubscription(subscription);
            log.info("Sync unsubscribe success:{}", subscription);
        }
    }

    @Override
    public List<Subscription> findAllSubscriptions(String id, String destination,
                                                   String schema, String table,
                                                   SubscribeStatus status) {

        if (StringUtils.isNotBlank(id)) {
            Subscription subscription = redisTemplate.opsForValue().get(id);
            return subscription == null ? Collections.emptyList() : Collections.singletonList(subscription);
        }

        Set<String> keys = redisTemplate.keys("*");
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }

        return Objects.requireNonNull(redisTemplate.opsForValue().multiGet(keys)).stream()
                .filter(subscription -> StringUtils.isBlank(destination) || subscription.getDestination().contains(destination))
                .filter(subscription -> StringUtils.isBlank(schema) || subscription.getSchema().contains(schema))
                .filter(subscription -> StringUtils.isBlank(table) || subscription.getTable().contains(table))
                .filter(subscription -> status == null || subscription.getStatus() == status)
                .collect(Collectors.toList());
    }


    /**
     * 提供唯一标识
     */
    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }


}
