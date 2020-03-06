package com.trxs.commons.convert;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全, 放心使用
 */
public class Base64Tools
{
    private Map<Thread, Base64.Decoder> base64DecoderMap = new ConcurrentHashMap<>(4);
    private Map<Thread, Base64.Encoder> base64EncoderMap = new ConcurrentHashMap<>(4);

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
        //BASE64Encoder encoder;
        Base64.Encoder encoder;
        Thread self = Thread.currentThread();
        encoder = base64EncoderMap.get(self);
        if ( encoder == null )
        {
            encoder = Base64.getEncoder();;
            base64EncoderMap.put(self,encoder);
        }

        return encoder.encodeToString(bytes);
    }

    public byte[] decode( String base64Text )
    {
        Thread self = Thread.currentThread();

        Base64.Decoder decoder = base64DecoderMap.get(self);

        if ( decoder == null )
        {
            decoder = Base64.getDecoder();
            base64DecoderMap.put(self,decoder);
        }

        try
        {
            return decoder.decode(base64Text);
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return new byte[0];
    }
}
