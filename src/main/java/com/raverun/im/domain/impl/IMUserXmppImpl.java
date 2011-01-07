package com.raverun.im.domain.impl;

import static com.raverun.shared.Constraint.NonNullArgument.check;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserXmpp;
import com.raverun.shared.Constraint;

/**
 * TODO
 * <ul>
 * <li>Needs a collaborator which can insert/delete from DB ? 
 * </ul>
 */
public class IMUserXmppImpl implements IMUserXmpp
{
    public static class FromClientBuilder
    {
        private String user;
        private String userXmpp;
        private boolean primordial;
        List<IMUserSetting> listOfUserSettings;

        public FromClientBuilder( @Nonnull String user, @Nonnull String userXmpp, boolean primordial )
        {
            check( user, "user" );
            check( userXmpp, "userXmpp" );

            this.user = user;
            this.userXmpp = userXmpp;
            this.primordial = primordial;

            listOfUserSettings = new ArrayList<IMUserSetting>(5);
            for( int i=0; i<5; i++)
                listOfUserSettings.add( i, null );
        }

        /**
         * @throws IllegalArgumentException when {@code userSetting} is null or {@code userSetting#isNew() == false}
         */
        public FromClientBuilder userSetting( @Nonnull IMUserSetting userSetting )
        {
            check( userSetting, "userSetting" );
            if( ! userSetting.isNew() )
                throw new IllegalArgumentException( "userSetting is NOT new" );

            int position = mapToPositionInTableSchema( userSetting.identity().transport() );
            _logger.debug( "Replacing position " + position + " with " + userSetting.toString() );
            listOfUserSettings.set( position, userSetting );
            return this;
        }

        public IMUserXmppImpl build()
        {
            return new IMUserXmppImpl( user, userXmpp, primordial, false, listOfUserSettings );
        }
    }

    public static class FromDatabaseBuilder
    {
        private String user;
        private String userXmpp;
        private boolean fromDb;
        private boolean primordial;

        List<IMUserSetting> listOfUserSettings;

        /**
         * @throws IllegalArgumentException if {@code sequence} or {@code user} is null
         */
        public FromDatabaseBuilder( @Nonnull String user, @Nonnull String userXmpp, boolean primordial )
        {
            check( user, "user" );
            check( userXmpp, "userXmpp" );

            this.user = user;
            this.userXmpp = userXmpp;
            this.primordial = primordial;
            this.fromDb = true;

            listOfUserSettings = new ArrayList<IMUserSetting>(5);
            for( int i=0; i<5; i++)
                listOfUserSettings.add( i, null );
        }

        /**
         * @throws IllegalArgumentException when {@code userSetting} is null
         */
        public FromDatabaseBuilder userSetting( @Nonnull IMUserSetting userSetting )
        {
            check( userSetting, "userSetting" );

            int position = mapToPositionInTableSchema( userSetting.identity().transport() );
            _logger.debug( "Replacing position " + position + " with " + userSetting.toString() );
            listOfUserSettings.set( position, userSetting );
            return this;
        }

        public IMUserXmppImpl build()
        {
            return new IMUserXmppImpl( user, userXmpp, primordial, fromDb, listOfUserSettings );
        }
    }

    private IMUserXmppImpl( String user, String userXmpp, boolean primordial, 
        boolean fromDb, List<IMUserSetting> theSettings )
    {
        _user = user;
        _userXmpp = userXmpp;
        _fromDb = fromDb;
        _primordial = primordial;
        _listOfUserSettings.addAll( theSettings );
    }

    /**
     * @throws IllegalArgumentException if {@code setting} is null
     */
    @Override
    public void addUserSetting( @Nonnull IMUserSetting setting )
    {
        check( setting, "setting" );

        setting.setUserid( user() );
        // Service.add( setting );
    }

    @Override
    public void removeUserSetting()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isNew()
    {
        return _fromDb == false;
    }

    @Override
    public boolean isPrimordial()
    {
        return _primordial;
    }

    @Nullable
    @Override
    public Long sequence()
    {
        return _sequence;
    }

    @Override
    public void setSequence( @Nonnull Long sequence )
    {
        check( sequence, "sequence" );
        _sequence = sequence;
    }

    @Override
    public List<IMUserSetting> transports()
    {
        return Collections.unmodifiableList( _listOfUserSettings );
    }

    @Override
    public List<IMUserSetting> transportsNonNulls()
    {
        List<IMUserSetting> retval = new ArrayList<IMUserSetting>(5);
        for( IMUserSetting setting : _listOfUserSettings )
            if( setting != null ) retval.add( setting );

        return retval;
    }

    @Override
    public String user()
    {
        return _user;
    }

    @Override
    public String userXMPP()
    {
        return _userXmpp;
    }

    @Override
    public BitSet viewOfUsableTransports()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean matchWith( String userXmppId )
    {
        if( Constraint.EmptyString.isFulfilledBy( userXmppId ) )
            return false;
        
        return _userXmpp.equals( userXmppId );
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "{ " );
        builder.append( "\"sequence\":" ).append( _sequence );
        builder.append( ", \"userId\":" ).append( _user );
        builder.append( ", \"userXmpp\":" ).append( _userXmpp );
        builder.append( ", \"primordial\":" ).append( _primordial );
        builder.append( " }" );
        return builder.toString();
    }

    private static final int mapToPositionInTableSchema( Transport transport )
    {
        switch( transport )
        {
        case GTALK:
            return 3;
        case MIM:
            return 0;
        case MSN:
            return 1;
        case QQ:
            return 4;
        case YAHOO:
            return 2;
        default:
            throw new AssertionError( "No way!" );
        }
    }

    private Long _sequence;

    private final String _user;
    private final String _userXmpp;
    private final boolean _fromDb;
    private final boolean _primordial;

    private List<IMUserSetting> _listOfUserSettings = new ArrayList<IMUserSetting>(5);

    private static final Logger _logger = Logger.getLogger( IMUserXmppImpl.class );

}
