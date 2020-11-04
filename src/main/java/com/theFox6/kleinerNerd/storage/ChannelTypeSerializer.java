package com.theFox6.kleinerNerd.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import net.dv8tion.jda.api.entities.ChannelType;

public class ChannelTypeSerializer extends JsonSerializer<ChannelType> {
	@Override
	public void serialize(ChannelType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(value.name());
	}
}
