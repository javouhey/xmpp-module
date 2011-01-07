package com.raverun.im.infrastructure.system;

/**
 * Generates keys to Nickname cache
 *
 * @author Gavin Bong
 * @see CacheOfNickNames
 */
public interface CacheKeyGenerator
{
    /**
     * The cache will ignore the put if the key is null<p>
     * Only call this method iff:-
     * <ul>
     * <li>You're not insane
     * </ul>
     *
     * @return null if a key could not be generated or it was purposely chosen to be a noop
     */
    String generateKey( String user, String userXmpp, String fromJid );

    String generateKey( String user, int imtype, String myid, String buddyId );
}
