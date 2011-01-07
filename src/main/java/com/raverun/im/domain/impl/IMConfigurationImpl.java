package com.raverun.im.domain.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMConfiguration;
import com.raverun.im.domain.IMIdentity;
import com.raverun.shared.Common;

public class IMConfigurationImpl implements IMConfiguration
{

    @Override
    public List<IMIdentity> supportedTransports()
    {
        return _transports;
    }

    @Inject
    public IMConfigurationImpl()
    {
        _transports = new ArrayList<IMIdentity>( twoTimes( 3 ) );

        _transports.add( new IMIdentity() 
        {
            public String imId() { return Common.EMPTY_STRING; }
            public String imIdRaw() { return Common.EMPTY_STRING; }
            public Transport transport() { return Transport.GTALK; }
            public Integer transportDbSequence() { return 4; }
         } );

        _transports.add( new IMIdentity() 
        {
            public String imId() { return Common.EMPTY_STRING; }
            public String imIdRaw() { return Common.EMPTY_STRING; }
            public Transport transport() { return Transport.MSN; }
            public Integer transportDbSequence() { return 2; }
         } );

        _transports.add( new IMIdentity() 
        {
            public String imId() { return Common.EMPTY_STRING; }
            public String imIdRaw() { return Common.EMPTY_STRING; }
            public Transport transport() { return Transport.YAHOO; }
            public Integer transportDbSequence() { return 1; }
         } );

        _transports.add( new IMIdentity() 
        {
            public String imId() { return Common.EMPTY_STRING; }
            public String imIdRaw() { return Common.EMPTY_STRING; }
            public Transport transport() { return Transport.MIM; }
            public Integer transportDbSequence() { return 3; }
         } );
    }

    private int twoTimes( int x ) { return 2*x; }

    private final List<IMIdentity> _transports;
}
