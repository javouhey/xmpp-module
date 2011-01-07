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

public interface XMPPConnectionIF
{
    public final static String KEY_XMPPSERVER_IP = "xmppserver.ip";

    public final static String KEY_XMPPSERVER_PORT = "xmppserver.port";

    public final static String KEY_XMPPSERVER_SERVICENAME = "xmppserver.servicename";

    /**
     * Returns the connection ID for this connection, which is the value set by the server
     * when opening a XMPP stream. If the server does not set a connection ID, this value
     * will be null. This value will be <tt>null</tt> if not connected to the server.
     *
     * @return the ID of this connection returned from the XMPP server or <tt>null</tt> if
     *      not connected to the server.
     */
    public String getConnectionID();

    /**
     * Returns the name of the service provided by the XMPP server for this connection. After
     * authenticating with the server the returned value may be different.
     *
     * @return the name of the service provided by the XMPP server.
     */
    public String getServiceName();

    /**
     * Returns the host name of the server where the XMPP server is running. This would be the
     * IP address of the server or a name that may be resolved by a DNS server.
     *
     * @return the host name of the server where the XMPP server is running.
     */
    public String getHost();

    /**
     * Returns the port number of the XMPP server for this connection. The default port
     * for normal connections is 5222. The default port for SSL connections is 5223.
     *
     * @return the port number of the XMPP server.
     */
    public int getPort();

    /**
     * Returns the full XMPP address of the user that is logged in to the connection or
     * <tt>null</tt> if not logged in yet. An XMPP address is in the form
     * username@server/resource.
     *
     * @return the full XMPP address of the user logged in.
     */
    public String getUser();

    /**
     * Logs in to the server using the strongest authentication mode supported by
     * the server, then sets presence to available. If more than five seconds
     * (default timeout) elapses in each step of the authentication process without
     * a response from the server, or if an error occurs, a XMPPException will be thrown.<p>
     *
     * It is possible to log in without sending an initial available presence by using
     * {@link ConnectionConfiguration#setSendPresence(boolean)}. If this connection is
     * not interested in loading its roster upon login then use
     * {@link ConnectionConfiguration#setRosterLoadedAtLogin(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     *
     * @param username the username.
     * @param password the password or <tt>null</tt> if using a CallbackHandler.
     * @throws XMPPException if an error occurs.
     */
    public void login( String username, String password )
        throws XMPPException;

    /**
     * Logs in to the server using the strongest authentication mode supported by
     * the server. If the server supports SASL authentication then the user will be
     * authenticated using SASL if not Non-SASL authentication will be tried. If more than
     * five seconds (default timeout) elapses in each step of the authentication process
     * without a response from the server, or if an error occurs, a XMPPException will be
     * thrown.<p>
     * 
     * Before logging in (i.e. authenticate) to the server the connection must be connected.
     * For compatibility and easiness of use the connection will automatically connect to the
     * server if not already connected.<p>
     *
     * It is possible to log in without sending an initial available presence by using
     * {@link ConnectionConfiguration#setSendPresence(boolean)}. If this connection is
     * not interested in loading its roster upon login then use
     * {@link ConnectionConfiguration#setRosterLoadedAtLogin(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     *
     * @param username the username.
     * @param password the password or <tt>null</tt> if using a CallbackHandler.
     * @param resource the resource.
     * @throws XMPPException if an error occurs.
     * @throws IllegalStateException if not connected to the server, or already logged in
     *      to the serrver.
     */
    public void login( String username, String password,
        String resource ) throws XMPPException;

    /**
     * Logs in to the server anonymously. Very few servers are configured to support anonymous
     * authentication, so it's fairly likely logging in anonymously will fail. If anonymous login
     * does succeed, your XMPP address will likely be in the form "server/123ABC" (where "123ABC"
     * is a random value generated by the server).
     *
     * @throws XMPPException if an error occurs or anonymous logins are not supported by the server.
     * @throws IllegalStateException if not connected to the server, or already logged in
     *      to the serrver.
     */
    public void loginAnonymously() throws XMPPException;

    /**
     * Returns the roster for the user logged into the server. If the user has not yet
     * logged into the server (or if the user is logged in anonymously), this method will return
     * <tt>null</tt>.
     *
     * @return the user's roster, or <tt>null</tt> if the user has not logged in yet.
     */
    public Roster getRoster();

    /**
     * Returns an account manager instance for this connection.
     *
     * @return an account manager for this connection.
     */
    public AccountManager getAccountManager();

    /**
     * Returns a chat manager instance for this connection. The ChatManager manages all incoming and
     * outgoing chats on the current connection.
     *
     * @return a chat manager instance for this connection.
     */
    public ChatManager getChatManager();

    /**
     * Returns true if currently connected to the XMPP server.
     *
     * @return true if connected.
     */
    public boolean isConnected();

    /**
     * Returns true if the connection to the server has successfully negotiated TLS. Once TLS
     * has been negotiatied the connection has been secured. @see #isUsingTLS. 
     *
     * @return true if a secure connection to the server.
     */
    public boolean isSecureConnection();

    /**
     * Returns true if currently authenticated by successfully calling the login method.
     *
     * @return true if authenticated.
     */
    public boolean isAuthenticated();

    /**
     * Returns true if currently authenticated anonymously.
     *
     * @return true if authenticated anonymously.
     */
    public boolean isAnonymous();

    /**
     * Closes the connection by setting presence to unavailable then closing the stream to
     * the XMPP server. The XMPPConnection can still be used for connecting to the server
     * again.<p>
     * <p/>
     * This method cleans up all resources used by the connection. Therefore, the roster,
     * listeners and other stateful objects cannot be re-used by simply calling connect()
     * on this connection again. This is unlike the behavior during unexpected disconnects
     * (and subsequent connections). In that case, all state is preserved to allow for
     * more seamless error recovery.
     */
    public void disconnect();

    /**
     * Closes the connection. A custom unavailable presence is sent to the server, followed
     * by closing the stream. The XMPPConnection can still be used for connecting to the server
     * again. A custom unavilable presence is useful for communicating offline presence
     * information such as "On vacation". Typically, just the status text of the presence
     * packet is set with online information, but most XMPP servers will deliver the full
     * presence packet with whatever data is set.<p>
     * <p/>
     * This method cleans up all resources used by the connection. Therefore, the roster,
     * listeners and other stateful objects cannot be re-used by simply calling connect()
     * on this connection again. This is unlike the behavior during unexpected disconnects
     * (and subsequent connections). In that case, all state is preserved to allow for
     * more seamless error recovery.
     *
     * @param unavailablePresence the presence packet to send during shutdown.
     */
    public void disconnect( Presence unavailablePresence );

    /**
     * Sends the specified packet to the server.
     *
     * @param packet the packet to send.
     */
    public void sendPacket( Packet packet );

    /**
     * Registers a packet listener with this connection. A packet filter determines
     * which packets will be delivered to the listener. If the same packet listener
     * is added again with a different filter, only the new filter will be used.
     *
     * @param packetListener the packet listener to notify of new packets.
     * @param packetFilter   the packet filter to use.
     */
    public void addPacketListener( PacketListener packetListener,
        PacketFilter packetFilter );

    /**
     * Removes a packet listener from this connection.
     *
     * @param packetListener the packet listener to remove.
     */
    public void removePacketListener( PacketListener packetListener );

    /**
     * Registers a packet listener with this connection. The listener will be
     * notified of every packet that this connection sends. A packet filter determines
     * which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * packet listener should complete all operations quickly or use a different
     * thread for processing.
     *
     * @param packetListener the packet listener to notify of sent packets.
     * @param packetFilter   the packet filter to use.
     */
    public void addPacketWriterListener(
        PacketListener packetListener, PacketFilter packetFilter );

    /**
     * Removes a packet listener from this connection.
     *
     * @param packetListener the packet listener to remove.
     */
    public void removePacketWriterListener(
        PacketListener packetListener );

    /**
     * Registers a packet interceptor with this connection. The interceptor will be
     * invoked every time a packet is about to be sent by this connection. Interceptors
     * may modify the packet to be sent. A packet filter determines which packets
     * will be delivered to the interceptor.
     *
     * @param packetInterceptor the packet interceptor to notify of packets about to be sent.
     * @param packetFilter      the packet filter to use.
     */
    public void addPacketWriterInterceptor(
        PacketInterceptor packetInterceptor, PacketFilter packetFilter );

    /**
     * Removes a packet interceptor.
     *
     * @param packetInterceptor the packet interceptor to remove.
     */
    public void removePacketWriterInterceptor(
        PacketInterceptor packetInterceptor );

    /**
     * Creates a new packet collector for this connection. A packet filter determines
     * which packets will be accumulated by the collector.
     *
     * @param packetFilter the packet filter to use.
     * @return a new packet collector.
     */
    public PacketCollector createPacketCollector(
        PacketFilter packetFilter );

    /**
     * Adds a connection listener to this connection that will be notified when
     * the connection closes or fails. The connection needs to already be connected
     * or otherwise an IllegalStateException will be thrown.
     *
     * @param connectionListener a connection listener.
     */
    public void addConnectionListener(
        ConnectionListener connectionListener );

    /**
     * Removes a connection listener from this connection.
     *
     * @param connectionListener a connection listener.
     */
    public void removeConnectionListener(
        ConnectionListener connectionListener );

    /**
     * Returns true if the connection to the server has successfully negotiated TLS. Once TLS
     * has been negotiatied the connection has been secured.
     *
     * @return true if the connection to the server has successfully negotiated TLS.
     */
    public boolean isUsingTLS();

    /**
     * Returns the SASLAuthentication manager that is responsible for authenticating with
     * the server.
     *
     * @return the SASLAuthentication manager that is responsible for authenticating with
     *         the server.
     */
    public SASLAuthentication getSASLAuthentication();

    /**
     * Returns true if network traffic is being compressed. When using stream compression network
     * traffic can be reduced up to 90%. Therefore, stream compression is ideal when using a slow
     * speed network connection. However, the server will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.<p>
     * <p/>
     * Note: to use stream compression the smackx.jar file has to be present in the classpath.
     *
     * @return true if network traffic is being compressed.
     */
    public boolean isUsingCompression();

    /**
     * Establishes a connection to the XMPP server and performs an automatic login
     * only if the previous connection state was logged (authenticated). It basically
     * creates and maintains a socket connection to the server.<p>
     * <p/>
     * Listeners will be preserved from a previous connection if the reconnection
     * occurs after an abrupt termination.
     *
     * @throws XMPPException if an error occurs while trying to establish the connection.
     *      Two possible errors can occur which will be wrapped by an XMPPException --
     *      UnknownHostException (XMPP error code 504), and IOException (XMPP error code
     *      502). The error codes and wrapped exceptions can be used to present more
     *      appropiate error messages to end-users.
     */
    public void connect() throws XMPPException;

    public void cleanup();

    public XMPPConnection downCast(); 

}
