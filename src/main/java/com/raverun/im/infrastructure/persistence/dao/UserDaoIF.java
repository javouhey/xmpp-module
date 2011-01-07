package com.raverun.im.infrastructure.persistence.dao;

import java.sql.SQLException;
import java.util.List;

import com.raverun.im.infrastructure.persistence.UserService.User;

public interface UserDaoIF
{
    public enum UserStatus 
    { 
        ACTIVE, SUSPENDED
    };

    /**
     * This method will create a row in table {@code imdb.mim_user}
     *
     * @param call - non null value
     * @return true if it was created successfully
     * @throws SQLException
     * @throws IllegalArgumentException if parameters are invalid
     */
    public boolean create( String userid ) throws SQLException;

    public boolean remove( String userid ) throws SQLException;

    public int count() throws SQLException;

    public List<User> find( final String userid ) throws SQLException;

    public int updateStatus( String userid, UserStatus newStatus ) throws SQLException;
}
