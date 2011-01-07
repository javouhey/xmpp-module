package com.raverun.im.interfaces.rest;

public interface ResourceRegistry
{
    /** 
     * @return null if no Resource found with supplied path
     */
    Resource get( String path );

    boolean exists( String path );
}
