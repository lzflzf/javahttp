package com.lizhifeng.study.nb;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.URLDecoder;
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
        //System.out.println(Thread.currentThread());

        try {
            InputStream inStream = incoming.getInputStream();
            OutputStream outStream = incoming.getOutputStream();

            int keepAlive = 100;
            this.incoming.setKeepAlive(true);  // 是否保持长连接

            while (keepAlive > 0)
            {
                ByteArrayOutputStream bbos = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];

                int total = 1024;
                while (total == 1024) {
                    total = inStream.read(bytes);
                    if (total != -1) {
                        bbos.write(bytes, 0, total);
                    }
                }
                // 客户端不停的发送byte过来，这里就呵呵啦

                //System.out.println(new String(bbos.toByteArray()));
                // 这里将socket的内容全部先读取到一个byte数组中再进行分析，如果body过大会占用很大的内存
                // 应当边读边进行分析，不过这样代码实现起来难度会大很多，
                // 即使现在将整个内容读取到一个byte数组中再进行解析，处理逻辑也是相当的别扭


                //System.out.println("KeepAlive------------" + this.incoming.getKeepAlive());

                System.out.println(new String(bbos.toByteArray()));

                handle(bbos.toByteArray(), outStream, inStream);

                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + keepAlive);
                keepAlive--;
            }

            this.incoming.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void handle(byte[] bytes, OutputStream os, InputStream inStream) throws UnsupportedEncodingException, FileNotFoundException, IOException {

        int flag = 0;
        Header header = new Header();

        boolean isFirstLine = true;

        for (int i = 0; i <= bytes.length - 2; i++) {
            if (bytes[i] == '\r' && bytes[i + 1] == '\n') {
                String item = new String(bytes, flag, i - flag);
                flag = i + 2;
                // System.out.println(item);
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

        ////  有可能只是读到了一个header 头

        ///  开始解析 cookie

        ///  根据cookie中的sessionid获取session对象
        /// (第一次访问不会含有sessionid，需要生成一个sessionid和一个相关联的文件(序列化存储session对象))
        ///  多个实例可以共享session存储的目录达到保持登录的要求，但是会不会有多个实例同时操作文件的冲突???
        ///  play处理session的手法比较特别，将session实体序列化存储到cookie中，同时为了防止cookie中的内容被篡改，
        ///  给序列化后字符串生成了一个定长的sign放在字符串前面 key-([sign][value])
        ///  if(encrypt(value)==sign)则证明session内容没有被篡改 。  (tomcat的处理则是序列化后存储到文件中)
        ///  会有多个线程同时操作这个session对象，因此序列化和反序列化应该 加锁
        ///  修改cookie中的sessionid 是不是就能获取到另外一个用户的信息了 session碰撞

        List<FormItem> FormItems = new ArrayList<FormItem>();
        ///  解析 url ?  后面的参数

        String[] path = header.filePath.split("\\?");
        if (path.length > 1) {
            header.filePath = path[0];
            String query_string = path[1];
            String[] parmArray = query_string.split("&");

            for (String parm : parmArray) {
                String[] key_value = parm.split("=");
                FormItem formItem = new FormItem();
                formItem.type = Type.string;
                formItem.keyname = URLDecoder.decode(key_value[0], "UTF-8");
                if (key_value.length == 2) {
                    formItem.value = URLDecoder.decode(key_value[1], "UTF-8");
                } else {
                    formItem.value = "";
                }
                FormItems.add(formItem);
            }
        }

        if (header.filePath.equals("/")) {
            header.filePath = "/a.html";   // 默认首页
        }

        // byte[] body  ;


        String ContentLength = header.headers.get("Content-Length");

        if (ContentLength != null) {
            //  获取body正文
            int contentLength = Integer.valueOf(ContentLength);

            if ((contentLength + flag) == bytes.length) {
                //  说明content已经被读取到byte中去了
                // body = new byte[contentLength];
                // System.arraycopy(bytes, flag, body, 0, contentLength);
            }

            if (flag == bytes.length) {
                //  说明 content 还没有读取
                ByteArrayOutputStream bbos = new ByteArrayOutputStream();
                byte[] bytetmp = new byte[1024];
                int allreadReadTotal = bytes.length;
                while (contentLength > allreadReadTotal) {
                    // 伪造一个错误的Content-Length 这里就会陷入死循环啦！！！
                    int total = 1024;
                    while (total == 1024) {
                        total = inStream.read(bytetmp);
                        if(total != -1) {
                            allreadReadTotal += total;
                            bbos.write(bytetmp, 0, total);
                        }
                    }
                }

                byte[] body = bbos.toByteArray();

                byte[] tmpByteArray = new byte[contentLength + flag];

                System.arraycopy(bytes, 0, tmpByteArray, 0, bytes.length);
                System.arraycopy(body, 0, tmpByteArray, bytes.length, contentLength);

                bytes = new byte[contentLength + flag];
                bytes = tmpByteArray;


                // System.out.println(new String(bbos.toByteArray()));
                // System.out.println("eeeeeeeeeeeeeeeeeeee");
            }
        }

        ///  开始解析正文  body  form 表单的解析
        ///  先获取Content-type 和 分界线
        ///
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
                            // System.out.println(item);

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
                                // System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + contentBegin);
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
                    formItem.keyname = URLDecoder.decode(key_vale[0], "UTF-8");
                    formItem.type = Type.string;

                    if (key_vale.length == 2) {
                        formItem.value = URLDecoder.decode(key_vale[1], "UTF-8");
                    } else {
                        formItem.value = "";
                    }

                    FormItems.add(formItem);
                }

                // System.out.println(content);
            } else if (ContentType.startsWith("text/plain")) {
                String content = new String(bytes, flag, bytes.length - flag);

                String[] items = content.split("\r\n");
                for (String item : items) {
                        // 一行一个 并且不需要使用URIDecode
                    int index = item.indexOf("=");
                    if (index > 0 && index < item.length() - 1) {
                        FormItem formItem = new FormItem();
                        formItem.keyname = item.substring(0, index);
                        formItem.value = item.substring(index + 1, item.length());
                        formItem.type = Type.string;
                        FormItems.add(formItem);
                    }

                    if (index == item.length() - 1) {
                        FormItem formItem = new FormItem();
                        formItem.keyname = item.substring(0, index);
                        formItem.value = "";
                        formItem.type = Type.string;
                        FormItems.add(formItem);
                    }
                }
            } else {

            }
        }


        String filePath = header.filePath;

        if (filePath.equals("/b.html")) {
            writeFormItems(FormItems, os);
            return;
        }

        ContentType = MimeTypes.getMimeType(filePath);
        writeContent(ContentType, filePath, os);

        // System.out.println("end!!!!!!!");
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
        // out.close();
    }

    private void writeContent(String ContentType, String filePath, OutputStream outStream) throws FileNotFoundException, IOException {

        String fullFilePath = rootPath + filePath;
        File file = new File(fullFilePath);
        PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);

        if (file.exists() && file.isFile()) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + ContentType + "; charset=utf-8");
            out.println("Connection: Keep-Alive");
            out.println("Content-Length: "+ file.length());
            // out.println("Cache-Control: max-age=31536000, public");
            // out.println("Content-Encoding: gzip");  // 正文使用gzip进行压缩
            out.println();    //  输出header头
            out.flush();

            InputStream filein = new BufferedInputStream(new FileInputStream(file));
            byte[] bytes = new byte[8192];
            int total = filein.read(bytes);
            while (total != -1) {
                outStream.write(bytes, 0, total);
                outStream.flush();
                total = filein.read(bytes);
            }
            outStream.flush();
        } else {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-Length: 8");
            out.println();
            out.println("404 page");
            out.flush();
        }

    }


    public String writeFile(byte[] bytes, int off, int length, String fileName) throws FileNotFoundException, IOException {

        if (fileName.equals("")) {
            return "";                        //  空文件
        }

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



