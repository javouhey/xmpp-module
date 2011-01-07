package com.raverun.im.interfaces.rest.impl.resources;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.domain.IMOffline;
import com.raverun.im.shared.rest.MeHolder;

public class BucketOfOfflineAccounts
{
    public static class Builder
    {
        public Builder( IMTypeTransportMapper imtypeTranslator )
        {
            builderImtypeTranslator = imtypeTranslator;
            builderMeHolders = new LinkedList<MeHolder>();
        }

        public Builder add( IMOffline offline )
        {
            if( offline == null )
                return this;

            MeHolder me = new MeHolder( offline.receiver().imId(), 
                builderImtypeTranslator.parse( offline.receiver().transport() ) );

            synchronized( builderMeHolders )
            {
                if( ! builderMeHolders.contains( me ) )
                    builderMeHolders.add( me );
            }

            return this;
        }

        public BucketOfOfflineAccounts build()
        {
            BucketOfOfflineAccounts retval = new BucketOfOfflineAccounts();
            retval._list.addAll( builderMeHolders );
            return retval;
        }

        private final List<MeHolder> builderMeHolders;
        private final IMTypeTransportMapper builderImtypeTranslator;
    }

    private BucketOfOfflineAccounts()
    {
        _list = new LinkedList<MeHolder>();
    }

    public Object serializeTo( MediaType outType )
    {
        try
        {
            if( outType == MediaType.APPLICATION_JSON_TYPE )
            {
                JSONArray array = new JSONArray();

                for( MeHolder me : _list )
                {
                    JSONObject meJson = me.toJson();
                    array.put(  meJson );
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

    private final List<MeHolder> _list;
    public static final String JSON_KEY_NAME = "dropped";
    private static final String JSON_KEY_ROOT = "list";
}
