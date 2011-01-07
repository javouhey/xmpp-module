package com.raverun.im.infrastructure.xmpp;

import com.raverun.im.common.*;

public interface XMPPUtility
{
    /**
     * @param transport - non nullable
     * @return the domain portion of a bare JID
     */
    String getTransportJID( Transport transport );

    /**
     * Correctly converts to safe BARE JID according to XEP-0106
     * Useful when we receive the contactId from the mobile client
     *
     * @param contactId
     * @param transport
     * @return a safe Bare JID according to XEP-0106
     */
    String newJIDfor( String contactId, Transport transport );

    /**
     * @throws IllegalArgumentException if {@code fullJid} is null 
     */
    Transport decodeTransportFor( String fullJid );

    /**
     * @throws IllegalArgumentException if {@code fullJid} is null
     * @return the name portion of the fullJid. Otherwise if {@code fullJid} represents a transport, {@link #TRANSPORT_ONLY} will be returned 
     */
    String decodeSender( String fullJid );

    public final static String TRANSPORT_ONLY = "@null";
}
