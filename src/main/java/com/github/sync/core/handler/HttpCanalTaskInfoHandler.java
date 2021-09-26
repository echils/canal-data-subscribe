package com.github.sync.core.handler;

import com.github.sync.core.IDataSyncInfoHandler;
import com.github.sync.model.DataSyncMessage;
import com.github.sync.model.ReceiptAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于Http进行数据推送
 *
 * @author echils
 */
@Slf4j
@Component
public class HttpCanalTaskInfoHandler implements IDataSyncInfoHandler {


    @Autowired
    private RestTemplate restTemplate;


    @Override
    public void handle(String schema, String table,
                       List<String> sqlInfos,
                       List<ReceiptAddress> receiptAddressList) {

        log.info("Http canal task handle sqlInfos: {},receiptAddressList: {}", sqlInfos, receiptAddressList);
        receiptAddressList.forEach(address -> {
            List<String> uniqueSqlInfos = sqlInfos.stream()
                    .map(sql -> conversion(sql, schema, table, address.getSchema(), address.getTable()))
                    .collect(Collectors.toList());
            DataSyncMessage dataSyncMessage = DataSyncMessage.newInstance(address.getSubscriptionTag(), uniqueSqlInfos);
            try {
                restTemplate.postForEntity(URI.create(address.getHost() + address.getChannel()), dataSyncMessage, Object.class);
            } catch (RestClientException e) {
                log.error("Http canal task handle error:{}", e.getMessage());
            }
        });
    }

    @Override
    public boolean match(HandlerType handlerType) {
        return HandlerType.HTTP == handlerType;
    }

}
