package com.raverun.im.infrastructure.xmpp.smack;

import org.jivesoftware.smack.packet.PacketExtension;

import com.raverun.shared.Common;

public class GatewayDeregisterPacketExtension implements PacketExtension
{

    @Override
    public String getElementName()
    {
        return "remove";
    }

    @Override
    public String getNamespace()
    {
        return Common.EMPTY_STRING;
    }

    @Override
    public String toXML()
    {
        return "<remove/>" ;
    }

}
