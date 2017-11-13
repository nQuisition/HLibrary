package com.nquisition.hlibrary.api;

import java.util.List;

public interface IDatabaseInterface {
	List<? extends IGFolder> getActiveFolders();
	List<? extends IGImage> getActiveImages();
	
	boolean addDirectoryToActive(String path, int depth);
}
