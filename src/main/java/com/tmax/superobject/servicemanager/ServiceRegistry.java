package com.tmax.superobject.servicemanager;

import com.tmax.superobject.config.Global;
import com.tmax.superobject.constant.SasConstants;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import io.netty.channel.Channel;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(ServiceRegistry.class.getName());
    private static ServiceRegistry instance = new ServiceRegistry();
    private Map<String, Channel> serviceChannelMap;

    public static ServiceRegistry getInstance() {
        return instance;
    }

    private ServiceRegistry() {
        super();
        serviceChannelMap = new HashMap<>();
    }

    public Map<String, Channel> getServiceChannelMap() {
        return serviceChannelMap;
    }

    public void setServiceChannelMap(Map<String, Channel> serviceChannelMap) {
        this.serviceChannelMap = serviceChannelMap;
    }

    public static String generateChannelUri(String serviceName) {
        return "ws://" + serviceName.substring(0, serviceName.lastIndexOf(".")) + ":" + (SasConstants.DEFAULT_WS_PORT);
    }

    public boolean isLocalService(String serviceName) {
        try {
            Global.getInstance().getServiceClassLoader().loadClass(serviceName);
            return true;
        } catch (ClassNotFoundException e) {
            logger.info(serviceName + " : is not local service");
            return false;
        }
    }

    public Channel findOutboundChannel(String serviceName) {
        String serviceUri = generateChannelUri(serviceName);
        Channel channel = serviceChannelMap.get(serviceName);
        if (channel != null) {
            return channel;
        }
        channel = serviceChannelMap.get(serviceUri);
        if (channel != null) {
            return channel;
        }
        return channel;
    }
}
