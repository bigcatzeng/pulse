package com.trxs.commons.encrypt;
import com.trxs.commons.convert.BytesTools;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AES
{
    public static String SECRET_KEY = "245aabdf6252e71f61f9ccc3fb759377";

    protected static Logger logger = LoggerFactory.getLogger(AES.class.getName());

    public static String genSecretKey() throws NoSuchAlgorithmException
    {
        //生成key
        SecureRandom secureRandom = new SecureRandom();
        KeyGenerator keyGenerator=KeyGenerator.getInstance("AES");
        keyGenerator.init( secureRandom );

        SecretKey secretKey = keyGenerator.generateKey();
        byte[] key1 = secretKey.getEncoded();

        String strSecretKey = Hex.encodeHexString(key1);

        logger.debug("AES密钥:{}", strSecretKey);
        return strSecretKey;
    }

    public static String encode(String key, String text)
    {
        try
        {
            //加密
            Cipher cipher= null;
            //key转换为密钥
            Key secretKey = new SecretKeySpec(Hex.decodeHex(key.toCharArray()), "AES");
            cipher = Cipher.getInstance("AES/ECB/PKCS5padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] result = cipher.doFinal(text.getBytes());
            String secretText = Hex.encodeHexString(result);

            logger.debug("AES密文:{}", secretText);
            return secretText;
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | DecoderException e)
        {
            e.printStackTrace();
        }
        return "none";
    }

    public static String decode(String key, String secretText)
    throws DecoderException, NoSuchPaddingException, NoSuchAlgorithmException,
           InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        // key转换为密钥
        Key secretKey = new SecretKeySpec(Hex.decodeHex(key.toCharArray()), "AES");
        byte[] result = Hex.decodeHex(secretText.toCharArray());
        Cipher cipher=Cipher.getInstance("AES/ECB/PKCS5padding");
        //解密
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        result = cipher.doFinal(result);

        return new String(result);
    }


    public static String encodeLong(String key, long l)
    {
        try
        {
            byte []values = new byte[8];
            //加密
            Cipher cipher= null;
            //key转换为密钥
            Key secretKey = new SecretKeySpec(Hex.decodeHex(key.toCharArray()), "AES");
            cipher = Cipher.getInstance("AES/ECB/PKCS5padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            BytesTools.longToBytes(l, values, 0);
            byte[] result = cipher.doFinal(values);
            String secretText = Hex.encodeHexString(result);

            logger.debug("AES密文:{}", secretText);
            return secretText;
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | DecoderException e)
        {
            e.printStackTrace();
        }
        return "none";
    }

    public static long decodeLong(String key, String secretText)
            throws DecoderException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        // key转换为密钥
        Key secretKey = new SecretKeySpec(Hex.decodeHex(key.toCharArray()), "AES");
        byte[] result = Hex.decodeHex(secretText.toCharArray());
        Cipher cipher=Cipher.getInstance("AES/ECB/PKCS5padding");
        //解密
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        result = cipher.doFinal(result);

        return BytesTools.bytesToLong(result,0);
    }
}
