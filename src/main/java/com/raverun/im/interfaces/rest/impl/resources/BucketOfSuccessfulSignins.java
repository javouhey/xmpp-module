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

import com.raverun.im.domain.IMIdentity;
import com.raverun.shared.Constraint;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class BucketOfSuccessfulSignins
{
    public static class Builder
    {
        public Builder()
        {
            builderSeenLoginIds = new HashSet<IMIdentity>(5);
            builderPermanentMap = new MultivaluedMapImpl();
            builderAdHocMap     = new MultivaluedMapImpl();
        }

        public BucketOfSuccessfulSignins build()
        {
            BucketOfSuccessfulSignins retval = new BucketOfSuccessfulSignins();
            retval._adhoc.putAll( builderAdHocMap );
            retval._permanent.putAll( builderPermanentMap );
            return retval;
        }

        /**
         * @throws IllegalArgumentException if {@code identity} is null
         */
        public Builder add( @Nonnull IMIdentity identity, boolean permanent )
        {
            Constraint.NonNullArgument.check( identity, "identity" );

            synchronized( builderSeenLoginIds )
            {
                if( builderSeenLoginIds.contains( identity ) )
                    return this;

                builderSeenLoginIds.add( identity );
            }

            final String transportCode = identity.transport().code();
            final String imId = identity.imId();

            if( permanent )
                builderPermanentMap.add( transportCode, imId );
            else
                builderAdHocMap.add( transportCode, imId );

            return this;
        }

        private final Set<IMIdentity> builderSeenLoginIds;

        private final MultivaluedMap<String, String> builderPermanentMap;
        private final MultivaluedMap<String, String> builderAdHocMap;
    }

    private BucketOfSuccessfulSignins()
    {
        _permanent = new MultivaluedMapImpl();
        _adhoc     = new MultivaluedMapImpl();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        serializeMap( builder, _permanent, true );
        serializeMap( builder, _adhoc, false );

        return builder.toString();
    }

    private void serializeMap( StringBuilder builder, MultivaluedMap<String, String> map,
        boolean permanent )
    {
        builder.append( (permanent) ? "permanent:" : "adhoc: " ).append( "\n" );
        for( String key : map.keySet() )
        {
            builder.append( "\t" + key ).append( "\n" );
            List<String> ids = map.get( key );
            for( String id : ids )
                builder.append( "\t\t" + id ).append( "\n" );
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

    public String keyName()
    {
        return JSON_KEY_NAME;
    }

    private static final String JSON_KEY_NAME = "success";

    private MultivaluedMap<String, String> _permanent;
    private MultivaluedMap<String, String> _adhoc;
}
