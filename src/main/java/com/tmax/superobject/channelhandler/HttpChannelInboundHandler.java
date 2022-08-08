package com.tmax.superobject.channelhandler;

import com.google.gson.JsonObject;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.*;
import com.tmax.superobject.service.SaveJar;
import com.tmax.superobject.util.HttpUtils;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HttpChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(HttpChannelInboundHandler.class.getName());
    // Create class logger for set own level

    public HttpChannelInboundHandler() {
        super();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.warn("type of msg:" + msg.getClass().getName());
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest httpRequest = (FullHttpRequest) msg;
            HttpHeaders headers = httpRequest.headers();
            logger.info("type of http Header:" + headers.getClass().getName());
//            logger.info("incomming http request : " + httpRequest);
            JsonObject jsonObject = HttpUtils.prepareJsonRequest(httpRequest) ;
            MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);

            if (headers.get("targetServiceName").equalsIgnoreCase("SaveJar")){

                SaveJar saveJar = new SaveJar();
                DefaultBodyObject defaultBodyObject = new DefaultBodyObject((CompositeByteBuf) httpRequest.content());
                DefaultHeaderObject defaultHeaderObject = new DefaultHeaderObject();
//                defaultHeaderObject.setJsonObject(headers);
                logger.info("type of content : " + httpRequest.content().getClass().getName());
                logger.info("defualtBodyObject: " + defaultBodyObject.getCompositeByteBuf());
//                String fileName = headers.get("fileName");
//                saveJar.service(defaultBodyObject, fileName);
                saveJar.service(messageObject.body());
            }
        };
    }
}