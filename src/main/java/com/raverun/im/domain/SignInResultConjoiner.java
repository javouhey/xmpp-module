package com.raverun.im.domain;

import javax.annotation.Nonnull;

import com.raverun.im.domain.IMConnection.SignInResult;

public interface SignInResultConjoiner
{
    /**
     * @throws IllegalArgumentException if any parameters are null
     */
    SignInResult conjoin( @Nonnull SignInResult first, @Nonnull SignInResult second );
}
