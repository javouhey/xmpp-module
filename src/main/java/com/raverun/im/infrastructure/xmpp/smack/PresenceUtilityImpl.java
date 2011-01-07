package com.raverun.im.infrastructure.xmpp.smack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jivesoftware.smack.packet.Presence;

import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

public class PresenceUtilityImpl implements PresenceUtilityIF
{
    @Override
    public MyMode derefXmpp( Presence.Mode mode )
    {
        Constraint.NonNullArgument.check( mode, "mode" );

        if( mode == Presence.Mode.available )
            return MyMode.AVAILABLE;
        else if( mode == Presence.Mode.away )
            return MyMode.AWAY;
        else if( mode == Presence.Mode.dnd )
            return MyMode.DO_NOT_DISTURB;
        else
            throw new AssertionError( "not applicable" );
    }

    @Nonnull
    @Override
    public Presence derefClient( @Nullable String mymode, @Nullable String status )
    {
        Presence.Type typeChosen = null;
        Presence.Mode modeChosen = null;
        String defaultStatus = Common.EMPTY_STRING;

        if( Constraint.EmptyString.isFulfilledBy( mymode ) )
        {
            modeChosen = Presence.Mode.available;
            typeChosen = Presence.Type.available;
        }
        else
        {
            String rawMode = mymode.trim();
            int integralMode = 4;

            try
            {
                integralMode = Integer.parseInt( rawMode );
            }
            catch( NumberFormatException nfe )
            {
            }

            switch( integralMode )
            {
            case 1:
                typeChosen = Presence.Type.unavailable;
                break;

            case 2:
                typeChosen = Presence.Type.available;
                modeChosen = Presence.Mode.dnd;
                defaultStatus = "Do Not Disturb";
                break;

            case 3:
                typeChosen = Presence.Type.available;
                modeChosen = Presence.Mode.away;
                defaultStatus = "Away";
                break;

            default:
                modeChosen = Presence.Mode.available;
                typeChosen = Presence.Type.available;
                defaultStatus = "Available";
                break;
            }
        }

        if( !Constraint.EmptyString.isFulfilledBy( status ) )
            defaultStatus = status.trim();


        if( typeChosen == Presence.Type.unavailable )
            return new Presence( typeChosen );
        else
            return new Presence( typeChosen, defaultStatus, DEFAULT_PRESENCE_PRIORITY, modeChosen );
    }

    public Presence derefClient( @Nonnull MyMode mymode, @Nullable String status )
    {
        Presence.Type typeChosen = null;
        Presence.Mode modeChosen = null;
        String defaultStatus = Common.EMPTY_STRING;

        if( mymode == null )
        {
            modeChosen = Presence.Mode.available;
            typeChosen = Presence.Type.available;
        }
        else
        {
            switch( mymode )
            {
            case UNAVAILABLE:
                typeChosen = Presence.Type.unavailable;
                break;

            case DO_NOT_DISTURB:
                typeChosen = Presence.Type.available;
                modeChosen = Presence.Mode.dnd;
                defaultStatus = "Do Not Disturb";
                break;

            case AWAY:
                typeChosen = Presence.Type.available;
                modeChosen = Presence.Mode.away;
                defaultStatus = "Away";
                break;

            default:
                modeChosen = Presence.Mode.available;
                typeChosen = Presence.Type.available;
                defaultStatus = "Available";
                break;
            }
        }

        if( !Constraint.EmptyString.isFulfilledBy( status ) )
            defaultStatus = status.trim();

        if( typeChosen == Presence.Type.unavailable )
            return new Presence( typeChosen );
        else
            return new Presence( typeChosen, defaultStatus, DEFAULT_PRESENCE_PRIORITY, modeChosen );
    }

    private static final int DEFAULT_PRESENCE_PRIORITY = 1;
}
