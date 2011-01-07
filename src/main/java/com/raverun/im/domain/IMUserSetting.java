package com.raverun.im.domain;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;

/**
 * Replaces {@code IMLoginInfo}
 * 
 * @author Gavin Bong
 */
public interface IMUserSetting extends Comparable<IMUserSetting>
{
    public static enum UserSettingType 
    {
        MANUALLOGIN(0), AUTOLOGIN(1), TEMPORARY(3);

        UserSettingType( int code )
        {
            _code = code;
        }

        public int code()
        {
            return _code;
        }

        /**
         * @return nullable
         */
        public static UserSettingType deref( int code )
        {
            UserSettingType[] verbs = values();
            Map<Integer, UserSettingType> m = new HashMap<Integer,UserSettingType>(3);
            for( int i=0; i<verbs.length; i++ )
                m.put( verbs[i].code(), verbs[i] );

            return m.get( code );
        }

        private final int _code;
    }

    @Nullable Long sequence();

    void setSequence( @Nonnull Long sequence ); 

    @Nullable String userId();

    void setUserid( @Nonnull String userId );

    IMIdentity identity();

    String imPassword();

    UserSettingType saved();

    @Nullable DateTime modified();

    boolean isNew();
}
