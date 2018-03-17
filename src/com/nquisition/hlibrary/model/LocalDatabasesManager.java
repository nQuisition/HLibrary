package com.nquisition.hlibrary.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalDatabasesManager {
	private static final transient Logger logger = LogManager.getLogger(LocalDatabasesManager.class);
	private Map<String, String> folderMap = new HashMap<>();
	private String configFilePath = null;
	private String localDBsPath = null;
	
	public LocalDatabasesManager(String configFilePath, String localDBsPath) {
		
	}
}
