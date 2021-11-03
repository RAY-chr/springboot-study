package com.chr.fweb.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * @author RAY
 * @descriptions 对于Server来说Netty的Handler必须是多例的
 * @since 2020/3/29
 */
@Component
public class HttpServer {

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Autowired
    private ObjectFactory<HttpServerHandler> objectFactory;
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private static int port = 8088;

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup, workerGroup) //设置链接和工作线程组
                    .channel(NioServerSocketChannel.class)  //设置服务端
                    .option(ChannelOption.SO_BACKLOG, 128) //设置客户端连接队列的链接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //设置保持活动链接状态
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            ChannelPipeline pipeline = sc.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(1024 * 64));
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                            pipeline.addLast(objectFactory.getObject());
                        }
                    });
            logger.info("服务器 is ready");
            ChannelFuture fu = bootstrap.bind(serverPort == 8080 ? port : 8089).sync();
            fu.addListener(future -> {
                if (future.isSuccess()) {
                    logger.info("监听端口" + port + "成功");
                } else {
                    System.out.println("监听失败");
                }
            });
            //监听通道
            fu.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

