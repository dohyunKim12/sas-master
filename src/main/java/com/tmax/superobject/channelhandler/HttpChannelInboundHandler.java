package com.tmax.superobject.channelhandler;

import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.AbstractBodyObject;
import com.tmax.superobject.object.BodyObject;
import com.tmax.superobject.object.DefaultBodyObject;
import com.tmax.superobject.service.SaveJar;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
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
            logger.info("incomming http request : " + httpRequest);

            if (headers.get("targetServiceName").equalsIgnoreCase("SaveJar")){
                SaveJar saveJar = new SaveJar();
                DefaultBodyObject defaultBodyObject = new DefaultBodyObject((CompositeByteBuf) httpRequest.content());
                logger.info("type of content : " + httpRequest.content().getClass().getName());
                logger.info("content: " + httpRequest.content().toString(StandardCharsets.UTF_8));
                logger.info("defualtBodyObject: " + defaultBodyObject.getCompositeByteBuf());
                saveJar.service(defaultBodyObject);
            }
        };
    }
}