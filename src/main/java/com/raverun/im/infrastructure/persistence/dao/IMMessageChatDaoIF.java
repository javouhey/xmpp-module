package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.List;

import com.raverun.im.domain.IMMessageChat;

public interface IMMessageChatDaoIF
{

    public long create( IMMessageChat chat ) throws SQLException;

    public int purgeAllForUser( String user ) throws SQLException;

    public List<IMMessageChat> getAllForUser( String user ) throws SQLException;

}
