package com.trxs.commons.pools;

public class ObjectX<T> implements AutoCloseable
{
    private ObjectPool<ObjectX> objectPool;

    private T object;

    public ObjectX(ObjectPool< ? extends ObjectX> pool)
    {
        objectPool = (ObjectPool<ObjectX>) pool;
    }

    public void setObject(T obj)
    {
        object = obj;
    }

    public T getObject()
    {
        return object;
    }

    @Override
    public void close()
    {
        objectPool.repay(this);
    }

    public void repay(ObjectX object)
    {
        objectPool.repay(object);
    }
}
