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
            // s.setSoTimeout(3000);
            while (true) {
                // wait for client connection
                Socket incoming = s.accept();
//                System.out.println(incoming.getRemoteSocketAddress().toString()) ;
//                System.out.println(incoming.getInetAddress().getHostAddress());
//                System.out.println(incoming.getInetAddress().getHostName());
                // incoming.setSoTimeout(1000);
                new Thread(new HandleThread2(incoming)).start();

            }
        }
    }
}

// 如何从请求报文中获取参数并且调用相应的servlet 处理逻辑
// jsp 还是编译成servlet
// 如何支持多host和多webapp 为什么支持多host?以前厂商卖虚拟空间，一个host就可以卖给一个客户，简直就是暴利
// 一个web容器跑一个web网站，支持多webapp似乎意思不大
// NIO
// keep-alive  一次socket连接处理多个请求
// 守护进程(如何制作守护进程)
//  Cache-Control: no-cache   明确告诉服务器要读取新文件
//  Cache-Control:
//  MVC
///  压缩的处理
///  request payload  不支持压缩处理



