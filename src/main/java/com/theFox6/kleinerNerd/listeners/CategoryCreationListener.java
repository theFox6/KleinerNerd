package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.MultiActionHandler;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CategoryCreationListener {
    public void setupCommands(CommandManager cm) {
        cm.registerCommand(
                Commands.slash("create-category", "erstellt eine neue Kategorie mit einem Text und einem Voice Kanal")
                        .addOption(OptionType.STRING, "kategoriename", "der Name für den die Kategorie", true)
                        .addOption(OptionType.STRING, "rollenname", "der Name der Rolle, die Zugriff auf die Kategorie haben soll", false)
                        .addOption(OptionType.STRING, "textkanalname", "der Name für den Textkanal", false)
                        .addOption(OptionType.STRING, "voicekanalname", "der Name für den Sprachkanal", false)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                this::onCCCommand
        );

    }

    public void onCCCommand(SlashCommandInteractionEvent ev) {
        if (!ev.isFromGuild()) //not supported
            return; //TODO: warn
        Guild g = ev.getGuild();
        if (g == null) {
            QueuedLog.error("Create-category command from guild without guild information.");
            return;
        }
        try {
            String catName = KNHelpers.getOptionMapping(ev, "kategoriename").getAsString();
            if (catName.isEmpty())
                return; //TODO: warn
            ev.getInteraction().deferReply().queue((ih) -> {
                if (g.getCategoriesByName(catName, true).size() > 0) {
                    QueuedLog.warning("Category already exists, aborting.");
                    ev.getInteraction().reply("Kategorie existiert bereits. Rekonfiguration noch nicht implementiert.")
                            .setEphemeral(true).queue();
                    return;
                }
                g.createCategory(catName).queue((cat) -> {
                    MultiActionHandler<Void> categorySetup = new MultiActionHandler<>(3,
                            (s) -> ih.sendMessage("Neue Kategorie erstellt.").queue(),
                            (e) -> QueuedLog.error("Error while creating category", e)
                    );
                    OptionMapping rolename = ev.getOption("rollenname");
                    if (rolename == null || rolename.getAsString().isEmpty())
                        categorySetup.success();
                    else {
                        if (g.getRolesByName(rolename.getAsString(), true).size() > 0) {
                            QueuedLog.warning("Role already exists, aborting.");
                            ev.getInteraction().reply("Rolle existiert bereits. Verwendung existierender Rollen noch nicht implementiert.")
                                    .setEphemeral(true).queue();
                        } else {
                            g.createRole().setName(rolename.getAsString()).queue((role) -> {
                                cat.upsertPermissionOverride(role).grant(Permission.VIEW_CHANNEL).queue(
                                        (s) -> cat.upsertPermissionOverride(g.getPublicRole()).deny(Permission.VIEW_CHANNEL)
                                                .queue(categorySetup::success)
                                );
                                OptionMapping voiceName = ev.getOption("voicekanalname");
                                if (voiceName == null || voiceName.getAsString().isEmpty())
                                    categorySetup.success();
                                else {
                                    cat.createVoiceChannel(voiceName.getAsString()).queue((vc) -> {
                                        vc.upsertPermissionOverride(role).grant(Permission.VIEW_CHANNEL).queue(
                                                (s) -> vc.upsertPermissionOverride(g.getPublicRole()).deny(Permission.VIEW_CHANNEL)
                                                        .queue(categorySetup::success)
                                        );
                                        categorySetup.success();
                                    });
                                }
                            });
                        }
                    }
                    OptionMapping textName = ev.getOption("textkanalname");
                    if (textName == null || textName.getAsString().isEmpty())
                        categorySetup.success();
                    else {
                        cat.createTextChannel(textName.getAsString()).queue(categorySetup::success);
                    }
                });
            });
        } catch (OptionNotFoundException e) {
            QueuedLog.error("could not extract create-category add option",e);
            ev.reply("konnte " + e.getOption() + " option nicht extrahieren").setEphemeral(true).queue();
        }

    }
}
