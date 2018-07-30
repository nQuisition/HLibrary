package com.nquisition.hlibrary.api;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

public interface PropertyProvider {
	String getName();
	String getIdentifier();
	String[] getCustomProps();
	//TODO too implementation specific; giving avay JsonReader is not safe
	void readPropertyFromJson(ReadOnlyEntryInfo entry, String propName, JsonReader reader) throws IOException;
}
