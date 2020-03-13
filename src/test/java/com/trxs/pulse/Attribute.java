package com.trxs.pulse;

public class Attribute
{
    private String key;
    private String value;

    public Attribute(String k, String v)
    {
        key = k;
        value = v;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
