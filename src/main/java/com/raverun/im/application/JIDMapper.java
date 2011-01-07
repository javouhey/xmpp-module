package com.raverun.im.application;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMIdentity;

public interface JIDMapper
{
    IMIdentity getCanonicalReceiver( Transport transport, String user, String userXmpp );

    /**
     * @param decodedSender - decoded from JID e.g. {@code me@gmail.com} instead of {@code me\40gmail.com}
     * @param transport
     * @return safe to be sent to the mobile client
     */
    String getCanonicalSender( String decodedSender, Transport transport );
}
