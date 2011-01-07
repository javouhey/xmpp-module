package com.raverun.im.infrastructure.system;

/**
 * @author Gavin Bong
 * @see CacheKeyGenerator
 */
public interface CacheOfNickNames
{
    void write( String user, String userXmpp, String fromJid, String nickName );

    String read( String user, String myId, String buddyId, int imtype );
}
