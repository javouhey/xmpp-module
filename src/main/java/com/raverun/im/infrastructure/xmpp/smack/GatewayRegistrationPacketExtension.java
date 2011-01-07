package com.raverun.im.infrastructure.xmpp.smack;

import org.jivesoftware.smack.packet.PacketExtension;

public class GatewayRegistrationPacketExtension implements PacketExtension
{

    @Override
    public String getElementName()
    {
        return ELEMENT;
    }

    @Override
    public String getNamespace()
    {
        return NAMESPACE;
    }

    @Override
    public String toXML()
    {
        return XML;
    }

    private final static String ELEMENT = "foo"; // "x" may clash
    private final static String NAMESPACE = "jabber:iq:gateway:register";
    private final static String XML = "<x xmlns=\"jabber:iq:gateway:register\" />";
}
