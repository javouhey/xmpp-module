package com.raverun.im.interfaces.rest;

import com.raverun.im.interfaces.rest.VersionChecker.Version;

public interface ProtocolProcessorSelector
{
    ProtocolProcessor selectBasedOn( Version version );
}
