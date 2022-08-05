package com.tmax.superobject.service;

import com.tmax.superobject.Main;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import org.slf4j.Logger;

public class saveJar {

    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(Main.class.getName());
    public static void save(){
        logger.info("saveJar - save() called");

    }
}
