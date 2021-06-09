package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SystemCommandListener implements CommandListener {
	public static final String[] noShutdown = {
			"Knapp daneben; Du hast den Ausschalter nicht getroffen.",
			"Da musst du schon den Stecker ziehen...",
			"Lass die Finger vom Ausschalter.",
			"Ich weigere mich.",
			"Nö."
	};

	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand("ping", "Misst und zeigt verschiedene Netzwerklatenzzeiten.").queue();
		jda.upsertCommand("pong", "Cooler als ping.").queue();
		jda.upsertCommand("logfile", "Schickt die Protokolldatei.").queue();
		jda.upsertCommand("shutdown", "Fährt den Bot herunter.").queue();
	}

	//perhaps make some more of these replies ephemeral

	@SubscribeEvent
	public void onCommand(SlashCommandEvent event) {
		String command = event.getName();
		User user = event.getUser();
		switch (command) {
			case "ping":
				long time = System.currentTimeMillis();
				event.reply("Pong!\n(waiting for an answer from Discord)").queue((h) -> {
					h.editOriginalFormat("Pong!\n"
									+ "sending time: %d ms\n"
									+ "last heartbeat: %d ms",
							System.currentTimeMillis() - time,
							event.getJDA().getGatewayPing()
					).queue();
				});
				break;
			case "pong":
				event.reply("PENG!").queue();
				break;
			case "shutdown":
				if (!ConfigFiles.getOwners().contains(user.getId())) {
					event.reply(KNHelpers.randomElement(noShutdown)).queue();
					return;
				}
				QueuedLog.action("Shutdown requested by " + user.getName());
				event.reply("fahre herunter").queue((m) -> {
					InstanceManager.setState(InstanceState.SHUTTING_DOWN);
					event.getJDA().shutdown();
				});
				break;
			case "logfile":
				if (!ConfigFiles.getOwners().contains(user.getId())) {
					event.reply("Die Protokolldaten sind zu sensibel um sie jedem zu schicken. Tut mir leid.").setEphemeral(true).queue();
					return;
				}
				QueuedLog.action("Logfile requested by " + user.getName());
				try {
					QueuedLog.flushInterruptibly();
				} catch (InterruptedException e) {
					QueuedLog.warning("logfile request was interrupted", e);
				}
				event.reply("log vom " + OffsetDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)))
						.queue((h) -> event.getChannel().sendFile(KleinerNerd.logFile).queue());
				break;
		}
	}

	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	String ownId = event.getJDA().getSelfUser().getId();
    	if (raw.startsWith(".shutdown") && msg.getMentionedUsers().stream().map(User::getId).anyMatch(el -> el.equals(ownId))) {
    		if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
				chan.sendMessage(KNHelpers.randomElement(noShutdown)).queue();
    			return;
    		}
    		QueuedLog.action("Shutdown requested by " + msg.getAuthor().getName());
    		chan.sendMessage("Shutting down").queue((m) -> {
				InstanceManager.setState(InstanceState.SHUTTING_DOWN);
				event.getJDA().shutdown();
			});
    	} else if (raw.equals(KleinerNerd.prefix + "update") ||
				(raw.startsWith(KleinerNerd.prefix + "update") && msg.getMentionedUsers().stream().map(User::getId).anyMatch(el -> el.equals(ownId)))) {
			if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
				//perhaps inform the user
				return;
			}
			chan.sendMessage("Starting update").queue();
			QueuedLog.action("Update requested by " + msg.getAuthor().getName());
			InstanceManager.setState(InstanceState.UPDATING);
			try {
				new ProcessBuilder("bash","update.sh").start();
			} catch (IOException e) {
				QueuedLog.error("Could not start update script",e);
			}
			event.getJDA().shutdown();
		}
    }
}
