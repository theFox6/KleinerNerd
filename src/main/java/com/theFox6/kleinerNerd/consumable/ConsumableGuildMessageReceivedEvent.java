package com.theFox6.kleinerNerd.consumable;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ConsumableGuildMessageReceivedEvent extends GuildMessageReceivedEvent implements Consumable {
	
	private boolean consumed = false;

	public ConsumableGuildMessageReceivedEvent(GuildMessageReceivedEvent event) {
		super(event.getJDA(), event.getResponseNumber(), event.getMessage());
	}

	public ConsumableGuildMessageReceivedEvent(JDA api, long responseNumber, Message message) {
		super(api, responseNumber, message);
	}

	@Override
	public void consume() {
		consumed  = true;
	}

	@Override
	public boolean isConsumed() {
		return consumed;
	}

}
