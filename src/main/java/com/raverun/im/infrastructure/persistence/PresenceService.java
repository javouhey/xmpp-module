package com.raverun.im.infrastructure.persistence;

import java.util.List;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMPresence;

public interface PresenceService
{
    /**
     * @param presence - a presence
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
     void add( @Nonnull IMPresence presence );

     /**
      * 
      * @throws javax.persistence.PersistenceException if DB layer encountered errors
      * @throws IllegalArgumentException if any of the parameters is null
      */
     List<IMPresence> findAllPresencesFor( String user );

     /**
      * 
      * @throws javax.persistence.PersistenceException if DB layer encountered errors
      * @throws IllegalArgumentException if any of the parameters is null
      */
     int purgePresencesFor( String user );
}
