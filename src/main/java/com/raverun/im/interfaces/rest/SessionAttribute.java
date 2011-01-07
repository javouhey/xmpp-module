package com.raverun.im.interfaces.rest;

/**
 * Session requirements for a HTTP Method
 * <ul>
 * <li>NEVER - no active session is required
 * <li>REQUIRES_NEW - a new session is required e.g. login use case
 * <li>REQUIRED - an active session is a MUST. Otherwise an error is thrown back to caller of method
 * <li>
 *
 * @author Gavin Bong
 */
public enum SessionAttribute
{
    NEVER, REQUIRES_NEW, REQUIRED
}
