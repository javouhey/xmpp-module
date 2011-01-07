package com.raverun.im.infrastructure.xmpp.support;

public class XMPPFault extends RuntimeException
{
    public static enum XmppFaultCode {
        ON_CREATE_XMPP_USER, ON_DELETE_XMPP_USER, ON_IMCONNECTION_CONNECT, DISCO_TRANSPORT, SIGNIN, SIGNOUT, SENDMSG, BUDDY_ADD, BUDDY_REMOVE, BUDDY_ACCEPT, BUDDY_REJECT, SET_MODE, REGISTER
    };

    public XMPPFault( String message, XmppFaultCode code )
    {
        super( message );
        _faultCode = code;
    }

    public XMPPFault( String message, Throwable throwable, XmppFaultCode code )
    {
        super( message, throwable );
        _faultCode = code;
    }

    
    public XmppFaultCode XmppFaultCode()
    {
        return _faultCode;
    }

    private final XmppFaultCode _faultCode;
    private static final long serialVersionUID = 1L;
}
