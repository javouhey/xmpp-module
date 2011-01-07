package com.raverun.im.domain.impl;

import com.raverun.im.domain.IMLoginCache;

public class IMLoginCacheImpl implements IMLoginCache
{
    public IMLoginCacheImpl( String userId, String device )
    {
        _userId = userId;
        _device = device;
    }

    @Override
    public String device()
    {
        return _device;
    }

    @Override
    public String userId()
    {
        return _userId;
    }

    private final String _device;
    private final String _userId;
}
