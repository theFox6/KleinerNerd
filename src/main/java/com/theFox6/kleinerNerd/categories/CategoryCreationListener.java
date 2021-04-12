package com.theFox6.kleinerNerd.categories;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.theFox6.kleinerNerd.KleinerNerd;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class CategoryCreationListener {
	private static final String rcCommand = KleinerNerd.prefix+"removecategory ";
	private Collection<String> startedChannels = new ConcurrentLinkedQueue<>();
	private Map<String, CategoryLayout> startedConfigs = new ConcurrentHashMap<>();

	@SubscribeEvent
	public void onGMessage(GuildMessageReceivedEvent event) {
		Message msg = event.getMessage();
		if (msg.isWebhookMessage() || msg.getAuthor().equals(event.getJDA().getSelfUser()))
			return;
		if (!msg.getMember().hasPermission(Permission.ADMINISTRATOR))
			return;
		TextChannel chan = event.getChannel();
		String raw = msg.getContentRaw();
		String chanId = chan.getId();
		if (raw.startsWith(rcCommand)) {
			String category = raw.substring(rcCommand.length());
			if (CategoryStorage.deleteByName(msg.getGuild(),category)) {
				chan.sendMessage("Kategorie gelöscht.").queue();
			} else {
				chan.sendMessage("Kategorie konnte nicht gelöscht werden.").queue();
			}
		} else if (raw.equals(KleinerNerd.prefix+"addcategory")) {
			chan.sendMessage("Wie soll die Kategorie heißen?").queue();
			startedChannels.add(chanId);
		} else if (startedChannels.contains(chanId)) {
			CategoryLayout l = CategoryStorage.getByName(msg.getGuild().getId(),raw);
			if (l == null) {
				List<Category> existing = msg.getGuild().getCategoriesByName(raw, true);
				if (existing.isEmpty()) {
					CategoryLayout cl = CategoryStorage.create(msg.getGuild().getId(),raw);
					startedConfigs.put(chanId, cl);
					startedChannels.remove(chanId);
					cl.create(msg.getGuild(), raw, (c) -> {
						chan.sendMessage("Kategorie " + c.getName() + " wurde erstellt.").queue();
						CategoryLayout.addRoleQuestion(chan);
					});
				} else {
					CategoryLayout cl = CategoryStorage.create(msg.getGuild().getId(),raw);
					startedConfigs.put(chanId, cl);
					startedChannels.remove(chanId);
					Category c = existing.get(0);
					chan.sendMessage("Kategorie " + c.getName() + " existiert bereits, gehe in Einrichtungsmodus.").queue();
					cl.startWithExisting(c.getId(),chan);
				}
			} else {
				chan.sendMessage("Einstellungen für Kategorie " + raw + " existieren bereits, gehe in Bearbeitungsmodus.").queue();
				startedConfigs.put(chanId, l);
				startedChannels.remove(chanId);
				l.startEditing(msg.getGuild(),chan);
			}
		} else if (startedConfigs.containsKey(chanId)) {
			if (startedConfigs.get(chanId).configMessage(raw,chan,msg))
				startedConfigs.remove(chanId);
		}
	}
}
