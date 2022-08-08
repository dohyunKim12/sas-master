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

public class SendJarWSClient {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(SendJarWSClient.class.getName());
    BodyObject response;
    public static String httpSendRequest(String targetURL, String filePath, String fileName) {
        HttpURLConnection connection = null;
        String fullPath = filePath + fileName;
        File binaryFile = new File(fullPath);

        try {
            //Create connection
            URL url = new URL(targetURL);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setRequestProperty("targetServiceName", "SaveJar");
            connection.setRequestProperty("fileName", fileName);

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Make json
            JsonObject jsonObject = new JsonObject();
            jsonObject.add(SapConstants.HEADER_KEY, new JsonObject());
            jsonObject.add(SapConstants.BODY_KEY, new JsonObject());
            MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);
            messageObject.header().setMessageType(SapConstants.MessageType.REQUEST);
            messageObject.header().setTargetServiceName("SaveJar");
            messageObject.header().setRequestId(1L);
            messageObject.body().getJsonObject().addProperty("path", "./bin/tmp/super-app-server.jar");
            File file = new File(fullPath);
            try{
                messageObject.body().setByteBuffer(ByteBuffer.wrap(Files.readAllBytes(file.toPath())));
            } catch (IOException e){
                e.printStackTrace();
            }
            // Cannot append messageObject to outputStream... (messageObject.getAsBinaryWebSocketFrame() method returns BinaryWebSocketFrame TT...

            // Set OutputStream
            DataOutputStream outputStream = new DataOutputStream (connection.getOutputStream());

            Files.copy(binaryFile.toPath(), outputStream);
            outputStream.flush();

            logger.info("File Sent: " + binaryFile.getName());
            logger.info("Type of Sent file: " + URLConnection.guessContentTypeFromName(binaryFile.getName()));
            //Get Response
            InputStream inputStream = connection.getInputStream();
            // get response in BufferedReader
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            // Use StringBuffer to read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            logger.info("Response from server: " + response);
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    public static void main(String[] args) throws URISyntaxException {
//        SendJarWSClient.httpSendRequest("http://127.0.0.1:8080", "./bin/files/", "super-app-server.jar");
        new SendJarWSClient().TestBinaryWebSocket();

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
                pipeline.addLast(new HttpObjectAggregator(8192));
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
                channel.pipeline().addLast(new WebSocketChannelInboundHandler(handshaker, new Runnable() {

                    @Override
                    public void run() {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.add(SapConstants.HEADER_KEY, new JsonObject());
                        jsonObject.add(SapConstants.BODY_KEY, new JsonObject());
                        MessageObject messageObject = MessageObject.newInstanceFromJsonObject(jsonObject);
                        messageObject.header().setMessageType(SapConstants.MessageType.REQUEST);
                        messageObject.header().setTargetServiceName("com.tmax.superobject.admin.AdminService");
                        messageObject.header().setRequestId(1L);
                        messageObject.body().getJsonObject().addProperty("path", "./bin/tmp/super-app-server.jar");
                        File file = new File("./bin/files/super-app-server.jar");
                        try {
                            messageObject.body().setByteBuffer(ByteBuffer.wrap(Files.readAllBytes(file.toPath())));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("finish send file");
                        channel.writeAndFlush(messageObject.getAsBinaryWebSocketFrame());
                    }
                }));
            }
        }

    }

}

