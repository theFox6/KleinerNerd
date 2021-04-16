package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.MultiActionHandler;
import com.theFox6.kleinerNerd.storage.ConfigFiles;

import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.deamon.ConditionNotifier.NotNotifiableException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.List;

public class SimpleCommandListener {
	public static final String pollcommand = KleinerNerd.prefix + "thumbpoll";

	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (!raw.startsWith(KleinerNerd.prefix))
    		return;
    	if (raw.equals(KleinerNerd.prefix + "ping")) {
            long time = System.currentTimeMillis();
            chan.sendMessage("Pong!\n(waiting for an answer from Discord)")
            .queue(response -> {
            	JDA jda = response.getJDA();
            	response.editMessageFormat("Pong!\n"
            			+ "sending time: %d ms\n"
            			+ "last heartbeat: %d ms",
						System.currentTimeMillis() - time,
						jda.getGatewayPing()
				).queue();
            });
    	} else if (raw.equals(KleinerNerd.prefix + "pong")) {
    		chan.sendMessage("PENG!").queue();
    	} else if (raw.equals(KleinerNerd.prefix + "shutdown")) {
    		if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
    			//perhaps inform the user
    			return;
    		}
    		chan.sendMessage("Shutting down").queue();
    		QueuedLog.action("Shutdown requested by " + msg.getAuthor().getName());
    		InstanceManager.setState(InstanceState.SHUTTING_DOWN);
    		event.getJDA().shutdown();
    	} else if (raw.equals(KleinerNerd.prefix + "logfile")) {
    		if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
    			return;
    		}
    		QueuedLog.action("Logfile requested by " + msg.getAuthor().getName());
    		try {
				QueuedLog.flushInterruptibly();
			} catch (InterruptedException e) {
				QueuedLog.warning("SimpleCommandListener was interrupted", e);
			} catch (NotNotifiableException e) {
				//this exception doesn't even exist in the latest version...
				QueuedLog.error("Yo Fox, your log implementation is faulty.", e);
			}
    		chan.sendFile(KleinerNerd.logFile).queue();
    	} else if (raw.startsWith(pollcommand)) {
    		if (raw.length() == pollcommand.length()) {
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
					}, (e) -> {
						QueuedLog.error("Could not create thumb poll", e);
					});
					try {
						chan.addReactionById(id, "U+1F44D").queue(poll::success, poll::failure);
						chan.addReactionById(id, "U+1F44E").queue(poll::success, poll::failure);
					} catch (NumberFormatException e) {
						QueuedLog.verbose("not an id");
						chan.sendMessage("Keine valide Nachrichten ID");
					}
				},(e) -> {
					QueuedLog.error("could not retrieve message history",e);
					chan.sendMessage("Ich konnte die Nachrichtenhistorie vor der Nachricht nicht abfragen.\n" +
							"Versuch doch mal die Nachrichten ID hinter den Befehl zu schreiben.").queue();
				});
			} else {
				int len = pollcommand.length() + 1;
				MultiActionHandler<? super Void> poll = new MultiActionHandler<>(2, (s) -> {
					//Discord doesn't allow deleting other peoples messages in DMs
					if (chan.getType() != ChannelType.PRIVATE)
						msg.delete().reason("thumb poll command auto deletion").queue();
				}, (e) -> {
					QueuedLog.error("Could not create thumb poll", e);
				});
				try {
					chan.addReactionById(raw.substring(len), "U+1F44D").queue(poll::success, poll::failure);
					chan.addReactionById(raw.substring(len), "U+1F44E").queue(poll::success, poll::failure);
				} catch (NumberFormatException e) {
					QueuedLog.verbose("not an id");
					chan.sendMessage("Keine valide Nachrichten ID");
				}
			}
    	}
    }
}
