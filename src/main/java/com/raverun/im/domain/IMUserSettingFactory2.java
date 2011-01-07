package com.raverun.im.domain;

import javax.annotation.Nonnull;

import com.raverun.im.infrastructure.persistence.TransportMapper;

/**
 * Use this factory to instantiate new IMUserSettings
 * 
 * @author Gavin Bong
 * @see IMUserSettingFactory
 */
public interface IMUserSettingFactory2
{
    IMUserSetting create( TransportMapper transportMapper, @Nonnull IMIdentity identity, 
        @Nonnull String imPassword, @Nonnull IMUserSetting.UserSettingType saved );
}
