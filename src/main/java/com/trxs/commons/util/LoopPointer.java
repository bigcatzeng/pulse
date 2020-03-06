package com.trxs.commons.util;

/**
 * @author zengshengwen 2019 19-1-17 下午11:43
 */
public class LoopPointer
{
    private int begin,end;
    protected int currentIndex;

    public LoopPointer(){reset(0,0);}

    /**
     * @param begin
     * @param end
     * Step from begin to end, Exclude end
     */
    public LoopPointer(int begin, int end){ reset(begin,end);}

    public int next()
    {
        return currentIndex = begin++;
    }

    public int next(int step)
    {
        currentIndex = begin;
        begin+=step;
        return currentIndex;
    }

    public int current()
    {
        return currentIndex;
    }

    public boolean inLoop()
    {
        return begin < end;
    }

    public void reset(int a, int b)
    {
        this.end = a;
        this.begin = b;
        this.currentIndex = b;
    }
}