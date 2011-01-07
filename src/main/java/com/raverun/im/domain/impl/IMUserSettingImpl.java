package com.raverun.im.domain.impl;

import static com.raverun.shared.Constraint.NonNullArgument.check;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.infrastructure.persistence.TransportMapper;

public class IMUserSettingImpl implements IMUserSetting
{
    @AssistedInject
    public IMUserSettingImpl( TransportMapper mapper, @Assisted String imId,
        @Assisted String imIdRaw, @Assisted String imPassword, @Assisted DateTime modified,
        @Assisted int transportSeq, @Assisted int saved, @Assisted String userId,
        @Assisted boolean fromDb )
    {
        check( mapper, "mapper" ); check( imId, "imId" ); check( imIdRaw, "imIdRaw" );
        check( imPassword, "imPassword" ); check( modified, "modified" ); check( userId, "userId" );

        IMIdentityImpl.FromDatabaseBuilder builder = new IMIdentityImpl.FromDatabaseBuilder( mapper );
        _identity = builder.transportSequence( transportSeq ).ids( imId, imIdRaw ).build();
        _imPassword = imPassword;
        _modified = modified;
        _saved = IMUserSetting.UserSettingType.deref( saved );
        _userId = userId;
        _fromDb = fromDb;
    }

    /**
     * Constructor for creating new instances based on input from mobile client
     * 
     * @param mapper
     * @param identity
     * @param imPassword
     * @param saved
     * @param fromDb
     */
    public IMUserSettingImpl( IMIdentity identity, String imPassword, 
        IMUserSetting.UserSettingType saved )
    {
        check( identity, "identity" );
        check( imPassword, "imPassword" );
        check( saved, "saved" );

        _imPassword = imPassword;
        _identity = identity;
        _saved = saved;

        _fromDb = false;
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
    public String imPassword()
    {
        return _imPassword;
    }

    @Nullable
    @Override
    public DateTime modified()
    {
        return _modified;
    }

    @Override
    public UserSettingType saved()
    {
        return _saved;
    }

    @Nullable
    @Override
    public String userId()
    {
        return _userId;
    }

    @Override
    public void setUserid( @Nonnull String userId )
    {
        check( userId, "userId" );
        _userId = userId;
    }

    @Override
    public boolean isNew()
    {
        return _fromDb == false;
    }

    @Override
    public IMIdentity identity()
    {
        return _identity;
    }

    @Override
    public int compareTo( IMUserSetting other )
    {
        int comp = 0;
        if( this.identity().hashCode() < other.identity().hashCode() )
            return 1;
        else if( this.identity().hashCode() > other.identity().hashCode() )
            return -1;

        return comp;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((_identity == null) ? 0 : _identity.hashCode());
//        result = prime * result + ((_saved == null) ? 0 : _saved.hashCode());
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
        IMUserSettingImpl other = (IMUserSettingImpl) obj;
        if( _identity == null )
        {
            if( other._identity != null )
                return false;
        }
        else if( !_identity.equals( other._identity ) )
            return false;

//        if( _saved == null )
//        {
//            if( other._saved != null )
//                return false;
//        }
//        else if( !_saved.equals( other._saved ) )
//            return false;

        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "{ " );
        builder.append( "\"sequence\":" ).append( _sequence );
        builder.append( ", \"modified\":" ).append( _modified );
        builder.append( ", \"userId\":" ).append( _userId );
        builder.append( ", \"saved\":" ).append( _saved.toString() );
        builder.append( ", \"identity\":" ).append( _identity.toString() );
        builder.append( " }" );
        return builder.toString();
    }
    
    private String _userId;
    private Long _sequence;
    private DateTime _modified;

    private final String _imPassword;
    private final IMIdentity _identity;
    private final UserSettingType _saved;
    private final boolean _fromDb;
}
