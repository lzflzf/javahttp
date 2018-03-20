package com.lizhifeng.study.nb;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static com.lizhifeng.study.nb.WebConfig.*;


public class Server {
    public static void main(String[] args) throws IOException {

        InetAddress address = InetAddress.getByName(host) ;
        // establish server socket
        try (ServerSocket s = new ServerSocket(port,backlog,address)) {
            while (true) {
                // wait for client connection
                Socket incoming = s.accept();
//                System.out.println(incoming.getRemoteSocketAddress().toString()) ;
//                System.out.println(incoming.getInetAddress().getHostAddress());
//                System.out.println(incoming.getInetAddress().getHostName());
                new Thread(new HandleThread2(incoming)).start();

            }
        }
    }
}

// 如何从请求报文中获取参数并且调用相应的servlet 处理逻辑
// jsp 还是编译成servlet
// 如何支持多host和多webapp
// NIO
// keep-alive  一次socket连接处理多个请求
// 守护进程(如何制作守护进程)
//  Cache-Control: no-cache   明确告诉服务器要读取新文件
//  Cache-Control:
//  MVC
///  压缩的处理



