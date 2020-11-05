package com.mars.ecsheet.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Mars
 * @date 2020/10/28
 * @description
 */
public class PakoGzipUtils {

    /**
     * @param str：正常的字符串
     * @return 压缩字符串 类型为：  ³)°K,NIc i£_`Çe#  c¦%ÂXHòjyIÅÖ`
     * @throws IOException
     */
    public static String compress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        return out.toString("ISO-8859-1");
    }


    /**
     * @param str：类型为：  ³)°K,NIc i£_`Çe#  c¦%ÂXHòjyIÅÖ`
     * @return 解压字符串  生成正常字符串。
     * @throws IOException
     */
    public static String uncompress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
        GZIPInputStream gunzip = new GZIPInputStream(in);
        byte[] buffer = new byte[256];
        int n;
        while ((n = gunzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
        // toString()使用平台默认编码，也可以显式的指定如toString("GBK")
        return out.toString();
    }

    /**
     * @param jsUriStr :字符串类型为：%1F%C2%8B%08%00%00%00%00%00%00%03%C2%B3)%C2%B0K%2CNI%03c%20i%C2%A3_%60%C3%87e%03%11%23%C2%82%0Dc%C2%A6%25%C3%82XH%C3%B2jyI%C3%85%05%C3%96%60%1E%00%17%C2%8E%3Dvf%00%00%00
     * @return 生成正常字符串
     * @throws IOException
     */
    public static String  unCompressURI(String jsUriStr) throws IOException {
        String gzipCompress=uncompress(jsUriStr);
        String result=URLDecoder.decode(gzipCompress,"UTF-8");

        return result;
    }
    /**
     * @param strData :字符串类型为： 正常字符串
     * @return 生成字符串类型为：%1F%C2%8B%08%00%00%00%00%00%00%03%C2%B3)%C2%B0K%2CNI%03c%20i%C2%A3_%60%C3%87e%03%11%23%C2%82%0Dc%C2%A6%25%C3%82XH%C3%B2jyI%C3%85%05%C3%96%60%1E%00%17%C2%8E%3Dvf%00%00%00
     * @throws IOException
     */
    public static String  compress2URI(String strData) throws IOException {
        String encodeGzip=compress(strData);
        String jsUriStr=URLEncoder.encode(encodeGzip,"UTF-8");
        return jsUriStr;
    }

}
