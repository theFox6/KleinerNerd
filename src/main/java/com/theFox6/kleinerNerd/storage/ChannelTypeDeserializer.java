package com.theFox6.kleinerNerd.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.dv8tion.jda.api.entities.ChannelType;

public class ChannelTypeDeserializer extends JsonDeserializer<ChannelType> {
	@Override
	public ChannelType deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String text = p.getText();
		return ChannelType.valueOf(text);
	}
}
