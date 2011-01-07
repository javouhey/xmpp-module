package com.raverun.im.domain.impl;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Responsible to add/remove/update entries in the Cache
 * 
 * @author gavin bong
 */
public class RosterListenerImpl implements RosterListener
{
    @AssistedInject
    public RosterListenerImpl( @Assisted String user, @Assisted String userXmpp )
    {
        _user = user; _userXmpp = userXmpp;
    }

    @Override
    public void entriesAdded( Collection<String> justAdded )
    {
//        StringBuilder builder = new StringBuilder();
//        for( String item : justAdded )
//        {
//            builder.append( item + " ... " );
//        }
//        _logger.debug( "%%% added >" + builder.toString() + "<" );
    }

    @Override
    public void entriesDeleted( Collection<String> justDeleted )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void entriesUpdated( Collection<String> justUpdated )
    {
//        StringBuilder builder = new StringBuilder();
//        for( String item : justUpdated )
//        {
//            builder.append( item + " ... " );
//        }
//        _logger.debug( "%%% updated >" + builder.toString() + "<" );
    }

    @Override
    public void presenceChanged( Presence presence )
    {
        // ignore
    }

    private final String _user;
    private final String _userXmpp;

    private static final Logger _logger = Logger.getLogger( RosterListenerImpl.class );
}
