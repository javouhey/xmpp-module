package com.raverun.im.interfaces.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raverun.im.interfaces.rest.support.Either;
import com.raverun.im.interfaces.rest.support.HttpVerb;

/**
 * responsibilities
 * <ul>
 * <li>Needs to be aware of its version
 * </ul>
 *
 * @author Gavin Bong
 */
public interface ProtocolProcessor
{
    /**
     * validates & parses the Http request
     * 
     * @return Either - 
     */
    Either receive( HttpVerb verb, HttpServletRequest request );

    /**
     * Writes out the response {@code resp} using {@code httpResponse}
     */
    void reply( RestResponse resp, HttpServletResponse httpResponse ) throws IOException;
}
