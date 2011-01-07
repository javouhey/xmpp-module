package com.raverun.im.infrastructure.persistence;

import java.util.List;

import javax.annotation.Nonnull;

public interface UserService
{
    /**
     * Creates a new record for {@code userid}
     *
     * @param userid - non empty string
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    boolean create( @Nonnull String userid );

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    boolean remove( @Nonnull String userid );

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    List<User> find( @Nonnull String userid );

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    int activate( @Nonnull String userid );

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    int suspend( @Nonnull String userid );

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     * @throws IllegalArgumentException if {@code userid} is null or invalid
     */
    boolean isActive( @Nonnull String userid );

    /**
     * @throws javax.persistence.PersistenceException if there was a problem persisting to the DB
     */
    int count();

    interface User
    {
        String userid();
        String dateCreated();
        boolean isActive();
    }
}
