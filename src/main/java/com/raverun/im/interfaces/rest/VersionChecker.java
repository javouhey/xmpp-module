package com.raverun.im.interfaces.rest;

import javax.servlet.http.HttpServletRequest;

public interface VersionChecker
{
    public final static String VERSION_KEY = "x-mc-version";

    /**
     * Will look for a version parameter as identified by key {@link #VERSION_KEY} 
     * in the following order:
     * <ul>
     * <li>HTTP header
     * <li>HTTP request URI parameter
     * </ul>
     */
    Version deduce( HttpServletRequest request );

    public enum Version
    {
        ZERO(0), ONE(1), TWO(2);

        private final int c;

        Version( int code )
        {
            c = code;
        }

        public int code()
        {
            return c;
        }

        public static Version parseInt( int code )
        {
            switch( code )
            {
            case 1:
                return ONE;
            case 2:
                return TWO;
            case 0:
            default:
                return ZERO;
            }
        }
    }
}
