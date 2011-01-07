package com.raverun.im.domain;

import java.util.Collections;
import java.util.List;

import com.raverun.im.domain.IMConnection.SignInErrorInfo;
import com.raverun.im.domain.IMConnection.SignInSuccessInfo;

/**
 * Common classes and constants for package {@link com.raverun.im.domain} 
 *
 * @author Gavin Bong
 */
public abstract class IMShared
{
    /**
     * Null Object to denote a noop.
     */
    public static IMConnection.SignInResult NULL_SIGNIN_RESULT = new IMConnection.SignInResult()
    {
        @Override
        public List<SignInErrorInfo> failed()
        {
            return Collections.emptyList();
        }

        @Override
        public List<SignInSuccessInfo> success2()
        {
            return Collections.emptyList();
        }
    };
}
