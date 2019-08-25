package com.trxs.pulse;

import com.trxs.pulse.data.TimerMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.trxs.commons.util.LangUtils.SLEEP;

/**
 * @author zengshengwen 2019 19-6-15 下午4:35
 */
@Scope("singleton")
@Component
public class TimerServer
{
    private static Logger logger = LoggerFactory.getLogger(TimerServer.class.getSimpleName());

    private AtomicBoolean ticktockRunning;
    private AtomicBoolean jobWatchRunning;

    private Thread  ticktockThread;
    private Thread  jobWatchThread;


    // 因为时间值已经占用0~59编码覆盖了部分字母编码, 所以重新给符号编码
    public static final byte        LAST = 0X4C; // L
    public static final byte    ASTERISK = 0X7B; // *
    public static final byte MINUS_SIGNS = 0X7C; // -
    public static final byte       COMMA = 0X7D; // ,
    public static final byte       SLANT = 0X7E; // /

    private static Queue<Long> ticktockQueue = new ConcurrentLinkedDeque<>();
    private static Map<Integer, TimerMessage> timerMessageMap = new ConcurrentHashMap<>(1024);
    private static PriorityQueue<TimerMessage> timerMessageQueue;

    private static AtomicBoolean timerMessageQueueAtomic;
    private static long maxTimestamp;

    @PostConstruct()
    public void init()
    {
        maxTimestamp = 0;

        ticktockThread = null;
        jobWatchThread = null;

        ticktockRunning = new AtomicBoolean(Boolean.valueOf(false));
        jobWatchRunning = new AtomicBoolean(Boolean.valueOf(false));

        timerMessageQueue = new PriorityQueue<>(1024);
        timerMessageQueueAtomic = new AtomicBoolean(false);
    }

    private boolean delMessages(TimerMessage message)
    {
        if ( timerMessageQueue.contains(message))
            return timerMessageQueue.remove(message);
        else
            return true;
    }

    private int addMessages(List<TimerMessage> messageList)
    {
        messageList.forEach( timerMessage ->
        {
            if ( timerMessageQueue.contains(timerMessage) ) return;
            timerMessageQueue.add(timerMessage);
            long t = timerMessageQueue.peek().getExpectTime().getTime();
            if ( t > maxTimestamp ) maxTimestamp = t;
        });

        return timerMessageQueue.size();
    }

    private boolean timeIsUp( long timestamp )
    {
        if ( timerMessageQueue.size() < 1 ) return false;

        return timerMessageQueue.peek().getExpectTime().getTime() <= timestamp;
    }

    public Long getTimestamp()
    {
        return ticktockQueue.poll();
    }

    @Async("timerTaskExecutor")
    public void jobLoad()
    {
        timerMessageMap.keySet();
    }

    @Async("timerTaskExecutor")
    public void jobWatchRunnable()
    {
        boolean result = jobWatchRunning.compareAndSet(false,true);
        if ( result == false )
        {
            logger.warn("JobWatchRunnable Startup {} failed!", this.toString());
            return;
        }

        jobWatchThread = Thread.currentThread();

        try
        {
            jobWatchProcLoop();
        }
        catch (Exception e)
        {
            logger.warn("TicktockRunnable <- {}", e.getMessage());
        }
        finally
        {
            jobWatchRunning.set(false);
        }
    }

    public void jobWatchProcLoop()
    {
        Long currentTime;

        logger.info( "doJobWatchProc, go go go ...");

        do
        {
            currentTime = getTimestamp();
            if (currentTime == null)
            {
                SLEEP(5000);
                continue;
            }

            scanTimerMessageQueue(currentTime.longValue());

        } while (jobWatchRunning.get());

        logger.info("doJobWatchProc exit.");

    }

    private void scanTimerMessageQueue( long timestamp )
    {
        DateTime dateTime;
        do
        {
            try
            {
                lock(timerMessageQueueAtomic);

                dateTime = new DateTime( timestamp );
                logger.debug("doTicktockProc -> {}.", dateTime.toString("yyyy-MM-dd  HH:mm:ss.SSS"));
            }
            finally
            {
                unLock(timerMessageQueueAtomic);
            }
        } while(timeIsUp(timestamp) );
    }

    @Async("timerTaskExecutor")
    public void ticktockRunnable()
    {
        boolean result = ticktockRunning.compareAndSet(false,true);
        if ( result == false )
        {
            logger.warn("TicktockRunnable startup {} failed!", this.toString());
            return;
        }

        ticktockThread = Thread.currentThread();

        try
        {
            ticktockProcLoop();
        }
        catch (Exception e)
        {
            logger.warn("TicktockRunnable <- {}", e.getMessage());
        }
        finally
        {
            ticktockRunning.set(false);
        }
    }

    public void ticktockProcLoop()
    {
        int  millis, dt;
        long currentTime;

        logger.info( "doTicktockProc, go go go ...");

        do
        {
            currentTime = System.currentTimeMillis();
            millis = Math.toIntExact(currentTime % 1000);
            dt = 1000 - millis;
            currentTime += dt;

            SLEEP(dt);
            addTimestamp(currentTime);
        } while (ticktockRunning.get());

        logger.info("doTicktockProc exit.");
    }

    public void addTimestamp(long t)
    {
        ticktockQueue.add(Long.valueOf(t));
        if ( jobWatchThread !=null && jobWatchThread.getState() == Thread.State.TIMED_WAITING )
        {
            jobWatchThread.interrupt();
        }
    }


    @Async("timerTaskExecutor")
    public void startup()
    {
        jobWatchRunnable();
        ticktockRunnable();
    }

    public void shutdown()
    {
        ticktockRunning.set(false);
        jobWatchRunning.set(false);
    }

    public void lock( AtomicBoolean atomicRef )
    {
        int  tryCount = 0;
        while ( !atomicRef.compareAndSet(false,true) )
        {
            if ( ++tryCount % 5 != 0 ) SLEEP(8); else Thread.yield();
        }
    }

    public void unLock( AtomicBoolean atomicRef  )
    {
        int  tryCount = 0;
        while ( atomicRef.get() && !atomicRef.compareAndSet(true,false) )
        {
            if ( ++tryCount % 5 != 0 ) SLEEP(8); else Thread.yield();
        }
    }
}
