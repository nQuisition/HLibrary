package com.nquisition.hlibrary.api;

import java.util.List;

public interface IDatabaseInterface {
	List<? extends IGFolder> getFolders();
	List<? extends IGImage> getImages();
	
	boolean addDirectory(String path, int depth);
}
