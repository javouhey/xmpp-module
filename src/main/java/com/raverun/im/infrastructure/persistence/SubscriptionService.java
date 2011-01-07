package com.raverun.im.infrastructure.persistence;

import java.util.List;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMSubscriptionRequest;

public interface SubscriptionService
{
    /*
    * @param receiver - non empty string
    * @param sender - non empty string
    * @param user - non empty string
    * @throws javax.persistence.PersistenceException if DB layer encountered errors
    * @throws IllegalArgumentException if any of the parameters is null
    */
    void addSubscriptionRequest( String receiver, String sender, Transport transport, String user );

    List<IMSubscriptionRequest> findAllFor( String user );

    int purgeFor( String user );
}
