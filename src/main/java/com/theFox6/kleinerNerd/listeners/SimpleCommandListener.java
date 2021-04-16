package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.storage.ConfigFiles;

import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.deamon.ConditionNotifier.NotNotifiableException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class SimpleCommandListener {
	public static final String pollcommand = KleinerNerd.prefix + "thumbpoll"

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
    		try {
				int len = pollcommand.length() + 1;
				//TODO: check whether both succeeded, report any failures, only delete on success
    			chan.addReactionById(raw.substring(len), "U+1F44D").queue();
    			chan.addReactionById(raw.substring(len), "U+1F44E").queue();
    			msg.delete().reason("thumbpoll command auto deletion").queue();
			} catch (NumberFormatException e) {
				QueuedLog.verbose("not an id");
				chan.sendMessage("Keine valide Nachrichten ID");
			}
    	}
    	//TODO: thumbpoll for last message (no arguments)
    }
}
