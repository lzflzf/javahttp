package com.lizhifeng.study.nb;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Scanner;

import static com.lizhifeng.study.nb.WebConfig.index;
import static com.lizhifeng.study.nb.WebConfig.rootPath;

public class HandleThread1 implements Runnable {

    private Socket incoming;

    public HandleThread1(Socket socket) {
        this.incoming = socket;
    }

    @Override
    public void run() {
        try {

            System.out.println(Thread.currentThread());

            InputStream inStream = incoming.getInputStream();
            OutputStream outStream = incoming.getOutputStream();
            PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);
            boolean flag = false;

            String filePath = "";
            String ContentType = "";

            try (Scanner in = new Scanner(inStream)) {
                while (in.hasNextLine()) {
                    String line = in.nextLine();
                    System.out.println(line);
                    if (!flag) {
                        String[] MethonPathHttpversion = line.split(" ");
                        filePath = MethonPathHttpversion[1];

                        filePath = URLDecoder.decode(filePath,"UTF-8") ;

                        if (MethonPathHttpversion[1].equals("/")) {
                            filePath = index;
                        }

                        if (filePath.indexOf('?') > 0) {
                            filePath = filePath.substring(0, filePath.indexOf('?'));
                        }

                        ContentType = MimeTypes.getMimeType(filePath) ;

                        flag = true;    //  解析第一行
                        continue;
                    }

                    if (line.equals("")) {
                        break;
                    }
                }


                filePath = rootPath + filePath;
                File file = new File(filePath);
                if (file.exists()) {
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: " + ContentType + "; charset=utf-8");
                    // out.println("Content-Encoding: gzip");  // 正文使用gzip进行压缩
                    out.println();    //  输出header头

                    FileInputStream filein = new FileInputStream(file);
                    byte[] bytes = new byte[8192];
                    int total = filein.read(bytes);
                    while (total != -1) {
                        outStream.write(bytes, 0, total);
                        outStream.flush();
                        total = filein.read(bytes);
                    }
                } else {
                    out.println("HTTP/1.1 404 Not Found");
                    // out.println("Content-Type: " + ContentType + "; charset=utf-8");
                    out.println();
                }

                out.flush();
                outStream.close();
                out.close();
                incoming.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 如何从请求报文中获取参数并且调用相应的servlet 处理逻辑
    // jsp 还是编译成servlet
    // 如何支持多host和多webapp
}

