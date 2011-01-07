package com.raverun.im.infrastructure.system.impl;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.raverun.im.domain.impl.IMLoginCacheImpl;
import com.raverun.im.infrastructure.persistence.CacheOfLoginsService;
import com.raverun.im.infrastructure.system.CacheOfLogins;
import com.raverun.im.infrastructure.system.IdMutexForLogins;
import com.raverun.im.infrastructure.system.Mutex;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint.EmptyString;

public class CacheOfLoginsImpl implements CacheOfLogins
{

    @Override
    public void delete( String user )
    {
        if( EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is null" );

        final Mutex mutex = _mutex.getMutex( user );
        synchronized( mutex )
        {
            int numDeleted = _cacheService.delete( user );
            _logger.debug( "deleted " + user + " " + numDeleted );
        }
    }

    @Override
    public PutResult putIfAbsent( String user, String theDevice )
    {
        if( EmptyString.isFulfilledBy( user ) )
            throw new IllegalArgumentException( "user is null" );

        String device = CacheOfLogins.DEFAULT_DEVICE;
        if( ! EmptyString.isFulfilledBy( theDevice ) )
            device = theDevice;

        final Mutex mutex = _mutex.getMutex( user );
        synchronized( mutex )
        {
            _logger.debug( "putIfAbsent " + user );
        // 1. attempt to add
            try
            {
                _cacheService.add( new IMLoginCacheImpl( user, device ) );
                return new PutResult() 
                {
                    public boolean putSuccessfull() { return true; }
                    public String deviceInUse() { return Common.EMPTY_STRING; }
                };
            }
            catch( PersistenceException pe )
            {
                if( !(pe instanceof EntityExistsException) ) 
                    throw pe;
                else
                    _logger.error( "Found a conflict", pe );
            }

        // 2. Need to retrieve existing device
            final String guiltyDevice = _cacheService.findDeviceForCurrentlyLoggedIn( user );
            return new PutResult()
            {
                public boolean putSuccessfull() { return false; }
                public String deviceInUse() 
                { 
                    if( EmptyString.isFulfilledBy( guiltyDevice ) )
                        return CacheOfLogins.DEFAULT_DEVICE;
                    else
                        return guiltyDevice;
                }
            };
        }
    }

    @Inject
    public CacheOfLoginsImpl( IdMutexForLogins mutex, CacheOfLoginsService cacheService )
    {
        _mutex = mutex;
        _cacheService = cacheService;
    }

    private final IdMutexForLogins _mutex;
    private final CacheOfLoginsService _cacheService;

    private final static Logger _logger = Logger.getLogger( CacheOfLoginsImpl.class );
}
