package com.raverun.im.domain;

import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;

public interface IMPresence
{
    String user();

    /**
     * @return the IM id of my registered account
     */
    IMIdentity receiver();

    /**
     * @return my buddy's IM id
     */
    String sender();

    PresenceUtilityIF.MyMode mode();

    String status();

    PresenceChangeType type();

    public static enum PresenceChangeType
    {
        OFFLINE(-1), MODE_STATUS_DELTA(0), ONLINE(1);

        PresenceChangeType( int code ) { _code = code; }
        public int code() { return _code; }
        public static PresenceChangeType deref( int code )
        {
            switch( code )
            {
            case 1:
                return PresenceChangeType.ONLINE;
            case -1:
                return PresenceChangeType.OFFLINE;
            case 0:
                return PresenceChangeType.MODE_STATUS_DELTA;
            default:
                throw new IllegalArgumentException( "code belongs only to integer set [-1,1] " );
            }
        }
        private final int _code;
    }

    public static final int MAX_PRESENCE_STATUS_LEN = 500;
}
