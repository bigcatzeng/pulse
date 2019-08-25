package com.trxs.commons.util;

import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @Description: Description...
 * @author: ZengShengwen
 * @email: mailto:zeng.good@139.com
 * @date: 2018/5/6 0006 下午 18:11
 */
public class MD5
{
    public static String hash(String value)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            //update(byte[])方法，输入原数据
            //类似StringBuilder对象的append()方法，追加模式，属于一个累计更改的过程
            md5.update(value.getBytes("UTF-8"));

            byte[] md5Array = md5.digest();
            return toHexString(md5Array);
        }
        catch (NoSuchAlgorithmException e)
        {
            return value;
        }
        catch (UnsupportedEncodingException e)
        {
            return value;
        }
    }

    /**
     * To byte array byte [ ].
     *
     * @param hexString the hex string
     * @return the byte [ ]
     */
    public static byte[] toByteArray(String hexString)
    {
        if (StringUtils.isEmpty(hexString))
            return null;
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() >> 1];
        int index = 0;
        for (int i = 0; i < hexString.length(); i++)
        {
            if (index  > hexString.length() - 1) return byteArray;

            byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
            byte  lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
            byteArray[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        return byteArray;
    }


    /**
     * byte[] to Hex string.
     *
     * @param byteArray the byte array
     * @return the string
     */

    public static String toHexString(byte[] byteArray)
    {
        final StringBuilder hexString = new StringBuilder();
        if (byteArray == null || byteArray.length <= 0) return null;
        for (int i = 0; i < byteArray.length; i++)
        {
            int v = byteArray[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2)
            {
                hexString.append(0);
            }
            hexString.append(hv);
        }
        return hexString.toString().toLowerCase();
    }
}
 
 
