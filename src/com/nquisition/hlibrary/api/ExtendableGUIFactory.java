package com.nquisition.hlibrary.api;

import javafx.scene.Node;

public interface ExtendableGUIFactory extends PropertyContainer {
	UIView build();
	ExtendableGUIFactory registerElements(String pos, Node... elements);
}
