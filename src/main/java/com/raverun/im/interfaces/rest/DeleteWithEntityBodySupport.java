package com.raverun.im.interfaces.rest;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.raverun.im.interfaces.rest.impl.resources.EitherResource;

/**
 * A mixin to allow HTTP DELETEs to process entity body.
 * It is important to note that this is only usable by {@code Resource}s that are
 * session aware only.
 * 
 * @author Gavin Bong
 */
public interface DeleteWithEntityBodySupport
{
    EitherResource delete( String key, MultivaluedMap<String, String> queryParams, MediaType inType, MediaType outType, Object entity, Map<String,Object> sessionAttributes, SessionUtils session );
}
