package com.trxs.commons.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter
{

    private static Logger log = LoggerFactory.getLogger(DateConverter.class);

    private static String[]formarts =
        {
            "",

            "yyyy-MM",
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            "yyyy/MM",
            "yyyy/MM/dd",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss.SSS",
            "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'"
        };

    private static String[]regexs =
        {
            "^[1-9]\\d*$",  // 0

            "^\\d{4}-\\d{1,2}$",    // 1
            "^\\d{4}-\\d{1,2}-\\d{1,2}$",   // 2
            "^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$",  // 3
            "^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$", // 4
            "^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}$",  // 5
            "^\\d{4}-\\d{1,2}-\\d{1,2}T{1}\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}Z{1}$",  // 6

            "^\\d{4}/\\d{1,2}$",    // 7
            "^\\d{4}/\\d{1,2}/\\d{1,2}$",   // 8
            "^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}$", // 9
            "^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$", // 10
            "^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}$", // 11
            "^\\d{4}/\\d{1,2}/\\d{1,2}T{1}\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}Z{1}$" // 12
        };

    private static int []jodaArray = { 6, 12 };
    private static boolean anyMatchJode( int check )
    {
        for ( int i : jodaArray ) if ( i == check ) return true;
        return false;
    }

    public static Date convert(String source)
    {
        if ( source == null || source!=null && source.trim().equals("") ) return  null;

        String value = source.trim();
        for ( int i = 0; i < regexs.length; ++i )
        {
            if ( i == 0 && source.matches(regexs[i]) )
                return new Date(Long.parseLong(value)); // 时间戳

            // yyyy-MM-dd HH:mm:ss JDK 转换时有BUG JDK没有对非法日期作校验, 因此换JODA转换
            if ( ( i == 4 || i == 10 ) && source.matches(regexs[i]) )
                return jodaParse(value, formarts[i]);

            if ( !anyMatchJode(i) && source.matches(regexs[i]) )
            {
                return parseDate(value, formarts[i]); // parseDate(value, formarts[i]);
            }

            if ( anyMatchJode(i) && value.matches(regexs[i]) )
            {
                return jodaParse(value);
            }
        }
        return null;
    }

    public static Date jodaParse(String dateStr )
    {
        try
        {
            String value = dateStr.replaceAll("/", "-");
            DateTime inDate = DateTime.parse(value); // inDate = inDate.withFieldAdded(DurationFieldType.hours(),8);
            return inDate.toDate();
        }
        catch (Exception e)
        {
            log.warn("\njodeParse <- {}",e.getMessage());
        }
        return null;
    }

    public static Date jodaParse(String dateStr, String format )
    {
        try
        {
            String value = dateStr.replaceAll("/", "-");

            DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(format);
            DateTime inDate = DateTime.parse(value, dateTimeFormatter);
            return inDate.toDate();
        }
        catch (Exception e)
        {
            log.warn("\njodeParse <- {}",e.getMessage());
        }
        return null;
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
            log.warn("\nparseDate <- {}",e.getMessage());
        }
        return date;
    }

    public static String dateFormat(Date date, String format )
    {
        if ( date == null ) return "";
        DateTime dt = new DateTime(date);
        return dt.toString(format);
    }
}
 
 
