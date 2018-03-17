package com.nquisition.hlibrary.ui;

import java.util.List;
import java.util.Map.Entry;
import com.nquisition.hlibrary.api.AbstractGUIFactory;
import com.nquisition.hlibrary.model.DatabaseInterface;
import javafx.scene.Node;

public class FolderViewerFactory extends AbstractGUIFactory {
    //FIXME make constructor and pass those properties in it?
	@Override
	public FolderViewer build() {
		DatabaseInterface databaseInterface = (DatabaseInterface)this.getProperty("dbInterface");
		String root = this.getPropString("root");
		FolderViewer fw = new FolderViewer(databaseInterface, root);
		for(Entry<String, List<Node>> entry : this.getElementSet()) {
			System.out.println("Adding to " + entry.getKey());
			fw.addElements(entry.getKey(), entry.getValue().toArray(new Node[entry.getValue().size()]));
		}
		fw.constructGUI();
		return fw;
	}
}
