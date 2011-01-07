package com.raverun.im.infrastructure.system;

import org.javasimon.Split;

public interface JavaSimonTimer
{
    Split start( String name );

    String httpHeaderKey();

    String httpHeaderValueFor( long delta );
}
