package com.theFox6.kleinerNerd.categories;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ConfigStateSerializer extends JsonSerializer<ConfigState> {
	@Override
	public void serialize(ConfigState value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(value.name());
	}
}