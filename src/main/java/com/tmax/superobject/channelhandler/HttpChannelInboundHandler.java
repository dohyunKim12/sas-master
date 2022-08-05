package com.tmax.superobject.channelhandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.MessageObject;
import com.tmax.superobject.service.saveJar;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;

import java.nio.charset.Charset;

public class HttpChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(HttpChannelInboundHandler.class.getName());
    // Create class logger for set own level

    public HttpChannelInboundHandler() {
        super();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest httpRequest = (FullHttpRequest) msg;
            HttpHeaders headers = httpRequest.headers();
            logger.info("incomming http request : " + httpRequest);

            if (headers.get("targetServiceName").equals("saveJar")){
                saveJar.save((CompositeByteBuf)httpRequest.content()); // call save() method with parameter(http body)
            }
        };
    }
}