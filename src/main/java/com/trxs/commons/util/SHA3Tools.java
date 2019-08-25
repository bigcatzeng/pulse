package com.trxs.commons.util;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.Charset;

/**
 * SHA3 哈希 Tools
 *
 * @author ZengShengwen
 * @date 2019/7/27
 *
 */
public class SHA3Tools
{
    // SHA3-224 算法
    // SHA3-256 算法
    // SHA3-384 算法
    // SHA3-512 算法
    public static String encode(byte[] bytes, int bitSize )
    {
        Digest digest = new SHA3Digest(bitSize);
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return Hex.toHexString(rsData);
    }

    // SHAKE-128 算法
    // SHAKE-256 算法
    public static String shake(byte[] bytes, int bitSize)
    {
        Digest digest = new SHAKEDigest(bitSize);
        digest.update(bytes, 0, bytes.length);
        byte[] rsData = new byte[digest.getDigestSize()];
        digest.doFinal(rsData, 0);
        return Hex.toHexString(rsData);
    }

    public static void main(String[] args) {
        byte[] bytes = "dsfasdfasdfasdfadsf".getBytes(Charset.forName("UTF-8"));
        String sha3224 = encode(bytes,224);
        System.out.println("sha3-224:" + sha3224 + ",lengh=" + sha3224.length());
        String sha3256 = encode(bytes, 256);
        System.out.println("sha3-256:" + sha3256 + ",lengh=" + sha3256.length());
        String sha3384 = encode(bytes, 384);
        System.out.println("sha3-384:" + sha3384 + ",lengh=" + sha3384.length());
        String sha3512 = encode(bytes, 512);
        System.out.println("sha3-512:" + sha3512 + ",lengh=" + sha3512.length());
        String shake128 = shake(bytes, 128);
        System.out.println("shake-128:" + shake128 + ",lengh=" + shake128.length());
        String shake256 = shake(bytes, 256);
        System.out.println("shake-256:" + shake256 + ",lengh=" + shake256.length());
    }
}
