package com.nquisition.hlibrary.api;

import java.io.IOException;
import java.util.Map;

import com.google.gson.stream.JsonReader;

public interface CustomPropertiesManager {
	Map<String, String> registerProvider(PropertyProvider provider);
	void readPropertyFromJson(ReadOnlyEntryInfo entry, String propName, JsonReader reader) throws IOException;
}
