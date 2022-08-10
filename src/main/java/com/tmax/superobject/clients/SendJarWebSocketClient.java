package com.tmax.superobject.clients;

import com.google.gson.JsonObject;
import com.tmax.superobject.channelhandler.WebSocketChannelInboundHandler;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.object.BodyObject;
import com.tmax.superobject.object.MessageObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class SendJarWebSocketClient {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(SendJarWebSocketClient.class.getName());
    public static void main(String[] args) throws URISyntaxException {
        new SendJarWebSocketClient().TestBinaryWebSocket();
    }
    private void TestBinaryWebSocket() throws URISyntaxException {
        URI uri = new URI("ws://localhost:8080");
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(1, new DefaultThreadFactory("test")));
        bootstrap.channel(NioSocketChannel.class).remoteAddress(uri.getHost(), uri.getPort()).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpObjectAggregator(104857600)); //100MB
            }
        });;

        ChannelFuture channelFuture = bootstrap.connect();
        channelFuture.addListener(new WebSocketHandshakerFireListener(uri.toString()));
    }
    private class WebSocketHandshakerFireListener implements GenericFutureListener<ChannelFuture> {
        String channelUri;

        public WebSocketHandshakerFireListener(String channelUri) {
            this.channelUri = channelUri;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(new URI(channelUri),
                    WebSocketVersion.V13,
                    null, false, new DefaultHttpHeaders());
            handshaker.handshake(future.channel())
                    .addListener(new WebSocketHandshakerFutureListener(channelUri, handshaker));
        }
        private class WebSocketHandshakerFutureListener implements GenericFutureListener<ChannelFuture> {
            String channelUri;
            WebSocketClientHandshaker handshaker;

            public WebSocketHandshakerFutureListener(String channelUri, WebSocketClientHandshaker handshaker) {
                this.channelUri = channelUri;
                this.handshaker = handshaker;
            }

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("handshake done");
                Channel channel = future.channel();
                logger.info("channel: "+ channel);
                channel.pipeline().addLast(new WebSocketChannelInboundHandler(handshaker, new Runnable() {

                    @Override
                    public void run() {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.add(SapConstants.HEADER_KEY, new JsonObject());
                        jsonObject.add(SapConstants.BODY_KEY, new JsonObject());
                        MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);
                        messageObject.header().setMessageType(SapConstants.MessageType.REQUEST);
                        messageObject.header().setTargetServiceName("com.tmax.superobject.service.SaveJar");
                        messageObject.header().setRequestId(1L);
                        messageObject.body().getJsonObject().addProperty("path", "./bin/tmp/super-app-server.jar");
                        File file = new File("./bin/files/super-app-server.jar");
                        try {
                            messageObject.body().setByteBuffer(ByteBuffer.wrap(Files.readAllBytes(file.toPath())));
                            logger.info("BinaryWSFrame File wrap success!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        logger.info("messageObject: "+ messageObject);
                        logger.info("messageObject.body : " + messageObject.body());
                        logger.info("messageObject.body : " + messageObject.body().getByteBuffer());
                        logger.info("messageObject.body.getJsonObject : " + messageObject.body().getJsonObject());
//                        logger.info("messageObject.getAsBinaryWebSocketFrame: " + messageObject.getAsBinaryWebSocketFrame());
//                        logger.info("messageObject.getAsBinaryWebSocketFrame: " + messageObject.getAsBinaryWebSocketFrame());
                        BinaryWebSocketFrame binaryWebSocketFrame = messageObject.getAsBinaryWebSocketFrame();
                        logger.info("binaryWSFrame: {} ", binaryWebSocketFrame);
                        channel.writeAndFlush(binaryWebSocketFrame);
//                        channel.writeAndFlush(messageObject.body());
                        System.out.println("finish send file");
                    }
                }));
            }
        }

    }

}

