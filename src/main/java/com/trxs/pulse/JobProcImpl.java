package com.trxs.pulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.trxs.commons.util.LangUtils.SLEEP;

public abstract class JobProcImpl
{
    private static Logger logger = LoggerFactory.getLogger(JobProcImpl.class.getSimpleName());

    private AtomicBoolean running;
    private Thread  selfThread;

    protected String jobName;

    public abstract void process();
    public abstract void onException(Exception e);

    public JobProcImpl()
    {
        init();
    }

    public JobProcImpl(String name)
    {
        jobName = name;
        init();
    }

    private void init()
    {
        selfThread = null;
        running = new AtomicBoolean(Boolean.valueOf(false));
    }

    public boolean isRunning()
    {
        return running.get();
    }

    public Thread.State getState()
    {
        if ( selfThread == null ) return Thread.State.TERMINATED;
        return selfThread.getState();
    }

    public void shutdown()
    {
        while ( running.get() && false == running.compareAndSet(true, false)) SLEEP(100);
    }


    public String getJobName()
    {
        return String.join( "@", jobName, String.valueOf(hashCode()) ) ;
    }

    @Async("timerTaskExecutor")
    public void run()
    {
        boolean result = running.compareAndSet(false,true);
        if ( result == false )
        {
            logger.warn("Startup {} failed!", this.toString());
            return;
        }

        selfThread = Thread.currentThread();

        try
        {
            process();
        }
        catch (Exception e)
        {
            onException(e);
        }
        finally
        {
            running.set(false);
        }
    }

    public void interrupt()
    {
        if ( selfThread != null ) selfThread.interrupt();
    }
}
