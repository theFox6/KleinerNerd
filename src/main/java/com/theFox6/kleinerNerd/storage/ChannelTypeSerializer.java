package com.theFox6.kleinerNerd.storage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.dv8tion.jda.api.entities.channel.ChannelType;

import java.io.IOException;

public class ChannelTypeSerializer extends JsonSerializer<ChannelType> {
	@Override
	public void serialize(ChannelType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(value.name());
	}
}
