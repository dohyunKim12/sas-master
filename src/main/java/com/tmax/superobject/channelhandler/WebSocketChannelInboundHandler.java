package com.tmax.superobject.channelhandler;

import com.google.gson.JsonObject;
import com.tmax.superobject.config.Global;
import com.tmax.superobject.constant.SapConstants.MessageType;
import com.tmax.superobject.listener.ChannelCompletionListener;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.BodyObject;
import com.tmax.superobject.object.MessageObject;
import com.tmax.superobject.servicemanager.ServiceManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.slf4j.Logger;

import java.nio.charset.Charset;

public class WebSocketChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(WebSocketChannelInboundHandler.class.getName());
    private WebSocketClientHandshaker handshaker = null;
    private Runnable completeHandshake = null;

    public WebSocketChannelInboundHandler(WebSocketClientHandshaker handshaker, Runnable runnable) {
        this.handshaker = handshaker;
        this.completeHandshake = runnable;
    }

    public WebSocketChannelInboundHandler() {
        super();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof TextWebSocketFrame) {
            logger.info("enter text websocket frame: " + ctx.channel().getClass().getName());
// <<<<<<< HEAD
//             logger.info("enter text websocket frame");

// =======
            TextWebSocketFrame webSocketFrame = (TextWebSocketFrame) msg;
            String message = webSocketFrame.content().toString(Charset.defaultCharset());

            logger.info("incoming ws message : " + message);
            JsonObject jsonObject = Global.gson.get().fromJson(message, JsonObject.class);

            MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);
            switch (messageObject.header().messageType()) {
                case REQUEST:
                    ServiceManager.getInstance().callAsync(ctx.channel(), messageObject,
                            new ChannelCompletionListener() {
                                @Override
                                public void afterCompletion(BodyObject responseBody) {
                                    messageObject.header().setMessageType(MessageType.RESPONSE);
                                    messageObject.setBody(responseBody);
                                    ctx.channel().writeAndFlush(messageObject.getAsTextWebSocketFrame());
                                }
                            });
                    break;
                case RESPONSE:
                    ServiceManager.getInstance().handleOutboundResponse(messageObject);
                    break;
                default:
                    break;
            }
        } else if (msg instanceof BinaryWebSocketFrame) {
            logger.info("enter binary websocket frame");
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) msg;
            MessageObject messageObject = MessageObject.newInstanceFromBinaryWebSocketFrame(binaryWebSocketFrame);
            logger.info("incoming binary message : " + messageObject.toString());
            switch (messageObject.header().messageType()) {
                case REQUEST:
                    ServiceManager.getInstance().callAsync(ctx.channel(), messageObject,
                            new ChannelCompletionListener() {
                                @Override
                                public void afterCompletion(BodyObject responseBody) {
                                    messageObject.header().setMessageType(MessageType.RESPONSE);
                                    messageObject.setBody(responseBody);
                                    BinaryWebSocketFrame response = messageObject.getAsBinaryWebSocketFrame();
                                    ctx.channel().writeAndFlush(response);
                                }
                            });
                    break;
                case RESPONSE:
                    ServiceManager.getInstance().handleOutboundResponse(messageObject);
                    break;
                default:
                    break;

            }
        } else if (msg instanceof CloseWebSocketFrame) {
            ctx.channel().close();
        } else {
            if (handshaker != null && !handshaker.isHandshakeComplete()) {
                logger.info("finish handshake");
                handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                completeHandshake.run();
            }
        }
    }
}
