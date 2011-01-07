package com.raverun.im.infrastructure.system;

import javax.annotation.Nonnull;

/**
 * Database encryption
 *
 * @author gavin bong
 */
public interface PasswordCipher
{
    String encrypt( @Nonnull String plainText );

    String decrypt( @Nonnull String cipherText );
}
