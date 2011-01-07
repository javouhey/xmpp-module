package com.raverun.im.interfaces.rest;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

import com.raverun.im.interfaces.rest.impl.resources.EitherResource;
import com.raverun.im.interfaces.rest.support.HttpVerb;

public abstract class AbstractSessionAwareResource implements SessionAwareResource
{
    public abstract EitherResource options();

    public abstract String path();

    public abstract boolean respondsTo( HttpVerb verb );

    public abstract SessionAttribute sessionAttributeFor( HttpVerb verb );

    public abstract EitherResource delete( String key,
        MultivaluedMap<String, String> queryParams, MediaType outType,
        Map<String, Object> sessionAttributes, SessionUtils session );

    public abstract EitherResource get( String key,
        MultivaluedMap<String, String> queryParams, MediaType outType,
        Map<String, Object> sessionAttributes, SessionUtils session );

    public abstract boolean invalidateSessionAfterDelete();

    public abstract EitherResource post( String key,
        MultivaluedMap<String, String> queryParams, MediaType inType,
        MediaType outType, Object entity,
        Map<String, Object> sessionAttributes, SessionUtils session );

    public abstract EitherResource put( String key,
        MultivaluedMap<String, String> queryParams, MediaType inType,
        MediaType outType, Object entity,
        Map<String, Object> sessionAttributes, SessionUtils session );

    @Override
    public EitherResource delete( String key, MediaType outType )
    {
        _logger.debug( "delete" );
        return new EitherResource.Builder( true, 200 ).build();
    }

    @Override
    public EitherResource get( String key, MediaType outType )
    {
        _logger.debug( "get" );
        return new EitherResource.Builder( true, 200 ).build();
    }

    public EitherResource head()
    {
        _logger.debug( "head" );
        return new EitherResource.Builder( true, 200 ).build();
    }

    @Override
    public EitherResource post( MediaType inType, MediaType outType,
        Object entity )
    {
        _logger.debug( "post" );
        return new EitherResource.Builder( true, 200 ).build();
    }

    @Override
    public EitherResource put( MediaType inType, MediaType outType,
        Object entity )
    {
        _logger.debug( "put" );
        return new EitherResource.Builder( true, 200 ).build();
    }

    private final Logger _logger = Logger.getLogger( AbstractSessionAwareResource.class );
}
