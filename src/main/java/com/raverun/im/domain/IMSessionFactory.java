package com.raverun.im.domain;

import javax.annotation.Nonnull;

public interface IMSessionFactory
{
    IMSession create( @Nonnull String userid );
}
