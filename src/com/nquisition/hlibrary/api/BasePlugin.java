package com.nquisition.hlibrary.api;

import java.util.List;
import java.util.function.Predicate;

public abstract class BasePlugin implements IBasePlugin {
	private IDatabaseInterface dbInterface;
	private UIManager uiManager;
	private CustomPropertiesManager propertiesManager;
	
	@Override
	public final void setDatabaseInterface(IDatabaseInterface dbInterface) {
		this.dbInterface = dbInterface;
	}
	
	@Override
	public final void setUIManager(UIManager manager) {
		this.uiManager = manager;
	}
	
	@Override
	public final void setCustomPropertiesManager(CustomPropertiesManager propertiesManager) {
		this.propertiesManager = propertiesManager;
	}
	
	public final IDatabaseInterface getDatabaseInterface() {
		return dbInterface;
	}
	
	public final UIManager getUIManager() {
		return uiManager;
	}
	
	public final CustomPropertiesManager getCustomPropertiesManager() {
		return propertiesManager;
	}
}
