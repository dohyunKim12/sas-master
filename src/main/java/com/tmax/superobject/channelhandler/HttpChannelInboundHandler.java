package com.tmax.superobject.channelhandler;

import com.google.gson.JsonObject;
import com.tmax.superobject.config.Global;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.constant.SapConstants.MessageType;
import com.tmax.superobject.listener.ChannelCompletionListener;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.*;
import com.tmax.superobject.service.SaveJar;
import com.tmax.superobject.servicemanager.ServiceManager;
import com.tmax.superobject.util.HttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(HttpChannelInboundHandler.class.getName());
    private FullHttpResponse response;
    private ByteBuf content;

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

            if (headers.get(HttpHeaderNames.CONNECTION).equalsIgnoreCase(HttpHeaderValues.UPGRADE.toString()) &&
                    headers.get(HttpHeaderNames.UPGRADE).equalsIgnoreCase(HttpHeaderValues.WEBSOCKET.toString())) {
                logger.info("upgrade to websocket channel");
                ctx.pipeline().replace(this, "webSocketInboundHandler", new WebSocketChannelInboundHandler());
                handleHandshake(ctx, httpRequest);

//                JsonObject jsonObject = HttpUtils.prepareJsonRequest(httpRequest);
//                MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);

            } else {
                JsonObject jsonObject = Global.gson.get().fromJson(httpRequest.content().toString(Charset.defaultCharset()),
                        JsonObject.class);
                MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);
                ServiceManager.getInstance().callAsync(ctx.channel(), messageObject, new ChannelCompletionListener() {
                    @Override
                    public void afterCompletion(BodyObject responseBody) {
                        messageObject.header().setMessageType(MessageType.RESPONSE);
                        messageObject.setBody(responseBody);

                        content = Unpooled.copiedBuffer(messageObject.toString(), Charset.defaultCharset());
                        response = HttpUtils.prepareHttpResponse(content);

                        ctx.channel().writeAndFlush(response);
                    }
                });

            }
//            if (headers.get("targetServiceName").equalsIgnoreCase("SaveJar")) {
//
//                SaveJar saveJar = new SaveJar();
//                DefaultBodyObject defaultBodyObject = new DefaultBodyObject((CompositeByteBuf) httpRequest.content());
//                DefaultHeaderObject defaultHeaderObject = new DefaultHeaderObject();
//                logger.info("type of content : " + httpRequest.content().getClass().getName());
//                logger.info("defualtBodyObject: " + defaultBodyObject.getCompositeByteBuf());
//                saveJar.service(messageObject.body());
//            }
        }
    }

    protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketURL(req), null,
                true, 104857600);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info("websocket handshaking is completed");
                }
            });
        }
    }
    protected String getWebSocketURL(HttpRequest req) {
        logger.info("req.headers(): "  + req.headers());
        return "ws://" + req.headers().get("Host") + req.uri();
    }
}