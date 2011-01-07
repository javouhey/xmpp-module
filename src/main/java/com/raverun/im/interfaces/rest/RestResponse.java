package com.raverun.im.interfaces.rest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

/**
 * Knows about the details of the target serialization response
 * <ul>
 * <li>what content type
 * <li>what HTTP status code
 * </ul>
 * <p>
 * Responsibilities:
 * <ul>
 * <li>can serialize
 * </ul>
 * <p>
 * This is <b>not</b> a Guice-managed entity
 *
 * @author Gavin Bong
 */
public interface RestResponse extends RestSerializable
{
    int httpStatusCode();

    String httpErrorMessage();

    boolean isError();

    /*
     * (non-Javadoc)
     * @see com.raverun.im.interfaces.rest.RestSerializable#serialize()
     */
    String serialize();

    MediaType type();

    boolean hasBody();

    void spitOutHttpHeaders( HttpServletResponse httpResponse  );
}
