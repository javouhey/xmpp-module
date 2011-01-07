package com.raverun.im.domain;

import javax.annotation.Nonnull;

import com.raverun.im.common.Transport;

public interface IMMessageHeadline
{
    @Nonnull Transport transport();

    String receiver();

    String user();

    String message();
}
