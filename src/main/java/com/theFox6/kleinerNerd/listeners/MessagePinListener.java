package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.function.Consumer;

public class MessagePinListener {
	public static final String PUSHPIN = "\uD83D\uDCCC";

	public MessagePinListener setupCommands(CommandManager cm) {
		cm.registerCommand(
			Commands.slash("pincount", "stellt ein nach wievielen pin reaktionen eine Nachricht gepinnt wird")
				.addOption(OptionType.INTEGER, "anzahl", "Anzahl der Reaktionen (0 für Deaktivierung)", true)
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)),
			MessagePinListener::onPinCountCommand
		);
		return this;
	}
	private static void onPinCountCommand(SlashCommandInteractionEvent e) {
		if (e.getGuild() == null) {
			// not supported on private channels cuz pin count is a server setting
			e.reply("Das wird leider noch nicht in DMs unterstützt. (pin doch einfach selbst)").setEphemeral(true).queue();
			return;
		}
		int reactCount;
		try {
			reactCount = KNHelpers.getOptionMapping(e, "anzahl").getAsInt();
		} catch (OptionNotFoundException ex) {
			QueuedLog.error("couldn't get pin count", ex);
			e.reply("konnte pinanzahl nicht lesen").setEphemeral(true).queue();
			return;
		}
		GuildStorage.setPinReactCount(e.getGuild(), reactCount);
		if (reactCount == 0) {
			e.reply("Nachrichten werden nicht mehr durch Reaktionen mit " + PUSHPIN + " angepinnt.").queue();
			return;
		}
		e.reply("Nachrichten werden nach " + reactCount + " Reaktionen mit " + PUSHPIN + " angepinnt.").queue();
	}

	@SubscribeEvent
	public void onReactionAdded(MessageReactionAddEvent ev) {
		if (!ev.getEmoji().getName().equals(PUSHPIN))
			return;
		doOnSufficientReactions(ev, (m) -> {if (!m.isPinned()) {m.pin().queue();}});
	}

	private void doOnSufficientReactions(MessageReactionAddEvent e, Consumer<? super Message> s) {
		if (!e.isFromGuild()) {
			// not supported on private channels cuz pin count is a server setting
			return;
		}
		int neededReactionCount = GuildStorage.getPinReactCount(e.getGuild());
		if (neededReactionCount < 1)
			return;
		MessageReaction r = e.getReaction();
		if (r.hasCount())
			if (r.getCount() < neededReactionCount)
				return;
		e.getChannel().retrieveMessageById(e.getMessageId())
				.queue((m) -> {
					MessageReaction rr = m.getReaction(r.getEmoji());
					if (rr == null) {
						QueuedLog.warning("couldn't fetch pushpin reaction from a message reacted to with a pushpin");
						return;
					}
					if (rr.getCount() < neededReactionCount)
						return;
					s.accept(m);
				});
	}
}
