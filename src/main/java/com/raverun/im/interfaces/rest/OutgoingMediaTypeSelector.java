package com.raverun.im.interfaces.rest;

import java.util.List;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.core.header.AcceptableMediaType;

public interface OutgoingMediaTypeSelector
{
    MediaType deduceFrom( List<AcceptableMediaType> requestMediaTypes );
}
