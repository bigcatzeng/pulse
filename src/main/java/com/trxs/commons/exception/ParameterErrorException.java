package com.trxs.commons.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ParameterErrorException extends HttpException
{
    public ParameterErrorException(String message )
    {
        super(HttpStatus.NOT_FOUND.value());
        setMessage(message);
    }

    public ParameterErrorException(String message , List<String> errors )
    {
        super(HttpStatus.NOT_FOUND.value());
        if ( errors != null && errors.size() > 0 ) addErrors(errors);
    }
}
