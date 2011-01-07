package com.raverun.im.infrastructure.system;

/**
 * FIXME I'm not sure how to create 2 different instances of 
 * {@code IdMutexProvider}, so I am duplicating the code. Perhaps
 * it is possible in Guice 2.0 I am not sure.
 * 
 * @author Gavin Bong
 */
public interface IdMutexForLogins
{
    Mutex getMutex( String id );

    int getMutexCount();
}
