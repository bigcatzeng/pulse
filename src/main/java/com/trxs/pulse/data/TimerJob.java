package com.trxs.pulse.data;

import java.util.Date;

public class TimerJob implements Comparable<TimerJob>
{
    private int id;
    private int status;
    private int result;

    private Date expectTime;
    private Date beginTime;
    private Date endTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Date getExpectTime() {
        return expectTime;
    }

    public void setExpectTime(Date expectTime) {
        this.expectTime = expectTime;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public int compareTo(TimerJob o)
    {
        if ( o == null ) return -1;

        if ( this.expectTime.getTime() > o.expectTime.getTime()) return  1;
        if ( this.expectTime.getTime() < o.expectTime.getTime()) return -1;

        return 0;
    }

    public boolean equals( Object o )
    {
        if ( o == null ) return false;
        if ( this == o ) return true;
        if ( o instanceof TimerJob) return this.getExpectTime().getTime() == ((TimerJob) o).getExpectTime().getTime();
        return false;
    }
}
