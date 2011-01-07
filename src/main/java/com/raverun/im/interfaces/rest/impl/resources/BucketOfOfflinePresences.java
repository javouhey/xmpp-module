package com.raverun.im.interfaces.rest.impl.resources;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.domain.IMPresence;
import com.raverun.im.domain.IMPresence.PresenceChangeType;
import com.raverun.im.interfaces.rest.impl.resources.updates.IMPresenceFilter;
import com.raverun.im.shared.rest.MeHolder;
import com.raverun.im.shared.rest.MultivaluedMeToStringMap;
import com.raverun.shared.Constraint;

public class BucketOfOfflinePresences
{
    public static class Builder
    {
        public Builder( IMTypeTransportMapper imtypeTranslator )
        {
            builderMap = new MultivaluedMeToStringMap();
            builderImtypeTranslator = imtypeTranslator;
//            builderSeenBuddies = new HashSet<String>(8);
        }

        /**
         * 20090921 Previously we would discard duplicate {@code BuddyHolder} but
         * we dropped that since {@link IMPresenceFilter} does that job externally.
         */
        public Builder addOffline( IMPresence presence )
        {
            Constraint.NonNullArgument.check( presence, "presence" );
            if( presence.type() != PresenceChangeType.OFFLINE )
                return this;

            MeHolder key = new MeHolder( presence.receiver().imId(), 
                builderImtypeTranslator.parse( presence.receiver().transport() ) );

            final String buddy = presence.sender();

//            synchronized( builderSeenBuddies )
//            {
//                if( builderSeenBuddies.contains( buddy ) )
//                    return this;
//    
//                builderSeenBuddies.add( buddy );
//            }

            builderMap.add( key, buddy );
            return this;
        }

        public BucketOfOfflinePresences build()
        {
            BucketOfOfflinePresences retval = new BucketOfOfflinePresences();
            retval._map.putAll( builderMap );
            return retval;
        }

        private final MultivaluedMeToStringMap builderMap;
        private final IMTypeTransportMapper builderImtypeTranslator;
//        private final Set<String> builderSeenBuddies;
    }

    private BucketOfOfflinePresences()
    {
        _map = new MultivaluedMeToStringMap();
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

                    JSONArray buddyArray = new JSONArray();
                    List<String> buddies = _map.get( me );
                    for( String buddy : buddies )
                    {
                        buddyArray.put( buddy );
                    }
                    obj.put( JSON_KEY_BUDDIES, buddyArray );
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

    private final MultivaluedMeToStringMap _map;
    private static final String JSON_KEY_NAME = "offline";
    private static final String JSON_KEY_ME = "me";
    private static final String JSON_KEY_BUDDIES = "buddies";
    private static final String JSON_KEY_ROOT = "list";
}
