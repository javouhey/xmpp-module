package com.raverun.im.interfaces.rest.impl.resources;

import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONException;

import com.raverun.im.domain.IMIdentity;
import com.raverun.im.interfaces.rest.impl.resources.buddy.BuddyPostCommand;
import com.raverun.im.interfaces.rest.impl.resources.buddy.BuddyPutCommand;

public interface BuddyEntityParser
{
    /**
     * @throws JSONException
     */
    @Nonnull BuddyPostResult parsePost( String entityBody )
        throws JSONException;

    /**
     * @throws JSONException
     */
    @Nonnull BuddyPutResult parsePut( String entityBody )
        throws JSONException;

    interface BuddyPostResult
    {
        BuddyPostCommand command();
        IMIdentity fromId();
        IMIdentity toId();
    }

    interface BuddyPutResult
    {
        BuddyPutCommand command();
        IMIdentity fromId();
        IMIdentity toId();
        String toNick();
        List<String> toGroups();
    }
}
