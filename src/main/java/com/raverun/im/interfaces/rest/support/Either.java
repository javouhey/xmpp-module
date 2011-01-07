package com.raverun.im.interfaces.rest.support;

import javax.ws.rs.core.MediaType;

import com.raverun.im.interfaces.rest.RestRequest;
import com.raverun.im.interfaces.rest.RestResponse;

public class Either
{
    private final boolean ok;
    private RestRequest req;
    private RestResponse resp;
    private MediaType outMimeType;

    public static class Builder
    {
        private boolean ok;
        private RestRequest req;
        private RestResponse resp;
        private MediaType outMimeType;
        
        public Builder( boolean ok, MediaType outMimeType )
        {
            this.ok = ok;
            this.outMimeType = outMimeType;
        }
        
        public Builder request( RestRequest req )
        {
            this.req = req;
            return this;
        }
        
        public Builder response( RestResponse resp )
        {
            this.resp = resp;
            return this;
        }
        
        public Either build()
        {
            Either result = new Either( this.ok, this.outMimeType );

            if( this.ok )
            {
                result.req = this.req;
            }
            else
            {
                result.resp = this.resp;
            }

            return result;
        }
    }
    
    
    private Either( boolean ok, MediaType outMimeType )
    {
        this.ok = ok;
        this.outMimeType = outMimeType;
    }

    public boolean isOk()
    {
        return ok;
    }

    public RestRequest getRequest()
    {
        return req;
    }
    
    public RestResponse getResponse()
    {
        return resp;
    }
    
    public MediaType getOutMediaType()
    {
        return outMimeType;
    }
}
