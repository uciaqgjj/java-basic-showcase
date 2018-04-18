package com.uc.net.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * User: uc
 * Date: 2018-04-12
 * Time: 18:25
 * Desc:
 */
public class TimeClient implements Runnable{

    private static final Logger L = LoggerFactory.getLogger(TimeClient.class);

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Selector selector;

    private SocketChannel sc;

    private volatile boolean stop = false;

    public TimeClient() {
        try {
            selector = Selector.open();
            sc = SocketChannel.open();
            sc.configureBlocking( false );
        } catch (IOException e) {
           L.error("Init TimeClient Fail", e);
        }
    }

    public void stop() {
        this.stop = true;
    }

    @Override
    public void run() {
        try {
            doConnect();
        } catch (IOException e) {
            L.info("Connect fail !", e);
            System.exit(1);
        }

        while (!stop) {
            try {
                selector.select( 1000 );
                final Iterator<SelectionKey> itrs = selector.selectedKeys().iterator();
                SelectionKey key = null;
                while (itrs.hasNext()) {
                    key = itrs.next();
                    itrs.remove();

                    try {
                        handleInput( key );
                    } catch (Exception ex) {
                        L.error("HandleInput error ", ex);
                        if ( key != null ) {
                            key.cancel();
                            if ( key.channel() != null ) {
                                key.channel().close();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                L.error("Select error", e);
            }
        }

        if ( selector != null ) {
            try {
                selector.close();
            } catch (IOException e) {
                L.error("Selector Close Error", e);
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException, InterruptedException {
        if ( !key.isValid() ) {
            return;
        }

        if ( key.isConnectable() ) {
            if ( sc.finishConnect() ) {
                //sc.register(selector, SelectionKey.OP_READ);
                doWrite();
            } else {
                System.exit(2); //connect error
            }
            return;
        }

        if ( key.isReadable() ) {

            ByteBuffer buf = ByteBuffer.allocate(1024);

            int readBytes = sc.read(buf);
            if ( readBytes > 0) {

                buf.flip();

                byte[] bytes = new byte[ buf.remaining() ];
                buf.get( bytes );
                String resp = new String(bytes, DEFAULT_CHARSET);
                L.info("Resp: {}", resp);
                TimeUnit.SECONDS.sleep(1);
                doWrite();
            } else if ( readBytes < 0 ) {
                key.cancel();
                sc.close();
                this.stop = true;
            } else {
                //0 bytes, do nothing
            }

            return;
        }
    }

    private void doConnect() throws IOException {
        if ( sc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8082)) ) {
            //sc.register(selector, SelectionKey.OP_READ);
            doWrite();
        } else {
            sc.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite() throws IOException {
        ByteBuffer buf = ByteBuffer.wrap("QT".getBytes( DEFAULT_CHARSET ) );
        while ( buf.hasRemaining() ) {
            sc.write(buf);
        }

        sc.register(selector, SelectionKey.OP_READ);
    }

    public static void main(String[] args) {
        new Thread( new TimeClient() ).start();
    }
}
