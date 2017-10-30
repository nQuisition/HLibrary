package com.nquisition.hlibrary.api;

public interface ProgressMonitor {
	void start(int totalCount);
	void done(); //TODO needed?
	void add(int count);
}
