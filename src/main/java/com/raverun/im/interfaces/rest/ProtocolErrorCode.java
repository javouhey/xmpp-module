package com.raverun.im.interfaces.rest;

public enum ProtocolErrorCode
{
    InvalidAcceptHttpHeader, InvalidResource, NotImplemented, InvalidContentType, InvalidURI, 
    UserAlreadyExists, MissingContentLength, MissingRequestBodyError, IncompleteBody, RequestTimeout,
    MethodNotAllowed, InternalError, MalformedXML, MalformedJSON, CommunicationError, UnderConstruction,
    NoSuchUser, InvalidIMSession, MissingIMSession, UserNotSpecified, IMAccountAlreadyExists,
    XMPPCommunicationError, IMAccountInvalidIdentifier, XMPPGatewayNotAvailable, InvalidCacheSetting,
    ConcurrentLoginDisallowed
}
