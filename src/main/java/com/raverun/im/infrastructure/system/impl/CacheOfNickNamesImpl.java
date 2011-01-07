package com.raverun.im.infrastructure.system.impl;

import com.google.inject.Inject;
import com.raverun.cache.CacheClient;
import com.raverun.cache.annotation.Ehcache;
import com.raverun.im.infrastructure.system.CacheKeyGenerator;
import com.raverun.im.infrastructure.system.CacheOfNickNames;

/**
 * @TODO This is NOT threadsafe!
 * 
 * @author gavin bong
 */
public class CacheOfNickNamesImpl implements CacheOfNickNames
{

    @Override
    public String read( String user, String myId, String buddyId, int imtype )
    {
        final String key = _keyGenerator.generateKey( user, imtype, myId, buddyId );
        Object valueInCache = _cache.get( key );
        return( ( valueInCache == null ) ? null : (String)valueInCache );
    }

    @Override
    public void write( String user, String userXmpp, String fromJid,
        String nickName )
    {
        final String key = _keyGenerator.generateKey( user, userXmpp, fromJid );
        if( null == key )
            return;

        _cache.put( key, nickName, 1800, 1800 );
    }

    @Inject
    public CacheOfNickNamesImpl( @Ehcache CacheClient cache, CacheKeyGenerator keyGenerator )
    {
        _cache = cache;
        _keyGenerator = keyGenerator;
    }

    private final CacheClient _cache;
    private final CacheKeyGenerator _keyGenerator;
}
