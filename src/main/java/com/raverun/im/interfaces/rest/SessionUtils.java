package com.raverun.im.interfaces.rest;

import javax.servlet.http.HttpServletRequest;

import com.raverun.im.domain.IMSession;
import com.raverun.im.interfaces.rest.impl.InvalidatedHttpSessionException;

/**
 * This is specifically targeted at the IM module
 * <p>
 * A new instance is created based on the current @link {@link HttpServletRequest}
 */
public interface SessionUtils
{
    /**
     * Internally it must also connect to openfire and only write to HttpSession if it completes successfully
     * 
     * @param autoLogin - true to ask {@code imSession} to autoLogin during connection to openfire.
     * @throws IllegalArgumentException if {@code imSession} is null
     */
    NewSessionResult createNewSessionAndSetAttribute( String key, IMSession imSession, boolean autoLogin );

    /**
     * Retrieves the {@code IMSession} from the SESSION scope for this request scope
     *
     * @param key - non nullable value
     * @return a null to mean that no session is associated with this request
     * @throws InvalidatedHttpSessionException if the session was invalidated
     */
    IMSession getSessionAttribute( String key );

    /**
     * @throws IllegalArgumentException if {@code key} is null
     */
    void invalidateThisSessionForUser( String key );

    String buildLocation( String key ); 

    public interface NewSessionResult
    {
        public boolean ok();
        public String message();
        public String httpSessionId();

        /**
         * @return a structure which contains a list of IM accounts for the user
         */
        public Object loginReport();
    }

    public final static String SESSION_ATTRIBUTE_IM = "imsession";
}
