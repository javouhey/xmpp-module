package com.raverun.im.interfaces.rest;

import javax.servlet.http.HttpServletRequest;

public interface SessionUtilsFactory
{
    SessionUtils create( HttpServletRequest request ) throws IllegalArgumentException;
}
