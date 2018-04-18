package com.uc.netty.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * User: uc
 * Date: 2018-04-16
 * Time: 22:20
 * Desc:
 */
@Slf4j
public class TimeServer {

    private static class TimeServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf in = (ByteBuf)msg;
            log.info("Receive msg: {}", in.toString(StandardCharsets.UTF_8));

            ctx.write(
                    Unpooled.wrappedBuffer(
                            String.format("NowTime: %d", System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)
                    )
            );
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    .addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.info("Inbound Chandle ex!", cause);
            ctx.close();
        }
    }

    private void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .localAddress(8082)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new TimeServerHandler());
                    }
                });

        ChannelFuture channelFuture = bootstrap.bind().sync();

        channelFuture.channel().closeFuture().sync();
    }

    public static void main(String[] args) throws InterruptedException {
        new TimeServer().start();
    }
}
