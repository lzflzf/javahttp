package com.lizhifeng.study.nb;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static com.lizhifeng.study.nb.WebConfig.blackIP;


public class Server {
    public static void main(String[] args) throws IOException {
        // establish server socket
        try (ServerSocket s = new ServerSocket(8090)) {
            while (true) {
                // wait for client connection
                Socket incoming = s.accept();
//                System.out.println(incoming.getRemoteSocketAddress().toString()) ;
//                System.out.println(incoming.getInetAddress().getHostAddress());
//                System.out.println(incoming.getInetAddress().getHostName());
                new Thread(new HandleThread1(incoming)).start();

            }
        }
    }
}

// 如何从请求报文中获取参数并且调用相应的servlet 处理逻辑
// jsp 还是编译成servlet
// 如何支持多host和多webapp
// NIO
// keep-alive



