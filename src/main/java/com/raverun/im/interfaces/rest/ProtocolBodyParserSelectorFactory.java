package com.raverun.im.interfaces.rest;

import javax.ws.rs.core.MediaType;

public interface ProtocolBodyParserSelectorFactory
{
    ProtocolBodyParserSelector create( MediaType mimeType ) throws IllegalArgumentException;
}
