package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import com.theFox6.kleinerNerd.commands.PermissionType;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class CategoryCreationListener {
    public CategoryCreationListener setupCommands(CommandManager cm) {
        cm.registerCommand(
                new CommandData("create-category", "erstellt eine neue Kategorie mit einem Text und einem Voice Kanal")
                        .addOption(OptionType.STRING, "kategoriename", "der Name für den die Kategorie", true)
                        .addOption(OptionType.STRING, "rollenname", "der Name der Rolle, die Zugriff auf die Kategorie haben soll", false)
                        .addOption(OptionType.STRING, "textkanalname", "der Name für den Textkanal", false)
                        .addOption(OptionType.STRING, "voicekanalname", "der Name für den Sprachkanal", false)
                        .setDefaultEnabled(false),
                this::onCCCommand,
                PermissionType.MOD_ONLY
        );

        return this;
    }

    public void onCCCommand(SlashCommandEvent ev) {
        if (!ev.isFromGuild()) //not supported
            return; //TODO: warn
        Guild g = ev.getGuild();
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
                    //TODO: check if role exists
                    g.createRole().setName(rolename.getAsString()).queue((role) -> {
                        cat.putPermissionOverride(role).grant(Permission.VIEW_CHANNEL).queue(
                                (s) -> cat.putPermissionOverride(g.getPublicRole()).deny(Permission.VIEW_CHANNEL).queue()
                        );
                    });
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
}
