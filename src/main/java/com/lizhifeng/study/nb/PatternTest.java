package com.lizhifeng.study.nb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {

    public static void main(String[] args)
    {
        Pattern keyNamePattern = Pattern.compile(".*name=\"(.*)\"") ;
        Matcher matcher = keyNamePattern.matcher("Content-Disposition: form-data; name=\"address\"") ;

        if(matcher.find())
        {
            System.out.println("bbbb");
        }

        System.out.println("aaaaaaaaaaaaaaaaaa");
    }
}
