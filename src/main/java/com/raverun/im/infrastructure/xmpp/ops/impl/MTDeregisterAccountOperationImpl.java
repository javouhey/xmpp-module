package com.raverun.im.infrastructure.xmpp.ops.impl;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.XMPPError;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.common.Transport;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.ops.DeregisterGatewayAccountOperation;
import com.raverun.im.infrastructure.xmpp.smack.GatewayDeregisterPacketExtension;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Constraint;

public class MTDeregisterAccountOperationImpl implements
    DeregisterGatewayAccountOperation
{

    @Override
    public DeregisterResult call() throws Exception
    {
        checkState();

        String targetJID = _xmppUtility.getTransportJID( _transport );

        Registration dereg = new Registration();
        dereg.setTo( targetJID );
        dereg.setType( IQ.Type.SET );
        dereg.addExtension( new GatewayDeregisterPacketExtension() );

        final PacketFilter idFilter = new PacketIDFilter( dereg.getPacketID() );
        PacketFilter myFilter = new PacketFilter() {
            public boolean accept(Packet packet) {
                final String from = packet.getFrom();
                return( from != null && from.startsWith( _transport.code() )
                    && idFilter.accept( packet ) );
            }
        };

        try
        {
            PacketCollector collector = _xmppConn.createPacketCollector( myFilter );
            _xmppConn.sendPacket( dereg );
            _logger.debug( "Sending deregister to " + targetJID );
            _logger.debug( _userXmpp + "\tC->S: " + dereg.toXML() );

            Packet result = null;

            _logger.debug( "wait for max of " + WAIT_MS + " ms" );
            try
            {
                result = collector.nextResult( WAIT_MS );
                if( result != null )
                {
                    _logger.debug( _userXmpp + " S->C: " + result.toXML() );
                    if( result instanceof IQ )
                    {
                        IQ.Type iqType = ((IQ)result).getType();
                        _logger.debug( "\tIQ.Type=" + iqType );

                        switch( translateFrom( iqType ) )
                        {
                        case ERROR:
                            XMPPError error = result.getError();
                            if( error == null )
                                throw new AssertionError( "How could an error IQ not contain an error element?" );

                            return DeregisterResultImpl.newFor( false, true, result, (error.getCode() == 407) );

                        case RESULT:
                        case OTHERS:
                        default:
                            return DeregisterResultImpl.newFor( true, true, result, false );
                        }
                    }
                    throw new AssertionError( "Expected an IQ packet" );
                }
                else
                    return DeregisterResultImpl.newFor( false, false, null, false );
            }
            catch( Exception e )
            {
                throw e;
            }
            finally
            {
                collector.cancel();
            }
        }
        catch( XMPPException xmppe )
        {
            throw new XMPPFault( "deregister failed for " + _transport.code() + " due to", xmppe, XMPPFault.XmppFaultCode.ON_DELETE_XMPP_USER );
        }
    }

    @AssistedInject
    public MTDeregisterAccountOperationImpl(
        XMPPUtility xmppUtility,
        @Assisted XMPPConnectionIF xmppConn, 
        @Assisted Transport transport,
        @Assisted String userXmpp )
    {
        _xmppUtility = xmppUtility;
        _transport = transport;
        _userXmpp = userXmpp;
        _xmppConn = xmppConn;
    }

    private final IQStatus translateFrom( IQ.Type iqType )
    {
        Constraint.NonNullArgument.check( iqType, "iqType" );

        if( IQ.Type.ERROR.equals( iqType ) )
            return IQStatus.ERROR;
        else if( IQ.Type.RESULT.equals( iqType ) )
            return IQStatus.RESULT;
        else
            return IQStatus.OTHERS;
    }

    private void checkState()
    {
        if( !_xmppConn.isConnected() )
            throw new IllegalStateException( "XMPP connection is disconnected");

        if( !_xmppConn.isAuthenticated() )
            throw new IllegalStateException( "XMPP connection is not authenticated");
    }

    /**
     * To bridge enumerations with existing Smack's org.jivesoftware.smack.packet.IQ.Type
     */
    enum IQStatus 
    {
        RESULT, ERROR, OTHERS;
    }

    public static class DeregisterResultImpl implements DeregisterGatewayAccountOperation.DeregisterResult
    {
        public static DeregisterResultImpl newFor( boolean ok, boolean receivedReply, Packet packet, boolean is407 )
        {
            return new DeregisterResultImpl( ok, receivedReply, packet, is407 );
        }

        private DeregisterResultImpl( boolean ok, boolean receivedReply, Packet packet, boolean is407 )
        {
            _ok = ok;
            _receivedReply = receivedReply;
            _packet = packet;
            _auth407 = is407;
        }

        public boolean isAuth407() { return _auth407; }
        public boolean didWeReceiveReply() { return _receivedReply; }
        public Packet getReceivedPacket() { return _packet; }
        public boolean isOk() { return _ok; }
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append( "ok: " ).append( _ok ).append( " | " );
            builder.append( "receivedReply: " ).append( _receivedReply ).append( " | " );
            builder.append( "is407? " ).append( _auth407 );
            return builder.toString();
        }

        Packet _packet;
        boolean _ok, _receivedReply, _auth407;
    }

// ---- constants ----
    private final static long WAIT_MS = 12000; 
 
// ---- immutables ---
    private final String _userXmpp;
    private final Transport _transport;
    private final XMPPUtility _xmppUtility;
    private final XMPPConnectionIF _xmppConn;

    private final Logger _logger = Logger.getLogger( MTDeregisterAccountOperationImpl.class );
}
