package com.nquisition.hlibrary.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.nquisition.hlibrary.HLibrary;

public abstract class UIManager {
	private Map<String, ExtendableGUIFactory> factories = new HashMap<>();
	private Map<String, List<UIView>> elements = new HashMap<>();
	private Map<UIView, String> reverseElements = new HashMap<>();
	private List<UIView> criticalElements = new ArrayList<>();
	
	public UIManager() {
	}
	
	public abstract void constructDefaults(Map<String, Object> parameters);
	
	public boolean registerUIFactory(String name, ExtendableGUIFactory factory) {
		//TODO maybe don't return false when this fails, insted change name and return it?
		System.out.println(factory.getClass());
		if(factories.containsKey(name))
			return false;
		factories.put(name, factory);
		return true;
	}
	
	public ExtendableGUIFactory getFactory(String name) {
		return factories.get(name);
	}
	
	public UIView buildFromFactory(String name, boolean critical) {
		return buildFromFactory(name, null, critical);
	}
	
	//TODO safe? Allows to overwrite params defined by default etx.
	public UIView buildFromFactory(String name, Map<String, Object> params, boolean critical) {
		if(!factories.containsKey(name))
			return null;
		ExtendableGUIFactory factory = factories.get(name);
		if(params != null) {
			for(Entry<String, Object> param : params.entrySet())
				factory.setProperty(param.getKey(), param.getValue());
		}
		UIView element = factory.build();
		List<UIView> list = elements.get(name);
		if(list == null) {
			list = new ArrayList<>();
			elements.put(name, list);
		}
		list.add(element);
		element.setOnCloseRequest(event -> requestClose(element));
		if(critical)
			criticalElements.add(element);
		reverseElements.put(element, name);
		return element;
	}
	
	public boolean requestClose(UIView element) {
		String name = reverseElements.get(element);
		if(name == null) {
			//TODO exception?
			//This element is not managed by this manager!
			return false;
		}
		if(criticalElements.contains(element) && !HLibrary.criticalCloseRequested())
			return false;
		List<UIView> list = elements.get(name);
		list.remove(element);
		if(list.size() <= 0)
			elements.remove(name);
		reverseElements.remove(element);
		criticalElements.remove(element);
		return true;
	}
}
