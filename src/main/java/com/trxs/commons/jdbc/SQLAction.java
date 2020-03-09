package com.trxs.commons.jdbc;

public class SQLAction
{
    private String sqlText;
    private Object[] parameters;

    public SQLAction(String sql, Object []args)
    {
        sqlText = sql;
        parameters = args;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
