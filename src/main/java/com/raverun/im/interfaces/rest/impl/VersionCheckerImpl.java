package com.raverun.im.interfaces.rest.impl;

import javax.servlet.http.HttpServletRequest;

import com.raverun.im.interfaces.rest.VersionChecker;
import com.raverun.shared.Constraint;

public class VersionCheckerImpl implements VersionChecker
{

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.VersionChecker#deduce(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Version deduce( HttpServletRequest request )
    {
        String headerVersion = request.getHeader( VERSION_KEY );
        String uriVersion    = request.getParameter( VERSION_KEY );

        if( Constraint.EmptyString.isFulfilledBy( headerVersion ) &&
            Constraint.EmptyString.isFulfilledBy( uriVersion ) )
            return DEFAULT_VERSION;

        if( ! Constraint.EmptyString.isFulfilledBy( headerVersion ) )
            return translate( headerVersion );

        if( ! Constraint.EmptyString.isFulfilledBy( uriVersion ) )
            return translate( uriVersion );

        throw new AssertionError( "should never come here" );
    }

    private Version translate( String version )
    {
        try
        {
            return Version.parseInt( Integer.parseInt( version.trim() ) );
        }
        catch( NumberFormatException nfe )
        {
            return DEFAULT_VERSION;
        }
    }

    private final static Version DEFAULT_VERSION = Version.ZERO;

}
