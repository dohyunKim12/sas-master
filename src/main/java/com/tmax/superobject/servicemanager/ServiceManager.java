package com.tmax.superobject.servicemanager;

import com.tmax.superobject.channelhandler.WebSocketChannelInboundHandler;
import com.tmax.superobject.config.Global;
import com.tmax.superobject.constant.SapConstants;
import com.tmax.superobject.constant.SapConstants.ContentType;
import com.tmax.superobject.listener.CompletionListener;
import com.tmax.superobject.object.*;
import com.tmax.superobject.object.TaskObject.ProcessState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServiceManager {
    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class.getName());

    private static ServiceManager serviceManager = new ServiceManager();
    private EventLoopGroup eventManager;
    private EventLoopGroup worker;
    private ClassLoader serviceClassLoader;
    private HashMap<Long, TaskObject> outboundRequestMap;
    private HashMap<String, List<TaskObject>> waitingTaskObjectMap;

    public HashMap<Long, TaskObject> getOutboundRequestMap() {
        return outboundRequestMap;
    }

    private ServiceManager() {
        // TODO: get config from... somewhere
        Global.getInstance().config();
        serviceClassLoader = Global.getInstance().getServiceClassLoader();
        eventManager = new NioEventLoopGroup(1, new DefaultThreadFactory("event-manager"));
        worker = new NioEventLoopGroup(100, new DefaultThreadFactory("service-worker"));

        // only accces in single thread(such as EM)
        outboundRequestMap = new HashMap<>();
        waitingTaskObjectMap = new HashMap<>();
    }

    public void handleOutboundResponse(MessageObject messageObject) {
        TaskObject waitingOutboundTaskObject = outboundRequestMap.get(messageObject.header().requestId());
        if (waitingOutboundTaskObject != null) {
            waitingOutboundTaskObject.setState(ProcessState.AFTER_SERVICE);
            waitingOutboundTaskObject.handleOutboundResponse(messageObject.body());
            resumeTask(waitingOutboundTaskObject);
        }
    }

    public void resumeTask(TaskObject taskObject) {
        eventManager.execute(taskObject);
    }

    public static ServiceManager getInstance() {
        return serviceManager;
    }

    private void connectWebSocket(URI uri) {
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(eventManager).channel(NioSocketChannel.class).remoteAddress(uri.getHost(), uri.getPort())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(8192));
                    }
                });
        ChannelFuture channelFuture = bootstrap.connect();
        channelFuture.addListener(new WebSocketHandshakerFireListener(uri.toString()));
        channelFuture.channel().closeFuture().addListener(new ClientChannelCloseFutureListener(uri.toString()));
    }

    private class ClientChannelCloseFutureListener implements GenericFutureListener<ChannelFuture> {
        private String channelUri;

        public ClientChannelCloseFutureListener(String channelUri) {
            this.channelUri = channelUri;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            ServiceRegistry.getInstance().getServiceChannelMap().remove(channelUri);
        }

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
            logger.info("web socket connection is completed : " + channelUri + ", " + future.channel());
            Channel channel = future.channel();
            channel.pipeline().addLast(new WebSocketChannelInboundHandler(handshaker, new Runnable() {

                @Override
                public void run() {
                    ServiceRegistry.getInstance().getServiceChannelMap().put(channelUri, channel);

                    List<TaskObject> waitingList = waitingTaskObjectMap.remove(channelUri);

                    if (waitingList != null) {
                        for (TaskObject taskObject : waitingList) {
                            eventManager.execute(taskObject);
                        }
                    }
                }
            }));
        }
    }

    public EventLoopGroup getEventManager() {
        return eventManager;
    }

    public EventLoopGroup getWorker() {
        return worker;
    }

    public void execOutboundTask(OutboundTaskObject outboundTaskObject) {
        String serviceName = outboundTaskObject.getRequestMessage().header().targetServiceName();
        String serviceUri = ServiceRegistry.generateChannelUri(serviceName); // refactor required
        Channel outboundChannel = ServiceRegistry.getInstance().findOutboundChannel(serviceName);
        if (outboundChannel == null) {
            List<TaskObject> waitingList = waitingTaskObjectMap.get(serviceUri);
            if (waitingList == null) {
                waitingList = new ArrayList<>();
                waitingTaskObjectMap.put(serviceUri, waitingList);
            }
            waitingList.add(outboundTaskObject);
            try {
                logger.info("connect new channel : " + serviceUri);
                connectWebSocket(new URI(serviceUri));
            } catch (URISyntaxException e) {
                logger.warn("exception is occured while connect outbound websocket connection", e);
            }
        } else {
            MessageObject message = outboundTaskObject.getRequestMessage();
            logger.info("write and flush outbound request : " + message.toString());
            message.header().setRequestId(Global.getInstance().safeSeq());
            if (outboundChannel instanceof NioSocketChannel) {
                WebSocketFrame wsFrame = null;
                switch (message.header().contentType()) {
                    case BINARY:
                        wsFrame = message.getAsBinaryWebSocketFrame();
                        break;
                    case TEXT:
                        wsFrame = message.getAsTextWebSocketFrame();
                        break;
                    default:
                        break;
                }
                outboundChannel.writeAndFlush(wsFrame);
            } else if (outboundChannel instanceof LocalChannel) {
                outboundChannel.writeAndFlush(message);
            }
            getOutboundRequestMap().put(message.header().requestId(), outboundTaskObject);
        }
    }

    /**
     * must be called in only inbound channel handler
     * 
     * @param inboundChannel
     * @param message
     * @param completionListener
     * @return
     */
    public TaskObject callAsync(Channel inboundChannel, MessageObject message, CompletionListener completionListener) {

        TaskObject requestTaskObject = null;
        try {
            requestTaskObject = TaskObject.newInstanceFromMessageObject(message);
        } catch (Throwable t) {
            logger.error("unexpected exception is thrown", t);
        }

        requestTaskObject.setInboundChannel(inboundChannel);

        if (completionListener != null)
            requestTaskObject.addCompletionListener(completionListener);

        eventManager.execute(requestTaskObject);
        return requestTaskObject;
    }

    public TaskObject callAsync(String serviceName, Object inDto, CompletionListener completionListener,
            boolean immediate) {
        HeaderObject header = new DefaultHeaderObject();
        BodyObject body = (BodyObject) inDto;
        MessageObject message = new MessageObject(header, body);
        header.setTargetServiceName(serviceName);
        header.setMessageType(SapConstants.MessageType.REQUEST);

        if (body.getByteBuffer() != null) {
            header.setContentType(ContentType.BINARY);
        } else {
            header.setContentType(ContentType.TEXT);
        }

        TaskObject requestTaskObject = null;
        try {
            requestTaskObject = TaskObject.newInstanceFromMessageObject(message);
        } catch (Throwable t) {
            logger.error("unexpected exception is thrown", t);
        }

        requestTaskObject.setParentTask(Global.currentTask.get());

        if (completionListener != null)
            requestTaskObject.addCompletionListener(completionListener);

        if (immediate) {
            eventManager.execute(requestTaskObject);
        } else {
            Global.afterService.get().add(requestTaskObject);
        }

        return requestTaskObject;
    }

    public BodyObject callSync(String serviceName, BodyObject inDto) {
        Class<?> clazz;
        ServiceObject callee = null;
        try {
            clazz = serviceClassLoader.loadClass(serviceName);
            callee = (ServiceObject) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        callee.service(inDto);
        callee.completed();
        BodyObject response = callee.getReply();

        return response;
    }
}
