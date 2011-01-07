package com.raverun.im.infrastructure.xmpp.smack;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ChatManager;
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

public class NullXMPPConnection implements XMPPConnectionIF
{

    @Override
    public void addConnectionListener( ConnectionListener connectionListener )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addPacketListener( PacketListener packetListener,
        PacketFilter packetFilter )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addPacketWriterInterceptor(
        PacketInterceptor packetInterceptor, PacketFilter packetFilter )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addPacketWriterListener( PacketListener packetListener,
        PacketFilter packetFilter )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void connect() throws XMPPException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public PacketCollector createPacketCollector( PacketFilter packetFilter )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnect()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void disconnect( Presence unavailablePresence )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public AccountManager getAccountManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChatManager getChatManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getConnectionID()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHost()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPort()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Roster getRoster()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SASLAuthentication getSASLAuthentication()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getServiceName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUser()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAnonymous()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAuthenticated()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isConnected()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSecureConnection()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUsingCompression()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUsingTLS()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void login( String username, String password ) throws XMPPException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void login( String username, String password, String resource )
        throws XMPPException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void loginAnonymously() throws XMPPException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeConnectionListener( ConnectionListener connectionListener )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removePacketListener( PacketListener packetListener )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removePacketWriterInterceptor(
        PacketInterceptor packetInterceptor )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removePacketWriterListener( PacketListener packetListener )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendPacket( Packet packet )
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cleanup()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public XMPPConnection downCast()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
