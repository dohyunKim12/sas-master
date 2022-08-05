package com.tmax.superobject;

import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.runtime.SuperAppMasterServer;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(Main.class.getName());
//    private static List<URL> jarList = new ArrayList<>();
    public static void main(String[] args) {

//Start SAS Server
        SuperAppMasterServer sas = new SuperAppMasterServer();
        sas.start();
        logger.info("Main started!!!");
    }
}