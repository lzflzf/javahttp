package com.lizhifeng.study.nb;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;


public class EchoServer {
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


    public static byte[] readFileContent(String fileName) throws IOException{

        ByteArrayOutputStream bbos = new ByteArrayOutputStream();
        GZIPOutputStream os = new GZIPOutputStream(bbos, 1024);

        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(filecontent);
            os.write(filecontent);
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {

            }
        }
        return bbos.toByteArray();
    }
}


class HandleThread1 implements Runnable {

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
                            System.out.println("this is index");
                            filePath = "/index.html";            // 默认首页
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

                out.flush();
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + ContentType);
                out.println("Content-Encoding: gzip");
                out.println();                             //  输出header头

                String rootPath = "D:\\moban2770\\moban2770";

                filePath = rootPath + filePath;

                byte[] fileContent = EchoServer.readFileContent(filePath);
                outStream.write(fileContent);
                //  输出正文 // 如何压缩输出正文
                // 大文件应该分块(chunk）输出， 并且不应该是每次都读出文件的内容
                outStream.flush();

                out.flush();
                outStream.close();
                incoming.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

