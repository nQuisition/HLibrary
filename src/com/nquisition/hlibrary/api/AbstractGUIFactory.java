package com.nquisition.hlibrary.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.scene.Node;

public abstract class AbstractGUIFactory implements ExtendableGUIFactory {
	private Map<String, List<Node>> elementMap = new HashMap<>();
	private Map<String, Object> properties = new HashMap<>();

	@Override
	public ExtendableGUIFactory registerElements(String pos, Node... elements) {
		List<Node> list = elementMap.get(pos);
		if(list == null) {
			list = new ArrayList<>();
			elementMap.put(pos, list);
		}
		Collections.addAll(list, elements);
		return this;
	}
	
	@Override
	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}
	
	@Override
	public Object getProperty(String name){
		return properties.get(name);
	}
	
	public Set<Entry<String, List<Node>>> getElementSet() {
		return elementMap.entrySet();
	}
}
