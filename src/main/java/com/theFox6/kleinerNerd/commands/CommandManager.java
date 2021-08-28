package com.theFox6.kleinerNerd.commands;

import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CommandManager implements ModRoleChangeListener {
    //commandDefinition, setup
    private final Map<CommandData, Consumer<Command>> setupDefinitions = new ConcurrentHashMap<>();
    //commandName, handler
    private final Map<String, Consumer<SlashCommandEvent>> handlers = new ConcurrentHashMap<>();
    //perhaps make this more generic with ModRoleChangeListener
    private final Set<String> modOnlyCommandIDs = ConcurrentHashMap.newKeySet();

    public void registerModOnlyCommand(CommandData def, Consumer<SlashCommandEvent> handler) {
        registerCommand(def, handler, (c) -> {
            makeModOnly(c);
            modOnlyCommandIDs.add(c.getId());
        });
    }

    public void registerCommand(CommandData def, Consumer<SlashCommandEvent> handler, Consumer<Command> setup) {
        String commandName = def.getName();
        QueuedLog.info("registered command /" + commandName);
        setupDefinitions.put(def, setup);
        if (handlers.put(commandName, handler) != null)
            QueuedLog.warning("Overriding handler for command /" + commandName);
    }

    public void registerCommand(CommandData def, Consumer<SlashCommandEvent> handler, PermissionType perms) {
        switch (perms) {
            case FREE_FOR_ALL:
                registerCommand(def, handler, (c) -> {});
                break;
            case SERVER_OWNER_ONLY:
                registerCommand(def, handler, CommandManager::makeServerOwnerOnly);
                break;
            case MOD_ONLY:
                registerModOnlyCommand(def, handler);
                break;
            default:
                throw new IllegalArgumentException("Unknown permission type: " + perms);
        }
    }

    public void registerCommand(CommandData def, Consumer<SlashCommandEvent> handler) {
        registerCommand(def, handler, PermissionType.FREE_FOR_ALL);
    }

    public void upsertCommands(JDA jda) {
        setupDefinitions.forEach(
                (d, s) -> jda.upsertCommand(d).queue(s,
                        (e) -> QueuedLog.error("error while trying to register command /" + d.getName(), e.getCause())
                )
        );
    }

    @SubscribeEvent
    public void onSlashCommand(SlashCommandEvent ev) {
        String commandName = ev.getName();
        if (handlers.containsKey(commandName))
            handlers.get(commandName).accept(ev);
        else
            QueuedLog.debug("No handler found for /" + commandName);
    }

    @Override
    public void onModRoleChange(Guild g, Role r) {
        modOnlyCommandIDs.forEach(
                (cid) -> g.getJDA().retrieveCommandById(cid).queue(
                        (c) -> makeServerOwnerAndRoleOnly(c, g, r),
                        (e) -> QueuedLog.error("could not retrieve mod only command on modrole change", e.getCause())
                )
        );
    }

    public static void makeServerOwnerOnly(Command c) {
        c.getJDA().getGuilds().forEach((g) -> makeServerOwnerOnly(c,g));
    }

    public static void makeServerOwnerOnly(Command c, Guild g) {
        c.updatePrivileges(g, CommandPrivilege.enableUser(g.getOwnerId())).queue((p) -> {}, (e) -> {
            if (e.getMessage().equals("50001: Missing Access")) {
                QueuedLog.debug("missing slash command access in guild " + g.getName());
                ModLog.sendToGuildOnce(g, "privileges missing access", "Mir fehlt die Berechtigung Privilegien für Slash Befehle einzurichten.\n" +
                        "Ich muss noch mal neu eingeladen werden um das zu beheben.");
            } else
                QueuedLog.error("Error while trying to make " + c.getName() + " command owner only in guild " + g.getName(), e);
        });
    }

    public static void makeModOnly(Command c) {
        c.getJDA().getGuilds().forEach((g) -> makeModOnly(c,g));
    }

    public static void makeModOnly(Command c, Guild g) {
        makeServerOwnerAndRoleOnly(c,g, GuildStorage.getModRole(g));
    }

    public static void makeServerOwnerAndRoleOnly(Command c, Guild g, Role r) {
        List<CommandPrivilege> privileges = new LinkedList<>();
        privileges.add(CommandPrivilege.enableUser(g.getOwnerId()));
        if (r != null)
            privileges.add(CommandPrivilege.enableRole(r.getId()));
        c.updatePrivileges(g, privileges).queue((p) -> {}, (e) -> {
            if (e.getMessage().equals("50001: Missing Access")) {
                QueuedLog.debug("missing slash command access in guild " + g.getName());
                ModLog.sendToGuildOnce(g, "privileges missing access", "Mir fehlt die Berechtigung Privilegien für Slash Befehle einzurichten.\n" +
                        "Ich muss noch mal neu eingeladen werden um das zu beheben.");
            } else
                QueuedLog.error("Error while trying to make " + c.getName() + " command role and owner only in guild " + g.getName(), e);
        });
    }
}
