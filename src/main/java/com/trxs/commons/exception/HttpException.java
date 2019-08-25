package com.trxs.commons.exception;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

public class HttpException extends RuntimeException
{
    private int status;
    private int errorCode;
    private String message;
    private List<String> errors;

    public HttpException( int httpCode )
    {
        this.status = httpCode;
        this.errorCode = 0;
        this.errors = new ArrayList<>(2);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int statusCode) {
        this.status = statusCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addErrors(List<String> errorList)
    {
        if ( errorList != null && errorList.size() > 0 ) this.errors.addAll(errorList);
    }

    public String toJsonString()
    {
        return JSON.toJSONString(this);
    }
}
