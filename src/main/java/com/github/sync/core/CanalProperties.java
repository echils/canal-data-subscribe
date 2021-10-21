package com.github.sync.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于连接Canal服务器的基础配置信息
 *
 * @author echils
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "data.sync.canal")
public class CanalProperties implements InitializingBean {

    /**
     * Canal服务器ipv4,默认
     */
    private String ipv4 = "localhost";

    /**
     * Canal服务器端口,默认端口11111
     */
    private int port = 11111;

    /**
     * Canal服务器用户名,默认为空
     */
    private String username = "";

    /**
     * Canal服务器密码,默认为空
     */
    private String password = "";

    /**
     * Canal服务器实例名称,默认的实例名称example
     */
    private List<String> destinations = Collections.singletonList("example");

    /**
     * 禁止订阅的数据信息: schema.table
     */
    private List<String> internalData = new ArrayList<>();

    /**
     * 每次拉去数量最大值,如果小于等于0，默认1000
     */
    private int batchSize = 1000;


    @Override
    public String toString() {
        return "CanalProperties{" +
                "ipv4='" + ipv4 + '\'' +
                ", port=" + port +
                ", username='" + "*****" + '\'' +
                ", password='" + "*****" + '\'' +
                ", destinations=" + destinations +
                ", internalData=" + internalData +
                ", batchSize=" + batchSize +
                '}';
    }


    @Override
    public void afterPropertiesSet() {
        log.info("Canal properties load complete. {}", toString());
    }

}
