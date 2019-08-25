package com.trxs.commons.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFoundException(NotFoundException e, HttpServletResponse response)
    {
        response.setStatus( e.getStatus() );
        return e.toJsonString();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ParameterErrorException.class)
    public Object handleNotFoundException(ParameterErrorException e, HttpServletResponse response)
    {
        response.setStatus( e.getStatus() );
        return e.toJsonString();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalServerErrorException.class)
    public Object handleNotFoundException(InternalServerErrorException e, HttpServletResponse response)
    {
        response.setStatus( e.getStatus() );
        return e.toJsonString();
    }
}
