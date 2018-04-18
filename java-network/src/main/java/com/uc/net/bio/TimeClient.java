package com.uc.net.bio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TimeClient {

	private static final Logger L = LoggerFactory.getLogger( TimeClient.class );

	private static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public static void main(String[] args) {
		try(Socket socket = new Socket("127.0.0.1", 8082);
			BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream(),  DEFAULT_CHARSET) );
			PrintWriter out =
					new PrintWriter( new OutputStreamWriter( socket.getOutputStream(), DEFAULT_CHARSET), true)
		) {
			
			while ( true ) {
				out.println("QT");
				String body = in.readLine();				
				L.info("Resp: {}", body);
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					L.error("Sleep Interrupt", e);
				}
			}			
		} catch (UnknownHostException e) {
			L.error( "error host!", e);
		} catch (IOException e) {
			L.error("IO Ex!", e);
		}
	}
	
}
