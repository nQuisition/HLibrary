package com.nquisition.hlibrary.ui;

import java.util.List;
import java.util.Map.Entry;

import com.nquisition.hlibrary.api.AbstractGUIFactory;
import com.nquisition.hlibrary.model.DatabaseInterface;
import com.nquisition.hlibrary.model.Gallery;
import javafx.scene.Node;

public class GalleryViewerFactory extends AbstractGUIFactory {
	@Override
	public GalleryViewer build() {
		DatabaseInterface databaseInterface = (DatabaseInterface)this.getProperty("dbInterface");
		Gallery gal = (Gallery)this.getProperty("gallery");
		String startFrom = (String)this.getProperty("startFrom");
		GalleryViewer gw = new GalleryViewer(databaseInterface);
		for(Entry<String, List<Node>> entry : this.getElementSet()) {
			gw.addElements(entry.getKey(), entry.getValue().toArray(new Node[entry.getValue().size()]));
		}
		gw.constructGUI();
		gw.setGallery(gal, startFrom);
		return gw;
	}
	
	
}
