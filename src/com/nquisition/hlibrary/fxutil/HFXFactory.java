package com.nquisition.hlibrary.fxutil;

import com.nquisition.fxutil.FXFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Label;

public class HFXFactory
{
	private HFXFactory() {}
	
	public static Label createMenuLabel(String text, HStyleSheet styleSheet)
	{
		Label res = FXFactory.createLabel(styleSheet.getSectionTitleFont(), -1,
				styleSheet.getSectionTitleBackground(), new Insets(2,2,2,2));
		res.setText(text);
		return res;
	}
}
