package com.raverun.im.common;

import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.raverun.shared.Constraint;

/**
 * TODO improve regex to enforce the following rules
 * <ul>
 * <li>minimum 4 characters long
 * <li>maximum 32 characters long
 * <li>no spaces in between
 * <li>must start with a character
 * <li>must have no more than 1 period character
 * <li>must never have consecutive underscores
 * <li>must never end with a period or an underscore
 * </ul>
 * 
 * <pre>
 * \s A whitespace character: [ \t\n\x0B\f\r]
 * \S A non-whitespace character: [^\s]
 * </pre> 
 */
public class YahooOriginalEmailPredicate implements Predicate<String>
{
    public YahooOriginalEmailPredicate()
    {
        _pattern = Pattern.compile( "^\\S+@yahoo\\.\\S+$", Pattern.CASE_INSENSITIVE );
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
