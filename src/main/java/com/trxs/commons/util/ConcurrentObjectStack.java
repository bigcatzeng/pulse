package com.trxs.commons.util;

import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentObjectStack<T> extends ObjectStack<T>
{
    AtomicInteger mutex = new AtomicInteger(Integer.valueOf(0));
    public ConcurrentObjectStack(int capacity)
    {
        super(capacity);
    }

    public T push( T e)
    {
        T t;
        try
        {
            while ( false == mutex.compareAndSet(0, 1) ) Thread.yield();
            t = super.push(e);
        }
        finally
        {
            mutex.set(0);
        }
        return t;
    }

    public T pop()
    {
        T t;
        try
        {
            while ( false == mutex.compareAndSet(0, 1) ) Thread.yield();
            t = super.pop();
        }
        finally
        {
            mutex.set(0);
        }
        return t;
    }

    public T peek()
    {

        T t;
        try
        {
            while ( false == mutex.compareAndSet(0, 1) ) Thread.yield();
            t = super.peek();
        }
        finally
        {
            mutex.set(0);
        }
        return t;
    }

}
