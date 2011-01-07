package com.raverun.im.common;

import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.raverun.shared.Constraint;

public class MSNEmailPredicate implements Predicate<String>
{
    public MSNEmailPredicate()
    {
        _patternWindowsLive = Pattern.compile( "^\\S+@windowslive\\.com$", Pattern.CASE_INSENSITIVE );
        _patternHotmail = Pattern.compile( "^\\S+@hotmail\\.\\S+$", Pattern.CASE_INSENSITIVE );
        _patternLive = Pattern.compile( "^\\S+@live\\.\\S+$", Pattern.CASE_INSENSITIVE );
        _patternMsn = Pattern.compile( "^\\S+@msn\\.\\S+$", Pattern.CASE_INSENSITIVE );
    }

    @Override
    public boolean apply( String emailAddy )
    {
        if( Constraint.EmptyString.isFulfilledBy( emailAddy ) )
            return false;

        return ( _patternWindowsLive.matcher( emailAddy ).matches() ||
            _patternHotmail.matcher( emailAddy ).matches() ||
            _patternLive.matcher( emailAddy ).matches() ||
            _patternMsn.matcher( emailAddy ).matches()
        );
    }

    Pattern _patternWindowsLive;
    Pattern _patternHotmail;
    Pattern _patternLive;
    Pattern _patternMsn;
}
