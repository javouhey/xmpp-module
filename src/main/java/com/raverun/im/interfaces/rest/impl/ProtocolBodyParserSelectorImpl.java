package com.raverun.im.interfaces.rest.impl;

import javax.ws.rs.core.MediaType;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.interfaces.rest.ProtocolBodyParser;
import com.raverun.im.interfaces.rest.ProtocolBodyParserRegistry;
import com.raverun.im.interfaces.rest.ProtocolBodyParserSelector;

public class ProtocolBodyParserSelectorImpl implements
    ProtocolBodyParserSelector
{
    @AssistedInject
    public ProtocolBodyParserSelectorImpl( ProtocolBodyParserRegistry parserRegistry, 
        @Assisted MediaType mediaType )
    {
        _parserRegistry = parserRegistry;
        _mediaType      = mediaType;
    }

    @Override
    public ProtocolBodyParser choose()
    {
        if( !_mediaType.equals( MediaType.APPLICATION_JSON_TYPE )
         && !_mediaType.equals( MediaType.TEXT_XML_TYPE ) )
            throw new AssertionError( "not possible in ProtocolBodyParserSelectorImpl#choose for _mediaType " + _mediaType );

        return _parserRegistry.get( _mediaType );
    }

    ProtocolBodyParserRegistry _parserRegistry;
    private final MediaType    _mediaType;
}
