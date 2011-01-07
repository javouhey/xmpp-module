package com.raverun.im.domain;

/**
 * Factory methods to create domain objects
 *
 * @author Gavin Bong
 */
public interface DomainUtility
{
    /**
     * Construct an {@code IMIdentity} by guessing its Transport type
     *
     * @throws InvalidIMIdentityException if the loginId is invalid
     * @throws IllegalStateException if a programming error occurs
     */
    IMIdentity newIdentityFor( String loginId );

    /**
     * Construct an {@code IMIdentity}
     * 
     * @throws InvalidIMIdentityException if the loginId is invalid
     * @throws IllegalStateException if a programming error occurs
     */
    IMIdentity newIdentityFor( String loginId, int theImType );
}
