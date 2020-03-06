package com.trxs.pulse.data;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CronExpression
{
    public static final int    SECOND = 0;
    public static final int    MINUTE = 1;
    public static final int      HOUR = 2;
    public static final int       DAY = 3;
    public static final int     MONTH = 4;
    public static final int DAYOFWEEK = 5;
    public static final int      YEAR = 6;

    private String text;
    private String[] items = new String[7];
    public CronExpression(){}

    public CronExpression(String exp)
    {
        text = exp;
        init();
    }

    private void init()
    {
        for ( int i = 0; i < 7; ++i ) items[i] = new String("*");
        items[5] = "?";
        String []options = text.split("( )+");
        for ( int i = 0; i < options.length && i < items.length; ++i ) items[i] = options[i];
    }

    private List<Long> getTimestamps(long beginTime, long duration)
    {
        List<Long> timestamps = new ArrayList<>();
        DateTime     dateTime = new DateTime(beginTime);
        long          endTime = beginTime + duration;

        do
        {
            if ( checkTime(dateTime ) ) timestamps.add( dateTime.getMillis() );
            dateTime.plusMillis(1000);
        } while (dateTime.getMillis() < endTime);

        return timestamps;
    }

    public boolean checkTime( final DateTime dateTime )
    {
        for ( int i = 0; i < items.length; ++i )
        {
            switch (i)
            {
                case SECOND:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getSecondOfMinute(), 60) ) return false;
                    break;
                case MINUTE:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getMinuteOfHour(), 60) ) return false;
                    break;
                case HOUR:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getHourOfDay(), 24) ) return false;
                    break;
                case DAY:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getDayOfMonth(), dateTime.dayOfMonth().getMaximumValue()) ) return false;
                    break;
                case MONTH:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getMonthOfYear(), 12) ) return false;
                    break;
                case DAYOFWEEK:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getDayOfWeek(), 7) ) return false;
                    break;
                case YEAR:
                    if ( ! checkItem(i, dateTime, items[i], dateTime.getYear(), 3000) ) return false;
                    break;
            }
        }
        return true;
    }

    private boolean checkItem(final int fieldIndex, final DateTime dateTime, final String item, final int value, final int maxValue )
    {
        String []options;
        switch (getItemType(item))
        {
            case 0:
                return value == Integer.valueOf(item).intValue();
            case 1:
                return Arrays.asList(item.split(",")).stream().anyMatch( v-> Integer.valueOf(v).intValue() == value);
            case 2:
                options = item.split("-");
                if ( options.length != 2 ) return false;
                return Integer.valueOf(options[0]).intValue() <= value && value <= Integer.valueOf(options[1]).intValue();
            case 3:
                options = item.split("/");
                if ( options.length != 2 ) return false;
                int startValue = Integer.valueOf(options[0]).intValue();
                int  stepValue = Integer.valueOf(options[1]).intValue();
                do
                {
                    if ( startValue == value ) return true;
                }while ( (startValue += stepValue) <= maxValue);
                break;
            case 4: // *
                return true;
            case 5: // ?
                if ( fieldIndex == DAY || fieldIndex == DAYOFWEEK )
                    return true;
                else
                    return false;
            case 6: // L
                if ( fieldIndex == DAY )
                    return dateTime.dayOfMonth().getMaximumValue() == value;
                else  if ( fieldIndex == DAYOFWEEK )
                    return dateTime.dayOfMonth().getMaximumValue() - 7 < dateTime.getDayOfMonth();
        }
        return false;
    }

    /**
     *
     * @param item
     * @return
     *
     * 0 xx
     * 1 xx,xx
     * 2 xx-xx
     * 3 xx/xx
     * 4 *
     * 5 ?
     * 6 L  Todo ...
     *
     */
    private int getItemType(String item)
    {
        if ( item.equals("*") ) return 4;
        if ( item.equals("?") ) return 5;
        if ( item.equals("L") ) return 6;

        int index = item.indexOf(",");
        if ( index >= 0 ) return 1;

        index = item.indexOf("-");
        if ( index >= 0 ) return 2;

        index = item.indexOf("/");
        if ( index >= 0 ) return 3;

        return 0;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
        init();
    }

    public String[] getItems()
    {
        return items;
    }

    public void setItems(String[] items)
    {
        this.items = items;
    }
}
