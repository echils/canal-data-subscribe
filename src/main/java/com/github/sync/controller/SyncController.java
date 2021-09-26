package com.github.sync.controller;


import com.github.sync.model.ResponseEntity;
import com.github.sync.model.SubscribeStatus;
import com.github.sync.model.Subscription;
import com.github.sync.service.ISyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据同步控制器
 *
 * @author echils
 */
@SuppressWarnings("rawtypes")
@RestController
public class SyncController {


    @Autowired
    private ISyncService syncService;


    /**
     * 新增订阅
     *
     * @param subscription 订阅信息
     */
    @PostMapping("/subscribe")
    public ResponseEntity subscribe(@RequestBody Subscription subscription) {

        return ResponseEntity.insertSuccess(syncService.subscribe(subscription));
    }


    /**
     * 暂停订阅
     *
     * @param subscriptionId 订阅唯一标识
     */
    @PutMapping("/subscribe/{subscriptionId}/pause")
    public ResponseEntity pause(@PathVariable String subscriptionId) {

        syncService.pause(subscriptionId);
        return ResponseEntity.updateSuccess();
    }


    /**
     * 开启订阅
     *
     * @param subscriptionId 订阅唯一标识
     */
    @PutMapping("/subscribe/{subscriptionId}/restart")
    public ResponseEntity restart(@PathVariable String subscriptionId) {

        syncService.restart(subscriptionId);
        return ResponseEntity.updateSuccess();
    }


    /**
     * 取消订阅
     *
     * @param subscriptionId 订阅唯一标识
     */
    @DeleteMapping("/subscribe/{subscriptionId}/stop")
    public ResponseEntity unsubscribe(@PathVariable String subscriptionId) {

        syncService.unsubscribe(subscriptionId);
        return ResponseEntity.deleteSuccess();
    }


    /**
     * 查询所有订阅
     *
     * @param id          唯一标识
     * @param destination 实例名称
     * @param schema      数据库名称
     * @param table       表名称
     * @param status      监测状态
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<List<Subscription>> findAllSubscriptions(@RequestParam(required = false) String id,
                                                                   @RequestParam(required = false) String destination,
                                                                   @RequestParam(required = false) String schema,
                                                                   @RequestParam(required = false) String table,
                                                                   @RequestParam(required = false) SubscribeStatus status) {

        return ResponseEntity.success(syncService.findAllSubscriptions(id, destination, schema, table, status));
    }

}
