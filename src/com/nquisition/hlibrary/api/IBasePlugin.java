package com.nquisition.hlibrary.api;

public interface IBasePlugin {
	void setDatabaseInterface(IDatabaseInterface dbInterface);
	void setUIManager(UIManager manager);
	void setCustomPropertiesManager(CustomPropertiesManager manager);
	void init();
	void start();
	void stop();
	void dispose();
}
