package com.nquisition.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ProcessQueue {
	public static final int NUM_PROCESSES = 8;
	
	private List<Process> processes;
	
	public ProcessQueue() {
		processes = new ArrayList<>();
	}
	
	public void waitForSlot(int sleepDelay) {
		while(processes.size() >= NUM_PROCESSES) {
            Iterator<Process> iter = processes.iterator();
            while (iter.hasNext()) {
                Process p = iter.next();
                if(!p.isAlive()) {
                    p = null;
                    iter.remove();
                }
            }
            try {
            	Thread.sleep(sleepDelay);
            } catch(InterruptedException e) { }
        }
	}
	
	public boolean add(Process process) {
		if(processes.size() >= NUM_PROCESSES)
			return false;
		processes.add(process);
		return true;
	}
}
