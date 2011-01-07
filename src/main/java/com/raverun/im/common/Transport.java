package com.raverun.im.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces IMSettings#TRANSPORT_xxx
 * <p>
 * Warning: the {@link #code()} relates to the database column in table {@code im_user_xmpp}
 * 
 * @author Gavin Bong
 *
 */
public enum Transport
{
    MIM("im"), MSN("msn"), YAHOO("yahoo"), GTALK("gtalk"), QQ("qq"),
    MYSPACEIM("myspaceim"), FACEBOOK("facebook");

    Transport( String code )
    {
        _code = code;
    }

    public String code()
    {
        return _code;
    }

    public static Transport deref( String code )
    {
        return _map.get( code );
    }

    private final String _code;

    private static final Map<String, Transport> _map;

    static {
        Transport[] verbs = values();
        _map = new HashMap<String,Transport>(verbs.length * 2);
        for( int i=0; i<verbs.length; i++ )
            _map.put( verbs[i].code(), verbs[i] );
    }
}
