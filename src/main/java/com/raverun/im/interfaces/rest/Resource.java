package com.raverun.im.interfaces.rest;

import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.impl.resources.EitherResource;
import com.raverun.im.interfaces.rest.support.HttpVerb;

public interface Resource
{
    /**
     * @return true if it accepts requests made through the verb {@code HttpVerb}
     */
    boolean respondsTo( HttpVerb verb );

    EitherResource put( MediaType inType, MediaType outType, Object entity );

    EitherResource delete( String key, MediaType outType );

    EitherResource post( MediaType inType, MediaType outType, Object entity );

    EitherResource get( String key, MediaType outType );

    EitherResource head();

    EitherResource options();

    /**
     * @return base path segment that is handled by this Resource.
     */
    String path();

    SessionAttribute sessionAttributeFor( HttpVerb verb );

    public final static EitherResource MALFORMED_JSON_ERROR = new EitherResource.Builder( false, 400 ).error( "The JSON you provided was invalid.", ProtocolErrorCode.MalformedJSON ).build();

    public final static String USER_EXISTS_ERROR_MESSAGE = "The supplied userid already exists. Please select a different id and try again";

    public final static String IM_ACCOUNT_EXISTS_ERROR_MESSAGE = "The supplied im account already exists. Please select a different id and try again";

    public final static String IM_TRANSPORT_UNAVAILABLE_ERROR_MESSAGE = "The specific transport that you specified is currently unavailable";
}
