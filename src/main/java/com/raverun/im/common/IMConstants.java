package com.raverun.im.common;

public interface IMConstants
{
    public interface Symbols
    {
        public final static String ALIAS         = "@";
        public final static String COLON         = ":";
        public final static String COMMA         = ",";
        public final static String SPACE         = " ";
        public final static String FULL_STOP     = ".";
        public final static String VERTICAL_LINE = "|";
    }

    /**
     * Represents the codes used by the mobile clients to indicate
     * the type of transport. This is different from the database-
     * backed constants in @link {@link Transport}
     *
     * @see Transport
     */
    public interface ClientLiteralsForTransport
    {
        public final static int MIM      = 0;
        public final static int MSN      = 1;
        public final static int YAHOO    = 2;
        public final static int GTALK    = 3;
        public final static int QQ       = 4;
        public final static int MYSPACE  = 5;
        public final static int FACEBOOK = 6;
    }

    public interface XmppOperation
    {
        public final static String KEY_CONFIG_SIGNIN_WAIT  = "xmpp.signin.wait.ms";
        public final static String KEY_CONFIG_SIGNIN_RETRY = "xmpp.signin.retry";
    }
}
