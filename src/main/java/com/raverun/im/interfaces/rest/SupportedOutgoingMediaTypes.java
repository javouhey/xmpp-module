package com.raverun.im.interfaces.rest;

import com.sun.jersey.core.header.AcceptableMediaType;

public interface SupportedOutgoingMediaTypes
{
    boolean isSatisfiedBy( AcceptableMediaType mediaType );
}
