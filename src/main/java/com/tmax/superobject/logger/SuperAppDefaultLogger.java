package com.tmax.superobject.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperAppDefaultLogger {
    private static SuperAppDefaultLogger instance;

    public static synchronized SuperAppDefaultLogger getInstance() {
        if(instance == null) {
            instance = new SuperAppDefaultLogger();
            Configurator.initialize(LogManager.ROOT_LOGGER_NAME, "./config/log4j2.xml");
            System.out.println("Logger Configured.");
        }
        return instance;
    }
    public Logger getLogger(String className) {
        return LoggerFactory.getLogger(className);
    }
}
