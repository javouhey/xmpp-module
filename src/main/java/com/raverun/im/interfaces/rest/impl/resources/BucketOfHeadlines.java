package com.raverun.im.interfaces.rest.impl.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.domain.IMMessageHeadline;
import com.raverun.im.shared.rest.MeHolder;
import com.raverun.shared.Constraint;

public class BucketOfHeadlines
{
    public static class Builder
    {
        public Builder( IMTypeTransportMapper imtypeTranslator )
        {
            builderMap = new HashMap<MeHolder,String>(4);
            builderImtypeTranslator = imtypeTranslator;
        }

        public Builder add( IMMessageHeadline line )
        {
            Constraint.NonNullArgument.check( line, "line" );

            MeHolder key = new MeHolder( line.receiver(), 
                builderImtypeTranslator.parse( line.transport() ) );

            builderMap.put( key, line.message() );
            return this;
        }

        public BucketOfHeadlines build()
        {
            BucketOfHeadlines retval = new BucketOfHeadlines();
            retval._map.putAll( builderMap );
            return retval;
        }

        private final Map<MeHolder,String> builderMap;
        private final IMTypeTransportMapper builderImtypeTranslator;
    }

    private BucketOfHeadlines()
    {
        _map = new HashMap<MeHolder,String>(4);
    }

    /**
     * @return a {@code JSONObject} if JSON
     * @throws BucketSerializationException for any problems with the data
     */
    public Object serializeTo( MediaType outType )
    {
        try
        {
            if( outType == MediaType.APPLICATION_JSON_TYPE )
            {
                JSONArray array = new JSONArray();

                for( MeHolder me : _map.keySet() )
                {
                    JSONObject obj = new JSONObject();
                    JSONObject meJson = me.toJson();
                    obj.put( JSON_KEY_ME, meJson );

                    String message = _map.get( me );
                    obj.put( JSON_KEY_HEADLINE, message );
                    array.put( obj );
                }

                JSONObject retval = new JSONObject();
                retval.put( JSON_KEY_ROOT, array );

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

    private final Map<MeHolder,String> _map;
    private static final String JSON_KEY_NAME = "headlines";
    private static final String JSON_KEY_ME = "me";
    private static final String JSON_KEY_ROOT = "list";
    private static final String JSON_KEY_HEADLINE = "headline";
}
