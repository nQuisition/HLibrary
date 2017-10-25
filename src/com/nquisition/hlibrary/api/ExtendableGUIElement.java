package com.nquisition.hlibrary.api;

import javafx.scene.Node;

public interface ExtendableGUIElement {
	void addElements(String pos, Node... elements);
	Object[] getData(String pos);
	String[] getAvailablePositions();
	void constructGUI();
}
