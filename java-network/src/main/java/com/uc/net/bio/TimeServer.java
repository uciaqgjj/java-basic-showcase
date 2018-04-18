package com.uc.net.bio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TimeServerHanlder implements Runnable {

	private static final Logger L = LoggerFactory.getLogger( TimeServerHanlder.class );

	private static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	
	private Socket socket;
	
	public TimeServerHanlder( Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		
		try(BufferedReader in = 
				new BufferedReader( new InputStreamReader( socket.getInputStream(), DEFAULT_CHARSET ));
			PrintWriter out = new PrintWriter(
					new OutputStreamWriter(socket.getOutputStream(), DEFAULT_CHARSET), true )
		) {
			
			while( true ) {
				String body = in.readLine();
				if ( body == null ) {
					break;
				}
				
				L.info("Timeserver receive body: {}", body);
				
				out.println( "QT".equals(body)? 
						"Now time " + System.currentTimeMillis() : "Bad Order: " + body);
			}
			
		} catch (IOException e) {
			L.error("server handler network ex", e);
		} finally {
			
			try {
				if ( this.socket != null ) {
					this.socket.close();				
				}
				this.socket = null;
				L.info("close socket success");
			} catch (Exception e) {
				L.error("close socket fail", e);
			}
		}
		
		
	}
	
}

public class TimeServer {

	private static final Logger L = LoggerFactory.getLogger( TimeServer.class );
	
	public static void main(String[] args) throws IOException {
		
		int port = 8081;
		
		if ( args != null && args.length > 0 ) {
			port = Integer.parseInt( args[0] );
		}
		
		ExecutorService executor = Executors.newCachedThreadPool();
		
		try(ServerSocket server = new ServerSocket(port)) {
			L.info("The time server is start on {}", port);
			
			while ( true ) {
				Socket socket = server.accept();
				executor.execute( new TimeServerHanlder(socket) );
			}
			
		} finally {
			executor.shutdown();
		}
		
	}
	
}
