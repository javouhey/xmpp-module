package com.raverun.im.interfaces.rest.impl.resources;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.domain.IMMessageChat;
import com.raverun.im.shared.rest.BuddyChatHolder;
import com.raverun.im.shared.rest.MeHolder;
import com.raverun.im.shared.rest.MultivaluedMeToBuddyChatMap;
import com.raverun.shared.Constraint;

public class BucketOfChatMessages
{
    public static class Builder
    {
        public Builder( IMTypeTransportMapper imtypeTranslator )
        {
            builderMap = new MultivaluedMeToBuddyChatMap();
            builderImtypeTranslator = imtypeTranslator;
        }

        public Builder add( IMMessageChat chat )
        {
            Constraint.NonNullArgument.check( chat, "chat" );

            MeHolder key = new MeHolder( chat.receiver(), 
                builderImtypeTranslator.parse( chat.transport() ) );

            builderMap.add( key, new BuddyChatHolder( chat.sender(), chat.message() ) );
            return this;
        }

        public BucketOfChatMessages build()
        {
            BucketOfChatMessages retval = new BucketOfChatMessages();
            retval._map.putAll( builderMap );
            return retval;
        }

        private final MultivaluedMeToBuddyChatMap builderMap;
        private final IMTypeTransportMapper builderImtypeTranslator;
    }

    private BucketOfChatMessages()
    {
        _map = new MultivaluedMeToBuddyChatMap();
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
                    List<BuddyChatHolder> buddies = _map.get( me );
                    for( BuddyChatHolder buddy : buddies )
                    {
                        buddyArray.put( buddy.toJson() );
                    }
                    obj.put( JSON_KEY_MSGFROM, buddyArray );
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

    private final MultivaluedMeToBuddyChatMap _map;
    private static final String JSON_KEY_NAME = "chats";
    private static final String JSON_KEY_ME = "me";
    private static final String JSON_KEY_ROOT = "list";
    private static final String JSON_KEY_MSGFROM = "msgFrom";
}
