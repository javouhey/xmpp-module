package com.raverun.im.infrastructure.system.impl;

import org.javasimon.SimonManager;
import org.javasimon.Split;

import com.raverun.im.common.IMConstants;
import com.raverun.im.infrastructure.system.JavaSimonTimer;
import com.raverun.shared.Constraint;

public class JavaSimonTimerImpl implements JavaSimonTimer
{

    @Override
    public String httpHeaderKey()
    {
        return KEY;
    }

    @Override
    public String httpHeaderValueFor( long delta )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( delta );
        builder.append( IMConstants.Symbols.SPACE );
        builder.append( NANOSECONDS );
        return builder.toString();
    }

    @Override
    public Split start( String name )
    {
        return SimonManager.getStopwatch( safeName( name ) ).start();
    }

    private final String safeName( String name )
    {
        if( Constraint.EmptyString.isFulfilledBy( name ) )
            return DEFAULT_NAME;

        return name;
    }

    private final static String DEFAULT_NAME = "default";
    private final static String KEY = "X-Runtime";
    private final static String NANOSECONDS = "ns";
}
