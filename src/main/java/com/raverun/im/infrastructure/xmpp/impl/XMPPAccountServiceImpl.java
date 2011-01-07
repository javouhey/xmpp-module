package com.raverun.im.infrastructure.xmpp.impl;

import java.util.Map;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.raverun.im.infrastructure.xmpp.XMPPAccountService;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.support.XMPPFault;
import com.raverun.shared.Constraint;

/**
 * Services that pertains to user accounts on an XMPP server (openfire/ejabberd)
 *
 * @author Gavin Bong
 */
public class XMPPAccountServiceImpl implements XMPPAccountService
{
    @Inject
    public XMPPAccountServiceImpl( Provider<XMPPConnectionIF> xmppConnProvider )
    {
        _xmppConnProvider = xmppConnProvider;
    }

    @Override
    public void createAccount( String userid, String password, Map<String,String> userProperties )
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            throw new IllegalArgumentException( "userid is empty" );

        if( Constraint.EmptyString.isFulfilledBy( password ) )
            throw new IllegalArgumentException( "password is empty" );

        Constraint.NonNullArgument.check( userProperties, "userProperties" );

        // TODO trim userid

        XMPPConnectionIF xmppConnection =_xmppConnProvider.get();
        try
        {
            xmppConnection.connect();
            AccountManager accountMgr = xmppConnection.getAccountManager();
            accountMgr.createAccount( userid, password, userProperties );
        }
        catch( XMPPException xe )
        {
            throw new XMPPFault( "Could NOT create account for user " + userid, xe, XMPPFault.XmppFaultCode.ON_CREATE_XMPP_USER );
        }
        finally
        {
            xmppConnection.disconnect();
            xmppConnection.cleanup();
        }
    }

    @Override
    public void removeAccount( String userid, String password )
    {
        if( Constraint.EmptyString.isFulfilledBy( userid ) )
            throw new IllegalArgumentException( "userid is empty" );

        XMPPConnectionIF xmppConnection =_xmppConnProvider.get();
        try
        {
            xmppConnection.connect();
            xmppConnection.login( userid, password );
            xmppConnection.getAccountManager().deleteAccount();
        }
        catch( XMPPException xe )
        {
            throw new XMPPFault( "Could NOT delete account for user " + userid, xe, XMPPFault.XmppFaultCode.ON_DELETE_XMPP_USER );
        }
        finally
        {
            xmppConnection.disconnect();
            xmppConnection.cleanup();
        }
    }

    private final Provider<XMPPConnectionIF> _xmppConnProvider;

}
