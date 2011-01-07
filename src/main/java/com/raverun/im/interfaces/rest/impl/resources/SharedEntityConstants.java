package com.raverun.im.interfaces.rest.impl.resources;

public interface SharedEntityConstants
{
    public interface IMTransport
    {
        public final static String LOGIN_ID  = "loginId";
        public final static String PASSWORD  = "pssword";
        public final static String AUTOLOGIN = "autoLogin";
        public final static String NEW_AUTOLOGIN = "new.autoLogin";
        public final static String IMTYPE    = "imType";
        public final static String ADHOC     = "adhoc";
        public final static String IMIDS     = "imids";
    }

    public interface Chat
    {
        public final static String FROM_ID = "fromId";
        public final static String TO_ID   = "toId";
        public final static String MSG     = "message";
    }

    public interface Subscription
    {
        public final static String NICKNAME      = "nick";
        public final static String GROUPS        = "groups";
        public final static String ACTION_ADD    = "add";
        public final static String ACTION_REMOVE = "remove";
        public final static String ACTION_ACCEPT = "accept";
        public final static String ACTION_REJECT = "reject";
    }

    public interface Presence
    {
        public final static String STATUS = "status";
        public final static String MODE   = "mode";
    }

    public interface Account
    {
        public final static String OPERATION_KEY = "accountMgmt";
        public final static String SUSPEND = "suspendAccount";
        public final static String REACTIVATE = "reactivateAccount";
        public final static String USERID_KEY = "mcuserid";
    }

    public interface Generic
    {
        public final static String NOOP    = "noop";
        public final static String COMMAND = "cmd";
    }
}
