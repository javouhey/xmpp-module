package com.raverun.im.infrastructure.system.impl;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import com.raverun.im.infrastructure.system.IdMutexProvider;
import com.raverun.im.infrastructure.system.Mutex;

/**
 * @link http://illegalargumentexception.blogspot.com/2008/04/java-synchronizing-on-transient-id.html
 */
public class IdMutexProviderImpl implements IdMutexProvider
{
    private final static int MAP_SIZE = 64;
    
    private final Map<Mutex, WeakReference<Mutex>> mutexMap = new WeakHashMap<Mutex, WeakReference<Mutex>>( MAP_SIZE );

    /** Get a mutex object for the given (non-null) id. */
    public Mutex getMutex( String id )
    {
        if( id == null )
        {
            throw new NullPointerException();
        }

        Mutex key = new MutexImpl( id );
        synchronized( mutexMap )
        {
            WeakReference<Mutex> ref = mutexMap.get( key );
            if( ref == null )
            {
                mutexMap.put( key, new WeakReference<Mutex>( key ) );
                return key;
            }
            Mutex mutex = ref.get();
            if( mutex == null )
            {
                mutexMap.put( key, new WeakReference<Mutex>( key ) );
                return key;
            }
            return mutex;
        }
    }

    /** Get the number of mutex objects being held */
    public int getMutexCount()
    {
        synchronized( mutexMap )
        {
            return mutexMap.size();
        }
    }

    private static class MutexImpl implements Mutex
    {
        private final String id;

        protected MutexImpl( String id )
        {
            this.id = id;
        }

        public boolean equals( Object o )
        {
            if( o == null )
            {
                return false;
            }
            if( this.getClass() == o.getClass() )
            {
                return this.id.equals( o.toString() );
            }
            return false;
        }

        public int hashCode()
        {
            return id.hashCode();
        }

        public String toString()
        {
            return id;
        }
    }
}
