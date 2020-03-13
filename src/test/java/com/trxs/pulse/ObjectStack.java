package com.trxs.pulse;

public class ObjectStack<E>
{
    private int index;
    private int maxIndex;
    private Object[] elements;

    public ObjectStack(int capacity)
    {
        index = -1;
        maxIndex = capacity - 1;
        elements = new Object[capacity];
    }

    public int size()
    {
        return index + 1;
    }

    public int capacity()
    {
        return elements == null ? 0 : elements.length;
    }

    public boolean isEmpty()
    {
        return index >= 0 ;
    }

    public E push( E e)
    {
        if ( index < maxIndex )
        {
            elements[++index] = e;
            return e;
        }
        throw new RuntimeException("The stack is full!");
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
