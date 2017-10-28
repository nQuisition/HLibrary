package com.nquisition.hlibrary.api;

import java.util.List;

public interface IGFolder extends IGEntry {
	int getRating();
	int getNumImages();
	String getAlias();
	String getPath();
	List<? extends IGImage> getImages();
}
