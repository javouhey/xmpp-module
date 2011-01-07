package com.raverun.im.interfaces.rest.impl;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.raverun.im.interfaces.rest.ProtocolProcessor;
import com.raverun.im.interfaces.rest.ProtocolProcessorSelector;
import com.raverun.im.interfaces.rest.VersionChecker.Version;
import com.raverun.im.interfaces.rest.support.guice.One;
import com.raverun.im.interfaces.rest.support.guice.Zero;

public class ProtocolProcessorSelectorImpl implements ProtocolProcessorSelector
{
    @Inject
    public ProtocolProcessorSelectorImpl( @Zero ProtocolProcessor processorZero, 
        @One ProtocolProcessor processorOne )
    {
        _processorZero = processorZero;
        _processorOne  = processorOne;

        _mapProcessors = new HashMap<Integer, ProtocolProcessor>(4);
        _mapProcessors.put( Version.ZERO.code(), _processorZero );
        _mapProcessors.put( Version.ONE.code(), _processorOne );
    }

    @Override
    public ProtocolProcessor selectBasedOn( Version version )
    {
        int numericVersion = version.code();
        if( _mapProcessors.containsKey( numericVersion ) )
            return _mapProcessors.get( numericVersion );

        return _processorZero;
    }

    private Map<Integer, ProtocolProcessor> _mapProcessors;

    private final ProtocolProcessor _processorOne;
    private final ProtocolProcessor _processorZero;
}
