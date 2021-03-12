package com.theFox6.kleinerNerd.categories;

import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.theFox6.kleinerNerd.data.MessageLocation;
import com.theFox6.kleinerNerd.reactionRoles.ReactionRoleStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class CategoryLayout {
	private static final String worried_face = "\ud83d\ude1f";
	@JsonProperty
	private String id;
	//private String name;
	@JsonProperty
	private String roleId;
	@JsonProperty
	private String reaction;
	@JsonProperty
	private String announceId;
	@JsonProperty
	private String announceChannel;
	@JsonSerialize(using = ConfigStateSerializer.class)//, as=String.class)
	@JsonDeserialize(using = ConfigStateDeserializer.class)
	private ConfigState configurationState;
	//private Map<String,String> textChannels;
	//private Map<String,VoiceConfiguration> voiceChannels;
	
	public CategoryLayout() {
		configurationState = ConfigState.UNCONFIGURED;
	}
	
	public void create(Guild g, String name, Consumer<Category> success) {
		g.createCategory(name).queue((c) -> {
			id = c.getId();
			configurationState = ConfigState.SET_ROLE;
			success.accept(c);
		}, (e) -> {
			//TODO: message
			QueuedLog.error("Could not create category", e);
		});
	}
	
	/**
	 * handles messages after the first configuration stage
	 * 
	 * careful: this may submit queues than change the state 
	 * @param raw the contents of the message the user sent
	 * @param chan the channel the message was sent in
	 * @param msg the message the user sent
	 * @return whether the configuration is finished
	 */
	public boolean configMessage(String raw, MessageChannel chan, Message msg) {
		switch (configurationState) {
		case CONFIGURED:
			QueuedLog.warning("CategoryLayout received a configMessage despite already being configured");
			return true;
		case EDIT_ROLE:
			if (raw.equalsIgnoreCase("ja")) {
				addRoleQuestion(chan);
				configurationState = ConfigState.SET_ROLE;
			} else if (raw.equalsIgnoreCase("nein")) {
				afterSetupRole(chan);
			} else {
				chan.sendMessage("Rolle mit Zugriff ändern? (ja oder nein)").queue();
			}
			return false;
		case SET_ROLE:
			Guild g = msg.getGuild();
			if (raw.equals("everyone")) {
				g.getCategoryById(id).upsertPermissionOverride(g.getPublicRole()).grant(Permission.MESSAGE_READ).queue();
				afterSetupRole(chan);
			} else {
				List<Role> roles = g.getRolesByName(raw, true);
				if (roles.isEmpty()) {
					g.createRole().queue((r) -> {
						r.getManager().setName(raw).queue((s) -> {
							chan.sendMessage("rolle erstellt").queue();
							roleId = r.getId();
							makePrivate(g);
							afterSetupRole(chan);
						});
					}, (e) -> {
						//TODO: send message
						QueuedLog.error("Couldn't create role " + raw, e);
						configurationState = ConfigState.PART_FAILED;
					});
				} else {
					chan.sendMessage("vorhandene Rolle verwendet").queue();
					roleId = roles.get(0).getId();
					makePrivate(g);
					afterSetupRole(chan);
				}
			}
			return false;
		case CHOOSE_ANNCOUNCE_CHANNEL:
			Guild guild = msg.getGuild();
			//perhaps add a null check for get channel
			if (raw.equalsIgnoreCase("keiner")) {
				//remove reaction role from old announce
				if (announceChannel != null && announceId != null && reaction != null) {
					guild.getTextChannelById(announceChannel).retrieveMessageById(announceId).queue((announce) -> {
						//actually get would be more appropriate
						ReactionRoleStorage.getOrCreateConfig(new MessageLocation(announce)).removeReactionRole(reaction);
						reaction = null;
					},(e) -> {
						QueuedLog.debug("old category announce not found");
					});
				}
				
				announceChannel = null;
				announceId = null;
				configurationState = ConfigState.CONFIGURED;
				chan.sendMessage("Einrichtung fertig").queue();
				return true;
			} else if (raw.equalsIgnoreCase("behalten")) {
				chan.sendMessage("Einrichtung fertig").queue();
				configurationState = ConfigState.CONFIGURED;
				return true;
			} else {
				//remove reaction role from old announce
				if (announceChannel != null && announceId != null && reaction != null) {
					guild.getTextChannelById(announceChannel).retrieveMessageById(announceId).queue((announce) -> {
						//actually get could be more appropriate
						ReactionRoleStorage.getOrCreateConfig(new MessageLocation(announce)).removeReactionRole(reaction);
					},(e) -> {
						QueuedLog.debug("old category announce not found");
					});
				}
				
				TextChannel target;
				List<TextChannel> chans = msg.getMentionedChannels();
				if (chans.isEmpty())
					target = null;
				else
					target = chans.get(0);
				if (target == null) {
					try {
						target = guild.getTextChannelById(raw);
					} catch (NumberFormatException e) {
						QueuedLog.verbose("not an id");
					}
				}
				if (target == null) {
					List<TextChannel> list = guild.getTextChannelsByName(raw, true);
					if (list.isEmpty()) {
						chan.sendMessage("Konnte Kanal \"" + raw + "\" nicht finden.").queue();
						return false;
					}
					target = list.get(0);
				}
				announceChannel = target.getId();
				chan.sendMessage("Soll eine in " + target.getAsMention() + " vorhandene Nachricht verwendet werden? Wenn nicht kann ich eine senden.").queue();
				configurationState = ConfigState.ADD_OR_EDIT_ANNCOUNCE;
				return false;
			}
			//should be unreachable
		case ADD_OR_EDIT_ANNCOUNCE:
			if (raw.equalsIgnoreCase("ja")) {
				chan.sendMessage("Die MessageID der Nachricht bitte.").queue();
				configurationState = ConfigState.CHOOSE_ANNOUNCE;
				return false;
			} else if (raw.equalsIgnoreCase("nein")) {
				chan.sendMessage("Was soll ich in die Ankündigung schreiben?").queue();
				configurationState = ConfigState.ADD_ANNOUNCE;
				return false;
			} else {
				chan.sendMessage("Ich mag nur ja und nein, lass mal abbrechen.").queue();
				configurationState = ConfigState.PART_FAILED;
				return false; //true would be ok too
			}
			//should be unreachable
		case CHOOSE_ANNOUNCE:
			announceId = raw;
			msg.getGuild().getTextChannelById(announceChannel).retrieveMessageById(raw).queue((t) -> {
				chan.sendMessage("Vorhandene Nachricht ausgewählt.\n"
						+ "Bei einer Reaktion mit welchem emote soll ich die Rolle vergeben? (`keins` um keine Reaktionsrolle einzurichten)").queue();
				configurationState = ConfigState.SET_RR_EMOTE;
			}, (e) -> {
				chan.sendMessage("Nachricht nicht gefunden.").queue();
				QueuedLog.warning("could not retrieve message", e);
				configurationState = ConfigState.PART_FAILED;
			});
			return false;
		case ADD_ANNOUNCE:
			msg.getGuild().getTextChannelById(announceChannel).sendMessage(raw).queue((m) -> {
				announceId = m.getId();
				chan.sendMessage("Nachricht gesendet.\n"
						+ "Bei einer Reaktion mit welchem emote soll ich die Rolle vergeben? (`keins` um keine Reaktionsrolle einzurichten)").queue();
				configurationState = ConfigState.SET_RR_EMOTE;
			}, (e) -> {
				chan.sendMessage("Konnte Nachricht nicht senden " + worried_face  + "\n"
						+ e.getMessage()).queue();
				QueuedLog.warning("couldn't send message", e);
				configurationState = ConfigState.PART_FAILED;
			});
			return false;
		case SET_RR_EMOTE:
			List<Emote> emotes = msg.getEmotes();
			if (raw.equalsIgnoreCase("keins")) {
				configurationState = ConfigState.CONFIGURED;
				chan.sendMessage("Einrichtung fertig").queue();
				return true;
			} else if (!emotes.isEmpty()) {
				Emote rrEmote = emotes.get(0);
				//this is the reaction code
				reaction = rrEmote.getName() + ":" + rrEmote.getId();
				msg.getGuild().getTextChannelById(announceChannel).retrieveMessageById(announceId).queue((a) -> {
					ReactionRoleStorage.getOrCreateConfig(new MessageLocation(a)).addReactionRole(reaction, roleId);
					a.addReaction(rrEmote).queue((s) -> {
						chan.sendMessage("Reaktion hinzugefügt").queue();
					}, (e) -> {
						chan.sendMessage("Ich konnte nicht mit dem Emote reagieren, reagiere gegebenenfalls selbst auf die Ankündigungsnachricht.").queue();
					});
					chan.sendMessage("Einrichtung fertig").queue();
					configurationState = ConfigState.CONFIGURED;
				}, (e) -> {
					chan.sendMessage("Ich konnte die Nachricht nicht mehr wiederfinden " + worried_face).queue();
					QueuedLog.warning("couldn't find message after making sure it existed", e);
					configurationState = ConfigState.PART_FAILED;
				});
				return true;
			} else if (!raw.isEmpty()) {
				reaction = raw;
				msg.getGuild().getTextChannelById(announceChannel).retrieveMessageById(announceId).queue((a) -> {
					if (roleId == null)
						QueuedLog.error("trying to set rr emote for null roleId");
					ReactionRoleStorage.getOrCreateConfig(new MessageLocation(a)).addReactionRole(reaction, roleId);
					a.addReaction(raw).queue((s) -> {
						chan.sendMessage("Reaktion hinzugefügt").queue();
					}, (e) -> {
						chan.sendMessage("Ich konnte nicht mit dem Emote reagieren, wahrscheinlich hast du mir Schrott geschickt.").queue();
					});
					chan.sendMessage("Einrichtung fertig").queue();
					configurationState = ConfigState.CONFIGURED;
				}, (e) -> {
					chan.sendMessage("Ich konnte die Nachricht nicht mehr wiederfinden " + worried_face).queue();
					QueuedLog.warning("couldn't find message after making sure it existed", e);
					configurationState = ConfigState.PART_FAILED;
				});
				/*
				chan.sendMessage("Keine Emotes gefunden "+worried_face).queue();
				configurationState = ConfigState.PART_FAILED;
				*/
				return true; //this should be handled better
			} else {
				QueuedLog.error("raw text was empty");
			}
		default:
			QueuedLog.warning("CategoryLayout in unknown state " + configurationState.name());
			return false;
		}
	}

	public static void addRoleQuestion(MessageChannel chan) {
		chan.sendMessage("Wie soll die Rolle heißen, die Zugriff auf diese Kategorie hat? (everyone für keine spezielle Rolle)").queue();
	}

	public void startWithExisting(String categoryId, TextChannel chan) {
		id = categoryId;
		addRoleQuestion(chan);
		configurationState = ConfigState.SET_ROLE;
	}

	public void startEditing(Guild g, TextChannel chan) {
		if (roleId == null)
			chan.sendMessage("Momentan ist keine spezielle Rolle mit Zugriff eingerichtet, soll das geändert werden?").queue();
		else
			chan.sendMessage("Momentan hat die Rolle " + g.getRoleById(roleId).getName() + " Zugriff auf diesen Channel, soll das geändert werden?").queue();
		configurationState = ConfigState.EDIT_ROLE;
	}

	private void makePrivate(Guild g) {
		Role r = g.getRoleById(roleId);
		Category c = g.getCategoryById(id);
		c.upsertPermissionOverride(r).grant(Permission.MESSAGE_READ).queue();
		c.upsertPermissionOverride(g.getPublicRole()).deny(Permission.MESSAGE_READ).queue();
	}
	
	private void afterSetupRole(MessageChannel chan) {
		chan.sendMessage("In welchem Kanal soll die Ankündigungsnachricht des Kanals sein?\n"
				+ "`keiner` um eine vorhandene Ankündigung zu dekonfigureren.\n"
				+ "`behalten` um die Konfiguration beizubehalten (falls die Kategorie neu ist ist keiner eingestellt)\n"
				+ "Ein Kanalname oder eine KanalID um den Kanal zu verwenden.").queue();
		configurationState = ConfigState.CHOOSE_ANNCOUNCE_CHANNEL;
	}

	public boolean delete(Guild guild) {
		configurationState = ConfigState.UNCONFIGURED;
		//perhaps ask whether to delete reaction role
		//remove reaction role from old announce
		if (announceChannel != null && announceId != null && reaction != null) {
			guild.getTextChannelById(announceChannel).retrieveMessageById(announceId).queue((announce) -> {
				//actually get would be more appropriate
				ReactionRoleStorage.getOrCreateConfig(new MessageLocation(announce)).removeReactionRole(reaction);
				reaction = null;
			},(e) -> {
				QueuedLog.debug("old category announce not found");
			});
		}
		//perhaps ask whether to delete announce
		announceChannel = null;
		announceId = null;
		reaction = null;
		//perhaps ask whether to delete role
		roleId = null;
		if (id != null) {
			Category category = guild.getCategoryById(id);
			if (category == null) {
				return false;
			} else {
				//perhaps check if this action succeeded 
				category.delete().reason("deleted category via command").queue();
				id = null;
			}
		}
		return true;
	}

}
