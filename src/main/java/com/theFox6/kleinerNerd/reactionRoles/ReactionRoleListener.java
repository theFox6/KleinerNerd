package com.theFox6.kleinerNerd.reactionRoles;

import com.theFox6.kleinerNerd.EmojiFormatException;
import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.commands.BadOptionTypeException;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import com.theFox6.kleinerNerd.commands.PermissionType;
import com.theFox6.kleinerNerd.data.MessageLocation;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ReactionRoleListener {
	// guildId, lastTimestamp
	private final Map<String, Instant> lastRRFaultMessage = new ConcurrentHashMap<>();

	public ReactionRoleListener setupCommands(CommandManager cm) {
		cm.registerCommand(
				new CommandData("rr", "ändert die Reaktions-Rollen Konfiguration").addSubcommands(
						new SubcommandData("add", "fügt eine Reaktion hinzu, die zum verteilen einer Rolle dient")
								.addOption(OptionType.CHANNEL, "kanal", "der Kanal in dem die Nachricht ist, unter die die Reaktion soll", true)
								.addOption(OptionType.STRING, "nachrichtenid", "die ID der Nachricht der die Reaktion angehängt werden soll", true)
								.addOption(OptionType.STRING, "emote", "das emote für die Rollenänderung", true)
								.addOption(OptionType.ROLE, "rolle", "die Rolle die reagierenden Benutzern zugewiesen wird", true)
				).setDefaultEnabled(false),
				this::onRRCommand,
				PermissionType.MOD_ONLY
		);

		return this;
	}

	public void onRRCommand(SlashCommandEvent ev) {
		if (!ev.isFromGuild()) //not supported for now
			return; //TODO: warn
		if ("add".equals(ev.getSubcommandName())) {
			try {
				Emoji emote = parseEmoteOption(ev);
				Role role = KNHelpers.getOptionMapping(ev, "rolle").getAsRole();
				parseAndFetchMessage(ev, (msg) -> addReactionRoleCommand(ev, emote, msg, role));
			} catch (OptionNotFoundException e) {
				QueuedLog.error("could not extract rr add option",e);
				ev.reply("konnte " + e.getOption() + " option nicht extrahieren").setEphemeral(true).queue();
			} catch (EmojiFormatException e) {
				QueuedLog.error("could not parse emoji in rr add command: " + e.getMessage());
				ev.reply("konnte emoji nicht erkennen: " + e.getMessage()).setEphemeral(true).queue();
			} catch (BadOptionTypeException e) {
				QueuedLog.error("bad option type for rr add",e);
				ev.reply("konnte " + e.getOption() + " option nicht korrekt extrahieren").setEphemeral(true).queue();
			}
		} else {
			QueuedLog.warning("unknown subcommand rr " + ev.getSubcommandName());
		}
	}

	private Emoji parseEmoteOption(SlashCommandEvent ev) throws OptionNotFoundException, EmojiFormatException {
		Emoji emote = Emoji.fromMarkdown(KNHelpers.getOptionMapping(ev,"emote").getAsString());
		if (emote.isUnicode()) {
			String u = emote.getName();
			if (u.codePointCount(0,u.length()) > 1)
				throw new EmojiFormatException("too many code points", emote.getAsMention());
		}
		return emote;
	}

	private void parseAndFetchMessage(SlashCommandEvent ev, Consumer<? super Message> onMessageFetched) throws OptionNotFoundException, BadOptionTypeException {
		OptionMapping channelOption = KNHelpers.getOptionMapping(ev, "kanal");
		MessageChannel chan = channelOption.getAsMessageChannel();
		if (chan == null)
			throw new BadOptionTypeException(ev.getCommandPath(), "kanal", channelOption.getType(), OptionType.CHANNEL, MessageChannel.class);
		chan.retrieveMessageById(KNHelpers.getOptionMapping(ev, "nachrichtenid").getAsString()).queue(onMessageFetched, (e) -> {
			if ("10008: Unknown Message".equals(e.getMessage())) {
				QueuedLog.debug("tried to fetch unknown message id");
				ev.reply("konnte Nachricht mit angegebener id im angegeben kanal nicht finden").setEphemeral(true).queue();
			} else {
				QueuedLog.warning("could not fetch message for rr",e);
				ev.reply("konnte nachricht nicht finden: " + e.getMessage()).setEphemeral(true).queue();
			}
		});
	}

	private void addReactionRoleCommand(SlashCommandEvent ev, Emoji emote, Message tmsg, Role role) {
		if (emote.isUnicode()) {
			String emoji = emote.getAsMention();
			ReactionRoleStorage.getOrCreateConfig(new MessageLocation(tmsg)).addReactionRole(emoji, role.getId());
			tmsg.addReaction(emoji).queue(s -> {
			}, (e) -> {
				if (e.getMessage().equals("50013: Missing Permissions")) {
					QueuedLog.debug("no permissions to react with rr emote");
					ev.getChannel().sendMessage("Ich habe keine Berechtigung mit dem rr Emote zu reagieren.").queue();
				} else {
					QueuedLog.warning("could not add rr emote", e);
					ev.getChannel().sendMessage("Ich konnte nicht mit dem rr emoji reagieren.").queue();
				}
			});
		} else {
			//perhaps also fetch external emotes and cancel if check fails
			ReactionRoleStorage.getOrCreateConfig(new MessageLocation(tmsg))
					.addReactionRole(emote.getName() + ":" + emote.getId(), role.getId());
			Guild g = ev.getGuild();
			if (g == null) {
				QueuedLog.warning("rr add outside of a guild");
				ev.reply("Dieser Befehl kann nur in Gilden/Servern verwendet werden.").setEphemeral(true).queue();
				return;
			}
			Emote guildEmote = g.getEmoteById(emote.getId());
			if (guildEmote == null) {
				QueuedLog.warning("could not find rr emote " + emote.getAsMention() + " within guild");
				ev.getChannel().sendMessage("Ich konnte nicht mit dem rr emoji reagieren, da ich es nicht in diesem Server finden konnte.").queue();
			} else {
				tmsg.addReaction(guildEmote).queue((s) -> {
				}, (e) -> {
					if (e.getMessage().equals("50013: Missing Permissions")) {
						QueuedLog.debug("no permissions to react with rr emote");
						ev.getChannel().sendMessage("Ich habe keine Berechtigung mit dem rr Emote zu reagieren.").queue();
					} else {
						QueuedLog.warning("could not add rr emote", e);
						ev.getChannel().sendMessage("Ich konnte nicht mit dem rr emoji reagieren.").queue();
					}
				});
			}
		}
		ev.reply("Mit " + emote.getAsMention() + " kann man jetzt die Rolle \"" + role.getName() + "\" bekommen.").queue();
	}

	private Role getReactionRoleFromEvent(GenericGuildMessageReactionEvent event) {
		MessageLocation loc = new MessageLocation(event);
		ReactionRoleMap config = ReactionRoleStorage.getConfig(loc);
		if (config == null)
			return null;
		String reactionCode = event.getReactionEmote().getAsReactionCode();
		String roleId = config.getRoleIdForReaction(reactionCode);
		if (roleId == null) {
			QueuedLog.debug(event.getUserId() + " reacted to a reaction role message with a different Emote " + reactionCode);
			if (config.isEmpty()) {
				QueuedLog.warning("empty configuration for message " + loc.messageId + " in channel <#"+loc.channelId+">");
				ReactionRoleStorage.removeIfEmpty(loc);
			} else {
				QueuedLog.debug("available reaction roles for this message:\n" + config.dump());
			}
			return null;
		}
		Guild guild = event.getGuild();
		Role r = guild.getRoleById(roleId);
		if (r == null) {
			QueuedLog.warning("could not find the role associated with the reaction");
		}
		return r;
	}
	
	/* more hungry, but enforces correct usage
	private void getMember(GenericGuildMessageReactionEvent event, Consumer<Member> action) {
		Member member = event.getMember();
		if (member == null) {
			event.getGuild().retrieveMemberById(event.getUserId()).queue(action, (e) -> {
				QueuedLog.error("could not retrieve member that caused a GuildEvent",e);
			});
		} else {
			action.accept(member);
		}
	}
	*/

	private boolean ratelimitRRFault(Guild guild) {
		String gid = guild.getId();
		if (lastRRFaultMessage.containsKey(gid))
			if (Instant.now().isBefore(lastRRFaultMessage.get(gid).plus(10, ChronoUnit.MINUTES)))
				return false;
		lastRRFaultMessage.put(gid, Instant.now());
		return true;
	}
	
	private void getMember(Member eventMember, Guild guild, String userId, Consumer<Member> action) {
		if (eventMember == null) {
			guild.retrieveMemberById(userId).queue(action, (e) -> QueuedLog.error("could not retrieve member",e));
		} else {
			action.accept(eventMember);
		}
	}
	
	@SubscribeEvent
	public void onReactionAdded(GuildMessageReactionAddEvent event) {
		Role r = getReactionRoleFromEvent(event);
		if (r == null) {
			return;
		}
		String userId = event.getUserId();
		if (event.getJDA().getSelfUser().getId().equals(userId))
			return;
		Guild guild = event.getGuild();
		getMember(event.getMember(), guild, userId, (member) -> {
			final String name = member.getEffectiveName();
			try {
				guild.addRoleToMember(member, r).queue(
						(s) -> {
							QueuedLog.debug("Role \"" + r.getName() + "\" was assigned to " + name + ".");
							member.getUser().openPrivateChannel().queue(
									(pc) -> pc.sendMessage("Rolle \"" + r.getName() + "\" zugewiesen.").queue()
							);
						},
						(f) -> {
							QueuedLog.warning("Couldn't assign role \"" + r.getName() + "\".", f.getCause());
							member.getUser().openPrivateChannel().queue(
									(pc) -> pc.sendMessage("Ich konnte die Rolle \""
											+ r.getName() + "\" aufgrund eines Fehlers nicht zuweisen:" +
											f.getMessage()).queue()
							);
						}
				);
			} catch (HierarchyException e) {
				QueuedLog.debug("too low in role hierarchy to assign reaction role");
				String heMsg = "Ich konnte die Rolle \"" + r.getName() + "\" nicht zuweisen, " +
						"da ich in der Rollenhierarchie unter ihr stehe.";
				member.getUser().openPrivateChannel().queue((pc) -> pc.sendMessage(heMsg).queue());
				if (ratelimitRRFault(guild))
					ModLog.sendToGuild(guild,heMsg);
			}
		});
	}

	@SubscribeEvent
	public void onReactionRemoved(GuildMessageReactionRemoveEvent event) {
		Role r = getReactionRoleFromEvent(event);
		if (r == null) {
			return;
		} 
		String userId = event.getUserId();
		if (event.getJDA().getSelfUser().getId().equals(userId))
			return;
		Guild guild = event.getGuild();
		getMember(event.getMember(), guild, userId, (member) -> {
			final String name = member.getEffectiveName();
			try {
				guild.removeRoleFromMember(member, r).queue(
						(s) -> {
							QueuedLog.debug("Role \"" + r.getName() + "\" was removed from " + name + ".");
							member.getUser().openPrivateChannel().queue(
									(pc) -> pc.sendMessage("Rolle \"" + r.getName() + "\" entfernt.").queue()
							);
						},
						(f) -> {
							QueuedLog.warning("Couldn't remove role \"" + r.getName() + "\".", f.getCause());
							member.getUser().openPrivateChannel().queue(
									(pc) -> pc.sendMessage("Ich konnte die Rolle \""
											+ r.getName() + "\" aufgrund eines Fehlers nicht entfernen:" +
									f.getMessage()).queue()
							);
						}
				);
			} catch (HierarchyException e) {
				QueuedLog.debug("too low in role hierarchy to remove reaction role");
				String heMsg = "Ich konnte die Rolle \"" + r.getName() + "\" nicht entfernen, " +
						"da ich in der Rollenhierarchie unter ihr stehe.";
				member.getUser().openPrivateChannel().queue((pc) -> pc.sendMessage(heMsg).queue());
				if (ratelimitRRFault(guild))
					ModLog.sendToGuild(guild,heMsg);
			}
		});
	}
}
