package com.lizhifeng.study.nb;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
    public static void main(String[] args) throws IOException {
        // establish server socket
        try (ServerSocket s = new ServerSocket(8090)) {
            while (true) {
                // wait for client connection
                Socket incoming = s.accept();
                new Thread(new HandleThread1(incoming)).start();
            }
        }
    }
}


