package com.trxs.commons.convert;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class URLDecoderTools
{
    public static String encodeValue(String value)
    {
        try
        {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        }
        catch (UnsupportedEncodingException ex)
        {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static String decodeValue(String value)
    {
        try
        {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        }
        catch (UnsupportedEncodingException ex)
        {
            throw new RuntimeException(ex.getCause());
        }
    }
}
