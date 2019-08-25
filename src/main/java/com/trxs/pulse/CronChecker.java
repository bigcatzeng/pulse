package com.trxs.pulse;

import org.joda.time.DateTime;

import java.util.Arrays;

/**
 * @author zengshengwen 2019 19-5-30 下午1:54
 *
 * 为减少在JVM中的小对象数量, 设计成无状态的单实例类
 * 表达式: 秒 分 时 日 月 周 年
 *
 * 用法:
 *      CronChecker checker = CronChecker.getInstance();
 *      boolean result = checker.canRunning("* 10 23 L  5 5 2019", curTime.getMillis());
 *
 */
public class CronChecker
{

    // 私有构造方法
    private CronChecker(){}
    /*
        cron表达式: 秒 分 时 日 月 周 年
        * 表示所有值；
        L 表示最大值, 只在月位置有效
        - 表示一个指定的范围；
        , 表示附加一个可能值；
        / 符号前表示开始时间，符号后表示每次递增的值；
        cron 表达式 秒 分 时 日 月 周 年
    */
    public static CronChecker getInstance()
    {
        return Singleton.INSTANCE.getInstance();
    }

    private enum Singleton
    {
        INSTANCE;
        private CronChecker singleton;
        Singleton()
        {
            singleton = new CronChecker();
        }

        public CronChecker getInstance()
        {
            return singleton;
        }
    }

    // cron表达式: 秒 分 时 日 月 周 年
    public String time2Expression(long time)
    {
        DateTime dateTime = new DateTime(time);

        int second = dateTime.getSecondOfMinute();
        int minute = dateTime.getMinuteOfHour();
        int   hour = dateTime.getHourOfDay();
        int    day = dateTime.getDayOfMonth();
        int  month = dateTime.getMonthOfYear();
        int   week = dateTime.getDayOfWeek();
        int   year = dateTime.getYear();
        return String.format("%d %d %d %d %d %d %d", second, minute, hour, day, month, week, year);
    }

    /*
    cron表达式: 秒 分 时 日 月 周 年
    * 表示所有值；
    L 表示最大值, 只在月位置有效
    - 表示一个指定的范围；
    , 表示附加一个可能值；
    / 符号前表示开始时间，符号后表示每次递增的值；
    cron 表达式 秒 分 时 日 月 周 年
    */
    public long expression2Time(String expression)
    {
        if ( expression.indexOf("*") >=0 ) return 0;
        if ( expression.indexOf("L") >=0 ) return 0;
        if ( expression.indexOf("-") >=0 ) return 0;
        if ( expression.indexOf(",") >=0 ) return 0;
        if ( expression.indexOf("/") >=0 ) return 0;

        String[]expItems = expression.split("(\t| )+");

        int second = Integer.valueOf(expItems[0]).intValue();
        int minute = Integer.valueOf(expItems[1]).intValue();
        int   hour = Integer.valueOf(expItems[2]).intValue();
        int    day = Integer.valueOf(expItems[3]).intValue();
        int  month = Integer.valueOf(expItems[4]).intValue();
        int   year = Integer.valueOf(expItems[6]).intValue();

        DateTime dateTime = new DateTime(year,month,day,hour,minute,second);
        return dateTime.getMillis();
    }

    public boolean canRunning(String[]expItems, long time)
    {
        final DateTime dateTime = new DateTime(time);
        for ( int i = expItems.length -1; i >= 0; i-- )
        {
            if ( 0 == checkItem(dateTime, i, expItems[i]) ) return false;
        }
        return true;
    }

    private int checkItem( DateTime time, int index, final String item)
    {
        switch (index)
        {
            case 0: return check(time.getSecondOfMinute(),item);// 秒
            case 1: return check(time.getMinuteOfHour(),item);  // 分
            case 2: return check(time.getHourOfDay(),item);     // 时
            case 3: return checkWithLast(time.getDayOfMonth(),item); // 日 支持 L(last)
            case 4: return check(time.getMonthOfYear(),item);        // 月
            case 5: return check(time.getDayOfWeek(),item);          // 周
            case 6: return check(time.getYear(),item);               // 年
        }
        return 0;
    }

    private int check(int value, String item)
    {
        if ( item.equals("*") ) return 1;
        if ( item.matches("\\d+") )         return checkOneValue( Integer.valueOf(item).intValue(), value );
        if ( item.matches("\\d+-\\d+") )    return checkInRange( item, value );
        if ( item.matches("\\d+(,\\d+)+") ) return checkInAssemble( item, value );
        if ( item.matches("\\d+/\\d") )     return checkValueInRepeat( item, value );
        return 0;
    }

    private int checkWithLast(int day, String item)
    {
        if ( item.equals("*") ) return 1;
        if ( item.matches("\\d+") )         return checkOneValue( Integer.valueOf(item).intValue(), day );
        if ( item.matches("\\d+-\\d+") )    return checkInRange( item, day );
        if ( item.matches("\\d+(,\\d+)+") ) return checkInAssemble( item, day );
        if ( item.matches("\\d+/\\d") )     return checkValueInRepeat( item, day );
        if ( item.equals("L") && day == DateTime.now().getDayOfMonth() ) return 1;
        return 0;
    }

    public int checkInRange(String range, int value)
    {
        String items[] = range.split("-");
        if ( items.length != 2 ) return 0;
        if ( value >= Integer.valueOf(items[0]).intValue() && value <= Integer.valueOf(items[1]).intValue() )
            return 1;
        else
            return 0;
    }

    public int checkInAssemble(String assemble, int value)
    {
        String items[] = assemble.split(",");
        if ( Arrays.stream(items).anyMatch(item -> Integer.valueOf(item).intValue() == value ) )
            return 1;
        else
            return 0;
    }

    public int checkOneValue( int check, int value)
    {
        if ( check == value ) return 1; else return 0;
    }

    public int checkValueInRepeat(String op, int value)
    {
        String[]items = op.split("/");
        int begin = Integer.valueOf(items[0]).intValue();
        int step = items.length > 1 ? Integer.valueOf(items[1]).intValue() : 0;
        if ( step == 0 ) return value == begin ? 1 : 0;
        if ( (value - begin) % step == 0 ) return 1; else return 0;
    }
}