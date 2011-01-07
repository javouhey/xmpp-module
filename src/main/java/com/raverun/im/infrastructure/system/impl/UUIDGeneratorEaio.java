package com.raverun.im.infrastructure.system.impl;

import com.eaio.uuid.UUID;
import com.raverun.im.infrastructure.system.UUIDGenerator;

public class UUIDGeneratorEaio implements UUIDGenerator
{
    public String generate()
    {
        return new UUID().toString();
    }
}
