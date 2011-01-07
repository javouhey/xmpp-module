package com.raverun.im.infrastructure.system;

/**
 * FIXME This is <strong>NOT</strong> a provider. Name is inappropriate. Rename it to {@code IdMutexForHttpSession}
 */
public interface IdMutexProvider
{
    Mutex getMutex( String id );

    int getMutexCount();
}
