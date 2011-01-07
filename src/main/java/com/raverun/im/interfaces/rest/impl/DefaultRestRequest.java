package com.raverun.im.interfaces.rest.impl;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.RestRequest;
import com.raverun.im.interfaces.rest.ProtocolUtils.InvokedPath;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.HttpVerb;

public class DefaultRestRequest implements RestRequest
{

    @Override
    public Object entity()
    {
        return _entity;
    }

    @Override
    public MediaType incoming()
    {
        return _incomingType;
    }

    @Override
    public InvokedPath invokedPath()
    {
        return _invokedPath;
    }

    @Override
    public MediaType outgoing()
    {
        return _outgoingType;
    }

    @Override
    public HttpVerb verb()
    {
        return _verb;
    }

    @Override
    public Version version()
    {
        return _version;
    }

    @Override
    public Map<String, Object> sessionAttributes()
    {
        return _sessionAttributes;
    }

    public DefaultRestRequest( InvokedPath invokedPath, Version version,
        MediaType incoming, MediaType outgoing, HttpVerb verb, Object entity )
    {
        _verb              = verb;
        _version           = version;
        _invokedPath       = invokedPath;
        _incomingType      = incoming;
        _outgoingType      = outgoing;
        _entity            = entity;
        _sessionAttributes = new HashMap<String, Object>(2);
    }

    private final Map<String, Object> _sessionAttributes;
    private final InvokedPath _invokedPath;
    private final MediaType _incomingType;
    private final MediaType _outgoingType;
    private final HttpVerb _verb;
    private final Version _version;
    private final Object _entity;
}
