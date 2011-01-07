package com.raverun.im.domain.impl;

import static com.raverun.shared.Constraint.NonNullArgument.check;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMUserSetting;
import com.raverun.im.domain.IMUserSettingFactory2;
import com.raverun.im.infrastructure.persistence.TransportMapper;

public class IMUserSettingFactory2Impl implements IMUserSettingFactory2
{

//    @Override
//    public IMUserSetting create( TransportMapper transportMapper, String imId,
//        String imIdRaw, String imPassword, Transport transport,
//        UserSettingType saved, String userId )
//    {
//        return new IMUserSettingImpl( transportMapper, imId, imIdRaw, imPassword, 
//            new DateTime( new Date() ), transportMapper.sequenceFor( transport ), 
//            saved.code(), userId, false );
//    }

    @Override
    public IMUserSetting create( TransportMapper transportMapper, @Nonnull IMIdentity identity, 
        @Nonnull String imPassword, @Nonnull IMUserSetting.UserSettingType saved )
    {
        check( transportMapper, "transportMapper" ); check( imPassword, "imPassword" );
        check( saved, "saved" ); check( identity, "identity" );

        return new IMUserSettingImpl( identity, imPassword, saved );
    }
}
