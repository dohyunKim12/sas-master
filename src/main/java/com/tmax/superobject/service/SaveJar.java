package com.tmax.superobject.service;

import com.tmax.superobject.Main;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.AbstractServiceObject;
import com.tmax.superobject.object.BodyObject;
import com.tmax.superobject.object.HeaderObject;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SaveJar extends AbstractServiceObject {

    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(Main.class.getName());
    private static OutputStream outputStream = null;
    private static String filePath = null;

    @Override
    public void service(BodyObject bodyObject) {
        logger.info("saveJar - save() called");
        filePath = "./bin/tmp/" + "super-app-server.jar"; // from header
        try {
            outputStream = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        logger.info("Before read: " + String.valueOf(bodyObject.getCompositeByteBuf().isReadable()));
        logger.info("Before read: " + String.valueOf(bodyObject.getCompositeByteBuf().readableBytes())); // write index(widx) - read index(ridx)
        try {
            bodyObject.getCompositeByteBuf().readBytes(outputStream, bodyObject.getCompositeByteBuf().capacity()); // get http body, and write to file
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("After read: " + bodyObject.getCompositeByteBuf().toString());
        logger.info("After read: " + String.valueOf(bodyObject.getCompositeByteBuf().isReadable()));
        logger.info("After read: " + String.valueOf(bodyObject.getCompositeByteBuf().readableBytes())); // write index(widx) - read index(ridx)
    }

    @Override
    public void completeService() {
    }
}
