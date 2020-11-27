package com.theFox6.kleinerNerd.categories;

import java.util.LinkedHashMap;
import java.util.Map;
import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.conversation.rulebased.ExpectedStatement;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class CategoryCreation {
	public static class DedicatedCategoryBuilder {
		private String fullName;
		private String shortName;
		private int voiceCount;
		private boolean announce;

		public void setName(String name) {
			fullName = name;
		}

		public void setShort(String name) {
			shortName = name;
		}

		public void setVoiceCount(int count) {
			voiceCount = count;
		}

		/**
		 * @param a whether to announce the new category
		 * @return this
		 */
		public DedicatedCategoryBuilder setAnnounceEnabled(boolean a) {
			announce = a;
			return this;
		}

		public void buildLoggingTo(Guild guild, MessageChannel chan) {
			try {
				guild.createCategory(fullName).queue((nc) -> {
					nc.createTextChannel(shortName).queue(
							(ntc) -> continueWithVoice(guild, nc, chan, false), (e) -> {
								//TODO log
								continueWithVoice(guild, nc, chan, true);
							}
						);
				},(e) -> {
					//TODO log
				});
			} catch (InsufficientPermissionException e) {
				chan.sendMessage("Mir fehlen Berechtigungungen um die Kategorie zu erstellen...\n"
						+ "Fehlende Berechtigung: "+ e.getPermission().getName()).queue();
			}
		}

		private void continueWithVoice(Guild guild, Category nc, MessageChannel chan, Boolean faults) {
			if (voiceCount < 1)
				return;
			if (voiceCount > 1) {
				recursiveVoice(guild,nc,voiceCount,chan,faults);
			} else {
				nc.createVoiceChannel(shortName).queue(
						(nvc) -> continueWithAnnounce(guild,chan,faults), (e) -> {
							//TODO log
							continueWithAnnounce(guild,chan,true);
						}
					);
			}
		}

		private void recursiveVoice(Guild guild, Category nc, int count, MessageChannel chan, Boolean faults) {
			if (count > 0) {
				nc.createVoiceChannel(shortName + "-" + (voiceCount + 1 - count)).queue((nvc) -> {
					recursiveVoice(guild,nc,count - 1,chan,faults);
				}, e -> {
					//TODO log
					continueWithAnnounce(guild,chan,true);
				});
			} else {
				continueWithAnnounce(guild,chan,faults);
			}
		}

		private void continueWithAnnounce(Guild guild, MessageChannel chan, Boolean faults) {
			if (faults) {
				//TODO report fail
			} else {
				if (announce) {
					//TODO
				} else {
					chan.sendMessage("Kategorie erfolgreich erstellt").queue();
				}
			}
		}
	}

	private Map<String,ExpectedStatement> startedCreactions = new LinkedHashMap<>();
	private Map<String,DedicatedCategoryBuilder> creationState = new LinkedHashMap<>();
	private final ExpectedStatement categoryDialogue =
			new ExpectedStatement(KleinerNerd.prefix + "addcategory", (msg) -> {
				MessageChannel chan = msg.getChannel();
				creationState.put(chan.getId(),new DedicatedCategoryBuilder());
				chan.sendMessage("Wie soll die neue Kategorie vollständig ausgeschrieben heißen?").queue();
				return ExpectedStatement.Action.STAY;
			}).addNext(new ExpectedStatement(".*", true, (msg) -> {
				MessageChannel chan = msg.getChannel();
				creationState.get(chan.getId()).setName(msg.getContentRaw());
				chan.sendMessage("Wie soll die Abkürzung für die neue Kategorie lauten?").queue();
				return ExpectedStatement.Action.STAY;
			}).addNext(new ExpectedStatement(".*", true, (msg) -> {
				MessageChannel chan = msg.getChannel();
				creationState.get(chan.getId()).setShort(msg.getContentRaw());
				chan.sendMessage("Wieviele Sprachkanäle soll es dafür geben?").queue();
				return ExpectedStatement.Action.STAY;
			}).addNext(new ExpectedStatement("\\A[0-3]\\Z", true, (msg) -> {
				MessageChannel chan = msg.getChannel();
				creationState.get(chan.getId()).setVoiceCount(Integer.parseInt(msg.getContentRaw()));
				chan.sendMessage("In welchem Kanal soll die Kategorie angekündigt werden?\n"
						+ "\"keine\" um die Kategorie nicht anzukündigen.").queue();
				return ExpectedStatement.Action.STAY;
			}).addNext(new ExpectedStatement("keine", (msg) -> {
				MessageChannel chan = msg.getChannel();
				chan.sendMessage("Kategorie wird erstellt...").queue();
				creationState.get(chan.getId()).setAnnounceEnabled(false).buildLoggingTo(msg.getGuild(),chan);				
				return ExpectedStatement.Action.QUIT;
			})).setUnexpected((msg) -> {
				//TODO: build announce in given channel or fail
				return ExpectedStatement.Action.QUIT;
			}))).addNext(new ExpectedStatement("\\A[0-9]+\\Z",true, (msg) -> {
				msg.getChannel().sendMessage("Momentan sind nicht mehr als drei Sprachkanäle erlaubt.\n"
						+ "Bitte gib eine akzeptierte Anzahl ein.").queue();
				return ExpectedStatement.Action.RETURN;
			})).setUnexpected((msg) -> {
				msg.getChannel().sendMessage("Deine Eingabe scheint keine reine Ganzzahl zu sein.\n"
						+ "Bitte gib die Anzahl von Sprachkanälen ein, die erstellt werden soll.").queue();
				return ExpectedStatement.Action.RETURN;
			}));
	
	@SubscribeEvent
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Message msg = event.getMessage();
		if (msg.isWebhookMessage())
			return;
    	if (msg.getAuthor().equals(event.getJDA().getSelfUser()))
    		return;
    	Member author = event.getMember();
		Guild guild = event.getGuild();
		if (!KNHelpers.isModerator(author, guild))
			return;
		String raw = msg.getContentRaw();
		String chanId = msg.getChannel().getId();
		if (categoryDialogue.rawMatches(raw)) {
			categoryDialogue.advancedTo(msg);
			startedCreactions.put(chanId, categoryDialogue);
    	} else if (startedCreactions.containsKey(chanId)) {
    		ExpectedStatement s = startedCreactions.get(chanId);
    		if (s != null) {
    			startedCreactions.put(chanId, s.advance(msg));
    		}
    	}
	}
}
