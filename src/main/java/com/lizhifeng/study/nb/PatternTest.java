package com.lizhifeng.study.nb;

import javax.servlet.http.Cookie;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {

    public static void main(String[] args)
    {


        char ch1 = 0x20 ;
        char ch2 = 0x7f ;

        Cookie cookie = new Cookie("aaaaa","fffffff\\n\\r") ;


//        Pattern keyNamePattern = Pattern.compile(".*name=\"(.*)\"");
//        Matcher matcher = keyNamePattern.matcher("Content-Disposition: form-data; name=\"address\"");
//
//        if (matcher.find()) {
//            System.out.println("bbbb");
//        }
//
//        System.out.println("aaaaaaaaaaaaaaaaaa");
    }
}
