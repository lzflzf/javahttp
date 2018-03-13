package com.lizhifeng.study.nb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

class MimeTypes
{
    private static Properties mimetypes = null;

    static {
        try {
            InputStream is = MimeTypes.class.getResourceAsStream("/mime-types.properties");
            mimetypes = new Properties();
            mimetypes.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMimeType(String filename) {
        String MimeType = "text/plain";
        int pot = filename.lastIndexOf(".");
        if (pot > 0) {
            String ext = filename.substring(pot + 1);
            MimeType = mimetypes.getProperty(ext);
            if (MimeType == null) {
                MimeType = "text/plain";
            }
        }
        return MimeType;
    }
}

