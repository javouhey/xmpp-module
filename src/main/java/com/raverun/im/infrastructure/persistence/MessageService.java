package com.raverun.im.infrastructure.persistence;

import java.util.List;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMMessageChat;
import com.raverun.im.domain.IMMessageHeadline;

public interface MessageService
{
   /**
    * @param chat - a chat message
    * @throws javax.persistence.PersistenceException if DB layer encountered errors
    * @throws IllegalArgumentException if any of the parameters is null
    */
    void add( @Nonnull IMMessageChat chat );

    /**
     * @param headline - a headline message
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    void add( @Nonnull IMMessageHeadline headline );

    /**
     * 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    List<IMMessageChat> findAllChatsFor( String user );

    /**
     * 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    List<IMMessageHeadline> findAllHeadlinesFor( String user );

    /**
     * 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    int purgeChatsFor( String user );

    /**
     * 
     * @throws javax.persistence.PersistenceException if DB layer encountered errors
     * @throws IllegalArgumentException if any of the parameters is null
     */
    int purgeHeadlinesFor( String user );
}
