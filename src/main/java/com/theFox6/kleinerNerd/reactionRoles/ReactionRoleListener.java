package com.theFox6.kleinerNerd.reactionRoles;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.MessageLocation;
import com.theFox6.kleinerNerd.listeners.CommandListener;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ReactionRoleListener implements CommandListener {
	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(new CommandData("rr", "ändert die Reaktions-Rollen Konfiguration").addSubcommands(
				new SubcommandData("add", "fügt eine Reaktion hinzu, die zum verteilen einer Rolle dient")
						.addOption(OptionType.CHANNEL, "kanal", "der Kanal in dem die Nachricht ist, unter die die Reaktion soll", true)
						.addOption(OptionType.STRING, "nachrichtenid", "die ID der Nachricht der die Reaktion angehängt werden soll", true)
						.addOption(OptionType.STRING, "emote", "das emote für die Rollenänderung", true)
						.addOption(OptionType.ROLE, "rolle", "die Rolle die reagierenden Benutzern zugewiesen wird", true)
		)).setDefaultEnabled(false).queue((c) -> jda.getGuilds().forEach((g) -> {
			List<CommandPrivilege> privileges = new LinkedList<>();
			privileges.add(CommandPrivilege.enableUser(g.getOwnerId()));
			String modrole = GuildStorage.getSettings(g.getId()).getModRole();
			if (modrole != null)
				privileges.add(CommandPrivilege.enableRole(modrole));
			c.updatePrivileges(g,privileges).queue();
		}));
		//TODO: update privileges on modrole update
	}

	@SubscribeEvent
	public void onCommand(SlashCommandEvent ev) {
		if (!ev.getName().equals("rr"))
			return;
		if (!ev.isFromGuild()) //not supported for now
			return;
		if ("add".equals(ev.getSubcommandName())) {
			Emoji emote = Emoji.fromMarkdown(ev.getOption("emote").getAsString());
			ev.getOption("kanal").getAsMessageChannel().retrieveMessageById(ev.getOption("nachrichtenid").getAsString()).queue((tmsg) -> {
				if (emote.isUnicode()) {
					String emoji = emote.getAsMention();
					ReactionRoleStorage.getOrCreateConfig(new MessageLocation(tmsg)).addReactionRole(emoji, ev.getOption("rolle").getAsRole().getId());
					tmsg.addReaction(emoji).queue(s -> {
					}, (e) -> {
						QueuedLog.warning("could not add rr emoji", e);
						ev.getChannel().sendMessage("could not add rr emoji, you will have to add it manually").queue();
					});
				} else {
					ReactionRoleStorage.getOrCreateConfig(new MessageLocation(tmsg))
							.addReactionRole(emote.getName() + ":" + emote.getId(), ev.getOption("rolle").getAsRole().getId());
					tmsg.addReaction(ev.getGuild().getEmoteById(emote.getId())).queue((s) -> {
					}, (e) -> {
						QueuedLog.warning("could not add rr emote", e);
						ev.getChannel().sendMessage("could not add rr emote, you will have to add it manually").queue();
					});
				}
				ev.reply("reaction role added").queue();
			}, (e) -> {
				if ("10008: Unknown Message".equals(e.getMessage())) {
					ev.reply("konnte Nachricht mit angegebener id nicht finden").setEphemeral(true).queue();
				} else {
					ev.reply("konnte nachricht nicht finden: " + e.getMessage()).setEphemeral(true).queue();
				}
			});
		} else {
			QueuedLog.warning("unknown subcommand rr " + ev.getSubcommandName());
		}
	}

	private Role getReactionRoleFromEvent(GenericGuildMessageReactionEvent event) {
		MessageLocation loc = new MessageLocation(event);
		ReactionRoleMap config = ReactionRoleStorage.getConfig(loc);
		if (config == null)
			return null;
		String reactionCode = event.getReactionEmote().getAsReactionCode();
		String roleId = config.getRoleIdForReaction(reactionCode);
		if (roleId == null) {
			//TODO: remove empty configs and warn
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
	
	/* unused, but enforces correct usage
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
	
	private void getMember(Member eventMember, Guild guild, String userId, Consumer<Member> action) {
		if (eventMember == null) {
			guild.retrieveMemberById(userId).queue(action, (e) -> {
				QueuedLog.error("could not retrieve member",e);
			});
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
			guild.addRoleToMember(member, r).queue(
					(s) -> {
						QueuedLog.debug("Rolle \""+r.getName()+"\" wurde "+name+" zugewiesen.");
						member.getUser().openPrivateChannel().queue((pc) -> {
							pc.sendMessage("Rolle \""+r.getName()+"\" zugewiesen.").queue();
						});
					},
					(f) -> {
						QueuedLog.warning("Konnte Rolle \""+r.getName()+"\" nicht zuweisen.",f.getCause());
						member.getUser().openPrivateChannel().queue((pc) -> {
							pc.sendMessage("Konnte Rolle \""+r.getName()+"\" nicht zuweisen.").queue();
						});
					}
				);
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
			guild.removeRoleFromMember(member, r).queue(
					(s) -> {
						QueuedLog.debug("Rolle \""+r.getName()+"\" wurde von "+name+" entfernt.");
						member.getUser().openPrivateChannel().queue((pc) -> {
							pc.sendMessage("Rolle \""+r.getName()+"\" entfernt.").queue();
						});
					},
					(f) -> {
						QueuedLog.warning("Konnte Rolle \""+r.getName()+"\" nicht entfernen.",f.getCause());
						member.getUser().openPrivateChannel().queue((pc) -> {
							pc.sendMessage("Konnte Rolle \""+r.getName()+"\" nicht entfernen.").queue();
						});
					}
				);
		});
	}
}
