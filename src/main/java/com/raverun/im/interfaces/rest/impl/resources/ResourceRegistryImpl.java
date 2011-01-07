package com.raverun.im.interfaces.rest.impl.resources;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.raverun.im.interfaces.rest.Resource;
import com.raverun.im.interfaces.rest.ResourceRegistry;
import com.raverun.shared.Constraint;

public class ResourceRegistryImpl implements ResourceRegistry
{
    @Inject
    public ResourceRegistryImpl( @Named("user") Resource userResource,
        @Named("session") Resource sessResource, 
        @Named("transport") Resource transportResource,
        @Named("xmpp") Resource xmppResource,
        @Named("updates") Resource updatesResource,
        @Named("chat") Resource chatResource ,
        @Named("buddy") Resource buddyResource,
        @Named("mode") Resource modeResource,
        @Named("cache") Resource cacheResource )
    {
        _resourcesMap.put( userResource.path(), userResource );
        _resourcesMap.put( sessResource.path(), sessResource );
        _resourcesMap.put( transportResource.path(), transportResource );
        _resourcesMap.put( xmppResource.path(), xmppResource );
        _resourcesMap.put( updatesResource.path(), updatesResource );
        _resourcesMap.put( chatResource.path(), chatResource );
        _resourcesMap.put( buddyResource.path(), buddyResource );
        _resourcesMap.put( modeResource.path(), modeResource );
        _resourcesMap.put( cacheResource.path(), cacheResource );
    }

    @Override
    public Resource get( String path )
    {
        if( Constraint.EmptyString.isFulfilledBy( path ))
            return null;

        if( !_resourcesMap.containsKey( path ) )
            return null;
        
        return _resourcesMap.get( path );
    }

    @Override
    public boolean exists( String path )
    {
        return ( get( path ) != null );
    }

    Map<String, Resource> _resourcesMap = new HashMap<String, Resource>(4);
}
