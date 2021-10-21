package com.github.sync.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sync.core.IDataSyncInfoHandler;
import com.github.sync.model.DataOperation;
import com.github.sync.model.DataSyncMessage;
import com.github.sync.model.ReceiptAddress;
import com.google.common.collect.Lists;
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

import static com.github.sync.service.ISyncService.SCHEMA_TABLE_SEPARATOR;


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
                       List<DataOperation> dataList,
                       List<ReceiptAddress> receiptAddressList) {

        log.info("Kafka canal task handle dataList: {},receiptAddressList: {}", dataList, receiptAddressList);
        receiptAddressList.forEach(address -> {
            Producer<String, String> kafkaProducer = producerBiFunction.apply(address);
            try {
                if (dataList.size() > DATA_BATCH_SIZE) {
                    for (List<DataOperation> dataOperations : Lists.partition(dataList, DATA_BATCH_SIZE)) {
                        kafkaProducer.send(new ProducerRecord<>(address.getChannel(),
                                schema + SCHEMA_TABLE_SEPARATOR + table,
                                objectMapper.writeValueAsString(DataSyncMessage.newInstance(address.getSubscriptionTag(), dataOperations))));
                    }

                } else {
                    kafkaProducer.send(new ProducerRecord<>(address.getChannel(),
                            schema + SCHEMA_TABLE_SEPARATOR + table,
                            objectMapper.writeValueAsString(DataSyncMessage.newInstance(address.getSubscriptionTag(), dataList))));
                }
            } catch (Exception ex) {
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
