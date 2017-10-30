package com.nquisition.hlibrary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {
	private ExecutorService executor;
	public static final int MAX_THREADS = 10;
	
	public TaskManager() {
		executor = Executors.newFixedThreadPool(MAX_THREADS);
	}
	
	public boolean submit() {
		return false;
	}
}
