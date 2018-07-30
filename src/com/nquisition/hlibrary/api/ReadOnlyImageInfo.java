package com.nquisition.hlibrary.api;

import java.io.IOException;

public interface ReadOnlyImageInfo extends ReadOnlyEntryInfo {
	String getName();
	String getFullPath();
	ReadOnlyFolderInfo getParent();
	boolean isOnTopLevel();
	//boolean computeSimilarity(boolean forceRecompute) throws IOException;
	//TODO need something like getSimilarityBytes
}
