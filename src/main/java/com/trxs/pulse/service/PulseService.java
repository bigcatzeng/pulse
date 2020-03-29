package com.trxs.pulse.service;

import com.trxs.pulse.jdbc.BaseService;
import com.trxs.pulse.jdbc.Record;
import com.trxs.pulse.jdbc.SqlFormatterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class PulseService extends BaseService
{
    private final static Logger logger = LoggerFactory.getLogger(PulseService.class.getName());

    public void test()
    {
        int rows = update("addDomain", "www.sohu.com");
        logger.debug("rows = {}", rows);

        final String tableName = "p_cron_tasks";
        Record domainsRecord = Record.newInstance(tableName);
        domainsRecord.setField("id", Integer.valueOf(-1));
        domainsRecord.setField("subsystem", Integer.valueOf(33));
        save(domainsRecord);

        logger.debug("id = {}, insertSQL:{}", domainsRecord.getField("id"), domainsRecord.insertAction().getSqlText() );

        rows = delRecordById("pDomains", 50);
        logger.debug("rows={}", rows);

        rows = delRecordById("p_domains", 49);
        logger.debug("rows={}", rows);

        String sql = sqlProvider.getSqlByKey("getQuestionnaireTemplateById");

        SqlFormatterUtils sqlFormatterUtils = new SqlFormatterUtils();

        logger.debug("sql -> {}", sqlFormatterUtils.format(sql));
        return;
    }
}
