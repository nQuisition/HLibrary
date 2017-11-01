package com.nquisition.hlibrary.api;

import java.io.IOException;

public interface IGImage extends IGEntry {
	String getName();
	String getFullPath();
	IGFolder getParent();
	boolean isOnTopLevel();
	boolean computeSimilarity(boolean forceRecompute) throws IOException;
	//TODO need something like getSimilarityBytes
}
