package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.MultiActionHandler;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class PollListener {
    public void setupCommands(CommandManager cm) {
        cm.registerCommand(Commands.slash("thumbpoll", "ertellt einfache Abstimmungen").addSubcommands(
                new SubcommandData("letzte", "reagiert mit Daumen hoch und runter unter der letzten Nachricht"),
                new SubcommandData("hier", "reagiert mit Daumen hoch und runter auf die Nachricht mit der gegebenen ID in diesem Kanal")
                        .addOption(OptionType.STRING, "nachrichtenid", "die ID der Nachricht", true),
                new SubcommandData("in-kanal", "reagiert mit Daumen hoch und runter auf die Nachricht mit der gegebenen ID im angegebenen Kanal")
                        .addOption(OptionType.CHANNEL, "kanal", "der Kanal in dem die Nachricht ist", true)
                        .addOption(OptionType.STRING, "nachrichtenid", "die ID der Nachricht", true)
                ), this::onPollCommand
        );
    }

    public void onPollCommand(SlashCommandInteractionEvent ev) {
        String subcommand = ev.getSubcommandName();
        if (subcommand == null) {
            QueuedLog.error("Thumbpoll without subcommand " + ev.getCommandPath());
            return;
        }
        switch (subcommand) {
            case "letzte":
                addThumbpollToLast(ev);
                break;
            case "hier":
                try {
                    addThumbpoll(ev, ev.getChannel(), KNHelpers.getOptionMapping(ev, "nachrichtenid").getAsString());
                } catch (OptionNotFoundException e) {
                    QueuedLog.error("could not get message ID option", e);
                    ev.reply("konnte option nicht extrahieren").setEphemeral(true).queue();
                }
            case "in-kanal":
                try {
                    addThumbpoll(ev, KNHelpers.getOptionMapping(ev, "kanal").getAsMessageChannel(), KNHelpers.getOptionMapping(ev, "nachrichtenid").getAsString());
                } catch (OptionNotFoundException e) {
                    QueuedLog.error("could not get option " + e.getOption(), e);
                    ev.reply("konnte option nicht extrahieren").setEphemeral(true).queue();
                }
                break;
            default:
                QueuedLog.error("unexpected subcommand to daumenabstimmung " + ev.getSubcommandName());
                break;
        }
    }

    private void addThumbpoll(SlashCommandInteractionEvent ev, MessageChannel chan, String msgId) {
        if (chan == null) {
            QueuedLog.error("got no MessageChannel for thumbpoll");
            ev.reply("konnte Kanal nicht finden").setEphemeral(true).queue();
            return;
        }
        MultiActionHandler<? super Void> poll = new MultiActionHandler<>(2,
                (s) -> ev.reply("Daumenabstimmung erstellt").setEphemeral(true).queue(),
                (e) -> QueuedLog.error("Could not create thumb poll", e)
        );
        try {
            chan.addReactionById(msgId, Emoji.fromUnicode("U+1F44D")).queue(poll::success, poll::failure);
            chan.addReactionById(msgId, Emoji.fromUnicode("U+1F44E")).queue(poll::success, poll::failure);
        } catch (NumberFormatException e) {
            QueuedLog.info("bad message id for thumbpoll " + msgId);
            ev.reply("Keine valide Nachrichten ID").setEphemeral(true).queue();
        }
    }

    private void addThumbpollToLast(SlashCommandInteractionEvent ev) {
        MessageChannel chan = ev.getChannel();
        MessageHistory h = chan.getHistory();
        h.retrievePast(1).queue((msgs) -> {
            MultiActionHandler<? super Void> poll = new MultiActionHandler<>(2,
                    (s) -> ev.reply("Daumenabstimmung erstellt").setEphemeral(true).queue(),
                    (e) -> {
                        QueuedLog.error("Could not create thumb poll", e);
                        ev.reply("Konnte Daumenabstimmung nicht erstellen: " + e.getMessage()).setEphemeral(true).queue();
                    }
            );
            Message msg = msgs.get(0);
            msg.addReaction(Emoji.fromUnicode("U+1F44D")).queue(poll::success, poll::failure);
            msg.addReaction(Emoji.fromUnicode("U+1F44E")).queue(poll::success, poll::failure);
        }, (e) -> {
            QueuedLog.error("could not retrieve MessageHistory for thumbpoll",e);
            ev.reply("Konnte letzte Nachricht nicht ermitteln.").setEphemeral(true).queue();
        });
    }
}
