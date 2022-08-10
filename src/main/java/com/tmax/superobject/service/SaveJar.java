package com.tmax.superobject.service;

import com.google.gson.JsonObject;
import com.tmax.superobject.Main;
import com.tmax.superobject.config.Global;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.AbstractServiceObject;
import com.tmax.superobject.object.BodyObject;
import com.tmax.superobject.object.DefaultBodyObject;
import com.tmax.superobject.object.HeaderObject;
import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SaveJar extends AbstractServiceObject {

    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(Main.class.getName());
    private static OutputStream outputStream = null;
    private static String filePath = null;

    @Override
    public void service(BodyObject bodyObject) {
//        logger.info("saveJar - save() called");
//        logger.info("received BodyObject: {}", bodyObject);
//
//        filePath = "./bin/tmp/" + "super-app-server.jar"; // file name from header
//        try {
//            outputStream = new FileOutputStream(filePath);
//            ByteBuffer buffer = (ByteBuffer) bodyObject.getByteBuffer().rewind();
//            outputStream.write(buffer.array(), 0, buffer.remaining());
//            logger.info("File write Done!!!");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        String response = "{response: Binary file successfully received}";
//        JsonObject jsonObject = Global.gson.get().fromJson(response, JsonObject.class);
//        setReply(new DefaultBodyObject(jsonObject));
        logger.info("entering admin service");
        File file = new File(bodyObject.getJsonObject().get("path").getAsString());
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
//            logger.info("Incoming File : " + new String(bodyObject.getByteBuffer().array()));
            Files.write(file.toPath(), bodyObject.getByteBuffer().array());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        setReply(new DefaultBodyObject());
    }

    @Override
    public void completeService() {
    }
}
