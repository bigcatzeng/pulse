package com.trxs.commons.util;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全, 放心使用
 */
public class Base64Tools
{
    private Map<Thread, BASE64Decoder> base64DecoderMap = new ConcurrentHashMap<>(4);
    private Map<Thread, BASE64Encoder> base64EncoderMap = new ConcurrentHashMap<>(4);

    private enum Singleton
    {
        INSTANCE;
        private Object singleton;
        Singleton()
        {
            singleton = new Base64Tools();
        }
        public Object getInstance()
        {
            return singleton;
        }
    }

    public static Base64Tools getInstance()
    {
        return (Base64Tools) Singleton.INSTANCE.getInstance();
    }

    private Base64Tools()
    {
    }


    public String encode( byte[] bytes )
    {
        BASE64Encoder encoder;
        Thread self = Thread.currentThread();
        encoder = base64EncoderMap.get(self);
        if ( encoder == null )
        {
            encoder = new BASE64Encoder();
            base64EncoderMap.put(self,encoder);
        }

        return encoder.encode(bytes);
    }

    public byte[] decode( String base64Text )
    {
        Thread self = Thread.currentThread();
        BASE64Decoder decoder = base64DecoderMap.get(self);
        if ( decoder == null )
        {
            decoder = new BASE64Decoder();
            base64DecoderMap.put(self,decoder);
        }

        try
        {
            return decoder.decodeBuffer(base64Text);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return new byte[0];
    }
}
