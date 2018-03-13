package com.lizhifeng.study.nb;

import java.util.ArrayList;
import java.util.List;

public class WebConfig {

    public static  final  String rootPath = "D:\\moban2770\\moban2770\\";

    public static final String index = "index.html" ;
    // index.htm  index.jsp

    public static final String[] fobbid = new String[]{"aaaaaa","bbbbbbb"} ;

    public static final List<String> blackIP = new ArrayList<String>() ;

    static
    {
        blackIP.add("127.0.0.100") ;
    }

}
