package com.raverun.im.interfaces.rest.impl.resources;

import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONException;

import com.raverun.im.domain.IMSession.TupleForAutologinChange;

public interface TransportParser
{
    /**
     * @throws JSONException
     */
    @Nonnull TransportParseResult parse( String entityBody )
        throws JSONException;

    interface TransportParseResult
    {
        PostCommand command();
        List<TupleForAutologinChange> list();
    }
}
