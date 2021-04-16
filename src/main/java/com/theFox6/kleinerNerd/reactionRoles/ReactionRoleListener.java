package com.theFox6.kleinerNerd.reactionRoles;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.MessageLocation;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ReactionRoleListener {
	private static final String addrrCommand = KleinerNerd.prefix + "rr add";
	private Map<String, RRConfigurator> started = new LinkedHashMap<>();

	@SubscribeEvent
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Guild guild = event.getGuild();
		Member author = event.getMember();
		if (author.getId().equals(event.getJDA().getSelfUser().getId()))
			return;
		if (!KNHelpers.isModerator(author, guild))
			return;
		Message msg = event.getMessage();
		String raw = msg.getContentRaw();
		TextChannel chan = event.getChannel();
		String chanId = chan.getId();
		//associate reaction roles with a message
		if (raw.equals(addrrCommand)) {
			started.put(chanId, new RRConfigurator(chan));
		} else if (started.containsKey(chanId)) {
			if (started.get(chanId).handle(raw, chan, msg)) {
				started.remove(chanId);
			}
		}
		//edit reaction role configurations
		//delete reaction role configurations
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
