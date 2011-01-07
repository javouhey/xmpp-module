package com.raverun.im.domain.impl;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.raverun.im.common.Transport;
import com.raverun.im.common.IMConstants.Symbols;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.InvalidIMIdentityException;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.shared.Constraint;

public class IMIdentityImpl implements IMIdentity
{
    public static class FromDatabaseBuilder
    {
        private TransportMapper mapper;
        private String imId;
        private String imIdRaw;
        private int transportSequence;

        public FromDatabaseBuilder( TransportMapper theMapper )
        {
            Constraint.NonNullArgument.check( theMapper, "theMapper" );
            mapper = theMapper;
        }

        /**
         * @param theTransportSequence - refers to content of the column {@code transportSeq} in table {@code mim_user_settings}
         * @throws IllegalArgumentException when {@code theTransportSequence} is invalid
         */
        public FromDatabaseBuilder transportSequence( int theTransportSequence )
        {
            if( null == mapper.transportFor( theTransportSequence ) )
                throw new IllegalArgumentException( "invalid theTransportSequence" );

            transportSequence = theTransportSequence;
            return this;
        }

        public FromDatabaseBuilder ids( String theImId, String theImIdRaw )
        {
            if( Constraint.EmptyString.isFulfilledBy( theImId ))
                throw new IllegalArgumentException( "theImId cannot be empty" );

            if( Constraint.EmptyString.isFulfilledBy( theImIdRaw ))
                throw new IllegalArgumentException( "theImIdRaw cannot be empty" );

            imId = theImId;
            imIdRaw = theImIdRaw;
            return this;
        }

        public IMIdentityImpl build()
        {
            return new IMIdentityImpl( mapper.transportFor( transportSequence ), 
                imIdRaw, imId, transportSequence );
        }
    }

    /**
     * Used only when processing client inputs for operation 106 (LOGIN SERVICE) & 107 (LOGOUT/REMOVE SERVICE)
     * (WITHOUT the specification of the transport type)
     * <p>
     * Requirements: Need to prevent false deduction e.g. 145 is deduced as QQ!
     * <ul>
     * <li>A standalone id without the &#x40; symbol means it is a YAHOO account (as long it doesn't start 
     * with a number, because that would QQ's signature)
     * </ul>
     */
    public static class FromClientBuilderWithMissingImType
    {
        private Predicate<String> qqPredicate;
        private Predicate<String> msnPredicate;
        private Predicate<String> googPredicate;
        private Predicate<String> ymailPredicate;
        private Predicate<String> rocketmailPredicate;
        private Predicate<String> yahooOriginalPredicate;
        private Predicate<String> yahooShortCircuitVerifier;

        private TransportMapper mapper;
        private String loginId;

        public FromClientBuilderWithMissingImType( TransportMapper theMapper, 
            Predicate<String> ymailPredicate, Predicate<String> rocketmailPredicate,
            Predicate<String> yahooOriginalPredicate,
            Predicate<String> googPredicate, Predicate<String> qqPredicate,
            Predicate<String> msnPredicate )
        {
            Constraint.NonNullArgument.check( theMapper, "theMapper" );
            Constraint.NonNullArgument.check( qqPredicate, "qqPredicate" );
            Constraint.NonNullArgument.check( msnPredicate, "msnPredicate" );
            Constraint.NonNullArgument.check( googPredicate, "googPredicate" );
            Constraint.NonNullArgument.check( ymailPredicate, "ymailPredicate" );
            Constraint.NonNullArgument.check( rocketmailPredicate, "rocketmailPredicate" );
            Constraint.NonNullArgument.check( yahooOriginalPredicate, "yahooOriginalPredicate" );

            mapper = theMapper; 

            this.yahooOriginalPredicate = yahooOriginalPredicate;
            this.rocketmailPredicate = rocketmailPredicate;
            this.ymailPredicate = ymailPredicate;
            this.googPredicate = googPredicate;
            this.msnPredicate = msnPredicate;
            this.qqPredicate = qqPredicate;

            this.yahooShortCircuitVerifier = Predicates.<String>or( yahooOriginalPredicate, 
                Predicates.<String>or( ymailPredicate, rocketmailPredicate ) );
        }

        /**
         * @param loginId - content carried by the XML/JSON attributes {@code login-id} or {@code login-existed}
         * @throws IllegalArgumentException when {@code theLoginId} is empty
         */
        public FromClientBuilderWithMissingImType loginId( String theLoginId )
        {
            if( Constraint.EmptyString.isFulfilledBy( theLoginId ))
                throw new IllegalArgumentException( "login id cannot be empty" );

            loginId = theLoginId.trim();
            return this;
        }

        /**
         * LIMITATION: cannot differentiate among an invalid input, a snipped yahoo or a MIM account 
         */
        private Transport deduceTransportFrom( String loginId )
        {
            if( googPredicate.apply( loginId ) )
                return Transport.GTALK;

            if( msnPredicate.apply( loginId  ) )
                return Transport.MSN;

            if( qqPredicate.apply( loginId ) )
                return Transport.QQ;

            if( yahooShortCircuitVerifier.apply( loginId ) )
                return Transport.YAHOO;

            return Transport.YAHOO;
        }

        /**
         * @throws InvalidIMIdentityException if the supplied {@code loginId} is invalid for the given transport
         * @throws IllegalStateException if you forget to call the method {@link #loginId(String)}
         */
        public IMIdentityImpl build()
        {
            if( Constraint.EmptyString.isFulfilledBy( loginId ))
                throw new IllegalStateException( "Did you forget to call the loginId(String) method?" );

            Transport transport = deduceTransportFrom( loginId );
            Integer transportDbSequence = mapper.sequenceFor( transport );

            switch( transport )
            {
            case YAHOO:
                if( yahooOriginalPredicate.apply( loginId ) )
                    return new IMIdentityImpl( transport, loginId, 
                        loginId.substring(0, loginId.indexOf( Symbols.ALIAS )), transportDbSequence );

                if( ymailPredicate.apply( loginId ) || rocketmailPredicate.apply( loginId ) )
                    return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );

                if( loginId.indexOf( Symbols.ALIAS ) != -1 )
                    return new IMIdentityImpl( transport, loginId, 
                        loginId.substring(0, loginId.indexOf( Symbols.ALIAS )), transportDbSequence );

                return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );
            case MSN:
            case GTALK:
            case QQ:
                return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );
            case MIM:
            default:
                throw new AssertionError( "Not possible" );
            }
        }
    }

    /**
     * Used only when processing client inputs for operation 106 (ADD_SERVICE)
     * that is the addition of a new service (WITH the specification of the transport type)
     * <p>
     * Requirements: 
     * <ul>
     * <li>allow people to enter a partial foo@yahoo.* id as well as the full address
     * <li>allow people to enter a partial foo@gmail.com id as well as the full address
     * </ul>
     */
    public static class FromClientBuilder
    {
        private Predicate<String> qqPredicate;
        private Predicate<String> msnPredicate;
        private Predicate<String> googPredicate;
        private Predicate<String> yahooOriginalPredicate;
        private Predicate<String> yahooShortCircuitVerifier;

        private TransportMapper mapper;
        private int imType;
        private String loginId;

        public FromClientBuilder( TransportMapper theMapper, 
            Predicate<String> ymailPredicate, Predicate<String> rocketmailPredicate,
            Predicate<String> yahooOriginalPredicate,
            Predicate<String> googPredicate, Predicate<String> qqPredicate,
            Predicate<String> msnPredicate )
        {
            Constraint.NonNullArgument.check( theMapper, "theMapper" );
            Constraint.NonNullArgument.check( qqPredicate, "qqPredicate" );
            Constraint.NonNullArgument.check( msnPredicate, "msnPredicate" );
            Constraint.NonNullArgument.check( googPredicate, "googPredicate" );
            Constraint.NonNullArgument.check( ymailPredicate, "ymailPredicate" );
            Constraint.NonNullArgument.check( rocketmailPredicate, "rocketmailPredicate" );
            Constraint.NonNullArgument.check( yahooOriginalPredicate, "yahooOriginalPredicate" );

            mapper = theMapper; 

            this.yahooOriginalPredicate = yahooOriginalPredicate;
            this.googPredicate = googPredicate;
            this.msnPredicate = msnPredicate;
            this.qqPredicate = qqPredicate;

            this.yahooShortCircuitVerifier = Predicates.<String>or( yahooOriginalPredicate, 
                Predicates.<String>or( ymailPredicate, rocketmailPredicate ) );
        }

        /**
         * @param imType - content carried by the XML/JSON attribute {@code im-type}
         * @throws IllegalArgumentException when {@code theImType} is invalid. Valid values are from 0 to 4.
         */
        public FromClientBuilder imType( int theImType )
        {
            if( theImType < 0 || theImType > 4 )
                throw new IllegalArgumentException( "Valid values for imTypes are [0,4]" );

            imType = theImType;
            return this;
        }

        /**
         * @param loginId - content carried by the XML/JSON attributes {@code login-id} or {@code login-existed}
         * @throws IllegalArgumentException when {@code theLoginId} is empty
         */
        public FromClientBuilder loginId( String theLoginId )
        {
            if( Constraint.EmptyString.isFulfilledBy( theLoginId ))
                throw new IllegalArgumentException( "login id cannot be empty" );

            loginId = theLoginId.trim();
            return this;
        }

        /**
         * @throws InvalidIMIdentityException if the supplied {@code loginId} is invalid for the given transport
         * @throws IllegalStateException if you forget to call the method {@link #loginId(String)} & {@link #imType(int)}
         */
        public IMIdentityImpl build()
        {
        // 1. Process the {@code imType}
            Transport transport = null;
            Integer transportDbSequence = 0;

            switch( imType )
            {
            case 0:
                transport = Transport.MIM;
                break;
            case 1:
                transport = Transport.MSN;
                break;
            case 2:
                transport = Transport.YAHOO;
                break;
            case 3:
                transport = Transport.GTALK;
                break;
            case 4:
                transport = Transport.QQ;
                break;
            default:
                throw new IllegalStateException( "Did you forget to call the imType(int) method?" );
            }
            transportDbSequence = mapper.sequenceFor( transport );

            // 2. Process the {@code loginId}
            if( Constraint.EmptyString.isFulfilledBy( loginId ))
                throw new IllegalStateException( "Did you forget to call the loginId(String) method?" );

            switch( transport )
            {
            case GTALK:
                String originalLoginId = loginId;
                if( ! googPredicate.apply( loginId ) )
                {
                    if( loginId.indexOf( Symbols.ALIAS ) != -1 )
                        throw new InvalidIMIdentityException( "google" );

                    loginId += "@gmail.com";
                }
                return new IMIdentityImpl( transport, originalLoginId, loginId, transportDbSequence );

            case MIM:
                return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );

            case MSN:
                if( ! msnPredicate.apply( loginId  ) )
                    throw new InvalidIMIdentityException( "msn" );

                return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );

            case QQ:
                if( ! qqPredicate.apply( loginId ) )
                    throw new InvalidIMIdentityException( "qq" );

                return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );

            case YAHOO:
                if( ! yahooShortCircuitVerifier.apply( loginId ) )
                {
                    if( loginId.indexOf( Symbols.ALIAS ) != -1 )
                        throw new InvalidIMIdentityException( "yahoo" );

                    return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );
                }

                if( yahooOriginalPredicate.apply( loginId ) )
                {
                    String canonicalLoginId = loginId.substring(0, loginId.indexOf( Symbols.ALIAS ));
                    return new IMIdentityImpl( transport, loginId, canonicalLoginId, transportDbSequence );
                }

                return new IMIdentityImpl( transport, loginId, loginId, transportDbSequence );

            default:
                throw new AssertionError( "processing loginId" );
            }
        }
    }

    /**
     * Constructor used when processing inputs from clients.
     * <p>
     * TODO it is dangerous to use the default locale for {@link String#toLowerCase()}.
     *
     * @param transport - content carried by the XML/JSON attribute {@code im-type}
     * @param rawLoginId - content carried by the XML/JSON attributes {@code login-id} or {@code login-existed}
     * @param canonicalLoginId - a version of {@code rawLoginId}. All characters will be converted to lower cased versions using the default Locale
     * @param transportDbSequence - the sequence for parameter {@code transport}
     */
    private IMIdentityImpl( Transport transport, String rawLoginId, 
        String canonicalLoginId, Integer transportDbSequence )
    {
        _imIdRaw = rawLoginId;
        _transport = transport;
        _imId = canonicalLoginId.toLowerCase();
        _transportDbSequence = transportDbSequence;
    }

    @Override
    public String imId()
    {
        return _imId;
    }

    @Override
    public String imIdRaw()
    {
        return _imIdRaw;
    }

    @Override
    public Transport transport()
    {
        return _transport;
    }

    @Override
    public Integer transportDbSequence()
    {
        return _transportDbSequence;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( _imId ).append( Symbols.VERTICAL_LINE );
        builder.append( _imIdRaw ).append( Symbols.VERTICAL_LINE );
        builder.append( _transport.code() );
        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_imId == null) ? 0 : _imId.hashCode());
        result = prime * result
            + ((_transport == null) ? 0 : _transport.hashCode());
        result = prime
            * result
            + ((_transportDbSequence == null) ? 0 : _transportDbSequence
                .hashCode());
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
        IMIdentityImpl other = (IMIdentityImpl) obj;
        if( _imId == null )
        {
            if( other._imId != null )
                return false;
        }
        else if( !_imId.equals( other._imId ) )
            return false;
        if( _transport == null )
        {
            if( other._transport != null )
                return false;
        }
        else if( !_transport.equals( other._transport ) )
            return false;
        if( _transportDbSequence == null )
        {
            if( other._transportDbSequence != null )
                return false;
        }
        else if( !_transportDbSequence.equals( other._transportDbSequence ) )
            return false;
        return true;
    }

    private final Integer   _transportDbSequence;
    private final Transport _transport;
    private final String    _imIdRaw;
    private final String    _imId;
}
