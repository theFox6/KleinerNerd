package com.theFox6.kleinerNerd.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * a Json Serializable form of a guild's settings
 */
public class GuildSettings {
    /**
     * the Id of a channel where log messages will be sent for moderators
     */
    @JsonProperty
    public String modLogChannel;

    /**
     * the Id of a role that can use moderator commands
     */
    @JsonProperty
    public String moderatorRole;
}
