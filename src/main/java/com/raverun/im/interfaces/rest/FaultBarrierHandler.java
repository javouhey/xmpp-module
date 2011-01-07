package com.raverun.im.interfaces.rest;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

/**
 * This handler captures uncaught exceptions which are not recoverable
 * 
 * @author Gavin Bong
 */
public interface FaultBarrierHandler
{
    /**
     * postcondition: a error message is generated and channelled via {@code httpResponse}
     *
     * @throws IOException
     */
    void handle( @Nonnull HttpServletResponse httpResponse ) throws IOException;
}
