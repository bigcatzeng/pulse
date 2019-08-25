package com.trxs.commons.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class InternalServerErrorException extends HttpException
{
    public InternalServerErrorException(String message )
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value());
        setMessage(message);
    }

    public InternalServerErrorException(String message , List<String> errors )
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value());
        if ( errors != null && errors.size() > 0 ) addErrors(errors);
    }
}
