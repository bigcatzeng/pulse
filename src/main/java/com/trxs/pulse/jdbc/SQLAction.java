package com.trxs.pulse.jdbc;

public class SQLAction
{
    private SQLEnum action;
    private String sqlText;
    private Object[] parameters;

    public SQLAction(SQLEnum sqlEnum, String sql, Object []args)
    {
        action = sqlEnum;
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

    public SQLEnum getAction() {
        return action;
    }

    public void setAction(SQLEnum action)
    {
        this.action = action;
    }
}
