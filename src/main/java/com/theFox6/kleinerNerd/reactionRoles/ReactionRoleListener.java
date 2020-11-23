package com.theFox6.kleinerNerd.reactionRoles;

import com.theFox6.kleinerNerd.data.MessageLocation;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class ReactionRoleListener {
	//private static final String addrrCommand = KleinerNerd.prefix + "rr add ";
	
	/*
	@SubscribeEvent
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Guild guild = event.getGuild();
		Member author = event.getMember();
		if (!KNHelpers.isModerator(author, guild))
			return;
		String raw = event.getMessage().getContentRaw();
		//associate reaction roles with a message
		if (raw.startsWith(addrrCommand)) {
			String args = raw.substring(addrrCommand.length());
			// args are: message id, emoji, role id
		}
		//edit reaction role configurations
	}
	*/
	
	private Role getReactionRoleFromEvent(GenericGuildMessageReactionEvent event) {
		MessageLocation loc = new MessageLocation(event);
		ReactionRoleMap config = ReactionRoleStorage.getConfig(loc);
		if (config == null)
			return null;
		String roleId = config.getRoleIdForReaction(event.getReactionEmote().getAsReactionCode());
		if (roleId == null) {
			QueuedLog.debug(event.getUserId() + " reacted to a reaction role message with a different Emote");
			return null;
		}
		Guild guild = event.getGuild();
		Role r = guild.getRoleById(roleId);
		if (r == null) {
			QueuedLog.warning("could not find the role associated with the reaction");
		}
		return r;
	}
	
	@SubscribeEvent
	public void onReactionAdded(GuildMessageReactionAddEvent event) {
		Role r = getReactionRoleFromEvent(event);
		if (r == null) {
			return;
		}
		Member member = event.getMember();
		event.getGuild().addRoleToMember(member, r).queue(
				(s) -> {
					QueuedLog.action("Rolle \""+r.getName()+"\" wurde "+member.getEffectiveName()+" zugewiesen.");
					event.getUser().openPrivateChannel().queue((pc) -> {
						pc.sendMessage("Rolle \""+r.getName()+"\" zugewiesen.").queue();
					});
				},
				(f) -> {
					QueuedLog.warning("Konnte Rolle \""+r.getName()+"\" nicht zuweisen.",f.getCause());
					event.getUser().openPrivateChannel().queue((pc) -> {
						pc.sendMessage("Konnte Rolle \""+r.getName()+"\" nicht zuweisen.").queue();
					});
				}
			);
	}
	
	@SubscribeEvent
	public void onReactionRemoved(GuildMessageReactionRemoveEvent event) {
		Role r = getReactionRoleFromEvent(event);
		if (r == null) {
			return;
		}
		Member member = event.getMember();
		event.getGuild().removeRoleFromMember(member, r).queue(
				(s) -> {
					QueuedLog.action("Rolle \""+r.getName()+"\" wurde von "+member.getEffectiveName()+" entfernt.");
					event.getUser().openPrivateChannel().queue((pc) -> {
						pc.sendMessage("Rolle \""+r.getName()+"\" entfernt.").queue();
					});
				},
				(f) -> {
					QueuedLog.warning("Konnte Rolle \""+r.getName()+"\" nicht entfernen.",f.getCause());
					event.getUser().openPrivateChannel().queue((pc) -> {
						pc.sendMessage("Konnte Rolle \""+r.getName()+"\" nicht entfernen.").queue();
					});
				}
			);
	}
}
