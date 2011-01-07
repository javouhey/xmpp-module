package com.raverun.im.interfaces.rest.impl.resources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.common.Transport;
import com.raverun.im.domain.IMSubscriptionRequest;
import com.raverun.im.shared.rest.MeHolder;
import com.raverun.im.shared.rest.MultivaluedMeToStringMap;
import com.raverun.shared.Common;
import com.raverun.shared.Constraint;

@NotThreadSafe
public class BucketOfSubscriptionRequests
{
    public static class Builder
    {
        public Builder( IMTypeTransportMapper imtypeTranslator )
        {
            builderMap = new MultivaluedMeToStringMap();
            builderImtypeTranslator = imtypeTranslator;
            builderSeenSubscribers = new HashSet<String>(8);
        }

        public Builder add( IMSubscriptionRequest request )
        {
            if( request == null )
                return this;

            String flattenRequest = flatten( request.receiver(), request.sender(), request.transport() );
            if( builderSeenSubscribers.contains( flattenRequest ) )
                return this;

            MeHolder me = new MeHolder( request.receiver(), builderImtypeTranslator.parse( request.transport() ) );
            builderMap.add( me, request.sender() );

            builderSeenSubscribers.add( flattenRequest );
            return this;
        }

        public BucketOfSubscriptionRequests build()
        {
            BucketOfSubscriptionRequests retval = new BucketOfSubscriptionRequests();
            retval._map.putAll( builderMap );

            return retval;
        }

        private final String flatten( String receiver, String requestor, Transport transport )
        {
            String canonicalReceiver = Common.EMPTY_STRING;
            if( !Constraint.EmptyString.isFulfilledBy( receiver ) )
                canonicalReceiver = receiver.trim().toLowerCase();

            String canonicalRequestor = Common.EMPTY_STRING;
            if( !Constraint.EmptyString.isFulfilledBy( requestor ) )
                canonicalRequestor = requestor.trim().toLowerCase();

            StringBuilder b = new StringBuilder();
            b.append( canonicalReceiver ).append( canonicalRequestor )
             .append( builderImtypeTranslator.parse( transport ) );
            return b.toString();
        }

        private final MultivaluedMeToStringMap builderMap;
        private final IMTypeTransportMapper builderImtypeTranslator;

        /**
         * Store the strings as a concatenation of id & {@code imtype} & targetid
         */
        private final Set<String> builderSeenSubscribers;
    }

    public String keyName()
    {
        return JSON_KEY_NAME;
    }

    private BucketOfSubscriptionRequests()
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

                    JSONArray subscriberArray = new JSONArray();
                    List<String> subscribers = _map.get( me );
                    for( String buddy : subscribers )
                    {
                        subscriberArray.put( buddy );
                    }
                    obj.put( JSON_KEY_SUBSCRIBERS, subscriberArray );
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

    public static final String JSON_KEY_NAME = "subscriptions";
    private static final String JSON_KEY_ME = "me";
    private static final String JSON_KEY_SUBSCRIBERS = "subscribers";
    private static final String JSON_KEY_ROOT = "list";

    private final MultivaluedMeToStringMap _map;
}
