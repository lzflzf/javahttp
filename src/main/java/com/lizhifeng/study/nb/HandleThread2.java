package com.lizhifeng.study.nb;

import javax.servlet.http.Cookie;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lizhifeng.study.nb.WebConfig.index;
import static com.lizhifeng.study.nb.WebConfig.rootPath;

public class HandleThread2 implements Runnable {

    private static Pattern keyNamePattern = Pattern.compile("name=\"(.*)\"");
    private static Pattern fileNamePattern = Pattern.compile("name=\"(.*?)\".*?filename=\"(.*?)\"");
    public static String SessionLock = "SessionLock" ;


    public static Map<String,Map<String,Object>>  SessionFactory = new HashMap<>() ;

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
            this.incoming.setKeepAlive(true);
            // 是否保持长连接
            // 支持长连接  虽想socket处理完100个请求再关闭 但实际情况是 如果等到处理100个请求(也许根本等不到)，此线程会一直hungup 一个客户端会占用很多的线程
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

                //System.out.println(new String(bbos.toByteArray()));


                if(bbos.toByteArray().length>0) {
                    handle(bbos.toByteArray(), outStream, inStream);
                    System.out.println("AAAAAAAAAAAAAAAAAAAAAAAA" + keepAlive+"AAAAAAAAAAAAAAAAAAAAAAAAAA"+Thread.currentThread());
                }

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

        ///

        Map<String,Cookie> requestCookies = new HashMap<String,Cookie>() ;
        Map<String,Cookie> reponseCookies = new HashMap<String,Cookie>() ;

        String JSESSIONID ;

        String cookieString = header.headers.get("Cookie") ;

        if(cookieString != null) {
            String[] cookies = cookieString.split(";");
            for (String cookie : cookies) {
                String key_value[] = cookie.split("=");
                Cookie tmpcookie = new Cookie(key_value[0].trim(), key_value[1].trim());
                requestCookies.put(key_value[0].trim(), tmpcookie);
            }
        }
        ///  此处应该有cookie数量和长度的限制 ，一切客户端的输入都是不可信的
        ///  各个浏览器对cookie的限制 https://www.cnblogs.com/henryhappier/archive/2011/03/03/1969564.html

        if (!requestCookies.containsKey("JSESSIONID")) {
            String UUID = java.util.UUID.randomUUID().toString();
            Cookie cookie = new Cookie("JSESSIONID", UUID);
            reponseCookies.put("JSESSIONID", cookie);
            Map<String, Object> session = new HashMap<String, Object>();
            session.put("requestTimes", 0);
            SessionFactory.put(UUID, session);
            JSESSIONID = UUID;
        } else {
            JSESSIONID = requestCookies.get("JSESSIONID").getValue();
        }

        int requestTimes = 0 ;

        if (SessionFactory.containsKey(JSESSIONID)) {
            requestTimes = (int) SessionFactory.get(JSESSIONID).get("requestTimes");
            SessionFactory.get(JSESSIONID).put("requestTimes", ++requestTimes);
        } else {
            SessionFactory.put(JSESSIONID, new HashMap<String, Object>());
            SessionFactory.get(JSESSIONID).put("requestTimes", ++requestTimes);
        }

        System.out.println("requestTimes AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + requestTimes);

        // SessionFactory.get(JESSIONID).put("fdsfdsfsdfsdafadsfasd",3) ;


        ////  有可能只是读到了一个header 头

        ///  开始解析 cookie

        ///  根据cookie中的sessionid获取session对象
        /// (第一次访问不会含有sessionid，需要生成一个sessionid和一个相关联的文件(序列化存储session对象))
        ///  Set-Cookie: PLAY_Online_FLASH=;Expires=Fri, 23-Mar-2018 05:29:31 GMT;Path=/
        ///  多个实例可以共享session存储的目录达到保持登录的要求，但是会不会有多个实例同时操作文件的冲突???
        ///  play处理session的手法比较特别，将session实体序列化存储到cookie中，同时为了防止cookie中的内容被篡改，
        ///  给序列化后字符串生成了一个定长的sign放在字符串前面 key-([sign][value])
        ///  if(encrypt(value)==sign)则证明session内容没有被篡改 。  (tomcat的处理则是序列化后存储到文件中)
        ///  不过这样会有一个问题，session永久不会失效
        ///  在session里面加一个过期时间参数，每次取出来和当前时间进行判断此session是否有效。。。
        ///  只要PLAY_Online_SESSION 不失效的话用户就用户可以处于登录状态了(即使修改了密码原来的cookie也照样可以登录)
        ///  只要不清除浏览器的缓存，打开网站就是处于登录状态了（比在公众场合记住密码更麻烦）
        ///  play 1.2.4 版本存在这个问题，以后版本session的处理方式不知道有没有改变
        ///  isHttpOnly 不支持
        ///
        ///  修改cookie中的sessionid 是不是就能获取到另外一个用户的信息了 session碰撞
        ///  play

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
        writeContent(ContentType, filePath, os,reponseCookies);

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
        //  没有告诉客户端content的长度，因此浏览器会一直转圈圈  // 长连接
        // out.close();
    }

    private void writeContent(String ContentType, String filePath, OutputStream outStream,Map<String,Cookie> cookieMap) throws FileNotFoundException, IOException {

        String fullFilePath = rootPath + filePath;
        File file = new File(fullFilePath);
        PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);

        if (file.exists() && file.isFile()) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + ContentType + "; charset=utf-8");
            out.println("Connection: Keep-Alive");
            out.println("Content-Length: "+ file.length());

            for (Map.Entry<String, Cookie> entry : cookieMap.entrySet()) {
                Cookie cookie = entry.getValue();
                out.println("Set-Cookie: " + cookie.getName() + "=" + cookie.getValue()+";Max-Age=-100;HTTPOnly");
            }

            // out.println("Set-Cookie: " + "Domain" + "=" + "Domain");
            //  cookie name 不能为保留字

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



