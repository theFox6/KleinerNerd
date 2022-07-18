package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.List;

public class CategoryCreationListener {
    public void setupCommands(CommandManager cm) {
        cm.registerCommand(
                Commands.slash("create-category", "erstellt eine neue Kategorie mit einem Text und einem Voice Kanal")
                        .addOption(OptionType.STRING, "kategoriename", "der Name für den die Kategorie", true)
                        .addOption(OptionType.STRING, "rollenname", "der Name der Rolle, die Zugriff auf die Kategorie haben soll", false)
                        .addOption(OptionType.STRING, "textkanalname", "der Name für den Textkanal", false)
                        .addOption(OptionType.STRING, "voicekanalname", "der Name für den Sprachkanal", false)
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                this::onCCCommand
        );

    }

    public void onCCCommand(SlashCommandInteractionEvent ev) {
        if (!ev.isFromGuild()) //not supported
            return; //TODO: warn
        Guild g = ev.getGuild();
        if (g == null) {
            QueuedLog.warning("Command from guild without guild.");
            return;
        }
        try {
            String catName = KNHelpers.getOptionMapping(ev, "kategoriename").getAsString();
            if (catName.isEmpty())
                return; //TODO: warn
            if (g.getCategoriesByName(catName, true).size() > 0) {
                QueuedLog.warning("Category already exists, aborting.");
                ev.getInteraction().reply("Kategorie existiert bereits. Rekonfiguration noch nicht implementiert.")
                        .setEphemeral(true).queue();
                return;
            }
            g.createCategory(catName).queue((cat) -> {
                OptionMapping rolename = ev.getOption("rollenname");
                if (rolename != null && !rolename.getAsString().isEmpty()) {
                    List<Role> existing = g.getRolesByName(rolename.getAsString(),true);
                    //perhaps ask whether to create one or use existing
                    if (existing.isEmpty()) {
                        g.createRole().setName(rolename.getAsString())
                                .queue((role) -> CategoryCreationListener.makeRoleExclusive(g, cat, role));
                    } else {
                        existing.forEach((r) -> CategoryCreationListener.makeRoleExclusive(g, cat, r));
                    }
                }
                OptionMapping textName = ev.getOption("textkanalname");
                if (textName != null && !textName.getAsString().isEmpty()) {
                    cat.createTextChannel(textName.getAsString()).queue();
                }
                OptionMapping voiceName = ev.getOption("voicekanalname");
                if (voiceName != null && !voiceName.getAsString().isEmpty()) {
                    cat.createTextChannel(voiceName.getAsString()).queue();
                }
            });
        } catch (OptionNotFoundException e) {
            QueuedLog.error("could not extract create-category add option",e);
            ev.reply("konnte " + e.getOption() + " option nicht extrahieren").setEphemeral(true).queue();
        }

    }

    private static void makeRoleExclusive(Guild g, Category category, Role role) {
        category.upsertPermissionOverride(role).grant(Permission.VIEW_CHANNEL).queue(
                (s) -> category.upsertPermissionOverride(g.getPublicRole()).deny(Permission.VIEW_CHANNEL).queue()
        );
    }
}
