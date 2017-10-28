package com.nquisition.hlibrary.api;

public abstract class BasePlugin implements IBasePlugin {
	private IDatabaseInterface dbInterface;
	private UIManager uiManager;
	
	public void setDatabaseInterface(IDatabaseInterface dbInterface) {
		this.dbInterface = dbInterface;
	}
	public void setUIManager(UIManager manager) {
		this.uiManager = manager;
	}
	
	public IDatabaseInterface getDatabaseInterface() {
		return dbInterface;
	}
	
	public UIManager getUIManager() {
		return uiManager;
	}
}
