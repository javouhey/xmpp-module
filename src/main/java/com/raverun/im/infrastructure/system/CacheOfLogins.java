package com.raverun.im.infrastructure.system;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

/**
 * A single point of entry to the cache which will store items to prevent concurrent logins
 *
 * @author Gavin Bong
 */
@ThreadSafe
public interface CacheOfLogins
{
    /**
     * @throws PersistenceException if there was an error with that layer
     * @return if the user is absent in cache & the put was successful, then it is reflected in the returned instance of {@code PutResult}.
     * If the user exists in cache, thus nothing was added, but the current device logged in is returned.
     */
    @Nonnull PutResult putIfAbsent( @Nonnull String user, @Nullable String device );

    /**
     * Deletes the user from the cache
     */
    void delete( @Nonnull String user );

    interface PutResult
    {
        boolean putSuccessfull();

        String deviceInUse();
    }

    public final static String DEFAULT_DEVICE = "unknown device";
}
