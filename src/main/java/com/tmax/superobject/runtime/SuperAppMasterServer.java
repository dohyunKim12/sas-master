package com.tmax.superobject.runtime;

import com.tmax.superobject.channelhandler.HttpChannelInboundHandler;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class SuperAppMasterServer {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(SuperAppMasterServer.class.getName());
    private EventLoopGroup eventManager;

    private final class ChannelFutureListenerImplementation implements ChannelFutureListener {
        private String channelType = null;
        public ChannelFutureListenerImplementation(String channelType) {
            this.channelType = channelType;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            logger.info(channelType + " is completed");
            //TODO: do more in here
        }
    }

    public void start() {
        eventManager = new NioEventLoopGroup(1, new DefaultThreadFactory("event-manager"));
        logger.info("start called.");
        httpChannelBootstrap().addListener(new ChannelFutureListenerImplementation("Http channel bootstrap"));
    }

    private ChannelFuture httpChannelBootstrap() {
        ServerBootstrap httpChannelServerBootstrap = new ServerBootstrap();
        httpChannelServerBootstrap.group(eventManager);
        httpChannelServerBootstrap.channel(NioServerSocketChannel.class);

        httpChannelServerBootstrap.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(1048576000));
                pipeline.addLast(new HttpChannelInboundHandler());
            }
        });
        return httpChannelServerBootstrap.bind(new InetSocketAddress(8080));
    }
}
