package com.trxs.commons.util;

import com.trxs.pulse.jdbc.SQLRender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectStack<E>
{
    private static Logger logger = LoggerFactory.getLogger(ObjectStack.class );
    private int index;
    private int maxIndex;
    private int maxCapacity = 1024;
    private int defaultCapacity = 32;
    private volatile Object[] elements;
    private int []hashCodeList;

    public ObjectStack(int limit)
    {
        index       = -1;
        maxIndex    = defaultCapacity - 1;
        maxCapacity = limit < 32 ? 32 : limit;
        elements    = new Object[defaultCapacity];
        hashCodeList= new int[elements.length];
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

    public void init( E []es)
    {
        for ( E e : es ) push(e);
    }

    public boolean push( E e)
    {
        if ( index < maxIndex )
        {
            elements[++index] = e;
            return true;
        }

        return dilatation(e);
    }

    // ˌdaɪləˈteɪʃn
    private boolean dilatation( E e)
    {
        if ( elements.length*2 > maxCapacity )
        {
            logger.warn("The stack was overflow!!!");
            return false;
        }

        Object[] newElements = new Object[elements.length*2];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        maxIndex = newElements.length - 1;
        elements = newElements;

        elements[++index] = e;
        logger.info("Stack@{} dilatation the capacity to {}!", this.hashCode(), newElements.length);

        return true;
    }

    public E pop()
    {
        if ( index < 0 ) throw new RuntimeException("The stack is empty!");
        return (E) elements[index--];
    }

    public E peek()
    {
        if ( index < 0 ) throw new RuntimeException("The stack is empty!");
        return (E) elements[index];
    }
}
