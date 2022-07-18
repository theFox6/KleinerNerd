package com.theFox6.kleinerNerd.commands;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CommandManager {
    //commandDefinition, setup
    private final Map<CommandData, Consumer<Command>> setupDefinitions = new ConcurrentHashMap<>();
    //commandName, handler
    private final Map<String, Consumer<SlashCommandInteractionEvent>> handlers = new ConcurrentHashMap<>();
    //perhaps make this more generic with ModRoleChangeListener

    public void registerCommand(CommandData def, Consumer<SlashCommandInteractionEvent> handler, Consumer<Command> setup) {
        String commandName = def.getName();
        QueuedLog.info("registered command /" + commandName);
        setupDefinitions.put(def, setup);
        if (handlers.put(commandName, handler) != null)
            QueuedLog.warning("Overriding handler for command /" + commandName);
    }

    /**
     * registers a command using certain permissions as defaults
     * @param def the command definition
     * @param handler the handler for command events
     * @param perms the default permissions
     * @deprecated replace by setDefaultPermissions in command definition
     */
    @Deprecated
    public void registerCommand(CommandData def, Consumer<SlashCommandInteractionEvent> handler, PermissionType perms) {
        switch (perms) {
            case FREE_FOR_ALL:
                registerCommand(def, handler);
                break;
            case SERVER_OWNER_ONLY:
                def.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
                registerCommand(def, handler);
                break;
            case MOD_ONLY:
                def.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
                registerCommand(def, handler);
                break;
            default:
                throw new IllegalArgumentException("Unknown permission type: " + perms);
        }
    }

    public void registerCommand(CommandData def, Consumer<SlashCommandInteractionEvent> handler) {
        registerCommand(def, handler, (c) -> {});
    }

    public void upsertCommands(JDA jda) {
        setupDefinitions.forEach(
                (d, s) -> jda.upsertCommand(d).queue(s,
                        (e) -> QueuedLog.error("error while trying to register command /" + d.getName(), e.getCause())
                )
        );
    }

    @SubscribeEvent
    public void onSlashCommand(SlashCommandInteractionEvent ev) {
        String commandName = ev.getName();
        if (handlers.containsKey(commandName))
            handlers.get(commandName).accept(ev);
        else
            QueuedLog.debug("No handler found for /" + commandName);
    }

}
