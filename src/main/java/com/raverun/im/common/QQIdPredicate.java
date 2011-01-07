package com.raverun.im.common;

import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.raverun.shared.Constraint;

public class QQIdPredicate implements Predicate<String>
{
    public QQIdPredicate()
    {
        _pattern = Pattern.compile( "^\\d+$" );
    }

    @Override
    public boolean apply( String emailAddy )
    {
        if( Constraint.EmptyString.isFulfilledBy( emailAddy ) )
            return false;

        return _pattern.matcher( emailAddy ).matches();
    }

    Pattern _pattern;
}
