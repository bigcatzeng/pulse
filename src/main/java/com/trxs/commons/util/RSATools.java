package com.trxs.commons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RSATools
{
    private static Logger log = LoggerFactory.getLogger(RSATools.class);
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    private FileTools fileTools = FileTools.getInstance();
    private Base64Tools base64Tools = Base64Tools.getInstance();

    private RSATools(){}

    private enum Singleton
    {
        INSTANCE;
        private Object singleton;
        Singleton()
        {
            singleton = new RSATools();
        }
        public Object getInstance()
        {
            return singleton;
        }

    }

    public static RSATools getInstance()
    {
        return (RSATools) Singleton.INSTANCE.getInstance();
    }

    /**
     *
     * java
     * Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
     * android
     * Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
     *
     */

    /**
     * 加密
     * @param values
     * @param key
     * @return
     * @throws Exception
     */
    public byte[] encryptByPublicKey( byte[] values, String key ) throws Exception
    {
        PublicKey publicKey = getPublicKey(key);
        return encrypt(values,publicKey);
    }

    public byte[] encryptByPrivateKey( byte[] values, String key ) throws Exception
    {
        PrivateKey privateKey = getPrivateKey(key);
        return encrypt(values, privateKey);
    }

    /**
     * 解密
     * @param values
     * @param key
     * @return
     * @throws Exception
     */
    public byte[] encrypt( byte[] values, Key key ) throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedValues = cipher.doFinal(values);
        return encryptedValues;
    }

    /**
     * 使用getPublicKey得到公钥,返回类型为PublicKey
     *
     * @param key String to PublicKey
     * @throws Exception
     */
    public PublicKey getPublicKey(String key) throws Exception
    {
        byte[] keyBytes;
        keyBytes = (new BASE64Decoder()).decodeBuffer(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }


    public byte[] decryptByPrivateKey( byte[] values, String key ) throws Exception
    {
        PrivateKey privateKey = getPrivateKey(key);
        return decrypt( values, privateKey);
    }

    public byte[] decryptByPublicKey( byte[] values, String key ) throws Exception
    {
        PublicKey publicKey = getPublicKey(key);
        return decrypt( values, publicKey);
    }

    public byte[] decrypt( byte[] values, Key key ) throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] originalValues = cipher.doFinal(values);
        return originalValues;
    }

    /**
     * 转换私钥
     *
     * @param key String to PrivateKey
     * @throws Exception
     */
    public PrivateKey getPrivateKey(String key) throws Exception
    {
        byte[] keyBytes;
        keyBytes = (new BASE64Decoder()).decodeBuffer(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    /**
     * 签名
     */
    public byte[] sign(byte[] data, String key) throws Exception
    {
        PrivateKey priK = getPrivateKey(key);
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initSign(priK);
        sig.update(data);
        return sig.sign();
    }

    /**
     * 验证
     */
    public boolean verify(byte[] data, byte[] sign, String key) throws Exception
    {
        PublicKey pubK = getPublicKey(key);
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initVerify(pubK);
        sig.update(data);
        return sig.verify(sign);
    }

    /**
     * 生成公钥和私钥
     * @throws NoSuchAlgorithmException
     *
     */
    public com.trxs.commons.util.KeyPair keyPairGenerator() throws NoSuchAlgorithmException
    {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);
        java.security.KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return newKeyPairs(privateKey,publicKey);
    }

    /**
     * 生成公钥和私钥
     *
     */
    public com.trxs.commons.util.KeyPair keyPairGenerator(String modulus, String exponent)
    {
        PrivateKey privateKey = getPrivateKey(modulus,exponent);
        PublicKey   publicKey = getPublicKey(modulus, exponent);
        return newKeyPairs(privateKey,publicKey);
    }

    public com.trxs.commons.util.KeyPair newKeyPairs(PrivateKey privateKey, PublicKey   publicKey )
    {
        com.trxs.commons.util.KeyPair keyPair = new com.trxs.commons.util.KeyPair();
        keyPair.setPrivateKey(privateKey);
        keyPair.setPublicKey(publicKey);

        return keyPair;
    }

    /**
     * 使用模和指数生成RSA私钥
     * 注意：【此代码用了默认补位方式，为RSA/None/PKCS1Padding，不同JDK默认的补位方式可能不同，如Android默认是RSA
     * /None/NoPadding】
     *
     * @param modulus
     *            模
     * @param exponent
     *            指数
     * @return
     */
    public static PrivateKey getPrivateKey(String modulus, String exponent)
    {
        try
        {
            BigInteger b1 = new BigInteger(modulus);
            BigInteger b2 = new BigInteger(exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(b1, b2);
            return keyFactory.generatePrivate(keySpec);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 使用模和指数生成RSA公钥
     * 注意：【此代码用了默认补位方式，为RSA/None/PKCS1Padding，不同JDK默认的补位方式可能不同，如Android默认是RSA
     * /None/NoPadding】
     *
     * @param modulus
     *            模
     * @param exponent
     *            指数
     * @return
     */
    public static PublicKey getPublicKey(String modulus, String exponent) {
        try {
            BigInteger b1 = new BigInteger(modulus);
            BigInteger b2 = new BigInteger(exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(b1, b2);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String readRsaPrivateKey(String filePath)
    {
        Path path = Paths.get(filePath);
        List<String> lines = new ArrayList<>(16);
        fileTools.readLines(path).forEach( line ->
        {
            if( line.matches("(\\-)+((BEGIN|END) RSA PRIVATE KEY)(\\-)+")) return;
            lines.add(line);
        });

        return String.join("\n", lines);
    }

    public String readRsaPrivateKeyWithPKCS8(String filePath)
    {
        Path path = Paths.get(filePath);
        List<String> lines = new ArrayList<>(16);
        fileTools.readLines(path).forEach( line ->
        {
            if( line.matches("(\\-)+((BEGIN|END) PRIVATE KEY)(\\-)+")) return;
            lines.add(line);
        });

        return String.join("\n", lines);
    }

    public String readRsaPublicKey(String filePath)
    {
        Path path = Paths.get(filePath);
        List<String> lines = new ArrayList<>(16);
        fileTools.readLines(path).forEach( line ->
        {
            if( line.matches("(\\-)+((BEGIN|END) PUBLIC KEY)(\\-)+")) return;
            lines.add(line);
        });

        return String.join("\n", lines);
    }

    /**
     * 从证书文件读取公钥
     * @param cerPath
     * @return
     */
    public String readRsaPublicKeyFromCER(String cerPath)
    {
        try
        {
            CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
            FileInputStream fis = null;
            fis = new FileInputStream(cerPath);
            X509Certificate Cert = (X509Certificate) certificatefactory.generateCertificate(fis);
            PublicKey pk = Cert.getPublicKey();
            String publicKey = base64Tools.encode(pk.getEncoded());
            return publicKey;
        }
        catch (FileNotFoundException | CertificateException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 从证书库读取私钥
     * @param storePath
     * @param alias 证书别名
     * @param storePwd 证书密码
     * @return
     */
    public String readRsaPrivateKeyFromKeyStore(String storePath, String alias, String storePwd)
    {
        try
        {
            FileInputStream is = new FileInputStream(storePath);
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(is, storePwd.toCharArray());
            is.close();
            PrivateKey key = (PrivateKey) ks.getKey(alias, storePwd.toCharArray());
            String privateKey = base64Tools.encode(key.getEncoded());
            return privateKey;
        }
        catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
