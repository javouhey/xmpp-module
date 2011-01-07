package com.raverun.im.infrastructure.xmpp.smack;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.raverun.shared.Configuration;

@Singleton
public class XMPPConnectionIFProvider implements Provider<XMPPConnectionIF>
{
    private final Configuration _config;

    private final ConnectionConfiguration _connConfig;

    @Override
    public XMPPConnectionIF get()
    {
        return XMPPConnectionAdapter.wrap( new XMPPConnection( _connConfig ) );
    }

    @Inject
    public XMPPConnectionIFProvider( Configuration config )
    {
        _config = config;

        _connConfig = new ConnectionConfiguration(
            _config.s( XMPPConnectionIF.KEY_XMPPSERVER_IP, "127.0.0.1" ),
            _config.i( XMPPConnectionIF.KEY_XMPPSERVER_PORT, 5222 ),
            _config.s( XMPPConnectionIF.KEY_XMPPSERVER_SERVICENAME, "gavin-pc" )
        );
    }

    public static class XMPPConnectionAdapter implements XMPPConnectionIF
    {
        private XMPPConnection _delegatee;

        public static XMPPConnectionAdapter wrap( XMPPConnection delegatee )
        {
            return new XMPPConnectionAdapter( delegatee );
        }

        public XMPPConnectionAdapter( XMPPConnection delegatee )
        {
            _delegatee = delegatee;
        }

        @Override
        public void addConnectionListener( ConnectionListener connectionListener )
        {
            _delegatee.addConnectionListener( connectionListener );
        }

        @Override
        public void addPacketListener( PacketListener packetListener,
            PacketFilter packetFilter )
        {
            _delegatee.addPacketListener( packetListener, packetFilter );
        }

        @Override
        public void addPacketWriterInterceptor(
            PacketInterceptor packetInterceptor, PacketFilter packetFilter )
        {
            _delegatee.addPacketWriterInterceptor( packetInterceptor, packetFilter );
        }

        @Override
        public void addPacketWriterListener( PacketListener packetListener,
            PacketFilter packetFilter )
        {
            _delegatee.addPacketWriterListener( packetListener, packetFilter );
        }

        @Override
        public void connect() throws XMPPException
        {
            _delegatee.connect();
        }

        @Override
        public PacketCollector createPacketCollector( PacketFilter packetFilter )
        {
            return _delegatee.createPacketCollector( packetFilter );
        }

        @Override
        public void disconnect()
        {
            _delegatee.disconnect();
        }

        @Override
        public void disconnect( Presence unavailablePresence )
        {
            _delegatee.disconnect( unavailablePresence );
        }

        @Override
        public AccountManager getAccountManager()
        {
            return _delegatee.getAccountManager();
        }

        @Override
        public ChatManager getChatManager()
        {
            return _delegatee.getChatManager();
        }

        @Override
        public String getConnectionID()
        {
            return _delegatee.getConnectionID();
        }

        @Override
        public String getHost()
        {
            return _delegatee.getHost();
        }

        @Override
        public int getPort()
        {
            return _delegatee.getPort();
        }

        @Override
        public Roster getRoster()
        {
            return _delegatee.getRoster();
        }

        @Override
        public SASLAuthentication getSASLAuthentication()
        {
            return _delegatee.getSASLAuthentication();
        }

        @Override
        public String getServiceName()
        {
            return _delegatee.getServiceName();
        }

        @Override
        public String getUser()
        {
            return _delegatee.getUser();
        }

        @Override
        public boolean isAnonymous()
        {
            return _delegatee.isAnonymous();
        }

        @Override
        public boolean isAuthenticated()
        {
            return _delegatee.isAuthenticated();
        }

        @Override
        public boolean isConnected()
        {
            return _delegatee.isConnected();
        }

        @Override
        public boolean isSecureConnection()
        {
            return _delegatee.isSecureConnection();
        }

        @Override
        public boolean isUsingCompression()
        {
            return _delegatee.isUsingCompression();
        }

        @Override
        public boolean isUsingTLS()
        {
            return _delegatee.isUsingTLS();
        }

        @Override
        public void login( String username, String password )
            throws XMPPException
        {
            _delegatee.login( username, password );
        }

        @Override
        public void login( String username, String password, String resource )
            throws XMPPException
        {
            _delegatee.login( username, password, resource );
        }

        @Override
        public void loginAnonymously() throws XMPPException
        {
            _delegatee.loginAnonymously();
        }

        @Override
        public void removeConnectionListener(
            ConnectionListener connectionListener )
        {
            _delegatee.removeConnectionListener( connectionListener );
        }

        @Override
        public void removePacketListener( PacketListener packetListener )
        {
            _delegatee.removePacketListener( packetListener );
        }

        @Override
        public void removePacketWriterInterceptor(
            PacketInterceptor packetInterceptor )
        {
            _delegatee.removePacketWriterInterceptor( packetInterceptor );
        }

        @Override
        public void removePacketWriterListener( PacketListener packetListener )
        {
            _delegatee.removePacketWriterListener( packetListener );
        }

        @Override
        public void sendPacket( Packet packet )
        {
            _delegatee.sendPacket( packet );
        }

        @Override
        public void cleanup()
        {
            _delegatee = null;
        }
        
        @Override
        public XMPPConnection downCast()
        {
            return _delegatee;
        }
    }//XMPPConnectionAdapter
}//XMPPConnectionIFProvider
