package com.raverun.im.interfaces.rest;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.ProtocolUtils.InvokedPath;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.HttpVerb;

public interface RestRequest
{
    Object entity();

    HttpVerb verb();

    /**
     * @return a non-nullable mime type
     */
    MediaType outgoing();

    /**
     * @return nullable mime type. It will be null if the {@link #verb()} is one of GET, OPTIONS, HEAD, DELETE
     */
    MediaType incoming();

    InvokedPath invokedPath();

    Version version();

    /**
     * @return non nullable
     */
    Map<String, Object> sessionAttributes();
}
