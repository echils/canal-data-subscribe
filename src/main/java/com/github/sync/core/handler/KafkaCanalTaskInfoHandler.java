package com.github.sync.core.handler;

import com.github.sync.core.IDataSyncInfoHandler;
import com.github.sync.model.DataSyncMessage;
import com.github.sync.model.ReceiptAddress;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.sync.core.CanalDataSyncContext.SCHEMA_TABLE_SEPARATOR;


/**
 * 基于Kafka进行数据推送
 *
 * @author echils
 */
@Slf4j
@Component
public class KafkaCanalTaskInfoHandler implements IDataSyncInfoHandler {

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public void handle(String schema, String table,
                       List<String> sqlInfos,
                       List<ReceiptAddress> receiptAddressList) {

        log.info("Kafka canal task handle sqlInfos: {},receiptAddressList: {}", sqlInfos, receiptAddressList);
        receiptAddressList.forEach(address -> {
            Producer<String, String> kafkaProducer = producerBiFunction.apply(address);
            try {
                List<String> uniqueSqlInfos = sqlInfos.stream()
                        .map(sql -> conversion(sql, schema, table, address.getSchema(), address.getTable()))
                        .collect(Collectors.toList());
                kafkaProducer.send(new ProducerRecord<>(address.getChannel(),
                        schema + SCHEMA_TABLE_SEPARATOR + table,
                        objectMapper.writeValueAsString(DataSyncMessage.newInstance(address.getSubscriptionTag(), uniqueSqlInfos))));
            } catch (JsonProcessingException ex) {
                log.error("Kafka canal task handle error:{}", ex.getMessage());
            }
            kafkaProducer.close();
        });
    }


    @Override
    public boolean match(HandlerType handlerType) {
        return HandlerType.KAFKA == handlerType;
    }


    /**
     * 生产者提供函数
     */
    private static final Function<ReceiptAddress, Producer<String, String>> producerBiFunction = (address) -> {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, address.getHost());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    };

}
