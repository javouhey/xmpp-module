package com.raverun.im.interfaces.rest.impl.resources;

import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONException;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.IMSession.TupleForAutologinChange;

public interface XMPPGatewayParser
{
    /**
     * @throws JSONException
     */
    @Nonnull XMPPGatewayParseResult parse( String entityBody )
        throws JSONException;

    interface XMPPGatewayParseResult
    {
        PostCommand command();
        List<TupleForAutologinChange> list();
        List<IMIdentity> listAsIdentities();
    }
}
