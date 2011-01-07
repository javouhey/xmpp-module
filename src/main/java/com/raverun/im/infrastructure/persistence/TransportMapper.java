package com.raverun.im.infrastructure.persistence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raverun.im.common.Transport;

public interface TransportMapper
{
    /**
     * Dereference the given transport
     * 
     * @throws IllegalArgumentException if supplied {@transport} is null
     * @throws javax.persistence.PersistenceException when we have problems communicating with data store
     * @return the numeric representation of the supplied {@code transport}. null if nothing found.
     */
    @Nullable Integer sequenceFor( @Nonnull Transport transport );

    /**
     * Dereference the given sequence number
     * 
     * @param sequence - non nullable
     * @return null if nothing for the supplied {@code sequence}
     */
    Transport transportFor( @Nonnull Integer sequence );
}
