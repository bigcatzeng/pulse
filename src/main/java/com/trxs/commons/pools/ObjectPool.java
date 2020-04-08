package com.trxs.commons.pools;

import com.trxs.commons.util.ObjectStack;

public class ObjectPool<T extends ObjectX>
{
    private ObjectStack<T> objectStack;

    public ObjectPool( int maxCapacity )
    {
        objectStack = new ObjectStack(maxCapacity);
    }

    public int maxCapacity()
    {
        return objectStack.maxCapacity();
    }

    public T borrow( )
    {
        return objectStack.pop();
    }

    public void repay(T o)
    {
        objectStack.push(o);
    }
}