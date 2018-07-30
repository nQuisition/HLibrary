package com.nquisition.hlibrary.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.stream.JsonReader;
import com.nquisition.hlibrary.api.CustomPropertiesManager;
import com.nquisition.hlibrary.api.ReadOnlyEntryInfo;
import com.nquisition.hlibrary.api.PropertyProvider;

public class HCustomPropertiesManager implements CustomPropertiesManager {
	private Map<String, PropertyProvider> providers = new HashMap<>();
	private Map<String, PropertyProvider> providerProps = new HashMap<>();
	private Map<String, String> reversePropMap = new HashMap<>();
	
	//TODO this drops on name conflict, probably there is a better way
	@Override
	public Map<String, String> registerProvider(PropertyProvider provider) {
		String id = provider.getIdentifier().replace('_', '-');
		if(providers.containsKey(id))
			return null;
		providers.put(id, provider);
		Map<String, String> res = new HashMap<>();
		for(String prop : provider.getCustomProps()) {
			String modProp = id + "_" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
			res.put(prop, modProp);
			reversePropMap.put(modProp, prop);
			providerProps.put(modProp, provider);
		}
		return res;
	}

	@Override
	public void readPropertyFromJson(ReadOnlyEntryInfo entry, String propName, JsonReader reader) throws IOException {
		PropertyProvider provider = providerProps.get(propName);
		if(provider == null) {
			//FIXME need to somehow save props even if the plugin that created them is unavailable!
			//Otherwise they will be lost on next save!
			reader.skipValue();
		} else {
			provider.readPropertyFromJson(entry, reversePropMap.get(propName), reader);
		}
	}
	
	
}
