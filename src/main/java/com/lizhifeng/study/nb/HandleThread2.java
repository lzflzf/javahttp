package com.lizhifeng.study.nb;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lizhifeng.study.nb.WebConfig.index;
import static com.lizhifeng.study.nb.WebConfig.rootPath;

public class HandleThread2 implements Runnable {

    private static Pattern keyNamePattern = Pattern.compile("name=\"(.*)\"");
    private static Pattern fileNamePattern = Pattern.compile("name=\"(.*?)\".*?filename=\"(.*?)\"");

    private Socket incoming;

    public HandleThread2(Socket socket) {
        this.incoming = socket;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread());

        try {
            InputStream inStream = incoming.getInputStream();
            OutputStream outStream = incoming.getOutputStream();

            int keepAlive = 100;

            // while (keepAlive > 1000)
            {
                ByteArrayOutputStream bbos = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];

                int total = 1024;
                while (total == 1024) {
                    total = inStream.read(bytes);
                    bbos.write(bytes, 0, total);
                }

                System.out.println(new String(bbos.toByteArray()));
                handle(bbos.toByteArray(), outStream);

                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + keepAlive);
                keepAlive--;
            }


            // this.incoming.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void handle(byte[] bytes, OutputStream os) throws UnsupportedEncodingException, FileNotFoundException, IOException {

        int flag = 0;
        Header header = new Header();

        boolean isFirstLine = true;

        for (int i = 0; i < bytes.length - 4; i++) {
            if (bytes[i] == '\r' && bytes[i + 1] == '\n') {
                String item = new String(bytes, flag, i - flag);
                flag = i + 2;
                System.out.println(item);
                if (item.equals("")) {
                    break;        // 解析 header 结束
                }

                if (isFirstLine) {
                    String[] MethonPathHttpversion = item.split(" ");
                    header.method = MethonPathHttpversion[0];
                    header.filePath = MethonPathHttpversion[1];
                    isFirstLine = false;
                } else {
                    String[] key_value = item.split(":");
                    header.headers.put(key_value[0].trim(), key_value[1].trim());
                }
            }
        }

        ///  开始解析正文
        ///  先获取Content-type 和 分界线


        List<FormItem> FormItems = new ArrayList<FormItem>();
        String ContentType = header.headers.get("Content-Type");

        if (ContentType != null) {

            if (ContentType.startsWith("multipart/form-data")) {

                int contentBegin = 0;
                int contentEnd = 0;
                boolean isNeedToLine = true;
                boolean isFile = false;
                String keyname = "";
                String filename = "";
                boolean isItemEnd = false;
                boolean isContentBegin = true;

                String[] temp = ContentType.split("=");
                String boundary = "--" + temp[1];

                for (int i = flag; i < bytes.length; i++) {
                    if (bytes[i] == '\r' && bytes[i + 1] == '\n') {

                        if (isNeedToLine) {
                            String item = new String(bytes, flag, i - flag);
                            flag = i + 2;
                            System.out.println(item);

                            //   Content-Disposition
                            if (item.contains("Content-Disposition")) {
                                if (item.contains("filename")) {
                                    isFile = true;
                                    Matcher matcher = fileNamePattern.matcher(item);
                                    if (matcher.find()) {
                                        keyname = matcher.group(1);
                                        filename = matcher.group(2);
                                    }
                                } else {
                                    isFile = false;
                                    Matcher matcher = keyNamePattern.matcher(item);
                                    if (matcher.find()) {
                                        keyname = matcher.group(1);
                                    }
                                }
                            }


                            if (item.equals("") && isContentBegin) {
                                contentBegin = flag;
                                isContentBegin = false;
                                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + contentBegin);
                            }

                            if (item.equals(boundary) || item.equals(boundary + "--")) {                  //  如果等于分界线
                                isContentBegin = true;
                                if (isItemEnd) {
                                    FormItem formItem = new FormItem();
                                    if (isFile) {
                                        String newFile = writeFile(bytes, contentBegin, contentEnd - contentBegin + 1, filename);
                                        formItem.keyname = keyname;
                                        formItem.value = newFile;
                                        formItem.type = Type.file;
                                    } else {
                                        formItem.keyname = keyname;
                                        formItem.value = new String(bytes, contentBegin, contentEnd - contentBegin + 1);
                                        formItem.type = Type.string;
                                    }

                                    FormItems.add(formItem);
                                } else {
                                    isItemEnd = true;
                                }
                            }
                            contentEnd = i - 1;

                        }
                    }
                }
            } else if (ContentType.startsWith("application/x-www-form-urlencoded")) {
                String content = new String(bytes, flag, bytes.length - flag);

                String[] items = content.split("\\&");
                for (String item : items) {
                    String[] key_vale = item.split("=");
                    FormItem formItem = new FormItem();
                    formItem.keyname = key_vale[0];
                    formItem.type = Type.string;

                    if (key_vale.length == 2) {
                        formItem.value = key_vale[1];
                    } else {
                        formItem.value = "";
                    }

                    FormItems.add(formItem);
                }

                System.out.println(content);
            }
        }


        String filePath = header.filePath;

        ContentType = MimeTypes.getMimeType(filePath);

        if (filePath.equals("/b.html")) {
            writeFormItems(FormItems, os);
            return;
        }

        writeContent(ContentType, filePath, os);

        System.out.println("end!!!!!!!");
    }

    private void writeFormItems(List<FormItem> formItems, OutputStream outStream) {

        PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Connection: Keep-Alive");
        out.println();    //  输出header头

        for (FormItem formItem : formItems) {
            if (formItem.type == Type.string) {
                out.println(String.format("key为%s  , value为%s</br></br>", formItem.keyname, formItem.value));
            } else {
                out.println(String.format("key为%s  , 文件路径为<a href=%s target= _blank>点击验证此文件是不是您刚才上传的文件</a></br></br>", formItem.keyname, formItem.value));
            }
        }
        out.flush();
        out.close();

    }

    private void writeContent(String ContentType, String filePath, OutputStream outStream) throws FileNotFoundException, IOException {

        String fullFilePath = rootPath + filePath;
        File file = new File(fullFilePath);
        PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);

        if (file.exists() && file.isFile()) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + ContentType + "; charset=utf-8");
            out.println("Connection: Keep-Alive");
            // out.println("Cache-Control: max-age=31536000, public");
            // out.println("Content-Encoding: gzip");  // 正文使用gzip进行压缩
            out.println();    //  输出header头

            InputStream filein = new BufferedInputStream(new FileInputStream(file));
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
            out.println("404页面");
        }

        /// out.println();  不需要的
        out.flush();
        outStream.close();
        out.close();
    }


    public String writeFile(byte[] bytes, int off, int length, String fileName) throws FileNotFoundException, IOException {
        String[] tempArray = fileName.split("\\.");
        String newFileNmae = "tmp/" + UUID.randomUUID().toString().replaceAll("-", "") + "." + tempArray[1];

        File file = new File(rootPath + newFileNmae);
        OutputStream outputStream = new FileOutputStream(file);
        outputStream.write(bytes, off, length);
        outputStream.flush();
        outputStream.close();

        return newFileNmae;
    }

}



