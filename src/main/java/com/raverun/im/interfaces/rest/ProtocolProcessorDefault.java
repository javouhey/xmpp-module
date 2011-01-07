package com.raverun.im.interfaces.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.Either;
import com.raverun.im.interfaces.rest.support.HttpVerb;


/**
 * Basic functionality for all implementations of @link {@link ProtocolProcessor} 
 *
 * @author Gavin Bong
 */
public interface ProtocolProcessorDefault
{
    Either receive( HttpVerb verb, HttpServletRequest request, Version version );

    void reply( RestResponse resp, HttpServletResponse httpResponse, Version version ) throws IOException;
}
