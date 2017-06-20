package com.nquisition.hlibrary.fxutil;

import com.nquisition.fxutil.FXFactory;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

public class HFXFactory
{
	private HFXFactory() {}
	
	public static Label createSectionTitleLabel(String text, HStyleSheet styleSheet)
	{
		Label res = FXFactory.createLabel(styleSheet.getSectionTitleFont(), -1,
				styleSheet.getSectionTitleBackground(), new Insets(2,2,2,2));
		res.setText(text);
		return res;
	}
	
	public static Label createMenuLabel(String text, HStyleSheet styleSheet)
	{
		Label res = FXFactory.createLabel(styleSheet.getDefaultMenuFont(), -1, null, Insets.EMPTY);
		res.setText(text);
		return res;
	}
	
	public static CheckBox createBoundCheckBox(String label, BooleanProperty property)
	{
		CheckBox res = FXFactory.createUnfocusedCheckBox(label);
		res.setSelected(property.get());
		res.selectedProperty().bindBidirectional(property);
		return res;
	}
	
	public static CheckBox createUnfocusedCheckBox(String label)
	{
		return FXFactory.createUnfocusedCheckBox(label);
	}
	
	public static Button createUnboundedButton(String label)
	{
		return FXFactory.createUnboundedButton(label);
	}
}
