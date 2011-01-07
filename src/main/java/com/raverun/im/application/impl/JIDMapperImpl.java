package com.raverun.im.application.impl;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.application.JIDMapper;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.infrastructure.persistence.SettingsService;
import com.raverun.im.infrastructure.persistence.TransportMapper;

public class JIDMapperImpl implements JIDMapper
{
    @Inject
    public JIDMapperImpl( SettingsService settingService,
        TransportMapper transportMapper )
    {
        _settingService = settingService;
        _transportMapper = transportMapper;
    }

    @Override
    public IMIdentity getCanonicalReceiver( final Transport transport, final String user, final String userXmpp )
    {
        if( transport == Transport.MIM )
        {
            return new IMIdentity() {
                public String imId() { return user; }
                public String imIdRaw() { return user; }
                public Transport transport() { return transport; }
                public Integer transportDbSequence() { return _transportMapper.sequenceFor( transport ); }
            };
        }

        IMUserSetting aReceiverSetting = _settingService.getOneSettingFor( user, userXmpp, transport );
        if( aReceiverSetting == null )
            throw new AssertionError( "getCanonicalReceiver:Should never happen" );

        return aReceiverSetting.identity();
    }

    @Override
    public String getCanonicalSender( String decodedSender, Transport transport )
    {
        if( transport != Transport.MIM )
        {
            return decodedSender;
        }

        return _settingService.getUserFor( decodedSender );
    }

    private final SettingsService _settingService;
    private final TransportMapper _transportMapper;

    private static final Logger _logger = Logger.getLogger( JIDMapperImpl.class );
}
