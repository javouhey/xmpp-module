package com.raverun.im.application.impl;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.common.IMConstants;
import com.raverun.im.common.Transport;
import com.raverun.shared.Constraint;

public class IMTypeTransportMapperImpl implements IMTypeTransportMapper
{

    @Override
    public Transport parse( int imType )
    {
        switch( imType )
        {
        case IMConstants.ClientLiteralsForTransport.MIM:
            return Transport.MIM;

        case IMConstants.ClientLiteralsForTransport.MSN:
            return Transport.MSN;

        case IMConstants.ClientLiteralsForTransport.YAHOO:
            return Transport.YAHOO;

        case IMConstants.ClientLiteralsForTransport.GTALK:
            return Transport.GTALK;

        case IMConstants.ClientLiteralsForTransport.QQ:
            return Transport.QQ;

        default:
            throw new IllegalArgumentException( "Only supports the range [0,4]" );
        }
    }

    @Override
    public int parse( Transport transport )
    {
        Constraint.NonNullArgument.check( transport, "transport" );

        switch( transport )
        {
        case FACEBOOK:
        case MYSPACEIM:
        default:
            throw new UnsupportedOperationException( "facebook, myspace" ); 

        case GTALK:
            return IMConstants.ClientLiteralsForTransport.GTALK;

        case MIM:
            return IMConstants.ClientLiteralsForTransport.MIM;

        case MSN:
            return IMConstants.ClientLiteralsForTransport.MSN;

        case QQ:
            return IMConstants.ClientLiteralsForTransport.QQ;

        case YAHOO:
            return IMConstants.ClientLiteralsForTransport.YAHOO;
        }
    }

}
