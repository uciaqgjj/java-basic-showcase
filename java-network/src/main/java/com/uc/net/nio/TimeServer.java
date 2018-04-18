package com.uc.net.nio;

public class TimeServer {

    public static void main(String[] args) {
        new Thread( new MultiplexerTimeServer() ).start();
    }
}
