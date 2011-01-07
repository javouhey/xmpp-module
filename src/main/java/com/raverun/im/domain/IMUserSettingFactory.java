package com.raverun.im.domain;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;

/**
 * Use this factory to instantiate IMUserSettings from query results against databases
 * 
 * @author Gavin Bong
 * @see IMUserSettingFactory2
 */
public interface IMUserSettingFactory
{
    IMUserSetting create( @Nonnull String imId, 
        @Nonnull String imIdRaw, @Nonnull String imPassword, @Nonnull DateTime modified, 
        @Nonnull int transportSeq, @Nonnull int saved, @Nonnull String userId,
        @Nonnull boolean fromDb );
}
