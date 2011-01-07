package com.raverun.im.common;

import java.util.List;

import javax.annotation.Nonnull;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.raverun.im.infrastructure.persistence.UserService;
import com.raverun.im.infrastructure.persistence.UserService.User;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

/**
 * Predicate to check for existence of users. A user exists if it was registered
 * via the REST API for accounts.
 * 
 * @author Gavin Bong
 */
public class UserExistsPredicate implements Predicate<String>
{

    /**
     * @throws IllegalArgumentException if {@code userid} is null/empty
     * @return true if User with {@code userid} exists in table {@code mim_user}
     */
    @Override
    public boolean apply( @Nonnull String userid )
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            return false;

        try
        {
            List<User> users = _userDbService.find( userid );
            return( users.size() > 0 );
        }
        catch( PersistenceException pe )
        {
            _logger.error( Common.EMPTY_STRING, pe );
            return false;
        }
    }

    @Inject
    public UserExistsPredicate( UserService service )
    {
        _userDbService = service;
    }

    private final UserService _userDbService;
    private final Logger _logger = Logger.getLogger( UserExistsPredicate.class );
}
