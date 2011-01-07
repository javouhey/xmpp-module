package com.raverun.im.interfaces.rest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import com.google.inject.Injector;
import com.raverun.im.bootstrap.GuiceServletContextListener;
import com.raverun.im.domain.IMSession;
import com.raverun.im.infrastructure.system.CacheOfLogins;

/**
 * Responsible to disconnect the session object {@code IMSession} when the session has timed-out.
 *
 * @author Gavin Bong
 */
public class IMSessionAttributeListener implements HttpSessionAttributeListener
{
    public void attributeAdded( HttpSessionBindingEvent sessionEvent )
    {
        ServletContext context = sessionEvent.getSession().getServletContext();
        Object value = sessionEvent.getValue();

        if( isIMSessionKey( sessionEvent.getName(), value ) )
        {
            IMSession ims = (IMSession)value;
            context.log( "IM session added for user [" + ims.userId() + "]" );
        }
    }

    public void attributeRemoved( HttpSessionBindingEvent sessionEvent )
    {
        ServletContext context = sessionEvent.getSession().getServletContext();
        Object value = sessionEvent.getValue();

        if( isIMSessionKey( sessionEvent.getName(), value ) )
        {
            IMSession ims = (IMSession)value;
            context.log( "IM session removed for user [" + ims.userId() + "]" );
            ims.stop();

            Injector injector = (Injector)context.getAttribute( GuiceServletContextListener.KEY );
            if( injector != null )
            {
                // @TODO this always return a new instance. Use a Provider ?
                CacheOfLogins loginCache = injector.getInstance( CacheOfLogins.class );
                if( loginCache != null )
                {
                    loginCache.delete( ims.userId() );
                    context.log( "Removed login cache for user [" + ims.userId() + "]" );
                }
            }
        }
    }

    public void attributeReplaced( HttpSessionBindingEvent sessionEvent )
    {
        // ignored
    }
    
    /**
     * @return true iff {@code name} matches {@value SessionUtils#SESSION_ATTRIBUTE_IM} and {@code value} is an instance of {@link IMSession}
     */
    private final boolean isIMSessionKey( String name, Object value )
    {
        if( ! name.equals( SessionUtils.SESSION_ATTRIBUTE_IM ) )
            return false;

        if( ! (value instanceof IMSession) )
            return false;

        return true;
    }
}

