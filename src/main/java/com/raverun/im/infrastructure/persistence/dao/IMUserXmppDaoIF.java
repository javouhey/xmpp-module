package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserXmpp;
import com.raverun.im.domain.IMUserXmppWrapper;

public interface IMUserXmppDaoIF
{

    Set<IMUserSetting> findAll( String user, String userXmpp )
        throws SQLException;

    Set<IMUserSetting> findAllForFilterBySaved(
        @Nonnull String user, @Nonnull String userXmpp,
        @Nonnull IMUserSetting.UserSettingType savedType ) throws SQLException;

    @Nullable IMUserSetting findSingle( @Nonnull String user, @Nonnull String userXmpp, @Nonnull Transport transport ) 
        throws SQLException;

    IMUserXmppWrapper findAllUserXmppForUser(
        @Nonnull String user ) throws SQLException;

    IMUserXmpp findPrimordialForUser( 
        @Nonnull String user ) throws SQLException;

    String findUserFor( String userXmpp ) throws SQLException;

    boolean addSetting( @Nonnull String user, @Nonnull String userXmpp,
        IMUserSetting aSetting ) throws SQLException;

    /**
     * This method will create a row in table {@code mim_user_xmpp}
     *
     * @param imUserXmpp - non null value and MUST satisfy condition {@code IMUserXmpp#isNew() == true}
     * @return the auto-generated PK if successful or null otherwise
     * @throws SQLException
     */
    Long create( @Nonnull IMUserXmpp imUserXmpp )
        throws SQLException;

    boolean remove( @Nonnull IMUserXmpp imUserXmpp )
        throws SQLException;

}
