package com.raverun.im.infrastructure.persistence;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMLoginCache;

public interface CacheOfLoginsService
{
    /**
     * @param login - a login request
     * @throws PersistenceException
     * @throws IllegalArgumentException if any of the parameters is null
     */
     void add( @Nonnull IMLoginCache login );

     /**
      * @param user - a existing user
      * @throws PersistenceException
      * @throws IllegalArgumentException if any of the parameters is null
      */
     String findDeviceForCurrentlyLoggedIn( @Nonnull String user );

     void purgeAll();

     /**
      * @param user - a existing user
      * @throws PersistenceException
      * @throws IllegalArgumentException if any of the parameters is null
      */
     int delete( @Nonnull String user );
}
