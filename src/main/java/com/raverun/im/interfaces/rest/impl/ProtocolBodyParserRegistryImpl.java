package com.raverun.im.interfaces.rest.impl;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.ProtocolBodyParser;
import com.raverun.im.interfaces.rest.ProtocolBodyParserRegistry;
import com.raverun.im.interfaces.rest.support.guice.JSON;
import com.raverun.im.interfaces.rest.support.guice.XML;

public class ProtocolBodyParserRegistryImpl implements
    ProtocolBodyParserRegistry
{

    @Override
    public ProtocolBodyParser get( MediaType type )
    {
        if( type == null )
            return null;

        if( !_map.containsKey( type ) )
            return null;
        
        return _map.get( type );
    }

    @Inject
    public ProtocolBodyParserRegistryImpl( @JSON ProtocolBodyParser jsonParser,
        @XML ProtocolBodyParser xmlParser )
    {
        _map.put( MediaType.APPLICATION_JSON_TYPE, jsonParser );
        _map.put( MediaType.TEXT_XML_TYPE, xmlParser );
    }

    Map<MediaType, ProtocolBodyParser> _map = new HashMap<MediaType, ProtocolBodyParser>(2);
}
