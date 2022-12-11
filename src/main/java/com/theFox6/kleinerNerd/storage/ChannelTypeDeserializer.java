package com.theFox6.kleinerNerd.storage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.dv8tion.jda.api.entities.channel.ChannelType;

import java.io.IOException;

//unused cuz some stuff is missing
public class ChannelTypeDeserializer extends JsonDeserializer<ChannelType> {
	@Override
	public ChannelType deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException {
		String text = p.getText();
		return ChannelType.valueOf(text);
	}
}
