package com.trxs.commons.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class NotFoundException extends HttpException
{
    public NotFoundException( String message )
    {
        super(HttpStatus.NOT_FOUND.value());
        setMessage(message);
    }

    public NotFoundException(String message , List<String> errors )
    {
        super(HttpStatus.NOT_FOUND.value());
        if ( errors != null && errors.size() > 0 ) addErrors(errors);
    }
}
