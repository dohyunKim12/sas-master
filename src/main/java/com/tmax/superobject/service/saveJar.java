package com.tmax.superobject.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tmax.superobject.Main;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.MessageObject;
import io.netty.buffer.CompositeByteBuf;
import org.slf4j.Logger;

import java.nio.charset.Charset;

public class saveJar {

    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(Main.class.getName());
    public static void save(CompositeByteBuf content){
        logger.info("saveJar - save() called");

        logger.info(content.toString());

        JsonObject jsonObject = new Gson().fromJson(content.toString(Charset.defaultCharset()),
                JsonObject.class);
        MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);

        logger.info(jsonObject.toString());
        logger.info(messageObject.toString());

    }
}
