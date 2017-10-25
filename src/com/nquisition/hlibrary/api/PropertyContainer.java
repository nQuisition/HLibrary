package com.nquisition.hlibrary.api;

public interface PropertyContainer {
	void setProperty(String name, Object value);
	Object getProperty(String name);
	default int getPropInt(String name) {
		return (Integer)getProperty(name);
	}
	default String getPropString(String name) {
		return (String)getProperty(name);
	}
}
