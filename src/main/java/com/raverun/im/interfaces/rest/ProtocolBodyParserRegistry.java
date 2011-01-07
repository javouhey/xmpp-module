package com.raverun.im.interfaces.rest;

import javax.ws.rs.core.MediaType;

public interface ProtocolBodyParserRegistry
{
    /** 
     * @return null if no ProtocolBodyParser found with supplied {@code type}
     */
    ProtocolBodyParser get( MediaType type );
}
