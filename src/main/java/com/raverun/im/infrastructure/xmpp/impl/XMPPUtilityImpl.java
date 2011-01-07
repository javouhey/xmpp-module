package com.raverun.im.infrastructure.xmpp.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.util.StringUtils;

import com.google.inject.Inject;
import com.raverun.im.common.Transport;
import com.raverun.im.common.IMConstants.Symbols;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.shared.Common;
import com.raverun.shared.Configuration;
import com.raverun.shared.Constraint;

public class XMPPUtilityImpl implements XMPPUtility
{
    @Inject
    public XMPPUtilityImpl( Configuration config )
    {
        _config = config; 
        _jidDomain = _config.s( XMPPConnectionIF.KEY_XMPPSERVER_SERVICENAME, "localhost" );
    }

    @Override
    public String getTransportJID( Transport transport )
    {
        Constraint.NonNullArgument.check( transport, "transport" );

        switch( transport )
        {
        case GTALK:
        case MSN:
        case QQ:
        case YAHOO:
        case FACEBOOK:
        case MYSPACEIM:
            return (transport.code() + Symbols.FULL_STOP + _jidDomain).intern();
        case MIM:
        default:
            return _jidDomain;
        }
    }

    @Override
    public String newJIDfor( String contactId, Transport transport )
    {
        if( Constraint.EmptyString.isFulfilledBy( contactId ))
            throw new IllegalArgumentException( "contactId is empty" );

        Constraint.NonNullArgument.check( transport, "transport" );

        return StringUtils.escapeNode( contactId ) + Symbols.ALIAS + getTransportJID( transport );
    }

    @Override
    public Transport decodeTransportFor( String fullJid )
    {
        if( Constraint.EmptyString.isFulfilledBy( fullJid ) )
            throw new IllegalArgumentException( "fullJid is empty" );

        String server = StringUtils.parseServer( fullJid ).trim();

        if( server.equals( Common.EMPTY_STRING) )
            throw new IllegalArgumentException( "fullJid is invalid. Empty" );

        _logger.debug( "[decodeTransportFor] server:" + server + " vs jidDomain:" + _jidDomain );
        if( _jidDomain.equalsIgnoreCase( server ) )
            return Transport.MIM;

        if( server.indexOf( Symbols.FULL_STOP ) == -1 || server.indexOf( Symbols.FULL_STOP ) == 0 )
            throw new IllegalArgumentException( "fullJid is invalid. MUST be of the form transport.hostname" );

        return Transport.deref( server.substring( 0, server.indexOf( Symbols.FULL_STOP ) ) );
    }

    /**
     * @throws IllegalArgumentException if {@code fullJid} is null
     * @return the name portion of the fullJid. Otherwise if {@code fullJid} represents a transport, {@link #TRANSPORT_ONLY} will be returned 
     */
    @Override
    public String decodeSender( String fullJid )
    {
        if( Constraint.EmptyString.isFulfilledBy( fullJid ) )
            throw new IllegalArgumentException( "fullJid is empty" );

        String name = StringUtils.parseName( fullJid ).trim();
        if( name.equals( Common.EMPTY_STRING) )
        {
//            throw new IllegalArgumentException( "fullJid is invalid. Empty" );
            return TRANSPORT_ONLY;
        }

        return StringUtils.unescapeNode( name );
    }

    private final Configuration _config;
    private final String        _jidDomain;

    private static final Logger _logger = Logger.getLogger( XMPPUtilityImpl.class );

}
