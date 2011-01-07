package com.raverun.im.interfaces.rest.impl.resources;

import com.raverun.shared.Constraint;

public enum PostCommand
{
    DELETE("delete"), UPDATE("update");

    PostCommand( String code )
    {
        _code = code;
    }

    public String code()
    {
        return _code;
    }

    /**
     * @throws IllegalArgumentException if {@code code} is <strong>NOT</strong> {@code "delete" or "update"}
     */
    public static PostCommand deref( String code )
    {
        if( Constraint.EmptyString.isFulfilledBy( code ))
            throw new IllegalArgumentException( "code MUST not be empty" );
    
        String trimmedCode = code.trim();
        if( trimmedCode.equals( "delete" ) )
            return PostCommand.DELETE;
        else if( trimmedCode.equals( "update" ) )
            return PostCommand.UPDATE;
        else
            throw new IllegalArgumentException( "code MUST be one of 'delete' or 'update'" );
    }

    private final String _code;
}
