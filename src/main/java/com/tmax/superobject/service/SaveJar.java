package com.tmax.superobject.service;

import com.tmax.superobject.Main;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.AbstractServiceObject;
import com.tmax.superobject.object.BodyObject;
import org.slf4j.Logger;

import java.io.*;

public class SaveJar extends AbstractServiceObject {

    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(Main.class.getName());
    private static OutputStream outputStream = null;

    @Override
    public void service(BodyObject compositeByteBuf) {
        logger.info("saveJar - save() called");
//        ByteBuf b1 = Unpooled.buffer();
//        compositeByteBuf.getCompositeByteBuf().addComponent(b1);
        try {
            outputStream = new FileOutputStream("./bin/tmpfile");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        logger.info("Before read: " + String.valueOf(compositeByteBuf.getCompositeByteBuf().isReadable()));
        logger.info("Before read: " + String.valueOf(compositeByteBuf.getCompositeByteBuf().readableBytes())); // write index(widx) - read index(ridx)
//        logger.info(String.valueOf(compositeByteBuf.getCompositeByteBuf().readBytes(5)));
        try {
            compositeByteBuf.getCompositeByteBuf().readBytes(outputStream,compositeByteBuf.getCompositeByteBuf().capacity()); // get http body, and write to file
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        compositeByteBuf.getCompositeByteBuf().getBytes(fileOutputStream, 1);

        logger.info("After read: " + compositeByteBuf.getCompositeByteBuf().toString());

        logger.info("After read: " + String.valueOf(compositeByteBuf.getCompositeByteBuf().isReadable()));
        logger.info("After read: " + String.valueOf(compositeByteBuf.getCompositeByteBuf().readableBytes())); // write index(widx) - read index(ridx)


//        JsonObject jsonObject = new Gson().fromJson(content.toString(Charset.defaultCharset()),
//                JsonObject.class);
//        MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);

//        logger.info(jsonObject.toString());
//        logger.info(messageObject.toString());

    }

    @Override
    public void completeService() {

    }
}
