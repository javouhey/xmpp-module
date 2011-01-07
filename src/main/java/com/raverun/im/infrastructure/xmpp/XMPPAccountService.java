package com.raverun.im.infrastructure.xmpp;

import java.util.Map;

import javax.annotation.Nonnull;

import com.raverun.im.infrastructure.xmpp.support.XMPPFault;

/**
 * Manages accounts on an XMPP server.
 * <p>
 * Scope of responsibilities
 * <ul>
 * <li>It does not access any database.
 * <li>Only speaks XMPP XML Stanzas
 * </ul>
 *
 * @author Gavin Bong
 */
public interface XMPPAccountService
{
    /**
     * Creates a XMPP user
     * <p>
     * If a user with id {@code userid} already exists, an {@code XMPPFault} is thrown and you can
     * identify the conflict by peering inside the wrapped exception
     * <pre>
     * XMPPFault xf = ...;
     * XMPPException xecp = (XMPPException)xf.getCause();
     * assertEquals( 409, xecp.getXMPPError().getCode() );
     * </pre>
     * 
     * @param userid non-nullable
     * @param password non-nullable
     * @param userProperties non-nullable. Pass in an empty {@code Map} if no properties are required
     * @throws XMPPFault if a XMPP communication error is encountered [FAULT]
     * @throws IllegalArgumentException if either one {@code userid} or {@code password} is null [CONTINGENCY]
     */
    void createAccount( @Nonnull String userid, @Nonnull String password, @Nonnull Map<String,String> userProperties );

    void removeAccount( @Nonnull String userid, @Nonnull String password );
}
