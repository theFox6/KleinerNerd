package com.theFox6.kleinerNerd.roles;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleCommandListener {
    public RoleCommandListener setupCommands(CommandManager cm) {
        //perhaps also allow this in DMs
        cm.registerCommand(Commands.slash("role", "suche Rollen aus").setGuildOnly(true), this::onGetRole);
        cm.registerCommand(Commands.slash("addrole", "füge eine Rolle hinzu, die normale Benutzer holen können")
                .addOption(OptionType.STRING, "kategorie", "die Kategorie die die Rolle angehört", true)
                .addOption(OptionType.ROLE, "rolle", "die Rolle, die man bekommen können soll", true)
                //.setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES)
                ), this::onAddRole);
        //TODO: remove role
        return this;
    }

    public void onAddRole(SlashCommandInteractionEvent ev) {
        Role role;
        String category;
        try {
            role = KNHelpers.getOptionMapping(ev, "rolle").getAsRole();
            category = KNHelpers.getOptionMapping(ev, "kategorie").getAsString();
        } catch (OptionNotFoundException ex) {
            QueuedLog.error("required option not found for addrole command", ex);
            ev.reply("Fehler bei der Verarbeitung der Argumente").setEphemeral(true).queue();
            return;
        }
        GetRoleStorage.addRole(role.getGuild().getId(), category,role.getId());
        ev.reply(role.getName() + " kann man jetzt per Befehl bekommen.").queue();
    }

    public void onGetRole(SlashCommandInteractionEvent ev) {
        String gid = ev.getGuild().getId();
        StringSelectMenu.Builder menu = StringSelectMenu.create("addrole:categories:"+gid)
                        .setPlaceholder("Kategorie") // shows the placeholder indicating what this menu is for
                        .setRequiredRange(1,1);
        //TODO: make sure it's only 25 options
        Map<String, Set<String>> rolesByCategory = GetRoleStorage.getRolesByCategory(gid);
        if (rolesByCategory == null) {
            ev.reply("Hier gibt es keine Rollen, die man per Befehl bekommen kann.")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        rolesByCategory.keySet().forEach((c) -> menu.addOption(c,c));

        ev.reply("Aus welcher Kategorie soll die Rolle sein?")
            .setEphemeral(true)
            .addActionRow(menu.build())
            .queue();
    }

    @SubscribeEvent
    public void onMenuSelection(StringSelectInteractionEvent ev) {
        String id = ev.getInteraction().getComponentId();
        if (!id.startsWith("addrole:"))
            return;
        if (id.startsWith("addrole:categories:")) {
            sendRoleMenu(ev);
        } else if (id.startsWith("addrole:category:")) {
            Member m = ev.getMember();
            Set<String> owned = m.getRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());
            Guild g = ev.getGuild();
            if (g == null) {
                QueuedLog.error("/role interaction outside of guild");
                return;
            }
            Set<String> selected = ev.getInteraction().getSelectedOptions().stream()
                    .map(SelectOption::getValue)
                    .collect(Collectors.toSet());
            Set<Role> toAdd = selected.stream()
                    .filter((r) -> !owned.contains(r))
                    .map(g::getRoleById)
                    .collect(Collectors.toSet());
            Set<Role> toRemove = ev.getInteraction().getSelectMenu().getOptions().stream()
                    .map(SelectOption::getValue)
                    .filter(owned::contains)
                    .filter((r) -> !selected.contains(r))
                    .map(g::getRoleById)
                    .collect(Collectors.toSet());
            g.modifyMemberRoles(m, toAdd, toRemove).reason("/role command used").queue(
                    (s) -> ev.reply("rollen wurden geändert").setEphemeral(true).queue(),
                    (e) -> {
                        QueuedLog.error("error while trying to modify roles with /role command", e);
                        ev.reply("Fehler bei der Rollenänderung: " + e.getMessage()).setEphemeral(true).queue();
                    });
        } else {
            QueuedLog.warning("unknown addrole interaction \"" + id + "\"");
        }
    }

    public void sendRoleMenu(StringSelectInteractionEvent ev) {
        Guild g = ev.getGuild();
        if (g == null) {
            QueuedLog.error("role interaction outside guild");
            return;
        }
        String category = ev.getInteraction().getSelectedOptions().get(0).getValue();
        Map<String, Set<String>> roles = GetRoleStorage.getRolesByCategory(g.getId());
        if (roles == null) {
            ev.reply("In der Kategorie scheint es keine Rollen mehr zu geben.")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        //TODO: make sure it's only 25 options
        Set<SelectOption> options = roles.get(category).stream()
                .map((i) -> SelectOption.of(g.getRoleById(i).getName(), i))
                .collect(Collectors.toSet());
        Set<String> owned = ev.getMember().getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        StringSelectMenu.Builder menu = StringSelectMenu.create("addrole:category:"+category)
                .setPlaceholder("Rollen") // shows the placeholder indicating what this menu is for
                .addOptions(options)
                .setRequiredRange(0,options.size())
                .setDefaultOptions(options.stream().filter((o) -> owned.contains(o.getValue())).collect(Collectors.toSet()));

        ev.reply("Welche Rollen möchtest du haben?")
                .setEphemeral(true)
                .addActionRow(menu.build())
                .queue();
    }
}
