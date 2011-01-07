package com.raverun.im.interfaces.rest;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.raverun.im.interfaces.rest.impl.resources.EitherResource;

public interface SessionAware
{
    EitherResource put( String key, MultivaluedMap<String, String> queryParams, MediaType inType, MediaType outType, Object entity, Map<String,Object> sessionAttributes, SessionUtils session );

    EitherResource delete( String key, MultivaluedMap<String, String> queryParams, MediaType outType, Map<String,Object> sessionAttributes, SessionUtils session );

    EitherResource post( String key, MultivaluedMap<String, String> queryParams, MediaType inType, MediaType outType, Object entity, Map<String,Object> sessionAttributes, SessionUtils session );

    EitherResource get( String key, MultivaluedMap<String, String> queryParams, MediaType outType, Map<String,Object> sessionAttributes, SessionUtils session );

    boolean invalidateSessionAfterDelete();
}
