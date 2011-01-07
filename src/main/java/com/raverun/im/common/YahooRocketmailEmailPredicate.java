package com.raverun.im.common;

import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.raverun.shared.Constraint;

public class YahooRocketmailEmailPredicate implements Predicate<String>
{
    public YahooRocketmailEmailPredicate()
    {
        _pattern = Pattern.compile( "^\\S+@rocketmail.com$", Pattern.CASE_INSENSITIVE );
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
