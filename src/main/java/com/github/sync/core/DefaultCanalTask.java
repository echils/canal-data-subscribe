package com.github.sync.core;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.github.sync.model.ReceiptAddress;
import com.github.sync.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.github.sync.core.CanalDataSyncContext.*;

/**
 * 同步任务，一个实例对应一个同步任务
 * 同步任务可监听不同的数据库和不同的表，然后推送到不同的服务器
 *
 * @author echils
 */
@Slf4j
public class DefaultCanalTask extends Thread {

    /**
     * 连接器
     */
    private CanalConnector connector;

    /**
     * 监听列表
     */
    private String subscribeFilter;

    /**
     * 结束任务，停止线程
     */
    private boolean destroy;

    /**
     * 是否刷新订阅
     */
    private boolean refresh;

    /**
     * 每次拉取数据最大值
     */
    private int batchSize;

    /**
     * 数据同步处理器
     */
    private List<IDataSyncInfoHandler> dataSyncInfoHandlers;

    /**
     * 缓存每个表的推送列表
     * k:schema.table
     * v:subscription
     */
    private static volatile Map<String, List<Subscription>> subscribeInfoMap = new ConcurrentHashMap<>();


    public DefaultCanalTask(CanalConnector connector,
                            Subscription subscription,
                            List<IDataSyncInfoHandler> dataSyncInfoHandlers,
                            int batchSize) {

        this.connector = connector;
        String subscribeFilter =
                subscription.getSchema() + SCHEMA_TABLE_SEPARATOR + subscription.getTable();
        this.subscribeFilter = subscribeFilter;
        refresh = true;

        List<Subscription> subscriptions =
                Optional.ofNullable(subscribeInfoMap.get(subscribeFilter)).orElse(new ArrayList<>());
        subscriptions.add(subscription);
        subscribeInfoMap.put(subscribeFilter, subscriptions);
        this.batchSize = batchSize;
        this.dataSyncInfoHandlers = dataSyncInfoHandlers;

        //建立连接
        connector.connect();
        //任务自启动
        super.start();
    }


    /**
     * 添加推送列表
     *
     * @param subscription 订阅信息
     */
    public void addSubscription(Subscription subscription) {

        String newSubscribeFilter =
                subscription.getSchema() + SCHEMA_TABLE_SEPARATOR + subscription.getTable();
        //判断是否存在同一张表要同步到不同的服务，如果存在，说明这张表已经被订阅，追加推送列表即可
        if (!subscribeFilter.contains(newSubscribeFilter)) {
            this.subscribeFilter += (SUBSCRIBE_FILTER_SEPARATOR + newSubscribeFilter);
            refresh = true;
        }
        List<Subscription> subscriptions =
                Optional.ofNullable(subscribeInfoMap.get(newSubscribeFilter)).orElse(new ArrayList<>());
        subscriptions.add(subscription);
        subscribeInfoMap.put(newSubscribeFilter, subscriptions);
    }


    /**
     * 删除推送列表
     *
     * @param subscription 订阅信息
     */
    public void delSubscription(Subscription subscription) {

        String delSubscribeFilter =
                subscription.getSchema() + SCHEMA_TABLE_SEPARATOR + subscription.getTable();

        List<Subscription> subscriptions =
                Optional.ofNullable(subscribeInfoMap.get(delSubscribeFilter)).orElse(new ArrayList<>());

        subscriptions.remove(subscription);
        subscribeInfoMap.put(delSubscribeFilter, subscriptions);
        //如果大于0，说明这张表存在一张表要同步到不同的服务的情况，故不能取消订阅.
        if (subscriptions.size() > 0) {
            return;
        }

        //移除订阅
        if (this.subscribeFilter.contains(SUBSCRIBE_FILTER_SEPARATOR + delSubscribeFilter)) {
            //该订阅不是第一个
            this.subscribeFilter = this.subscribeFilter.replace(
                    SUBSCRIBE_FILTER_SEPARATOR + delSubscribeFilter, BLANK);
        } else if (this.subscribeFilter.contains(delSubscribeFilter + SUBSCRIBE_FILTER_SEPARATOR)) {
            //该订阅是第一个,且还有其他监听
            this.subscribeFilter = this.subscribeFilter.replace(
                    SUBSCRIBE_FILTER_SEPARATOR + delSubscribeFilter, BLANK);
        } else {
            //该订阅是唯一的一个,直接删除实例对应的缓存，并停止监听
            this.subscribeFilter = this.subscribeFilter.replace(delSubscribeFilter, BLANK);
        }
        refresh = true;
    }


    /**
     * 判断任务是否在闲置，当任务没有监听列表
     */
    public boolean isIdle() {

        return StringUtils.isBlank(subscribeFilter);
    }


    /**
     * 关闭同步任务,释放连接
     */
    public void release() {
        this.destroy = true;
        refresh = true;
        connector.disconnect();
    }


    /**
     * 处理同步数据
     *
     * @param syncInfo 同步信息
     */
    private void handleSyncInfo(SyncInfo syncInfo) {
        List<String> sqlList = syncInfo.getSqlList();
        syncInfo.getReceiptAddresses().stream().collect(Collectors.groupingBy(ReceiptAddress::getType))
                .forEach((key, value) -> {
                    Optional<IDataSyncInfoHandler> infoHandlerOptional = dataSyncInfoHandlers.stream()
                            .filter(dataSyncInfoHandler -> dataSyncInfoHandler.match(key)).findFirst();
                    infoHandlerOptional.ifPresent(dataSyncInfoHandler ->
                            dataSyncInfoHandler.handle(syncInfo.getSchema(), syncInfo.getTable(), sqlList, value));
                });
    }


    @Override
    public void run() {
        while (!destroy) {
            if (refresh) {
                connector.subscribe(subscribeFilter);
                log.info("Canal task refresh subscribe: {}", subscribeFilter);
                refresh = false;
            }
            if (isIdle()) {
                monitorWait();
                continue;
            }
            try {
                connector.rollback();
                Message message = connector.getWithoutAck(batchSize);
                long messageBatchId = message.getId();
                if (messageBatchId == -1 && CollectionUtils.isEmpty(message.getEntries())) {
                    monitorWait();
                    continue;
                }
                parseMessage(message).ifPresent(syncInfos -> syncInfos.forEach(this::handleSyncInfo));
                connector.ack(messageBatchId);
            } catch (Exception ex) {
                log.error("Canal task occur error: {}", ex.getMessage(), ex);
            }
        }
    }


    /**
     * 同步数据信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncInfo {

        /**
         * 数据库名称
         */
        private String schema;

        /**
         * 表名称
         */
        private String table;

        /**
         * SQL
         */
        private List<String> sqlList = new ArrayList<>();

        /**
         * 推送地址
         */
        private Set<ReceiptAddress> receiptAddresses = new LinkedHashSet<>();

    }


    /**
     * 解析同步信息
     */
    private Optional<List<SyncInfo>> parseMessage(Message message) {
        List<CanalEntry.Entry> entries = message.getEntries();
        Map<String, SyncInfo> syncInfoMap = new HashMap<>();
        for (CanalEntry.Entry entry : entries) {
            try {
                if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                    CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                    if (rowChange.getIsDdl()) {
                        //数据库结构变更，影响较大，可能导致相关功能不可用，先不处理
                        return Optional.empty();
                    }
                    CanalEntry.Header header = entry.getHeader();
                    String tableName = header.getTableName();
                    String schemaName = header.getSchemaName();
                    if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(schemaName)) {
                        String syncTaskKey = schemaName + SCHEMA_TABLE_SEPARATOR + tableName;
                        if (!subscribeInfoMap.containsKey(syncTaskKey)) {
                            continue;
                        }
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        String executeSQL = null;
                        if (eventType == CanalEntry.EventType.DELETE) {
                            executeSQL = parseDeleteSql(schemaName, tableName, rowChange);
                        }
                        if (eventType == CanalEntry.EventType.INSERT) {
                            executeSQL = parseInsertSql(schemaName, tableName, rowChange);
                        }
                        if (eventType == CanalEntry.EventType.UPDATE) {
                            executeSQL = parseUpdateSql(schemaName, tableName, rowChange);
                        }
                        if (StringUtils.isNotBlank(executeSQL)) {
                            SyncInfo syncInfo = syncInfoMap.computeIfAbsent(syncTaskKey, key -> {
                                SyncInfo body = new SyncInfo();
                                body.setTable(tableName);
                                body.setSchema(schemaName);
                                body.getReceiptAddresses().addAll(subscribeInfoMap.get(syncTaskKey)
                                        .stream().map(Subscription::getReceiptAddress).collect(Collectors.toList()));
                                return body;
                            });
                            syncInfo.getSqlList().add(executeSQL);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Canal parse message occur error:{}", e.getMessage(), e);
            }
        }
        return syncInfoMap.size() == 0 ? Optional.empty() : Optional.of(new ArrayList<>(syncInfoMap.values()));
    }


    /**
     * 监听等待
     */
    private void monitorWait() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
    }


    /**
     * 解析UpdateSQL
     */
    private String parseUpdateSql(String schemaName, String tableName, CanalEntry.RowChange rowChange) {
        List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
        StringBuilder updateSQL = new StringBuilder("update " + filedTagFunction.apply(schemaName)
                + SCHEMA_TABLE_SEPARATOR + filedTagFunction.apply(tableName) + " set");
        for (CanalEntry.RowData rowData : rowDataList) {
            List<CanalEntry.Column> newColumnList = rowData.getAfterColumnsList()
                    .stream().filter(CanalEntry.Column::getUpdated).collect(Collectors.toList());
            for (int i = 0; i < newColumnList.size(); i++) {
                CanalEntry.Column column = newColumnList.get(i);
                updateSQL.append(" ").append(filedTagFunction.apply(column.getName()))
                        .append(" = ").append(valueWrapFunction.apply(column.getMysqlType(), column.getValue()));
                if (i != newColumnList.size() - 1) { updateSQL.append(","); }
            }
            updateSQL.append(" where ");
            List<CanalEntry.Column> primaryKeyColumnList = rowData.getBeforeColumnsList()
                    .stream().filter(CanalEntry.Column::getIsKey).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(primaryKeyColumnList)) {
                log.error("Canal com.github.sync update task scheme:{} table:{} no primary key," +
                        "There is no basis for synchronization", schemaName, tableName);
                return null;
            }
            for (int i = 0; i < primaryKeyColumnList.size(); i++) {
                CanalEntry.Column column = primaryKeyColumnList.get(i);
                updateSQL.append(filedTagFunction.apply(column.getName()))
                        .append(" = ").append(valueWrapFunction.apply(column.getMysqlType(), column.getValue()));
                if (i != primaryKeyColumnList.size() - 1) { updateSQL.append(" and "); }
            }
        }
        return updateSQL.toString();
    }

    /**
     * 解析DeleteSQL
     */
    private String parseDeleteSql(String schemaName, String tableName, CanalEntry.RowChange rowChange) {
        List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
        StringBuilder deleteSQL = new StringBuilder("delete from " + filedTagFunction.apply(schemaName)
                + SCHEMA_TABLE_SEPARATOR + filedTagFunction.apply(tableName) + " where ");
        for (CanalEntry.RowData rowData : rowDataList) {
            List<CanalEntry.Column> primaryKeyColumnList = rowData.getBeforeColumnsList()
                    .stream().filter(CanalEntry.Column::getIsKey).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(primaryKeyColumnList)) {
                log.error("Canal com.github.sync delete task scheme:{} table:{} no primary key," +
                        "There is no basis for synchronization", schemaName, tableName);
                return null;
            }
            for (int i = 0; i < primaryKeyColumnList.size(); i++) {
                CanalEntry.Column column = primaryKeyColumnList.get(i);
                deleteSQL.append(filedTagFunction.apply(column.getName()))
                        .append(" = ").append(valueWrapFunction.apply(column.getMysqlType(), column.getValue()));
                if (i != primaryKeyColumnList.size() - 1) { deleteSQL.append(" and "); }
            }
        }
        return deleteSQL.toString();
    }


    /**
     * 解析InsertSQL
     */
    private String parseInsertSql(String schemaName, String tableName, CanalEntry.RowChange rowChange) {
        List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
        StringBuilder insertSQL = new StringBuilder("insert into " + filedTagFunction.apply(schemaName)
                + SCHEMA_TABLE_SEPARATOR + filedTagFunction.apply(tableName) + " (");
        for (CanalEntry.RowData rowData : rowDataList) {
            List<CanalEntry.Column> columnList = rowData.getAfterColumnsList();
            for (int i = 0; i < columnList.size(); i++) {
                insertSQL.append(filedTagFunction.apply(columnList.get(i).getName()));
                if (i != columnList.size() - 1) { insertSQL.append(","); }
            }
            insertSQL.append(") VALUES (");
            for (int i = 0; i < columnList.size(); i++) {
                insertSQL.append(valueWrapFunction.apply(columnList.get(i).getMysqlType(), columnList.get(i).getValue()));
                if (i != columnList.size() - 1) { insertSQL.append(","); }
            }
        }
        return insertSQL.append(")").toString();
    }


}