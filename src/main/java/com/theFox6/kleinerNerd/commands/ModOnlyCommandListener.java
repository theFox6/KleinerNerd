package com.theFox6.kleinerNerd.commands;

import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

//perhaps add a ModRoleChangeListener interface
public abstract class ModOnlyCommandListener implements CommandListener {
    private final Set<String> commands = new ConcurrentSkipListSet<>();

    protected void addModOnlyCommand(Command c) {
        commands.add(c.getId());
        c.getJDA().getGuilds().forEach((g) -> makeModOnly(c, g));
    }

    public void onModroleChange(Guild g, Role r) {
        commands.forEach((cid) -> g.getJDA().retrieveCommandById(cid).queue(
                (c) -> makeRoleAndOwnerOnly(c, g, r),
                (e) -> QueuedLog.error("could not retrieve modrole command", e)
        ));
    }

    @SubscribeEvent
    public void onGuildJoin(GuildJoinEvent ev) {
        Guild g = ev.getGuild();
        commands.forEach((cid) -> g.getJDA().retrieveCommandById(cid).queue(
                (c) -> makeModOnly(c, g),
                (e) -> QueuedLog.error("could not retrieve modrole command", e)
        ));
    }

    public static void makeModOnly(Command c, Guild g) {
        makeRoleAndOwnerOnly(c,g,GuildStorage.getModRole(g));
    }

    public static void makeRoleAndOwnerOnly(Command c, Guild g, Role r) {
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

    public static void makeOwnerOnly(Command c) {
        c.getJDA().getGuilds().forEach((g) -> makeOwnerOnly(c,g));
    }

    public static void makeOwnerOnly(Command c, Guild g) {
        c.updatePrivileges(g, CommandPrivilege.enableUser(g.getOwnerId())).queue((p) -> {}, (e) -> {
            if (e.getMessage().equals("50001: Missing Access")) {
                QueuedLog.debug("missing slash command access in guild " + g.getName());
                ModLog.sendToGuildOnce(g, "privileges missing access", "Mir fehlt die Berechtigung Privilegien für Slash Befehle einzurichten.\n" +
                        "Ich muss noch mal neu eingeladen werden um das zu beheben.");
            } else
                QueuedLog.error("Error while trying to make " + c.getName() + " command owner only in guild " + g.getName(), e);
        });
    }
}
