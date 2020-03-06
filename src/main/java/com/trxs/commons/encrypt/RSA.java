package com.trxs.commons.encrypt;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Decoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSA
{
    /**
     * 加密
     * @param publicKey
     * @param srcBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    protected byte[] encrypt(RSAPublicKey publicKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        if(publicKey!=null){
            //Cipher负责完成加密或解密工作，基于RSA
            Cipher cipher = Cipher.getInstance("RSA");
            //根据公钥，对Cipher对象进行初始化
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] resultBytes = cipher.doFinal(srcBytes);
            return resultBytes;
        }
        return null;
    }

    /**
     * 解密
     * @param privateKey
     * @param srcBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    protected byte[] decrypt(RSAPrivateKey privateKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        if(privateKey!=null){
            //Cipher负责完成加密或解密工作，基于RSA
            Cipher cipher = Cipher.getInstance("RSA");
            //根据公钥，对Cipher对象进行初始化
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] resultBytes = cipher.doFinal(srcBytes);
            return resultBytes;
        }
        return null;
    }

    /**
     * 使用getPublicKey得到公钥,返回类型为PublicKey
     * @param key String to PublicKey
     * @throws Exception
     */
    public static PublicKey getPublicKey(String key)
    {
        byte[] keyBytes;
        try {
            keyBytes = (new BASE64Decoder()).decodeBuffer(key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 转换私钥
     *
     * @param key String to PrivateKey
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String key)
    {
        byte[] keyBytes;
        try {
            keyBytes = (new BASE64Decoder()).decodeBuffer(key);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            return privateKey;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param args
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        RSA rsa = new RSA();
        String msg = "zengshengwen -V-";
        String privateKey2 = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJs8c41EJgLODFUsHGaPiFfyCj8tDWQPY/Qjd7KqqctNfNIB/5b0UzJV2crl0PfnkHbOE87V83TmVUZEPL1rY+KuqZaXtwZHIWZlrZIATMRW5bzFl4LsSJoOB8QuLH8d8QVjbZ9JJiGfz9mTd7tQQkh8rGkKh0RS+8r9Q/U8lSrDAgMBAAECgYBUPV3HTzABXac7oRBYZ5NphiMhXWVi4ycumQCfqBU0CfyuSf9U/4kWS5hAjq3zmWm/ztzY5SmUSloEI4uCjwBIbVcEPaZAy8pvVLevIF+czwRzERFi0Hp5uqgjtJ2Nrkb1J4VTPkp2pWw4iTxunBAuoNft5qIqyV9dsqAqLelH4QJBANzt+yl7DuT03wrzKF9oooY6kp0ywQVINHfGpCzvDutNy15cD2reOekk3Pji5WevUsShFRxUtuuk5qNiAEHr1W0CQQCz4Nqn1NcmNAzRGpyXit1iNYA1qfofOa3oPm4k4Ho4Ey21EDe01dHjn0hZIKgCNWuXOv1UurvZsLu9CGs3vFLvAkEA0FQc3f2rit0ZofKu5GD/uMFs1Y0xlHCFAVkwISsAD4TZO85lv2l0hFP9hzg2CLK7wqz/AiskHkruLazQ/1iKdQJADImns/sJ5AfMvOZ187oiJC5GeXcXkAWdMYrocnmTC0WK8gvnVhtxPcRkbpHwI/dFQI1ECxvY7Bt9eneTwZbG0QJAL3F8MCUrs0pmEzMkVxGvbpwix6yMjy5TkFC8uabShaZxNhuuhYsrYJA182eVB8PvMCPzI1Pe4dFcvyzPRfaN/w==";
        String publicKey2 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCbPHONRCYCzgxVLBxmj4hX8go/LQ1kD2P0I3eyqqnLTXzSAf+W9FMyVdnK5dD355B2zhPO1fN05lVGRDy9a2PirqmWl7cGRyFmZa2SAEzEVuW8xZeC7EiaDgfELix/HfEFY22fSSYhn8/Zk3e7UEJIfKxpCodEUvvK/UP1PJUqwwIDAQAB";
        //KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        //初始化密钥对生成器，密钥大小为1024位
        keyPairGen.initialize(1024);
        //生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        keyPair.getPrivate();
        keyPair.getPublic();
        //得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(privateKey2);//RSAPrivateKey)keyPair.getPrivate();
        //得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) getPublicKey(publicKey2);//RSAPublicKey)keyPair.getPublic();

//        String strPrivateKey = (new BASE64Encoder()).encode(privateKey.getEncoded());
//        String strPublicKey  = (new BASE64Encoder()).encode(publicKey.getEncoded());
//        System.out.println("私钥是:" + strPrivateKey);
//        System.out.println("公钥是:" + strPublicKey);
        String strPrivateKey1 = Base64.encodeBase64String(privateKey.getEncoded());
        String strPublicKey1  = Base64.encodeBase64String(publicKey.getEncoded());
        System.out.println("私钥是1:" + strPrivateKey1);
        System.out.println("公钥是1:" + strPublicKey1);
        //用公钥加密
        byte[] srcBytes = msg.getBytes();
        byte[] resultBytes = rsa.encrypt(publicKey, srcBytes);

        //用私钥解密
        byte[] decBytes = rsa.decrypt(privateKey, resultBytes);

        System.out.println("明文是:" + msg);
        System.out.println("加密后是:" + Base64.encodeBase64String(resultBytes));
        System.out.println("解密后是:" + new String(decBytes));
    }
}
 
 
