package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.List;

import javax.annotation.Nonnull;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMSubscriptionRequest;

public interface IMSubscriptionRequestDaoIF
{
    /**
     * @return the auto-generated sequence number for the newly created {@link IMSubscriptionRequest}
     * @throws SQLException - when shit happens with mysql
     */
    public long create( @Nonnull String receiver, @Nonnull String sender, @Nonnull Transport transport, @Nonnull String user  ) throws SQLException;

    public int purgeAllForUser( @Nonnull String user ) throws SQLException;

    public List<IMSubscriptionRequest> getAllForUser( @Nonnull String user ) throws SQLException;
}
