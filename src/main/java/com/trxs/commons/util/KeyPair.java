package com.trxs.commons.util;

import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyPair
{
    private PublicKey publicKey;
    private PrivateKey privateKey;

    private Base64Tools base64Tools = Base64Tools.getInstance();
    private RSATools rsaTools = RSATools.getInstance();

    public KeyPair(){}

    public String publicKey()
    {
        if ( publicKey == null ) return "";
        byte[] keyBytes = publicKey.getEncoded();
        return base64Tools.encode(keyBytes);
    }

    public String privateKey()
    {
        if ( privateKey == null ) return "";
        byte[] keyBytes = privateKey.getEncoded();
        return base64Tools.encode(keyBytes);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String base64Key)
    {
        try
        {
            setPublicKey(rsaTools.getPublicKey(base64Key));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String base64key)
    {
        try
        {
            setPrivateKey(rsaTools.getPrivateKey(base64key));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
