package com.uc.net.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * User: uc
 * Date: 2018-04-12
 * Time: 19:53
 * Desc:
 */


public class TimeServer extends Thread{

    private static final Logger L = LoggerFactory.getLogger(TimeServer.class);
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private AsynchronousServerSocketChannel asc;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public TimeServer() {
        try {
            asc = AsynchronousServerSocketChannel.open();
            asc.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8083));
            L.info("Server listen on {}", 8083);
        } catch (IOException e) {
            L.error("Init fail", e);
        }
    }

    @Override
    public void run() {
        try {
            doAccept();
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void doAccept() {
        asc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                asc.accept(null, this);
                try {
                    L.info("R connect: {}",  result.getRemoteAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteBuffer buf = ByteBuffer.allocate(1024);
                result.read(buf, buf, new ReadCompletionHandler( result ));
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                countDownLatch.countDown();
            }
        });
    }

    private  static  class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

        private AsynchronousSocketChannel sc;

        public ReadCompletionHandler(AsynchronousSocketChannel sc) {
            this.sc = sc;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            attachment.flip();
            byte[] bytes = new byte[ attachment.remaining() ];
            attachment.get( bytes );
            String body = new String(bytes, DEFAULT_CHARSET);
            L.info("R body: {}", body);
            String resp = String.format("Now Time %d", System.currentTimeMillis());

            doWrite( resp );
        }

        private void doWrite(String resp) {

            ByteBuffer buf = ByteBuffer.wrap(resp.getBytes(DEFAULT_CHARSET));
            sc.write(buf, buf, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if ( attachment.hasRemaining() ) {
                        sc.write(attachment, attachment, this);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    L.info("S write error ", exc);
                }
            });
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            L.error("Read Error", exc);
            try {
                this.sc.close();
            } catch (IOException e) {
                L.error("Remote Socket close fail", e);
            }
        }
    }

    public static void main(String[] args) {
        new TimeServer().start();
    }
}
