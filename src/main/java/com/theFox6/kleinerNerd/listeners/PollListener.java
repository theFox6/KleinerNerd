package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.MultiActionHandler;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.List;

public class PollListener {
    public static final String pollCommand = KleinerNerd.prefix + "thumbpoll";

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        String raw = msg.getContentRaw();
        MessageChannel chan = event.getChannel();
        if (raw.startsWith(pollCommand)) {
            if (raw.length() == pollCommand.length()) {
                chan.getHistoryBefore(msg.getId(),2).queue((h) -> {
                    List<Message> msgs = h.getRetrievedHistory();
                    int i = 0;
                    if (msgs.get(i).getId().equals(msg.getId()))
                        i++;
                    String id = msgs.get(i).getId();
                    MultiActionHandler<? super Void> poll = new MultiActionHandler<>(2, (s) -> {
                        //I don't really care but it causes errors to try
                        if (chan.getType() != ChannelType.PRIVATE)
                            msg.delete().reason("thumb poll command auto deletion").queue();
                    }, (e) -> QueuedLog.error("Could not create thumb poll", e));
                    try {
                        chan.addReactionById(id, "U+1F44D").queue(poll::success, poll::failure);
                        chan.addReactionById(id, "U+1F44E").queue(poll::success, poll::failure);
                    } catch (NumberFormatException e) {
                        QueuedLog.verbose("not an id");
                        chan.sendMessage("Keine valide Nachrichten ID").queue();
                    }
                },(e) -> {
                    QueuedLog.error("could not retrieve message history",e);
                    chan.sendMessage("Ich konnte die Nachrichtenhistorie vor der Nachricht nicht abfragen.\n" +
                            "Versuch doch mal die Nachrichten ID hinter den Befehl zu schreiben.").queue();
                });
            } else {
                int len = pollCommand.length() + 1;
                MultiActionHandler<? super Void> poll = new MultiActionHandler<>(2, (s) -> {
                    //Discord doesn't allow deleting other peoples messages in DMs
                    if (chan.getType() != ChannelType.PRIVATE)
                        msg.delete().reason("thumb poll command auto deletion").queue();
                }, (e) -> QueuedLog.error("Could not create thumb poll", e));
                try {
                    chan.addReactionById(raw.substring(len), "U+1F44D").queue(poll::success, poll::failure);
                    chan.addReactionById(raw.substring(len), "U+1F44E").queue(poll::success, poll::failure);
                } catch (NumberFormatException e) {
                    QueuedLog.verbose("not an id");
                    chan.sendMessage("Keine valide Nachrichten ID").queue();
                }
            }
        }
    }
}
