package com.github.sync.core;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal连接工厂
 *
 * @author echils
 */
@Component
public class CanalConnectorFactory {


    /**
     * 缓存同实例下的连接,一个实例只能有一个客户端消费
     */
    private static final Map<String, CanalConnector> connectorMap = new ConcurrentHashMap<>();


    /**
     * 创建一个Canal连接
     *
     * @param ipv4        Canal服务器IPV4
     * @param port        Canal服务器端口
     * @param username    Canal服务器用户名
     * @param password    Canal服务器密码
     * @param destination Canal实例名称
     */
    public CanalConnector create(String ipv4, int port,
                                 String username,
                                 String password,
                                 String destination) {

        return connectorMap.computeIfAbsent(destination, instance ->
                CanalConnectors.newSingleConnector(new InetSocketAddress(ipv4, port),
                        destination, username, password)
        );
    }


}
