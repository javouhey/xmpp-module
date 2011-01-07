package com.raverun.im.domain.impl;

import org.jivesoftware.smack.packet.Presence;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMPresence;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF.MyMode;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint.EmptyString;
import com.raverun.shared.Constraint.NonNullArgument;

public class IMPresenceImpl implements IMPresence
{
    public static class FromXmppServerBuilder
    {
        /**
         * @throws IllegalArgumentException
         */
        public FromXmppServerBuilder( String userid, IMIdentity receiver, String buddy, Presence.Type type )
        {
            NonNullArgument.check( receiver, "receiver" );
            NonNullArgument.check( userid, "userid" );
            NonNullArgument.check( buddy, "buddy" );
            NonNullArgument.check( type, "type" );

            builderReceiver = receiver;
            builderUserid = userid;
            builderBuddy = buddy;
            builderType = type;
        }

       /**
        * @throws IllegalArgumentException
        */
        public FromXmppServerBuilder mode( MyMode mode )
        {
            NonNullArgument.check( mode, "mode" );
            builderMode = mode; 
            return this;
        }

        /**
         * @param status - cannot be longer than {@link IMPresence#MAX_PRESENCE_STATUS_LEN}
         * @return
         */
        public FromXmppServerBuilder status( String status )
        {
            if( EmptyString.isFulfilledBy( status ))
                return this;

            String deltaStatus = status.trim();
            if( deltaStatus.length() > IMPresence.MAX_PRESENCE_STATUS_LEN )
                deltaStatus = deltaStatus.substring( 0, IMPresence.MAX_PRESENCE_STATUS_LEN );

            builderStatus = deltaStatus;
            return this;
        }

        /**
         * @throws IllegalArgumentException 
         */
        private final PresenceChangeType resolveType( Presence.Type type )
        {
            if( type == Presence.Type.available )
                return PresenceChangeType.ONLINE;
            else if( type == Presence.Type.unavailable )
                return PresenceChangeType.OFFLINE;
            else
                throw new IllegalArgumentException( "not applicable" );
        }

        public IMPresence build()
        {
            final PresenceChangeType theType = resolveType( builderType );
            IMPresenceImpl retval = new IMPresenceImpl( builderUserid, builderReceiver, builderBuddy, theType );
            if( theType == PresenceChangeType.OFFLINE )
                retval._mode = MyMode.UNAVAILABLE;
            else
                retval._mode = builderMode;

            retval._status = builderStatus;
            return retval;
        }

    // ---- mandatory ----
        private final IMIdentity builderReceiver;
        private final String builderBuddy;
        private final String builderUserid;
        private final Presence.Type builderType;

    // ---- optional ----
        private String builderStatus = Common.EMPTY_STRING;
        private MyMode builderMode = MyMode.AVAILABLE; 
    }

    public static class FromDatabaseBuilder
    {
        public FromDatabaseBuilder( String userid, IMIdentity receiver, String buddy, PresenceChangeType type, MyMode mode, String status  )
        {
            NonNullArgument.check( userid, "userid" );
            NonNullArgument.check( receiver, "receiver" );
            NonNullArgument.check( buddy, "buddy" );
            NonNullArgument.check( type, "type" );
            NonNullArgument.check( mode, "mode" );

            if( !EmptyString.isFulfilledBy( status ) )
                builderStatus = status.trim();

            builderReceiver = receiver;
            builderBuddy = buddy;
            builderUserid = userid;
            builderType = type;
            builderMode = mode;
        }

        public IMPresence build()
        {
            IMPresenceImpl impl = new IMPresenceImpl( builderUserid, builderReceiver, builderBuddy, builderType );
            impl._mode = builderMode;
            impl._status = builderStatus;
            return impl;
        }

    // ---- mandatory ----
        private final IMIdentity builderReceiver;
        private final String builderBuddy;
        private final String builderUserid;
        private final PresenceChangeType builderType;
        private String builderStatus = Common.EMPTY_STRING;
        private MyMode builderMode = MyMode.AVAILABLE; 
    }

    // @TODO should this be private ?
    public IMPresenceImpl( String userid, IMIdentity receiver, String buddy, PresenceChangeType type )
    {
        _userid = userid; _receiver = receiver; _buddy = buddy; _type = type;
    }

    @Override
    public MyMode mode()
    {
        return _mode;
    }

    @Override
    public IMIdentity receiver()
    {
        return _receiver;
    }

    @Override
    public String sender()
    {
        return _buddy;
    }

    @Override
    public String status()
    {
        return _status;
    }

    @Override
    public PresenceChangeType type()
    {
        return _type;
    }

    @Override
    public String user()
    {
        return _userid;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_buddy == null) ? 0 : _buddy.hashCode());
        result = prime * result + ((_mode == null) ? 0 : _mode.hashCode());
        result = prime * result + ((_status == null) ? 0 : _status.hashCode());
        result = prime * result + ((_userid == null) ? 0 : _userid.hashCode());
        result = prime * result + ((_type == null) ? 0 : _type.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        IMPresenceImpl other = (IMPresenceImpl) obj;
        if( _buddy == null )
        {
            if( other._buddy != null )
                return false;
        }
        else if( !_buddy.equals( other._buddy ) )
            return false;

        if( _mode == null )
        {
            if( other._mode != null )
                return false;
        }
        else if( !_mode.equals( other._mode ) )
            return false;

        if( _status == null )
        {
            if( other._status != null )
                return false;
        }
        else if( !_status.equals( other._status ) )
            return false;

        if( _userid == null )
        {
            if( other._userid != null )
                return false;
        }
        else if( !_userid.equals( other._userid ) )
            return false;

        if( _type == null )
        {
            if( other._type != null )
                return false;
        }
        else if( !_type.equals( other._type ) )
            return false;

        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "[ userid=" ).append( _userid ).append( " | " );
        builder.append( "mode=" ).append( _mode ).append( " | " );
        builder.append( "status=" ).append( _status ).append( " | " );
        builder.append( "buddy=" ).append( _buddy ).append( " | " );
        builder.append( "myimid=" ).append( _receiver.imIdRaw() ).append( " | " );
        builder.append( "type=" ).append( _type ).append( " | " );
        builder.append( "transport=" ).append( _receiver.transport().code() ).append( " ]" );
        return builder.toString();
    }

    // ---- mandatory ----
    private final IMIdentity _receiver;
    private final String _buddy;
    private final String _userid;
    private final PresenceChangeType _type;

    // ---- optional ----
    private String _status = Common.EMPTY_STRING;
    private MyMode _mode = MyMode.AVAILABLE; 
}

