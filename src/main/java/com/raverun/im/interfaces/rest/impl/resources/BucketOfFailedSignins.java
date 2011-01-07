package com.raverun.im.interfaces.rest.impl.resources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.common.IMConstants;
import com.raverun.im.domain.IMIdentity;
import com.raverun.im.domain.SigninErrorCodes;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class BucketOfFailedSignins
{
    public static class Builder
    {
        public Builder()
        {
            builderSeenLoginIds = new HashSet<IMIdentity>(5);
            builderPermanentMap = new MultivaluedMapImpl();
            builderAdHocMap     = new MultivaluedMapImpl();
        }

        public BucketOfFailedSignins build()
        {
            BucketOfFailedSignins retval = new BucketOfFailedSignins();
            retval._adhoc.putAll( builderAdHocMap );
            retval._permanent.putAll( builderPermanentMap );

            if( builderSeenLoginIds.size() > 0 )
                retval._numberSeen = builderSeenLoginIds.size();

            return retval;
        }

        /**
         * @throws IllegalArgumentException if either {@code identity} or {code why} is null
         */
        public Builder add( @Nonnull IMIdentity identity, boolean permanent, SigninErrorCodes why )
        {
            Constraint.NonNullArgument.check( identity, "identity" );
            Constraint.NonNullArgument.check( why, "why" );

            synchronized( builderSeenLoginIds )
            {
                if( builderSeenLoginIds.contains( identity ) )
                    return this;
    
                builderSeenLoginIds.add( identity );
            }

            final String transportCode = identity.transport().code();
            final String imId = identity.imId();

            if( permanent )
                builderPermanentMap.add( transportCode, fuse( imId, why ) );
            else
                builderAdHocMap.add( transportCode, fuse( imId, why ) );

            return this;
        }

        private String fuse( String imId, SigninErrorCodes why )
        {
            return imId + IMConstants.Symbols.COLON + why;
        }

        private final Set<IMIdentity> builderSeenLoginIds;

        private final MultivaluedMap<String, String> builderPermanentMap;
        private final MultivaluedMap<String, String> builderAdHocMap;
    }

    private BucketOfFailedSignins()
    {
        _permanent = new MultivaluedMapImpl();
        _adhoc     = new MultivaluedMapImpl();
    }

    public String toDisplay()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( Common.EMPTY_STRING );

        for( String transport :_permanent.keySet() )
        {
            List<String> ids = _permanent.get( transport );
            for( String id : ids )
                builder.append( id ).append( IMConstants.Symbols.COMMA ).append( IMConstants.Symbols.SPACE );
        }

        for( String transport :_adhoc.keySet() )
        {
            List<String> ids = _adhoc.get( transport );
            for( String id : ids )
                builder.append( id ).append( IMConstants.Symbols.COMMA ).append( IMConstants.Symbols.SPACE );
        }

        return builder.toString();
    }

    /**
     * defaults to JSON output
     */
    @Override
    public String toString()
    {
        try
        {
            JSONObject o = (JSONObject)serializeTo( MediaType.APPLICATION_JSON_TYPE );
            return( o.toString() );
        }
        catch( BucketSerializationException e )
        {
            return e.getMessage();
        }
    }

    private JSONObject accumulateForMap( MultivaluedMap<String, String> map ) throws JSONException
    {
        JSONObject o = new JSONObject();

        for( String key : map.keySet() )
        {
            JSONArray p = new JSONArray();
            List<String> ids = map.get( key );
            for( String id : ids )
                p.put( id );

            if( ids.size() > 0 )
                o.put( key, p );
        }

        return o;
    }

    /**
     * Returns a serialised form of this bucket depending 
     * on the type of {@code outType}
     *
     * @param outType either one of {@code MediaType#APPLICATION_JSON_TYPE} 
     * or {@code MediaType#APPLICATION_XML_TYPE}
     * @return a {@code JSONObject} if JSON
     * @throws BucketSerializationException for any problems with the data
     */
    public Object serializeTo( MediaType outType )
    {
        try
        {
            if( outType == MediaType.APPLICATION_JSON_TYPE )
            {
                JSONObject permanentValue = accumulateForMap( _permanent );
                JSONObject adhocValue = accumulateForMap( _adhoc );

                JSONObject retval = new JSONObject();
                retval.put( "permanent", permanentValue );
                retval.put( "adhoc", adhocValue );

                return retval;
            }
        }
        catch( JSONException e )
        {
            throw new BucketSerializationException( "Serialization to " + outType.toString() + " failed due to", e );
        }

        if( outType == MediaType.APPLICATION_XML_TYPE )
        {
            throw new UnsupportedOperationException( "xml under construction" );
        }

        throw new AssertionError( "only recognizes JSON & XML" );
    }

    public int numberOfFailedIds()
    {
        return _numberSeen;
    }

    public String keyName()
    {
        return JSON_KEY_NAME;
    }

    private static final String JSON_KEY_NAME = "fail";

    private int _numberSeen = 0;
    private MultivaluedMap<String, String> _permanent;
    private MultivaluedMap<String, String> _adhoc;
}
