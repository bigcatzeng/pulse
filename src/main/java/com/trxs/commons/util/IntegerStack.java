package com.trxs.commons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegerStack
{
    private int index;
    private int maxIndex;
    private int maxCapacity = 1024;
    private int defaultCapacity = 32;
    private volatile Integer[] elements;

    public IntegerStack(int limit)
    {
        index       = -1;
        maxIndex    = defaultCapacity - 1;
        maxCapacity = limit;
        elements    = new Integer[defaultCapacity];
    }


    public int size()
    {
        return index + 1;
    }

    public int capacity()
    {
        return elements.length;
    }

    public int maxCapacity()
    {
        return maxCapacity;
    }

    public boolean isEmpty()
    {
        return index < 0;
    }

    public boolean isNotEmpty()
    {
        return index >= 0 ;
    }

    public void push( Integer e)
    {
        if ( index < maxIndex )
        {
            elements[++index] = e;
            return;
        }

        if ( dilatation(e) ) return;

        throw new RuntimeException("The stack was overflow!");
    }

    // ˌdaɪləˈteɪʃn
    private boolean dilatation( Integer e)
    {
        if ( elements.length*2 > maxCapacity ) return false;

        Integer[] newElements = new Integer[elements.length*2];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        maxIndex = newElements.length - 1;
        elements = newElements;

        elements[++index] = e;

        return true;
    }

    public Integer pop()
    {
        if ( index < 0 ) throw new RuntimeException("The stack is empty!");
        return elements[index--];
    }

    public Integer peek()
    {
        if ( index < 0 ) throw new RuntimeException("The stack is empty!");
        return elements[index];
    }
}
