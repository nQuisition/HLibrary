package com.nquisition.hlibrary.api;

import java.util.List;

public interface IDatabaseInterface {
	List<? extends ReadOnlyFolderInfo> getActiveFolders();
	List<? extends ReadOnlyImageInfo> getActiveImages();
	
	boolean addDirectoryToActive(String path, int depth);
}
