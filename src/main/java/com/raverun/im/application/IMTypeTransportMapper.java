package com.raverun.im.application;

import com.raverun.im.common.IMConstants;
import com.raverun.im.common.Transport;

public interface IMTypeTransportMapper
{
    /**
     * @throws IllegalArgumentException if {@code imType} not within acceptable range
     * @see IMConstants.ClientLiteralsForTransport
     */
    Transport parse( int imType );

    /**
     * @throws IllegalArgumentException if {@code transport} is null
     */
    int parse( Transport transport );
}
