package com.nquisition.hlibrary.ui;

import java.util.Map;

import com.nquisition.hlibrary.api.UIHelper;
import com.nquisition.hlibrary.api.UIManager;
import com.nquisition.hlibrary.api.UIView;
import com.nquisition.hlibrary.model.DatabaseInterface;

public class DefaultUIManager extends UIManager {
	private UIHelper helper;
	@Override
	public void constructDefaults(Map<String, Object> parameters) {
		//FolderViewer, GalleryViewer, LocalDatabaseViewer?, SimilarityViewer, ThumbViewer
		DatabaseInterface dbInterface = (DatabaseInterface)parameters.get("dbInterface");
		String root = (String)parameters.get("root"); 
		FolderViewerFactory folderViewerFactory = new FolderViewerFactory();
		folderViewerFactory.setProperty("dbInterface", dbInterface);
		folderViewerFactory.setProperty("root", root);
		this.registerUIFactory("FolderViewer", folderViewerFactory);
		
		GalleryViewerFactory galleryViewerFactory = new GalleryViewerFactory();
		galleryViewerFactory.setProperty("dbInterface", dbInterface);
		this.registerUIFactory("GalleryViewer", galleryViewerFactory);
		
		helper = new DefaultUIHelper(this, dbInterface);
	}
	
	@Override
	public UIHelper getUIHelper() {
		return helper;
	}
}
