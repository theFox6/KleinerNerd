package com.theFox6.kleinerNerd.categories;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ConfigStateDeserializer extends JsonDeserializer<ConfigState> {
	@Override
	public ConfigState deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String text = p.getText();
		return ConfigState.valueOf(text);
	}
}