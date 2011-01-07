package com.raverun.im.domain;

import javax.annotation.Nonnull;

import com.raverun.im.common.Transport;

public interface IMMessageChat
{
    @Nonnull Transport transport();

    String receiver();

    String sender();

    String user();

    String message();
}
