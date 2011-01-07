package com.raverun.im.infrastructure.persistence;

import java.util.List;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMOffline;

public interface OfflineService
{
    /**
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    List<IMOffline> findAllOfflineNotificationsFor( String user );

    /**
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    int purgeOfflineNotificationsFor( final String user );

    /**
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
     void add( @Nonnull IMOffline offline );
}
