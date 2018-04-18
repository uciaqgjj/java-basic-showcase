package com.uc.netty.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * User: uc
 * Date: 2018-04-17
 * Time: 19:13
 * Desc:
 */
@Slf4j
public class TimeClient {

    private static class TimeClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(
                    Unpooled.wrappedBuffer("Query Time".getBytes(StandardCharsets.UTF_8))
            );
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            log.info("Server Resp: {}", msg.toString(StandardCharsets.UTF_8));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("Client Error!", cause);
            ctx.close();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress("127.0.0.1", 8082)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast( new TimeClientHandler() );
                        }
                    });

            final ChannelFuture f = bootstrap.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }

    }
}
