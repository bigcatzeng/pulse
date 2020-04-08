package com.trxs.pulse;

public class SsgAccountBaseLog
{
    private String baiduAccount;
    private String productName;
    private String dataPullTime;
    private String mark;

    public SsgAccountBaseLog(){};

    public SsgAccountBaseLog(String str1, String str2, String str3, String str4 )
    {
        baiduAccount = str1;
        productName = str2;
        dataPullTime = str3;
        mark = str4;
    }

    public String getBaiduAccount() {
        return baiduAccount;
    }

    public void setBaiduAccount(String baiduAccount) {
        this.baiduAccount = baiduAccount;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDataPullTime() {
        return dataPullTime;
    }

    public void setDataPullTime(String dataPullTime) {
        this.dataPullTime = dataPullTime;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
