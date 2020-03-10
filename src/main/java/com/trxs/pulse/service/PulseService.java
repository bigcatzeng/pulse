package com.trxs.pulse.service;

import com.trxs.commons.jdbc.BaseService;
import com.trxs.commons.jdbc.Record;
import org.springframework.stereotype.Service;


@Service
public class PulseService  extends BaseService
{
    public PulseService()
    {
        super("/sql/pulse.sql");
    }

    public void test()
    {
        int rows = update("addDomain", "www.sohu.com");
        logger.debug("rows = {}", rows);

        final String tableName = "p_cron_tasks";
        Record domainsRecord = Record.newInstance(tableName);
        domainsRecord.setField("id", Integer.valueOf(-1));
        domainsRecord.setField("subsystem", Integer.valueOf(33));

        logger.debug("id = {}, insertSQL:{}", domainsRecord.getField("id"), domainsRecord.insertAction().getSqlText() );

        rows = delRecordById("pDomains", 50);
        logger.debug("rows={}", rows);

        rows = delRecordById("p_domains", 49);
        logger.debug("rows={}", rows);

        return;
    }
}
