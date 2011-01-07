package com.raverun.im.domain.impl;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.raverun.im.domain.DomainUtility;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.infrastructure.persistence.TransportMapper;

public class DomainUtilityImpl implements DomainUtility
{

    @Override
    public IMIdentity newIdentityFor( String loginId )
    {
        IMIdentityImpl.FromClientBuilderWithMissingImType builder =
            new IMIdentityImpl.FromClientBuilderWithMissingImType( _transportMapper, 
                _ymailPredicate, _rocketmailPredicate,
                _yahooPredicate, _googlePredicate, _qqPredicate, 
                _msnPredicate );

        return builder.loginId( loginId ).build();
    }

    @Override
    public IMIdentity newIdentityFor( String loginId, int theImType )
    {
        IMIdentityImpl.FromClientBuilder builder = 
            new IMIdentityImpl.FromClientBuilder(_transportMapper, 
                _ymailPredicate, _rocketmailPredicate,
                _yahooPredicate, _googlePredicate, _qqPredicate, 
                _msnPredicate );

        return builder.loginId( loginId ).imType( theImType ).build();
    }

    @Inject
    public DomainUtilityImpl(TransportMapper transportMapper,
        @Named("ymail") Predicate<String> ymailPredicate,
        @Named("rocketmail") Predicate<String> rocketmailPredicate,
        @Named("yahoo") Predicate<String> yahooPredicate,
        @Named("google") Predicate<String> googlePredicate,
        @Named("qq") Predicate<String> qqPredicate,
        @Named("msn") Predicate<String> msnPredicate )
    {
        _rocketmailPredicate = rocketmailPredicate;
        _transportMapper     = transportMapper;
        _googlePredicate     = googlePredicate;
        _ymailPredicate      = ymailPredicate;
        _yahooPredicate      = yahooPredicate;
        _qqPredicate         = qqPredicate;
        _msnPredicate        = msnPredicate;
    }

    private final TransportMapper   _transportMapper;
    private final Predicate<String> _ymailPredicate;
    private final Predicate<String> _rocketmailPredicate;
    private final Predicate<String> _yahooPredicate;
    private final Predicate<String> _googlePredicate;
    private final Predicate<String> _qqPredicate;
    private final Predicate<String> _msnPredicate;
}
