package com.nquisition.hlibrary.api;

import java.util.List;

public interface ReadOnlyFolderInfo extends ReadOnlyEntryInfo {
	int getRating();
	int getNumImages();
	String getAlias();
	String getPath();
	List<? extends ReadOnlyImageInfo> getImages();
}
