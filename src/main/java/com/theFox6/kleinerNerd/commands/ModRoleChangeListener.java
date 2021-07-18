package com.theFox6.kleinerNerd.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

@FunctionalInterface
public interface ModRoleChangeListener {
    void onModRoleChange(Guild g, Role r);
}
