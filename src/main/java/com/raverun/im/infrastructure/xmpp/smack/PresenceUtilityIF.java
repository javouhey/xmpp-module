package com.raverun.im.infrastructure.xmpp.smack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jivesoftware.smack.packet.Presence;

public interface PresenceUtilityIF
{
    /**
     * Adapted from {@code IMManager#setStatus(..)}
     * <p>The mapping are as follows:
     * <ul>
     * <li><code>"1"</code> maps to an unavailable Presence
     * <li><code>"2"</code> maps to an available Presence (dnd)
     * <li><code>"3"</code> maps to an available Presence (away)
     * <li><code>anyString</code> maps to an available Presence
     * </ul>
     *
     * @param mymode - ranges from the set { "1", "2", "3", anyString }
     * @return Presence with {@code to} missing.
     */
    @Nonnull Presence derefClient( @Nullable String mymode, @Nullable String status );

    @Nonnull Presence derefClient( @Nonnull MyMode mymode, @Nullable String status );

    /**
     * Use this method only if you know that the {@code Presence.Type == available}
     *
     * @param mode - either one of {@code available, away or dnd}
     * @throws AssertionError if anything else if passed in
     */
    @Nonnull MyMode derefXmpp( @Nonnull Presence.Mode mode );

    /**
     * Our mobile client's translation of XMPP's {@code Presence.Mode}s
     */
    public static enum MyMode
    {
        UNAVAILABLE(1), DO_NOT_DISTURB(2), AWAY(3), AVAILABLE(4);

        MyMode( int code )
        {
            _code = code;
        }

        public int code()
        {
            return _code;
        }

        public static MyMode deref( int code )
        {
            switch( code )
            {
            case 1:
                return MyMode.UNAVAILABLE;
            case 2:
                return MyMode.DO_NOT_DISTURB;
            case 3:
                return MyMode.AWAY;
            case 4:
                return MyMode.AVAILABLE;
            default:
                throw new IllegalArgumentException( "mode belongs only to set [1,4] " );
            }
        }

        private final int _code;
    }
}
