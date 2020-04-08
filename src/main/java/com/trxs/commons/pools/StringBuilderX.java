package com.trxs.commons.pools;

public class StringBuilderX extends ObjectX<StringBuilder>
{
    public StringBuilderX(StringBuilderObjectPool pool)
    {
        super(pool);
        setObject(new StringBuilder(128));
    }
}
