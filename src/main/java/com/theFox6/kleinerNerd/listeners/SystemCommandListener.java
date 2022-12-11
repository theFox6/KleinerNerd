package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class SystemCommandListener {
	public static final String[] noShutdown = {
			"Knapp daneben; Du hast den Ausschalter nicht getroffen.",
			"Da musst du schon den Stecker ziehen...",
			"Lass die Finger vom Ausschalter.",
			"Ich weigere mich.",
			"Nö."
	};

	public SystemCommandListener setupCommands(CommandManager cm) {
		cm.registerCommand(Commands.slash("ping", "Misst und zeigt verschiedene Netzwerklatenzzeiten."),
				(event) -> {
					long time = System.currentTimeMillis();
					event.reply("Pong!\n(warte auf Antwort von Discord)").queue(
							(h) -> h.editOriginalFormat("Pong!\n"
											+ "Sendezeit: %d ms\n"
											+ "letzter heartbeat: %d ms",
									System.currentTimeMillis() - time,
									h.getJDA().getGatewayPing()
							).queue()
					);
				}
		);
		cm.registerCommand(Commands.slash("pong", "Cooler als ping."),
				(event) -> event.reply("PENG!").queue()
		);
		cm.registerCommand(Commands.slash("logfile", "Schickt die Protokolldatei."), this::onLogfileCommand);
		cm.registerCommand(Commands.slash("shutdown", "Fährt den Bot herunter."), this::onShutdownCommand);
		cm.registerCommand(Commands.slash("update", "Aktualisiert den Bot und startet ihn neu."), this::onUpdateCommand);

		return this;
	}

	public void onShutdownCommand(SlashCommandInteractionEvent event) {
		User user = event.getUser();
		if (!ConfigFiles.getOwners().contains(user.getId())) {
			event.reply(KNHelpers.randomElement(noShutdown)).queue();
			return;
		}
		QueuedLog.action("Shutdown requested by " + user.getName());
		event.reply("fahre herunter").queue((ih) -> shutdown(ih.getJDA(), InstanceState.SHUTTING_DOWN));
	}

	private void onUpdateCommand(SlashCommandInteractionEvent event) {
		User user = event.getUser();
		if (!ConfigFiles.getOwners().contains(user.getId())) {
			event.reply(KNHelpers.randomElement(noShutdown)).queue();
			return;
		}
		QueuedLog.action("Update requested by " + user.getName());
		event.reply("aktualisiere").queue((ih) -> shutdown(ih.getJDA(), InstanceState.PREPARING_UPDATE));
	}

	public void onLogfileCommand(SlashCommandInteractionEvent event) {
		User user = event.getUser();
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
				.queue((h) -> event.getChannel().sendFiles(FileUpload.fromData(KleinerNerd.logFile)).queue());
	}

	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	String ownId = event.getJDA().getSelfUser().getId();
    	if (raw.startsWith(".shutdown") && msg.getMentions().getUsers().stream().map(User::getId).anyMatch(el -> el.equals(ownId))) {
    		if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
				chan.sendMessage(KNHelpers.randomElement(noShutdown)).queue();
    			return;
    		}
    		QueuedLog.action("Shutdown requested by " + msg.getAuthor().getName());
    		chan.sendMessage("Fahre herunter").queue((m) -> shutdown(m.getJDA(), InstanceState.SHUTTING_DOWN));
    	}
    }

	public void shutdown(JDA jda, InstanceState reason) {
		InstanceManager.setState(reason);
		jda.shutdown();
		/* unregister all the commands
		jda.retrieveCommands().queue((cl) -> {
			Consumer<? super Void> shutdown = (v) -> jda.shutdown();
			MultiActionHandler<Void> unregister = new MultiActionHandler<>(cl.size(), shutdown, (e) -> {
				QueuedLog.error("couldn't unregister command: " + e.getMessage());
				shutdown.accept(null);
			});
			cl.forEach((c) -> c.delete().queue(unregister::success, unregister::failure));
		});
		*/
	}
}
