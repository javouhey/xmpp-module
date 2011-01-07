package com.raverun.im.interfaces.rest;

import javax.servlet.http.HttpServletRequest;

public interface ResourceDispatcher
{
    RestResponse dispatch( RestRequest request, HttpServletRequest httpRequest );
}
