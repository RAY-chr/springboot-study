package com.chr.fweb.netty;

import com.google.common.base.MoreObjects;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author RAY
 * @descriptions
 * @since 2020/3/29
 */
@Component
@Scope("prototype")
public class HttpServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static Map<String, Channel> CHANNELS = new ConcurrentHashMap<>();

    public static void send(String message) {
        Channel qrcode = CHANNELS.get("qrcode");
        Objects.requireNonNull(qrcode);
        System.out.println("send线程id为："+Thread.currentThread().getId());
        qrcode.writeAndFlush(new TextWebSocketFrame(message));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        if (frame.text().startsWith("qrcode")) {
            System.out.println("Read0线程id为："+Thread.currentThread().getId());
            CHANNELS.put("qrcode", ctx.channel());
        }
        System.out.println("收到消息：" + frame.text());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel() + " 已连接");
    }
}
