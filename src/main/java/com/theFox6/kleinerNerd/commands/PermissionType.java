package com.theFox6.kleinerNerd.commands;

/**
 * Standard permission types for commands that make registration easier
 */
public enum PermissionType {
    /* permissions are guild specific so this is disabled for now
     * command limited to people listed in owners config file
     BOT_OWNER_ONLY,
     */
    /**
     * only available for the owner of a server on their server
     * makes the command only available in guilds
     */
    SERVER_OWNER_ONLY,
    /**
     * only available for the owner of a server on their server and for the members of the modrole of that server
     * makes the command only available in guilds
     */
    MOD_ONLY,
    /**
     * makes the command available for anyone, anywhere
     */
    FREE_FOR_ALL
}
