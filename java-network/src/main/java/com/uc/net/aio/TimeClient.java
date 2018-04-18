package com.uc.net.aio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * User: uc
 * Date: 2018-04-12
 * Time: 19:53
 * Desc:
 */
public class TimeClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        AsynchronousSocketChannel sc = AsynchronousSocketChannel.open();
        sc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8083), null, new CompletionHandler<Void, Object>() {
            @Override
            public void completed(Void result, Object attachment) {
                try {
                    ByteBuffer buf = ByteBuffer.wrap("测试呀！！".getBytes("UTF-8"));
                    final Future<Integer> write = sc.write(buf);

                    if ( write.get() > 0 ) {
                        ByteBuffer rbuf = ByteBuffer.allocate(1024);
                        final Future<Integer> read = sc.read(rbuf);

                        if ( read.get() > 0 ) {
                            rbuf.flip();
                            byte[] rbytes = new byte[ rbuf.remaining() ];
                            rbuf.get(rbytes);
                            System.out.println( new String( rbytes, "UTF-8"));
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });

        TimeUnit.HOURS.sleep(1);
    }
}
