package com.trxs.commons.util;

import java.util.List;

/**
 * @Description: Description...
 * @author: ZengShengwen
 * @email: mailto:zeng.good@139.com
 * @date: 2018/5/6 0006 下午 12:21
 */
public  class LangUtils
{

    public static byte[] IntegerToBytes(int values)
    {
        byte[] buffer = new byte[Integer.SIZE/8];
        for (int i = 0; i < Integer.SIZE/8; i++)
        {
            int offset = Integer.SIZE - (i + 1) * 8;
            buffer[i] = (byte) ((values >> offset) & 0xff);
        }
        return buffer;
    }

    public static long BytesToInteger(byte[] buffer) throws Exception
    {
        long  values = 0;
        if ( Integer.SIZE/8 > buffer.length ) throw new Exception("输入参数不足以专为整数!");

        for (int i = 0; i < Integer.SIZE/8; i++)
        {
            values <<= 8; values|= (buffer[i] & 0xff);
        }
        return values;
    }

    public static byte[] LongToBytes(long values)
    {
        byte[] buffer = new byte[Long.SIZE/8];
        for (int i = 0; i < 8; i++)
        {
            int offset = 64 - (i + 1) * 8;
            buffer[i] = (byte) ((values >> offset) & 0xff);
        }
        return buffer;
    }

    public static long BytesToLong(byte[] buffer)
    {
        long  values = 0;
        for (int i = 0; i < 8; i++)
        {
            values <<= 8; values|= (buffer[i] & 0xff);
        }
        return values;
    }

    public static String formatCountDown(String countDown)
    {
        long longCountDown = Long.valueOf(countDown);

        boolean isExpired =  longCountDown < 0;

        if ( isExpired )
        {
            longCountDown = -longCountDown;
        }

        int hour = Math.toIntExact(longCountDown / 3600);
        int minut = Math.toIntExact((longCountDown % 3600 )/60);

        return String.join("",
                isExpired ? "逾期":"",
                Integer.valueOf(hour).toString(),"小时",
                Integer.valueOf(minut).toString(), "分钟");
    }

    /**
     * @Description: 手机号码掩码
     * @email: mailto:zengshengwen02@hnjing.com
     * @date: 2018年04月21日 11时45分
     */
    public static String maskPhone(String phone )
    {
        if ( phone == null ) return "";

        if ( phone.length() < 7 )
        {
            return phone;
        }

        StringBuilder sb = new StringBuilder();
        if ( phone.length() == 7 )
        {
            sb.append(phone.substring(0,3));
            for( int i = 0;i < phone.length()-5;++i ) sb.append("*");
            sb.append(phone.substring(phone.length()-2));
            return sb.toString();
        }

        sb.append(phone.substring(0,3));
        for( int i = 0;i < phone.length()-7;++i ) sb.append("*");
        sb.append(phone.substring(phone.length()-4));
        return sb.toString();
    }

    /**
     * @Description: 列转行
     * @email: mailto:zengshengwen02@hnjing.com
     * @date: 2018年04月21日 11时45分
     */
    public static String List2String(List<String> keyList )
    {
        StringBuilder sb = new StringBuilder();
        if ( keyList == null ) return sb.toString();

        keyList.forEach(key->{ if ( key!=null && key.length() > 0 ) sb.append(key).append(","); });
        String temp = sb.toString();

        return temp.length() > 0 ? temp.substring(0,temp.length()-1) : temp;
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to
     * the precision and accuracy of system timers and schedulers. The thread
     * does not lose ownership of any monitors.
     *
     * @param  timeInMillis
     *         the length of time to sleep in milliseconds
    */
    public static void SLEEP( long timeInMillis )
    {
        try
        {
            Thread.sleep(timeInMillis);
        }
        catch (InterruptedException e)
        {
            //System.out.println("interrupt Thread.sleep ...");
        }
    }
}
 
 
