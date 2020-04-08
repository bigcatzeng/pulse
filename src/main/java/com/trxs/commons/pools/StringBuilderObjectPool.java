package com.trxs.commons.pools;

public class StringBuilderObjectPool extends ObjectPool<StringBuilderX>
{
    public StringBuilderObjectPool(int maxCapacity)
    {
        super(maxCapacity);
    }
}
