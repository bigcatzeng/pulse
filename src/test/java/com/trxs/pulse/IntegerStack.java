package com.trxs.pulse;

public class IntegerStack
{
    private int index;
    private int maxIndex;
    private int []elements;

    public IntegerStack(int size)
    {
        index = -1;
        maxIndex = size - 1;
        elements = new int[size];
    }

    public int size()
    {
        return index + 1;
    }

    public int capacity()
    {
        return elements == null ? 0 : elements.length;
    }

    public int push( int e)
    {
        if ( index < maxIndex )
        {
            elements[++index] = e;
            return e;
        }
        throw new RuntimeException("The stack is full!");
    }

    public int pop()
    {
        if ( index < 0 ) throw new RuntimeException("The stack is empty!");
        return elements[index--];
    }

    public boolean isEmpty()
    {
        return index >= 0 ;
    }

    public int peek()
    {
        if ( index < 0 ) throw new RuntimeException("The stack is empty!");
        return elements[index];
    }
}
