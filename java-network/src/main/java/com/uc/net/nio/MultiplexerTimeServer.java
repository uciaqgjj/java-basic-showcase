package com.uc.net.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

	private static final Logger L = 
			LoggerFactory.getLogger( MultiplexerTimeServer.class );
	
	private Selector selector;
	
	private ServerSocketChannel channel;
	
	private volatile boolean stop = false;
	
	public MultiplexerTimeServer() {
		int port = 8082;
		try {
			channel = ServerSocketChannel.open();
			selector = Selector.open();
			
			channel.configureBlocking( false );
			channel.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 1024);

			channel.register(selector, SelectionKey.OP_ACCEPT);
			L.info("Multiplexer server listen port {}", port);
		} catch (IOException e) {
			L.error("Multiplexer Server init fail!", e);
		}
	}

	public void  stop() {
		this.stop = true;
	}
	
	@Override
	public void run() {
		while (!stop) {
			try {
				selector.select( 1000 );
				final Set<SelectionKey> selectionKeys = selector.selectedKeys();
				final Iterator<SelectionKey> iterator = selectionKeys.iterator();

				SelectionKey key = null;
				while ( iterator.hasNext() ) {
					key = iterator.next();
					iterator.remove();

					try {
						handleInput( key );
					} catch ( Exception ex ) {
						L.info("Server handle input ex", ex);
						if ( key != null ) {
							key.cancel();
							if ( key.channel() != null ) {
								key.channel().close();
							}
						}
					}
				}
			} catch (IOException e) {
				L.info("Server select exception ", e);
			}
		}

		if ( selector != null ) {
			try {
				selector.close();
			} catch (IOException e) {
				L.error("Close selector error", e);
			}
		}
	}

	private void handleInput(SelectionKey key) throws IOException {
		if ( !key.isValid() ) {
			return;
		}

		if ( key.isAcceptable() ) {
			ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
			SocketChannel sc = ssc.accept();
			sc.configureBlocking( false );
			sc.register(selector, SelectionKey.OP_READ);
			L.info("Server accept client {}", sc.getRemoteAddress());
		} else if ( key.isReadable() ) {
			SocketChannel sc = (SocketChannel) key.channel();
			ByteBuffer buf = ByteBuffer.allocate( 1024 );
			final int readBytes = sc.read(buf);

			if ( readBytes > 0 ) {
				buf.flip();
				byte[] bytes = new byte[ readBytes ];
				buf.get( bytes );

				String body = new String( bytes, "UTF-8").replace("\r\n", "");
				L.info("S receive {}:{}", sc.getRemoteAddress(), body);

				String resp = "QT".equalsIgnoreCase( body )?
						String.format("Now time: %1$tF %1$tT %n", Calendar.getInstance()):
						String.format("Bad CMD %s %n", body);

				doWrite( sc, resp );
			} else if ( readBytes < 0 ) {
				//remote peer close
				key.cancel();
				sc.close();
			} else {
				//do nothing
			}
		}
	}

	private void doWrite(SocketChannel sc, String resp) throws IOException {
		if ( resp == null || resp.trim().length() < 1 ) {
			return;
		}
		ByteBuffer buf = ByteBuffer.wrap( resp.getBytes("UTF-8"));
		//buf.flip();
		while ( buf.hasRemaining() ) {
			final int b = sc.write(buf);
			L.info("write {}", b);
		}
	}

}
