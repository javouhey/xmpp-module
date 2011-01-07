package com.raverun.im.interfaces.rest.impl;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.ProtocolBodyParser;

public class ProtocolBodyParserXML implements ProtocolBodyParser
{

    @Override
    public Object parse( InputStream is, int contentLength )
    {
        throw new UnsupportedOperationException();
    }

    private final MediaType MIME_TYPE = MediaType.TEXT_XML_TYPE;
}
