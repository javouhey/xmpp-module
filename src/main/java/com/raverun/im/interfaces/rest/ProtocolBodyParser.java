package com.raverun.im.interfaces.rest;

import java.io.InputStream;

public interface ProtocolBodyParser
{
    /**
     * @throws EntityTooSmallException
     * @throws IllegalArgumentException
     */
    Object parse( InputStream is, int contentLength );
}
