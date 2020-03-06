package com.trxs.commons.convert;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateConverter
{
    private static Logger logger = LoggerFactory.getLogger(DateConverter.class);

    private static final List<String> formats = new ArrayList<>(6);

    static
    {
        formats.add("yyyy-MM");
        formats.add("yyyy-MM-dd");
        formats.add("yyyy-MM-dd HH:mm");
        formats.add("yyyy-MM-dd HH:mm:ss");
        formats.add("yyyy-MM-dd HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    public static Date convert(String source) throws Exception
    {
        logger.debug("convert -> '{}'", source);

        if ( source == null || source!=null && source.trim().equals("") ) return  null;

        String value = source.trim();

        if(source.matches("^[1-9]\\d*$"))
        {
            return new Date(Long.parseLong(source));
        }
        if(source.matches("^\\d{4}-\\d{1,2}$"))
        {
            return parseDate(source, formats.get(0));
        }
        else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$"))
        {
            return parseDate(source, formats.get(1));
        }
        else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$"))
        {
            return parseDate(source, formats.get(2));
        }
        else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$"))
        {
            return parseDate(source, formats.get(3));
        }
        else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}T{1}\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}Z$"))
        {
            DateTime inDate = DateTime.parse(source);
            return inDate.toDate();
        }
        else
        {
            logger.warn("Invalid Date value -> '{}'", source);
        }
        throw new Exception(String.join( ":","无效的时间格式", source));
    }

    /**
     * 格式化日期
     * @param dateStr String 字符型日期
     * @param format String 格式
     * @return Date 日期
     */
    public static Date parseDate(String dateStr, String format)
    {
        Date date=null;
        try
        {
            DateFormat dateFormat = new SimpleDateFormat(format);
            date = dateFormat.parse(dateStr);
        } catch (Exception e)
        {
            logger.warn("parseDate <- {}",e.getMessage());
        }
        return date;
    }

    public static String dateFormat(final Date date, final String format )
    {
        DateTime dt = new DateTime(date);
        return dt.toString(format);
    }

    /**
     * 函数功能描述:UTC时间转本地时间格式
     * @param utcTime UTC时间
     * @param utcTimePatten UTC时间格式
     * @param localTimePatten   本地时间格式
     * @return 本地时间格式的时间
     * eg:utc2Local("2017-06-14 09:37:50.788+08:00", "yyyy-MM-dd HH:mm:ss.SSSXXX", "yyyy-MM-dd HH:mm:ss.SSS")
     */
    public static String utc2Local(String utcTime, String utcTimePatten, String localTimePatten)
    {
        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));//时区定义并进行时间获取
        Date gpsUTCDate = null;
        try {
            gpsUTCDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return utcTime;
        }
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTime = localFormater.format(gpsUTCDate.getTime());
        return localTime;
    }

    /**
     * 函数功能描述:UTC时间转本地时间格式
     * @param utcTime UTC时间
     * @param localTimePattern 本地时间格式(要转换的本地时间格式)
     * @return 本地时间格式的时间
     */
    public static String utc2Local(String utcTime, String localTimePattern)
    {
        String utcTimePattern = "yyyy-MM-dd";
        //UTC时间格式以 yyyy-MM-dd 开头,将utc时间的前10位截取掉,之后是含有多时区时间格式信息的数据
        String subTime = utcTime.substring(10);

        //处理当后缀为:+8:00时,转换为:+08:00 或 -8:00转换为-08:00
        if(subTime.indexOf("+") != -1){
            subTime = changeUtcSuffix(subTime, "+");
        }
        if(subTime.indexOf("-") != -1){
            subTime = changeUtcSuffix(subTime, "-");
        }
        utcTime = utcTime.substring(0, 10) + subTime;

        //依据传入函数的utc时间,得到对应的utc时间格式
        //步骤一:处理 T
        if(utcTime.indexOf("T") != -1){
            utcTimePattern = utcTimePattern + "'T'";
        }

        //步骤二:处理毫秒SSS
        if(utcTime.indexOf(".") != -1){
            utcTimePattern = utcTimePattern + " HH:mm:ss.SSS";
        }else{
            utcTimePattern = utcTimePattern + " HH:mm:ss";
        }

        //步骤三:处理时区问题
        if(subTime.indexOf("+") != -1 || subTime.indexOf("-") != -1){
            utcTimePattern = utcTimePattern + "XXX";
        }
        else if(subTime.indexOf("Z") != -1){
            utcTimePattern = utcTimePattern + "'Z'";
        }

        if("yyyy-MM-dd HH:mm:ss".equals(utcTimePattern) || "yyyy-MM-dd HH:mm:ss.SSS".equals(utcTimePattern)){
            return utcTime;
        }

        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePattern);
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date gpsUtcDate = null;
        try
        {
            gpsUtcDate = utcFormater.parse(utcTime);
        }
        catch (Exception e)
        {
            logger.error("utcTime converter localTime failed!!!", e);
            return utcTime;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(localTimePattern);
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        String localTime = simpleDateFormat.format(gpsUtcDate.getTime());
        return localTime;
    }

    /**
     * 函数功能描述:修改时间格式后缀
     * 函数使用场景:处理当后缀为:+8:00时,转换为:+08:00 或 -8:00转换为-08:00
     * @param subTime
     * @param sign
     * @return
     */
    private static String changeUtcSuffix(String subTime, String sign)
    {
        String timeSuffix = null;
        String[] splitTimeArrayOne = subTime.split("\\\\" + sign);
        String[] splitTimeArrayTwo = splitTimeArrayOne[1].split(":");
        if(splitTimeArrayTwo[0].length() < 2)
        {
            timeSuffix = "+" + "0" + splitTimeArrayTwo[0] + ":" + splitTimeArrayTwo[1];
            subTime = splitTimeArrayOne[0] + timeSuffix;
            return subTime;
        }
        return subTime;
    }

    /**
     * 函数功能描述:获取本地时区的表示(比如:第八区-->+08:00)
     * @return
     */
    public static String getTimeZoneByNumExpress()
    {
        Calendar cal = Calendar.getInstance();
        TimeZone timeZone = cal.getTimeZone();
        int rawOffset = timeZone.getRawOffset();
        int timeZoneByNumExpress = rawOffset/3600/1000;
        String timeZoneByNumExpressStr = "";
        if(timeZoneByNumExpress > 0 && timeZoneByNumExpress < 10)
        {
            timeZoneByNumExpressStr = "+" + "0" + timeZoneByNumExpress + ":" + "00";
        }
        else if(timeZoneByNumExpress >= 10)
        {
            timeZoneByNumExpressStr = "+" + timeZoneByNumExpress + ":" + "00";
        }
        else if(timeZoneByNumExpress > -10 && timeZoneByNumExpress < 0)
        {
            timeZoneByNumExpress = Math.abs(timeZoneByNumExpress);
            timeZoneByNumExpressStr = "-" + "0" + timeZoneByNumExpress + ":" + "00";
        }
        else if(timeZoneByNumExpress <= -10)
        {
            timeZoneByNumExpress = Math.abs(timeZoneByNumExpress);
            timeZoneByNumExpressStr = "-" + timeZoneByNumExpress + ":" + "00";
        }
        else
        {
            timeZoneByNumExpressStr = "Z";
        }
        return timeZoneByNumExpressStr;
    }

    public static String currentTime(long now)
    {
        return new DateTime(now).toString("HH:mm:ss");
    }
}
 
 
