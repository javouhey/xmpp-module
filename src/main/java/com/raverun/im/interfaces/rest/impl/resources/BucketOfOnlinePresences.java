package com.raverun.im.interfaces.rest.impl.resources;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.domain.IMPresence;
import com.raverun.im.domain.IMPresence.PresenceChangeType;
import com.raverun.im.infrastructure.system.CacheOfNickNames;
import com.raverun.im.interfaces.rest.impl.resources.updates.IMPresenceFilter;
import com.raverun.im.shared.rest.BuddyHolder;
import com.raverun.im.shared.rest.MeHolder;
import com.raverun.im.shared.rest.MultivaluedMeToBuddyMap;
import com.raverun.shared.Constraint;

public class BucketOfOnlinePresences
{
    public static class Builder
    {
        public Builder( IMTypeTransportMapper imtypeTranslator )
        {
            builderMap = new MultivaluedMeToBuddyMap();
            builderImtypeTranslator = imtypeTranslator;
//            builderSeenBuddies = new HashSet<BuddyHolder>(8);
        }

        /**
         * 20090921 Previously we would discard duplicate {@code BuddyHolder} but
         * we dropped that since {@link IMPresenceFilter} does that job externally.
         */
        public Builder addOnline( IMPresence presence )
        {
            Constraint.NonNullArgument.check( presence, "presence" );
            if( presence.type() != PresenceChangeType.ONLINE )
                return this;

            MeHolder key = new MeHolder( presence.receiver().imId(), 
                builderImtypeTranslator.parse( presence.receiver().transport() ) );

            BuddyHolder buddy = new BuddyHolder( presence.sender(), presence.mode().code(), presence.status() );

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

        public BucketOfOnlinePresences build()
        {
            BucketOfOnlinePresences retval = new BucketOfOnlinePresences();
            retval._map.putAll( builderMap );

            return retval;
        }

        private final MultivaluedMeToBuddyMap builderMap;
        private final IMTypeTransportMapper builderImtypeTranslator;
        //private final Set<BuddyHolder> builderSeenBuddies;
    }

    private BucketOfOnlinePresences()
    {
        _map = new MultivaluedMeToBuddyMap();
    }

    /**
     * @return a {@code JSONObject} if JSON
     * @throws BucketSerializationException for any problems with the data
     */
    public Object serializeTo( MediaType outType, CacheOfNickNames cacheNickNames, String userid )
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
                    List<BuddyHolder> buddies = _map.get( me );
                    for( BuddyHolder buddy : buddies )
                    {
                        String nickName = cacheNickNames.read( userid, me.id, buddy.id, me.imtype );
                        JSONObject buddyJson = buddy.toJson( nickName );
                        buddyArray.put( buddyJson );
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

    private final MultivaluedMeToBuddyMap _map;
    private static final String JSON_KEY_NAME = "online";
    private static final String JSON_KEY_ME = "me";
    private static final String JSON_KEY_BUDDIES = "buddies";
    private static final String JSON_KEY_ROOT = "list";
}
