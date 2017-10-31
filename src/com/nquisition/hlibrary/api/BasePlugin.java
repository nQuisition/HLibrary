package com.nquisition.hlibrary.api;

public abstract class BasePlugin implements IBasePlugin {
	private IDatabaseInterface dbInterface;
	private UIManager uiManager;
	private CustomPropertiesManager propertiesManager;
	
	@Override
	public void setDatabaseInterface(IDatabaseInterface dbInterface) {
		this.dbInterface = dbInterface;
	}
	
	@Override
	public void setUIManager(UIManager manager) {
		this.uiManager = manager;
	}
	
	@Override
	public void setCustomPropertiesManager(CustomPropertiesManager propertiesManager) {
		this.propertiesManager = propertiesManager;
	}
	
	public IDatabaseInterface getDatabaseInterface() {
		return dbInterface;
	}
	
	public UIManager getUIManager() {
		return uiManager;
	}
	
	public CustomPropertiesManager getCustomPropertiesManager() {
		return propertiesManager;
	}
}
