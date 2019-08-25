package com.trxs.pulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PulseIgnition implements ApplicationRunner
{
    private static Logger logger = LoggerFactory.getLogger(PulseIgnition.class.getSimpleName());

    @Autowired
    private TimerServer timerServer;

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        logger.debug("startup ...");
        timerServer.ticktockRunnable();
        timerServer.jobWatchRunnable();
    }
}
